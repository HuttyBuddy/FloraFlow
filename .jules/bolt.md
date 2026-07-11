## 2024-06-18 - Jetpack Compose Lazy List Performance
**Learning:** By default, Compose uses item position as the key for elements in Lazy lists (`items()`). When items are added, removed, or reordered, this can cause unnecessary re-renders of the entire list.
**Action:** Always provide a stable, unique identifier (e.g., `key = { it.id }`) to the `items()` parameter in `LazyColumn` and `LazyRow` to allow Compose to smartly reuse elements and skip re-renders.
## 2024-06-21 - Gradle Java Home configuration
**Learning:** If `gradle.properties` contains a hardcoded Windows path for `org.gradle.java.home` (e.g., `F:/Android Studio/jbr`), it will cause build failures in Linux/sandbox environments.
**Action:** Always check `gradle.properties` and comment out or remove hardcoded `org.gradle.java.home` values before running Gradle commands if you encounter Java home errors.
## 2024-07-26 - Room Database Batch Insert Optimization
**Learning:** Inserting multiple items into a Room Database one-by-one inside a loop forces a separate transaction for every insert, leading to significant I/O overhead.
**Action:** When inserting multiple items, accumulate them in a list and use a single `@Insert` method in the DAO that accepts a `List<Entity>` to perform a batch insert in a single transaction.
## 2024-08-01 - Jetpack Compose LazyList Key Pitfalls
**Learning:** Explicitly passing the list index as a key in `LazyColumn` or `LazyRow` is a no-op because Compose defaults to position anyway. Using `System.identityHashCode(it)` is dangerous and ineffective because it risks key collisions and changes whenever an immutable data class is copied during state updates.
**Action:** Only provide a custom key to Lazy lists if you have a stable, unique property (like a database ID) inside the item's data class.

## 2024-08-01 - Jetpack Compose Allocation Avoidance
**Learning:** Defining static lists (e.g., `listOf("All", "Flower", "Shrub")`) directly inside a `@Composable` function forces the UI to re-allocate those objects on every recomposition.
**Action:** Extract static data structures like filter lists to private top-level constants outside the Composable to prevent unnecessary memory allocations and GC overhead.
## 2024-08-01 - Room Database Batch Insert
**Learning:** Calling `insertPlant` multiple times inside a loop (like parsing Gemini output) creates unnecessary database transactions and degrades performance.
**Action:** Always accumulate objects in a list (`mutableListOf<Plant>`) and use a batch insert function like `insertPlants(listOf(...))` to perform the insertion in a single transaction.
## 2024-05-18 - Optimize nested loops with precomputed O(1) arrays
**Learning:** During heavy Compose recompositions, nested iteration `O(N * M)` operations like `.firstOrNull()` inside UI lists or grids can severely degrade frame rate and CPU performance.
**Action:** When a static list must be frequently queried by multiple nested UI elements, map it into a 1D or 2D Array before the loops. Lookups via direct array indices change complexity from `O(N)` to `O(1)`, resulting in massive (often >20x) iteration time improvements.
## 2024-08-01 - 2D Grid Distance Calculations
**Learning:** In integer-based grid distance checks (e.g., finding adjacent cells), computing `sqrt(dx*dx + dy*dy)` inside `O(N^2)` nested loops is mathematically redundant and computationally expensive.
**Action:** Replace `sqrt()` calculations with squared distance comparisons (e.g., `distSq <= 1.5 * 1.5`). Additionally, use early-exit bounds checks (`if (dx > 1) continue`) to entirely bypass the computation for distant elements, turning heavy `O(N^2)` work into near `O(1)` per close interaction.
