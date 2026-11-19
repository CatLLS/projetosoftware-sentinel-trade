package com.sentineltrade.infrastructure.persistence;

import com.sentineltrade.domain.audit.AuditLog;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

/** <<audit>>: deliberately exposes no update or delete operation. */
public interface AuditLogRepository extends Repository<AuditLog, Long> {

    AuditLog save(AuditLog log);

    Page<AuditLog> findAll(Pageable pageable);

    List<AuditLog> findByEntityNameAndEntityIdOrderByIdAsc(String entityName, String entityId);

    List<AuditLog> findByEventType(String eventType);
}
