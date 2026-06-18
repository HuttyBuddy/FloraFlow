## 2024-06-18 - [Secure Input for CVC]
**Vulnerability:** [CVC Code input was visible during entry and used a standard number keyboard, leaving it vulnerable to shoulder surfing and OS keyboard caching]
**Learning:** [CVC Code fields are sensitive authentication data and must be treated as passwords]
**Prevention:** [Always use PasswordVisualTransformation() and KeyboardType.NumberPassword for sensitive numeric inputs]
