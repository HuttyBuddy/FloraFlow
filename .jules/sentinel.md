## 2024-06-18 - Avoid printStackTrace in production
**Vulnerability:** Information leakage through standard error stream via `e.printStackTrace()`.
**Learning:** Printing stack traces to standard error circumvents structured logging and can expose sensitive application or system state to logcat or adjacent processes.
**Prevention:** Always use Android's structured logging framework (`android.util.Log`) to record exceptions, ensuring they are properly categorized and potentially filtered in production builds.
