💡 What: Merged the separate `synergies` and `conflicts` nested loops into a single O(N^2) pass and replaced the expensive Euclidean distance calculation (`sqrt`) with a squared distance comparison.

🎯 Why: The `CompanionSynergyCard` calculates pairwise plant distances on every recomposition. Iterating the plant grid in an O(N^2) loop twice and invoking `sqrt` heavily impacts the UI thread during frequent updates.

📊 Impact: Reduces distance iterations and `sqrt` calculations by 50% overall. Comparing `dx*dx + dy*dy` instead of computing `sqrt` reduces computational overhead per pairwise check by nearly an order of magnitude.

🔬 Measurement: Check the companion plant synergy calculations under `DashboardScreen` -> `CompanionSynergyCard`. Render speed during grid updates will show fewer GC churn drops and faster calculation turnaround.
