## 2024-06-18 - Jetpack Compose Lazy List Performance
**Learning:** By default, Compose uses item position as the key for elements in Lazy lists (`items()`). When items are added, removed, or reordered, this can cause unnecessary re-renders of the entire list.
**Action:** Always provide a stable, unique identifier (e.g., `key = { it.id }`) to the `items()` parameter in `LazyColumn` and `LazyRow` to allow Compose to smartly reuse elements and skip re-renders.
## 2024-06-21 - Gradle Java Home configuration
**Learning:** If `gradle.properties` contains a hardcoded Windows path for `org.gradle.java.home` (e.g., `F:/Android Studio/jbr`), it will cause build failures in Linux/sandbox environments.
**Action:** Always check `gradle.properties` and comment out or remove hardcoded `org.gradle.java.home` values before running Gradle commands if you encounter Java home errors.
