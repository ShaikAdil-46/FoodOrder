package com.foodorder.food;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FoodRepository extends JpaRepository<FoodItem, Long> {
    @Query("""
            select f from FoodItem f
            where (:available is null or f.available = :available)
              and (:category is null or lower(f.category) = lower(:category))
              and (:search is null or lower(f.name) like lower(concat('%', :search, '%'))
                   or lower(f.description) like lower(concat('%', :search, '%')))
            """)
    Page<FoodItem> search(@Param("search") String search,
                          @Param("category") String category,
                          @Param("available") Boolean available,
                          Pageable pageable);
}
