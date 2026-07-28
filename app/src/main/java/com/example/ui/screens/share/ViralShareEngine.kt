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
import android.util.Log
import androidx.core.content.FileProvider
import com.example.analytics.ShareAnalytics
import com.example.data.model.PlantParentArchetype
import java.io.File
import java.io.FileOutputStream

/**
 * Everything shown on a shareable story card.
 *
 * The archetype leads: it is the line people actually repeat when they post ("I'm a
 * Cyberpunk Botanist"), whereas a percentage is a statistic nobody screenshots. The score
 * stays on the card as supporting evidence.
 */
data class ShareCardData(
    val archetype: PlantParentArchetype = PlantParentArchetype.JUNGLE_MAXIMALIST,
    val vibeTag: String = "Cozy Botanical Sanctuary",
    val score: Int = 88,
    val topUpgrades: List<String> = listOf(
        "Add Snake Plant for night oxygen boost",
        "Introduce 10Hz binaural ambient flow",
        "Reposition desk 2ft closer to natural daylight"
    ),
    /** This install's referral code, burned into the QR and the printed link. */
    val shareCode: String? = null
)

/**
 * Renders the single 1080x1920 story card used by every share surface.
 *
 * There used to be two renderers — a 512px square on the onboarding result path and this
 * one on Vibe Check. The square looked pixelated in a Story and sat on the higher-traffic
 * surface, so both paths now come here.
 */
object ViralShareEngine {

    private const val TAG = "ViralShareEngine"

    private const val WIDTH = 1080
    private const val HEIGHT = 1920

    // Fixed vertical layout. Every block has a reserved band so a long archetype
    // description or upgrade line can never push the QR footer off the canvas.
    private const val CARD_TOP = 210f
    private const val CARD_BOTTOM = 1860f
    private const val HALO_CENTER_Y = 500f
    private const val ICON_BASELINE = 560f
    private const val EYEBROW_Y = 670f
    private const val TITLE_Y = 765f
    private const val DESC_TOP = 825f
    private const val DESC_LINE_HEIGHT = 48f
    private const val DESC_MAX_LINES = 2
    private const val RING_CENTER_Y = 1010f
    private const val RING_LABEL_Y = 1165f
    private const val PILL_TOP = 1215f
    private const val PILL_BOTTOM = 1307f
    private const val PILL_TEXT_Y = 1275f
    private const val UPGRADES_TITLE_Y = 1375f
    private const val UPGRADES_TOP = 1430f
    private const val UPGRADES_LINE_HEIGHT = 55f
    private const val FOOTER_TOP = 1660f

    // Palette
    private const val MINT = "#7FE3B5"
    private const val MINT_SOFT = "#9AE6C4"
    private const val PARCHMENT = "#E0F7ED"
    private const val SAGE = "#C3EBD9"

    fun generate9by16StoryBitmap(context: Context, data: ShareCardData): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawBackground(canvas)
        drawHeader(canvas)

        drawCardContainer(canvas, RectF(60f, CARD_TOP, WIDTH - 60f, CARD_BOTTOM))

        drawArchetypeHero(canvas, data.archetype)
        drawScoreRing(canvas, data.score)
        drawVibeTag(canvas, data.vibeTag)
        drawUpgrades(canvas, data.topUpgrades)
        drawFooter(canvas, data.shareCode)

