package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.Content
import com.example.data.api.GeminiApiClient
import com.example.data.api.Part
import com.example.data.database.GardenDatabase
import com.example.data.model.*
import com.example.data.repository.GardenRepository
import androidx.core.content.edit
import com.example.ui.screens.community.CommunityPost
import com.example.ui.screens.community.CommunityComment
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

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

    // Reactive database streams
    val allLayouts: StateFlow<List<GardenLayout>>
    val allMoodLogs: StateFlow<List<MoodLog>>
    val allCommunityPosts: StateFlow<List<CommunityPost>>

    // Active selection states
    private val _activeLayout = MutableStateFlow<GardenLayout?>(null)
    val activeLayout: StateFlow<GardenLayout?> = _activeLayout.asStateFlow()

    private val _activePlants = MutableStateFlow<List<Plant>>(emptyList())
    val activePlants: StateFlow<List<Plant>> = _activePlants.asStateFlow()

    // Real-time AI chat stream
    private val _aiChatHistory = MutableStateFlow<List<Content>>(emptyList())
    val aiChatHistory: StateFlow<List<Content>> = _aiChatHistory.asStateFlow()

    private val _aiStatus = MutableStateFlow<String>("")
    val aiStatus: StateFlow<String> = _aiStatus.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Mock AR Lens Placement States
    private val _arPlacedPlants = MutableStateFlow<List<ArPlantPlacement>>(emptyList())
    val arPlacedPlants: StateFlow<List<ArPlantPlacement>> = _arPlacedPlants.asStateFlow()

    // Premium subscription state for Devil's Advocate paywall demo
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

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

    fun saveAssessmentResult(score: Int, categories: List<String>) {
        _assessmentScore.value = score
        _lowestCategories.value = categories
        sharedPrefs.edit {
            putInt("assessment_score", score)
            putString("assessment_categories", categories.joinToString(","))
        }
    }

    fun skipAssessment() {
        _isAssessmentSkipped.value = true
        sharedPrefs.edit { putBoolean("assessment_skipped", true) }
        completeOnboarding()
    }

    fun resetAssessment() {
        _isAssessmentSkipped.value = false
        _assessmentScore.value = null
        _lowestCategories.value = emptyList()
        _isOnboardingCompleted.value = false
        sharedPrefs.edit {
            putBoolean("assessment_skipped", false)
            remove("assessment_score")
            remove("assessment_categories")
            putBoolean("onboarding_completed", false)
        }
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
        val steps = WalkthroughStep.values()
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

    private val sharedPrefs = application.getSharedPreferences("floraflow_billing_prefs", android.content.Context.MODE_PRIVATE)

    fun upgradeToPremium() {
        _showBillingDialog.value = true
    }

    fun setBillingDialogVisible(visible: Boolean) {
        _showBillingDialog.value = visible
    }

    fun setSubscriptionManagementVisible(visible: Boolean) {
        _showSubscriptionManagement.value = visible
    }

    fun processPurchase(tier: String, price: String, isAnnual: Boolean) {
        val txId = "GPA." + (1000..9999).random().toString() + "-" + 
                   (1000..9999).random().toString() + "-" + 
                   (1000..9999).random().toString() + "-" + 
                   (10000..99999).random().toString()
        
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US)
        val cal = java.util.Calendar.getInstance()
        if (isAnnual) {
            cal.add(java.util.Calendar.YEAR, 1)
        } else {
            cal.add(java.util.Calendar.MONTH, 1)
        }
        val nextBillingDate = sdf.format(cal.time)

        _isPremium.value = true
        _subscriptionTier.value = tier
        _subscriptionTransactionId.value = txId
        _subscriptionBillingDate.value = nextBillingDate

        sharedPrefs.edit {
            putBoolean("is_premium", true)
            putString("subscription_tier", tier)
            putString("subscription_transaction_id", txId)
            putString("subscription_billing_date", nextBillingDate)
            putBoolean("purchased_historically", true)
        }
    }

    fun cancelPremiumSubscription() {
        _isPremium.value = false
        _subscriptionTier.value = null
        _subscriptionTransactionId.value = null
        _subscriptionBillingDate.value = null

        sharedPrefs.edit {
            putBoolean("is_premium", false)
            putString("subscription_tier", null)
            putString("subscription_transaction_id", null)
            putString("subscription_billing_date", null)
        }
    }

    fun restorePurchases(): Boolean {
        val hasHistory = sharedPrefs.getBoolean("purchased_historically", false)
        return if (hasHistory) {
            val txId = "GPA.RESTORED-" + (1000..9999).random().toString() + "-RE"
            val tier = "FloraFlow PRO Annual"
            val nextDate = "Jun 15, 2027"
            
            _isPremium.value = true
            _subscriptionTier.value = tier
            _subscriptionTransactionId.value = txId
            _subscriptionBillingDate.value = nextDate

            sharedPrefs.edit {
                putBoolean("is_premium", true)
                putString("subscription_tier", tier)
                putString("subscription_transaction_id", txId)
                putString("subscription_billing_date", nextDate)
            }
            true
        } else {
            // Force seed a standard restored purchase if they don't have local history (makes demo seamless)
            val txId = "GPA.DEMO-" + (1000..9999).random().toString() + "-RESTORED"
            val tier = "FloraFlow PRO Monthly"
            val nextDate = "Jul 21, 2026"
            
            _isPremium.value = true
            _subscriptionTier.value = tier
            _subscriptionTransactionId.value = txId
            _subscriptionBillingDate.value = nextDate

            sharedPrefs.edit {
                putBoolean("is_premium", true)
                putString("subscription_tier", tier)
                putString("subscription_transaction_id", txId)
                putString("subscription_billing_date", nextDate)
                putBoolean("purchased_historically", true)
            }
            true
        }
    }

    // Active screen index hoist state for onboarding assessments completion callbacks
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    fun setCurrentTab(tab: Int) {
        _currentTab.value = tab
    }

    init {
        // Load persistent billing subscription and onboarding values on start
        val savedPremium = sharedPrefs.getBoolean("is_premium", false)
        _isPremium.value = savedPremium
        _isOnboardingCompleted.value = sharedPrefs.getBoolean("onboarding_completed", false)
        _subscriptionTier.value = sharedPrefs.getString("subscription_tier", null)
        _subscriptionTransactionId.value = sharedPrefs.getString("subscription_transaction_id", null)
        _subscriptionBillingDate.value = sharedPrefs.getString("subscription_billing_date", null)
        _isAssessmentSkipped.value = sharedPrefs.getBoolean("assessment_skipped", false)

        val savedScore = sharedPrefs.getInt("assessment_score", -1)
        if (savedScore != -1) {
            _assessmentScore.value = savedScore
        }
        val savedCategories = sharedPrefs.getString("assessment_categories", null)
        if (savedCategories != null) {
            _lowestCategories.value = savedCategories.split(",").filter { it.isNotBlank() }
        }

        val savedTheme = sharedPrefs.getString("theme_mode", "SYSTEM")
        val mode = try { ThemeMode.valueOf(savedTheme ?: "SYSTEM") } catch (_: Exception) { ThemeMode.SYSTEM }
        _themeMode.value = mode
        _isDarkTheme.value = when (mode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> null
        }

        val savedFeedback = sharedPrefs.getString("feedback_submissions_list", null)
        if (savedFeedback != null) {
            val list = savedFeedback.split("###").mapNotNull { FeedbackSubmission.fromSerializedString(it) }
            _feedbackSubmissions.value = list
        }

        val db = database ?: GardenDatabase.getDatabase(application)
        repository = GardenRepository(db.gardenDao())

        allLayouts = repository.allLayouts
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

        allCommunityPosts = repository.allPosts
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

        viewModelScope.launch(ioDispatcher) {
            try {
                val existingPosts = repository.allPosts.firstOrNull() ?: emptyList()
                if (existingPosts.isEmpty()) {
                    val p1Id = repository.insertPost(
                        CommunityPost(
                            title = "The Golden Rule of Watering Succulents",
                            content = "Always wait until the soil is bone dry before watering again. Stick a wooden skewer or your finger about 2 inches deep. If it's damp, hold off! Overwatering is the #1 killer of succulents.",
                            category = "Tips",
                            author = "GardenGuru",
                            likes = 12
                        )
                    ).toInt()
                    repository.insertComment(CommunityComment(postId = p1Id, author = "CactusJack", content = "Totally agree. In winter, I only water mine once a month and they thrive!", likes = 5))
                    repository.insertComment(CommunityComment(postId = p1Id, author = "MonsteraMom", content = "I learned this the hard way after losing my first zebra plant. Great advice!", likes = 3))

                    val p2Id = repository.insertPost(
                        CommunityPost(
                            title = "My Monstera Deliciosa got its first fenestration!",
                            content = "I've been keeping it in bright, indirect light near my east-facing window and feeding it dilute fertilizer once a month. The leaf just uncurled this morning and it's perfect! Don't lose hope if yours takes a while; consistency is key.",
                            category = "Experiences",
                            author = "MonsteraMom",
                            likes = 24
                        )
                    ).toInt()
                    repository.insertComment(CommunityComment(postId = p2Id, author = "PlantBae", content = "Congratulations! It's the best feeling ever. Can't wait for my propagation to fenestrate.", likes = 6))

                    val p3Id = repository.insertPost(
                        CommunityPost(
                            title = "White spots on Rose leaves - help?",
                            content = "My miniature rose bush has developed a dusty white coating on its lower leaves. Is this powdery mildew? I'm watering it from the top daily. Any tips to treat it organically would be super appreciated!",
                            category = "Questions",
                            author = "GreenNoob",
                            likes = 5
                        )
                    ).toInt()
                    repository.insertComment(CommunityComment(postId = p3Id, author = "RoseLover", content = "Definitely powdery mildew! Try to water the soil directly, not the leaves. Wet leaves invite spores.", likes = 8))
                    repository.insertComment(CommunityComment(postId = p3Id, author = "OrganicGardener", content = "You can spray it with a mixture of milk and water (40/60 ratio) in direct sunlight. It works as a natural fungicide!", likes = 10))
                    repository.insertComment(CommunityComment(postId = p3Id, author = "ZenMaster", content = "Remember to prune the affected leaves and dispose of them (don't compost them) so the spores don't spread.", likes = 4))

                    val p4Id = repository.insertPost(
                        CommunityPost(
                            title = "Mindful Pruning: How to Connect with Your Plants",
                            content = "Pruning isn't just about maintenance; it's a form of meditation. Approach your plant with quiet focus. Use clean shears, and as you cut dead stems, take a slow breath. Visualize making space for new, healthy growth in your own life.",
                            category = "Tips",
                            author = "ZenMaster",
                            likes = 18
                        )
                    ).toInt()
                }

                val existing = repository.allLayouts.firstOrNull() ?: emptyList()
                if (existing.isEmpty()) {
                    val defaultLayout = GardenLayout(
                        name = "My First Space",
                        style = "Zen Garden",
                        climate = "Temperate",
                        gridString = "0,0,Bonsai Juniper|4,4,English Lavender",
                    )
                    val layoutId = repository.insertLayout(defaultLayout).toInt()

                    repository.insertPlant(
                        Plant(
                            layoutId = layoutId,
                            name = "Bonsai Juniper",
                            type = "Tree",
                            careSpring = "Prune branches to maintain classic zen shape. Water regularly.",
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
                        )
                    )

                    repository.insertPlant(
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
                    )

                    repository.insertMoodLog(
                        MoodLog(
                            mood = "Serene",
                            moodScore = 4,
                            activityMinutes = 30,
                            notes = "Had a wonderful morning watering the garden.",
                            growthIndex = 30
                        )
                    )
                    repository.insertMoodLog(
                        MoodLog(
                            mood = "Refreshed",
                            moodScore = 5,
                            activityMinutes = 45,
                            notes = "Breathed fresh air and spent quality time under the sun.",
                            growthIndex = 40
                        )
                    )
                    repository.insertMoodLog(
                        MoodLog(
                            mood = "Calm",
                            moodScore = 5,
                            activityMinutes = 40,
                            notes = "Very restful and grounding companion planting session.",
                            growthIndex = 50
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("GardenViewModel", "Failed to initialize default data", e)
            }
        }

        viewModelScope.launch {
            allLayouts.collect { layouts ->
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
        }
    }

    fun clearLayoutGrid() {
        val current = _activeLayout.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateLayoutGrid(current.id, "")
            _activeLayout.value = current.copy(gridString = "")
        }
    }

    fun autoSowClimateSeeds() {
        val current = _activeLayout.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val items = parseGridString(current.gridString).toMutableList()
            val occupied = items.asSequence().map { Pair(it.x, it.y) }.toSet()
            val available = mutableListOf<Pair<Int, Int>>()
            for (r in 0..4) {
                for (c in 0..4) {
                    if (!occupied.contains(Pair(r, c))) {
                        available.add(Pair(r, c))
                    }
                }
            }
            if (available.isEmpty()) return@launch

            val templates = ClimatePlants.getTemplatesForClimate(current.climate)
            if (templates.isEmpty()) return@launch

            val countToSow = minOf(5, available.size)
            available.shuffle()
            val slotsToSow = available.take(countToSow)

            for (slot in slotsToSow) {
                val tpl = templates.random()
                items.add(GridPlantItem(slot.first, slot.second, tpl.name))
                
                if (_activePlants.value.none { it.name == tpl.name }) {
                    repository.insertPlant(
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

            val newGridString = toGridString(items)
            repository.updateLayoutGrid(current.id, newGridString)
            _activeLayout.value = current.copy(gridString = newGridString)
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
        }
    }

    fun updatePlantProgress(plantId: Int, progress: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val plants = _activePlants.value
            val p = plants.firstOrNull { it.id == plantId } ?: return@launch
            repository.updatePlant(p.copy(growthProgress = progress.coerceIn(0, 100)))
        }
    }

    fun deletePlant(plantId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePlantById(plantId)
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
        }
    }

    fun deleteMoodLog(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMoodLogById(id)
        }
    }

    // --- Real-time Gemini Client interactions ---
    fun sendAiChatMessage(message: String) {
        if (message.isBlank()) return
        
        val upsellMsg = "🔒 Free AI Advisor biophilic limit reached (3/3 queries).\n\nPlease upgrade to FloraFlow PRO to unlock unlimited conversational plant care, professional garden blueprinting, and expert AI botany diagnosis! 🌸✨"
        if (checkPremiumLimit(message, upsellMsg, 3)) return

        val currentHistory = _aiChatHistory.value.toMutableList()
        currentHistory.add(Content(role = "user", parts = listOf(Part(text = message))))
        _aiChatHistory.value = currentHistory

        _isAiLoading.value = true

        viewModelScope.launch {
            val score = _assessmentScore.value
            val categories = _lowestCategories.value
            val systemIns = if (score != null) {
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
                "6. You are warm, knowledgeable, and direct. Not clinical. Not salesy. Like a smart friend who happens to know biophilic design science."
            } else {
                "You are a friendly, conversational Master Botanist, Garden Stylist, and Mindfulness Coach. " +
                "Your job is to advise users on how to design their dream garden, suggest specific plants, resolve pest diagnoses, and discuss how surrounding ourselves with nature coordinates positive mental health. " +
                "Keep answers highly engaging, brief, and structured with clear tips."
            }

            val response = GeminiApiClient.getGardeningAdvice(
                prompt = message,
                chatHistory = currentHistory.dropLast(1),
                systemInstruction = systemIns
            )

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
        val upsellMsg = "🔒 Free AI Advisor consultation limit reached (2/2 queries).\n\nPlease upgrade to FloraFlow PRO to unlock advanced layout analysis, visual companion additions, and expert styling advice! 🌸✨"
        if (checkPremiumLimit(userQuery, upsellMsg, 2)) return

        val plantsListStr = _activePlants.value.joinToString(", ") { it.name }
        val prompt = "I have a garden layout styled as a '${layout.style}' in a '${layout.climate}' climate region. " +
                "The current vegetation includes: [$plantsListStr]. " +
                "Please analyze this combination, tell me if they are highly compatible, suggest 2 other species that thrive in these circumstances, and give me a layout decoration tip."

        _isAiLoading.value = true

        viewModelScope.launch {
            val advice = GeminiApiClient.getGardeningAdvice(prompt)
            appendAiChatInteraction("Suggest some visual additions and companion compatibility checks!", advice)
            _isAiLoading.value = false
        }
    }

    fun generateAILayoutSuggestion() {
        val layout = _activeLayout.value ?: return
        
        val userQuery = "Generate a companion design blueprint for my space!"
        val upsellMsg = "🔒 Free AI Advisor consultation limit reached (2/2 queries).\n\nPlease upgrade to FloraFlow PRO to unlock AI garden layout generator, instant database seeding, and dynamic blueprinting! 🌸✨"
        if (checkPremiumLimit(userQuery, upsellMsg, 2)) return
        
        val prompt = buildLayoutSuggestionPrompt(layout)

        _isAiLoading.value = true

        viewModelScope.launch {
            val response = GeminiApiClient.getGardeningAdvice(prompt)
            
            appendAiChatInteraction(userQuery, response)
            parseAndInsertPlants(response, layout.id)

            _isAiLoading.value = false
        }
    }

    // --- Private AI Helper Methods (extracted/refactored) ---
    private fun checkPremiumLimit(userQuery: String, upsellMessage: String, limitCount: Int): Boolean {
        val userCount = _aiChatHistory.value.count { it.role == "user" }
        if (!_isPremium.value && userCount >= limitCount) {
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
            for (line in lines) {
                val parts = line.split("|").map { it.trim() }
                if (parts.size >= 2) {
                    val name = parts[0].replace(Regex("^[^a-zA-Z0-9]+"), "")
                    val type = parts[1]
                    val soil = if (parts.size >= 3) parts[2] else "Standard garden compost"
                    val sun = if (parts.size >= 4) parts[3] else "Full sun to dappled shade"
                    
                    repository.insertPlant(
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
        } catch (_: Exception) {}
    }

    // --- Real AR Control Methods (Fixed parameters and list manipulation) ---
    fun addArPlant(name: String, emoji: String, customX: Float? = null, customY: Float? = null, customZ: Float? = null) {
        val list = _arPlacedPlants.value.toMutableList()
        val nextId = (list.maxOfOrNull { it.id } ?: 0) + 1
        
        val spawnX = customX ?: 0f
        val spawnY = customY ?: 0f
        val spawnZ = customZ ?: -1.0f 

        android.util.Log.d("FloraFlow", "addArPlant called: name=$name, emoji=$emoji, nextId=$nextId, x=$spawnX, y=$spawnY, z=$spawnZ")

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
            sharedPrefs.edit { putString("feedback_submissions_list", serialized) }
            
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
            
            try {
                val client = OkHttpClient()
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    android.util.Log.d("FloraFlow", "Feedback successfully synced with Google Forms CRM!")
                } else {
                    android.util.Log.e("FloraFlow", "Google Forms Sync returned non-success code: ${response.code}. Saved locally.")
                }
            } catch (e: Exception) {
                android.util.Log.e("FloraFlow", "Google Forms Sync failed: ${e.message}. Saved locally as offline fallback.", e)
            } finally {
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
        if (currentCount >= 2) {
            _showInAppRatePrompt.value = true
        }
    }

    fun dismissRatePrompt() {
        _showInAppRatePrompt.value = false
        sharedPrefs.edit { putInt("positive_interaction_count", 0) }
    }

    fun declineRatePrompt() {
        _showInAppRatePrompt.value = false
        sharedPrefs.edit { putBoolean("has_rated_or_declined", true) }
    }

    fun acceptRatePrompt() {
        _showInAppRatePrompt.value = false
        sharedPrefs.edit { putBoolean("has_rated_or_declined", true) }
    }

    // --- Community Actions ---
    fun createPost(title: String, content: String, category: String, author: String) {
        viewModelScope.launch(ioDispatcher) {
            val post = CommunityPost(
                title = title,
                content = content,
                category = category,
                author = author.ifBlank { "Anonymous Gardener" }
            )
            repository.insertPost(post)
        }
    }

    fun toggleLikePost(postId: Int, currentLikes: Int, currentIsLiked: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            val newIsLiked = !currentIsLiked
            val newLikes = if (newIsLiked) currentLikes + 1 else (currentLikes - 1).coerceAtLeast(0)
            repository.updatePostLikes(postId, newLikes, newIsLiked)
        }
    }

    fun deletePost(postId: Int) {
        viewModelScope.launch(ioDispatcher) {
            repository.deletePost(postId)
        }
    }

    fun getCommentsForPost(postId: Int): Flow<List<CommunityComment>> {
        return repository.getCommentsForPost(postId)
    }

    fun addComment(postId: Int, author: String, content: String) {
        viewModelScope.launch(ioDispatcher) {
            val comment = CommunityComment(
                postId = postId,
                author = author.ifBlank { "Anonymous Gardener" },
                content = content
            )
            repository.insertComment(comment)
        }
    }

    fun toggleLikeComment(commentId: Int, currentLikes: Int, currentIsLiked: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            val newIsLiked = !currentIsLiked
            val newLikes = if (newIsLiked) currentLikes + 1 else (currentLikes - 1).coerceAtLeast(0)
            repository.updateCommentLikes(commentId, newLikes, newIsLiked)
        }
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
        val safeCategory = category.replace("|", "\\pipe")
        val safeComments = comments.replace("|", "\\pipe").replace("\n", "\\newline")
        val safeEmail = email.replace("|", "\\pipe")
        return "$safeCategory|$rating|$safeComments|$safeEmail|$timestamp"
    }

    companion object {
        fun fromSerializedString(str: String): FeedbackSubmission? {
            val parts = str.split("|")
            if (parts.size >= 5) {
                val category = parts[0].replace("\\pipe", "|")
                val rating = parts[1].toIntOrNull() ?: 3
                val comments = parts[2].replace("\\pipe", "|").replace("\\newline", "\n")
                val email = parts[3].replace("\\pipe", "|")
                val timestamp = parts[4].toLongOrNull() ?: System.currentTimeMillis()
                return FeedbackSubmission(category, rating, comments, email, timestamp)
            }
            return null
        }
    }
}
