package com.sentineltrade.application.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.sentineltrade.domain.audit.AuditLog;
import com.sentineltrade.domain.order.LimitOrder;
import com.sentineltrade.domain.order.OrderSide;
import com.sentineltrade.domain.order.OrderStatus;
import com.sentineltrade.infrastructure.persistence.AuditLogRepository;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class AuditServiceTest {

    private final AuditLogRepository repository = mock(AuditLogRepository.class);
    private final AuditService auditService = new AuditService(repository);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void recordsOrderTransitionResolvingActorFromJwtAndIpFromRequest() {
        Jwt jwt = Jwt.withTokenValue("tok")
                .header("alg", "HS256")
                .subject("42")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.15");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        LimitOrder order = new LimitOrder("k1", 1L, "PETR4", OrderSide.BUY, BigDecimal.TEN, new BigDecimal("35.00"));
        ReflectionTestUtils.setField(order, "id", 99L);
        order.validate();

        auditService.record(AuditEvent.orderTransition(order, OrderStatus.CREATED));

        ArgumentCaptor<AuditLog> saved = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(saved.capture());
        AuditLog log = saved.getValue();
        assertThat(log.getEventType()).isEqualTo("ORDER_VALIDATED");
        assertThat(log.getActorId()).isEqualTo(42L);
        assertThat(log.getEntityName()).isEqualTo("Order");
        assertThat(log.getEntityId()).isEqualTo("99");
        assertThat(log.getPreviousState()).isEqualTo("CREATED");
        assertThat(log.getNewState()).isEqualTo("VALIDATED");
        assertThat(log.getIpAddress()).isEqualTo("10.0.0.15");
    }

    @Test
    void recordsSecurityAndSystemEventsWithoutActiveHttpRequest() {
        LimitOrder order = new LimitOrder("k1", 1L, "VALE3", OrderSide.SELL, BigDecimal.ONE, new BigDecimal("60.00"));
        ReflectionTestUtils.setField(order, "id", 55L);

        AuditEvent customOrderEvent = AuditEvent.order("ORDER_AMENDED", order, Map.of("newPrice", 61));
        auditService.record(customOrderEvent);

        AuditEvent securityEvent = AuditEvent.security("LOGIN_FAILED", 12L, Map.of("username", "trader"));
        auditService.record(securityEvent);

        ArgumentCaptor<AuditLog> saved = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues().get(0).getActorId()).isNull();
        assertThat(saved.getAllValues().get(0).getIpAddress()).isNull();
        assertThat(saved.getAllValues().get(1).getActorId()).isEqualTo(12L);
        assertThat(saved.getAllValues().get(1).getEntityName()).isEqualTo("User");
    }
}
