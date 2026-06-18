package com.example.ui.screens

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
import com.example.ui.viewmodel.GardenViewModel
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
            description = "Get full AR Lens access, unlimited AI advice, and standard botany design templates.",
            isPopular = false,
            isAnnual = false
        ),
        BillingPlan(
            name = "FloraFlow PRO Annual",
            price = "$39.99",
            period = "year",
            trial = "14-Day Free Trial",
            description = "Best value! Save over 33% on elite catalog design parameters and dynamic 2D growth modeling.",
            isPopular = true,
            isAnnual = true
        ),
        BillingPlan(
            name = "FloraFlow Elite Suite",
            price = "$14.99",
            period = "month",
            trial = "Priority Access",
            description = "Ultimate package for professional landscaping: continuous high-compute model upgrades, PDF layout exports, and companion team garden synchronizations.",
            isPopular = false,
            isAnnual = false
        )
    )

    var currentStep by remember { mutableStateOf(1) } // 1: Select Plan, 2: Payment Method, 3: Card Entry, 4: Loading Banking, 5: Receipt Success
    var selectedPlanIndex by remember { mutableStateOf(1) } // Default to Annual Plan
    var selectedPaymentMethod by remember { mutableStateOf("GooglePlay") } // "GooglePlay", "CreditCard", "GooglePay"

    // Card details input values
    var cardNumber by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvc by remember { mutableStateOf("") }
    var cardName by remember { mutableStateOf("") }

    // Validation failures
    var validationError by remember { mutableStateOf<String?>(null) }

    // Loading pipeline texts state
    var progressStatusText by remember { mutableStateOf("Contacting transaction host...") }

    val activePlan = tiers[selectedPlanIndex]

    // Key stroke parsing helpers to auto-format inputs nicely like a top-tier fintech app
    val formattedCardNumber = remember(cardNumber) {
        val digits = cardNumber.filter { it.isDigit() }.take(16)
        digits.chunked(4).joinToString(" ")
    }

    val formattedExpiry = remember(cardExpiry) {
        val digits = cardExpiry.filter { it.isDigit() }.take(4)
        if (digits.length >= 3) {
            "${digits.substring(0, 2)}/${digits.substring(2)}"
        } else {
            digits
        }
    }

    val formattedCvc = remember(cardCvc) {
        cardCvc.filter { it.isDigit() }.take(4)
    }

    // Coroutine loader triggered on Step 4 (Processing payment)
    if (currentStep == 4) {
        LaunchedEffect(Unit) {
            progressStatusText = "Connecting with Google Play Billing Client..."
            delay(1000)
            progressStatusText = "Generating secure one-time token certificate..."
            delay(900)
            if (selectedPaymentMethod == "CreditCard") {
                progressStatusText = "Authorizing credit gateway transaction via SSL..."
            } else {
                progressStatusText = "Frictionless checkout processing with Google Pay..."
            }
            delay(1100)
            progressStatusText = "Updating system database with tier entitlements..."
            delay(800)
            
            // Finish purchase action
            viewModel.processPurchase(
                tier = activePlan.name,
                price = activePlan.price,
                isAnnual = activePlan.isAnnual
            )
            currentStep = 5
        }
    }

    Dialog(
        onDismissRequest = {
            if (currentStep != 4) {
                viewModel.setBillingDialogVisible(false)
                currentStep = 1
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = currentStep != 4,
            dismissOnClickOutside = currentStep != 4,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(enabled = currentStep != 4) {
                    viewModel.setBillingDialogVisible(false)
                    currentStep = 1
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            // Elegant slide-up card layout
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .clickable(enabled = false) {} // Consume drag click
                    .testTag("billing_flow_container"),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                text = if (currentStep == 5) "Purchase Successful! ✨" else "Secure Upgrade",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Step $currentStep of 5 • Sandbox Test Mode",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        if (currentStep != 4) {
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
                            1 -> PlanSelectionStep(
                                plans = tiers,
                                selectedIndex = selectedPlanIndex,
                                onIndexChange = { selectedPlanIndex = it }
                            )
                            2 -> PaymentMethodStep(
                                activePlan = activePlan,
                                selectedMethod = selectedPaymentMethod,
                                onMethodChange = { selectedPaymentMethod = it }
                            )
                            3 -> CardEntryStep(
                                activePlan = activePlan,
                                cardNumber = formattedCardNumber,
                                cardExpiry = formattedExpiry,
                                cardCvc = formattedCvc,
                                cardName = cardName,
                                onCardNumberChange = { cardNumber = it },
                                onCardExpiryChange = { cardExpiry = it },
                                onCardCvcChange = { cardCvc = it },
                                onCardNameChange = { cardName = it },
                                validationError = validationError
                            )
                            4 -> BankHandshakeLoadingStep(
                                statusText = progressStatusText,
                                activePlan = activePlan
                            )
                            5 -> SuccessReceiptStep(
                                viewModel = viewModel,
                                activePlan = activePlan,
                                onDismiss = {
                                    viewModel.setBillingDialogVisible(false)
                                    currentStep = 1
                                }
                            )
                        }
                    }

                    // Navigation Footer buttons (excluding Loading state 4 and Receipt page 5)
                    if (currentStep in 1..3) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (currentStep > 1) {
                                OutlinedButton(
                                    onClick = {
                                        validationError = null
                                        currentStep--
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Back", fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = {
                                    if (currentStep == 1) {
                                        currentStep = 2
                                    } else if (currentStep == 2) {
                                        if (selectedPaymentMethod == "CreditCard") {
                                            currentStep = 3
                                        } else {
                                            // Directly skip card layout and check out using Google Fast Pay balance
                                            currentStep = 4
                                        }
                                    } else if (currentStep == 3) {
                                        // Validate mock card input fields
                                        val digitsOnly = cardNumber.filter { it.isDigit() }
                                        if (digitsOnly.length < 16) {
                                            validationError = "Please enter a valid 16-digit card number"
                                        } else if (cardExpiry.filter { it.isDigit() }.length < 4) {
                                            validationError = "Please enter expiry MM/YY"
                                        } else if (cardCvc.filter { it.isDigit() }.length < 3) {
                                            validationError = "Please enter secure 3-digit CVC"
                                        } else if (cardName.trim().isEmpty()) {
                                            validationError = "Please fill in Cardholder name"
                                        } else {
                                            validationError = null
                                            currentStep = 4
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(if (currentStep > 1) 1.5f else 1f)
                                    .height(52.dp)
                                    .testTag("billing_next_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(
                                    text = when (currentStep) {
                                        1 -> "Select Plan"
                                        2 -> if (selectedPaymentMethod == "CreditCard") "Add Card Details" else "Pay Now (${activePlan.price})"
                                        else -> "Authorize & Subscribe"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            } else {
                Brush.linearGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = if (isSelected) 2.5.dp else 1.dp,
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
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = plan.name,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (plan.isPopular) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFE8F5E9), RoundedCornerShape(6.dp))
                                            .border(1.dp, Color(0xFF4CAF50), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "POPULAR",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = plan.trial,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = plan.price,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "/ ${plan.period}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = plan.description,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(start = 48.dp)
                    )
                }
            }
        }
    }
}

// STEP 2: Payment options selection
@Composable
fun PaymentMethodStep(
    activePlan: BillingPlan,
    selectedMethod: String,
    onMethodChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(16.dp)
        ) {
            Text("Selected Purchase:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(activePlan.name, fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                Text("${activePlan.price} / ${activePlan.period}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Text("Entitlements start immediately following Sandbox certification approval.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }

        Text("Select Preferred Payment Channel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        // Options
        PaymentRowItem(
            id = "GooglePlay",
            title = "Google Play Balance (Sandbox Mock Account)",
            subtitle = "Sufficient Funds • Dynamic Trial Authorized",
            icon = Icons.Default.AccountBalanceWallet,
            iconColor = Color(0xFF34A853),
            isSelected = selectedMethod == "GooglePlay",
            onClick = onMethodChange
        )

        PaymentRowItem(
            id = "CreditCard",
            title = "Add Credit or Debit Card",
            subtitle = "Pays via encrypted checkout pipeline modal",
            icon = Icons.Default.CreditCard,
            iconColor = Color(0xFF1A73E8),
            isSelected = selectedMethod == "CreditCard",
            onClick = onMethodChange
        )

        PaymentRowItem(
            id = "GooglePay",
            title = "Google Pay Fast-Wallet Connection",
            subtitle = "Tap to charge dynamic token profile securely",
            icon = Icons.Default.Payment,
            iconColor = Color(0xFFEA4335),
            isSelected = selectedMethod == "GooglePay",
            onClick = onMethodChange
        )
    }
}

@Composable
fun PaymentRowItem(
    id: String,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    isSelected: Boolean,
    onClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(14.dp)
            )
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
            .clickable { onClick(id) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconColor.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        RadioButton(selected = isSelected, onClick = { onClick(id) })
    }
}

// STEP 3: Beautiful simulated Credit card entry!
@Composable
fun CardEntryStep(
    activePlan: BillingPlan,
    cardNumber: String,
    cardExpiry: String,
    cardCvc: String,
    cardName: String,
    onCardNumberChange: (String) -> Unit,
    onCardExpiryChange: (String) -> Unit,
    onCardCvcChange: (String) -> Unit,
    onCardNameChange: (String) -> Unit,
    validationError: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Visual premium credit card simulation component
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF1E261C), // Rich moss
                            Color(0xFF2E6F40), // Sprout botanical emerald
                            Color(0xFF4C8D5E)  // Shimmer leaf
                        )
                    )
                )
                .border(1.dp, Color(0xFFFFD54F).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            // Shiny visual gold microchip representation
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp, 26.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFFD54F).copy(alpha = 0.8f)) // Gold chips
                    )
                    Text("FLORAFLOW VIP", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                }

                Text(
                    text = if (cardNumber.isEmpty()) "•••• •••• •••• ••••" else cardNumber,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.5.sp
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("CARD HOLDER", fontSize = 7.sp, color = Color.White.copy(alpha = 0.6f))
                        Text(
                            text = if (cardName.isEmpty()) "YOUR NAME" else cardName.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("EXPIRES", fontSize = 7.sp, color = Color.White.copy(alpha = 0.6f))
                        Text(
                            text = if (cardExpiry.isEmpty()) "MM/YY" else cardExpiry,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Text("Billing Method Verification Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

        // Error message if fields invalid
        if (validationError != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFEBEE))
                    .border(1.dp, Color(0xFFEF5350), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, contentDescription = "Error", tint = Color(0xFFD32F2F), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(validationError, fontSize = 11.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                }
            }
        }

        // Row of text inputs
        OutlinedTextField(
            value = cardName,
            onValueChange = onCardNameChange,
            label = { Text("Cardholder Name") },
            placeholder = { Text("e.g. Jane Forester") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        OutlinedTextField(
            value = cardNumber,
            onValueChange = onCardNumberChange,
            label = { Text("Card Number") },
            placeholder = { Text("1234 5678 1234 5678") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = cardExpiry,
                onValueChange = onCardExpiryChange,
                label = { Text("Expiry (MM/YY)") },
                placeholder = { Text("12/29") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
                value = cardCvc,
                onValueChange = onCardCvcChange,
                label = { Text("CVC Code") },
                placeholder = { Text("381") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

// STEP 4: Bank tunnel loader progress wheel!
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
            text = "Executing Sandbox Subscription Flow",
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

// STEP 5: Success Receipt Generation Layout
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
            color = Color(0xFF2E7D32)
        )

        Text(
            text = "Congratulations! FloraFlow PRO state is unlocked and persistent on this device. You now have unrestricted access to premium models and visualizers.",
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // Custom receipt paper card style
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "GOOGLE PLAY BILLING SANDBOX RECEIPT",
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
                ReceiptItemRow(label = "Status State Key", value = "ENTITLED_SANDBOX_STABLE")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("billing_dismiss_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Launch My Premium Garden Tools 🚀", fontWeight = FontWeight.Bold)
        }
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
