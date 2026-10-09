package com.foodorder.food;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/foods")
public class FoodController {
    private final FoodService service;

    public FoodController(FoodService service) {
        this.service = service;
    }

    @GetMapping
    public FoodDtos.FoodPage search(@RequestParam(required = false) String search,
                                    @RequestParam(required = false) String category,
                                    @RequestParam(required = false) Boolean available,
                                    @RequestParam(defaultValue = "0") @Min(0) int page,
                                    @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size) {
        return service.search(search, category, available, page, size);
    }

    @GetMapping("/{id}")
    public FoodDtos.FoodResponse get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public FoodDtos.FoodResponse create(@Valid @RequestBody FoodDtos.FoodRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public FoodDtos.FoodResponse update(@PathVariable long id, @Valid @RequestBody FoodDtos.FoodRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable long id) {
        service.delete(id);
    }
}
