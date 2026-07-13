package com.example.ui.copy

/** Practical, non-medical guidance shown after a biophilic space assessment. */
object AssessmentStepCopy {
    fun detailFor(category: String, score: Int): String {
        val scorePrefix = "You scored $score/2 on ${category.lowercase().replaceFirstChar { it.uppercase() }}. "
        val action = when (category) {
            "NATURE VIEWS" -> "Clear visual blockages or place a plant in your direct line of sight."
            "LIVING PLANTS" -> "Add two or three easy-care plants to the area you use most."
            "NATURAL LIGHT" -> "Move your desk or sitting area toward the brightest suitable natural light."
            "ACOUSTIC CALM" -> "Create a quieter pause with a calmer seating zone or optional nature sounds."
            "NATURAL MATERIALS" -> "Bring in one natural texture, such as wood, a woven fiber, or a clay planter."
            "AIR & VENTILATION" -> "Build a fresh-air rhythm with a window break or a gentle fan."
            "ORGANIC FORMS" -> "Add a curved pot, planter, path, or botanical print to soften sharp angles."
            "WATER FEATURES" -> "Try a subtle water feature or rainfall sounds if they suit your space."
            "SENSORY RICHNESS" -> "Add a natural scent or tactile texture that makes the space feel personal."
            "SEASONAL AWARENESS" -> "Choose a seasonal plant or small ritual that connects the space to the time of year."
            else -> return "Choose one small biophilic change to make your space feel more alive this week."
        }
        return scorePrefix + action
    }
}
