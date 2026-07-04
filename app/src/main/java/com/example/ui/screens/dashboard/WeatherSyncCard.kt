package com.example.ui.screens.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.WeatherInfo

@Composable
fun WeatherSyncCard(
    weather: WeatherInfo,
    onWeatherClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Dynamic styling based on condition
    val themeInfo = when (weather.condition.lowercase()) {
        "rain" -> WeatherThemeInfo(
            "🌧️",
            "Natural rain detected. Skip manual watering today to avoid root rot and waterlogging.",
            Color(0xFF1E88E5),
            Brush.verticalGradient(listOf(Color(0xFFE3F2FD), Color(0xFFBBDEFB)))
        )
        "snow" -> WeatherThemeInfo(
            "❄️",
            "Freezing snow! Cover outdoor crops and move container pots indoors to shield roots.",
            Color(0xFF00ACC1),
            Brush.verticalGradient(listOf(Color(0xFFE0F7FA), Color(0xFFB2EBF2)))
        )
        "clear" -> WeatherThemeInfo(
            "☀️",
            "Sunny day with high evaporation. Consider watering early in the morning before sun peaks.",
            Color(0xFFFDD835),
            Brush.verticalGradient(listOf(Color(0xFFFFFDE7), Color(0xFFFFF9C4)))
        )
        "clouds" -> WeatherThemeInfo(
            "☁️",
            "Overcast skies slow down soil evaporation. Always check soil moisture before watering.",
            Color(0xFF757575),
            Brush.verticalGradient(listOf(Color(0xFFF5F5F5), Color(0xFFE0E0E0)))
        )
        "heatwave" -> WeatherThemeInfo(
            "🥵",
            "Extreme heatwave active! Move sensitive plants to shade, water deeply, and apply mulch.",
            Color(0xFFE53935),
            Brush.verticalGradient(listOf(Color(0xFFFFEBEE), Color(0xFFFFCDD2)))
        )
        "frost" -> WeatherThemeInfo(
            "🥶",
            "Frost warning! Cover sensitive outdoor plant foliage to prevent cell freezing.",
            Color(0xFF3949AB),
            Brush.verticalGradient(listOf(Color(0xFFE8EAF6), Color(0xFFC5CAE9)))
        )
        else -> WeatherThemeInfo(
            "⛅",
            "Mild climate conditions. Ideal day for companion pruning or sowed plant rearrangement.",
            Color(0xFF43A047),
            Brush.verticalGradient(listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9)))
        )
    }
    val weatherEmoji = themeInfo.weatherEmoji
    val alertText = themeInfo.alertText
    val themeColor = themeInfo.themeColor
    val bgGradient = themeInfo.bgGradient

    // Gentle pulse animation on card border to feel responsive
    val infiniteTransition = rememberInfiniteTransition(label = "WeatherPulse")
    val animatedAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlphaPulse"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onWeatherClick() }
            .testTag("weather_sync_card"),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, themeColor.copy(alpha = animatedAlpha))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgGradient)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(weatherEmoji, fontSize = 28.sp)
                    Column {
                        Text(
                            text = weather.cityName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D3C28)
                        )
                        Text(
                            text = "Eco-Zone Weather Sync",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1D3C28).copy(alpha = 0.7f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onWeatherClick() }
                        .background(Color.White.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Change Location",
                        tint = Color(0xFF1D3C28),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Edit Loc",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D3C28)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Temp
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    Icon(Icons.Default.Thermostat, contentDescription = null, tint = themeColor, modifier = Modifier.size(18.dp))
                    Text(
                        text = "${weather.temperatureFahrenheit.toInt()}°F",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1D3C28)
                    )
                }

                // Humidity
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    Icon(Icons.Default.WaterDrop, contentDescription = null, tint = themeColor, modifier = Modifier.size(18.dp))
                    Text(
                        text = "${weather.humidity.toInt()}% Hum",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1D3C28)
                    )
                }
            }

            // Weather Alert Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Alert Tip",
                    tint = themeColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = alertText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1D3C28),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

private data class WeatherThemeInfo(
    val weatherEmoji: String,
    val alertText: String,
    val themeColor: Color,
    val bgGradient: Brush
)
