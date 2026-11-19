package com.sentineltrade.domain.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** <<audit>>: an immutable, append-only record of a security or business event. */
@Entity
@Immutable
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "entity_name")
    private String entityName;

    @Column(name = "entity_id")
    private String entityId;

    @Column(name = "previous_state")
    private String previousState;

    @Column(name = "new_state")
    private String newState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json")
    private Map<String, Object> metadata;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected AuditLog() {
    }

    public AuditLog(String eventType, Long actorId, String entityName, String entityId,
                    String previousState, String newState, Map<String, Object> metadata,
                    String ipAddress) {
        this.eventType = eventType;
        this.actorId = actorId;
        this.entityName = entityName;
        this.entityId = entityId;
        this.previousState = previousState;
        this.newState = newState;
        this.metadata = metadata;
        this.ipAddress = ipAddress;
    }

    public Long getId() { return id; }
    public String getEventType() { return eventType; }
    public Long getActorId() { return actorId; }
    public String getEntityName() { return entityName; }
    public String getEntityId() { return entityId; }
    public String getPreviousState() { return previousState; }
    public String getNewState() { return newState; }
    public Map<String, Object> getMetadata() { return metadata; }
    public String getIpAddress() { return ipAddress; }
    public Instant getCreatedAt() { return createdAt; }
}
