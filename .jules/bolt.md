## 2024-05-24 - Compose Object Allocations in Grid Views
**Learning:** Local variable allocations inside functions called frequently during Compose rendering (like checking cell neighbors in a grid) can compound into thousands of allocations per frame, causing excessive Garbage Collection and UI stutter.
**Action:** Always extract static data structures (like maps, lists of rules) to top-level constants or `remember` them if they depend on state, so they are not recreated on every render frame.
