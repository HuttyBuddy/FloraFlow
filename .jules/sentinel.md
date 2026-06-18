## 2023-10-27 - Exception Stack Trace Exposure
**Vulnerability:** Use of `printStackTrace()` in production code (ArLensScreen.kt).
**Learning:** `printStackTrace()` writes directly to standard error, bypassing the Android logging framework. This can inadvertently leak sensitive application state, internal pathways, or sensor failure details to logcat without proper level filtering.
**Prevention:** Always use the standard Android logging framework (e.g., `android.util.Log.e(...)`) to handle and report exceptions, ensuring proper log levels and tag attribution are maintained.
