package com.example.ui.copy

/** User-facing language for the five primary jobs in FloraFlow. */
enum class AppDestination(val label: String, val accessibilityLabel: String = label) {
    TODAY("Today"),
    PLAN("Plan"),
    MY_GARDEN("My Garden"),
    COUNSEL("Counsel", "Garden Counsel"),
    RESTORATION("Restoration")
}
