package com.inchef.recipeimage;

import java.util.List;
import java.util.Optional;

public interface ImageSearchProvider {

    /**
     * Name identifier for the provider (e.g. "GoogleCustomSearch").
     */
    String getProviderName();

    /**
     * Indicates whether this provider has valid API credentials configured.
     */
    boolean isAvailable();

    /**
     * Searches for a high-confidence authentic image of the given recipe.
     *
     * @param recipeTitle     Exact recipe name
     * @param cuisine         Recipe cuisine (e.g. "North Indian", "Bengali")
     * @param mainIngredients Top distinctive ingredients
     * @return Validated direct image URL, or empty if not found / ambiguous
     */
    Optional<String> searchImage(String recipeTitle, String cuisine, List<String> mainIngredients);
}
