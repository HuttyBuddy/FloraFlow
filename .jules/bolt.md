## 2026-06-18 - Room Database N+1 Batch Insertion Optimization
**Learning:** Inserting records sequentially within a loop using Room Database causes significant N+1 performance bottlenecks due to repeated I/O transactions.
**Action:** Always accumulate entities in a collection and use a batch `@Insert` DAO method (e.g., `insertPlants(plants: List<Plant>)`) when processing and saving multiple records simultaneously.
