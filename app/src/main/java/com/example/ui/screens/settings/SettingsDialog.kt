package com.example.ui.screens.settings

import androidx.compose.foundation.background
import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.GardenViewModel
import com.example.ui.viewmodel.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onFeedbackClick: () -> Unit,
    onHelpClick: () -> Unit,
    viewModel: GardenViewModel,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val currentThemeMode by viewModel.themeMode.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var showRateDialog by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("settings_dialog")
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f) // Constrain height to force scrolling if needed
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Theme Mode Selector Header
                Text(
                    text = "Application Theme",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Theme selection layout (Row of 3 cards)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeOptionCard(
                        title = "System",
                        icon = Icons.Default.SettingsSuggest,
                        isSelected = currentThemeMode == ThemeMode.SYSTEM,
                        onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionCard(
                        title = "Light",
                        icon = Icons.Default.LightMode,
                        isSelected = currentThemeMode == ThemeMode.LIGHT,
                        onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionCard(
                        title = "Dark",
                        icon = Icons.Default.DarkMode,
                        isSelected = currentThemeMode == ThemeMode.DARK,
                        onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Settings List Actions
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SettingsActionRow(
                        title = "Rate Your App",
                        subtitle = "Help us grow with a store review",
                        icon = Icons.Default.Star,
                        iconTint = Color(0xFFFFB300),
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
                            viewModel.acceptRatePrompt()
                        }
                    )

                    SettingsActionRow(
                        title = "Share Feedback",
                        subtitle = "Suggest features or report issues directly",
                        icon = Icons.Default.Feedback,
                        onClick = {
                            onFeedbackClick()
                        }
                    )

                    SettingsActionRow(
                        title = "Replay App Tour",
                        subtitle = "Launch the walkthrough overlay again",
                        icon = Icons.Default.AutoAwesome,
                        onClick = {
                            onDismiss()
                            viewModel.startWalkthrough()
                        }
                    )

                    SettingsActionRow(
                        title = "Retake Biophilic Assessment",
                        subtitle = "Recalculate your Neural Load score & tips",
                        icon = Icons.Default.Eco,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = {
                            onDismiss()
                            viewModel.resetAssessment()
                        }
                    )

                    val simulate30Days by viewModel.simulate30Days.collectAsState()
                    SettingsActionRow(
                        title = if (simulate30Days) "Disable 30-Day Simulation" else "Simulate 30 Days Elapsed",
                        subtitle = "Toggle the monthly Neural Load audit reminder",
                        icon = Icons.Default.Timer,
                        iconTint = if (simulate30Days) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        onClick = {
                            viewModel.toggleSimulation30Days()
                        }
                    )

                    SettingsActionRow(
                        title = "Invite a Garden Buddy",
                        subtitle = "Share FloraFlow and unlock 5 exotic species",
                        icon = Icons.Default.Share,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Plant a Seed on FloraFlow")
                                putExtra(Intent.EXTRA_TEXT, "Your friend planted a seed for you on FloraFlow! 🌸 Use this link to download the app and unlock a gift of 5 exotic species to cultivate calm: https://floraflow.app/referral?by=buddy")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Invite a Garden Buddy"))
                        }
                    )

                    SettingsActionRow(
                        title = "Help & FAQs",
                        subtitle = "Browse tutorials and troubleshooting guides",
                        icon = Icons.Default.Help,
                        onClick = {
                            onDismiss()
                            onHelpClick()
                        }
                    )

                    SettingsActionRow(
                        title = "Privacy Policy",
                        subtitle = "Data storage and Gemini AI details",
                        icon = Icons.Default.Lock,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        onClick = { showPrivacy = true }
                    )

                    SettingsActionRow(
                        title = "Terms of Service",
                        subtitle = "Billing terms and app usage licensing",
                        icon = Icons.Default.Gavel,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        onClick = { showTerms = true }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Footer
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "FloraFlow: Cultivating Calm through Mindful Gardening",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "v1.0.0-beta | HuttyBuddy",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }

    // Connect child dialogs
    RateAppDialog(
        visible = showRateDialog,
        onDismiss = { showRateDialog = false },
        viewModel = viewModel
    )

    LegalDialog(
        showPrivacy = showPrivacy,
        showTerms = showTerms,
        onDismiss = {
            showPrivacy = false
            showTerms = false
        }
    )
}

@Composable
fun ThemeOptionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
        ),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsActionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(16.dp)
        )
    }
}
