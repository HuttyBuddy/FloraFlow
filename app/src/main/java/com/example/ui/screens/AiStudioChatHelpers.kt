package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.Content
import com.example.ui.components.PlantImages
import com.example.ui.viewmodel.AiReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AttachedImage(
    val name: String,
    val mimeType: String,
    val base64: String
)

suspend fun convertUriToBase64(context: android.content.Context, uri: android.net.Uri): String? {
    return withContext(Dispatchers.IO) {
        try {
            val imageLoader = coil.ImageLoader(context)
            val request = coil.request.ImageRequest.Builder(context)
                .data(uri)
                .size(1024)
                .allowHardware(false)
                .build()
            val result = imageLoader.execute(request)
            if (result is coil.request.SuccessResult) {
                val drawable = result.drawable
                if (drawable is android.graphics.drawable.BitmapDrawable) {
                    val bitmap = drawable.bitmap
                    val outputStream = java.io.ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
                    val compressedBytes = outputStream.toByteArray()
                    outputStream.close()
                    android.util.Base64.encodeToString(compressedBytes, android.util.Base64.NO_WRAP)
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("AiStudioScreen", "Error downloading/decoding image: ${e.message}")
            null
        }
    }
}

fun getUriMimeType(context: android.content.Context, uri: android.net.Uri): String {
    return context.contentResolver.getType(uri) ?: "image/jpeg"
}

@Composable
fun ChatBubbleContent(
    content: Content,
    isUser: Boolean,
    onReportAi: (() -> Unit)? = null
) {
    val text = remember(content) { content.parts.firstOrNull { it.text != null }?.text ?: "" }
    val userImageBitmap = remember(content) {
        val inlinePart = content.parts.firstOrNull { it.inlineData != null }
        inlinePart?.inlineData?.data?.let { base64Str ->
            try {
                val decodedBytes = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (e: Exception) {
                null
            }
        }
    }

    val plantNames = remember {
        listOf(
            "Snake Plant", "Peace Lily", "ZZ Plant", "Cast Iron Plant", "Fiddle Leaf Fig",
            "Swiss Cheese Plant", "Spider Plant", "Jade Plant", "Chinese Money Plant",
            "String of Pearls", "Rubber Plant", "Calathea Ornata", "Parlor Palm",
            "Prayer Plant", "Aloe Vera", "Golden Pothos", "Boston Fern", "Saguaro Cactus",
            "Desert Marigold", "Rosemary", "Prickly Pear Cactus", "Agave Americana",
            "Bird of Paradise", "Monstera Deliciosa", "Red Ginger", "Hibiscus",
            "Plumeria", "Cypress Tree", "Lavender", "English Lavender", "Bougainvillea",
            "Jacaranda Tree", "Mealy Cup Sage", "Olive Tree", "Sweet Fig Tree",
            "California Poppy", "Columbine", "Alpine Aster", "Creeping Thyme",
            "Edelweiss", "Alpine Gentian", "Snowdrop", "Heather", "Fuchsia",
            "English Rose", "Japanese Maple", "English Ivy", "Bonsai Juniper",
            "Hydrangea", "Peonies", "Sunflower", "Marigold", "Snapdragon", "Hostas"
        )
    }

    val detectedPlants = remember(text) {
        if (text.isBlank()) emptyList() else {
            val matches = mutableListOf<String>()
            val sortedNames = plantNames.sortedByDescending { it.length }
            val lowerText = text.lowercase()
            for (name in sortedNames) {
                if (lowerText.contains(name.lowercase()) && !matches.any { it.contains(name, ignoreCase = true) }) {
                    matches.add(name)
                }
            }
            matches.mapNotNull { name ->
                PlantImages.forPlantName(name)?.let { resId -> name to resId }
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (!isUser) {
            Text(
                text = "JULIAN GREENLEAF · AI GARDEN ADVISOR",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )
            // GenAI policy: answers must be identifiable as AI-generated
            Text(
                text = "AI-generated answer",
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        userImageBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Uploaded plant image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .padding(bottom = 6.dp),
                contentScale = ContentScale.Crop
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isUser) {
                Text("🍃", fontSize = 12.sp)
            }
            Text(
                text = text,
                fontSize = 13.sp,
                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                style = androidx.compose.ui.text.TextStyle(lineHeight = 17.sp),
                modifier = Modifier.weight(1f)
            )
        }

        if (detectedPlants.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                detectedPlants.take(3).forEach { (name, resId) ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .width(85.dp)
                            .height(105.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize().padding(4.dp)
                        ) {
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = name,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = name,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                textAlign = TextAlign.Center,
                                lineHeight = 11.sp,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // GenAI policy: every AI answer must have an in-app report/flag control
        if (!isUser && onReportAi != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onReportAi,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Flag,
                        contentDescription = "Report this AI answer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

/**
 * GenAI policy: confirmation shown after an AI answer is reported. Gives the
 * user an anonymous reference ID and a one-tap route to email it to support
 * for human review. No message content is ever attached automatically.
 */
@Composable
fun AiReportConfirmationDialog(
    report: AiReport,
    onEmailSupport: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report recorded", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Thanks — your report was recorded anonymously.",
                    fontSize = 13.sp
                )
                Text(
                    "Reference ID: ${report.id}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Want a person to review it? Email the reference ID to our support team — nothing else is sent unless you add it.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onEmailSupport) { Text("Email support") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}
/**
 * GenAI policy: prominent disclosure shown before the first AI prompt or photo
 * is transmitted off-device. The user must acknowledge it to continue.
 */
@Composable
fun AiDisclosureDialog(
    onAcknowledge: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Before you chat with the AI", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Your messages and plant photos are sent to Google's Gemini AI for processing — they leave your device.",
                    fontSize = 13.sp
                )
                Text(
                    "Answers are AI-generated and can be wrong. Double-check plant care advice before acting on it.",
                    fontSize = 13.sp
                )
                Text(
                    "This is general gardening information, not professional or medical advice.",
                    fontSize = 13.sp
                )
                Text(
                    "You can report any answer with the flag button underneath it.",
                    fontSize = 13.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAcknowledge) { Text("I understand") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now") }
        }
    )
}

/**
 * GenAI policy: in-app flow for flagging/reporting an AI-generated answer.
 * Only the chosen reason category is recorded — never the message content.
 */
@Composable
fun ReportAiContentDialog(
    onSubmit: (reason: String) -> Unit,
    onDismiss: () -> Unit
) {
    val reasons = listOf(
        "Offensive or inappropriate",
        "Wrong or misleading information",
        "Gives medical or health advice",
        "Something else"
    )
    var selected by remember { mutableStateOf(reasons[1]) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report this AI answer", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "What's wrong with this answer? Reports are anonymous.",
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                reasons.forEach { reason ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = reason }
                            .padding(vertical = 5.dp)
                    ) {
                        RadioButton(
                            selected = selected == reason,
                            onClick = { selected = reason }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(reason, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(selected) }) { Text("Send report") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
