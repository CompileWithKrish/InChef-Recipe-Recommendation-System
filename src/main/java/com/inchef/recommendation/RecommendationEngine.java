package com.inchef.recommendation;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.regex.Pattern;

import com.inchef.preference.CookingSkillLevel;
import com.inchef.preference.DietaryType;
import com.inchef.recipe.DifficultyLevel;
import com.inchef.recipe.MealType;
import com.inchef.recipe.Recipe;
import com.inchef.recipe.RecipeDetailResponse;
import com.inchef.recipe.RecipeIngredientResponse;

@Component
public class RecommendationEngine {

    public static class PreferenceContext {
        public DietaryType dietaryType;
        public List<String> preferredCuisines = new ArrayList<>();
        public List<String> allergies = new ArrayList<>();
        public Integer maxCookingTime;
        public MealType mealType;
        public CookingSkillLevel cookingSkillLevel;
    }

    public RecommendationResponse evaluateRecipe(
            Recipe recipe,
            Set<String> normalizedUserIngredients,
            PreferenceContext preferences,
            RecipeDetailResponse recipeDetail) {

        List<RecipeIngredientResponse> recipeIngredients = recipeDetail.getIngredients() != null
                ? recipeDetail.getIngredients()
                : Collections.emptyList();

        // 1. Distinct canonical recipe ingredients
        Set<String> distinctRecipeIngredients = new LinkedHashSet<>();
        for (RecipeIngredientResponse rir : recipeIngredients) {
            if (rir.getIngredientName() != null && !rir.getIngredientName().isBlank()) {
                distinctRecipeIngredients.add(rir.getIngredientName().trim());
            }
        }

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (String ingName : distinctRecipeIngredients) {
            boolean isMatched = isIngredientMatched(ingName, normalizedUserIngredients);
            if (isMatched) {
                matched.add(ingName);
            } else {
                missing.add(ingName);
            }
        }

        int totalDistinct = Math.max(1, distinctRecipeIngredients.size());
        double ingredientRatio = (double) matched.size() / totalDistinct;
        double ingredientCoveragePercentage = Math.round(ingredientRatio * 100.0 * 10.0) / 10.0;
        double ingredientScore = Math.round(ingredientRatio * 60.0 * 10.0) / 10.0;

        // Dietary Score (Max 15)
        double dietaryScore = calculateDietaryScore(recipe.getDietaryType(), preferences.dietaryType);

        // Cuisine Score (Max 10)
        double cuisineScore = calculateCuisineScore(recipe.getCuisine(), preferences.preferredCuisines);

        // Cooking Time Score (Max 10)
        double cookingTimeScore = calculateTimeScore(recipe.getTotalTimeMinutes(), preferences.maxCookingTime);

        // Category / Meal Type Score (Max 5)
        double categoryScore = calculateMealTypeScore(recipe.getMealType(), preferences.mealType);

        double totalScore = ingredientScore + dietaryScore + cuisineScore + cookingTimeScore + categoryScore;
        totalScore = Math.min(100.0, Math.max(0.0, Math.round(totalScore * 10.0) / 10.0));

        ScoreBreakdown breakdown = ScoreBreakdown.builder()
                .ingredientScore(ingredientScore)
                .ingredientMatchPercentage(ingredientCoveragePercentage)
                .dietaryScore(dietaryScore)
                .cuisineScore(cuisineScore)
                .cookingTimeScore(cookingTimeScore)
                .categoryScore(categoryScore)
                .skillScore(0.0)
                .totalScore(totalScore)
                .build();

        String reason = generateReason(matched.size(), totalDistinct, recipe, preferences, totalScore);

        return RecommendationResponse.builder()
                .recipe(recipeDetail)
                .matchScore((int) Math.round(totalScore))
                .ingredientMatchPercentage(ingredientCoveragePercentage)
                .matchedIngredients(matched)
                .missingIngredients(missing)
                .reason(reason)
                .scoreBreakdown(breakdown)
                .build();
    }

