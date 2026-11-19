package com.sentineltrade.api;

import com.sentineltrade.api.dto.AuditLogResponse;
import com.sentineltrade.infrastructure.persistence.AuditLogRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-logs")
@Tag(name = "Audit")
public class AuditLogController {

    private final AuditLogRepository auditLogs;

    public AuditLogController(AuditLogRepository auditLogs) {
        this.auditLogs = auditLogs;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AUDITOR', 'ADMIN')")
    public Page<AuditLogResponse> list(
            @ParameterObject @PageableDefault(size = 50, sort = "id", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return auditLogs.findAll(pageable).map(AuditLogResponse::from);
    }
}
