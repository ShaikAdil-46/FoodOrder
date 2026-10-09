package com.foodorder.cart;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findAllByUser_IdOrderByIdAsc(Long userId);
    Optional<CartItem> findByUser_IdAndFoodItem_Id(Long userId, Long foodId);
    void deleteAllByUser_Id(Long userId);
}
