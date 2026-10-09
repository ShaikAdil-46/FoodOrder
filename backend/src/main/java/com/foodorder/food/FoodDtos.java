package com.foodorder.food;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

public final class FoodDtos {
    private FoodDtos() {}

    public record FoodResponse(Long id, String name, String description, String category,
                               BigDecimal price, String imageUrl, boolean available,
                               boolean vegetarian, Instant createdAt) {
        public static FoodResponse from(FoodItem item) {
            return new FoodResponse(item.getId(), item.getName(), item.getDescription(), item.getCategory(),
                    item.getPrice(), item.getImageUrl(), item.isAvailable(), item.isVegetarian(), item.getCreatedAt());
        }
    }

    public record FoodRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 500) String description,
            @NotBlank @Size(max = 40) String category,
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 8, fraction = 2) BigDecimal price,
            @NotBlank @Size(max = 500) String imageUrl,
            boolean available,
            boolean vegetarian) {}

    public record FoodPage(java.util.List<FoodResponse> items, int page, int size, long totalItems, int totalPages) {}
}