    private static final Map<String, String> CULINARY_ALIASES = Map.ofEntries(
            Map.entry("scallion", "green onion"),
            Map.entry("scallions", "green onion"),
            Map.entry("spring onion", "green onion"),
            Map.entry("spring onions", "green onion"),
            Map.entry("hari mirch", "green chilli"),
            Map.entry("green chili", "green chilli"),
            Map.entry("green chillies", "green chilli"),
            Map.entry("red chili", "red chilli"),
            Map.entry("red chillies", "red chilli"),
            Map.entry("dhania", "coriander"),
            Map.entry("dhania leaves", "coriander"),
            Map.entry("coriander leaves", "coriander"),
            Map.entry("cilantro", "coriander"),
            Map.entry("dahi", "yogurt"),
            Map.entry("curd", "yogurt"),
            Map.entry("jeera", "cumin"),
            Map.entry("haldi", "turmeric"),
            Map.entry("adrak", "ginger"),
            Map.entry("lahsun", "garlic"),
            Map.entry("lasun", "garlic"),
            Map.entry("aloo", "potato"),
            Map.entry("pyaaz", "onion"),
            Map.entry("pyaz", "onion"),
            Map.entry("tamatar", "tomato"),
            Map.entry("palak", "spinach"),
            Map.entry("methi", "fenugreek"),
            Map.entry("sarson", "mustard"),
            Map.entry("besan", "gram flour"),
            Map.entry("maida", "all purpose flour"),
            Map.entry("atta", "wheat flour"),
            Map.entry("capsicum", "bell pepper"),
            Map.entry("shimla mirch", "bell pepper")
    );

    public String resolveAlias(String normalized) {
        if (normalized == null) return "";
        return CULINARY_ALIASES.getOrDefault(normalized, normalized);
    }

