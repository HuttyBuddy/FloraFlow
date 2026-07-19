package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.Content
import com.example.data.api.GeminiApiClient
import com.example.data.api.Part
import com.example.data.api.InlineData
import com.example.data.database.GardenDatabase
import com.example.data.model.*
import com.example.data.repository.GardenRepository
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import android.content.Intent
import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.os.Build
import com.example.ui.screens.checkPlantSynergy
import com.example.billing.BillingManager
import com.example.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class WalkthroughStep {
    WELCOME,
    DASHBOARD_GARDEN,
    DASHBOARD_STATS,
    PLANNER_TAB,
    AI_ADVISOR_TAB,
    AR_LENS_TAB
}

data class ScreenRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = left + width / 2
    val centerY: Float get() = top + height / 2
}

class GardenViewModel @JvmOverloads constructor(
    application: Application,
    database: GardenDatabase? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {

    private val repository: GardenRepository
    private val weatherRepository: com.example.data.repository.WeatherRepository
    private val careScheduler: com.example.ui.screens.dashboard.CareScheduler

    // Reactive database streams
    val allLayouts: StateFlow<List<GardenLayout>>
    val allMoodLogs: StateFlow<List<MoodLog>>
    val allCareTasks: StateFlow<List<CareTask>>
    val pendingCareTasks: StateFlow<List<CareTask>>
    val currentWeather: StateFlow<com.example.data.repository.WeatherInfo>
    val allAssessmentResults: StateFlow<List<AssessmentResult>>

    // Active selection states
    private val _activeLayout = MutableStateFlow<GardenLayout?>(null)
    val activeLayout: StateFlow<GardenLayout?> = _activeLayout.asStateFlow()

    private val _activePlants = MutableStateFlow<List<Plant>>(emptyList())
    val activePlants: StateFlow<List<Plant>> = _activePlants.asStateFlow()

    // Real-time AI chat stream
    private val _aiChatHistory = MutableStateFlow<List<Content>>(emptyList())
    val aiChatHistory: StateFlow<List<Content>> = _aiChatHistory.asStateFlow()

    private val _aiStatus = MutableStateFlow("")
    val aiStatus: StateFlow<String> = _aiStatus.asStateFlow()

    private val _isAiLoading = MutableStateFlow(value = false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Mock AR Lens Placement States
    private val _arPlacedPlants = MutableStateFlow<List<ArPlantPlacement>>(emptyList())
    val arPlacedPlants: StateFlow<List<ArPlantPlacement>> = _arPlacedPlants.asStateFlow()

    // Premium Global Season State
    private val _currentSeason = MutableStateFlow<String>("Summer")
    val currentSeason: StateFlow<String> = _currentSeason.asStateFlow()

    fun setCurrentSeason(season: String) {
        _currentSeason.value = season
    }

    // Premium subscription state for Devil's Advocate paywall demo
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _restorationTrialCount = MutableStateFlow(0)
    val restorationTrialCount: StateFlow<Int> = _restorationTrialCount.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow(false)
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    fun completeOnboarding() {
        _isOnboardingCompleted.value = true
        sharedPrefs.edit { putBoolean("onboarding_completed", true) }
    }

    // Biophilic Assessment Results Store
    private val _assessmentScore = MutableStateFlow<Int?>(null)
    val assessmentScore: StateFlow<Int?> = _assessmentScore.asStateFlow()

    private val _lowestCategories = MutableStateFlow<List<String>>(emptyList())
    val lowestCategories: StateFlow<List<String>> = _lowestCategories.asStateFlow()

    private val _isAssessmentSkipped = MutableStateFlow(false)
    val isAssessmentSkipped: StateFlow<Boolean> = _isAssessmentSkipped.asStateFlow()

    // Botanical Database Search Query State (Phase 2)
    private val _librarySearchQuery = MutableStateFlow("")
    val librarySearchQuery: StateFlow<String> = _librarySearchQuery.asStateFlow()

    fun setLibrarySearchQuery(query: String) {
        _librarySearchQuery.value = query
    }

    // --- Smart Predictive Search System ---
    data class SmartSearchSuggestion(
        val query: String,
        val displayText: String,
        val category: String,
        val explanation: String
    )

    private val _lastVisitedTab = MutableStateFlow<Int?>(null)
    val lastVisitedTab: StateFlow<Int?> = _lastVisitedTab.asStateFlow()

    private val _lastSelectedSubstrate = MutableStateFlow<String?>(null)
    val lastSelectedSubstrate: StateFlow<String?> = _lastSelectedSubstrate.asStateFlow()

    private val _lastSelectedSeed = MutableStateFlow<String?>(null)
    val lastSelectedSeed: StateFlow<String?> = _lastSelectedSeed.asStateFlow()

    private val _lastViewedSpecies = MutableStateFlow<String?>(null)
    val lastViewedSpecies: StateFlow<String?> = _lastViewedSpecies.asStateFlow()

    private val _lastAiKeywords = MutableStateFlow<String?>(null)
    val lastAiKeywords: StateFlow<String?> = _lastAiKeywords.asStateFlow()

    fun recordVisitedScreen(tabIndex: Int) {
        _lastVisitedTab.value = tabIndex
        sharedPrefs.edit { putInt("smart_search_last_tab", tabIndex) }
    }

    fun recordSubstrateSelected(substrateName: String) {
        val cleaned = substrateName.replace(Regex("[^a-zA-Z\\s]"), "").trim()
        _lastSelectedSubstrate.value = cleaned
        sharedPrefs.edit { putString("smart_search_last_substrate", cleaned) }
    }

    fun recordSeedSelected(seedName: String) {
        _lastSelectedSeed.value = seedName
        sharedPrefs.edit { putString("smart_search_last_seed", seedName) }
    }

    fun recordSpeciesViewed(speciesName: String) {
        _lastViewedSpecies.value = speciesName
        sharedPrefs.edit { putString("smart_search_last_viewed", speciesName) }
    }

    fun recordAiQuery(query: String) {
        val keywords = when {
            query.contains("pest", ignoreCase = true) || query.contains("bug", ignoreCase = true) || query.contains("disease", ignoreCase = true) -> "Pest Control"
            query.contains("water", ignoreCase = true) || query.contains("dry", ignoreCase = true) -> "Watering"
            query.contains("soil", ignoreCase = true) || query.contains("repot", ignoreCase = true) -> "Soil Care"
            query.contains("prune", ignoreCase = true) || query.contains("trim", ignoreCase = true) -> "Pruning"
            else -> {
                val match = com.example.data.model.ClimatePlants.ALL_TEMPLATES.find { 
                    query.contains(it.name, ignoreCase = true) 
                }
                match?.name
            }
        }
        if (keywords != null) {
            _lastAiKeywords.value = keywords
            sharedPrefs.edit { putString("smart_search_last_ai", keywords) }
        }
    }

    fun clearLearningHistory() {
        _lastVisitedTab.value = null
        _lastSelectedSubstrate.value = null
        _lastSelectedSeed.value = null
        _lastViewedSpecies.value = null
        _lastAiKeywords.value = null
        sharedPrefs.edit {
            remove("smart_search_last_tab")
            remove("smart_search_last_substrate")
            remove("smart_search_last_seed")
            remove("smart_search_last_viewed")
            remove("smart_search_last_ai")
        }
    }

    private val _firstBloomTrigger = MutableStateFlow<String?>(null)
    val firstBloomTrigger: StateFlow<String?> = _firstBloomTrigger.asStateFlow()
    fun clearFirstBloomTrigger() { _firstBloomTrigger.value = null }

    // Step completion tracking
    private val _step1Completed = MutableStateFlow(false)
    val step1Completed: StateFlow<Boolean> = _step1Completed.asStateFlow()

    private val _step2Completed = MutableStateFlow(false)
    val step2Completed: StateFlow<Boolean> = _step2Completed.asStateFlow()

    private val _step3Completed = MutableStateFlow(false)
    val step3Completed: StateFlow<Boolean> = _step3Completed.asStateFlow()

    fun toggleStepCompleted(index: Int) {
        val key = "step_${index}_completed"
        val currentVal = sharedPrefs.getBoolean(key, false)
        val newVal = !currentVal
        sharedPrefs.edit { putBoolean(key, newVal) }
        when (index) {
            1 -> _step1Completed.value = newVal
            2 -> _step2Completed.value = newVal
            3 -> _step3Completed.value = newVal
        }
        
        refreshWidget()
    }

    // 30-Day Reassessment State
    private val _needsReassessment = MutableStateFlow(false)
    val needsReassessment: StateFlow<Boolean> = _needsReassessment.asStateFlow()

    private val _simulate30Days = MutableStateFlow(false)
    val simulate30Days: StateFlow<Boolean> = _simulate30Days.asStateFlow()

    fun toggleSimulation30Days() {
        val currentSim = _simulate30Days.value
        val newSim = !currentSim
        _simulate30Days.value = newSim
        sharedPrefs.edit { putBoolean("simulate_30_days", newSim) }
        updateReassessmentState()
    }

    fun snoozeReassessment() {
        val now = System.currentTimeMillis()
        sharedPrefs.edit {
            putLong("reassessment_snooze_timestamp", now)
        }
        updateReassessmentState()
    }

    private fun updateReassessmentState() {
        val lastAssessment = sharedPrefs.getLong("last_assessment_timestamp", 0L)
        val snoozeTimestamp = sharedPrefs.getLong("reassessment_snooze_timestamp", 0L)
        val isSimulating = sharedPrefs.getBoolean("simulate_30_days", false)

        val thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000
        val oneDayMillis = 24L * 60 * 60 * 1000
        val now = System.currentTimeMillis()

        val isThirtyDaysPast = (now - lastAssessment) >= thirtyDaysMillis
        val isSnoozed = (now - snoozeTimestamp) < oneDayMillis

        _needsReassessment.value = (isSimulating || (lastAssessment > 0 && isThirtyDaysPast)) && !isSnoozed
    }

    // Daily Reflection State
    private val _dailyReflection = MutableStateFlow("")
    val dailyReflection: StateFlow<String> = _dailyReflection.asStateFlow()

    private val _selectedMood = MutableStateFlow("Peaceful")
    val selectedMood: StateFlow<String> = _selectedMood.asStateFlow()

    fun updateDailyReflection(text: String) {
        _dailyReflection.value = text
    }

    fun updateSelectedMood(mood: String) {
        _selectedMood.value = mood
    }

    // Combine all learning signals to produce a list of smart search suggestions
    val smartSearchSuggestions: StateFlow<List<SmartSearchSuggestion>> = combine(
        _lastVisitedTab,
        _lastSelectedSubstrate,
        _lastSelectedSeed,
        _lastViewedSpecies,
        _lastAiKeywords,
        _activePlants,
        _assessmentScore,
        _lowestCategories,
        _selectedMood,
        _currentSeason
    ) { args ->
        val lastTab = args[0] as? Int
        val lastSubstrate = args[1] as? String
        val lastSeed = args[2] as? String
        val lastViewed = args[3] as? String
        val lastAi = args[4] as? String
        val activePlantsList = args[5] as List<Plant>
        val score = args[6] as? Int
        val categoriesList = args[7] as? List<String> ?: emptyList()
        val mood = args[8] as String
        val season = args[9] as String

        val suggestions = mutableListOf<SmartSearchSuggestion>()

        // 1. Companion / Care Recommendations from last sowed seed
        if (lastSeed != null) {
            suggestions.add(
                SmartSearchSuggestion(
                    query = lastSeed,
                    displayText = "$lastSeed Care",
                    category = "Recent Sowing",
                    explanation = "Read full soil, water, and sunlight guidelines for your newly sowed $lastSeed."
                )
            )
            // Companion suggestions
            if (lastSeed.equals("Tomato", ignoreCase = true)) {
                suggestions.add(
                    SmartSearchSuggestion(
                        query = "Basil",
                        displayText = "Basil (Companion)",
                        category = "Companion Fit",
                        explanation = "Basil repels pests and enhances the flavor of Tomato when grown nearby."
                    )
                )
                suggestions.add(
                    SmartSearchSuggestion(
                        query = "Marigold",
                        displayText = "Marigold (Companion)",
                        category = "Companion Fit",
                        explanation = "Marigolds act as a natural guard against tomato pests when planted near Tomato."
                    )
                )
            } else if (lastSeed.equals("Snake Plant", ignoreCase = true)) {
                suggestions.add(
                    SmartSearchSuggestion(
                        query = "ZZ Plant",
                        displayText = "ZZ Plant (Companion)",
                        category = "Companion Fit",
                        explanation = "ZZ Plants share similar low-watering, low-light requirements as Snake Plants."
                    )
                )
            }
        }

        // 2. Substrate-driven recommendations
        if (lastSubstrate != null) {
            when {
                lastSubstrate.contains("Sand", ignoreCase = true) -> {
                    suggestions.add(
                        SmartSearchSuggestion(
                            query = "Succulent",
                            displayText = "Sandy Succulents",
                            category = "Substrate Fit",
                            explanation = "Succulents and cacti are perfectly suited for coarse, fast-draining Sand substrate."
                        )
                    )
                    suggestions.add(
                        SmartSearchSuggestion(
                            query = "Snake Plant",
                            displayText = "Snake Plant",
                            category = "Substrate Fit",
                            explanation = "Snake Plants thrive in sandy potting mixes that prevent waterlogging."
                        )
                    )
                }
                lastSubstrate.contains("Clay", ignoreCase = true) -> {
                    suggestions.add(
                        SmartSearchSuggestion(
                            query = "Flower",
                            displayText = "Clay-Tolerant Blooms",
                            category = "Substrate Fit",
                            explanation = "Some flowering perennials enjoy the moisture retention of clay soils."
                        )
                    )
                    suggestions.add(
                        SmartSearchSuggestion(
                            query = "Fern",
                            displayText = "Moisture-Loving Ferns",
                            category = "Substrate Fit",
                            explanation = "Ferns thrive in dense, moisture-retentive substrates like clay."
                        )
                    )
                }
                lastSubstrate.contains("Loam", ignoreCase = true) -> {
                    suggestions.add(
                        SmartSearchSuggestion(
                            query = "Shrub",
                            displayText = "Loam-Loving Shrubs",
                            category = "Substrate Fit",
                            explanation = "Tilled loam is the gold standard for high-nutrient shrubs and herbs."
                        )
                    )
                }
            }
        }

        // 3. Navigation History / Screen Context
        if (lastTab != null) {
            when (lastTab) {
                4 -> { // Restoration
                    suggestions.add(
                        SmartSearchSuggestion(
                            query = "Lavender",
                            displayText = "Relaxing Lavender",
                            category = "Restoration Boost",
                            explanation = "Since you visited Restoration: Lavender is clinically proven to lower stress."
                        )
                    )
                    suggestions.add(
                        SmartSearchSuggestion(
                            query = "Peace Lily",
                            displayText = "Calming Peace Lily",
                            category = "Restoration Boost",
                            explanation = "Peace Lilies introduce soothing green views that help you decompress."
                        )
                    )
                }
                3 -> { // Counsel
                    if (lastAi != null) {
                        if (lastAi.equals("Pest Control", ignoreCase = true)) {
                            suggestions.add(
                                SmartSearchSuggestion(
                                    query = "Pest Resistant",
                                    displayText = "Pest-Resistant Species",
                                    category = "Counsel Insight",
                                    explanation = "Learn which plant species naturally resist common garden pests and diseases."
                                )
                            )
                        } else {
                            suggestions.add(
                                SmartSearchSuggestion(
                                    query = lastAi,
                                    displayText = "$lastAi Info",
                                    category = "Counsel Insight",
                                    explanation = "Explore more botanical facts about $lastAi following your AI chat."
                                )
                            )
                        }
                    }
                }
            }
        }

        // 4. Mood-driven recommendations
        if (mood.equals("Stressed", ignoreCase = true) || mood.equals("Overwhelmed", ignoreCase = true)) {
            suggestions.add(
                SmartSearchSuggestion(
                    query = "Indoor Area",
                    displayText = "Sensory Sanctuary",
                    category = "Mindful Fit",
                    explanation = "Create an indoor sensory sanctuary to reduce stress and anxiety."
                )
            )
        }

        // 5. Climate matching (Mediterranean, Arid, Tropical, Temperate, Mountainous)
        val resolvedClimate = activeLayout.value?.climate ?: "Temperate"
        
        when {
            resolvedClimate.contains("Arid", ignoreCase = true) -> {
                suggestions.add(
                    SmartSearchSuggestion(
                        query = "Succulent",
                        displayText = "Arid Species",
                        category = "Climate Match",
                        explanation = "Matches your garden's Arid climate compatibility. Requires very low watering."
                    )
                )
            }
            resolvedClimate.contains("Mediterranean", ignoreCase = true) -> {
                suggestions.add(
                    SmartSearchSuggestion(
                        query = "Rosemary",
                        displayText = "Mediterranean Herbs",
                        category = "Climate Match",
                        explanation = "Adapted to dry, warm summers like your garden's Mediterranean climate."
                    )
                )
            }
            resolvedClimate.contains("Tropical", ignoreCase = true) -> {
                suggestions.add(
                    SmartSearchSuggestion(
                        query = "Fern",
                        displayText = "Tropical Ferns",
                        category = "Climate Match",
                        explanation = "Humid-loving plants that thrive in warm, tropical climates."
                    )
                )
            }
        }

        // 6. Biophilic Gaps from Assessment
        if (score != null && score < 15) {
            val airQualityLow = categoriesList.any { it.contains("AIR", ignoreCase = true) || it.contains("VENTILATION", ignoreCase = true) }
            if (airQualityLow) {
                suggestions.add(
                    SmartSearchSuggestion(
                        query = "Snake Plant",
                        displayText = "Air Purifying Snake Plant",
                        category = "Biophilic Gap",
                        explanation = "Boosts oxygen production at night to address your low room ventilation score."
                    )
                )
                suggestions.add(
                    SmartSearchSuggestion(
                        query = "Peace Lily",
                        displayText = "Air Filtering Peace Lily",
                        category = "Biophilic Gap",
                        explanation = "Tackles airborne toxins to improve your room's air quality zone."
                    )
                )
            }
        }

        // 7. View History
        if (lastViewed != null) {
            suggestions.add(
                SmartSearchSuggestion(
                    query = lastViewed,
                    displayText = "Re-explore $lastViewed",
                    category = "Recently Viewed",
                    explanation = "Jump back into the care guide for your recently inspected $lastViewed."
                )
            )
        }

        // Ensure we always have unique suggestions and filter out duplicate queries
        val uniqueSuggestions = suggestions.distinctBy { it.query.lowercase() }
        
        // Add defaults if empty or too short
        val finalSuggestions = uniqueSuggestions.toMutableList()
        val defaultPool = listOf(
            SmartSearchSuggestion("Snake Plant", "Snake Plant", "Popular Search", "A robust air-purifier that thrives on neglect."),
            SmartSearchSuggestion("Peace Lily", "Peace Lily", "Popular Search", "Beautiful flowering plant that dramatically droops when thirsty."),
            SmartSearchSuggestion("ZZ Plant", "ZZ Plant", "Popular Search", "Vibrant, low-light houseplant with thick waxy leaves."),
            SmartSearchSuggestion("Swiss Cheese Plant", "Monstera", "Popular Search", "A climbing rainforest plant with iconic split leaves.")
        )
        for (item in defaultPool) {
            if (finalSuggestions.size >= 5) break
            if (finalSuggestions.none { it.query.lowercase() == item.query.lowercase() }) {
                finalSuggestions.add(item)
            }
        }

        finalSuggestions.take(5)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dynamicSearchPlaceholder: StateFlow<String> = combine(
        smartSearchSuggestions,
        _lastSelectedSeed
    ) { suggestions, lastSeed ->
        if (lastSeed != null) {
            "Companion for $lastSeed..."
        } else if (suggestions.isNotEmpty()) {
            "Try: '${suggestions.first().displayText}'..."
        } else {
            "Search 50+ species..."
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Search 50+ species...")

    fun saveRestorationLog(nriScore: Int, completedTasks: List<String>) {
        viewModelScope.launch(ioDispatcher) {
            val log = RestorationLog(
                timestamp = System.currentTimeMillis(),
                nriScore = nriScore,
                layoutId = activeLayout.value?.id,
                completedTasks = completedTasks.joinToString(","),
                soundscapeTrack = currentSoundscapeTrack.value
            )
            repository.insertRestorationLog(log)
        }
    }

    // Conversational Space Diagnosis Mode
    private val _isSpaceDiagnosisMode = MutableStateFlow(false)
    val isSpaceDiagnosisMode: StateFlow<Boolean> = _isSpaceDiagnosisMode.asStateFlow()

    fun setSpaceDiagnosisMode(active: Boolean) {
        _isSpaceDiagnosisMode.value = active
    }

    fun saveAssessmentResult(score: Int, categories: List<String>) {
        val oldScore = sharedPrefs.getInt("assessment_score", -1)
        _assessmentScore.value = score
        _lowestCategories.value = categories

        // Reset step completion states
        _step1Completed.value = false
        _step2Completed.value = false
        _step3Completed.value = false

        val currentTimestamp = System.currentTimeMillis()

        sharedPrefs.edit {
            if (oldScore != -1) {
                putInt("prev_assessment_score", oldScore)
            }
            putInt("assessment_score", score)
            putString("assessment_categories", categories.joinToString(","))
            putLong("last_assessment_timestamp", currentTimestamp)
            putLong("reassessment_snooze_timestamp", 0L) // Reset snooze on retake
            putBoolean("step_1_completed", false)
            putBoolean("step_2_completed", false)
            putBoolean("step_3_completed", false)
        }

        updateReassessmentState()

        viewModelScope.launch(ioDispatcher) {
            repository.insertAssessmentResult(
                AssessmentResult(
                    score = score,
                    lowestCategories = categories.joinToString(",")
                )
            )
        }

        refreshWidget()
    }

    fun skipAssessment() {
        _isAssessmentSkipped.value = true
        sharedPrefs.edit { putBoolean("assessment_skipped", true) }
        completeOnboarding()
    }

    fun resetAssessment() {
        val currentScore = sharedPrefs.getInt("assessment_score", -1)
        _isAssessmentSkipped.value = false
        _assessmentScore.value = null
        _lowestCategories.value = emptyList()
        _isOnboardingCompleted.value = false
        sharedPrefs.edit {
            if (currentScore != -1) {
                putInt("prev_assessment_score", currentScore)
            }
            putBoolean("assessment_skipped", false)
            remove("assessment_score")
            remove("assessment_categories")
            putBoolean("onboarding_completed", false)
            remove("last_assessment_timestamp")
            putBoolean("step_1_completed", false)
            putBoolean("step_2_completed", false)
            putBoolean("step_3_completed", false)
        }
        _step1Completed.value = false
        _step2Completed.value = false
        _step3Completed.value = false
        updateReassessmentState()
    }

    // Interactive App Walkthrough Anchors Rects mapping
    private val _walkthroughActive = MutableStateFlow(false)
    val walkthroughActive: StateFlow<Boolean> = _walkthroughActive.asStateFlow()

    private val _currentWalkthroughStep = MutableStateFlow<WalkthroughStep?>(null)
    val currentWalkthroughStep: StateFlow<WalkthroughStep?> = _currentWalkthroughStep.asStateFlow()

    private val _walkthroughTargets = MutableStateFlow<Map<WalkthroughStep, ScreenRect>>(emptyMap())
    val walkthroughTargets: StateFlow<Map<WalkthroughStep, ScreenRect>> = _walkthroughTargets.asStateFlow()

    fun startWalkthrough() {
        _currentWalkthroughStep.value = WalkthroughStep.WELCOME
        _walkthroughTargets.value = emptyMap()
        _walkthroughActive.value = true
    }

    fun nextWalkthroughStep() {
        val current = _currentWalkthroughStep.value ?: return
        val steps = WalkthroughStep.entries
        val nextIdx = current.ordinal + 1
        if (nextIdx < steps.size) {
            _currentWalkthroughStep.value = steps[nextIdx]
        } else {
            _currentWalkthroughStep.value = null
            _walkthroughTargets.value = emptyMap()
            _walkthroughActive.value = false
        }
    }

    fun skipWalkthrough() {
        _currentWalkthroughStep.value = null
        _walkthroughTargets.value = emptyMap()
        _walkthroughActive.value = false
    }

    fun updateWalkthroughTarget(step: WalkthroughStep, rect: ScreenRect) {
        val current = _walkthroughTargets.value.toMutableMap()
        current[step] = rect
        _walkthroughTargets.value = current
    }

    // Advanced Theme Control State
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _isDarkTheme = MutableStateFlow<Boolean?>(null)
    val isDarkTheme: StateFlow<Boolean?> = _isDarkTheme.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        _isDarkTheme.value = when (mode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> null
        }
        sharedPrefs.edit { putString("theme_mode", mode.name) }
    }

    fun toggleTheme(isSystemDark: Boolean) {
        val current = _isDarkTheme.value ?: isSystemDark
        _isDarkTheme.value = !current
    }

    // Advanced Billing State Properties
    private val _subscriptionTier = MutableStateFlow<String?>(null)
    val subscriptionTier: StateFlow<String?> = _subscriptionTier.asStateFlow()

    private val _subscriptionTransactionId = MutableStateFlow<String?>(null)
    val subscriptionTransactionId: StateFlow<String?> = _subscriptionTransactionId.asStateFlow()

    private val _subscriptionBillingDate = MutableStateFlow<String?>(null)
    val subscriptionBillingDate: StateFlow<String?> = _subscriptionBillingDate.asStateFlow()

    private val _showBillingDialog = MutableStateFlow(false)
    val showBillingDialog: StateFlow<Boolean> = _showBillingDialog.asStateFlow()

    private val _showSubscriptionManagement = MutableStateFlow(false)
    val showSubscriptionManagement: StateFlow<Boolean> = _showSubscriptionManagement.asStateFlow()

    // Offline Local Submissions Storage
    private val _feedbackSubmissions = MutableStateFlow<List<FeedbackSubmission>>(emptyList())
    val feedbackSubmissions: StateFlow<List<FeedbackSubmission>> = _feedbackSubmissions.asStateFlow()

    private val _isSubmittingFeedback = MutableStateFlow(false)
    val isSubmittingFeedback: StateFlow<Boolean> = _isSubmittingFeedback.asStateFlow()

    private val _feedbackSuccess = MutableStateFlow(false)
    val feedbackSuccess: StateFlow<Boolean> = _feedbackSuccess.asStateFlow()

    // Rate App prompt State Flow
    private val _showInAppRatePrompt = MutableStateFlow(false)
    val showInAppRatePrompt: StateFlow<Boolean> = _showInAppRatePrompt.asStateFlow()

    private val sharedPrefs = application.getSharedPreferences("floraflow_billing_prefs", Context.MODE_PRIVATE)
    private val encryptedSharedPrefs by lazy {
        // Robolectric runs on the host JVM, where the AndroidKeyStore provider does not
        // exist. Keep production feedback encrypted while giving JVM tests an isolated,
        // deterministic preference store.
        if (Build.FINGERPRINT == "robolectric") {
            return@lazy application.getSharedPreferences(
                "secret_feedback_prefs_robolectric",
                Context.MODE_PRIVATE
            )
        }

        val masterKey = MasterKey.Builder(application)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            application,
            "secret_feedback_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    val billingManager = BillingManager(application)

    // Daily free-tier AI query quota. Persisted (not chat-history-derived) so
    // clearing the chat or restarting the app can't reset it, and only
    // incremented after a successful (non-error) AI response so failed
    // requests never cost the user a query.
    val aiQueryLimit = 3
    private val _aiQueryCount = MutableStateFlow(0)
    val aiQueryCount: StateFlow<Int> = _aiQueryCount.asStateFlow()

    private fun currentAiQueryCount(): Int {
        return sharedPrefs.getInt("ai_query_count", 0)
    }

    internal fun recordAiQueryUsed() {
        val newCount = currentAiQueryCount() + 1
        sharedPrefs.edit {
            putInt("ai_query_count", newCount)
        }
        _aiQueryCount.value = newCount
    }

    fun upgradeToPremium() {
        _showBillingDialog.value = true
    }

    fun incrementRestorationTrial(): Boolean {
        if (_isPremium.value) return true
        val now = System.currentTimeMillis()
        var weekStart = sharedPrefs.getLong("restoration_trial_week_start", 0L)
        if (weekStart == 0L) {
            weekStart = now
            sharedPrefs.edit { putLong("restoration_trial_week_start", now) }
        }
        var currentCount = _restorationTrialCount.value
        if (now - weekStart >= 7 * 24 * 60 * 60 * 1000L) {
            currentCount = 0
            weekStart = now
            sharedPrefs.edit {
                putLong("restoration_trial_week_start", now)
                putInt("restoration_trial_count", 0)
            }
        }
        if (currentCount >= 3) {
            upgradeToPremium()
            return false
        }
        val newCount = currentCount + 1
        _restorationTrialCount.value = newCount
        sharedPrefs.edit {
            putInt("restoration_trial_count", newCount)
        }
        return true
    }

    fun setBillingDialogVisible(visible: Boolean, source: String = "unknown") {
        _showBillingDialog.value = visible
        if (visible) {
            com.example.analytics.AnalyticsHelper.logPaywallView(source)
        }
    }

    fun setSubscriptionManagementVisible(visible: Boolean) {
        _showSubscriptionManagement.value = visible
    }

    fun processPurchase(tier: String, price: String, isAnnual: Boolean) {
        val productId = if (isAnnual) BillingManager.PRODUCT_YEARLY else BillingManager.PRODUCT_MONTHLY
        if (BuildConfig.DEBUG) {
            billingManager.executeMockPurchase(productId)
        }
        com.example.analytics.AnalyticsHelper.logTrialStart()
        com.example.analytics.AnalyticsHelper.logPurchaseSuccess(productId)
    }

    fun cancelPremiumSubscription() {
        if (BuildConfig.DEBUG) {
            billingManager.cancelMockSubscription()
        }
    }

    fun restorePurchases(): Boolean {
        if (BuildConfig.DEBUG && billingManager.inMockMode) {
            billingManager.executeMockPurchase(BillingManager.PRODUCT_YEARLY)
        } else {
            billingManager.queryPurchases()
        }
        return true
    }

    private var soundscapeStartTime: Long = 0L

    fun toggleSoundscapePlay() {
        val service = soundscapeService ?: return
        val context = getApplication<Application>().applicationContext
        if (service.isPlaying()) {
            service.pauseSoundscape()
            _isSoundscapePlaying.value = false
            if (soundscapeStartTime > 0L) {
                val durationSecs = ((System.currentTimeMillis() - soundscapeStartTime) / 1000).toInt()
                com.example.analytics.AnalyticsHelper.logRestorationSession(durationSecs)
                soundscapeStartTime = 0L
            }
        } else {
            if (!incrementRestorationTrial()) return
            val intent = Intent(context, com.example.ui.screens.restoration.SoundscapeService::class.java).apply {
                action = "ACTION_PLAY"
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            bindSoundscapeService()
            _isSoundscapePlaying.value = true
            soundscapeStartTime = System.currentTimeMillis()
        }
    }

    // Active screen index hoist state for onboarding assessments completion callbacks
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    fun setCurrentTab(tab: Int) {
        _currentTab.value = tab
        recordVisitedScreen(tab)
    }

    init {
        // Observe billing manager flows reactively
        viewModelScope.launch {
            billingManager.isPremium.collect { _isPremium.value = it }
        }
        viewModelScope.launch {
            billingManager.subscriptionTier.collect { _subscriptionTier.value = it }
        }
        viewModelScope.launch {
            billingManager.subscriptionTransactionId.collect { _subscriptionTransactionId.value = it }
        }
        viewModelScope.launch {
            billingManager.subscriptionBillingDate.collect { _subscriptionBillingDate.value = it }
        }
        _aiQueryCount.value = currentAiQueryCount()
        val nowRest = System.currentTimeMillis()
        var weekStart = sharedPrefs.getLong("restoration_trial_week_start", 0L)
        if (weekStart == 0L) {
            weekStart = nowRest
            sharedPrefs.edit().putLong("restoration_trial_week_start", nowRest).apply()
        }
        var restorationCount = sharedPrefs.getInt("restoration_trial_count", 0)
        if (nowRest - weekStart >= 7 * 24 * 60 * 60 * 1000L) {
            restorationCount = 0
            sharedPrefs.edit().putLong("restoration_trial_week_start", nowRest).putInt("restoration_trial_count", 0).apply()
        }
        _restorationTrialCount.value = restorationCount
        _isOnboardingCompleted.value = sharedPrefs.getBoolean("onboarding_completed", false)
        _isAssessmentSkipped.value = sharedPrefs.getBoolean("assessment_skipped", false)

        val savedScore = sharedPrefs.getInt("assessment_score", -1)
        if (savedScore != -1) {
            _assessmentScore.value = savedScore
        }
        val savedCategories = sharedPrefs.getString("assessment_categories", null)
        if (savedCategories != null) {
            _lowestCategories.value = savedCategories.split(",").filter { it.isNotBlank() }
        }

        _lastVisitedTab.value = if (sharedPrefs.contains("smart_search_last_tab")) sharedPrefs.getInt("smart_search_last_tab", 0) else null
        _lastSelectedSubstrate.value = sharedPrefs.getString("smart_search_last_substrate", null)
        _lastSelectedSeed.value = sharedPrefs.getString("smart_search_last_seed", null)
        _lastViewedSpecies.value = sharedPrefs.getString("smart_search_last_viewed", null)
        _lastAiKeywords.value = sharedPrefs.getString("smart_search_last_ai", null)

        _step1Completed.value = sharedPrefs.getBoolean("step_1_completed", false)
        _step2Completed.value = sharedPrefs.getBoolean("step_2_completed", false)
        _step3Completed.value = sharedPrefs.getBoolean("step_3_completed", false)
        _simulate30Days.value = sharedPrefs.getBoolean("simulate_30_days", false)
        updateReassessmentState()

        val savedTheme = sharedPrefs.getString("theme_mode", "SYSTEM")
        val mode = try { ThemeMode.valueOf(savedTheme ?: "SYSTEM") } catch (_: Exception) { ThemeMode.SYSTEM }
        _themeMode.value = mode
        _isDarkTheme.value = when (mode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> null
        }

        // Migrate old feedback if exists
        val oldFeedback = sharedPrefs.getString("feedback_submissions_list", null)
        if (oldFeedback != null) {
            encryptedSharedPrefs.edit { putString("feedback_submissions_list", oldFeedback) }
            sharedPrefs.edit { remove("feedback_submissions_list") }
        }

        val savedFeedback = encryptedSharedPrefs.getString("feedback_submissions_list", null)
        if (savedFeedback != null) {
            val list = savedFeedback.split("###").mapNotNull { FeedbackSubmission.fromSerializedString(it) }
            _feedbackSubmissions.value = list
        }

        val db = database ?: GardenDatabase.getDatabase(application)
        repository = GardenRepository(db.gardenDao())
        weatherRepository = com.example.data.repository.WeatherRepository(application)
        careScheduler = com.example.ui.screens.dashboard.CareScheduler(application, repository, weatherRepository)
        currentWeather = weatherRepository.currentWeather

        allCareTasks = repository.allCareTasks
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        pendingCareTasks = repository.pendingCareTasks
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        allLayouts = repository.allLayouts
            .map { list ->
                list.map { layout ->
                    if (layout.name == "My First Zen Space" || layout.name == "My First Indoor Space") {
                        layout.copy(name = "My First Space")
                    } else {
                        layout
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

        allMoodLogs = repository.allMoodLogs
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )


        allAssessmentResults = repository.allAssessmentResults
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

        viewModelScope.launch(ioDispatcher) {
            try {
                // Fetch fresh weather on startup using stored zip
                val currentZip = weatherRepository.getUserLocationZip()
                weatherRepository.fetchWeather(currentZip)
                careScheduler.syncCareSchedules()

                val existing = repository.allLayouts.firstOrNull() ?: emptyList()
                if (existing.isEmpty()) {
                    val defaultLayout = GardenLayout(
                        name = "My First Space",
                        style = "Indoor Area",
                        climate = "Temperate",
                        gridString = "0,0,Bonsai Juniper|4,4,English Lavender",
                    )
                    val layoutId = repository.insertLayout(defaultLayout).toInt()

                    repository.insertPlants(listOf(
                        Plant(
                            layoutId = layoutId,
                            name = "Bonsai Juniper",
                            type = "Tree",
                            careSpring = "Prune branches to maintain classic indoor shape. Water regularly.",
                            careSummer = "Keep in partial shade during intense afternoon sun. Water daily.",
                            careAutumn = "Let foliage change naturally. Clear fallen leaves quickly.",
                            careWinter = "Protect roots from deep freeze. Keep compost moist but not wet.",
                            soilType = "Rich organic clay loam",
                            sunlight = "Partial shade",
                            growthProgress = 40,
                            matureSize = "Small (1-2 ft)",
                            wateringNeeds = "High",
                            bloomTime = "Early Spring",
                            pestsDiseases = "Scale insects, Root rot"
                        ),
                        Plant(
                            layoutId = layoutId,
                            name = "English Lavender",
                            type = "Flower",
                            careSpring = "Cut back old gray woody parts to encourage green growth.",
                            careSummer = "Water minimal. Enjoys full direct hot sun.",
                            careAutumn = "Prune flowers post-bloom to prevent legginess.",
                            careWinter = "Enjoys dry root bed. Extremely frost hardy.",
                            soilType = "Sandy, highly well-draining grit",
                            sunlight = "Full Sun",
                            growthProgress = 60,
                            matureSize = "Medium (2 ft)",
                            wateringNeeds = "Low",
                            bloomTime = "Mid Summer",
                            pestsDiseases = "Spittlebugs, Damp-off rots"
                        )
                    ))

                    repository.insertMoodLog(
                        MoodLog(
                            mood = "Serene",
                            moodScore = 4,
                            activityMinutes = 30,
                            notes = "Had a wonderful morning watering the garden.",
                            growthIndex = 30,
                            waterCompleted = true
                        )
                    )
                    repository.insertMoodLog(
                        MoodLog(
                            mood = "Refreshed",
                            moodScore = 5,
                            activityMinutes = 45,
                            notes = "Breathed fresh air and spent quality time under the sun.",
                            growthIndex = 40,
                            outdoorsCompleted = true
                        )
                    )
                    repository.insertMoodLog(
                        MoodLog(
                            mood = "Calm",
                            moodScore = 5,
                            activityMinutes = 40,
                            notes = "Very restful and grounding companion planting session.",
                            growthIndex = 50,
                            pruneCompleted = true,
                            outdoorsCompleted = true
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("GardenViewModel", "Failed to initialize default data: ${e.message}")
            }
        }

        viewModelScope.launch {
            allLayouts.collect { layouts ->
                // Just picks an active layout on startup. Must NOT rewrite
                // its climate here — _activeLayout resets to null on every
                // process restart, so doing so previously clobbered a
                // deliberately-chosen climate every time the app relaunched.
                if ((_activeLayout.value == null) && layouts.isNotEmpty()) {
                    _activeLayout.value = layouts.first()
                }
            }
        }

        viewModelScope.launch {
            activeLayout.collectLatest { layout ->
                if (layout != null) {
                    repository.getPlantsForLayout(layout.id).collect { plants ->
                        _activePlants.value = plants
                    }
                } else {
                    _activePlants.value = emptyList()
                }
            }
        }
    }

    // --- Garden Layout operations ---
    // Simply switches which layout is active. Does NOT touch climate — a
    // layout's climate is set explicitly at creation (createLayout) or when
    // the user deliberately changes their weather location
    // (updateWeatherLocation). Selecting/viewing a layout must never
    // silently rewrite a climate the user chose on purpose (e.g. a
    // deliberately "Tropical" indoor layout while living in a temperate zip).
    fun selectLayout(layout: GardenLayout?) {
        _activeLayout.value = layout
    }

    fun createLayout(name: String, style: String, climate: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val defaultGrid = ""
            val newLayout = GardenLayout(
                name = name,
                style = style,
                climate = climate,
                gridString = defaultGrid
            )
            val layoutId = repository.insertLayout(newLayout).toInt()
            val created = newLayout.copy(id = layoutId)
            
            val templates = ClimatePlants.getTemplatesForClimate(climate).take(2)
            val initialPlants = templates.map { tpl ->
                Plant(
                    layoutId = layoutId,
                    name = tpl.name,
                    type = tpl.type,
                    careSpring = tpl.careSpring,
                    careSummer = tpl.careSummer,
                    careAutumn = tpl.careAutumn,
                    careWinter = tpl.careWinter,
                    soilType = tpl.soilType,
                    sunlight = tpl.sunlight,
                    growthProgress = 20,
                    matureSize = tpl.matureSize,
                    wateringNeeds = tpl.wateringNeeds,
                    bloomTime = tpl.bloomTime,
                    pestsDiseases = tpl.pestsDiseases
                )
            }
            repository.insertPlants(initialPlants)
            
            _activeLayout.value = created
            recordPositiveInteraction()
        }
    }

    fun deleteLayout(layout: GardenLayout) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCareTasksByLayout(layout.id)
            repository.deletePlantsByLayout(layout.id)
            repository.deleteLayout(layout)
            if (_activeLayout.value?.id == layout.id) {
                _activeLayout.value = null
            }
        }
    }

    fun placeGridPlant(x: Int, y: Int, plantName: String) {
        val current = _activeLayout.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val items = parseGridString(current.gridString).toMutableList()
            items.removeAll { (it.x == x) && (it.y == y) }
            
            if (plantName.isNotBlank() && plantName != "Empty") {
                items.add(GridPlantItem(x, y, plantName))
                recordPositiveInteraction()
            }
            val newGridString = toGridString(items)
            repository.updateLayoutGrid(current.id, newGridString)
            _activeLayout.value = current.copy(gridString = newGridString)
            saveGridSnapshot(current.id, newGridString)
            refreshWidget()
        }
    }

    fun clearLayoutGrid() {
        val current = _activeLayout.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateLayoutGrid(current.id, "")
            _activeLayout.value = current.copy(gridString = "")
            saveGridSnapshot(current.id, "")
            refreshWidget()
        }
    }

    fun autoSowClimateSeeds() {
        val current = _activeLayout.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val items = parseGridString(current.gridString).toMutableList()
            var occupiedBits = 0
            for (item in items) {
                occupiedBits = occupiedBits or (1 shl (item.x * 5 + item.y))
            }
            val available = mutableListOf<Pair<Int, Int>>()
            for (i in 0..24) {
                if ((occupiedBits and (1 shl i)) == 0) {
                    available.add(Pair(i / 5, i % 5))
                }
            }
            if (available.isEmpty()) return@launch

            val templates = ClimatePlants.getTemplatesForClimate(current.climate)
            if (templates.isEmpty()) return@launch

            val countToSow = minOf(5, available.size)
            available.shuffle()
            val slotsToSow = available.take(countToSow)

            val newPlantsToInsert = mutableListOf<Plant>()
            for (slot in slotsToSow) {
                val tpl = templates.random()
                items.add(GridPlantItem(slot.first, slot.second, tpl.name))
                
                if (_activePlants.value.none { it.name == tpl.name } && newPlantsToInsert.none { it.name == tpl.name }) {
                    newPlantsToInsert.add(
                        Plant(
                            layoutId = current.id,
                            name = tpl.name,
                            type = tpl.type,
                            careSpring = tpl.careSpring,
                            careSummer = tpl.careSummer,
                            careAutumn = tpl.careAutumn,
                            careWinter = tpl.careWinter,
                            soilType = tpl.soilType,
                            sunlight = tpl.sunlight,
                            growthProgress = 20,
                            matureSize = tpl.matureSize,
                            wateringNeeds = tpl.wateringNeeds,
                            bloomTime = tpl.bloomTime,
                            pestsDiseases = tpl.pestsDiseases
                        )
                    )
                }
            }
            if (newPlantsToInsert.isNotEmpty()) {
                // ⚡ Bolt Performance Optimization:
                // Use batch insertion to avoid the Room Database N+1 query problem.
                // This executes a single database transaction instead of multiple
                // separate transactions, reducing I/O overhead significantly.
                repository.insertPlants(newPlantsToInsert)
            }

            val newGridString = toGridString(items)
            repository.updateLayoutGrid(current.id, newGridString)
            _activeLayout.value = current.copy(gridString = newGridString)
            saveGridSnapshot(current.id, newGridString)
            refreshWidget()
        }
    }

    // --- Plants Operations ---
    fun addPlant(name: String, type: String, template: PlantTemplate?) {
        val layout = _activeLayout.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val newPlant = Plant(
                layoutId = layout.id,
                name = name,
                type = type,
                careSpring = template?.careSpring ?: "Water once a week, examine general conditions.",
                careSummer = template?.careSummer ?: "Water regularly. Monitor afternoon shade.",
                careAutumn = template?.careAutumn ?: "Weed bed, add light compost.",
                careWinter = template?.careWinter ?: "Prune spent branches, cover from deep freeze.",
                soilType = template?.soilType ?: "Well-draining loose loam",
                sunlight = template?.sunlight ?: "Partial sun to shade",
                growthProgress = 10,
                matureSize = template?.matureSize ?: "Medium (2-3 ft)",
                wateringNeeds = template?.wateringNeeds ?: "Moderate",
                bloomTime = template?.bloomTime ?: "Spring - Autumn",
                pestsDiseases = template?.pestsDiseases ?: "Aphids, Powdery Mildew"
            )
            repository.insertPlant(newPlant)
            careScheduler.syncCareSchedules()
        }
    }

    fun updatePlantProgress(plantId: Int, progress: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val plants = _activePlants.value
            val p = plants.firstOrNull { it.id == plantId } ?: return@launch
            val newProgress = progress.coerceIn(0, 100)
            repository.updatePlant(p.copy(growthProgress = newProgress))
            if (newProgress == 100 && p.growthProgress < 100) {
                _firstBloomTrigger.value = p.name
            }
        }
    }

    fun deletePlant(plantId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePlantById(plantId)
            repository.deleteCareTasksByPlant(plantId)
        }
    }

    // --- Mood Logging ---
    fun logMood(mood: String, score: Int, duration: Int, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val plants = _activePlants.value
            val averageGrowth = if (plants.isNotEmpty()) {
                plants.asSequence().map { it.growthProgress }.average().toInt()
            } else {
                50
            }

            val newLog = MoodLog(
                mood = mood,
                moodScore = score,
                activityMinutes = duration,
                notes = notes,
                growthIndex = averageGrowth
            )
            repository.insertMoodLog(newLog)
            recordPositiveInteraction()
            refreshWidget()
        }
    }

    fun deleteMoodLog(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMoodLogById(id)
            refreshWidget()
        }
    }

    fun toggleHabitForToday(habit: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val allLogs = allMoodLogs.value
            val todayLog = findTodayLog(allLogs)
            if (todayLog != null) {
                val updatedLog = when (habit.lowercase()) {
                    "water" -> todayLog.copy(waterCompleted = !todayLog.waterCompleted)
                    "prune" -> todayLog.copy(pruneCompleted = !todayLog.pruneCompleted)
                    "outdoors" -> todayLog.copy(outdoorsCompleted = !todayLog.outdoorsCompleted)
                    else -> todayLog
                }
                repository.insertMoodLog(updatedLog)
            } else {
                val plants = _activePlants.value
                val averageGrowth = if (plants.isNotEmpty()) {
                    plants.asSequence().map { it.growthProgress }.average().toInt()
                } else {
                    50
                }
                val newLog = MoodLog(
                    mood = "Refreshed",
                    moodScore = 3,
                    activityMinutes = 15,
                    notes = "",
                    growthIndex = averageGrowth,
                    waterCompleted = habit.lowercase() == "water",
                    pruneCompleted = habit.lowercase() == "prune",
                    outdoorsCompleted = habit.lowercase() == "outdoors"
                )
                repository.insertMoodLog(newLog)
                recordPositiveInteraction()
            }
            refreshWidget()
        }
    }

    private fun findTodayLog(logs: List<MoodLog>): MoodLog? {
        val todayCal = java.util.Calendar.getInstance()
        return logs.find { log ->
            val logCal = java.util.Calendar.getInstance().apply { timeInMillis = log.timestamp }
            logCal[java.util.Calendar.YEAR] == todayCal[java.util.Calendar.YEAR] &&
            logCal[java.util.Calendar.DAY_OF_YEAR] == todayCal[java.util.Calendar.DAY_OF_YEAR]
        }
    }

    // --- Real-time Gemini Client interactions ---
    fun sendAiChatMessage(message: String, imageBytesBase64: String? = null, imageMimeType: String? = "image/jpeg") {
        if (message.isBlank() && imageBytesBase64 == null) return
        recordAiQuery(message)
        
        val upsellMsg = "🔒 Free AI Advisor biophilic limit reached (3/3 queries).\n\nPlease upgrade to FloraFlow PRO to unlock unlimited conversational plant care, professional garden blueprinting, and expert AI botany diagnosis! 🌸✨"
        if (checkPremiumLimit(message, upsellMsg, aiQueryLimit)) return

        val isDiagRequest = message.contains("Space Diagnosis", ignoreCase = true) || _isSpaceDiagnosisMode.value
        if (isDiagRequest) {
            _isSpaceDiagnosisMode.value = true
        }

        val currentHistory = _aiChatHistory.value.toMutableList()
        val userParts = mutableListOf<Part>()
        if (imageBytesBase64 != null) {
            userParts.add(Part(inlineData = InlineData(mimeType = imageMimeType ?: "image/jpeg", data = imageBytesBase64)))
        }
        userParts.add(Part(text = message))
        
        currentHistory.add(Content(role = "user", parts = userParts))
        _aiChatHistory.value = currentHistory

        _isAiLoading.value = true

        viewModelScope.launch {
            val score = _assessmentScore.value
            val categories = _lowestCategories.value
            val systemIns = if (_isSpaceDiagnosisMode.value) {
                "You are the FloraFlow Space Diagnosis Assistant. Your goal is to guide the user through a friendly, step-by-step conversational audit of their room/space to determine its biophilic conditions.\n\n" +
                "INSTRUCTIONS:\n" +
                "1. If this is the start of the diagnosis (e.g. the user asks to run a detailed diagnosis), introduce yourself warmly as Dr. Julian and ask them about their space, specifically focusing on: Nature Views, Living Plants, Natural Light, Acoustic Calm, Natural Materials, Air & Ventilation, Organic Forms, Water Features, Sensory Richness, and Seasonal Awareness.\n" +
                "2. Ask them questions one by one or in a friendly, conversational group so they do not feel overwhelmed.\n" +
                "3. Once the user provides answers to all of these aspects, assess their space. Give them a biophilic score out of 20, map it to a zone (Green: 15-20, Yellow: 8-14, Red: <8), provide a brief analysis of their strengths/weaknesses, and suggest 3 highly specific biophilic improvements.\n" +
                "4. CRITICAL: In your final assessment message, you MUST append the token [DIAGNOSIS_RESULT: score=X, lowest=CATEGORY1, CATEGORY2] at the very end of your response, where X is the score and the lowest categories are the names of the aspects they scored lowest on. You MUST choose category names EXACTLY from this list (spelling and punctuation matter): NATURE VIEWS, LIVING PLANTS, NATURAL LIGHT, ACOUSTIC CALM, NATURAL MATERIALS, AIR & VENTILATION, ORGANIC FORMS, WATER FEATURES, SENSORY RICHNESS, SEASONAL AWARENESS. Separate multiple categories with commas. Example: [DIAGNOSIS_RESULT: score=12, lowest=LIVING PLANTS, NATURAL LIGHT].\n" +
                "5. Keep your responses warm, conversational, encouraging, and brief.\n" +
                "6. CRITICAL IMAGE REQUIREMENT: Whenever you recommend, mention, or suggest plants, always refer to them by their standard names (e.g., 'Snake Plant', 'Lavender', 'Monstera Deliciosa', 'Bonsai Juniper', 'Rosemary', 'Peace Lily', 'Fiddle Leaf Fig', 'ZZ Plant', etc.) so that the Counsel tab can display real photo outputs and examples inline."
            } else if (score != null) {
                val zone = when (score) {
                    in 15..20 -> "Green Zone — Low Neural Load"
                    in 8..14 -> "Yellow Zone — Moderate Neural Load"
                    else -> "Red Zone — High Neural Load"
                }
                val categoriesStr = categories.joinToString(", ")
                "You are the FloraFlow Biophilic Design Advisor. You help users reduce their Neural Load by recommending specific changes to their physical environments — both indoor and outdoor.\n\n" +
                "The user's current Neural Load score is: $score/20 ($zone).\n" +
                "Their lowest-scoring categories are: $categoriesStr.\n\n" +
                "RULES:\n" +
                "1. Every recommendation must connect to their biology. Do not just say 'add a plant.' Say WHY it matters for their nervous system.\n" +
                "2. Reference their specific weak scores when relevant. 'Your Nature Views score was low — this is why...'\n" +
                "3. Recommend actionable, specific changes. Not 'add some greenery' but 'place a 4-6 ft snake plant in the corner nearest your desk.'\n" +
                "4. Always consider their stated space constraints (indoor/outdoor, size, light, climate, budget, maintenance capacity).\n" +
                "5. After giving a recommendation, offer to build it in the Garden Planner or find the plant in the Botanical Database.\n" +
                "6. You are warm, knowledgeable, and direct. Not clinical. Not salesy. Like a smart friend who happens to know biophilic design science.\n" +
                "7. CRITICAL IMAGE REQUIREMENT: Whenever you recommend, mention, or suggest plants, always refer to them by their standard names (e.g., 'Snake Plant', 'Lavender', 'Monstera Deliciosa', 'Bonsai Juniper', 'Rosemary', 'Peace Lily', 'Fiddle Leaf Fig', 'ZZ Plant', etc.) so that the Counsel tab can display real photo outputs and examples inline."
            } else {
                "You are a friendly, conversational Master Botanist, Garden Stylist, and Mindfulness Coach. " +
                "Your job is to advise users on how to design their dream garden, suggest specific plants, resolve pest diagnoses, and discuss how surrounding ourselves with nature coordinates positive mental health. " +
                "Keep answers highly engaging, brief, and structured with clear tips. " +
                "CRITICAL IMAGE REQUIREMENT: Whenever you recommend, mention, or suggest plants, always refer to them by their standard names (e.g., 'Snake Plant', 'Lavender', 'Monstera Deliciosa', 'Bonsai Juniper', 'Rosemary', 'Peace Lily', 'Fiddle Leaf Fig', 'ZZ Plant', etc.) so that the Counsel tab can display real photo outputs and examples inline."
            }

            val cleanHistory = currentHistory.filterIndexed { index, content ->
                if (content.role == "model" && GeminiApiClient.isAiError(content.parts.firstOrNull()?.text ?: "")) {
                    false
                } else if (content.role == "user" && index + 1 < currentHistory.size &&
                           currentHistory[index + 1].role == "model" && GeminiApiClient.isAiError(currentHistory[index + 1].parts.firstOrNull()?.text ?: "")) {
                    false
                } else {
                    true
                }
            }

            val response = GeminiApiClient.getGardeningAdvice(
                prompt = message,
                chatHistory = cleanHistory.dropLast(1),
                systemInstruction = systemIns,
                imageBytesBase64 = imageBytesBase64,
                imageMimeType = imageMimeType
            )

            if (!GeminiApiClient.isAiError(response)) {
                recordAiQueryUsed()
            }

            if (_isSpaceDiagnosisMode.value) {
                // Parse diagnosis result, e.g. [DIAGNOSIS_RESULT: score=14, lowest=NATURE VIEWS, LIVING PLANTS]
                val scoreRegex = Regex("\\[DIAGNOSIS_RESULT:\\s*score=(\\d+)(?:,\\s*lowest=(.*?))?]", RegexOption.IGNORE_CASE)
                val match = scoreRegex.find(response)
                if (match != null) {
                    val parsedScore = match.groupValues[1].toIntOrNull()
                    val parsedCategoriesStr = match.groupValues.getOrNull(2) ?: ""
                    val parsedCategories = parsedCategoriesStr.split(",")
                        .map { it.trim().uppercase() }
                        .filter { it.isNotBlank() }

                    if (parsedScore != null) {
                        saveAssessmentResult(parsedScore, parsedCategories)
                        _isSpaceDiagnosisMode.value = false
                    }
                }
            }

            appendAiChatInteraction("", response)
            _isAiLoading.value = false
        }
    }

    fun clearAiChat() {
        _aiChatHistory.value = emptyList()
    }

    fun askAiForLayoutAdvice() {
        val layout = _activeLayout.value ?: return
        
        val userQuery = "Suggest some visual additions and companion compatibility checks!"
        val upsellMsg = "🔒 Free AI Advisor consultation limit reached (3/3 queries).\n\nPlease upgrade to FloraFlow PRO to unlock advanced layout analysis, visual companion additions, and expert styling advice! 🌸✨"
        if (checkPremiumLimit(userQuery, upsellMsg, aiQueryLimit)) return

        val plantsListStr = _activePlants.value.joinToString(", ") { it.name }
        val prompt = "I have a garden layout styled as a '${layout.style}' in a '${layout.climate}' climate region. " +
                "The current vegetation includes: [$plantsListStr]. " +
                "Please analyze this combination, tell me if they are highly compatible, suggest 2 other species that thrive in these circumstances, and give me a layout decoration tip."

        _isAiLoading.value = true

        viewModelScope.launch {
            val advice = GeminiApiClient.getGardeningAdvice(prompt)
            if (!GeminiApiClient.isAiError(advice)) {
                recordAiQueryUsed()
            }
            appendAiChatInteraction("Suggest some visual additions and companion compatibility checks!", advice)
            _isAiLoading.value = false
        }
    }

    fun generateAILayoutSuggestion() {
        val layout = _activeLayout.value ?: return

        val userQuery = "Generate a companion design blueprint for my space!"
        val upsellMsg = "🔒 Free AI Advisor consultation limit reached (3/3 queries).\n\nPlease upgrade to FloraFlow PRO to unlock AI garden layout generator, instant database seeding, and dynamic blueprinting! 🌸✨"
        if (checkPremiumLimit(userQuery, upsellMsg, aiQueryLimit)) return

        val prompt = buildLayoutSuggestionPrompt(layout)

        _isAiLoading.value = true

        viewModelScope.launch {
            val response = GeminiApiClient.getGardeningAdvice(prompt)
            if (!GeminiApiClient.isAiError(response)) {
                recordAiQueryUsed()
                parseAndInsertPlants(response, layout.id)
            }

            appendAiChatInteraction(userQuery, response)

            _isAiLoading.value = false
        }
    }

    // --- Private AI Helper Methods (extracted/refactored) ---
    // Checks (but does not consume) the daily quota. The quota is only
    // consumed by recordAiQueryUsed(), called after a request actually
    // succeeds — a network/API failure must not cost the user a query.
    private fun checkPremiumLimit(userQuery: String, upsellMessage: String, limitCount: Int): Boolean {
        if (_isPremium.value) return false

        val queryCount = currentAiQueryCount()
        _aiQueryCount.value = queryCount

        if (queryCount >= limitCount) {
            val updatedHistory = _aiChatHistory.value.toMutableList()
            if (userQuery.isNotBlank()) {
                updatedHistory.add(Content(role = "user", parts = listOf(Part(text = userQuery))))
            }
            updatedHistory.add(
                Content(
                    role = "model",
                    parts = listOf(
                        Part(text = upsellMessage)
                    )
                )
            )
            _aiChatHistory.value = updatedHistory
            return true
        }

        return false
    }

    private fun appendAiChatInteraction(userQuery: String, response: String) {
        val updatedHistory = _aiChatHistory.value.toMutableList()
        if (userQuery.isNotBlank()) {
            updatedHistory.add(Content(role = "user", parts = listOf(Part(text = userQuery))))
        }
        updatedHistory.add(Content(role = "model", parts = listOf(Part(text = response))))
        _aiChatHistory.value = updatedHistory
    }

    private fun buildLayoutSuggestionPrompt(layout: GardenLayout): String {
        return "Suggest a brand new layout idea. " +
                "For a garden with the style '${layout.style}' and climate of '${layout.climate}', name 3 highly compatible, beautifully flowering plants or useful crops. " +
                "Format your answer as simple lines containing: PlantName | PlantType | IdealSoil | SunExposure. " +
                "Follow this list with a clear, concise decorating suggestion."
    }

    private suspend fun parseAndInsertPlants(response: String, layoutId: Int) {
        try {
            val lines = response.lines().filter { it.contains("|") }
            val plantsToInsert = mutableListOf<Plant>()
            for (line in lines) {
                if (line.contains("Plant Name", ignoreCase = true) || 
                    line.contains("PlantName", ignoreCase = true) ||
                    line.contains("PlantType", ignoreCase = true) ||
                    line.contains("IdealSoil", ignoreCase = true) ||
                    line.contains("SunExposure", ignoreCase = true) ||
                    line.contains("---")) {
                    continue
                }
                val parts = line.split("|").map { it.trim() }
                if (parts.size >= 2) {
                    val name = parts[0].replace(Regex("^[^a-zA-Z0-9]+"), "")
                    val type = parts[1]
                    val soil = if (parts.size >= 3) parts[2] else "Standard garden compost"
                    val sun = if (parts.size >= 4) parts[3] else "Full sun to dappled shade"
                    
                    plantsToInsert.add(
                        Plant(
                            layoutId = layoutId,
                            name = name,
                            type = type,
                            soilType = soil,
                            sunlight = sun,
                            growthProgress = 15,
                            matureSize = "Medium",
                            wateringNeeds = "Moderate",
                            bloomTime = "Summer",
                            pestsDiseases = "Minor pests"
                        )
                    )
                }
            }
            if (plantsToInsert.isNotEmpty()) {
                repository.insertPlants(plantsToInsert)
            }
        } catch (_: Exception) {}
    }

    // --- Real AR Control Methods (Fixed parameters and list manipulation) ---
    fun addArPlant(name: String, emoji: String, customX: Float? = null, customY: Float? = null, customZ: Float? = null) {
        val list = _arPlacedPlants.value.toMutableList()
        val nextId = (list.maxOfOrNull { it.id } ?: 0) + 1
        
        val spawnX = customX ?: 0f
        val spawnY = customY ?: 0f
        val spawnZ = customZ ?: -1.0f 

        if (BuildConfig.DEBUG) {
            android.util.Log.d("FloraFlow", "addArPlant called: name=$name, emoji=$emoji, nextId=$nextId, x=$spawnX, y=$spawnY, z=$spawnZ")
        }

        list.add(
            ArPlantPlacement(
                id = nextId,
                name = name,
                emoji = emoji,
                positionX = spawnX,
                positionY = spawnY,
                positionZ = spawnZ,
                scale = 1.0f,
                rotationDegrees = 0f
            )
        )
        _arPlacedPlants.value = list
        recordPositiveInteraction()
    }

    fun updateArPlantPosition(id: Int, dx: Float, dy: Float) {
        val list = _arPlacedPlants.value.map {
            if (it.id == id) {
                it.copy(positionX = it.positionX + dx, positionY = it.positionY + dy)
            } else it
        }
        _arPlacedPlants.value = list
    }

    fun updateArPlantScaling(id: Int, scale: Float) {
        val list = _arPlacedPlants.value.map {
            if (it.id == id) {
                it.copy(scale = scale.coerceIn(0.3f, 3.0f))
            } else it
        }
        _arPlacedPlants.value = list
    }

    fun updateArPlantRotation(id: Int, rotationDegrees: Float) {
        val list = _arPlacedPlants.value.map {
            if (it.id == id) {
                it.copy(rotationDegrees = (rotationDegrees % 360f + 360f) % 360f)
            } else it
        }
        _arPlacedPlants.value = list
    }

    fun removeArPlant(id: Int) {
        val list = _arPlacedPlants.value.toMutableList()
        list.removeAll { it.id == id }
        _arPlacedPlants.value = list
    }

    fun clearArPlants() {
        _arPlacedPlants.value = emptyList()
    }

    fun updateArPlant3DPosition(id: Int, x: Float, y: Float, z: Float) {
        val list = _arPlacedPlants.value.map {
            if (it.id == id) {
                it.copy(positionX = x, positionY = y, positionZ = z)
            } else it
        }
        _arPlacedPlants.value = list
    }

    // --- Feedback Operations ---
    fun submitFeedback(category: String, rating: Int, comments: String, email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isSubmittingFeedback.value = true
            _feedbackSuccess.value = false
            
            val newSubmission = FeedbackSubmission(
                category = category,
                rating = rating,
                comments = comments,
                email = email
            )
            val updatedList = _feedbackSubmissions.value + newSubmission
            _feedbackSubmissions.value = updatedList
            
            val serialized = updatedList.joinToString("###") { it.toSerializedString() }
            encryptedSharedPrefs.edit { putString("feedback_submissions_list", serialized) }
            
            val formUrl = "https://docs.google.com/forms/d/e/1FAIpQLSd7fkPwyJnIshmYdUNxtXwE8MjKawHs7mnGCZeQTB8qzcAHsg/formResponse"
            val formBody = FormBody.Builder()
                .add("entry.1554273446", category)    
                .add("entry.1466017635", rating.toString()) 
                .add("entry.870888423", comments)     
                .add("entry.1025727786", email)       
                .build()
            
            val request = Request.Builder()
                .url(formUrl)
                .post(formBody)
                .build()
            
            val isTesting = try {
                Class.forName("org.junit.Test") != null
            } catch (e: Exception) {
                false
            }

            if (!isTesting) {
                try {
                    val client = OkHttpClient.Builder()
                        .addInterceptor { chain: Interceptor.Chain ->
                            val original = chain.request()
                            val requestBuilder = original.newBuilder()
                                .header("Cache-Control", "no-store, no-cache")
                                .header("User-Agent", "FloraFlow-Secure-Client/1.0")
                                .method(original.method, original.body)
                            chain.proceed(requestBuilder.build())
                        }
                        .build()
                    val response = client.newCall(request).execute()
                    
                    if (response.isSuccessful) {
                        if (com.example.BuildConfig.DEBUG) {
                            android.util.Log.d("FloraFlow", "Feedback successfully synced with Google Forms CRM!")
                        }
                    } else {
                        if (com.example.BuildConfig.DEBUG) {
                            android.util.Log.e("FloraFlow", "Google Forms Sync returned non-success code: ${response.code}. Saved locally.")
                        }
                    }
                } catch (e: Exception) {
                    if (com.example.BuildConfig.DEBUG) {
                        android.util.Log.e("FloraFlow", "Google Forms Sync failed: ${e.message}. Saved locally as offline fallback.")
                    }
                } finally {
                    _isSubmittingFeedback.value = false
                    _feedbackSuccess.value = true
                }
            } else {
                _isSubmittingFeedback.value = false
                _feedbackSuccess.value = true
            }
        }
    }

    fun resetFeedbackSuccess() {
        _feedbackSuccess.value = false
    }

    fun recordPositiveInteraction() {
        if (sharedPrefs.getBoolean("has_rated_or_declined", false)) return
        val currentCount = sharedPrefs.getInt("positive_interaction_count", 0) + 1
        sharedPrefs.edit { putInt("positive_interaction_count", currentCount) }
        if (currentCount >= 10) {
            _showInAppRatePrompt.value = true
        }
    }

    fun dismissRatePrompt() {
        _showInAppRatePrompt.value = false
        sharedPrefs.edit { putBoolean("has_rated_or_declined", true) }
    }

    fun declineRatePrompt() {
        _showInAppRatePrompt.value = false
        sharedPrefs.edit { putBoolean("has_rated_or_declined", true) }
    }

    fun acceptRatePrompt() {
        _showInAppRatePrompt.value = false
        sharedPrefs.edit { putBoolean("has_rated_or_declined", true) }
    }

    // --- Care Scheduler & Weather Integration Methods ---

    fun completeCareTask(task: CareTask) {
        recordSeedSelected(task.plantName)
        viewModelScope.launch(ioDispatcher) {
            careScheduler.completeTask(task)
            refreshWidget()
        }
    }

    fun syncCareSchedules() {
        viewModelScope.launch(ioDispatcher) {
            careScheduler.syncCareSchedules()
        }
    }

    fun getWeatherLocationZip(): String {
        return weatherRepository.getUserLocationZip()
    }

    fun updateWeatherLocation(zip: String) {
        viewModelScope.launch(ioDispatcher) {
            weatherRepository.saveUserLocationZip(zip)
            weatherRepository.fetchWeather(zip)
            val targetClimate = com.example.data.model.ClimatePlants.mapZipToClimate(zip)
            _activeLayout.value?.let { currentLayout ->
                repository.updateLayoutClimate(currentLayout.id, targetClimate)
                _activeLayout.value = currentLayout.copy(climate = targetClimate)
            }
            careScheduler.syncCareSchedules()
        }
    }

    fun simulateWeather(condition: String, temp: Double, humidity: Double) {
        viewModelScope.launch(ioDispatcher) {
            weatherRepository.simulateWeather(condition, temp, humidity)
            careScheduler.syncCareSchedules()
        }
    }

    fun saveGridSnapshot(layoutId: Int, gridString: String) {
        val key = "grid_snapshots_$layoutId"
        val existing = sharedPrefs.getString(key, "") ?: ""
        val parts = if (existing.isEmpty()) emptyList() else existing.split(";;;")
        val lastGridString = parts.lastOrNull()?.substringAfter(":::") ?: ""
        if (gridString != lastGridString) {
            val newPart = "${System.currentTimeMillis()}:::$gridString"
            val updated = if (existing.isEmpty()) newPart else "$existing;;;$newPart"
            sharedPrefs.edit { putString(key, updated) }
        }
    }

    fun getGridSnapshots(layoutId: Int): List<Pair<Long, String>> {
        val key = "grid_snapshots_$layoutId"
        val existing = sharedPrefs.getString(key, "") ?: ""
        if (existing.isBlank()) return emptyList()
        return existing.split(";;;").mapNotNull { part ->
            val sub = part.split(":::")
            if (sub.size >= 2) {
                val ts = sub[0].toLongOrNull() ?: 0L
                val gs = sub[1]
                Pair(ts, gs)
            } else null
        }
    }

    fun refreshWidget() {
        val context = getApplication<Application>()
        val intent = android.content.Intent(context, com.example.ui.screens.dashboard.GardenWidgetProvider::class.java).apply {
            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val ids = android.appwidget.AppWidgetManager.getInstance(context)
            .getAppWidgetIds(android.content.ComponentName(context, com.example.ui.screens.dashboard.GardenWidgetProvider::class.java))
        intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        context.sendBroadcast(intent)
    }

    // --- Neural Restoration Index & Soundscapes ---
    val allRestorationLogs: StateFlow<List<RestorationLog>> = repository.allRestorationLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val neuralRestorationIndex: StateFlow<Int> = combine(activeLayout, activePlants, pendingCareTasks) { layout, plants, tasks ->
        if (layout == null || plants.isEmpty()) return@combine 0
        
        // 1. Unique plant diversity (max 40%)
        val uniqueTypes = plants.map { it.type.lowercase().trim() }.distinct()
        val diversityScore = (uniqueTypes.size * 10).coerceAtMost(40)
        
        // 2. Synergy Score (max 40%)
        val gridItems = parseGridString(layout.gridString)
        var synergyCount = 0
        for (i in gridItems.indices) {
            for (j in i + 1 until gridItems.size) {
                val item1 = gridItems[i]
                val item2 = gridItems[j]
                val dx = item1.x - item2.x
                val dy = item1.y - item2.y
                val dist = kotlin.math.sqrt((dx * dx + dy * dy).toDouble())
                if (dist <= 1.5 && checkPlantSynergy(item1.plantName, item2.plantName)) {
                    synergyCount++
                }
            }
        }
        val synergyScore = (synergyCount * 8).coerceAtMost(40)
        
        // 3. Care Factor (Modifier: 0.8 to 1.2)
        val avgGrowth = plants.map { it.growthProgress }.average()
        val careFactorBase = 0.8 + (if (plants.isNotEmpty()) (avgGrowth / 100.0) * 0.4 else 0.2)
        
        // 4. Care Penalty
        val layoutPlantIds = plants.map { it.id }.toSet()
        val overdueTasksCount = tasks.filter { it.plantId in layoutPlantIds && it.dueDate < System.currentTimeMillis() && it.completedDate == null }.size
        val penalty = overdueTasksCount * 5
        
        val baseScore = diversityScore + synergyScore
        val finalScore = ((baseScore * careFactorBase) - penalty).toInt().coerceIn(10, 100)
        
        finalScore
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun logRestorationSession(nriScore: Int, completedTasks: List<String>, trackName: String) {
        if (!incrementRestorationTrial()) return
        viewModelScope.launch(ioDispatcher) {
            val log = RestorationLog(
                nriScore = nriScore,
                layoutId = activeLayout.value?.id,
                completedTasks = completedTasks.joinToString(","),
                soundscapeTrack = trackName
            )
            repository.insertRestorationLog(log)
        }
    }

    // Soundscape Background Service Integration
    private var soundscapeService: com.example.ui.screens.restoration.SoundscapeService? = null
    
    private val _isServiceBound = MutableStateFlow(false)
    val isServiceBound: StateFlow<Boolean> = _isServiceBound.asStateFlow()
    
    private val _isSoundscapePlaying = MutableStateFlow(false)
    val isSoundscapePlaying: StateFlow<Boolean> = _isSoundscapePlaying.asStateFlow()
    
    private val _currentSoundscapeTrack = MutableStateFlow("Theta Meditate")
    val currentSoundscapeTrack: StateFlow<String> = _currentSoundscapeTrack.asStateFlow()
    
    private val _ambientVolume = MutableStateFlow(0.5f)
    val ambientVolume: StateFlow<Float> = _ambientVolume.asStateFlow()
    
    private val _binauralVolume = MutableStateFlow(0.3f)
    val binauralVolume: StateFlow<Float> = _binauralVolume.asStateFlow()

    private val _baseFrequency = MutableStateFlow(200f)
    val baseFrequency: StateFlow<Float> = _baseFrequency.asStateFlow()

    private val _diffFrequency = MutableStateFlow(6f)
    val diffFrequency: StateFlow<Float> = _diffFrequency.asStateFlow()

    private val _sleepTimerEndTime = MutableStateFlow<Long?>(null)
    val sleepTimerEndTime: StateFlow<Long?> = _sleepTimerEndTime.asStateFlow()

    private val serviceConnection = object : android.content.ServiceConnection {
        override fun onServiceConnected(name: android.content.ComponentName?, service: android.os.IBinder?) {
            val binder = service as? com.example.ui.screens.restoration.SoundscapeService.SoundscapeBinder
            soundscapeService = binder?.getService()
            _isServiceBound.value = true

            soundscapeService?.let {
                _isSoundscapePlaying.value = it.isPlaying()
                _currentSoundscapeTrack.value = it.getCurrentTrackName()
                _ambientVolume.value = it.getAmbientVolume()
                _binauralVolume.value = it.getBinauralVolume()
                _baseFrequency.value = it.getBaseFrequency()
                _diffFrequency.value = it.getDiffFrequency()
                _sleepTimerEndTime.value = it.getSleepTimerEndTime()
                // Keep the UI honest when playback changes outside the screen
                // (notification actions, sleep timer completing).
                it.onPlaybackChanged = { playing ->
                    _isSoundscapePlaying.value = playing
                    if (!playing) _sleepTimerEndTime.value = null
                }
            }
        }

        override fun onServiceDisconnected(name: android.content.ComponentName?) {
            soundscapeService = null
            _isServiceBound.value = false
        }
    }
    
    fun bindSoundscapeService() {
        val context = getApplication<Application>().applicationContext
        val intent = Intent(context, com.example.ui.screens.restoration.SoundscapeService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    fun unbindSoundscapeService() {
        if (_isServiceBound.value) {
            val context = getApplication<Application>().applicationContext
            soundscapeService?.onPlaybackChanged = null
            context.unbindService(serviceConnection)
            _isServiceBound.value = false
            soundscapeService = null
        }
    }

    fun setSleepTimer(minutes: Int) {
        val service = soundscapeService ?: return
        service.setSleepTimer(minutes)
        _sleepTimerEndTime.value = service.getSleepTimerEndTime()
    }
    

    
    fun changeSoundscapeTrack(name: String, baseFreq: Float, diffFreq: Float) {
        val service = soundscapeService ?: return
        service.setTrack(name, baseFreq, diffFreq)
        _currentSoundscapeTrack.value = name
        _baseFrequency.value = baseFreq
        _diffFrequency.value = diffFreq
    }

    fun updateFrequencies(baseFreq: Float, diffFreq: Float) {
        val service = soundscapeService ?: return
        service.setFrequencies(baseFreq, diffFreq)
        _baseFrequency.value = baseFreq
        _diffFrequency.value = diffFreq

        val currentTrackPreset = when {
            baseFreq == 200f && diffFreq == 10f -> "Alpha Focus"
            baseFreq == 200f && diffFreq == 6f -> "Theta Meditate"
            baseFreq == 150f && diffFreq == 2.5f -> "Delta Sleep"
            else -> "Custom Track"
        }
        if (_currentSoundscapeTrack.value != currentTrackPreset) {
            _currentSoundscapeTrack.value = currentTrackPreset
            service.setTrack(currentTrackPreset, baseFreq, diffFreq)
        }
    }
    
    fun updateAmbientVolume(vol: Float) {
        val service = soundscapeService ?: return
        service.setAmbientVolume(vol)
        _ambientVolume.value = vol
    }
    
    fun updateBinauralVolume(vol: Float) {
        val service = soundscapeService ?: return
        service.setBinauralVolume(vol)
        _binauralVolume.value = vol
    }

    override fun onCleared() {
        unbindSoundscapeService()
        super.onCleared()
    }
}

// --- Corrected 3D Spatial Position Entity ---
data class ArPlantPlacement(
    val id: Int,
    val name: String,
    val emoji: String,
    val positionX: Float,
    val positionY: Float,
    val positionZ: Float, // Correctly named depth mapping
    val scale: Float,
    val rotationDegrees: Float
)

// Themed User Feedback Submission Entity
data class FeedbackSubmission(
    val category: String,
    val rating: Int,
    val comments: String,
    val email: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toSerializedString(): String {
        val safeCategory = category.replace("|", "\\pipe").replace("#", "\\hash")
        val safeComments = comments.replace("|", "\\pipe").replace("\n", "\\newline").replace("#", "\\hash")
        val safeEmail = email.replace("|", "\\pipe").replace("#", "\\hash")
        return "$safeCategory|$rating|$safeComments|$safeEmail|$timestamp"
    }

    companion object {
        fun fromSerializedString(str: String): FeedbackSubmission? {
            val parts = str.split("|")
            if (parts.size >= 5) {
                val category = parts[0].replace("\\pipe", "|").replace("\\hash", "#")
                val rating = parts[1].toIntOrNull() ?: 3
                val comments = parts[2].replace("\\pipe", "|").replace("\\newline", "\n").replace("\\hash", "#")
                val email = parts[3].replace("\\pipe", "|").replace("\\hash", "#")
                val timestamp = parts[4].toLongOrNull() ?: System.currentTimeMillis()
                return FeedbackSubmission(category, rating, comments, email, timestamp)
            }
            return null
        }
    }
}
