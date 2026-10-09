package com.foodorder.order;

import com.foodorder.cart.CartRepository;
import com.foodorder.security.AppPrincipal;
import com.foodorder.user.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orders;
    private final CartRepository carts;
    private final UserRepository users;

    public OrderService(OrderRepository orders, CartRepository carts, UserRepository users) {
        this.orders = orders;
        this.carts = carts;
        this.users = users;
    }

    public List<OrderDtos.OrderResponse> mine(Long userId) {
        return orders.findAllByUser_IdOrderByCreatedAtDesc(userId).stream().map(OrderDtos.OrderResponse::from).toList();
    }

    public List<OrderDtos.OrderResponse> all() {
        return orders.findAllByOrderByCreatedAtDesc().stream().map(OrderDtos.OrderResponse::from).toList();
    }

    @Transactional
    public OrderDtos.OrderResponse checkout(AppPrincipal principal, String address) {
        var cart = carts.findAllByUser_IdOrderByIdAsc(principal.getId());
        if (cart.isEmpty()) throw new IllegalArgumentException("Your cart is empty.");
        if (cart.stream().anyMatch(item -> !item.getFoodItem().isAvailable())) {
            throw new IllegalArgumentException("Remove unavailable items before checking out.");
        }
        BigDecimal total = cart.stream().map(item -> item.getFoodItem().getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        var order = new CustomerOrder(users.findById(principal.getId()).orElseThrow(), total, address.trim());
        cart.forEach(item -> order.addItem(new OrderItem(item.getFoodItem().getName(),
                item.getFoodItem().getPrice(), item.getQuantity())));
        var saved = orders.save(order);
        carts.deleteAll(cart);
        return OrderDtos.OrderResponse.from(saved);
    }

    @Transactional
    public OrderDtos.OrderResponse updateStatus(long id, OrderStatus status) {
        var order = orders.findById(id)
                .orElseThrow(() -> new com.foodorder.common.ResourceNotFoundException("Order not found"));
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Completed orders cannot be changed.");
        }
        boolean canCancel = status == OrderStatus.CANCELLED
                && (order.getStatus() == OrderStatus.PLACED || order.getStatus() == OrderStatus.CONFIRMED
                || order.getStatus() == OrderStatus.PREPARING);
        boolean canAdvance = switch (order.getStatus()) {
            case PLACED -> status == OrderStatus.CONFIRMED;
            case CONFIRMED -> status == OrderStatus.PREPARING;
            case PREPARING -> status == OrderStatus.OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> status == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
        if (!canAdvance && !canCancel) {
            throw new IllegalArgumentException("Choose the next order status or cancel an order before delivery.");
        }
        order.setStatus(status);
        return OrderDtos.OrderResponse.from(order);
    }
}
