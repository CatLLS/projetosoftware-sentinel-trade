package com.sentineltrade.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sentineltrade.api.dto.AmendOrderRequest;
import com.sentineltrade.api.dto.AuditLogResponse;
import com.sentineltrade.api.dto.LoginRequest;
import com.sentineltrade.api.dto.MfaRequest;
import com.sentineltrade.api.dto.OrderResponse;
import com.sentineltrade.api.dto.PlaceOrderRequest;
import com.sentineltrade.api.dto.TokenResponse;
import com.sentineltrade.application.ResourceNotFoundException;
import com.sentineltrade.application.audit.AuditEvent;
import com.sentineltrade.application.auth.AuthService;
import com.sentineltrade.application.order.OrderService;
import com.sentineltrade.domain.audit.AuditLog;
import com.sentineltrade.domain.market.UnknownAssetException;
import com.sentineltrade.domain.order.DuplicateOrderException;
import com.sentineltrade.domain.order.InvalidOrderStateException;
import com.sentineltrade.domain.order.LimitOrder;
import com.sentineltrade.domain.order.OrderSide;
import com.sentineltrade.domain.order.OrderStatus;
import com.sentineltrade.domain.order.OrderType;
import com.sentineltrade.domain.risk.InsufficientBalanceException;
import com.sentineltrade.infrastructure.persistence.AuditLogRepository;
import com.sentineltrade.infrastructure.security.TokenService;
import com.sentineltrade.infrastructure.security.TokenService.Token;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.hibernate.query.sqm.UnknownPathException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.util.TypeInformation;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ApiControllersAndExceptionHandlerTest {

    // --- Controllers & DTOs --------------------------------------------------

    @Test
    void authControllerDelegatesLoginAndMfaVerification() {
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService);

        when(authService.login("trader", "secret"))
                .thenReturn(new Token("pre-jwt", 300, TokenService.PRE_AUTH));
        TokenResponse loginRes = controller.login(new LoginRequest("trader", "secret"));
        assertThat(loginRes.accessToken()).isEqualTo("pre-jwt");
        assertThat(loginRes.tokenType()).isEqualTo("Bearer");
        assertThat(loginRes.expiresIn()).isEqualTo(300L);
        assertThat(loginRes.stage()).isEqualTo(TokenService.PRE_AUTH);

        when(authService.verifyMfa(7L, "123456"))
                .thenReturn(new Token("final-jwt", 900, TokenService.MFA_VERIFIED));
        Authentication auth = new TestingAuthenticationToken("7", null, TokenService.PRE_AUTH);
        TokenResponse mfaRes = controller.verifyMfa(auth, new MfaRequest("123456"));
        assertThat(mfaRes.accessToken()).isEqualTo("final-jwt");
        assertThat(mfaRes.stage()).isEqualTo(TokenService.MFA_VERIFIED);
    }

    @Test
    void orderControllerHandlesPlaceReplayListGetAmendAndCancel() {
        OrderService orderService = mock(OrderService.class);
        OrderController controller = new OrderController(orderService);

        Authentication traderAuth = new TestingAuthenticationToken("7", null, "ROLE_TRADER", "MFA_VERIFIED");
        Authentication auditorAuth = new TestingAuthenticationToken("8", null, "ROLE_AUDITOR", "MFA_VERIFIED");

        LimitOrder order = new LimitOrder("key-12345678", 10L, "PETR4", OrderSide.BUY,
                new BigDecimal("100"), new BigDecimal("35.00"));
        ReflectionTestUtils.setField(order, "id", 1L);

        PlaceOrderRequest req = new PlaceOrderRequest(OrderType.LIMIT, "PETR4", OrderSide.BUY,
                new BigDecimal("100"), new BigDecimal("35.00"));
        assertThat(req.isLimitPriceConsistent()).isTrue();
        assertThat(new PlaceOrderRequest(OrderType.MARKET, "PETR4", OrderSide.BUY,
                new BigDecimal("100"), new BigDecimal("35.00")).isLimitPriceConsistent()).isFalse();
        assertThat(new PlaceOrderRequest(OrderType.LIMIT, "PETR4", OrderSide.BUY,
                new BigDecimal("100"), null).isLimitPriceConsistent()).isFalse();

        // 201 Created on new placement
        when(orderService.place(7L, req.toCommand("key-12345678")))
                .thenReturn(new OrderService.Placement(order, true));
        ResponseEntity<OrderResponse> created = controller.place(traderAuth, "key-12345678", req);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getHeaders().getLocation()).hasToString("/api/v1/orders/1");
        assertThat(created.getBody().id()).isEqualTo(1L);

        // 200 OK on idempotent replay
        when(orderService.place(7L, req.toCommand("key-12345678")))
                .thenReturn(new OrderService.Placement(order, false));
        ResponseEntity<OrderResponse> replayed = controller.place(traderAuth, "key-12345678", req);
        assertThat(replayed.getStatusCode()).isEqualTo(HttpStatus.OK);

        // List & Get with trader (readAll = false) and auditor (readAll = true)
        PageRequest pageReq = PageRequest.of(0, 20);
        when(orderService.list(7L, false, OrderStatus.CREATED, "PETR4", pageReq))
                .thenReturn(new PageImpl<>(List.of(order)));
        Page<OrderResponse> page = controller.list(traderAuth, OrderStatus.CREATED, "PETR4", pageReq);
        assertThat(page.getContent()).hasSize(1);

        when(orderService.get(8L, true, 1L)).thenReturn(order);
        assertThat(controller.get(auditorAuth, 1L).id()).isEqualTo(1L);

        // Amend & Cancel
        when(orderService.amend(7L, 1L, new BigDecimal("36.00"))).thenReturn(order);
        assertThat(controller.amend(traderAuth, 1L, new AmendOrderRequest(new BigDecimal("36.00"))).id()).isEqualTo(1L);

        when(orderService.cancel(7L, 1L)).thenReturn(order);
        assertThat(controller.cancel(traderAuth, 1L).id()).isEqualTo(1L);
    }

    @Test
    void auditLogControllerMapsRepositoryPageToDto() {
        AuditLogRepository repo = mock(AuditLogRepository.class);
        AuditLogController controller = new AuditLogController(repo);
        AuditLog log = new AuditLog("ORDER_CREATED", 7L, "Order", "1", null, "CREATED", Map.of(), "127.0.0.1");
        ReflectionTestUtils.setField(log, "id", 500L);
        PageRequest pageReq = PageRequest.of(0, 50);
        when(repo.findAll(pageReq)).thenReturn(new PageImpl<>(List.of(log)));

        Page<AuditLogResponse> result = controller.list(pageReq);

        assertThat(result.getContent()).singleElement().satisfies(dto -> {
            assertThat(dto.id()).isEqualTo(500L);
            assertThat(dto.eventType()).isEqualTo("ORDER_CREATED");
            assertThat(dto.actorId()).isEqualTo(7L);
            assertThat(dto.newState()).isEqualTo("CREATED");
        });
    }

    // --- GlobalExceptionHandler (<<no down-flow>>) ---------------------------

    @Test
    void globalExceptionHandlerMapsEveryDomainAndInfrastructureExceptionToRfc7807() {
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        GlobalExceptionHandler handler = new GlobalExceptionHandler(events);

        ProblemDetail p422Risk = handler.orderRejected(new InsufficientBalanceException());
        assertThat(p422Risk.getStatus()).isEqualTo(422);
        assertThat(p422Risk.getTitle()).isEqualTo("Order rejected");

        ProblemDetail p422Asset = handler.orderRejected(new UnknownAssetException());
        assertThat(p422Asset.getStatus()).isEqualTo(422);

        ProblemDetail p409State = handler.conflict(new InvalidOrderStateException("Invalid state"));
        assertThat(p409State.getStatus()).isEqualTo(409);

        ProblemDetail p409Dup = handler.conflict(new DuplicateOrderException());
        assertThat(p409Dup.getStatus()).isEqualTo(409);

        ProblemDetail p409Lock = handler.concurrentChange();
        assertThat(p409Lock.getStatus()).isEqualTo(409);
        assertThat(p409Lock.getDetail()).contains("concurrent change");

        ProblemDetail p404 = handler.notFound(new ResourceNotFoundException("Order"));
        assertThat(p404.getStatus()).isEqualTo(404);
        assertThat(p404.getDetail()).isEqualTo("Order not found");

        ProblemDetail p401 = handler.unauthorized();
        assertThat(p401.getStatus()).isEqualTo(401);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/audit-logs");
        ProblemDetail p403 = handler.forbidden(req);
        assertThat(p403.getStatus()).isEqualTo(403);
        verify(events).publishEvent(org.mockito.ArgumentMatchers.any(AuditEvent.class));

        ProblemDetail p400Sort = handler.badQuery(
                new InvalidDataAccessApiUsageException("bad sort", new UnknownPathException("bad path")));
        assertThat(p400Sort.getStatus()).isEqualTo(400);
        assertThat(p400Sort.getDetail()).isEqualTo("Invalid sort property");

        PropertyReferenceException propEx = new PropertyReferenceException(
                "unknownField", TypeInformation.of(LimitOrder.class), List.of());
        ProblemDetail p400Prop = handler.badQuery(propEx);
        assertThat(p400Prop.getStatus()).isEqualTo(400);
        assertThat(p400Prop.getDetail()).isEqualTo("Invalid sort property");

        ProblemDetail p500FromDataAccess = handler.badQuery(new InvalidDataAccessApiUsageException("other db error"));
        assertThat(p500FromDataAccess.getStatus()).isEqualTo(500);
        assertThat(p500FromDataAccess.getDetail()).isEqualTo("Unexpected error");

        ProblemDetail p500 = handler.unexpected(new RuntimeException("secret SQL details"));
        assertThat(p500.getStatus()).isEqualTo(500);
        assertThat(p500.getDetail()).isEqualTo("Unexpected error");
    }

    @Test
    void standaloneMockMvcValidatesBeanConstraintsAndRendersProblemDetailWithoutStacktrace() throws Exception {
        AuthService authService = mock(AuthService.class);
        OrderService orderService = mock(OrderService.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);

        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService), new OrderController(orderService))
                .setControllerAdvice(new GlobalExceptionHandler(events))
                .build();

        // Blank username/password triggers MethodArgumentNotValidException -> 400 ProblemDetail
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.trace").doesNotExist());

        // Invalid X-Idempotency-Key and malformed assetSymbol trigger HandlerMethodValidationException -> 400
        mvc.perform(post("/api/v1/orders")
                        .header("X-Idempotency-Key", "short")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"MARKET\",\"assetSymbol\":\"bad;sql\",\"side\":\"BUY\",\"quantity\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
