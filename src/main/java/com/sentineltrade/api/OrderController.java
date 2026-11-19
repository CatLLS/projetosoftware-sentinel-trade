package com.sentineltrade.api;

import com.sentineltrade.api.dto.AmendOrderRequest;
import com.sentineltrade.api.dto.AssetSymbol;
import com.sentineltrade.api.dto.OrderResponse;
import com.sentineltrade.api.dto.PlaceOrderRequest;
import com.sentineltrade.application.order.OrderService;
import com.sentineltrade.domain.order.OrderStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Traders manage their own orders. Auditors and admins can read every order.
 * The whole path requires an MFA_VERIFIED token (see SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasRole('TRADER')")
    @Operation(summary = "Place an order (201 when created, 200 when replaying the same idempotency key)")
    public ResponseEntity<OrderResponse> place(
            Authentication authentication,
            @Parameter(description = "Unique key per order: a retry with the same key returns the original order")
            @RequestHeader("X-Idempotency-Key") @Pattern(regexp = "^[A-Za-z0-9-]{8,64}$") String idempotencyKey,
            @Valid @RequestBody PlaceOrderRequest request) {
        var placement = orderService.place(CurrentUser.of(authentication).id(), request.toCommand(idempotencyKey));
        var body = OrderResponse.from(placement.order());
        return placement.created()
                ? ResponseEntity.created(URI.create("/api/v1/orders/" + body.id())).body(body)
                : ResponseEntity.ok(body);
    }

    @GetMapping
    @Operation(summary = "List orders, optionally filtered by status and asset")
    public Page<OrderResponse> list(
            Authentication authentication,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @Pattern(regexp = AssetSymbol.PATTERN) String assetSymbol,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        var user = CurrentUser.of(authentication);
        return orderService.list(user.id(), user.canReadAll(), status, assetSymbol, pageable)
                .map(OrderResponse::from);
    }

    @GetMapping("/{id}")
    public OrderResponse get(Authentication authentication, @PathVariable Long id) {
        var user = CurrentUser.of(authentication);
        return OrderResponse.from(orderService.get(user.id(), user.canReadAll(), id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('TRADER')")
    @Operation(summary = "Change the price of an OPEN limit order without executions")
    public OrderResponse amend(Authentication authentication, @PathVariable Long id,
                               @Valid @RequestBody AmendOrderRequest request) {
        return OrderResponse.from(orderService.amend(CurrentUser.of(authentication).id(), id, request.limitPrice()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('TRADER')")
    @Operation(summary = "Request cancellation; the remaining reservation is released once the exchange confirms")
    public OrderResponse cancel(Authentication authentication, @PathVariable Long id) {
        return OrderResponse.from(orderService.cancel(CurrentUser.of(authentication).id(), id));
    }
}
