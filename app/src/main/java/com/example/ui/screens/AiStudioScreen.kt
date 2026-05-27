package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.GardenViewModel
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

    val userQueriesCount = remember(chatHistory) {
        chatHistory.count { it.role == "user" }
    }

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Smooth scroll to latest message when history increases
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Dr. Julian Greenleaf's Glowing Live Profile Header
        BotanistProfileHeader(
            isAiLoading = isAiLoading,
            aiStatus = aiStatus,
            hasHistory = chatHistory.isNotEmpty(),
            onClearChat = { viewModel.clearAiChat() }
        )

        // 2. Transsender Soundwave Voice Synth Feedback
        LiveBotanistVoiceSynth(isGenerating = isAiLoading)

        // 3. Sensor Lab Console (synchronized with current 2D planner status)
        BotanistLiveLabConsole(activeLayout = activeLayout, activePlants = activePlants)

        // 4. Main Chat List Box container
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

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                "🌿 SELECT QUICK DIAGNOSTIC CHECK:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val suggestions = listOf(
                                "🌱 Suggest perfect companion plant matches" to "Suggest highly compatible companion plants for a Zen style Garden design. What thrives alongside Bonsai Cherry and Lavender?",
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
                                    border = androidx.compose.foundation.BorderStroke(
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
                                                    Color(0xFF386641), // Vibrant leaf green
                                                    Color(0xFF6A994E)  // Fresh sprout green
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
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    if (!isUser) {
                                        Text("🍃", fontSize = 12.sp)
                                    }
                                    Text(
                                        text = text,
                                        fontSize = 13.sp,
                                        color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                                        style = androidx.compose.ui.text.TextStyle(lineHeight = 17.sp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Send message or premium wall trigger section
        if (!isPremium && userQueriesCount >= 2) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
                    .testTag("premium_ai_paywall_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 2.dp,
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
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
                            text = "Consultation Limit Reached (2/2)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = "You have exhausted your free botanist credentials. Upgrade to PRO to unlock unlimited Live expert analyses, 2D model companions, and diagnostic AR overlays!",
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
                            text = "Live consultations remaining: ${2 - userQueriesCount}/2",
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                        keyboardActions = KeyboardActions(onSend = {
                            if (textInput.isNotBlank()) {
                                viewModel.sendAiChatMessage(textInput)
                                textInput = ""
                            }
                        })
                    )

                    FloatingActionButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.sendAiChatMessage(textInput)
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
}

@Composable
fun BotanistProfileHeader(
    isAiLoading: Boolean,
    aiStatus: String,
    hasHistory: Boolean,
    onClearChat: () -> Unit
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
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
                Text("🧑‍🔬", fontSize = 24.sp)
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.BottomEnd)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Dr. Julian Greenleaf",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .graphicsLayer { alpha = pulseAlpha }
                            .background(Color(0xFF4CAF50), CircleShape)
                    )
                }
                Text(
                    text = "Live Gemini Agent • PhD in Botanical Systems",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (hasHistory) {
                IconButton(
                    onClick = onClearChat,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = "Clear Chat",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LiveBotanistVoiceSynth(
    isGenerating: Boolean
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val infiniteTransition = rememberInfiniteTransition(label = "voiceSynth")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sineOffset"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1.4f)
            ) {
                Icon(
                    imageVector = if (isGenerating) Icons.Default.Mic else Icons.Default.Hearing,
                    contentDescription = "Voice synthesis feed",
                    tint = if (isGenerating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = if (isGenerating) "Julian is analyzing microclimates..." else "MICROCLIMATE SYSTEM FEED STANDBY",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGenerating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        letterSpacing = 0.3.sp
                    )
                    Text(
                        text = if (isGenerating) "Transmitting direct synaptic plant feedback..." else "Live monitoring active...",
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 14.dp)
            ) {
                val waveWidth = size.width
                val waveHeight = size.height
                val midY = waveHeight / 2f
                val amplitude = if (isGenerating) 12f else 3f
                val frequency = if (isGenerating) 0.08f else 0.04f

                val points = mutableListOf<Offset>()
                for (x in 0..waveWidth.toInt() step 2) {
                    val y = midY + amplitude * sin((x * frequency) + offset)
                    points.add(Offset(x.toFloat(), y))
                }

                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = if (isGenerating) primaryColor else secondaryColor.copy(alpha = 0.6f),
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = if (isGenerating) 3.5f else 1.8f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
fun BotanistLiveLabConsole(
    activeLayout: com.example.data.model.GardenLayout?,
    activePlants: List<com.example.data.model.Plant>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                        imageVector = Icons.Default.Spa,
                        contentDescription = "Sensor flow icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "BOTANICAL LAB TRANSSENDER FEED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.8.sp
                    )
                }
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

            if (activeLayout != null) {
                Text(
                    text = "Layout: ${activeLayout.name} (${activeLayout.style} • ${activeLayout.climate})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BotanistMetricItem(
                        icon = "🌡️",
                        label = "Soil Temp",
                        value = "21.6°C",
                        desc = "Optimal range",
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
                        modifier = Modifier.weight(1f)
                    )
                    BotanistMetricItem(
                        icon = "🌱",
                        label = "Species Logs",
                        value = "${activePlants.size} Active",
                        desc = "In database",
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Text(
                    text = "No Garden layout active. Select or create design blueprints in the 2D Planner to stream Live lab sensors!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f), RoundedCornerShape(8.dp))
            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
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
