package com.example.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalDialog(
    showPrivacy: Boolean,
    showTerms: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!showPrivacy && !showTerms) return

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("legal_dialog"),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
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
                            imageVector = if (showPrivacy) Icons.Default.Lock else Icons.Default.Gavel,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (showPrivacy) "Privacy Policy" else "Terms of Service",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (showPrivacy) {
                        PrivacyPolicyContent()
                    } else {
                        TermsOfServiceContent()
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("I Understand", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PrivacyPolicyContent() {
    Text(
        text = "Last Updated: July 2026",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )

    SectionHeader("1. Information Collection")
    ParagraphText("HuttyBuddy operates the FloraFlow application. Your custom garden designs, companion plant layouts, greenhouse cultivars, and therapeutic gardening logs are stored locally on your device in an encrypted SQLite database. We do not have access to or transmit this data to our servers.")

    SectionHeader("2. Generative AI Consultations")
    ParagraphText("FloraFlow utilizes the Google Gemini API to provide real-time botanical consultations, companion matching suggestions, and seasonal care answers. When you prompt the AI Advisor:")
    BulletPoint("Your text prompts are sent securely to the Google Gemini API to compile answers.")
    BulletPoint("No personal identification data is attached to these requests.")
    BulletPoint("Your API interactions are governed by Google's standard Generative AI Privacy Terms.")

    SectionHeader("3. Third-Party Services")
    ParagraphText("We employ Google Play Services to manage app distribution, licensing verification, and subscription status. We may also link to external botanical reference websites or use Google Gemini API for natural language queries.")

    SectionHeader("4. Data Security")
    ParagraphText("Since your data remains in secure sandboxed local storage, its safety relies on Android system permissions. We recommend keeping your device updated and backed up via Google Drive device backup.")

    SectionHeader("5. Children's Privacy")
    ParagraphText("Our Service does not address anyone under the age of 13. We do not knowingly collect personally identifiable information from anyone.")
}

@Composable
fun TermsOfServiceContent() {
    Text(
        text = "Last Updated: July 2026",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )

    SectionHeader("1. Acceptance of Terms")
    ParagraphText("By downloading or using FloraFlow: Indoor Sanctuaries, you agree to be bound by these Terms of Service. If you do not agree, please do not use the application.")

    SectionHeader("2. Use License")
    ParagraphText("We grant you a personal, non-transferable, non-exclusive license to use the app for personal, non-commercial gardening, landscaping, and mindfulness tracking. You must not decompile, reverse engineer, or redistribute app binaries.")

    SectionHeader("3. Generative AI & Consultations")
    ParagraphText("The AI Advisor (Dr. Greenleaf) is a botanical assistant powered by large language models. The information is provided for general guidance, landscaping ideas, and mindfulness tracking. It does NOT constitute certified agricultural or structural engineering advice.")

    SectionHeader("4. Subscriptions and Payments")
    ParagraphText("Certain features are locked behind a FloraFlow PRO subscription tier. Subscriptions are billed through Google Play billing services and are subject to Google Play store terms and refund policies.")

    SectionHeader("5. Local Storage and Backup")
    ParagraphText("All garden designs and mood ratings are stored locally on your device. We are not liable for any data loss resulting from hardware failure, app uninstallation, or factory reset.")

    SectionHeader("6. Disclaimer of Warranties")
    ParagraphText("FloraFlow is provided 'as is' without warranties of any kind. We do not guarantee that AI advice will always be accurate or that specific plants will thrive under every condition.")
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
fun ParagraphText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = 20.sp
    )
}

@Composable
fun BulletPoint(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("•", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}
