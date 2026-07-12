package com.example.ui.copy

/** Converts the first assessment priority into a practical design action. */
object AssessmentFocus {
    fun actionFor(category: String): String = when (category) {
        "NATURAL LIGHT" -> "Choose the brightest suitable spot before placing plants."
        "LIVING PLANTS" -> "Add two easy-care plants to start your living layer."
        "NATURE VIEWS" -> "Place your first plant where you will see it every day."
        "ACOUSTIC CALM" -> "Plan a quiet corner, then add a Restoration session when you are ready."
        "NATURAL MATERIALS" -> "Pair your plants with one natural texture, such as wood, stone, or clay."
        "AIR & VENTILATION" -> "Choose plants and placement that work with your space's airflow."
        "ORGANIC FORMS" -> "Use a curved arrangement to soften the edges of your space."
        "WATER FEATURES" -> "Create a small sensory corner and add a water-inspired Restoration session."
        "SENSORY RICHNESS" -> "Start with plants that add visible texture or a gentle natural scent."
        "SEASONAL AWARENESS" -> "Choose plants that match the current season and your care routine."
        else -> "Choose one small change that makes your space feel more alive."
    }
}
