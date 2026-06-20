package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.window.Dialog
import com.example.ui.viewmodel.GardenViewModel
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun AiStudioScreen(
    viewModel: GardenViewModel,
    modifier: Modifier = Modifier
) {
    val chatHistory by viewModel.aiChatHistory.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val aiStatus by viewModel.aiStatus.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val activeLayout by viewModel.activeLayout.collectAsStateWithLifecycle()
    val activePlants by viewModel.activePlants.collectAsStateWithLifecycle()
    val assessmentScore by viewModel.assessmentScore.collectAsStateWithLifecycle()
    val lowestCategories by viewModel.lowestCategories.collectAsStateWithLifecycle()
    val isAssessmentSkipped by viewModel.isAssessmentSkipped.collectAsStateWithLifecycle()

    val userQueriesCount = remember(chatHistory) {
        chatHistory.count { it.role == "user" }
    }

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showLabPopup by remember { mutableStateOf(false) }
    var attachedImage by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showAttachmentDialog by remember { mutableStateOf(false) }

    // Toggle states for portrait dashboard features
    var showNeuralLoad by remember { mutableStateOf(false) }
    var showBreather by remember { mutableStateOf(false) }

    val onMetricClicked: (String) -> Unit = { metricType ->
        val layout = activeLayout
        if (layout != null) {
            val msg = when (metricType) {
                "soil_temp" -> "Dr. Julian, my current soil temperature is 21.6°C. Is this optimal for my '${layout.style}' garden in '${layout.climate}' climate? What biology-driven suggestions do you have to regulate it?"
                "moisture" -> {
                    val moistureVal = when (layout.style) {
                        "Desert", "Xeriscaping" -> "18%"
                        "Zen Garden" -> "44%"
                        "Tropical" -> "75%"
                        else -> "52%"
                    }
                    "Dr. Julian, my garden soil moisture is currently at $moistureVal. How does this level affect the transpiration and nutrient absorption for a '${layout.style}' styled space?"
                }
                "species" -> {
                    val plantsList = activePlants.joinToString(", ") { it.name }
                    "Dr. Julian, I have these active species in my garden: [$plantsList]. Can you analyze their companion compatibility and physiological synergy?"
                }
                else -> ""
            }
            if (msg.isNotBlank()) {
                viewModel.sendAiChatMessage(msg)
            }
        }
    }

    if (showAttachmentDialog) {
        AlertDialog(
            onDismissRequest = { showAttachmentDialog = false },
            title = { Text("Simulate Plant Photo Capture", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select a mock plant condition to simulate camera capture and pass to Dr. Julian for visual diagnosis:")
                    val mockOptions = listOf(
                        "Rose Powdery Mildew Spot" to "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7",
                        "Monstera Chlorosis (Yellowing)" to "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7",
                        "Tomato Aphid Infestation" to "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7"
                    )
                    mockOptions.forEach { (name, b64) ->
                        Button(
                            onClick = {
                                attachedImage = name to b64
                                showAttachmentDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text(name, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAttachmentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Smooth scroll to latest message when history increases
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp || configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val isWideScreen = isLandscape && (isTablet || configuration.screenWidthDp >= 600)

    if (!isWideScreen) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // 1. Dr. Julian Greenleaf's Glowing Live Profile Header
            BotanistProfileHeader(
                isAiLoading = isAiLoading,
                aiStatus = aiStatus,
                hasHistory = chatHistory.isNotEmpty(),
                onClearChat = { viewModel.clearAiChat() },
                onOpenLab = { showLabPopup = true }
            )

            // Interactive Toggle Control Bar for Portrait Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ControlTabButton(
                    icon = Icons.Default.Favorite,
                    label = "Neural Scan",
                    active = showNeuralLoad,
                    onClick = { showNeuralLoad = !showNeuralLoad },
                    modifier = Modifier.weight(1f)
                )
                ControlTabButton(
                    icon = Icons.Default.SelfImprovement,
                    label = "Breather",
                    active = showBreather,
                    onClick = { showBreather = !showBreather },
                    modifier = Modifier.weight(1f)
                )
            }

            AnimatedVisibility(
                visible = showNeuralLoad,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                NeuralLoadDashboardWidget(
                    assessmentScore = assessmentScore,
                    lowestCategories = lowestCategories,
                    onStartAssessment = { viewModel.resetAssessment() }
                )
            }

            AnimatedVisibility(
                visible = showBreather,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                BreathingGardenCircle()
            }

            // 3. Main Chat List Box container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(8.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (chatHistory.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "🌱 Live Garden Intelligence Consultation",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Ask anything about companion planting compatibility, Soil temperature variations, microclimate diagnostics, or therapeutic mental benefits of plants.",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                Text(
                                    "🌿 SELECT QUICK DIAGNOSTIC CHECK:",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val suggestions = listOf(
                                    "🌱 Suggest perfect companion plant matches" to "Suggest highly compatible companion plants for a Zen style Garden design. What thrives alongside Bonsai Juniper and Lavender?",
                                    "🐛 Analyze yellowing leaves / plant pest diagnosis" to "How do I diagnose yellowing speckled leaves on young plants, and what organic pesticides act as a therapeutic cure?",
                                    "🧘 Discuss therapy and nature cognitive wellness" to "How does maintaining, smelling, or surrounding ourselves with a green garden reduce cortisol levels and improve microclimate mindfulness?"
                                )

                                suggestions.forEach { pair ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                viewModel.sendAiChatMessage(pair.second)
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.AutoAwesome,
                                                contentDescription = "Diagnostic action",
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = pair.first,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(chatHistory) { content ->
                            val isUser = content.role == "user"
                            val text = content.parts.firstOrNull()?.text ?: ""

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                            ) {
                                if (!isUser) {
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 8.dp, top = 2.dp)
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🧑‍🔬", fontSize = 16.sp)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .widthIn(max = 280.dp)
                                        .testTag(if (isUser) "user_chat_bubble" else "model_chat_bubble")
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isUser) 16.dp else 4.dp,
                                                bottomEnd = if (isUser) 4.dp else 16.dp
                                            )
                                        )
                                        .background(
                                            if (isUser) {
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        Color(0xFF386641),
                                                        Color(0xFF6A994E)
                                                    )
                                                )
                                            } else {
                                                Brush.linearGradient(
                                                    listOf(
                                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                        MaterialTheme.colorScheme.surface
                                                    )
                                                )
                                            }
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isUser) Color.Transparent else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isUser) 16.dp else 4.dp,
                                                bottomEnd = if (isUser) 4.dp else 16.dp
                                            )
                                        )
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (!isUser) {
                                            Text(
                                                text = "DR. JULIAN GREENLEAF",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.Top,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            if (!isUser) {
                                                Text("🍃", fontSize = 12.sp)
                                            }
                                            Text(
                                                text = text,
                                                fontSize = 13.sp,
                                                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                                                style = androidx.compose.ui.text.TextStyle(lineHeight = 17.sp),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Send message or premium wall trigger section
            if (!isPremium && (userQueriesCount >= 3)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                        .testTag("premium_ai_paywall_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    ),
                    border = BorderStroke(
                        width = 2.dp,
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFFFFD54F), MaterialTheme.colorScheme.primary)
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Premium lock icon",
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Consultation Limit Reached (3/3)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "You have exhausted your free credentials. Upgrade to PRO to unlock unlimited Live expert analyses, 2D model companions, and diagnostic AR overlays!",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                        )
                        Button(
                            onClick = { viewModel.upgradeToPremium() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Unlock Unlimited AI Gemini PRO ✨", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!isPremium) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Live consultations remaining: ${3 - userQueriesCount}/3",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "GO PRO ✨",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { viewModel.upgradeToPremium() }
                            )
                        }
                    }

                    if (attachedImage != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Spa,
                                        contentDescription = "Diagnostic leaf",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Photo Capture Diagnosis",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = attachedImage?.first ?: "",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { attachedImage = null }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear image attachment", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { showAttachmentDialog = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = "Attach plant photo for diagnosis",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("Ask Julian about soil compatibility...", fontSize = 13.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_chat_text_input"),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (textInput.isNotBlank() || attachedImage != null) {
                                        viewModel.sendAiChatMessage(
                                            message = if (textInput.isBlank() && attachedImage != null) "Diagnose plant: ${attachedImage?.first}" else textInput,
                                            imageBytesBase64 = attachedImage?.second,
                                            imageMimeType = "image/gif"
                                        )
                                        attachedImage = null
                                        textInput = ""
                                    }
                                }
                            )
                        )

                        FloatingActionButton(
                            onClick = {
                                if (textInput.isNotBlank() || attachedImage != null) {
                                    viewModel.sendAiChatMessage(
                                        message = if (textInput.isBlank() && attachedImage != null) "Diagnose plant: ${attachedImage?.first}" else textInput,
                                        imageBytesBase64 = attachedImage?.second,
                                        imageMimeType = "image/gif"
                                    )
                                    attachedImage = null
                                    textInput = ""
                                }
                            },
                            modifier = Modifier
                                .size(54.dp)
                                .testTag("send_ai_chat_button"),
                            shape = RoundedCornerShape(16.dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send advice query")
                        }
                    }
                }
            }

            if (showLabPopup) {
                Dialog(onDismissRequest = { showLabPopup = false }) {
                    BotanistLiveLabConsole(
                        activeLayout = activeLayout, 
                        activePlants = activePlants,
                        onClose = { showLabPopup = false },
                        onMetricClicked = { metric ->
                            onMetricClicked(metric)
                            showLabPopup = false
                        }
                    )
                }
            }
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Left Column (Chat box, Header, and Input): Weight 0.55f
            Column(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BotanistProfileHeader(
                    isAiLoading = isAiLoading,
                    aiStatus = aiStatus,
                    hasHistory = chatHistory.isNotEmpty(),
                    onClearChat = { viewModel.clearAiChat() },
                    onOpenLab = { showLabPopup = true }
                )

                // Chat Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(8.dp)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (chatHistory.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillParentMaxSize()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "🌱 Live Garden Intelligence Consultation",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Ask anything about companion planting compatibility, Soil temperature variations, microclimate diagnostics, or therapeutic mental benefits of plants.",
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }
                            }
                        } else {
                            items(chatHistory) { content ->
                                val isUser = content.role == "user"
                                val text = content.parts.firstOrNull()?.text ?: ""

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                                ) {
                                    if (!isUser) {
                                        Box(
                                            modifier = Modifier
                                                .padding(end = 8.dp, top = 2.dp)
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("🧑‍🔬", fontSize = 16.sp)
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .widthIn(max = 280.dp)
                                            .testTag(if (isUser) "user_chat_bubble" else "model_chat_bubble")
                                            .clip(
                                                RoundedCornerShape(
                                                    topStart = 16.dp,
                                                    topEnd = 16.dp,
                                                    bottomStart = if (isUser) 16.dp else 4.dp,
                                                    bottomEnd = if (isUser) 4.dp else 16.dp
                                                )
                                            )
                                            .background(
                                                if (isUser) {
                                                    Brush.horizontalGradient(
                                                        listOf(
                                                            Color(0xFF386641),
                                                            Color(0xFF6A994E)
                                                        )
                                                    )
                                                } else {
                                                    Brush.linearGradient(
                                                        listOf(
                                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                            MaterialTheme.colorScheme.surface
                                                        )
                                                    )
                                                }
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isUser) Color.Transparent else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(
                                                    topStart = 16.dp,
                                                    topEnd = 16.dp,
                                                    bottomStart = if (isUser) 16.dp else 4.dp,
                                                    bottomEnd = if (isUser) 4.dp else 16.dp
                                                )
                                            )
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (!isUser) {
                                                Text(
                                                    text = "DR. JULIAN GREENLEAF",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    letterSpacing = 0.5.sp
                                                )
                                            }
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.Top,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                if (!isUser) {
                                                    Text("🍃", fontSize = 12.sp)
                                                }
                                                Text(
                                                    text = text,
                                                    fontSize = 13.sp,
                                                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                                                    style = androidx.compose.ui.text.TextStyle(lineHeight = 17.sp),
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Chat Input section
                if (!isPremium && (userQueriesCount >= 3)) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                            .testTag("premium_ai_paywall_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                        ),
                        border = BorderStroke(
                            width = 2.dp,
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFFFFD54F), MaterialTheme.colorScheme.primary)
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Premium lock icon",
                                    tint = Color(0xFFFFD54F),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Consultation Limit Reached (3/3)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = "You have exhausted your free credentials. Upgrade to PRO to unlock unlimited Live expert analyses, 2D model companions, and diagnostic AR overlays!",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                            )
                            Button(
                                onClick = { viewModel.upgradeToPremium() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Unlock Unlimited AI Gemini PRO ✨", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (!isPremium) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Live consultations remaining: ${3 - userQueriesCount}/3",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "GO PRO ✨",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { viewModel.upgradeToPremium() }
                                )
                            }
                        }

                        if (attachedImage != null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Spa,
                                            contentDescription = "Diagnostic leaf",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Photo Capture Diagnosis",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = attachedImage?.first ?: "",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { attachedImage = null }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear image attachment", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { showAttachmentDialog = true },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = "Attach plant photo for diagnosis",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            OutlinedTextField(
                                value = textInput,
                                onValueChange = { textInput = it },
                                placeholder = { Text("Ask Julian about soil compatibility...", fontSize = 13.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ai_chat_text_input"),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (textInput.isNotBlank() || attachedImage != null) {
                                            viewModel.sendAiChatMessage(
                                                message = if (textInput.isBlank() && attachedImage != null) "Diagnose plant: ${attachedImage?.first}" else textInput,
                                                imageBytesBase64 = attachedImage?.second,
                                                imageMimeType = "image/gif"
                                            )
                                            attachedImage = null
                                            textInput = ""
                                        }
                                    }
                                )
                            )

                            FloatingActionButton(
                                onClick = {
                                    if (textInput.isNotBlank() || attachedImage != null) {
                                        viewModel.sendAiChatMessage(
                                            message = if (textInput.isBlank() && attachedImage != null) "Diagnose plant: ${attachedImage?.first}" else textInput,
                                            imageBytesBase64 = attachedImage?.second,
                                            imageMimeType = "image/gif"
                                        )
                                        attachedImage = null
                                        textInput = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(54.dp)
                                    .testTag("send_ai_chat_button"),
                                shape = RoundedCornerShape(16.dp),
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send advice query")
                            }
                        }
                    }
                }
            }

            // Right Column (Sensors, suggestions): Weight 0.45f
            Column(
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Neural Load Dashboard Widget on widescreen
                NeuralLoadDashboardWidget(
                    assessmentScore = assessmentScore,
                    lowestCategories = lowestCategories,
                    onStartAssessment = { viewModel.resetAssessment() }
                )

                // Breathing Garden Circle on widescreen
                BreathingGardenCircle()

                BotanistLiveLabConsole(
                    activeLayout = activeLayout, 
                    activePlants = activePlants,
                    onClose = null,
                    onMetricClicked = onMetricClicked
                )

                // Suggestions / Quick diagnostic checks zone always available on widescreen
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "🌿 QUICK DIAGNOSTIC CHECKS:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        val suggestions = listOf(
                            "🌱 Suggest companion plant matches" to "Suggest highly compatible companion plants for a Zen style Garden design. What thrives alongside Bonsai Juniper and Lavender?",
                            "🐛 Analyze yellowing leaves diagnosis" to "How do I diagnose yellowing speckled leaves on young plants, and what organic pesticides act as a therapeutic cure?",
                            "🧘 Discuss therapy and nature wellness" to "How does maintaining, smelling, or surrounding ourselves with a green garden reduce cortisol levels and improve microclimate mindfulness?"
                        )

                        suggestions.forEach { pair ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.sendAiChatMessage(pair.second)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = "Diagnostic action",
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = pair.first,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(80.dp)) // Nav bar padding
            }
        }
    }
}

