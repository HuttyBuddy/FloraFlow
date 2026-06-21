## 2026-06-18 - Add clear CTA to empty search results\n**Learning:** Users can get stuck in a 'no results' state when applying multiple filters without an obvious way to reset them all at once. This empty state is a critical point for user recovery.\n**Action:** Always provide a clear, one-tap 'Clear Filters' CTA within any empty state that is triggered by search or filtering logic, rather than just suggesting it in text.

## 2026-06-21 - Dynamic contentDescription for Toggle Buttons
**Learning:** Toggleable icons (like expand/collapse) often have static 'contentDescription' strings that become inaccurate when the state changes, confusing screen reader users.
**Action:** Dynamically update 'contentDescription' based on component state to provide accurate screen reader announcements (e.g., read 'Collapse details' when already expanded).
