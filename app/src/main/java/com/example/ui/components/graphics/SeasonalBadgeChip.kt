package com.example.ui.components.graphics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.EnergySavingsLeaf
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BiophilicPrimary
import com.example.ui.theme.BiophilicSecondary
import com.example.ui.theme.PremiumGold

enum class BotanicalSeason(val label: String, val icon: ImageVector) {
    SPRING_BLOOM("Spring Dew", Icons.Default.Eco),
    SUMMER_SOLSTICE("Summer Canopy", Icons.Default.WbSunny),
    AUTUMN_HARVEST("Autumn Solstice", Icons.Default.EnergySavingsLeaf),
    WINTER_REST("Winter Dormancy", Icons.Default.AcUnit)
}

/**
 * Editorial seasonal badge chip rendered with gradient stroke borders and vector leaf icon indicators.
 */
@Composable
fun SeasonalBadgeChip(
    modifier: Modifier = Modifier,
    season: BotanicalSeason = BotanicalSeason.SPRING_BLOOM,
    containerColor: Color = BiophilicPrimary.copy(alpha = 0.10f),
    contentColor: Color = BiophilicPrimary
) {
    val gradientBorder = Brush.horizontalGradient(
        colors = listOf(
            BiophilicPrimary.copy(alpha = 0.5f),
            BiophilicSecondary.copy(alpha = 0.7f),
            PremiumGold.copy(alpha = 0.4f)
        )
    )

    Row(
        modifier = modifier
            .background(
                color = containerColor,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                brush = gradientBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = season.icon,
            contentDescription = season.label,
            tint = contentColor,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = season.label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            ),
            color = contentColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
