package com.example.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
fun InAppRatePromptDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    viewModel: GardenViewModel,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("in_app_rate_prompt_dialog")
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
                // Highlighting icon with a premium circle background
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFD54F).copy(alpha = 0.35f),
                                    Color(0xFFFFB300).copy(alpha = 0.05f)
                                )
                            ),
                            shape = RoundedCornerShape(32.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating Star",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "Enjoying FloraFlow? 🌱",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Your feedback helps us grow and create better tools for your garden designs! Would you mind taking a moment to rate the app on the Play Store?",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Actions Layout
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val packageName = context.packageName
                            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                            }
                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                            try {
                                context.startActivity(marketIntent)
                            } catch (e: Exception) {
                                context.startActivity(webIntent)
                            }
                            viewModel.acceptRatePrompt()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("rate_prompt_rate_now_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Rate 5 Stars Now", fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.dismissRatePrompt()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("rate_prompt_later_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Maybe Later", style = MaterialTheme.typography.labelLarge)
                        }

                        TextButton(
                            onClick = {
                                viewModel.declineRatePrompt()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("rate_prompt_never_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            )
                        ) {
                            Text("No, Thanks", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}
