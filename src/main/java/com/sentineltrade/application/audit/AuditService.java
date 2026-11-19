package com.sentineltrade.application.audit;

import com.sentineltrade.domain.audit.AuditLog;
import com.sentineltrade.infrastructure.persistence.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * <<audit>> / <<fair exchange>>: the listener runs synchronously and joins the caller's
 * transaction, so an order change and its audit record commit (or roll back) together.
 * Events published outside a transaction get their own.
 */
@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @EventListener
    @Transactional
    public void record(AuditEvent event) {
        Long actor = event.actorId() != null ? event.actorId() : currentUserId();
        repository.save(new AuditLog(event.type(), actor, event.entityName(), event.entityId(),
                event.previousState(), event.newState(), event.metadata(), clientIp()));
    }

    /** Authenticated user, or {@code null} for system jobs such as the reconciler. */
    private static Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return Long.valueOf(jwt.getSubject());
        }
        return null;
    }

    private static String clientIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            return request.getRemoteAddr();
        }
        return null;
    }
}
