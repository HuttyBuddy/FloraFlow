package com.example.ui.screens

import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.ui.viewmodel.GardenViewModel
import com.example.ui.components.FloraFlowButton
import com.example.ui.components.FloraFlowCard
import com.example.ui.components.ButtonVariant
import com.example.ui.theme.extendedColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingDialog(
    viewModel: GardenViewModel,
    modifier: Modifier = Modifier
) {
    val showDialog by viewModel.showBillingDialog.collectAsStateWithLifecycle()
    if (!showDialog) return

    val tiers = listOf(
        BillingPlan(
            name = "FloraFlow PRO Monthly",
            price = "$4.99",
            period = "month",
            trial = "7-Day Free Trial",
            description = "Try premium features: full Neural Restoration Journal access, synthesized brainwave soundscapes, and 3 free Gemini AI expert queries.",
            isPopular = false,
            isAnnual = false
        ),
        BillingPlan(
            name = "FloraFlow PRO Annual",
            price = "$39.99",
            period = "year",
            trial = "14-Day Free Trial",
            description = "Save over 33%! Unlocks unlimited Gemini-Powered AI advice, full Eco-Acoustic journal chimes, stress metric logs, and advanced layouts.",
            isPopular = true,
            isAnnual = true
        )
    )

    val billingManager = viewModel.billingManager
    val context = LocalContext.current
    val activity = context as? Activity
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()

    // Step 2/3 (mock Google Play sheet + loader) only exist to simulate a
    // purchase in debug builds without real Play products configured. In
    // release, launchPurchaseFlow hands off to Play's own purchase UI, and
    // we simply wait for isPremium to flip via onPurchasesUpdated.
    var currentStep by remember { mutableIntStateOf(1) } // 1: Select Plan, 2: Mock Sheet (debug only), 3: Mock Loading (debug only), 4: Success Receipt
    var selectedPlanIndex by remember { mutableIntStateOf(1) } // Default to Annual Plan
    var progressStatusText by remember { mutableStateOf("Connecting with Google Play Billing Client...") }
    var purchaseErrorMessage by remember { mutableStateOf<String?>(null) }
    var isLaunchingRealFlow by remember { mutableStateOf(false) }

    // Real, Play-verified price + trial info for the paywall — queried once
    // per dialog open so the CTA never advertises a trial Play doesn't have.
    var monthlyOffer by remember { mutableStateOf<com.example.billing.BillingManager.OfferInfo?>(null) }
    var annualOffer by remember { mutableStateOf<com.example.billing.BillingManager.OfferInfo?>(null) }
    LaunchedEffect(Unit) {
        billingManager.queryOfferDetails(
            listOf(com.example.billing.BillingManager.PRODUCT_MONTHLY, com.example.billing.BillingManager.PRODUCT_YEARLY)
        ) { offers ->
            monthlyOffer = offers[com.example.billing.BillingManager.PRODUCT_MONTHLY]
            annualOffer = offers[com.example.billing.BillingManager.PRODUCT_YEARLY]
        }
    }

    val activePlan = tiers[selectedPlanIndex]

    fun startUpgrade(isAnnual: Boolean) {
        selectedPlanIndex = if (isAnnual) 1 else 0
        purchaseErrorMessage = null
        if (BuildConfig.DEBUG && billingManager.inMockMode) {
            currentStep = 2
            return
        }
        val act = activity
        if (act == null) {
            purchaseErrorMessage = "Couldn't start checkout. Please try again from the app."
            return
        }
        val productId = if (isAnnual) com.example.billing.BillingManager.PRODUCT_YEARLY else com.example.billing.BillingManager.PRODUCT_MONTHLY
        isLaunchingRealFlow = true
        billingManager.launchPurchaseFlow(
            activity = act,
            productId = productId,
            onMockTrigger = { currentStep = 2 },
            onError = { message ->
                isLaunchingRealFlow = false
                purchaseErrorMessage = message
            }
        )
        // Play's own purchase sheet takes over from here; isLaunchingRealFlow
        // is cleared once onPurchasesUpdated resolves (success or error).
    }

    // Listen to premium activation to automatically jump to success step
    // (this is how a REAL purchase completes: Play calls back into
    // BillingManager, isPremium flips, and we land on the receipt.)
    LaunchedEffect(isPremium) {
        if (isPremium && currentStep < 4) {
            isLaunchingRealFlow = false
            currentStep = 4
        }
    }

    // Coroutine loader triggered on Step 3 — debug-only mock purchase simulation.
    if (currentStep == 3 && BuildConfig.DEBUG) {
        LaunchedEffect(Unit) {
            progressStatusText = "Connecting with Google Play Billing Client..."
            delay(1000)
            progressStatusText = "Generating secure transaction token..."
            delay(900)
            progressStatusText = "Frictionless checkout processing with Google Play..."
            delay(1100)
            progressStatusText = "Updating system database with tier entitlements..."
            delay(800)

            // Finish purchase action
            viewModel.processPurchase(
                tier = activePlan.name,
                price = activePlan.price,
                isAnnual = activePlan.isAnnual
            )
            currentStep = 4
        }
    }

    Dialog(
        onDismissRequest = {
            if (currentStep != 3) {
                viewModel.setBillingDialogVisible(false)
                currentStep = 1
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = currentStep != 3,
            dismissOnClickOutside = currentStep != 3,
            usePlatformDefaultWidth = false
        )
    ) {
        if (currentStep == 1) {
            Column {
                PremiumUpsellScreen(
                    onCloseClick = { viewModel.setBillingDialogVisible(false) },
                    onUpgradeClick = { isAnnual -> startUpgrade(isAnnual) },
                    onRestoreClick = {
                        viewModel.restorePurchases()
                    },
                    monthlyOffer = monthlyOffer,
                    annualOffer = annualOffer
                )
                if (purchaseErrorMessage != null) {
                    LaunchedEffect(purchaseErrorMessage) {
                        android.widget.Toast.makeText(context, purchaseErrorMessage, android.widget.Toast.LENGTH_LONG).show()
                        purchaseErrorMessage = null
                    }
                }
            }
        } else if (currentStep in 2..3 && !BuildConfig.DEBUG) {
            // Should be unreachable in release (mock steps are debug-only),
            // but never render a fake receipt if somehow reached.
            LaunchedEffect(Unit) {
                currentStep = 1
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(enabled = currentStep != 3) {
                        viewModel.setBillingDialogVisible(false)
                        currentStep = 1
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                // Elegant slide-up card layout
                FloraFlowCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .clickable(enabled = false) {} // Consume drag click
                        .testTag("billing_flow_container"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    elevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        // Title Bar / Close control
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (currentStep == 4) "Purchase Successful! ✨" else "Secure Upgrade",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (currentStep == 2) "Google Play Billing" else "Step $currentStep of ${if (billingManager.inMockMode) 4 else 3}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            if (currentStep != 3) {
                                IconButton(
                                    onClick = {
                                        viewModel.setBillingDialogVisible(false)
                                        currentStep = 1
                                    },
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                                        .size(36.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Checkout", modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        // Wizard Step Containers
                        Box(modifier = Modifier.weight(1f)) {
                            when (currentStep) {
                                2 -> GooglePlayMockSheet(
                                    activePlan = activePlan
                                )
                                3 -> BankHandshakeLoadingStep(
                                    statusText = progressStatusText,
                                    activePlan = activePlan
                                )
                                4 -> SuccessReceiptStep(
                                    viewModel = viewModel,
                                    activePlan = activePlan,
                                    onDismiss = {
                                        viewModel.setBillingDialogVisible(false)
                                        currentStep = 1
                                    }
                                )
                            }
                        }

                        // Navigation footer for the debug-only mock sheet (step 2).
                        // Real purchases hand off to Play's own UI and resolve via
                        // isPremium, so this footer is unreachable in release builds.
                        if (currentStep == 2) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                FloraFlowButton(
                                    text = "Back",
                                    onClick = { currentStep = 1 },
                                    modifier = Modifier.weight(1f),
                                    variant = ButtonVariant.Outlined
                                )

                                FloraFlowButton(
                                    text = "Pay Now (${activePlan.price})",
                                    onClick = { currentStep = 3 },
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .testTag("billing_next_button")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Plan specifications structure
data class BillingPlan(
    val name: String,
    val price: String,
    val period: String,
    val trial: String,
    val description: String,
    val isPopular: Boolean,
    val isAnnual: Boolean
)

// STEP 1: Plan Selector Lists Custom Layout
@Composable
fun PlanSelectionStep(
    plans: List<BillingPlan>,
    selectedIndex: Int,
    onIndexChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Select Your Premium Plan Tier",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        plans.forEachIndexed { index, plan ->
            val isSelected = selectedIndex == index
            val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            val backgroundGradient = if (isSelected) {
                Brush.linearGradient(
                    listOf(
                        Color(0xFFE8F5E9).copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            } else {
                Brush.linearGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (plan.isPopular) 8.dp else 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            brush = if (isSelected) {
                                Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.primary, Color(0xFFFFD54F))
                                )
                            } else {
                                SolidColor(borderColor)
                            },
                            shape = RoundedCornerShape(16.dp)
                        )
                        .background(backgroundGradient)
                        .clickable { onIndexChange(index) }
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onIndexChange(index) }
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = plan.name,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = plan.trial,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = plan.price,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "/ ${plan.period}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = plan.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(start = 32.dp)
                        )
                    }
                }

                if (plan.isPopular) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 24.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF2E7D32), Color(0xFF4CAF50))
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "POPULAR",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

// STEP 2: Google Play bottom sheet mockup (used in mock mode)
@Composable
fun GooglePlayMockSheet(
    activePlan: BillingPlan
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Play Store Header Mockup
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Google Play",
                tint = Color(0xFF34A853),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Google Play",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Purchase Item Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activePlan.name,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "FloraFlow • Billed by Google Play",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${activePlan.price} / ${activePlan.period}",
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Details list
        FloraFlowCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            elevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "• Subscription starts immediately.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "• Free Trial period: ${activePlan.trial}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "• Billed automatically. Cancel anytime in Subscriptions on Google Play.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Payment Method Mockup
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = "Wallet",
                tint = Color(0xFF34A853),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Google Play Balance",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "kevin@gmail.com",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "$50.00",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// STEP 3: Bank tunnel loader progress wheel!
@Composable
fun BankHandshakeLoadingStep(
    statusText: String,
    activePlan: BillingPlan
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Rotating loader image or customized canvas
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(72.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 6.dp
            )
            Icon(Icons.Default.Spa, contentDescription = "Active Processing", tint = Color(0xFF6A994E), modifier = Modifier.size(32.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Executing Google Play Billing Flow",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = statusText,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 32.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Total amount due today: ${activePlan.price} after trial period logic completes.",
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// STEP 4: Success Receipt Generation Layout
@Composable
fun SuccessReceiptStep(
    viewModel: GardenViewModel,
    activePlan: BillingPlan,
    onDismiss: () -> Unit
) {
    val transactionId by viewModel.subscriptionTransactionId.collectAsStateWithLifecycle()
    val billingDate by viewModel.subscriptionBillingDate.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Color(0xFFE8F5E9), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = "Paid", tint = Color(0xFF2E7D32), modifier = Modifier.size(36.dp))
        }

        Text(
            text = "Payment Entitlements Verified!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.extendedColors.success
        )

        Text(
            text = "Congratulations! FloraFlow PRO state is unlocked and persistent on your Google Play account. You now have unrestricted access to premium models and visualizers.",
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // Custom receipt paper card style
        FloraFlowCard(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            elevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "GOOGLE PLAY SUBSCRIPTION RECEIPT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                ReceiptItemRow(label = "Subscribed Plan Tier", value = activePlan.name)
                ReceiptItemRow(label = "Transaction Order ID", value = transactionId ?: "GPA.DEMO-RESTORED")
                ReceiptItemRow(label = "Trial Period Entitlement", value = activePlan.trial)
                ReceiptItemRow(label = "Payment Authorized price", value = "${activePlan.price} / ${activePlan.period}")
                ReceiptItemRow(label = "Automatic Renewal Date", value = billingDate ?: "Next Month")
                ReceiptItemRow(label = "Billing Status", value = "ACTIVE")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        FloraFlowButton(
            text = "Launch My Premium Garden Tools 🚀",
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("billing_dismiss_button")
        )
    }
}

@Composable
fun ReceiptItemRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
