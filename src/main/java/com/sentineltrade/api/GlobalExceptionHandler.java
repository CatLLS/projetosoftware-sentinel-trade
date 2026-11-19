package com.sentineltrade.api;

import com.sentineltrade.application.ResourceNotFoundException;
import com.sentineltrade.domain.market.UnknownAssetException;
import com.sentineltrade.domain.order.DuplicateOrderException;
import com.sentineltrade.domain.order.InvalidOrderStateException;
import com.sentineltrade.domain.risk.RiskViolationException;
import com.sentineltrade.infrastructure.security.SecurityAuditHandler;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.hibernate.query.sqm.UnknownPathException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * <<no down-flow>>: every error becomes an RFC 7807 ProblemDetail. Only messages written by us
 * are returned; anything unexpected is logged server-side and answered with a generic 500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ApplicationEventPublisher events;

    public GlobalExceptionHandler(ApplicationEventPublisher events) {
        this.events = events;
    }

    @ExceptionHandler({RiskViolationException.class, UnknownAssetException.class})
    ProblemDetail orderRejected(RuntimeException e) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Order rejected", e.getMessage());
    }

    @ExceptionHandler({InvalidOrderStateException.class, DuplicateOrderException.class})
    ProblemDetail conflict(RuntimeException e) {
        return problem(HttpStatus.CONFLICT, "Conflict", e.getMessage());
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, DataIntegrityViolationException.class})
    ProblemDetail concurrentChange() {
        return problem(HttpStatus.CONFLICT, "Conflict",
                "The request conflicts with a concurrent change or existing data; please retry");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail notFound(ResourceNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Not found", e.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail unauthorized() {
        return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", "Authentication failed");
    }

    /** Denials from @PreAuthorize land here rather than in the security filter. */
    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail forbidden(HttpServletRequest request) {
        events.publishEvent(SecurityAuditHandler.accessDenied(request));
        return problem(HttpStatus.FORBIDDEN, "Forbidden", "Access denied");
    }

    /** An unknown {@code sort} property surfaces as one of these, depending on the query type. */
    @ExceptionHandler({PropertyReferenceException.class, InvalidDataAccessApiUsageException.class})
    ProblemDetail badQuery(RuntimeException e) {
        boolean unknownProperty = e instanceof PropertyReferenceException || causedByUnknownPath(e);
        return unknownProperty
                ? problem(HttpStatus.BAD_REQUEST, "Bad request", "Invalid sort property")
                : unexpected(e);
    }

    private static boolean causedByUnknownPath(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof UnknownPathException) {
                return true;
            }
        }
        return false;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception e) {
        log.error("Unexpected error", e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", "Unexpected error");
    }

    // Validation failures list which fields failed (the messages come from our annotations).

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return validationProblem(e.getBindingResult().getAllErrors().stream()
                .map(error -> describe(error.getObjectName(), error))
                .toList());
    }

    /** Raised instead of the above when the method also has constrained parameters (e.g. a header). */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException e,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return validationProblem(e.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> describe(result.getMethodParameter().getParameterName(), error)))
                .toList());
    }

    private static String describe(String fallbackName, MessageSourceResolvable error) {
        String name = error instanceof FieldError field ? field.getField() : fallbackName;
        return name + ": " + error.getDefaultMessage();
    }

    private static ResponseEntity<Object> validationProblem(List<String> errors) {
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Invalid request", "Request validation failed");
        body.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
