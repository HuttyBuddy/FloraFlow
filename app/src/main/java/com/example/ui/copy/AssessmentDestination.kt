package com.example.ui.copy

/** Routes each assessment priority to the part of FloraFlow that can act on it. */
object AssessmentDestination {
    const val TODAY = 0
    const val PLAN = 1
    const val MY_GARDEN = 2
    const val COUNSEL = 3
    const val RESTORATION = 4

    fun tabFor(category: String): Int = when (category) {
        "NATURE VIEWS", "NATURAL LIGHT", "ORGANIC FORMS" -> PLAN
        "LIVING PLANTS", "NATURAL MATERIALS", "SENSORY RICHNESS", "SEASONAL AWARENESS" -> MY_GARDEN
        "ACOUSTIC CALM", "WATER FEATURES" -> RESTORATION
        "AIR & VENTILATION" -> COUNSEL
        else -> TODAY
    }
}