    public boolean isIngredientMatched(String recipeIngredient, Set<String> userIngredients) {
        if (recipeIngredient == null || userIngredients == null || userIngredients.isEmpty()) {
            return false;
        }

        String normRecipe = resolveAlias(normalize(recipeIngredient));
        if (normRecipe.isEmpty()) {
            return false;
        }

        for (String userIng : userIngredients) {
            if (userIng == null) continue;
            String normUser = resolveAlias(normalize(userIng));
            if (normUser.isEmpty()) continue;

            if (matchesTokenOrPhrase(normRecipe, normUser)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesTokenOrPhrase(String normRecipe, String normUser) {
        if (normRecipe.equals(normUser)) {
            return true;
        }

        // False-positive guards: distinct culinary ingredients that must never match
        if (isFalsePositivePair(normRecipe, normUser)) {
            return false;
        }

        // Check if normUser is a full word / phrase in normRecipe (e.g., "chicken" in "chicken breast", "onion" in "green onion")
        if (containsAsWord(normRecipe, normUser)) {
            return true;
        }

        // Check if normRecipe is a full word / phrase in normUser (e.g., recipe needs "chicken", user entered "chicken breast")
        if (containsAsWord(normUser, normRecipe)) {
            return true;
        }

        return false;
    }

    private boolean isFalsePositivePair(String a, String b) {
        // "pea" must not match "peanut", "egg" must not match "eggplant"
        if (("pea".equals(a) && b.contains("peanut")) || ("pea".equals(b) && a.contains("peanut"))) {
            return true;
        }
        if (("egg".equals(a) && b.contains("eggplant")) || ("egg".equals(b) && a.contains("eggplant"))) {
            return true;
        }
        return false;
    }

    private boolean containsAsWord(String text, String target) {
        if (text == null || target == null || target.isEmpty()) return false;
        String regex = "(?i).*\\b" + Pattern.quote(target) + "\\b.*";
        return text.matches(regex);
    }

    public String normalize(String s) {
        if (s == null) return "";
        String clean = s.toLowerCase().trim()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (clean.isEmpty()) return "";

        String[] words = clean.split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String w = singularize(words[i]);
            if (i > 0) sb.append(" ");
            sb.append(w);
        }
        return sb.toString();
    }

    private String singularize(String word) {
        if (word == null || word.length() <= 3) return word;
        if (word.endsWith("ies") && word.length() > 4) {
            return word.substring(0, word.length() - 3) + "y";
        }
        if (word.endsWith("es")) {
            if (word.endsWith("shes") || word.endsWith("ches") || word.endsWith("xes") || word.endsWith("zes") || word.endsWith("toes")) {
                return word.substring(0, word.length() - 2);
            }
        }
        if (word.endsWith("s") && !word.endsWith("ss") && !word.endsWith("us") && !word.endsWith("is")) {
            return word.substring(0, word.length() - 1);
        }
        return word;
    }

    public boolean isDietaryCompatible(DietaryType recipeDiet, DietaryType userDiet) {
        if (userDiet == null || userDiet == DietaryType.NON_VEGETARIAN) {
            return true; // Any diet is acceptable
        }
        if (recipeDiet == userDiet) {
            return true;
        }
        // Vegan recipes satisfy Vegetarian
        if (userDiet == DietaryType.VEGETARIAN && recipeDiet == DietaryType.VEGAN) {
            return true;
        }
        // Vegan or Vegetarian recipes satisfy Eggetarian
        if (userDiet == DietaryType.EGGETARIAN &&
                (recipeDiet == DietaryType.VEGETARIAN || recipeDiet == DietaryType.VEGAN || recipeDiet == DietaryType.EGGETARIAN)) {
            return true;
        }
        return false;
    }

    public double calculateDietaryScore(DietaryType recipeDiet, DietaryType userDiet) {
        if (userDiet == null) {
            return 15.0; // neutral full score when no preference specified
        }
        return isDietaryCompatible(recipeDiet, userDiet) ? 15.0 : 0.0;
    }

    public double calculateCuisineScore(String recipeCuisine, List<String> preferredCuisines) {
        if (preferredCuisines == null || preferredCuisines.isEmpty()) {
            return 10.0; // neutral full score when no preference stated
        }
        if (recipeCuisine == null) {
            return 0.0;
        }
        String normRc = RecommendationService.normalizeCuisine(recipeCuisine);
        for (String pref : preferredCuisines) {
            if (pref == null || pref.isBlank() || pref.trim().equalsIgnoreCase("all")) continue;
            String normPref = RecommendationService.normalizeCuisine(pref);
            if (normRc.equals(normPref)) {
                return 10.0;
            }
            if ("indian".equals(normPref) && (normRc.contains("indian") || normRc.contains("tamil")
                    || normRc.contains("punjabi") || normRc.contains("bengali") || normRc.contains("kerala")
                    || normRc.contains("awadhi") || normRc.contains("chettinad") || normRc.contains("mughlai")
                    || normRc.contains("konkan") || normRc.contains("karnataka") || normRc.contains("andhra")
                    || normRc.contains("gujarati") || normRc.contains("rajasthani") || normRc.contains("goan")
                    || normRc.contains("kashmiri") || normRc.contains("mangalorean") || normRc.contains("parsi"))) {
                return 10.0;
            }
            if ("asian".equals(normPref) && (normRc.contains("asian") || normRc.contains("chinese")
                    || normRc.contains("japanese") || normRc.contains("thai") || normRc.contains("korean")
                    || normRc.contains("vietnamese") || normRc.contains("malaysian") || normRc.contains("indo chinese"))) {
                return 10.0;
            }
        }
        return 0.0;
    }

    public double calculateTimeScore(Integer recipeTimeMinutes, Integer maxCookingTime) {
        if (maxCookingTime == null || maxCookingTime <= 0) {
            return 10.0; // no constraint
        }
        if (recipeTimeMinutes == null) {
            return 5.0; // unknown cooking time handled consistently, not treated as zero
        }
        return recipeTimeMinutes <= maxCookingTime ? 10.0 : 0.0;
    }

    public double calculateMealTypeScore(MealType recipeMeal, MealType requestedMeal) {
        if (requestedMeal == null || recipeMeal == requestedMeal) {
            return 5.0; // full score if no preference or exact match
        }
        return 0.0;
    }

    public double calculateSkillScore(DifficultyLevel recipeDifficulty, CookingSkillLevel userSkill) {
        if (userSkill == null) {
            return 5.0; // neutral
        }
        DifficultyLevel recDiff = recipeDifficulty != null ? recipeDifficulty : DifficultyLevel.MEDIUM;
        switch (userSkill) {
            case BEGINNER:
                return recDiff == DifficultyLevel.EASY ? 5.0 : (recDiff == DifficultyLevel.MEDIUM ? 2.5 : 0.0);
            case INTERMEDIATE:
                return (recDiff == DifficultyLevel.EASY || recDiff == DifficultyLevel.MEDIUM) ? 5.0 : 2.5;
            case ADVANCED:
                return recDiff == DifficultyLevel.HARD ? 5.0 : 4.0;
            default:
                return 5.0;
        }
    }

    public boolean isSkillCompatible(DifficultyLevel recipeDifficulty, CookingSkillLevel userSkill) {
        if (userSkill == null) {
            return true; // No filter
        }
        DifficultyLevel diff = recipeDifficulty != null ? recipeDifficulty : DifficultyLevel.MEDIUM;
        switch (userSkill) {
            case BEGINNER:
                return diff == DifficultyLevel.EASY;
            case INTERMEDIATE:
                return diff == DifficultyLevel.EASY || diff == DifficultyLevel.MEDIUM;
            case ADVANCED:
                return true;
            default:
                return true;
        }
    }

    private String generateReason(int matchedCount, int totalCount, Recipe recipe, PreferenceContext prefs, double totalScore) {
        StringBuilder sb = new StringBuilder();
        int missingCount = totalCount - matchedCount;

        if (matchedCount == totalCount && totalCount > 0) {
            sb.append(String.format("Perfect match! You have all %d required ingredients.", totalCount));
        } else if (matchedCount > 0) {
            double pct = ((double) matchedCount / totalCount) * 100.0;
            sb.append(String.format("Matched %d of %d ingredients (%.0f%%) with %d missing.",
                    matchedCount, totalCount, pct, missingCount));
        } else {
            sb.append("No matching ingredients available in your selection.");
        }

        if (prefs.dietaryType != null && isDietaryCompatible(recipe.getDietaryType(), prefs.dietaryType)) {
            sb.append(String.format(" Fully compliant with your %s diet.", recipe.getDietaryType()));
        }

        if (recipe.getCuisine() != null && prefs.preferredCuisines != null && !prefs.preferredCuisines.isEmpty()) {
            boolean matchesCuisine = prefs.preferredCuisines.stream().anyMatch(c -> c != null && recipe.getCuisine().toLowerCase().contains(c.toLowerCase()));
            if (matchesCuisine) {
                sb.append(String.format(" Matches your preferred %s cuisine.", recipe.getCuisine()));
            }
        }

        if (recipe.getTotalTimeMinutes() != null) {
            if (prefs.maxCookingTime != null && recipe.getTotalTimeMinutes() <= prefs.maxCookingTime) {
                sb.append(String.format(" Ready in %d mins (under your %d min limit).",
                        recipe.getTotalTimeMinutes(), prefs.maxCookingTime));
            } else if (recipe.getTotalTimeMinutes() <= 30) {
                sb.append(String.format(" Quick to make in %d mins.", recipe.getTotalTimeMinutes()));
            }
        }

        if (prefs.cookingSkillLevel != null) {
            sb.append(String.format(" Suitable for %s skill level (%s recipe).",
                    prefs.cookingSkillLevel, recipe.getDifficulty() != null ? recipe.getDifficulty() : "Standard"));
        }

        return sb.toString().trim();
    }
}
