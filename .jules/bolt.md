## 2024-06-13 - Missing Stable Keys in Jetpack Compose Lists
**Learning:** Omitted `key` parameter in `LazyColumn`/`LazyRow` list items (`items(list)`) causes Compose to re-evaluate and re-render the entire list when data changes. It is a critical performance anti-pattern.
**Action:** Always provide a stable unique identifier to the `key` parameter in dynamic lists (`items(list, key = { it.id })`) to enable Compose to smartly reuse DOM-like elements and reduce unnecessary re-renders.
