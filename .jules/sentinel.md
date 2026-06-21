## 2024-05-24 - Prevent Stack Trace Exposure
**Vulnerability:** Information Leakage via Stack Trace
**Learning:** Raw stack traces exposed via e.printStackTrace() can leak sensitive implementation details and execution flow information in production environments.
**Prevention:** Use Android standard logging framework (Log.e) which can be properly managed, filtered, or stripped in release builds.
## 2026-06-20 - [Stack Trace Leakage in Android Logcat]
**Vulnerability:** Information Exposure Through Stack Trace via `e.printStackTrace()` or `android.util.Log.e("...", "...", e)`.
**Learning:** In Android, both `e.printStackTrace()` and logging exceptions directly with `Log.e(..., e)` output the full stack trace to Logcat, which can be read by other apps or attackers, leaking sensitive internal implementation details.
**Prevention:** Instead of logging the full exception, log only a safe, sanitized error message such as `e.message` (e.g., `android.util.Log.e("TAG", "Error: ${e.message}")`).
