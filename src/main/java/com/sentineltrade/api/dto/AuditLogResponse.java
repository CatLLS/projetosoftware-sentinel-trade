package com.sentineltrade.api.dto;

import com.sentineltrade.domain.audit.AuditLog;
import java.time.Instant;
import java.util.Map;

public record AuditLogResponse(Long id, String eventType, Long actorId, String entityName, String entityId,
                               String previousState, String newState, Map<String, Object> metadata,
                               String ipAddress, Instant createdAt) {

    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(log.getId(), log.getEventType(), log.getActorId(), log.getEntityName(),
                log.getEntityId(), log.getPreviousState(), log.getNewState(), log.getMetadata(),
                log.getIpAddress(), log.getCreatedAt());
    }
}
