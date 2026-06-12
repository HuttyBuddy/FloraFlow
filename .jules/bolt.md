## 2026-06-12 - Prevent Static List Allocations on Recomposition
**Learning:** In Jetpack Compose, declaring standard lists like `listOf(...)` or `arrayOf(...)` inside composables or repeatedly called utility functions (like grid collision checks) allocates new objects on every frame or cycle, which can cause subtle GC stalls.
**Action:** Always extract constant filter configurations and utility rules to top-level `private val` declarations.
