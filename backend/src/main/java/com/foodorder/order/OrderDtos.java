package com.foodorder.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {
    private OrderDtos() {}
    public record CheckoutRequest(@NotBlank @Size(max = 300) String deliveryAddress) {}
    public record StatusRequest(@NotNull OrderStatus status) {}
    public record OrderLine(String name, BigDecimal unitPrice, int quantity) {}
    public record OrderResponse(Long id, String customerName, String customerEmail, OrderStatus status,
                                BigDecimal totalAmount, String deliveryAddress, Instant createdAt,
                                List<OrderLine> items) {
        public static OrderResponse from(CustomerOrder order) {
            return new OrderResponse(order.getId(), order.getUser().getFirstName() + " " + order.getUser().getLastName(),
                    order.getUser().getEmail(), order.getStatus(), order.getTotalAmount(), order.getDeliveryAddress(),
                    order.getCreatedAt(), order.getItems().stream()
                    .map(item -> new OrderLine(item.getFoodName(), item.getUnitPrice(), item.getQuantity())).toList());
        }
    }
}
