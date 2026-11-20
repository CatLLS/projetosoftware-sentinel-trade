package com.sentineltrade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sentineltrade.domain.account.Account;
import com.sentineltrade.domain.audit.AuditLog;
import com.sentineltrade.domain.order.OrderStatus;
import com.sentineltrade.infrastructure.persistence.AccountRepository;
import com.sentineltrade.infrastructure.persistence.AuditLogRepository;
import com.sentineltrade.infrastructure.persistence.OrderRepository;
import com.sentineltrade.infrastructure.persistence.PositionRepository;
import com.sentineltrade.infrastructure.persistence.UserRepository;
import com.sentineltrade.infrastructure.security.TotpMfaVerifier;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Full HTTP flow against a real MySQL (Testcontainers): MFA login, placement, idempotency,
 * amendment, cancellation with release of the reservation, rejections and audit trail.
 * Skipped when Docker is not available.
 */
@SpringBootTest(properties = {
        "app.security.jwt-secret=integration-test-jwt-secret-of-32-bytes-or-more",
        "app.security.mfa-encryption-key=integration-test-encryption-key",
        "app.seed.enabled=true",
        "app.seed.password=" + OrderFlowIntegrationTest.PASSWORD,
        "app.seed.mfa-secret=" + OrderFlowIntegrationTest.MFA_SECRET,
        "app.market.volatility=0",
        "app.exchange.timeout-rate=0",
        "app.reconciler.interval=PT1H"})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class OrderFlowIntegrationTest {

    static final String PASSWORD = "Integration-Test-1";
    static final String MFA_SECRET = "JBSWY3DPEHPK3PXP";

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withCommand("--log-bin-trust-function-creators=1");

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired AccountRepository accounts;
    @Autowired PositionRepository positions;
    @Autowired OrderRepository orders;
    @Autowired AuditLogRepository auditLogs;

    // --- Authentication ------------------------------------------------------

    @Test
    void ordersRequireAVerifiedSecondFactor() throws Exception {
        String preAuth = login("trader");
        int deniedBefore = auditLogs.findByEventType("ACCESS_DENIED").size();

        mvc.perform(get("/api/v1/orders").header("Authorization", "Bearer " + preAuth))
                .andExpect(status().isForbidden());

        assertThat(auditLogs.findByEventType("ACCESS_DENIED")).hasSize(deniedBefore + 1);
    }

    @Test
    void wrongMfaCodeIsRefusedAndAudited() throws Exception {
        int failedBefore = auditLogs.findByEventType("MFA_FAILED").size();

        mvc.perform(post("/api/v1/auth/mfa/verify")
                        .header("Authorization", "Bearer " + login("trader"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"000000\"}"))
                .andExpect(status().isUnauthorized());

        assertThat(auditLogs.findByEventType("MFA_FAILED")).hasSize(failedBefore + 1);
    }

    // --- Orders --------------------------------------------------------------

    @Test
    void marketBuyExecutesSettlesAndIsAudited() throws Exception {
        String token = token("trader");
        BigDecimal availableBefore = traderAccount().getAvailableBalance();
        BigDecimal petr4Before = petr4Quantity();

        String body = placeOrder(token, newKey(), "{\"type\":\"MARKET\",\"assetSymbol\":\"PETR4\",\"side\":\"BUY\",\"quantity\":10}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("EXECUTED"))
                .andReturn().getResponse().getContentAsString();

        assertThat(traderAccount().getAvailableBalance()).isEqualByComparingTo(availableBefore.subtract(new BigDecimal("375.00")));
        assertThat(petr4Quantity()).isEqualByComparingTo(petr4Before.add(BigDecimal.TEN));
        assertThat(auditLogs.findByEntityNameAndEntityIdOrderByIdAsc("Order", id(body)))
                .extracting(AuditLog::getNewState)
                .containsExactly("CREATED", "VALIDATED", "SENT", "OPEN", "EXECUTED");

        mvc.perform(get("/api/v1/orders/" + id(body)).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"));

        mvc.perform(get("/api/v1/orders").param("assetSymbol", "PETR4").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void sameIdempotencyKeyNeverCreatesASecondOrder() throws Exception {
        String token = token("trader");
        String key = newKey();
        String order = "{\"type\":\"MARKET\",\"assetSymbol\":\"PETR4\",\"side\":\"BUY\",\"quantity\":1}";

        String first = placeOrder(token, key, order).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        BigDecimal availableAfterFirst = traderAccount().getAvailableBalance();
        String second = placeOrder(token, key, order).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(id(second)).isEqualTo(id(first));
        assertThat(traderAccount().getAvailableBalance()).isEqualByComparingTo(availableAfterFirst);
        placeOrder(token, key, order.replace("\"quantity\":1", "\"quantity\":2")).andExpect(status().isConflict());
    }

    @Test
    void limitOrderCanBeAmendedAndCancelledReleasingTheReservation() throws Exception {
        String token = token("trader");
        BigDecimal reservedBefore = traderAccount().getReservedBalance();

        // 32.00 is below the 37.50 quote, so the buy order rests on the book
        String id = id(placeOrder(token, newKey(), "{\"type\":\"LIMIT\",\"assetSymbol\":\"PETR4\",\"side\":\"BUY\",\"quantity\":100,\"limitPrice\":32.00}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString());
        assertThat(traderAccount().getReservedBalance()).isEqualByComparingTo(reservedBefore.add(new BigDecimal("3200.00")));

        mvc.perform(patch("/api/v1/orders/" + id).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"limitPrice\":33.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(33.0));
        assertThat(traderAccount().getReservedBalance()).isEqualByComparingTo(reservedBefore.add(new BigDecimal("3300.00")));

        mvc.perform(post("/api/v1/orders/" + id + "/cancel").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(traderAccount().getReservedBalance()).isEqualByComparingTo(reservedBefore);
    }

    @Test
    void orderAboveTheRiskLimitIsRejectedAndAudited() throws Exception {
        String key = newKey();

        placeOrder(token("trader"), key, "{\"type\":\"MARKET\",\"assetSymbol\":\"PETR4\",\"side\":\"BUY\",\"quantity\":5000}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Order rejected"));

        var rejected = orders.findByIdempotencyKey(key).orElseThrow();
        assertThat(rejected.getStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(auditLogs.findByEntityNameAndEntityIdOrderByIdAsc("Order", rejected.getId().toString()))
                .extracting(AuditLog::getNewState)
                .containsExactly("CREATED", "REJECTED");
    }

    @Test
    void sellWithoutPositionIsRejected() throws Exception {
        placeOrder(token("trader"), newKey(), "{\"type\":\"MARKET\",\"assetSymbol\":\"ITUB4\",\"side\":\"SELL\",\"quantity\":10}")
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void invalidInputIsRejectedWithoutLeakingInternals() throws Exception {
        placeOrder(token("trader"), newKey(), "{\"type\":\"LIMIT\",\"assetSymbol\":\"petr4; drop\",\"side\":\"BUY\",\"quantity\":-1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    // --- Role-based access ---------------------------------------------------

    @Test
    void onlyAuditorsAndAdminsReadTheAuditTrail() throws Exception {
        mvc.perform(get("/api/v1/audit-logs").header("Authorization", "Bearer " + token("trader")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/audit-logs").header("Authorization", "Bearer " + token("auditor")))
                .andExpect(status().isOk());
    }

    @Test
    void auditorsCannotPlaceOrders() throws Exception {
        placeOrder(token("auditor"), newKey(), "{\"type\":\"MARKET\",\"assetSymbol\":\"PETR4\",\"side\":\"BUY\",\"quantity\":1}")
                .andExpect(status().isForbidden());
    }

    // --- Helpers -------------------------------------------------------------

    private String login(String username) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private String token(String username) throws Exception {
        String code = TotpMfaVerifier.generate(MFA_SECRET, Instant.now());
        String body = mvc.perform(post("/api/v1/auth/mfa/verify")
                        .header("Authorization", "Bearer " + login(username))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private ResultActions placeOrder(String token, String key, String json) throws Exception {
        return mvc.perform(post("/api/v1/orders")
                .header("Authorization", "Bearer " + token)
                .header("X-Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private Account traderAccount() {
        Long traderId = users.findByUsername("trader").orElseThrow().getId();
        return accounts.findByUserId(traderId).orElseThrow();
    }

    private BigDecimal petr4Quantity() {
        return positions.findByAccountIdAndAssetSymbol(traderAccount().getId(), "PETR4").orElseThrow().getQuantity();
    }

    private static String id(String orderJson) {
        return JsonPath.read(orderJson, "$.id").toString();
    }

    private static String newKey() {
        return UUID.randomUUID().toString();
    }
}