@Composable
fun BotanistProfileHeader(
    isAiLoading: Boolean,
    aiStatus: String,
    hasHistory: Boolean,
    onClearChat: () -> Unit,
    onOpenLab: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseDot"
    )

    // Animated glow scale for avatar ring
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(52.dp)
            ) {
                // Pulsing glow ring around avatar
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .graphicsLayer {
                            scaleX = glowScale
                            scaleY = glowScale
                            alpha = if (isAiLoading) pulseAlpha * 0.4f else 0.15f
                        }
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.tertiaryContainer
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🧑‍🔬", fontSize = 20.sp)
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.BottomEnd)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Dr. Julian Greenleaf",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 17.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                    if (isAiLoading) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .graphicsLayer { alpha = pulseAlpha }
                                .background(Color(0xFF4CAF50), CircleShape)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isAiLoading && aiStatus.isNotBlank()) aiStatus else "Live Gemini Agent • PhD in Botanical Systems",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    lineHeight = 16.sp
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onOpenLab,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = "Open Lab Console",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (hasHistory) {
                    IconButton(
                        onClick = onClearChat,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Chat",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BotanistLiveLabConsole(
    activeLayout: com.example.data.model.GardenLayout?,
    activePlants: List<com.example.data.model.Plant>,
    onClose: (() -> Unit)? = null,
    onMetricClicked: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(if (onClose != null) 16.dp else 2.dp),
        shape = RoundedCornerShape(if (onClose != null) 24.dp else 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (onClose != null) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(if (onClose != null) 20.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (onClose != null) 12.dp else 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (onClose != null) 8.dp else 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = "Sensor flow icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(if (onClose != null) 20.dp else 14.dp)
                    )
                    Text(
                        "BOTANICAL LAB TRANSSENDER FEED",
                        style = if (onClose != null) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.8.sp
                    )
                }
                if (onClose != null) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close popup")
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (activeLayout != null) "SYNCED" else "STANDBY",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Interactive Bio-Signal wave!
            if (activeLayout != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE SIGNAL TRANSLATOR",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "120.4Hz • STABLE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    BotanistLiveSignalWave(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (activeLayout != null) {
                Text(
                    text = "Layout: ${activeLayout.name} (${activeLayout.style} • ${activeLayout.climate})",
                    style = if (onClose != null) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (onClose != null) 8.dp else 6.dp)
                ) {
                    BotanistMetricItem(
                        icon = "🌡️",
                        label = "Soil Temp",
                        value = "21.6°C",
                        desc = "Optimal range",
                        onClick = { onMetricClicked("soil_temp") },
                        modifier = Modifier.weight(1f)
                    )
                    BotanistMetricItem(
                        icon = "💧",
                        label = "Moisture",
                        value = when (activeLayout.style) {
                            "Desert", "Xeriscaping" -> "18%"
                            "Zen Garden" -> "44%"
                            "Tropical" -> "75%"
                            else -> "52%"
                        },
                        desc = "Stable bed",
                        onClick = { onMetricClicked("moisture") },
                        modifier = Modifier.weight(1f)
                    )
                    BotanistMetricItem(
                        icon = "🌱",
                        label = "Species Logs",
                        value = "${activePlants.size} Active",
                        desc = "In database",
                        onClick = { onMetricClicked("species") },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Text(
                    text = "The greenhouse is quiet. Choose a plot from your garden beds to listen to its botanical signals.",
                    style = if (onClose != null) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = if (onClose != null) 12.dp else 4.dp)
                )
            }
        }
    }
}

@Composable
fun BotanistMetricItem(
    icon: String,
    label: String,
    value: String,
    desc: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f), RoundedCornerShape(8.dp))
            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(icon, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
        Text(label, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(desc, fontSize = 7.5.sp, color = Color.Gray)
    }
}

