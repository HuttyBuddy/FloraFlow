## 2024-07-20 - Sensitive Data Exposure via Logging
**Vulnerability:** Application logs were exposing internal API state and potential failure messages unrestrictedly.
**Learning:** Even diagnostic logs can leak sensitive backend URL parameters or state configurations if they aren't stripped from production builds.
**Prevention:** Always wrap diagnostic `Log.e` or `Log.d` statements with `if (BuildConfig.DEBUG)` using the application's BuildConfig to ensure they are compiled out or skipped in release builds.