        return bitmap
    }

    // ------------------------------------------------------------------
    // Card sections
    // ------------------------------------------------------------------

    private fun drawBackground(canvas: Canvas) {
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, HEIGHT.toFloat(),
                intArrayOf(
                    Color.parseColor("#061A12"),
                    Color.parseColor("#0B2B1D"),
                    Color.parseColor("#030D08")
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), bgPaint)

        val glowPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#1F5A3E")
            alpha = 70
        }
        canvas.drawCircle(WIDTH * 0.8f, HEIGHT * 0.2f, 350f, glowPaint)
        canvas.drawCircle(WIDTH * 0.2f, HEIGHT * 0.75f, 400f, glowPaint)
    }

    private fun drawHeader(canvas: Canvas) {
        val brandPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor(MINT)
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.15f
        }
        canvas.drawText("FLORAFLOW: INDOOR SANCTUARIES", 80f, 150f, brandPaint)
    }

    private fun drawCardContainer(canvas: Canvas, rect: RectF) {
        val cardPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#14FFFFFF")
        }
        val strokePaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#337FE3B5")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(rect, 48f, 48f, cardPaint)
        canvas.drawRoundRect(rect, 48f, 48f, strokePaint)
    }

    private fun drawArchetypeHero(canvas: Canvas, archetype: PlantParentArchetype) {
        val badgeColor = parseColorOrDefault(archetype.badgeColorHex, Color.parseColor(MINT))

        // Halo tinted to the archetype so each one reads as a distinct badge.
        val haloPaint = Paint().apply {
            isAntiAlias = true
            color = badgeColor
            alpha = 46
        }
        canvas.drawCircle(WIDTH / 2f, HALO_CENTER_Y, 200f, haloPaint)

        val iconPaint = Paint().apply {
            isAntiAlias = true
            textSize = 200f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(archetype.icon, WIDTH / 2f, ICON_BASELINE, iconPaint)

        val eyebrowPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor(MINT_SOFT)
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.2f
        }
        canvas.drawText("MY PLANT PARENT ARCHETYPE", WIDTH / 2f, EYEBROW_Y, eyebrowPaint)

        val titlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 92f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        // Shrink rather than clip: "Serial Overwaterer" is wider than "Cactus Survivor".
        val titleMaxWidth = WIDTH - 180f
        while (titlePaint.measureText(archetype.title) > titleMaxWidth && titlePaint.textSize > 56f) {
            titlePaint.textSize -= 4f
        }
        canvas.drawText(archetype.title, WIDTH / 2f, TITLE_Y, titlePaint)

        val descPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor(SAGE)
            textSize = 36f
            textAlign = Paint.Align.CENTER
        }
        var y = DESC_TOP
        wrapText(archetype.description, descPaint, WIDTH - 260f)
            .take(DESC_MAX_LINES)
            .forEach { line ->
                canvas.drawText(line, WIDTH / 2f, y, descPaint)
                y += DESC_LINE_HEIGHT
            }
    }

    private fun drawScoreRing(canvas: Canvas, score: Int) {
        val centerY = RING_CENTER_Y
        val radius = 105f

        val trackPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#22FFFFFF")
            style = Paint.Style.STROKE
            strokeWidth = 18f
        }
        val arcPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor(MINT)
            style = Paint.Style.STROKE
            strokeWidth = 18f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawCircle(WIDTH / 2f, centerY, radius, trackPaint)
        val bounds = RectF(
            WIDTH / 2f - radius, centerY - radius,
            WIDTH / 2f + radius, centerY + radius
        )
        canvas.drawArc(bounds, -90f, (score.coerceIn(0, 100) / 100f) * 360f, false, arcPaint)

        val scorePaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 68f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("$score%", WIDTH / 2f, centerY + 24f, scorePaint)

        val labelPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor(MINT_SOFT)
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.1f
        }
        canvas.drawText("BIOPHILIC VITALITY SCORE", WIDTH / 2f, RING_LABEL_Y, labelPaint)
    }

    private fun drawVibeTag(canvas: Canvas, vibeTag: String) {
        val tagTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor(PARCHMENT)
            textSize = 40f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val label = "✨ $vibeTag"

        // Pill hugs the text instead of spanning the card, so short tags don't float.
        val textWidth = tagTextPaint.measureText(label)
        val pillHalf = (textWidth / 2f + 44f).coerceAtMost(WIDTH / 2f - 110f)
        val tagRect = RectF(WIDTH / 2f - pillHalf, PILL_TOP, WIDTH / 2f + pillHalf, PILL_BOTTOM)

        val tagBgPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#267FE3B5")
        }
        canvas.drawRoundRect(tagRect, 46f, 46f, tagBgPaint)
        canvas.drawText(label, WIDTH / 2f, PILL_TEXT_Y, tagTextPaint)
    }

    private fun drawUpgrades(canvas: Canvas, upgrades: List<String>) {
        val titlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor(PARCHMENT)
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("🚀 My next sanctuary upgrades:", 110f, UPGRADES_TITLE_Y, titlePaint)

        val itemPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor(SAGE)
            textSize = 31f
        }
        val bulletPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor(MINT)
        }

        // One line per upgrade, ellipsized. The old renderer split at a fixed character
        // count, which cut words in half and could grow the block past the card.
        val maxWidth = WIDTH - 340f
        upgrades.take(3).forEachIndexed { index, item ->
            val y = UPGRADES_TOP + index * UPGRADES_LINE_HEIGHT
            canvas.drawCircle(130f, y - 10f, 9f, bulletPaint)
            canvas.drawText(ellipsize(item, itemPaint, maxWidth), 165f, y, itemPaint)
        }
    }

    private fun drawFooter(canvas: Canvas, shareCode: String?) {
        val qrSize = 150
        val qrLeft = 110f
        val qrTop = FOOTER_TOP

        val qr = QrCodeRenderer.encode(ShareLinks.shareUrl(shareCode), qrSize)
        var textLeft = qrLeft

        if (qr != null) {
            // White quiet-zone plate: scanners need contrast the dark card can't provide.
            val platePaint = Paint().apply {
                isAntiAlias = true
                color = Color.WHITE
            }
            val plate = RectF(qrLeft, qrTop, qrLeft + qrSize + 24f, qrTop + qrSize + 24f)
            canvas.drawRoundRect(plate, 16f, 16f, platePaint)
            canvas.drawBitmap(qr, qrLeft + 12f, qrTop + 12f, null)
            qr.recycle()
            textLeft = qrLeft + qrSize + 52f
        }

        val ctaPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor(MINT)
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Find your archetype 🌿", textLeft, qrTop + 58f, ctaPaint)

        val handlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor(PARCHMENT)
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(ShareLinks.HANDLE, textLeft, qrTop + 104f, handlePaint)

        val linkPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#88C3EBD9")
            textSize = 28f
        }
        canvas.drawText(ShareLinks.displayLink(shareCode), textLeft, qrTop + 146f, linkPaint)
    }

    // ------------------------------------------------------------------
    // Sharing
    // ------------------------------------------------------------------

    /**
     * Renders the card, hands it to the system chooser, and logs the attempt.
     *
     * Android reports neither the chosen app nor whether the post completed, so
     * `share_initiated` plus install attribution on the card's code is as close to a
     * measured loop as the platform allows.
     */
    fun shareCard(context: Context, data: ShareCardData, surface: String) {
        try {
            val bitmap = generate9by16StoryBitmap(context, data)
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "floraflow_archetype_card.png")
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            bitmap.recycle()

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "My Plant Parent Archetype")
                putExtra(Intent.EXTRA_TEXT, shareText(data))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share your archetype")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

            ShareAnalytics.logShareInitiated(
                surface = surface,
                asset = ShareAnalytics.Asset.STORY_CARD,
                archetype = data.archetype.name,
                score = data.score
            )
        } catch (e: Exception) {
            Log.e(TAG, "Story card share failed", e)
            ShareAnalytics.logShareFailed(
                surface = surface,
                asset = ShareAnalytics.Asset.STORY_CARD,
                reason = e.javaClass.simpleName
            )
        }
    }

    /** Caption copy. Leads with the archetype, because that is what gets quoted back. */
    fun shareText(data: ShareCardData): String =
        "I'm a ${data.archetype.icon} ${data.archetype.title} — my indoor sanctuary scores " +
            "${data.score}%. What's your Plant Parent Archetype? " +
            ShareLinks.shareUrl(data.shareCode)

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Trims to [maxWidth], appending an ellipsis only when text actually had to be cut. */
    internal fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var candidate = text
        while (candidate.isNotEmpty() && paint.measureText("$candidate…") > maxWidth) {
            candidate = candidate.dropLast(1)
        }
        return candidate.trimEnd() + "…"
    }

    /** Greedy word wrap against real measured text width. */
    internal fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return emptyList()
        val words = text.trim().split(' ')
        val lines = mutableListOf<String>()
        var current = StringBuilder()

        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth || current.isEmpty()) {
                current = StringBuilder(candidate)
            } else {
                lines.add(current.toString())
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }

    private fun parseColorOrDefault(hex: String, fallback: Int): Int =
        try {
            Color.parseColor(hex)
        } catch (e: IllegalArgumentException) {
            fallback
        }
}
