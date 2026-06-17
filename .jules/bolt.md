## 2024-05-24 - Missing Stable Keys in Jetpack Compose Lazy Lists
**Learning:** By default, Jetpack Compose `LazyColumn` and `LazyRow` use the item position as the key. For dynamic lists (e.g. `items(moodLogs)`), this causes unnecessary recompositions when items are inserted, moved, or deleted because Compose cannot smartly track the elements.
**Action:** Always provide a stable, unique identifier to the `key` parameter in dynamic lists (e.g., `items(list, key = { it.id })`) to ensure smart element reuse and prevent full-list re-renders.
