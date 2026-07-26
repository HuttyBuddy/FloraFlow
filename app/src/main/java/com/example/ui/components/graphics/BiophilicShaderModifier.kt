package com.example.ui.components.graphics

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.theme.BiophilicPrimary
import com.example.ui.theme.BiophilicSecondary
import org.intellij.lang.annotations.Language

@Language("AGSL")
private val AGSL_FOLIAGE_SHADER = """
    uniform shader uContent;
    uniform float2 uResolution;
    uniform float uTime;
    uniform float uAlpha;
    
    half4 main(in float2 fragCoord) {
        half4 content = uContent.eval(fragCoord);
        float2 st = fragCoord / uResolution;
        
        // Dynamic procedural sunlight-through-leaves noise simulation
        float wave1 = sin(st.x * 6.0 + uTime * 0.5) * cos(st.y * 5.0 + uTime * 0.3);
        float wave2 = cos(st.x * 12.0 - uTime * 0.4) * sin(st.y * 10.0 + uTime * 0.6);
        float pattern = (wave1 + wave2 * 0.5) * 0.5 + 0.5;
        
        // Deep Botanical Forest green (#1F483E) into Warm Amber/Gold (#D4AF37)
        half3 colorBg = half3(0.12, 0.28, 0.24);
        half3 colorSun = half3(0.83, 0.68, 0.21);
        half3 overlayColor = mix(colorBg, colorSun, pattern * 0.35);
        float overlayAlpha = uAlpha * (0.6 + pattern * 0.4);
        
        return mix(content, half4(overlayColor, 1.0), overlayAlpha);
    }
""".trimIndent()

/**
 * Applies a dynamic biophilic foliage shimmer gradient overlay.
 * Uses hardware-accelerated Compose drawWithCache for 100% rendering safety
 * across all Android API levels and emulator configurations without gray-box defects.
 */
fun Modifier.biophilicShader(
    enabled: Boolean = true,
    alpha: Float = 0.12f
): Modifier = composed {
    if (!enabled) return@composed this

    // Lifecycle-aware animation handling
    val lifecycleOwner = LocalLifecycleOwner.current
    var isAppResumed by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isAppResumed = (event == Lifecycle.Event.ON_RESUME)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val transition = rememberInfiniteTransition(label = "biophilicShaderTransition")
    val rawTime by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (isAppResumed) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10_000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "biophilicShaderTime"
    )

    this.drawWithCache {
        val width = size.width
        val height = size.height

        val shaderBrush = Brush.radialGradient(
            colors = listOf(
                BiophilicPrimary.copy(alpha = alpha * (0.8f + rawTime * 0.4f)),
                BiophilicSecondary.copy(alpha = alpha * (0.4f + (1f - rawTime) * 0.3f)),
                Color.Transparent
            ),
            center = androidx.compose.ui.geometry.Offset(width * 0.8f, height * 0.2f),
            radius = (width.coerceAtLeast(height)) * 0.9f
        )
        onDrawWithContent {
            drawContent()
            drawRect(brush = shaderBrush)
        }
    }
}

