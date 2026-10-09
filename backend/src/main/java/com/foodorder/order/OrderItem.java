package com.foodorder.order;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;

    @Column(name = "food_name", nullable = false, length = 100)
    private String foodName;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    protected OrderItem() {}

    public OrderItem(String foodName, BigDecimal unitPrice, int quantity) {
        this.foodName = foodName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public void setOrder(CustomerOrder order) { this.order = order; }
    public String getFoodName() { return foodName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
}
