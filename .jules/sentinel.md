## 2024-05-24 - Prevent Stack Trace Exposure
**Vulnerability:** Information Leakage via Stack Trace
**Learning:** Raw stack traces exposed via e.printStackTrace() can leak sensitive implementation details and execution flow information in production environments.
**Prevention:** Use Android standard logging framework (Log.e) which can be properly managed, filtered, or stripped in release builds.
## 2026-06-20 - [Stack Trace Leakage in Android Logcat]
**Vulnerability:** Information Exposure Through Stack Trace via `e.printStackTrace()` or `android.util.Log.e("...", "...", e)`.
**Learning:** In Android, both `e.printStackTrace()` and logging exceptions directly with `Log.e(..., e)` output the full stack trace to Logcat, which can be read by other apps or attackers, leaking sensitive internal implementation details.
**Prevention:** Instead of logging the full exception, log only a safe, sanitized error message such as `e.message` (e.g., `android.util.Log.e("TAG", "Error: ${e.message}")`).
## 2024-05-24 - Logging Stack Traces in WeatherRepository
**Vulnerability:** Full exception objects passed to Android loggers.
**Learning:** Passing `e` as the last parameter to `Log.e` dumps the entire stack trace to Logcat, exposing internal application paths and potentially sensitive device state.
**Prevention:** Use string interpolation with `${e.message}` instead of passing the `Throwable` to loggers, ensuring only the sanitized error message is visible in Logcat.
## 2024-06-28 - [Stack Trace Leakage in Android Logcat for Camera Capture]
**Vulnerability:** Information Exposure Through Stack Trace via `e.printStackTrace()` when converting URI to Base64 or creating temp camera URI.
**Learning:** In Android, `e.printStackTrace()` outputs the full stack trace to Logcat. Using it for basic media/file operations like generating URIs exposes implementation details, directory structures, and application flow.
**Prevention:** Instead of logging the full exception via `e.printStackTrace()`, use `android.util.Log.e` and log only a safe, sanitized error message such as `e.message`.
