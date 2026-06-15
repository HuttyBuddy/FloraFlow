## 2024-05-24 - Unmasked Sensitive Payment Input
**Vulnerability:** CVC input field in BillingDialog lacked visual masking and used a standard number keyboard, exposing sensitive data to shoulder-surfing and OS keyboard caching.
**Learning:** Jetpack Compose `OutlinedTextField` requires explicit `visualTransformation` and `keyboardType` settings to protect sensitive data; standard number inputs do not provide this protection.
**Prevention:** Always use `PasswordVisualTransformation()` and `KeyboardOptions(keyboardType = KeyboardType.NumberPassword)` for sensitive numeric inputs like CVCs, PINs, or passwords.
