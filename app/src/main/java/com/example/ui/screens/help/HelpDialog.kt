package com.example.ui.screens.help

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.viewmodel.GardenViewModel

data class FAQItem(
    val id: Int,
    val question: String,
    val answer: String,
    val category: String
)

// Static FAQ Data
val faqs = listOf(
    FAQItem(
        id = 1,
        question = "How do I design a garden in the 2D Planner?",
        answer = "Go to the '2D Planner' tab. Tap the 'Add Plant' button, select a plant species from our database, and tap anywhere on the canvas to place it. You can drag and drop plants to rearrange them, tap to select, and resize or rotate them.",
        category = "2D Planner"
    ),
    FAQItem(
        id = 2,
        question = "What is the AI Advisor and how can it help?",
        answer = "The AI Advisor is powered by Gemini and acts as your personal master gardener. Tap the 'AI Advisor' tab to start a chat. Ask about planting calendars, pest control, companion planting, or lighting requirements for specific species.",
        category = "AI Advisor"
    ),
    FAQItem(
        id = 3,
        question = "How do I use the AR Lens?",
        answer = "The AR Lens lets you view your garden design in the real world in 3D. Open the 'AR Lens' tab (requires FloraFlow Premium), select a plant from your list, scan a flat surface in your room or yard, and tap the screen to place a 3D model.",
        category = "AR Lens"
    ),
    FAQItem(
        id = 4,
        question = "Is a premium subscription required?",
        answer = "FloraFlow is free for basic 2D layout planning, greenhouse inventory, and standard AI advice. FloraFlow Premium unlocks the AR Lens, unlimited AI queries, custom watering logs, and premium plant species catalog access.",
        category = "Premium"
    ),
    FAQItem(
        id = 5,
        question = "How do I water my plants and log schedules?",
        answer = "In the 'Greenhouse' tab, select a plant in your garden and tap 'Log Watering'. You can see the water level meter update in real time. The greenhouse will show alerts when the plant is dry based on its specific watering interval.",
        category = "Getting Started"
    ),
    FAQItem(
        id = 6,
        question = "Can I export or share my designs?",
        answer = "Yes! In the 2D Planner, you can tap the export icon to save a high-resolution screenshot of your layout directly to your device gallery or share it with friends.",
        category = "2D Planner"
    ),
    FAQItem(
        id = 7,
        question = "How do I change the app theme?",
        answer = "Open Settings (cog icon in the top right) and select between 'System' (follows device preferences), 'Light', or 'Dark' mode. Settings are persisted automatically.",
        category = "Getting Started"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    viewModel: GardenViewModel,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .testTag("help_dialog_card"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.background
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Help & Support Center",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Learn how to use FloraFlow & troubleshoot issues",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("help_close_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Help Center")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content
                Box(modifier = Modifier.weight(1f)) {
                    FaqTabContent()
                }
            }
        }
    }
}

@Composable
fun FaqTabContent() {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var expandedFaqId by remember { mutableStateOf<Int?>(null) }

    val categories = listOf("All", "Getting Started", "2D Planner", "AR Lens", "AI Advisor", "Premium")

    val filteredFaqs = remember(searchQuery, selectedCategory) {
        faqs.filter { faq ->
            val matchesSearch = faq.question.contains(searchQuery, ignoreCase = true) ||
                    faq.answer.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == "All" || faq.category.equals(selectedCategory, ignoreCase = true)
            matchesSearch && matchesCategory
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search frequently asked questions...") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("faq_search_input"),
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category) },
                    modifier = Modifier.testTag("faq_chip_$category")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // FAQ list
        if (filteredFaqs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpCenter,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No questions found matching '$searchQuery'",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredFaqs) { faq ->
                    val isExpanded = expandedFaqId == faq.id
                    FAQCard(
                        faq = faq,
                        isExpanded = isExpanded,
                        onToggle = { expandedFaqId = if (isExpanded) null else faq.id }
                    )
                }
            }
        }
    }
}

@Composable
fun FAQCard(
    faq: FAQItem,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val rotationState by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .testTag("faq_card_${faq.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = faq.question,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier
                        .rotate(rotationState)
                        .testTag("faq_chevron_${faq.id}")
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = faq.answer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
