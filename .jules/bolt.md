## 2023-10-24 - N+1 Query in Room DB Auto Sow Seeds
**Learning:** Room DB operations inside a loop in Android ViewModels cause N+1 query performance bottlenecks. Sequential single inserts are substantially slower than a single transaction batch insert.
**Action:** When inserting multiple related entities simultaneously, always collect them into a list and utilize `@Insert` methods taking a `List<Entity>` in the Room DAO to execute a batch insert transaction.
