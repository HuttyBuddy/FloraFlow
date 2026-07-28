package com.example.ui.screens.share

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.analytics.ShareAnalytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ShareCardOverlay(
    data: ShareCardData,
    surface: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(data) {
        // Rendering a 1080x1920 bitmap on the main thread drops frames on mid-range devices.
        generatedBitmap = withContext(Dispatchers.Default) {
            ViralShareEngine.generate9by16StoryBitmap(context, data)
        }
    }

    LaunchedEffect(surface) {
        ShareAnalytics.logShareSurfaceViewed(surface)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0C2419))
                    .border(1.dp, Color(0xFF337FE3B5), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✨ Your Archetype Card",
                        color = Color(0xFF7FE3B5),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                // Card Preview
                generatedBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Story Card Preview",
                        modifier = Modifier
                            .height(380.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFF7FE3B5).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    )
                } ?: CircularProgressIndicator(color = Color(0xFF7FE3B5))

                // Action Buttons
                Button(
                    onClick = {
                        ViralShareEngine.shareCard(context, data, surface)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7FE3B5),
                        contentColor = Color(0xFF03140C)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share to TikTok / Instagram Stories",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF7FE3B5).copy(alpha = 0.5f))
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFE0F7ED)
                    )
                ) {
                    Text(
                        text = "Done",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
