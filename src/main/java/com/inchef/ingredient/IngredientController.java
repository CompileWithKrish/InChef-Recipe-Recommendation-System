package com.inchef.ingredient;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.inchef.common.ApiResponse;

@RestController
@RequestMapping("/api/v1/ingredients")
public class IngredientController {

    private final IngredientService ingredientService;

    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<IngredientResponse>> createIngredient(@Valid @RequestBody IngredientCreateRequest request) {
        IngredientResponse response = ingredientService.createIngredient(request);
        return new ResponseEntity<>(ApiResponse.created("Ingredient created successfully", response), HttpStatus.CREATED);
    }

    @GetMapping("/autocomplete")
    public ResponseEntity<ApiResponse<List<IngredientResponse>>> autocomplete(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        List<IngredientResponse> list = ingredientService.autocompleteIngredients(query, limit);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<IngredientResponse>>> getAllIngredients(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer limit) {
        List<IngredientResponse> list = ingredientService.getAllIngredients(query, category, limit);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<IngredientResponse>> getIngredientById(@PathVariable Long id) {
        IngredientResponse ingredient = ingredientService.getIngredientById(id);
        return ResponseEntity.ok(ApiResponse.ok(ingredient));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteIngredient(@PathVariable Long id) {
        ingredientService.deleteIngredient(id);
        return ResponseEntity.ok(ApiResponse.ok("Ingredient deleted successfully", null));
    }
}
