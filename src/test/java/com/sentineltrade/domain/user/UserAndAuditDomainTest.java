package com.sentineltrade.domain.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.sentineltrade.domain.audit.AuditLog;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UserAndAuditDomainTest {

    @Test
    void userStoresHashedPasswordEncryptedSecretAndRole() {
        User user = new User("trader", "$2a$10$hash", "JBSWY3DPEHPK3PXP", Role.TRADER);

        assertThat(user.getUsername()).isEqualTo("trader");
        assertThat(user.getPasswordHash()).isEqualTo("$2a$10$hash");
        assertThat(user.getMfaSecret()).isEqualTo("JBSWY3DPEHPK3PXP");
        assertThat(user.getRole()).isEqualTo(Role.TRADER);
        assertThat(user.isActive()).isTrue();
    }

    @Test
    void rolesMapToSpringSecurityAuthorities() {
        assertThat(Role.TRADER.authority()).isEqualTo("ROLE_TRADER");
        assertThat(Role.AUDITOR.authority()).isEqualTo("ROLE_AUDITOR");
        assertThat(Role.ADMIN.authority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void auditLogStoresAllImmutableFields() {
        Map<String, Object> metadata = Map.of("side", "BUY", "assetSymbol", "PETR4");
        AuditLog log = new AuditLog("ORDER_CREATED", 7L, "Order", "101",
                null, "CREATED", metadata, "127.0.0.1");

        assertThat(log.getEventType()).isEqualTo("ORDER_CREATED");
        assertThat(log.getActorId()).isEqualTo(7L);
        assertThat(log.getEntityName()).isEqualTo("Order");
        assertThat(log.getEntityId()).isEqualTo("101");
        assertThat(log.getPreviousState()).isNull();
        assertThat(log.getNewState()).isEqualTo("CREATED");
        assertThat(log.getMetadata()).containsEntry("assetSymbol", "PETR4");
        assertThat(log.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(log.getCreatedAt()).isNotNull();
    }
}
