package com.foodorder.cart;

import com.foodorder.food.FoodService;
import com.foodorder.security.AppPrincipal;
import com.foodorder.user.UserRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CartService {
    private final CartRepository carts;
    private final UserRepository users;
    private final FoodService foods;

    public CartService(CartRepository carts, UserRepository users, FoodService foods) {
        this.carts = carts;
        this.users = users;
        this.foods = foods;
    }

    public CartDtos.CartResponse get(Long userId) {
        return response(userId);
    }

    @Transactional
    public CartDtos.CartResponse add(Long userId, CartDtos.AddItemRequest request) {
        if (request.foodId() == null || request.quantity() < 1 || request.quantity() > 20) {
            throw new IllegalArgumentException("Choose between 1 and 20 items.");
        }
        var food = foods.requireFood(request.foodId());
        if (!food.isAvailable()) throw new IllegalArgumentException("This food item is currently unavailable.");
        var item = carts.findByUser_IdAndFoodItem_Id(userId, food.getId())
                .orElseGet(() -> new CartItem(users.findById(userId).orElseThrow(), food, 0));
        int quantity = item.getQuantity() + request.quantity();
        if (quantity > 20) throw new IllegalArgumentException("You can add up to 20 of each item.");
        item.setQuantity(quantity);
        carts.save(item);
        return response(userId);
    }

    @Transactional
    public CartDtos.CartResponse setQuantity(Long userId, long foodId, int quantity) {
        var item = carts.findByUser_IdAndFoodItem_Id(userId, foodId)
                .orElseThrow(() -> new com.foodorder.common.ResourceNotFoundException("Cart item not found"));
        if (quantity < 0 || quantity > 20) throw new IllegalArgumentException("Quantity must be between 0 and 20.");
        if (quantity == 0) carts.delete(item);
        else item.setQuantity(quantity);
        return response(userId);
    }

    @Transactional
    public CartDtos.CartResponse remove(Long userId, long foodId) {
        carts.findByUser_IdAndFoodItem_Id(userId, foodId).ifPresent(carts::delete);
        return response(userId);
    }

    @Transactional
    public void clear(Long userId) {
        carts.deleteAllByUser_Id(userId);
    }

    private CartDtos.CartResponse response(Long userId) {
        var lines = carts.findAllByUser_IdOrderByIdAsc(userId).stream().map(item -> {
            var food = item.getFoodItem();
            var lineTotal = food.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            return new CartDtos.CartLine(food.getId(), food.getName(), food.getImageUrl(), food.getPrice(),
                    item.getQuantity(), lineTotal, food.isAvailable());
        }).toList();
        var subtotal = lines.stream().map(CartDtos.CartLine::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartDtos.CartResponse(lines, subtotal, lines.stream().mapToInt(CartDtos.CartLine::quantity).sum());
    }
}
