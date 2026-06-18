## 2024-05-24 - Prevent Stack Trace Exposure
**Vulnerability:** Information Leakage via Stack Trace
**Learning:** Raw stack traces exposed via e.printStackTrace() can leak sensitive implementation details and execution flow information in production environments.
**Prevention:** Use Android standard logging framework (Log.e) which can be properly managed, filtered, or stripped in release builds.
