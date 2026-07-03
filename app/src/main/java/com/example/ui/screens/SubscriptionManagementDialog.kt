package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.ui.viewmodel.GardenViewModel
import com.example.ui.components.FloraFlowButton
import com.example.ui.components.FloraFlowCard
import com.example.ui.components.ButtonVariant

@Composable
fun SubscriptionManagementDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    viewModel: GardenViewModel,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val context = LocalContext.current
    val tier by viewModel.subscriptionTier.collectAsStateWithLifecycle()
    val transactionId by viewModel.subscriptionTransactionId.collectAsStateWithLifecycle()
    val billingDate by viewModel.subscriptionBillingDate.collectAsStateWithLifecycle()

    var showCancelConfirmation by remember { mutableStateOf(false) }

    if (showCancelConfirmation) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmation = false },
            title = {
                Text(
                    "Cancel Subscription?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    if (BuildConfig.DEBUG) {
                        "Are you sure you want to cancel your high-speed Master AI insights, exotic species records, and interactive Restoration Journal features? Your custom garden designs will remain saved."
                    } else {
                        "Subscriptions are managed by Google Play. You'll be taken to your Play Store subscriptions page to cancel — your premium access continues until the end of the current billing period."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelConfirmation = false
                        if (BuildConfig.DEBUG) {
                            viewModel.cancelPremiumSubscription()
                            onDismiss()
                            android.widget.Toast.makeText(context, "Subscription Downgraded to Free Tier.", android.widget.Toast.LENGTH_LONG).show()
                        } else {
                            val uri = viewModel.billingManager.buildManageSubscriptionUri()
                            try {
                                context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Couldn't open Google Play. Please open the Play Store app to manage your subscription.", android.widget.Toast.LENGTH_LONG).show()
                            }
                            onDismiss()
                        }
                    }
                ) {
                    Text(
                        if (BuildConfig.DEBUG) "Yes, Downgrade Plan" else "Manage on Google Play",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton( onClick = { showCancelConfirmation = false } ) {
                    Text("Keep Premium", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        FloraFlowCard(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("subscription_management_container"),
            containerColor = MaterialTheme.colorScheme.surface,
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header section with Crown
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color(0xFFFFF8E1), CircleShape)
                        .border(2.dp, Color(0xFFFFB300), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.WorkspacePremium,
                        contentDescription = "Active Pro status token",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(34.dp)
                    )
                }

                Text(
                    text = "Subscription Console",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )

                Box(
                    modifier = Modifier
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "ACTIVE (GOOGLE PLAY SUBSCRIPTION)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Detailed plan items list
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SubscriptionDetailRow(label = "Premium Plan Tier", value = tier ?: "FloraFlow PRO Monthly")
                    SubscriptionDetailRow(label = "Order ID Ref", value = transactionId ?: "GPA.DEMO-8791-0312")
                    SubscriptionDetailRow(label = "Renewal Date", value = billingDate ?: "Next billing cycle")
                    SubscriptionDetailRow(label = "Payment Gateway", value = "Google Play Services")
                    SubscriptionDetailRow(label = "Entitlement Key", value = "PRO_VERIFIED_SECURE")
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Action buttons
                FloraFlowButton(
                    text = "Change Plan / Upgrade Tier",
                    onClick = {
                        onDismiss()
                        viewModel.upgradeToPremium()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = Icons.Default.SettingsBackupRestore
                )
 
                FloraFlowButton(
                    text = "Cancel Subscription",
                    onClick = { showCancelConfirmation = true },
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.Outlined,
                    leadingIcon = Icons.Default.Cancel
                )
 
                FloraFlowButton(
                    text = "Close Console",
                    onClick = onDismiss,
                    variant = ButtonVariant.Text,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun SubscriptionDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
