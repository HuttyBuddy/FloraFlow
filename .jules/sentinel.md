## 2025-02-28 - Secure Logging Configuration
**Vulnerability:** Sensitive error messages were being logged to logcat in production builds via `Log.d` and `Log.e` in background workers.
**Learning:** Background workers and viewmodels often contain exception handling that logs full error details for debugging. If left unguarded, this leaks internal application state, stack traces, or potentially user data into the system log.
**Prevention:** Always wrap potentially sensitive Android `Log` statements in `if (com.example.BuildConfig.DEBUG)` checks to ensure they are stripped from release builds, as Android's default ProGuard rules do not automatically remove all log calls.
