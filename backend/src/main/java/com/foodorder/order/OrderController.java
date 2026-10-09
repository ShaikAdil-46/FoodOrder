package com.foodorder.order;

import com.foodorder.security.AppPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    public OrderDtos.OrderResponse checkout(@AuthenticationPrincipal AppPrincipal user,
                                            @Valid @RequestBody OrderDtos.CheckoutRequest request) {
        return service.checkout(user, request.deliveryAddress());
    }

    @GetMapping
    public List<OrderDtos.OrderResponse> mine(@AuthenticationPrincipal AppPrincipal user) {
        return service.mine(user.getId());
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public List<OrderDtos.OrderResponse> all() {
        return service.all();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public OrderDtos.OrderResponse updateStatus(@PathVariable long id,
                                               @Valid @RequestBody OrderDtos.StatusRequest request) {
        return service.updateStatus(id, request.status());
    }
}
