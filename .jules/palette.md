## 2024-05-24 - Layout Deletion Confirmation
**Learning:** A critical destructive action (deleting a garden layout) lacked a confirmation dialog, making it prone to accidental data loss. This is a severe UX flaw, especially when users might invest significant effort into creating a layout.
**Action:** Added an `AlertDialog` to explicitly ask for confirmation before calling `viewModel.deleteLayout`. This should be a standard pattern for all delete actions.
