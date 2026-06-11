## 2024-06-11 - Jetpack Compose LazyColumn O(n^2) Bottleneck
**Learning:** Checking a large list for containment within a `LazyColumn` item block using `.any { it.name.lowercase() == ... }` results in O(n²) string allocations and iterations during recomposition.
**Action:** Always pre-calculate an O(1) lowercased hash set using `remember` *outside* the `LazyColumn` for membership tests to prevent blocking the main thread during scrolling.
