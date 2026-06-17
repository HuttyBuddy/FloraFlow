## 2024-06-17 - Compose Lazy List Re-renders
**Learning:** Jetpack Compose `LazyColumn` and `LazyRow` components will fully re-render all items if dynamic lists change without stable `key` parameters. This causes unnecessary overhead on the UI thread.
**Action:** Always provide a stable, unique identifier to the `key` parameter in dynamic lists (e.g., `items(list, key = { it.id })`) for `LazyColumn` and `LazyRow` to smartly reuse elements and prevent unnecessary full-list re-renders.
