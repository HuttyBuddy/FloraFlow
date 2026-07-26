package com.example.ui.components.graphics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BiophilicPrimary
import com.example.ui.theme.BiophilicSecondary

/**
 * Draws organic leaf & stem vector flourishes in card corners using hardware-accelerated Compose Canvas.
 */
@Composable
fun BotanicalCornerAccents(
    modifier: Modifier = Modifier,
    leafColor: Color = BiophilicPrimary.copy(alpha = 0.25f),
    accentColor: Color = BiophilicSecondary.copy(alpha = 0.35f),
    showTopRight: Boolean = true,
    showBottomLeft: Boolean = false
) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            if (showTopRight && w > 60.dp.toPx()) {
                // Top-Right Stem Curve
                val stemPath = Path().apply {
                    moveTo(w - 60.dp.toPx(), 0f)
                    cubicTo(
                        w - 35.dp.toPx(), 10.dp.toPx(),
                        w - 15.dp.toPx(), 30.dp.toPx(),
                        w, 50.dp.toPx()
                    )
                }
                drawPath(
                    path = stemPath,
                    color = leafColor,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Top-Right Leaf 1
                val leaf1Path = Path().apply {
                    moveTo(w - 40.dp.toPx(), 12.dp.toPx())
                    quadraticTo(
                        w - 52.dp.toPx(), 28.dp.toPx(),
                        w - 38.dp.toPx(), 32.dp.toPx()
                    )
                    quadraticTo(
                        w - 28.dp.toPx(), 22.dp.toPx(),
                        w - 40.dp.toPx(), 12.dp.toPx()
                    )
                }
                drawPath(path = leaf1Path, color = accentColor)

                // Top-Right Leaf 2
                val leaf2Path = Path().apply {
                    moveTo(w - 22.dp.toPx(), 28.dp.toPx())
                    quadraticTo(
                        w - 32.dp.toPx(), 44.dp.toPx(),
                        w - 18.dp.toPx(), 46.dp.toPx()
                    )
                    quadraticTo(
                        w - 10.dp.toPx(), 34.dp.toPx(),
                        w - 22.dp.toPx(), 28.dp.toPx()
                    )
                }
                drawPath(path = leaf2Path, color = leafColor)
            }

            if (showBottomLeft && h > 60.dp.toPx()) {
                // Bottom-Left Stem Curve
                val stemPath = Path().apply {
                    moveTo(0f, h - 50.dp.toPx())
                    cubicTo(
                        15.dp.toPx(), h - 30.dp.toPx(),
                        35.dp.toPx(), h - 10.dp.toPx(),
                        60.dp.toPx(), h
                    )
                }
                drawPath(
                    path = stemPath,
                    color = leafColor,
                    style = Stroke(width = 1.8.dp.toPx())
                )

                // Bottom-Left Leaf
                val leafPath = Path().apply {
                    moveTo(25.dp.toPx(), h - 25.dp.toPx())
                    quadraticTo(
                        12.dp.toPx(), h - 42.dp.toPx(),
                        30.dp.toPx(), h - 44.dp.toPx()
                    )
                    quadraticTo(
                        38.dp.toPx(), h - 30.dp.toPx(),
                        25.dp.toPx(), h - 25.dp.toPx()
                    )
                }
                drawPath(path = leafPath, color = accentColor)
            }
        }
    }
}
