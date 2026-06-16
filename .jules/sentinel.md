## 2024-05-18 - CVC Input Exposed
**Vulnerability:** The CVC code input in the Billing dialog (`BillingDialog.kt`) lacked visual transformation, displaying sensitive numeric data as plaintext during input.
**Learning:** Even internal or sandbox billing dialogs require full production-level security for sensitive fields to prevent shoulder surfing or OS caching, as `KeyboardType.Number` alone is insufficient.
**Prevention:** Always use `PasswordVisualTransformation()` and `KeyboardType.NumberPassword` (or `KeyboardType.Password`) for CVC, CVV, passwords, and sensitive keys in Compose forms.
