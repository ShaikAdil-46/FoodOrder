package com.foodorder.food;

import com.foodorder.common.ResourceNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FoodService {
    private final FoodRepository foods;

    public FoodService(FoodRepository foods) {
        this.foods = foods;
    }

    public FoodDtos.FoodPage search(String search, String category, Boolean available, int page, int size) {
        var result = foods.search(blankToNull(search), blankToNull(category), available,
                PageRequest.of(page, size, Sort.by("name").ascending()));
        return new FoodDtos.FoodPage(result.getContent().stream().map(FoodDtos.FoodResponse::from).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    public FoodDtos.FoodResponse get(long id) {
        return FoodDtos.FoodResponse.from(requireFood(id));
    }

    @Transactional
    public FoodDtos.FoodResponse create(FoodDtos.FoodRequest request) {
        var item = new FoodItem(request.name().trim(), request.description().trim(), request.category().trim(),
                request.price(), request.imageUrl().trim(), request.available(), request.vegetarian());
        return FoodDtos.FoodResponse.from(foods.save(item));
    }

    @Transactional
    public FoodDtos.FoodResponse update(long id, FoodDtos.FoodRequest request) {
        var item = requireFood(id);
        item.update(request.name().trim(), request.description().trim(), request.category().trim(),
                request.price(), request.imageUrl().trim(), request.available(), request.vegetarian());
        return FoodDtos.FoodResponse.from(item);
    }

    @Transactional
    public void delete(long id) {
        foods.delete(requireFood(id));
    }

    public FoodItem requireFood(long id) {
        return foods.findById(id).orElseThrow(() -> new ResourceNotFoundException("Food item not found"));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
