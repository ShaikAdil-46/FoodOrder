package com.foodorder.cart;

import com.foodorder.food.FoodItem;
import com.foodorder.user.UserEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "cart_items", uniqueConstraints =
        @UniqueConstraint(name = "uk_cart_user_food", columnNames = {"user_id", "food_id"}))
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "food_id", nullable = false)
    private FoodItem foodItem;

    @Column(nullable = false)
    private int quantity;

    protected CartItem() {}

    public CartItem(UserEntity user, FoodItem foodItem, int quantity) {
        this.user = user;
        this.foodItem = foodItem;
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public FoodItem getFoodItem() { return foodItem; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
