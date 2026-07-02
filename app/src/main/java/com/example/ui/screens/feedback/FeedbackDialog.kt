package com.example.ui.screens.feedback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.viewmodel.GardenViewModel
import com.example.ui.components.FloraFlowButton
import com.example.ui.components.FloraFlowCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    viewModel: GardenViewModel
) {
    if (!visible) return

    val isSubmitting by viewModel.isSubmittingFeedback.collectAsState()
    val success by viewModel.feedbackSuccess.collectAsState()

    var category by remember { mutableStateOf("✨ Feature Request") }
    var rating by remember { mutableIntStateOf(5) }
    var comments by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Validations
    val isCommentValid = comments.isNotBlank() && comments.length <= 500
    val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()
    val isEmailValid = email.isBlank() || emailRegex.matches(email)

    LaunchedEffect(email) {
        emailError = email.isNotBlank() && !emailRegex.matches(email)
    }

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isSubmitting,
            dismissOnClickOutside = !isSubmitting,
            usePlatformDefaultWidth = false
        )
    ) {
        FloraFlowCard(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .testTag("feedback_dialog_card"),
            containerColor = MaterialTheme.colorScheme.background,
            elevation = 16.dp,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (success) {
                    // Success Screen with beautiful blooming flower canvas
                    SuccessFeedbackView(
                        category = category,
                        rating = rating,
                        comments = comments,
                        onDone = {
                            // Reset inputs and close
                            comments = ""
                            email = ""
                            category = "✨ Feature Request"
                            rating = 5
                            viewModel.resetFeedbackSuccess()
                            onDismiss()
                        }
                    )
                } else if (isSubmitting) {
                    // Syncing loader state
                    SyncingFeedbackView()
                } else {
                    // Interactive feedback form
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(scrollState)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Feedback,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Nurture FloraFlow",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("feedback_close_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close dialog",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Help us cultivate a better therapeutic garden experience. Share your thoughts, report bugs, or suggest features!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Category Chips
                        Text(
                            text = "What is this regarding?",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val categories = listOf(
                            "✨ Feature Request",
                            "🐛 Bug Report",
                            "🌸 App Design/Vibe",
                            "💬 General Comment"
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.take(2).forEach { cat ->
                                CategoryChip(
                                    label = cat,
                                    selected = category == cat,
                                    onClick = { category = cat }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.drop(2).forEach { cat ->
                                CategoryChip(
                                    label = cat,
                                    selected = category == cat,
                                    onClick = { category = cat }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Rating Scale
                        Text(
                            text = "How would you rate your experience?",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = getRatingDescriptor(rating),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        BotanicalRatingRow(
                            selectedRating = rating,
                            onRatingSelected = { rating = it }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Comments Textfield
                        Text(
                            text = "Tell us more",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = comments,
                            onValueChange = { if (it.length <= 500) comments = it },
                            placeholder = { Text("Tell us how we can nurture your experience...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .testTag("feedback_comments_input"),
                            shape = RoundedCornerShape(16.dp),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            supportingText = {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "${comments.length}/500",
                                        modifier = Modifier.align(Alignment.CenterEnd),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (comments.length > 450) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Contact Email (Optional)
                        Text(
                            text = "Email address (Optional)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it.trim() },
                            placeholder = { Text("username@example.com") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("feedback_email_input"),
                            shape = RoundedCornerShape(14.dp),
                            isError = emailError,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            ),
                            supportingText = {
                                if (emailError) {
                                    Text(
                                        text = "Please enter a valid email address",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                errorBorderColor = MaterialTheme.colorScheme.error
                            )
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Submit Button
                        FloraFlowButton(
                            text = "Cultivate Feedback",
                            onClick = {
                                if (isCommentValid && isEmailValid) {
                                    viewModel.submitFeedback(category, rating, comments, email)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("feedback_submit_button"),
                            enabled = isCommentValid && isEmailValid,
                            leadingIcon = Icons.Default.Eco
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("category_chip_${label.replace(" ", "_")}"),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun BotanicalRatingRow(
    selectedRating: Int,
    onRatingSelected: (Int) -> Unit
) {
    val ratingOptions = listOf(
        RatingOption(1, "🥀", "Wilted"),
        RatingOption(2, "🍂", "Stagnant"),
        RatingOption(3, "🌿", "Growing"),
        RatingOption(4, "🌸", "Blooming"),
        RatingOption(5, "🌟", "Thriving")
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("botanical_rating_row"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ratingOptions.forEach { opt ->
            val isSelected = selectedRating == opt.value
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.25f else 1.0f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onRatingSelected(opt.value) }
                    .testTag("rating_item_${opt.value}")
                    .padding(4.dp)
            ) {
                Text(
                    text = opt.emoji,
                    fontSize = 32.sp,
                    modifier = Modifier.scale(scale)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = opt.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

data class RatingOption(val value: Int, val emoji: String, val name: String)

fun getRatingDescriptor(rating: Int): String {
    return when (rating) {
        1 -> "🥀 Wilted: Having critical issues or blockers. Feeling extremely discouraged."
        2 -> "🍂 Stagnant: Below expectations, needs direct fertilization and improvements."
        3 -> "🌿 Growing: Performing adequately, but there are areas with room for cultivation."
        4 -> "🌸 Blooming: Highly satisfying, therapeutic vibe is blooming beautifully."
        5 -> "🌟 Thriving: Pure bliss! The botanical garden app is perfectly aligned with peace."
        else -> "Nurturing design ideas"
    }
}

@Composable
fun SyncingFeedbackView() {
    val infiniteTransition = rememberInfiniteTransition(label = "rotating_loader")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("feedback_syncing_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Eco,
            contentDescription = "Syncing in progress",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(72.dp)
                .rotate(rotation)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Cultivating Feedback...",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Transmitting your botanical insights securely to our repository. Please wait.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun SuccessFeedbackView(
    category: String,
    rating: Int,
    comments: String,
    onDone: () -> Unit
) {
    var triggerBloom by remember { mutableStateOf(false) }
    val bloomProgress by animateFloatAsState(
        targetValue = if (triggerBloom) 1.0f else 0.0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 100f)
    )

    LaunchedEffect(Unit) {
        // Trigger flower blooming animation shortly after screen opens
        triggerBloom = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("feedback_success_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF2E7D32), // High contrast deep green
                modifier = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Feedback Cultivated!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Your insights have been successfully compiled & planted.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Custom drawn sprouting & blooming canvas
        BloomingSproutCanvas(
            progress = bloomProgress,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(vertical = 8.dp)
                .testTag("blooming_sprout_canvas")
        )

        // Summary Card
        FloraFlowCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            elevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SEED SUMMARY",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Category: $category",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Rating: ${getRatingEmoji(rating)} ${getRatingName(rating)} ($rating/5)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (comments.length > 40) {
                    Text(
                        text = "Review: \"${comments.take(40)}...\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Review: \"$comments\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Dismiss Action
        FloraFlowButton(
            text = "Done",
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("feedback_success_done_button")
        )
    }
}

fun getRatingEmoji(rating: Int): String {
    return when (rating) {
        1 -> "🥀"
        2 -> "🍂"
        3 -> "🌿"
        4 -> "🌸"
        5 -> "🌟"
        else -> "🌱"
    }
}

fun getRatingName(rating: Int): String {
    return when (rating) {
        1 -> "Wilted"
        2 -> "Stagnant"
        3 -> "Growing"
        4 -> "Blooming"
        5 -> "Thriving"
        else -> "Healthy"
    }
}

@Composable
fun BloomingSproutCanvas(progress: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val groundY = height * 0.82f
        val centerX = width / 2f

        // Draw Soil Mound
        drawArc(
            color = Color(0xFF533825), // Warm rich dark soil
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = androidx.compose.ui.geometry.Offset(centerX - 40.dp.toPx(), groundY - 10.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(80.dp.toPx(), 20.dp.toPx())
        )

        if (progress > 0f) {
            val maxStemHeight = height * 0.48f
            val currentStemHeight = maxStemHeight * progress
            val endY = groundY - currentStemHeight

            // Stem Bezier Curve
            val stemPath = Path().apply {
                moveTo(centerX, groundY)
                quadraticTo(
                    centerX - 12.dp.toPx() * progress,
                    groundY - (currentStemHeight / 2f),
                    centerX,
                    endY
                )
            }

            drawPath(
                path = stemPath,
                color = Color(0xFF6A994E),
                style = Stroke(
                    width = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            // Draw side leaves
            if (progress > 0.3f) {
                val leafProgress = ((progress - 0.3f) / 0.7f).coerceIn(0f, 1f)
                val leafY = groundY - (maxStemHeight * 0.22f)
                val leafWidth = 14.dp.toPx() * leafProgress

                // Left Leaf path
                val leftLeaf = Path().apply {
                    moveTo(centerX - 2.dp.toPx(), leafY)
                    quadraticTo(
                        centerX - 16.dp.toPx() * leafProgress,
                        leafY - 8.dp.toPx() * leafProgress,
                        centerX - leafWidth - 2.dp.toPx(),
                        leafY - 2.dp.toPx()
                    )
                    quadraticTo(
                        centerX - 8.dp.toPx() * leafProgress,
                        leafY + 4.dp.toPx() * leafProgress,
                        centerX - 2.dp.toPx(),
                        leafY
                    )
                }
                drawPath(path = leftLeaf, color = Color(0xFF8AB17D))

                // Right Leaf path (slightly higher up stem)
                val rightLeafY = groundY - (maxStemHeight * 0.42f)
                val rightLeaf = Path().apply {
                    moveTo(centerX + 2.dp.toPx(), rightLeafY)
                    quadraticTo(
                        centerX + 16.dp.toPx() * leafProgress,
                        rightLeafY - 8.dp.toPx() * leafProgress,
                        centerX + leafWidth + 2.dp.toPx(),
                        rightLeafY - 2.dp.toPx()
                    )
                    quadraticTo(
                        centerX + 8.dp.toPx() * leafProgress,
                        rightLeafY + 4.dp.toPx() * leafProgress,
                        centerX + 2.dp.toPx(),
                        rightLeafY
                    )
                }
                drawPath(path = rightLeaf, color = Color(0xFF8AB17D))
            }

            // Draw Blooming Flower head at the tip
            if (progress > 0.65f) {
                val bloomProgress = ((progress - 0.65f) / 0.35f).coerceIn(0f, 1f)
                val flowerRadius = 14.dp.toPx() * bloomProgress
                val centerFlowerX = centerX
                val centerFlowerY = endY

                // Draw 5 petals around the stem tip
                val petalColor = Color(0xFFF4A261) // Sunset Warm Orange / Peach
                for (i in 0 until 5) {
                    val angle = (i * 2 * Math.PI / 5) - Math.PI / 2
                    val petalX = centerFlowerX + (flowerRadius * 0.72f * Math.cos(angle)).toFloat()
                    val petalY = centerFlowerY + (flowerRadius * 0.72f * Math.sin(angle)).toFloat()
                    drawCircle(
                        color = petalColor,
                        radius = flowerRadius * 0.58f,
                        center = androidx.compose.ui.geometry.Offset(petalX, petalY)
                    )
                }

                // Core bud
                drawCircle(
                    color = Color(0xFFFFE359), // Sunshine yellow core center
                    radius = flowerRadius * 0.38f,
                    center = androidx.compose.ui.geometry.Offset(centerFlowerX, centerFlowerY)
                )
            }
        }
    }
}
