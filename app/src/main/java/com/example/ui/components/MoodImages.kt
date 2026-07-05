package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.R

/**
 * Real photography representing each logged garden mood, replacing the
 * emoji-only look of the mental well-being logs with editorial-grade imagery.
 */
object MoodImages {

    @DrawableRes
    fun forMood(mood: String?): Int? = when (mood) {
        "Peaceful" -> R.drawable.mood_peaceful
        "Energized" -> R.drawable.mood_energized
        "Refreshed" -> R.drawable.mood_refreshed
        "Happy" -> R.drawable.mood_happy
        "Anxious" -> R.drawable.mood_anxious
        "Tired" -> R.drawable.mood_tired
        else -> null
    }
}

/**
 * Photo tile for a logged mood: shows real mood photography when available,
 * otherwise the emoji fallback (used for the unlogged "tap to add" state).
 */
@Composable
fun MoodPhoto(
    mood: String?,
    fallbackEmoji: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp),
    emojiFontSize: TextUnit = 24.sp
) {
    val imageRes = MoodImages.forMood(mood)
    Box(
        modifier = modifier.clip(shape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        if (imageRes != null) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = mood?.let { "$it mood photo" } ?: "Mood photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(fallbackEmoji, fontSize = emojiFontSize, textAlign = TextAlign.Center)
        }
    }
}