@Composable
fun ControlTabButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (active) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }
    val contentColor = if (active) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderColor = if (active) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    }

    Card(
        modifier = modifier
            .height(38.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
fun NeuralLoadDashboardWidget(
    assessmentScore: Int?,
    lowestCategories: List<String>,
    onStartAssessment: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (assessmentScore != null) {
                val zoneColor = when (assessmentScore) {
                    in 15..20 -> Color(0xFF4CAF50)
                    in 8..14 -> Color(0xFFFFC107)
                    else -> Color(0xFFF44336)
                }
                val zoneName = when (assessmentScore) {
                    in 15..20 -> "Green Zone — Low Neural Load"
                    in 8..14 -> "Yellow Zone — Moderate Load"
                    else -> "Red Zone — High Neural Load"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(zoneColor, CircleShape)
                        )
                        Text(
                            text = "Neural Load Score",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onStartAssessment,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retake Assessment",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(60.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { assessmentScore.toFloat() / 20f },
                            modifier = Modifier.size(60.dp),
                            color = zoneColor,
                            strokeWidth = 6.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round,
                        )
                        Text(
                            text = "$assessmentScore/20",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = zoneName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = zoneColor
                        )
                        if (lowestCategories.isNotEmpty()) {
                            Text(
                                text = "Focus areas: ${lowestCategories.take(3).joinToString(", ")}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Health scan icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Optimize Biophilic Harmony",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Take a 2-minute neural load scan to personalize Dr. Julian's recommendations.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 13.sp
                        )
                    }

                    Button(
                        onClick = onStartAssessment,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Scan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BreathingGardenCircle(modifier: Modifier = Modifier) {
    var isExpanded by remember { mutableStateOf(false) }
    var breathingState by remember { mutableStateOf("Ready") }
    var breathingProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            while (true) {
                breathingState = "Inhale"
                animate(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = tween(4000, easing = LinearOutSlowInEasing)
                ) { value, _ ->
                    breathingProgress = value
                }
                breathingState = "Hold"
                delay(4000)
                breathingState = "Exhale"
                animate(
                    initialValue = 1f,
                    targetValue = 0f,
                    animationSpec = tween(4000, easing = FastOutLinearInEasing)
                ) { value, _ ->
                    breathingProgress = value
                }
                delay(2000)
            }
        } else {
            breathingProgress = 0f
            breathingState = "Ready"
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SelfImprovement,
                        contentDescription = "Mindfulness icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Biophilic Breather",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = if (isExpanded) "Close" else "Begin",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Breathe in harmony with your botanical surroundings.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(160.dp)
                            .padding(16.dp)
                    ) {
                        val scale = 0.7f + (breathingProgress * 0.4f)
                        val alpha = 0.15f + (breathingProgress * 0.35f)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    this.alpha = alpha
                                }
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            Color.Transparent
                                        )
                                    ),
                                    shape = CircleShape
                                )
                        )

                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .graphicsLayer {
                                    scaleX = 0.9f + (breathingProgress * 0.2f)
                                    scaleY = 0.9f + (breathingProgress * 0.2f)
                                }
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    ),
                                    shape = CircleShape
                                )
                                .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = breathingState,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Text(
                        text = when (breathingState) {
                            "Inhale" -> "Slowly fill your lungs with fresh air (4s)..."
                            "Hold" -> "Suspend your breath, feel the calm (4s)..."
                            "Exhale" -> "Release tension, breathe out completely (4s)..."
                            else -> "Prepare to align your breath..."
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BotanistLiveSignalWave(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "signalWave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val secondPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -(2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "secondPhase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
    ) {
        val width = size.width
        val height = size.height
        val points = 80
        val wavePath1 = Path()
        val wavePath2 = Path()

        wavePath1.moveTo(0f, height / 2f)
        wavePath2.moveTo(0f, height / 2f)

        for (i in 0..points) {
            val x = i * (width / points.toFloat())
            val y1 = (height / 2f) + 8f * sin((i * 0.15f) + phase)
            wavePath1.lineTo(x, y1)

            val y2 = (height / 2f) + 5f * sin((i * 0.25f) + secondPhase)
            wavePath2.lineTo(x, y2)
        }

        drawPath(
            path = wavePath1,
            color = color,
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        )

        drawPath(
            path = wavePath2,
            color = color.copy(alpha = 0.4f),
            style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
