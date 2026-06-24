package com.example.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.GardenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateAppDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    viewModel: GardenViewModel,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val context = LocalContext.current
    var rating by remember { mutableIntStateOf(0) }
    var reviewText by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }

    // Pulse animation for selected stars
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("rate_app_dialog")
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RateAppDialogHeader(onDismiss = onDismiss)

                if (!isSubmitted) {
                    Text(
                        text = "How has your gardening experience been so far? Rate us to support a solo creator! 🪴",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    StarRatingRow(
                        rating = rating,
                        onRatingChanged = { rating = it }
                    )

                    AnimatedVisibility(
                        visible = rating > 0,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (rating >= 4) {
                                HighRatingContent(
                                    context = context,
                                    onSubmitted = { isSubmitted = true }
                                )
                            } else {
                                LowRatingContent(
                                    rating = rating,
                                    reviewText = reviewText,
                                    onReviewTextChanged = { reviewText = it },
                                    viewModel = viewModel,
                                    context = context,
                                    onSubmitted = { isSubmitted = true }
                                )
                            }
                        }
                    }
                } else {
                    SuccessContent(rating = rating, onDismiss = onDismiss)
                }
            }
        }
    }
}

@Composable
private fun RateAppDialogHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Rate FloraFlow",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
    }
}

@Composable
private fun StarRatingRow(rating: Int, onRatingChanged: (Int) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        for (i in 1..5) {
            val isSelected = i <= rating
            Icon(
                imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "$i Stars",
                tint = if (isSelected) Color(0xFFFFB300) else MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier
                    .size(42.dp)
                    .clickable { onRatingChanged(i) }
            )
        }
    }
}

@Composable
private fun HighRatingContent(
    context: android.content.Context,
    onSubmitted: () -> Unit
) {
    Text(
        text = "Wow! We're so glad you're enjoying FloraFlow. Please leave a review on the Play Store to help others find us! \ud83c\udf38",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center
    )

    Button(
        onClick = {
            val packageName = context.packageName
            val marketIntent = Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }
            val webIntent = Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$packageName".toUri())
            try {
                context.startActivity(marketIntent)
            } catch (e: Exception) {
                context.startActivity(webIntent)
            }
            onSubmitted()
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("Write Review on Play Store", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LowRatingContent(
    rating: Int,
    reviewText: String,
    onReviewTextChanged: (String) -> Unit,
    viewModel: GardenViewModel,
    context: android.content.Context,
    onSubmitted: () -> Unit
) {
    Text(
        text = "We're sorry to hear that. What can we do to make your experience better?",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center
    )

    OutlinedTextField(
        value = reviewText,
        onValueChange = onReviewTextChanged,
        placeholder = { Text("Tell us how to improve...", fontSize = 14.sp) },
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp),
        maxLines = 3,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary
        )
    )

    Button(
        onClick = {
            if (reviewText.isNotBlank()) {
                viewModel.submitFeedback(
                    category = "Play Store Rating Fallback (Stars: $rating)",
                    rating = rating,
                    comments = reviewText,
                    email = "anonymous@review.flow"
                )
                Toast.makeText(context, "Thank you! We've received your feedback.", Toast.LENGTH_SHORT).show()
            }
            onSubmitted()
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("Submit Feedback", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SuccessContent(rating: Int, onDismiss: () -> Unit) {
    Text(
        text = if (rating >= 4) "🎉 Thank you for supporting FloraFlow!" else "🌱 Thank you for your honest feedback. We are constantly improving our garden design tools based on your thoughts.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Medium
    )

    Button(
        onClick = onDismiss,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("Close", fontWeight = FontWeight.Bold)
    }
}
