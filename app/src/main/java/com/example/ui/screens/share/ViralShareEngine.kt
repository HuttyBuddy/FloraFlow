package com.example.ui.screens.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

data class ShareCardData(
    val title: String = "FloraFlow AI Vibe Check",
    val vibeTag: String = "Cozy Botanical Sanctuary",
    val score: Int = 88,
    val scoreDelta: Int = 34,
    val topUpgrades: List<String> = listOf(
        "Add Snake Plant for night oxygen boost",
        "Introduce 10Hz binaural ambient flow",
        "Reposition desk 2ft closer to natural daylight"
    )
)

object ViralShareEngine {

    fun generate9by16StoryBitmap(context: Context, data: ShareCardData): Bitmap {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Sleek Emerald & Midnight Gradient Background
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(
                    Color.parseColor("#061A12"),
                    Color.parseColor("#0B2B1D"),
                    Color.parseColor("#030D08")
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Glow Circles in background
        val glowPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#1F5A3E")
            alpha = 70
        }
        canvas.drawCircle(width * 0.8f, height * 0.2f, 350f, glowPaint)
        canvas.drawCircle(width * 0.2f, height * 0.75f, 400f, glowPaint)

        // 3. Header Branding
        val brandPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#7FE3B5")
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.15f
        }
        canvas.drawText("FLORAFLOW: INDOOR SANCTUARIES • VIBE CHECK", 90f, 160f, brandPaint)

        // 4. Main Glassmorphism Card Container
        val cardPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#14FFFFFF") // Translucent white
        }
        val strokePaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#337FE3B5")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val cardRect = RectF(70f, 220f, width - 70f, height - 240f)
        canvas.drawRoundRect(cardRect, 48f, 48f, cardPaint)
        canvas.drawRoundRect(cardRect, 48f, 48f, strokePaint)

        // 5. Vibe Tag Pill
        val tagBgPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#267FE3B5")
        }
        val tagRect = RectF(110f, 270f, width - 110f, 370f)
        canvas.drawRoundRect(tagRect, 50f, 50f, tagBgPaint)

        val tagTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#E0F7ED")
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("✨ ${data.vibeTag}", width / 2f, 335f, tagTextPaint)

        // 6. Score Circle Display
        val circleCenterY = 620f
        val circleRadius = 180f

        val outerArcPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#7FE3B5")
            style = Paint.Style.STROKE
            strokeWidth = 24f
            strokeCap = Paint.Cap.ROUND
        }
        val trackArcPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#22FFFFFF")
            style = Paint.Style.STROKE
            strokeWidth = 24f
        }
        canvas.drawCircle(width / 2f, circleCenterY, circleRadius, trackArcPaint)
        val arcBounds = RectF(
            width / 2f - circleRadius,
            circleCenterY - circleRadius,
            width / 2f + circleRadius,
            circleCenterY + circleRadius
        )
        val sweepAngle = (data.score / 100f) * 360f
        canvas.drawArc(arcBounds, -90f, sweepAngle, false, outerArcPaint)

        // Score Text inside Circle
        val scoreTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 120f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("${data.score}%", width / 2f, circleCenterY + 40f, scoreTextPaint)

        val scoreLabelPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#9AE6C4")
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.1f
        }
        canvas.drawText("BIOPHILIC VITALITY SCORE", width / 2f, 870f, scoreLabelPaint)

        val deltaPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#52E09B")
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("+${data.scoreDelta}% Potential Upgrade Unlocked 🌿", width / 2f, 925f, deltaPaint)

        // 7. Divider Line
        val divPaint = Paint().apply {
            color = Color.parseColor("#22FFFFFF")
            strokeWidth = 2f
        }
        canvas.drawLine(120f, 970f, width - 120f, 970f, divPaint)

        // 8. Top Upgrades Section
        val upgradeTitlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#E0F7ED")
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("🚀 Recommended Sanctuary Upgrades:", 120f, 1040f, upgradeTitlePaint)

        val upgradeItemPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#C3EBD9")
            textSize = 32f
            typeface = Typeface.DEFAULT
        }
        val bulletPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#7FE3B5")
        }

        var currentY = 1120f
        data.topUpgrades.forEachIndexed { idx, item ->
            canvas.drawCircle(140f, currentY - 10f, 10f, bulletPaint)

            if (item.length > 38) {
                val line1 = item.take(38)
                val line2 = item.drop(38)
                canvas.drawText(line1, 175f, currentY, upgradeItemPaint)
                currentY += 45f
                canvas.drawText(line2, 175f, currentY, upgradeItemPaint)
            } else {
                canvas.drawText(item, 175f, currentY, upgradeItemPaint)
            }
            currentY += 75f
        }

        // 9. Bottom Footer Branding
        val footerPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#7FE3B5")
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("FloraFlow: Indoor Sanctuaries 🌿 • Breathe life into your space", width / 2f, height - 120f, footerPaint)

        return bitmap
    }

    fun shareCard(context: Context, data: ShareCardData) {
        try {
            val bitmap = generate9by16StoryBitmap(context, data)
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "floraflow_vibe_check.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "My indoor plant sanctuary score is ${data.score}%! Discover your Plant Parent Archetype with FloraFlow: Indoor Sanctuaries 🌿✨"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Sanctuary Story Card")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
