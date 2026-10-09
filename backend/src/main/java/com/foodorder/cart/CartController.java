package com.foodorder.cart;

import com.foodorder.security.AppPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    @GetMapping
    public CartDtos.CartResponse get(@AuthenticationPrincipal AppPrincipal user) {
        return service.get(user.getId());
    }

    @PostMapping("/items")
    public CartDtos.CartResponse add(@AuthenticationPrincipal AppPrincipal user,
                                     @Valid @RequestBody CartDtos.AddItemRequest request) {
        return service.add(user.getId(), request);
    }

    @PutMapping("/items/{foodId}")
    public CartDtos.CartResponse update(@AuthenticationPrincipal AppPrincipal user, @PathVariable long foodId,
                                        @RequestParam @Min(0) @Max(20) int quantity) {
        return service.setQuantity(user.getId(), foodId, quantity);
    }

    @DeleteMapping("/items/{foodId}")
    public CartDtos.CartResponse remove(@AuthenticationPrincipal AppPrincipal user, @PathVariable long foodId) {
        return service.remove(user.getId(), foodId);
    }
}
