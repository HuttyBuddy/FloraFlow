package com.example.ui.screens.share

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.analytics.ShareAnalytics
import com.example.data.model.PlantParentArchetype
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ReelsExporterOverlay(
    score: Int = 88,
    archetype: PlantParentArchetype = PlantParentArchetype.JUNGLE_MAXIMALIST,
    frequencyHz: Float = 10.0f,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableFloatStateOf(0f) }
    var generatedFile by remember { mutableStateOf<File?>(null) }
    var exportedWithAudio by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val shareCode = remember { ShareLinks.shareCode(context) }

    LaunchedEffect(Unit) {
        ShareAnalytics.logShareSurfaceViewed(ShareAnalytics.Surface.REELS_EXPORTER)
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
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0C2419))
                    .border(1.dp, Color(0xFF337FE3B5), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color(0xFF7FE3B5),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🎬 Export 15s Ambient Reel",
                            color = Color(0xFF7FE3B5),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                // Subtitle
                Text(
                    text = "Generate a 1080x1920 15-second MP4 with your ${frequencyHz.toInt()}Hz binaural soundscape on the audio track and your ${archetype.title} badge — built for TikTok & Instagram Reels.",
                    fontSize = 14.sp,
                    color = Color(0xFFC3EBD9)
                )

                if (isExporting) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 16.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { exportProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF7FE3B5),
                            trackColor = Color(0xFF143B2B)
                        )
                        Text(
                            text = "Encoding 15s MP4 Reel... ${(exportProgress * 100).toInt()}%",
                            color = Color(0xFF9AE6C4),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else if (generatedFile != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF143B2B),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (exportedWithAudio) {
                                "✅ Your 15s Reel is ready — with sound. Tap below to share to TikTok or Instagram Reels."
                            } else {
                                // Be straight with the user rather than let them post a silent clip unknowingly.
                                "✅ Your 15s Reel is ready, but this device couldn't encode the soundscape — the video is silent. Add music in the app you post to."
                            },
                            color = Color(0xFF7FE3B5),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }

                    Button(
                        onClick = {
                            ReelsVideoExporter.shareReelVideo(
                                context = context,
                                videoFile = generatedFile!!,
                                shareText = "${archetype.icon} ${archetype.title} — my ${frequencyHz.toInt()}Hz " +
                                    "sanctuary soundscape. Headphones on 🌿 ${ShareLinks.shareUrl(shareCode)}"
                            )
                            ShareAnalytics.logShareInitiated(
                                surface = ShareAnalytics.Surface.REELS_EXPORTER,
                                asset = ShareAnalytics.Asset.REEL_VIDEO,
                                archetype = archetype.name,
                                score = score
                            )
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
                        Text(
                            text = "Share Video to TikTok / Reels",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            isExporting = true
                            errorMessage = null
                            scope.launch(Dispatchers.Default) {
                                val startedAt = System.currentTimeMillis()
                                ReelsVideoExporter.generate15SecondReelVideo(
                                    context = context,
                                    score = score,
                                    archetype = archetype,
                                    frequencyHz = frequencyHz,
                                    shareCode = shareCode,
                                    onProgress = { p -> exportProgress = p },
                                    onComplete = { file, hasAudio ->
                                        generatedFile = file
                                        exportedWithAudio = hasAudio
                                        isExporting = false
                                        ShareAnalytics.logReelGenerated(
                                            archetype = archetype.name,
                                            durationMs = System.currentTimeMillis() - startedAt,
                                            hasAudio = hasAudio
                                        )
                                    },
                                    onError = { err ->
                                        errorMessage = err.message
                                        isExporting = false
                                        ShareAnalytics.logReelGenerationFailed(
                                            err.javaClass.simpleName
                                        )
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7FE3B5),
                            contentColor = Color(0xFF03140C)
                        )
                    ) {
                        Text(
                            text = "Render 15s Video Reel",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                errorMessage?.let { err ->
                    Text(
                        text = "Error generating video: $err",
                        color = Color(0xFFFF8A80),
                        fontSize = 12.sp
                    )
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFE0F7ED)
                    )
                ) {
                    Text("Close")
                }
            }
        }
    }
}
