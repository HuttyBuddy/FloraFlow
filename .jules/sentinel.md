## 2024-05-18 - CVC Code Plain Text Exposure in BillingDialog
**Vulnerability:** The CVC code input field in `BillingDialog.kt` used plain text, exposing sensitive information.
**Learning:** Text fields handling sensitive data must explicitly use visual transformations to mask input. The absence of `PasswordVisualTransformation` makes the code vulnerable to shoulder surfing or screen recording.
**Prevention:** Always verify that input fields for passwords, CVCs, or other sensitive information implement `PasswordVisualTransformation()`.
