package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.Content
import com.example.data.api.GeminiApiClient
import com.example.data.api.Part
import com.example.data.database.GardenDatabase
import com.example.data.model.*
import com.example.data.repository.GardenRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log

class GardenViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GardenRepository

    // Reactive database streams
    val allLayouts: StateFlow<List<GardenLayout>>
    val allMoodLogs: StateFlow<List<MoodLog>>

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
    // List of assets currently floating in the user's "yard AR" overlay
    private val _arPlacedPlants = MutableStateFlow<List<ArPlantPlacement>>(emptyList())
    val arPlacedPlants: StateFlow<List<ArPlantPlacement>> = _arPlacedPlants.asStateFlow()

    // Premium subscription state for Devil's Advocate paywall demo
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow(false)
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    fun completeOnboarding() {
        _isOnboardingCompleted.value = true
        sharedPrefs.edit().putBoolean("onboarding_completed", true).apply()
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

        sharedPrefs.edit().apply {
            putBoolean("is_premium", true)
            putString("subscription_tier", tier)
            putString("subscription_transaction_id", txId)
            putString("subscription_billing_date", nextBillingDate)
            putBoolean("purchased_historically", true)
            apply()
        }
    }

    fun cancelPremiumSubscription() {
        _isPremium.value = false
        _subscriptionTier.value = null
        _subscriptionTransactionId.value = null
        _subscriptionBillingDate.value = null

        sharedPrefs.edit().apply {
            putBoolean("is_premium", false)
            putString("subscription_tier", null)
            putString("subscription_transaction_id", null)
            putString("subscription_billing_date", null)
            apply()
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

            sharedPrefs.edit().apply {
                putBoolean("is_premium", true)
                putString("subscription_tier", tier)
                putString("subscription_transaction_id", txId)
                putString("subscription_billing_date", nextDate)
                apply()
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

            sharedPrefs.edit().apply {
                putBoolean("is_premium", true)
                putString("subscription_tier", tier)
                putString("subscription_transaction_id", txId)
                putString("subscription_billing_date", nextDate)
                putBoolean("purchased_historically", true)
                apply()
            }
            true
        }
    }

    // Theme toggling state (null means follow system theme, true means dark theme, false means light theme)
    private val _isDarkTheme = MutableStateFlow<Boolean?>(null)
    val isDarkTheme: StateFlow<Boolean?> = _isDarkTheme.asStateFlow()

    fun toggleTheme(isSystemDark: Boolean) {
        val current = _isDarkTheme.value ?: isSystemDark
        _isDarkTheme.value = !current
    }

    init {
        // Load persistent billing subscription and onboarding values on start
        val savedPremium = sharedPrefs.getBoolean("is_premium", false)
        _isPremium.value = savedPremium
        _isOnboardingCompleted.value = sharedPrefs.getBoolean("onboarding_completed", false)
        _subscriptionTier.value = sharedPrefs.getString("subscription_tier", null)
        _subscriptionTransactionId.value = sharedPrefs.getString("subscription_transaction_id", null)
        _subscriptionBillingDate.value = sharedPrefs.getString("subscription_billing_date", null)

        val database = GardenDatabase.getDatabase(application)
        repository = GardenRepository(database.gardenDao())

        allLayouts = repository.allLayouts
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        allMoodLogs = repository.allMoodLogs
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        // Seed default database values if empty
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existing = repository.allLayouts.firstOrNull() ?: emptyList()
                if (existing.isEmpty()) {
                    val defaultLayout = GardenLayout(
                        name = "My First Zen Space",
                        style = "Zen Garden",
                        climate = "Temperate",
                        gridString = "0,0,Bonsai Cherry|4,4,English Lavender"
                    )
                    val layoutId = repository.insertLayout(defaultLayout).toInt()

                    repository.insertPlant(
                        Plant(
                            layoutId = layoutId,
                            name = "Bonsai Cherry",
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
                Log.e("GardenViewModel", "Garden layout loading failed", e)
            }
        }

        // Auto-select first layout on launch if available
        viewModelScope.launch {
            allLayouts.collect { layouts ->
                if (_activeLayout.value == null && layouts.isNotEmpty()) {
                    _activeLayout.value = layouts.first()
                }
            }
        }

        // Sync active plants with Active Layout
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
            
            // Add initial default companion plants from climate templates to help get the user started!
            val templates = ClimatePlants.getTemplatesForClimate(climate).take(2)
            for (tpl in templates) {
                repository.insertPlant(
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
                )
            }
            
            // Auto select newly created layout
            _activeLayout.value = created
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
            // Remove any existing plant in the cell
            items.removeAll { it.x == x && it.y == y }
            
            // If plant name is empty, we leave it empty (removed)
            if (plantName.isNotBlank() && plantName != "Empty") {
                items.add(GridPlantItem(x, y, plantName))
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
            // Identify all free spots on the 5x5 grid
            val occupied = items.map { Pair(it.x, it.y) }.toSet()
            val available = mutableListOf<Pair<Int, Int>>()
            for (r in 0..4) {
                for (c in 0..4) {
                    if (!occupied.contains(Pair(r, c))) {
                        available.add(Pair(r, c))
                    }
                }
            }
            if (available.isEmpty()) return@launch

            // Fetch compatible plant templates for the layout climate
            val templates = ClimatePlants.getTemplatesForClimate(current.climate)
            if (templates.isEmpty()) return@launch

            // Randomly select up to 5 empty slots to sow seeds
            val countToSow = minOf(5, available.size)
            available.shuffle()
            val slotsToSow = available.take(countToSow)

            for (slot in slotsToSow) {
                val tpl = templates.random()
                items.add(GridPlantItem(slot.first, slot.second, tpl.name))
                
                // Also add to inventory if not already cultivated
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
            // Calculate active garden growth progress index as simple average of active plant growths
            val plants = _activePlants.value
            val averageGrowth = if (plants.isNotEmpty()) {
                plants.map { it.growthProgress }.average().toInt()
            } else {
                50 // Default half growth index
            }

            val newLog = MoodLog(
                mood = mood,
                moodScore = score,
                activityMinutes = duration,
                notes = notes,
                growthIndex = averageGrowth
            )
            repository.insertMoodLog(newLog)
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
        
        val userCount = _aiChatHistory.value.count { it.role == "user" }
        if (!_isPremium.value && userCount >= 2) {
            val currentHistory = _aiChatHistory.value.toMutableList()
            currentHistory.add(Content(role = "user", parts = listOf(Part(text = message))))
            currentHistory.add(
                Content(
                    role = "model",
                    parts = listOf(
                        Part(
                            text = "🔒 Free AI Advisor consultation limit reached (2/2 queries).\n\nPlease upgrade to FloraFlow PRO to unlock unlimited conversational plant care, professional garden blueprinting, and expert AI botany diagnosis! 🌸✨"
                        )
                    )
                )
            )
            _aiChatHistory.value = currentHistory
            return
        }

        val currentHistory = _aiChatHistory.value.toMutableList()
        currentHistory.add(Content(role = "user", parts = listOf(Part(text = message))))
        _aiChatHistory.value = currentHistory

        _isAiLoading.value = true
        _aiStatus.value = "Consulting garden archives..."

        viewModelScope.launch {
            val systemIns = "You are a friendly, conversational Master Botanist, Garden Stylist, and Mindfulness Coach. " +
                    "Your job is to advise users on how to design their dream garden, suggest specific plants, resolve pest diagnoses, and discuss how surrounding ourselves with nature coordinates positive mental health. " +
                    "Keep answers highly engaging, brief, and structured with clear tips."

            val response = GeminiApiClient.getGardeningAdvice(
                prompt = message,
                chatHistory = currentHistory.dropLast(1), // passing older context
                systemInstruction = systemIns
            )

            val updatedHistory = _aiChatHistory.value.toMutableList()
            updatedHistory.add(Content(role = "model", parts = listOf(Part(text = response))))
            _aiChatHistory.value = updatedHistory
            _isAiLoading.value = false
            _aiStatus.value = ""
        }
    }

    fun clearAiChat() {
        _aiChatHistory.value = emptyList()
    }

    // Automatic Layout advice based on current design style
    fun askAiForLayoutAdvice() {
        val layout = _activeLayout.value ?: return
        
        val userCount = _aiChatHistory.value.count { it.role == "user" }
        if (!_isPremium.value && userCount >= 2) {
            val updatedHistory = _aiChatHistory.value.toMutableList()
            updatedHistory.add(Content(role = "user", parts = listOf(Part(text = "Suggest some visual additions and companion compatibility checks!"))))
            updatedHistory.add(
                Content(
                    role = "model",
                    parts = listOf(
                        Part(
                            text = "🔒 Free AI Advisor consultation limit reached (2/2 queries).\n\nPlease upgrade to FloraFlow PRO to unlock advanced layout analysis, visual companion additions, and expert styling advice! 🌸✨"
                        )
                    )
                )
            )
            _aiChatHistory.value = updatedHistory
            return
        }

        val plantsListStr = _activePlants.value.joinToString(", ") { it.name }
        val prompt = "I have a garden layout styled as a '${layout.style}' in a '${layout.climate}' climate region. " +
                "The current vegetation includes: [$plantsListStr]. " +
                "Please analyze this combination, tell me if they are highly compatible, suggest 2 other species that thrive in these circumstances, and give me a layout decoration tip."

        _isAiLoading.value = true
        _aiStatus.value = "Analyzing garden visual blueprint..."

        viewModelScope.launch {
            val advice = GeminiApiClient.getGardeningAdvice(prompt)
            val updatedHistory = _aiChatHistory.value.toMutableList()
            updatedHistory.add(Content(role = "user", parts = listOf(Part(text = "Suggest some visual additions and companion compatibility checks!"))))
            updatedHistory.add(Content(role = "model", parts = listOf(Part(text = advice))))
            _aiChatHistory.value = updatedHistory
            _isAiLoading.value = false
            _aiStatus.value = ""
        }
    }

    // Automatically generate visual plant selections for layouts using Gemini
    fun generateAILayoutSuggestion() {
        val layout = _activeLayout.value ?: return
        
        val userCount = _aiChatHistory.value.count { it.role == "user" }
        if (!_isPremium.value && userCount >= 2) {
            val updatedHistory = _aiChatHistory.value.toMutableList()
            updatedHistory.add(Content(role = "user", parts = listOf(Part(text = "Generate a companion design blueprint for my space!"))))
            updatedHistory.add(
                Content(
                    role = "model",
                    parts = listOf(
                        Part(
                            text = "🔒 Free AI Advisor consultation limit reached (2/2 queries).\n\nPlease upgrade to FloraFlow PRO to unlock AI garden layout generator, instant database seeding, and dynamic blueprinting! 🌸✨"
                        )
                    )
                )
            )
            _aiChatHistory.value = updatedHistory
            return
        }
        
        val prompt = "Suggest a brand new layout idea. " +
                "For a garden with the style '${layout.style}' and climate of '${layout.climate}', name 3 highly compatible, beautifully flowering plants or useful crops. " +
                "Format your answer as simple lines containing: PlantName | PlantType | IdealSoil | SunExposure. " +
                "Follow this list with a clear, concise decorating suggestion."

        _isAiLoading.value = true
        _aiStatus.value = "Sowing AI botanical ideas..."

        viewModelScope.launch {
            val response = GeminiApiClient.getGardeningAdvice(prompt)
            
            // Feed response to the chat log
            val updatedHistory = _aiChatHistory.value.toMutableList()
            updatedHistory.add(Content(role = "user", parts = listOf(Part(text = "Generate a companion design blueprint for my space!"))))
            updatedHistory.add(Content(role = "model", parts = listOf(Part(text = response))))
            _aiChatHistory.value = updatedHistory
            
            // Try and parse plants from response to inject into database
            try {
                // Find lines having "|" symbol
                val lines = response.lines().filter { it.contains("|") }
                for (line in lines) {
                    val parts = line.split("|").map { it.trim() }
                    if (parts.size >= 2) {
                        val name = parts[0].replace(Regex("^[^a-zA-Z0-9]+"), "") // Clean up bullet symbols if any
                        val type = parts[1]
                        val soil = if (parts.size >= 3) parts[2] else "Standard garden compost"
                        val sun = if (parts.size >= 4) parts[3] else "Full sun to dappled shade"
                        
                        repository.insertPlant(
                            Plant(
                                layoutId = layout.id,
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
            } catch (e: Exception) {
                // Ignore parsing slip-ups, response remains visible in chat
            }

            _isAiLoading.value = false
            _aiStatus.value = ""
        }
    }

    // --- AR Preview Control Methods ---
    fun addArPlant(name: String, emoji: String) {
        val list = _arPlacedPlants.value.toMutableList()
        val nextId = (list.maxOfOrNull { it.id } ?: 0) + 1
        list.add(
            ArPlantPlacement(
                id = nextId,
                name = name,
                emoji = emoji,
                offsetX = 0f,
                offsetY = 0f,
                scale = 1.0f,
                rotationDegrees = 0f
            )
        )
        _arPlacedPlants.value = list
    }

    fun updateArPlantPosition(id: Int, dx: Float, dy: Float) {
        val list = _arPlacedPlants.value.map {
            if (it.id == id) {
                it.copy(offsetX = it.offsetX + dx, offsetY = it.offsetY + dy)
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
}

// Data holder for mock AR positioning. Allows drag-and-drop scaling of elements in camera mode
data class ArPlantPlacement(
    val id: Int,
    val name: String,
    val emoji: String,
    val offsetX: Float,
    val offsetY: Float,
    val scale: Float,
    val rotationDegrees: Float
)
