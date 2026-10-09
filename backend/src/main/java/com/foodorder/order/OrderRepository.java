package com.foodorder.order;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findAllByUser_IdOrderByCreatedAtDesc(Long userId);
    List<CustomerOrder> findAllByOrderByCreatedAtDesc();
}
