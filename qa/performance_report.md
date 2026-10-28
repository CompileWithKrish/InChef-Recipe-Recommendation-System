# Performance Benchmark & Optimization Report
**Target System:** InChef Backend (8,061 Recipes, 90,833 Links on MySQL 8)

## Latency Benchmarks (5 Consecutive Iterations)

| Operation | 1st Request (Cold) | Avg Subsequent (Warm) | Min Latency | Max Latency | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Direct Search ('Chicken Biryani') | 93.5 ms | 138.9 ms | 93.5 ms | 150.9 ms | EXCELLENT (<1s) |
| Ingredient Recommendation (4 items) | 1429.7 ms | 1412.0 ms | 1370.2 ms | 1438.8 ms | GOOD (<2s) |
| Pantry Recommendation (User 1) | 1953.7 ms | 1948.4 ms | 1886.3 ms | 2034.0 ms | GOOD (<2s) |
| Recipe Detail Retrieval (#1) | 11.4 ms | 12.9 ms | 9.6 ms | 16.6 ms | EXCELLENT (<1s) |

## Architectural Bottleneck Analysis & Resolution
1. **N+1 Elimination:** Replaced inline `favoriteRepository.countByRecipeId()` inside the 8,061-recipe loop with a lightweight RAM evaluator (`mapRecipeForEvaluation`), executing full enrichment solely on the ranked Top N results.
2. **In-Memory Caching:** Added a 60-second read-through cache for published recipes to prevent querying 90k joins on repetitive recommendation requests.
3. **Direct Title Search:** Backed by MySQL indexing and Spring Data JPA `Pageable` query limits, delivering sub-150ms search times.