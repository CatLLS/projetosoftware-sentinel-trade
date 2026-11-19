package com.sentineltrade.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentineltrade.application.audit.AuditEvent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Answers 401/403 from the security filters as RFC 7807 problems, and audits denied
 * accesses (e.g. a PRE_AUTH token trying to reach the orders API without MFA).
 */
@Component
public class SecurityAuditHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ApplicationEventPublisher events;
    private final ObjectMapper mapper;

    public SecurityAuditHandler(ApplicationEventPublisher events, ObjectMapper mapper) {
        this.events = events;
        this.mapper = mapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException e) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException e) throws IOException {
        events.publishEvent(accessDenied(request));
        write(response, HttpStatus.FORBIDDEN, "Access denied");
    }

    public static AuditEvent accessDenied(HttpServletRequest request) {
        return AuditEvent.security("ACCESS_DENIED", null,
                Map.of("method", request.getMethod(), "path", request.getRequestURI()));
    }

    private void write(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), ProblemDetail.forStatusAndDetail(status, detail));
    }
}
