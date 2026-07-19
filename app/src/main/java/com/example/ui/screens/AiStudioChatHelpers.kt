package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    isUser: Boolean
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
                text = "DR. JULIAN GREENLEAF",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
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
    }
}
