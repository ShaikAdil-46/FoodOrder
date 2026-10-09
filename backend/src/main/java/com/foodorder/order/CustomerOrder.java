package com.foodorder.order;

import com.foodorder.user.UserEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_orders_status", columnList = "status")
})
public class CustomerOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status = OrderStatus.PLACED;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "delivery_address", nullable = false, length = 300)
    private String deliveryAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    protected CustomerOrder() {}

    public CustomerOrder(UserEntity user, BigDecimal totalAmount, String deliveryAddress) {
        this.user = user;
        this.totalAmount = totalAmount;
        this.deliveryAddress = deliveryAddress;
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public Long getId() { return id; }
    public UserEntity getUser() { return user; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public Instant getCreatedAt() { return createdAt; }
    public List<OrderItem> getItems() { return items; }
    public void setStatus(OrderStatus status) { this.status = status; this.updatedAt = Instant.now(); }
}
