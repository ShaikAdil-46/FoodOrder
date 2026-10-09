package com.foodorder.cart;

import java.math.BigDecimal;
import java.util.List;

public final class CartDtos {
    private CartDtos() {}
    public record CartLine(Long foodId, String name, String imageUrl, BigDecimal unitPrice,
                           int quantity, BigDecimal lineTotal, boolean available) {}
    public record CartResponse(List<CartLine> items, BigDecimal subtotal, int itemCount) {}
    public record AddItemRequest(Long foodId, int quantity) {}
}
