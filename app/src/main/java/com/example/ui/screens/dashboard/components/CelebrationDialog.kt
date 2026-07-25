package com.example.ui.screens.dashboard.components
 
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun CelebrationDialog(
    title: String,
    subtitle: String,
    extraText: String,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val colors = listOf(Color(0xFF4CAF50), Color(0xFF81C784), Color(0xFFFFD700), Color(0xFFFFA500), Color(0xFFC8E6C9), Color(0xFFFFF59D))
    
    val particles = remember {
        mutableStateListOf<ConfettiParticle>().apply {
            repeat(80) {
                add(
                    ConfettiParticle(
                        x = (0..1000).random().toFloat(),
                        y = -100f - (0..800).random().toFloat(),
                        vx = (-5..5).random().toFloat(),
                        vy = (5..15).random().toFloat(),
                        color = colors.random(),
                        size = (10..24).random().toFloat()
                    )
                )
            }
        }
    }
    
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(16)
            particles.forEach { p ->
                p.y += p.vy
                p.x += p.vx
                p.vy += 0.08f
                if (p.y > 2200f) {
                    p.y = -100f
                    p.x = (0..1000).random().toFloat()
                    p.vy = (5..15).random().toFloat()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 420.dp)
                .testTag("celebration_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    particles.forEach { p ->
                        val drawX = p.x % size.width
                        drawCircle(
                            color = p.color,
                            radius = p.size,
                            center = Offset(drawX, p.y)
                        )
                    }
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🎉", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("close_celebration_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Dismiss", fontWeight = FontWeight.Bold)
                        }
                        
                        Button(
                            onClick = {
                                shareCelebrationCard(context, "Achievement!", subtitle, extraText)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("share_celebration_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share achievement", tint = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Particle data class
class ConfettiParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    val size: Float
)

fun shareCelebrationCard(
    context: android.content.Context,
    title: String,
    subtitle: String,
    extraText: String
) {
    val size = 1000
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    
    val bgPaint = android.graphics.Paint().apply {
        color = 0xFFFCF9F1.toInt()
        style = android.graphics.Paint.Style.FILL
    }
    canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)
    
    val borderPaint = android.graphics.Paint().apply {
        color = 0xFF1F483E.toInt()
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 16f
    }
    canvas.drawRect(20f, 20f, size.toFloat() - 20f, size.toFloat() - 20f, borderPaint)
    
    val thinBorderPaint = android.graphics.Paint().apply {
        color = 0xFF1F483E.toInt()
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 4f
    }
    canvas.drawRect(32f, 32f, size.toFloat() - 32f, size.toFloat() - 32f, thinBorderPaint)

    val titlePaint = android.graphics.Paint().apply {
        color = 0xFF1F483E.toInt()
        textSize = 54f
        isFakeBoldText = true
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
    }
    canvas.drawText(title, size / 2f, 300f, titlePaint)
    
    val subtitlePaint = android.graphics.Paint().apply {
        color = 0xFF1B1C17.toInt()
        textSize = 36f
        isFakeBoldText = true
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
    }
    
    if (subtitle.length > 35) {
        val middle = subtitle.indexOf(' ', subtitle.length / 2)
        val part1 = if (middle != -1) subtitle.substring(0, middle) else subtitle
        val part2 = if (middle != -1) subtitle.substring(middle + 1) else ""
        canvas.drawText(part1, size / 2f, 450f, subtitlePaint)
        if (part2.isNotEmpty()) {
            canvas.drawText(part2, size / 2f, 500f, subtitlePaint)
        }
    } else {
        canvas.drawText(subtitle, size / 2f, 470f, subtitlePaint)
    }
    
    try {
        val logoSrc = android.graphics.BitmapFactory.decodeResource(context.resources, com.example.R.drawable.ic_logo_heart)
        if (logoSrc != null) {
            val scaledLogo = android.graphics.Bitmap.createScaledBitmap(logoSrc, 120, 120, true)
            canvas.drawBitmap(scaledLogo, size / 2f - 60f, 580f, null)
        }
    } catch (e: Exception) {
        if (com.example.BuildConfig.DEBUG) {
            android.util.Log.e("CelebrationDialog", "Error drawing logo", e)
        }
    }
    
    val watermarkTitlePaint = android.graphics.Paint().apply {
        color = 0xFF1F483E.toInt()
        textSize = 36f
        isFakeBoldText = true
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
    }
    canvas.drawText("FloraFlow", size / 2f, 760f, watermarkTitlePaint)
    
    val watermarkSubPaint = android.graphics.Paint().apply {
        color = 0xFF43493E.toInt()
        textSize = 22f
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
    }
    canvas.drawText("Designed with FloraFlow", size / 2f, 810f, watermarkSubPaint)
    
    try {
        val cachePath = java.io.File(context.cacheDir, "shared_gardens")
        cachePath.mkdirs()
        val file = java.io.File(cachePath, "celebration_snapshot.png")
        val stream = java.io.FileOutputStream(file)
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()
        
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "My FloraFlow Achievement")
            putExtra(android.content.Intent.EXTRA_TEXT, extraText)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Achievement"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Failed to share: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}
