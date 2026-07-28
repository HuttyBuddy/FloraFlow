package com.example.ui.components.graphics

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.intellij.lang.annotations.Language
import kotlin.math.cos
import kotlin.math.sin

@Language("AGSL")
private val DAPPLED_SUNLIGHT_AGSL = """
    uniform shader uContent;
    uniform float2 uResolution;
    uniform float uTime;
    uniform float uIntensity;

    half4 main(in float2 fragCoord) {
        half4 content = uContent.eval(fragCoord);
        float2 st = fragCoord / uResolution;

        // Dappled leaf canopy shadow noise simulation
        float wave1 = sin(st.x * 8.0 + uTime * 0.4) * cos(st.y * 7.0 + uTime * 0.3);
        float wave2 = cos(st.x * 14.0 - uTime * 0.5) * sin(st.y * 12.0 + uTime * 0.7);
        float dapple = (wave1 + wave2 * 0.5) * 0.5 + 0.5;

        // Warm golden sunlight ray (#F4D03F) vs deep forest shadow (#1F483E)
        half3 sunRay = half3(0.95, 0.81, 0.24);
        half3 leafShadow = half3(0.12, 0.28, 0.24);

        half3 dappledColor = mix(leafShadow, sunRay, dapple * 0.5 + 0.25);
        float alpha = uIntensity * (0.4 + dapple * 0.6);

        return mix(content, half4(dappledColor, 1.0), alpha);
    }
""".trimIndent()

/**
 * Modifier that applies a dynamic dappled sunlight & leaf canopy shadow overlay.
 * Uses hardware-accelerated AGSL RuntimeShader on Android 13+ (API 33+),
 * falling back to custom Compose drawWithCache radial gradients on API < 33.
 */
fun Modifier.dappledSunlightOverlay(
    enabled: Boolean = true,
    intensity: Float = 0.18f
): Modifier = composed {
    if (!enabled) return@composed this

    val lifecycleOwner = LocalLifecycleOwner.current
    var isResumed by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isResumed = (event == Lifecycle.Event.ON_RESUME)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val transition = rememberInfiniteTransition(label = "dappledSunlightTransition")
    val animPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (isResumed) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dappledPhase"
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { RuntimeShader(DAPPLED_SUNLIGHT_AGSL) }
        this.graphicsLayer {
            shader.setFloatUniform("uTime", animPhase * 14f)
            shader.setFloatUniform("uIntensity", intensity)
            shader.setFloatUniform("uResolution", size.width, size.height)
            renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "uContent").asComposeRenderEffect()
        }
    } else {
        // Compose drawWithCache fallback for Android API < 33
        this.drawWithCache {
            val w = size.width
            val h = size.height

            val cx1 = w * (0.2f + 0.3f * sin(animPhase * 6.28f))
            val cy1 = h * (0.15f + 0.2f * cos(animPhase * 6.28f))

            val sunGradient = Brush.radialGradient(
                colors = listOf(
                    Color(0x33F4D03F),
                    Color(0x1ADB9724),
                    Color.Transparent
                ),
                center = Offset(cx1, cy1),
                radius = w.coerceAtLeast(h) * 0.75f
            )

            val shadowGradient = Brush.radialGradient(
                colors = listOf(
                    Color(0x221F483E),
                    Color.Transparent
                ),
                center = Offset(w * 0.8f, h * 0.8f),
                radius = w.coerceAtLeast(h) * 0.6f
            )

            onDrawWithContent {
                drawContent()
                drawRect(brush = sunGradient)
                drawRect(brush = shadowGradient)
            }
        }
    }
}

/**
 * Standalone DappledSunlightCanvas component for rendering animated canopy sunlight rays.
 */
@Composable
fun DappledSunlightCanvas(
    modifier: Modifier = Modifier,
    intensity: Float = 0.2f,
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .dappledSunlightOverlay(intensity = intensity)
    ) {
        content()
    }
}
