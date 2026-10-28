package com.inchef.dataset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import com.inchef.common.MeasurementUnit;
import com.inchef.favorite.FavoriteService;
import com.inchef.ingredient.Ingredient;
import com.inchef.ingredient.IngredientRepository;
import com.inchef.pantry.UserIngredientRequest;
import com.inchef.pantry.UserIngredientService;
import com.inchef.preference.DietaryType;
import com.inchef.preference.SpiceLevel;
import com.inchef.preference.UserPreferenceRequest;
import com.inchef.preference.UserPreferenceService;
import com.inchef.recipe.DifficultyLevel;
import com.inchef.recipe.MealType;
import com.inchef.recipe.RecipeCreateRequest;
import com.inchef.recipe.RecipeDetailResponse;
import com.inchef.recipe.RecipeIngredientRequest;
import com.inchef.recipe.RecipeRepository;
import com.inchef.recipe.RecipeService;
import com.inchef.user.UserCreateRequest;
import com.inchef.user.UserRepository;
import com.inchef.user.UserResponse;
import com.inchef.user.UserService;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final UserService userService;
    private final RecipeService recipeService;
    private final UserPreferenceService userPreferenceService;
    private final UserIngredientService userIngredientService;
    private final FavoriteService favoriteService;

    public DataInitializer(UserRepository userRepository,
                           RecipeRepository recipeRepository,
                           IngredientRepository ingredientRepository,
                           UserService userService,
                           RecipeService recipeService,
                           UserPreferenceService userPreferenceService,
                           UserIngredientService userIngredientService,
                           FavoriteService favoriteService) {
        this.userRepository = userRepository;
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
        this.userService = userService;
        this.recipeService = recipeService;
        this.userPreferenceService = userPreferenceService;
        this.userIngredientService = userIngredientService;
        this.favoriteService = favoriteService;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0 || recipeRepository.count() > 0) {
            log.info("Database already initialized. Skipping sample data seed.");
            return;
        }

        log.info("Starting InChef database initialization with sample data...");

        // 1. Seed Ingredients
        seedIngredients();

        // 2. Seed Users
        UserResponse alex = userService.createUser(UserCreateRequest.builder()
                .email("alex@inchef.com")
                .name("Alex Johnson")
                .phone("+1-555-0101")
                .avatarUrl("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150")
                .build());

        UserResponse sophia = userService.createUser(UserCreateRequest.builder()
                .email("sophia@inchef.com")
                .name("Sophia Chen")
                .phone("+1-555-0102")
                .avatarUrl("https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150")
                .build());

        UserResponse marcus = userService.createUser(UserCreateRequest.builder()
                .email("marcus@inchef.com")
                .name("Marcus Rivera")
                .phone("+1-555-0103")
                .avatarUrl("https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=150")
                .build());

        // 3. User Preferences
        userPreferenceService.saveOrUpdateUserPreference(alex.getId(), UserPreferenceRequest.builder()
                .dietaryType(DietaryType.VEGETARIAN)
                .maxCookingTime(45)
                .preferredCuisines(Arrays.asList("Italian", "Mexican"))
                .allergies(List.of("Peanuts"))
                .preferredSpiceLevel(SpiceLevel.MEDIUM)
                .build());

        userPreferenceService.saveOrUpdateUserPreference(sophia.getId(), UserPreferenceRequest.builder()
                .dietaryType(DietaryType.NON_VEGETARIAN)
                .maxCookingTime(30)
                .preferredCuisines(Arrays.asList("Chinese", "Asian"))
                .allergies(List.of())
                .preferredSpiceLevel(SpiceLevel.SPICY)
                .build());

        userPreferenceService.saveOrUpdateUserPreference(marcus.getId(), UserPreferenceRequest.builder()
                .dietaryType(DietaryType.VEGAN)
                .maxCookingTime(60)
                .preferredCuisines(Arrays.asList("Mexican", "Indian"))
                .allergies(Arrays.asList("Dairy", "Shellfish"))
                .preferredSpiceLevel(SpiceLevel.EXTRA_SPICY)
                .build());

        // 4. Seed User Pantries
        seedPantry(alex.getId(), Arrays.asList("Garlic", "Olive Oil", "Tomato", "Pasta", "Onion", "Bell Pepper", "Basil", "Mozzarella"));
        seedPantry(sophia.getId(), Arrays.asList("Chicken Breast", "Soy Sauce", "Ginger", "Garlic", "Rice", "Sesame Oil", "Green Onions"));
        seedPantry(marcus.getId(), Arrays.asList("Black Beans", "Corn", "Avocado", "Onion", "Lime", "Cilantro", "Cumin"));

        // 5. Seed Recipes
        RecipeDetailResponse r1 = seedRecipe1Margherita();
        RecipeDetailResponse r2 = seedRecipe2GarlicPasta();
        RecipeDetailResponse r3 = seedRecipe3KungPaoChicken();
        RecipeDetailResponse r4 = seedRecipe4BlackBeanBowl();
        RecipeDetailResponse r5 = seedRecipe5Guacamole();
        RecipeDetailResponse r6 = seedRecipe6PaneerTikka();
        RecipeDetailResponse r7 = seedRecipe7TofuStirFry();
        RecipeDetailResponse r8 = seedRecipe8ChickenCaesar();
        RecipeDetailResponse r9 = seedRecipe9Pancakes();
        RecipeDetailResponse r10 = seedRecipe10HummusWrap();

        // 6. Seed Favorites
        favoriteService.addFavorite(alex.getId(), r1.getId());
        favoriteService.addFavorite(alex.getId(), r2.getId());
        favoriteService.addFavorite(sophia.getId(), r3.getId());
        favoriteService.addFavorite(sophia.getId(), r8.getId());
        favoriteService.addFavorite(marcus.getId(), r4.getId());
        favoriteService.addFavorite(marcus.getId(), r5.getId());

        log.info("InChef sample data initialized successfully! (3 users, 10 recipes, pantries & preferences configured)");
    }

    private void seedIngredients() {
        List<Ingredient> ingredients = Arrays.asList(
                createIng("Tomato", "Vegetables", MeasurementUnit.PIECES, "https://images.unsplash.com/photo-1592924357228-91a4daadcfea?w=200"),
                createIng("Garlic", "Vegetables", MeasurementUnit.PIECES, "https://images.unsplash.com/photo-1615485500704-8e990f9900f7?w=200"),
                createIng("Onion", "Vegetables", MeasurementUnit.PIECES, "https://images.unsplash.com/photo-1508747703725-719777637510?w=200"),
                createIng("Bell Pepper", "Vegetables", MeasurementUnit.PIECES, "https://images.unsplash.com/photo-1563565375-f3fdfdbefa83?w=200"),
                createIng("Basil", "Spices & Herbs", MeasurementUnit.GRAMS, "https://images.unsplash.com/photo-1608686207856-001b95cf60ca?w=200"),
                createIng("Olive Oil", "Condiments & Oils", MeasurementUnit.MILLILITERS, "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=200"),
                createIng("Pasta", "Grains", MeasurementUnit.GRAMS, "https://images.unsplash.com/photo-1551462147-ff29053bfc14?w=200"),
                createIng("Mozzarella", "Dairy & Eggs", MeasurementUnit.GRAMS, "https://images.unsplash.com/photo-1588195538326-c5b1e9f80a1b?w=200"),
                createIng("Pizza Dough", "Grains", MeasurementUnit.PIECES, "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=200"),
                createIng("Chicken Breast", "Proteins", MeasurementUnit.GRAMS, "https://images.unsplash.com/photo-1604503468506-a8da13d82791?w=200"),
                createIng("Soy Sauce", "Condiments & Oils", MeasurementUnit.MILLILITERS, "https://images.unsplash.com/photo-1582293041079-7814c2f12063?w=200"),
                createIng("Ginger", "Vegetables", MeasurementUnit.GRAMS, "https://images.unsplash.com/photo-1615485290382-441e4d049cb5?w=200"),
                createIng("Rice", "Grains", MeasurementUnit.GRAMS, "https://images.unsplash.com/photo-1586201375761-83865001e31c?w=200"),
                createIng("Sesame Oil", "Condiments & Oils", MeasurementUnit.MILLILITERS, "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=200"),
                createIng("Green Onions", "Vegetables", MeasurementUnit.PIECES, "https://images.unsplash.com/photo-1518977676601-b53f82aba655?w=200"),
                createIng("Black Beans", "Proteins", MeasurementUnit.GRAMS, "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=200"),
                createIng("Corn", "Vegetables", MeasurementUnit.GRAMS, "https://images.unsplash.com/photo-1551754655-cd27e38d2076?w=200"),
                createIng("Avocado", "Vegetables", MeasurementUnit.PIECES, "https://images.unsplash.com/photo-1523049673857-eb18f1d7b578?w=200"),
                createIng("Lime", "Fruits", MeasurementUnit.PIECES, "https://images.unsplash.com/photo-1532336414038-cf19250c5757?w=200"),
                createIng("Cilantro", "Spices & Herbs", MeasurementUnit.GRAMS, "https://images.unsplash.com/photo-1608686207856-001b95cf60ca?w=200"),
                createIng("Cumin", "Spices & Herbs", MeasurementUnit.TEASPOONS, "https://images.unsplash.com/photo-1599940824399-b87987ceb72a?w=200"),
                createIng("Butter", "Dairy & Eggs", MeasurementUnit.GRAMS, "https://images.unsplash.com/photo-1589985270826-4b7bb135bc9d?w=200"),
                createIng("Parmesan", "Dairy & Eggs", MeasurementUnit.GRAMS, "https://images.unsplash.com/photo-1452195100486-9cc805987862?w=200"),
                createIng("Paneer", "Dairy & Eggs", MeasurementUnit.GRAMS, "https://images.unsplash.com/photo-1631452180519-c014fe946bc7?w=200"),
                createIng("Garam Masala", "Spices & Herbs", MeasurementUnit.TEASPOONS, "https://images.unsplash.com/photo-1599940824399-b87987ceb72a?w=200"),
                createIng("Tofu", "Proteins", MeasurementUnit.GRAMS, "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=200"),
                createIng("Broccoli", "Vegetables", MeasurementUnit.GRAMS, "https://images.unsplash.com/photo-1459411621453-7b03977f4bfc?w=200"),
                createIng("Romaine Lettuce", "Vegetables", MeasurementUnit.GRAMS, "https://images.unsplash.com/photo-1556881286-fc6915169721?w=200"),
                createIng("Croutons", "Grains", MeasurementUnit.GRAMS, "https://images.unsplash.com/photo-1551462147-ff29053bfc14?w=200"),
                createIng("Flour", "Grains", MeasurementUnit.CUPS, "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=200"),
                createIng("Milk", "Dairy & Eggs", MeasurementUnit.MILLILITERS, "https://images.unsplash.com/photo-1563636619-e9143da7973b?w=200"),
                createIng("Eggs", "Dairy & Eggs", MeasurementUnit.PIECES, "https://images.unsplash.com/photo-1506976785307-8732e854ad03?w=200"),
                createIng("Hummus", "Condiments & Oils", MeasurementUnit.TABLESPOONS, "https://images.unsplash.com/photo-1577906096429-f73c2c312435?w=200"),
                createIng("Tortilla Wrap", "Grains", MeasurementUnit.PIECES, "https://images.unsplash.com/photo-1565299585323-38d6b0865b47?w=200"),
                createIng("Peanuts", "Proteins", MeasurementUnit.GRAMS, "https://images.unsplash.com/photo-1567892328003-8d39e334336c?w=200")
        );

        ingredientRepository.saveAll(ingredients);
    }

    private Ingredient createIng(String name, String cat, MeasurementUnit unit, String img) {
        return Ingredient.builder()
                .name(name)
                .category(cat)
                .defaultUnit(unit)
                .imageUrl(img)
                .build();
    }

    private void seedPantry(Long userId, List<String> ingredientNames) {
        for (String name : ingredientNames) {
            userIngredientService.addOrUpdateUserIngredient(userId, UserIngredientRequest.builder()
                    .ingredientName(name)
                    .quantity(2.0)
                    .unit(MeasurementUnit.PIECES)
                    .expiryDate(LocalDate.now().plusDays(14))
                    .build());
        }
    }

    private RecipeDetailResponse seedRecipe1Margherita() {
        return recipeService.createRecipe(RecipeCreateRequest.builder()
                .title("Classic Margherita Pizza")
                .description("Authentic Neapolitan pizza topped with fresh San Marzano tomatoes, sliced mozzarella, fresh basil, and extra virgin olive oil.")
                .cuisine("Italian")
                .mealType(MealType.DINNER)
                .dietaryType(DietaryType.VEGETARIAN)
                .difficulty(DifficultyLevel.MEDIUM)
                .prepTimeMinutes(20)
                .cookTimeMinutes(12)
                .servings(2)
                .calories(650)
                .spiceLevel(SpiceLevel.MILD)
                .instructions("1. Preheat oven to highest setting with a pizza stone.\n2. Stretch pizza dough into a 12-inch circle.\n3. Spread crushed tomato sauce evenly.\n4. Scatter fresh mozzarella slices and torn basil leaves.\n5. Drizzle with extra virgin olive oil.\n6. Bake for 10-12 minutes until crust is golden blistered.")
                .imageUrl("https://images.unsplash.com/photo-1604382355076-af4b0eb60143?w=600")
                .ingredients(Arrays.asList(
                        req("Pizza Dough", 1.0, MeasurementUnit.PIECES, false),
                        req("Tomato", 3.0, MeasurementUnit.PIECES, false),
                        req("Mozzarella", 200.0, MeasurementUnit.GRAMS, false),
                        req("Basil", 15.0, MeasurementUnit.GRAMS, false),
                        req("Olive Oil", 2.0, MeasurementUnit.TABLESPOONS, false),
                        req("Garlic", 1.0, MeasurementUnit.PIECES, true)
                ))
                .build());
    }

    private RecipeDetailResponse seedRecipe2GarlicPasta() {
        return recipeService.createRecipe(RecipeCreateRequest.builder()
                .title("Creamy Garlic Butter Pasta")
                .description("Silky, comforting pasta tossed in a velvety garlic-infused butter sauce finished with freshly grated parmesan.")
                .cuisine("Italian")
                .mealType(MealType.DINNER)
                .dietaryType(DietaryType.VEGETARIAN)
                .difficulty(DifficultyLevel.EASY)
                .prepTimeMinutes(10)
                .cookTimeMinutes(15)
                .servings(3)
                .calories(520)
                .spiceLevel(SpiceLevel.MILD)
                .instructions("1. Boil salted water and cook pasta until al dente.\n2. In a skillet, melt butter over medium heat and sauté minced garlic until fragrant.\n3. Add olive oil and a splash of reserved pasta water.\n4. Toss pasta into the garlic butter sauce.\n5. Finish with freshly grated parmesan and chopped basil.")
                .imageUrl("https://images.unsplash.com/photo-1621996346565-e3d5d6281292?w=600")
                .ingredients(Arrays.asList(
                        req("Pasta", 300.0, MeasurementUnit.GRAMS, false),
                        req("Garlic", 4.0, MeasurementUnit.PIECES, false),
                        req("Butter", 50.0, MeasurementUnit.GRAMS, false),
                        req("Olive Oil", 1.0, MeasurementUnit.TABLESPOONS, false),
                        req("Parmesan", 60.0, MeasurementUnit.GRAMS, false),
                        req("Basil", 10.0, MeasurementUnit.GRAMS, true)
                ))
                .build());
    }

    private RecipeDetailResponse seedRecipe3KungPaoChicken() {
        return recipeService.createRecipe(RecipeCreateRequest.builder()
                .title("Spicy Kung Pao Chicken")
                .description("Tender cubes of chicken breast wok-seared with peppers, ginger, garlic, green onions, and peanuts in a savory-sweet glaze.")
                .cuisine("Chinese")
                .mealType(MealType.DINNER)
                .dietaryType(DietaryType.NON_VEGETARIAN)
                .difficulty(DifficultyLevel.MEDIUM)
                .prepTimeMinutes(15)
                .cookTimeMinutes(12)
                .servings(4)
                .calories(580)
                .spiceLevel(SpiceLevel.SPICY)
                .instructions("1. Dice chicken breast and marinate with 1 tbsp soy sauce.\n2. Heat sesame oil in a wok over high heat.\n3. Add ginger, garlic, and sliced green onions.\n4. Stir-fry chicken until browned on all sides.\n5. Add diced bell peppers, peanuts, and soy sauce glaze.\n6. Serve immediately over steamed rice.")
                .imageUrl("https://images.unsplash.com/photo-1525755662778-989d0524087e?w=600")
                .ingredients(Arrays.asList(
                        req("Chicken Breast", 400.0, MeasurementUnit.GRAMS, false),
                        req("Bell Pepper", 2.0, MeasurementUnit.PIECES, false),
                        req("Ginger", 20.0, MeasurementUnit.GRAMS, false),
                        req("Garlic", 3.0, MeasurementUnit.PIECES, false),
                        req("Soy Sauce", 3.0, MeasurementUnit.TABLESPOONS, false),
                        req("Sesame Oil", 1.5, MeasurementUnit.TABLESPOONS, false),
                        req("Green Onions", 3.0, MeasurementUnit.PIECES, false),
                        req("Peanuts", 50.0, MeasurementUnit.GRAMS, false),
                        req("Rice", 250.0, MeasurementUnit.GRAMS, true)
                ))
                .build());
    }

    private RecipeDetailResponse seedRecipe4BlackBeanBowl() {
        return recipeService.createRecipe(RecipeCreateRequest.builder()
                .title("Loaded Black Bean Fiesta Bowls")
                .description("Nutritious plant-based fiesta bowls packed with seasoned black beans, sweet corn, creamy avocado, and fresh cilantro lime rice.")
                .cuisine("Mexican")
                .mealType(MealType.LUNCH)
                .dietaryType(DietaryType.VEGAN)
                .difficulty(DifficultyLevel.EASY)
                .prepTimeMinutes(15)
                .cookTimeMinutes(15)
                .servings(2)
                .calories(480)
                .spiceLevel(SpiceLevel.MEDIUM)
                .instructions("1. Cook rice and fluff with fresh lime juice and chopped cilantro.\n2. Warm black beans in a skillet with cumin, diced onion, and olive oil.\n3. Lightly char corn in a dry pan.\n4. Assemble bowls with rice, beans, corn, sliced avocado, and diced bell pepper.\n5. Garnish with lime wedges.")
                .imageUrl("https://images.unsplash.com/photo-1543339308-43e59d6b73a6?w=600")
                .ingredients(Arrays.asList(
                        req("Black Beans", 300.0, MeasurementUnit.GRAMS, false),
                        req("Corn", 150.0, MeasurementUnit.GRAMS, false),
                        req("Avocado", 1.0, MeasurementUnit.PIECES, false),
                        req("Rice", 180.0, MeasurementUnit.GRAMS, false),
                        req("Onion", 1.0, MeasurementUnit.PIECES, false),
                        req("Lime", 2.0, MeasurementUnit.PIECES, false),
                        req("Cilantro", 15.0, MeasurementUnit.GRAMS, false),
                        req("Cumin", 1.0, MeasurementUnit.TEASPOONS, false)
                ))
                .build());
    }

    private RecipeDetailResponse seedRecipe5Guacamole() {
        return recipeService.createRecipe(RecipeCreateRequest.builder()
                .title("Authentic Guacamole & Pico de Gallo")
                .description("Creamy mashed Hass avocados folded with diced red onion, ripe tomatoes, fresh cilantro, lime juice, and sea salt.")
                .cuisine("Mexican")
                .mealType(MealType.SNACK)
                .dietaryType(DietaryType.VEGAN)
                .difficulty(DifficultyLevel.EASY)
                .prepTimeMinutes(10)
                .cookTimeMinutes(0)
                .servings(4)
                .calories(210)
                .spiceLevel(SpiceLevel.MEDIUM)
                .instructions("1. Cut avocados in half, remove pit, and scoop into a bowl.\n2. Mash roughly with a fork, leaving satisfying chunks.\n3. Stir in finely diced onion, diced tomatoes, cilantro, and minced garlic.\n4. Squeeze fresh lime juice and season with cumin and salt.\n5. Serve immediately with tortilla chips.")
                .imageUrl("https://images.unsplash.com/photo-1540420773420-3366772f4999?w=600")
                .ingredients(Arrays.asList(
                        req("Avocado", 3.0, MeasurementUnit.PIECES, false),
                        req("Tomato", 2.0, MeasurementUnit.PIECES, false),
                        req("Onion", 1.0, MeasurementUnit.PIECES, false),
                        req("Lime", 2.0, MeasurementUnit.PIECES, false),
                        req("Cilantro", 20.0, MeasurementUnit.GRAMS, false),
                        req("Garlic", 1.0, MeasurementUnit.PIECES, true),
                        req("Cumin", 0.5, MeasurementUnit.TEASPOONS, true)
                ))
                .build());
    }

    private RecipeDetailResponse seedRecipe6PaneerTikka() {
        return recipeService.createRecipe(RecipeCreateRequest.builder()
                .title("Paneer Butter Masala")
                .description("Soft cottage cheese cubes simmered in a rich, buttery, spiced tomato gravy with aromatic garam masala.")
                .cuisine("Indian")
                .mealType(MealType.DINNER)
                .dietaryType(DietaryType.VEGETARIAN)
                .difficulty(DifficultyLevel.MEDIUM)
                .prepTimeMinutes(15)
                .cookTimeMinutes(25)
                .servings(3)
                .calories(540)
                .spiceLevel(SpiceLevel.SPICY)
                .instructions("1. Sauté onion, garlic, ginger, and tomatoes until soft; blend into a smooth puree.\n2. In a skillet, melt butter and add garam masala, cumin, and the tomato puree.\n3. Simmer for 10 minutes until oil separates.\n4. Gently add cubed paneer and simmer for 5 more minutes.\n5. Garnish with chopped cilantro and serve hot with rice.")
                .imageUrl("https://images.unsplash.com/photo-1631452180519-c014fe946bc7?w=600")
                .ingredients(Arrays.asList(
                        req("Paneer", 250.0, MeasurementUnit.GRAMS, false),
                        req("Tomato", 4.0, MeasurementUnit.PIECES, false),
                        req("Onion", 2.0, MeasurementUnit.PIECES, false),
                        req("Garlic", 4.0, MeasurementUnit.PIECES, false),
                        req("Ginger", 15.0, MeasurementUnit.GRAMS, false),
                        req("Butter", 40.0, MeasurementUnit.GRAMS, false),
                        req("Garam Masala", 1.5, MeasurementUnit.TEASPOONS, false),
                        req("Cilantro", 10.0, MeasurementUnit.GRAMS, true)
                ))
                .build());
    }

    private RecipeDetailResponse seedRecipe7TofuStirFry() {
        return recipeService.createRecipe(RecipeCreateRequest.builder()
                .title("Crispy Tofu Vegetable Stir-Fry")
                .description("Golden pan-crisped tofu tossed with crunchy broccoli florets and bell peppers in a savory ginger garlic sesame sauce.")
                .cuisine("Asian")
                .mealType(MealType.DINNER)
                .dietaryType(DietaryType.VEGAN)
                .difficulty(DifficultyLevel.EASY)
                .prepTimeMinutes(15)
                .cookTimeMinutes(15)
                .servings(2)
                .calories(390)
                .spiceLevel(SpiceLevel.MEDIUM)
                .instructions("1. Press tofu to remove excess moisture and cut into bite-sized cubes.\n2. Sear tofu in sesame oil until golden on all sides.\n3. Add minced garlic, ginger, broccoli florets, and sliced bell pepper.\n4. Deglaze pan with soy sauce and toss until vegetables are tender-crisp.\n5. Garnish with green onions and serve hot.")
                .imageUrl("https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=600")
                .ingredients(Arrays.asList(
                        req("Tofu", 300.0, MeasurementUnit.GRAMS, false),
                        req("Broccoli", 200.0, MeasurementUnit.GRAMS, false),
                        req("Bell Pepper", 1.0, MeasurementUnit.PIECES, false),
                        req("Garlic", 2.0, MeasurementUnit.PIECES, false),
                        req("Ginger", 10.0, MeasurementUnit.GRAMS, false),
                        req("Soy Sauce", 2.0, MeasurementUnit.TABLESPOONS, false),
                        req("Sesame Oil", 1.0, MeasurementUnit.TABLESPOONS, false),
                        req("Green Onions", 2.0, MeasurementUnit.PIECES, true)
                ))
                .build());
    }

    private RecipeDetailResponse seedRecipe8ChickenCaesar() {
        return recipeService.createRecipe(RecipeCreateRequest.builder()
                .title("Classic Chicken Caesar Salad")
                .description("Crispy romaine lettuce, herb-grilled chicken breast, crunchy garlic croutons, and shaved parmesan tossed in classic dressing.")
                .cuisine("American")
                .mealType(MealType.LUNCH)
                .dietaryType(DietaryType.NON_VEGETARIAN)
                .difficulty(DifficultyLevel.EASY)
                .prepTimeMinutes(15)
                .cookTimeMinutes(10)
                .servings(2)
                .calories(460)
                .spiceLevel(SpiceLevel.MILD)
                .instructions("1. Season chicken breast with salt, pepper, and olive oil.\n2. Grill for 5-6 minutes each side until cooked through; slice into strips.\n3. Chop romaine lettuce and place into a salad bowl.\n4. Add chicken slices, crunchy croutons, and shaved parmesan.\n5. Drizzle with dressing and toss gently.")
                .imageUrl("https://images.unsplash.com/photo-1550304943-4f24f54ddde9?w=600")
                .ingredients(Arrays.asList(
                        req("Chicken Breast", 250.0, MeasurementUnit.GRAMS, false),
                        req("Romaine Lettuce", 150.0, MeasurementUnit.GRAMS, false),
                        req("Parmesan", 40.0, MeasurementUnit.GRAMS, false),
                        req("Croutons", 50.0, MeasurementUnit.GRAMS, false),
                        req("Olive Oil", 1.0, MeasurementUnit.TABLESPOONS, false),
                        req("Garlic", 1.0, MeasurementUnit.PIECES, true)
                ))
                .build());
    }

    private RecipeDetailResponse seedRecipe9Pancakes() {
        return recipeService.createRecipe(RecipeCreateRequest.builder()
                .title("Fluffy Golden Buttermilk Pancakes")
                .description("Light, airy, melt-in-your-mouth breakfast pancakes cooked to golden perfection on a hot buttered griddle.")
                .cuisine("American")
                .mealType(MealType.BREAKFAST)
                .dietaryType(DietaryType.VEGETARIAN)
                .difficulty(DifficultyLevel.EASY)
                .prepTimeMinutes(10)
                .cookTimeMinutes(10)
                .servings(4)
                .calories(340)
                .spiceLevel(SpiceLevel.MILD)
                .instructions("1. Whisk flour, sugar, and baking powder in a large bowl.\n2. In a separate bowl, whisk milk, eggs, and melted butter.\n3. Combine wet and dry ingredients until just mixed.\n4. Pour 1/4 cup batter onto a hot buttered skillet.\n5. Flip when bubbles burst on the surface; cook until golden brown.")
                .imageUrl("https://images.unsplash.com/photo-1528207776546-365bb710ee93?w=600")
                .ingredients(Arrays.asList(
                        req("Flour", 2.0, MeasurementUnit.CUPS, false),
                        req("Milk", 300.0, MeasurementUnit.MILLILITERS, false),
                        req("Eggs", 2.0, MeasurementUnit.PIECES, false),
                        req("Butter", 30.0, MeasurementUnit.GRAMS, false)
                ))
                .build());
    }

    private RecipeDetailResponse seedRecipe10HummusWrap() {
        return recipeService.createRecipe(RecipeCreateRequest.builder()
                .title("Mediterranean Hummus & Roasted Veggie Wrap")
                .description("Warm tortilla wraps filled with creamy hummus, crisp bell peppers, sweet red onions, tomatoes, and mixed greens.")
                .cuisine("Mediterranean")
                .mealType(MealType.LUNCH)
                .dietaryType(DietaryType.VEGAN)
                .difficulty(DifficultyLevel.EASY)
                .prepTimeMinutes(10)
                .cookTimeMinutes(5)
                .servings(2)
                .calories(370)
                .spiceLevel(SpiceLevel.MILD)
                .instructions("1. Warm tortilla wraps lightly in a skillet.\n2. Spread generous layer of hummus over center of each wrap.\n3. Top with sliced bell pepper, diced tomato, and thinly sliced onion.\n4. Drizzle lightly with olive oil.\n5. Roll tightly, slice diagonally, and serve.")
                .imageUrl("https://images.unsplash.com/photo-1565299585323-38d6b0865b47?w=600")
                .ingredients(Arrays.asList(
                        req("Tortilla Wrap", 2.0, MeasurementUnit.PIECES, false),
                        req("Hummus", 4.0, MeasurementUnit.TABLESPOONS, false),
                        req("Bell Pepper", 1.0, MeasurementUnit.PIECES, false),
                        req("Tomato", 1.0, MeasurementUnit.PIECES, false),
                        req("Onion", 0.5, MeasurementUnit.PIECES, false),
                        req("Olive Oil", 1.0, MeasurementUnit.TABLESPOONS, true)
                ))
                .build());
    }

    private RecipeIngredientRequest req(String name, Double qty, MeasurementUnit unit, Boolean opt) {
        return RecipeIngredientRequest.builder()
                .ingredientName(name)
                .quantity(qty)
                .unit(unit)
                .isOptional(opt)
                .build();
    }
}
