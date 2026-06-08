package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.sceneview.ar.ARSceneView

@Composable
fun TestAR() {
    ARSceneView(
        modifier = Modifier,
        onTouchEvent = { motionEvent, hitResult ->
            true
        }
    )
}
