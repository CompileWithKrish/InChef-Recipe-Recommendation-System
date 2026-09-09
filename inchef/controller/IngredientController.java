package com.inchef.controller;

import com.inchef.dto.request.IngredientCreateRequest;
import com.inchef.dto.response.ApiResponse;
import com.inchef.dto.response.IngredientResponse;
import com.inchef.service.IngredientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping
    public ResponseEntity<ApiResponse<List<IngredientResponse>>> getAllIngredients(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category) {
        List<IngredientResponse> list = ingredientService.getAllIngredients(query, category);
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
