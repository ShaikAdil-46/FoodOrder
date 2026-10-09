package com.foodorder.food;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "food_items", indexes = {
        @Index(name = "idx_food_category_available", columnList = "category, available"),
        @Index(name = "idx_food_name", columnList = "name")
})
public class FoodItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, length = 40)
    private String category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private boolean available = true;

    @Column(nullable = false)
    private boolean vegetarian;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected FoodItem() {}

    public FoodItem(String name, String description, String category, BigDecimal price,
                    String imageUrl, boolean available, boolean vegetarian) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.imageUrl = imageUrl;
        this.available = available;
        this.vegetarian = vegetarian;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public BigDecimal getPrice() { return price; }
    public String getImageUrl() { return imageUrl; }
    public boolean isAvailable() { return available; }
    public boolean isVegetarian() { return vegetarian; }
    public Instant getCreatedAt() { return createdAt; }
    public void update(String name, String description, String category, BigDecimal price,
                       String imageUrl, boolean available, boolean vegetarian) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.imageUrl = imageUrl;
        this.available = available;
        this.vegetarian = vegetarian;
    }
}
