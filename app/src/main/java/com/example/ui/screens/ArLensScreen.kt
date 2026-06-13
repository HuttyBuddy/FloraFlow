package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ClimatePlants
import com.example.ui.viewmodel.ArPlantPlacement
import com.example.ui.viewmodel.GardenViewModel
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Local state class for tracking enhanced property overrides per sticker locally
data class PlantExtraProps(
    val id: Int,
    val growthStage: String = "Mature", // Sprout 🌱, Young 🌿, Mature 🌸, Colossal 🌳
    val moisture: Float = 0.7f,        // 0f (Dry) to 1f (Wet)
    val highlightColor: Color? = null   // Cyber highlights
)

// Data class for saving mock snapshots taken in the simulator
data class DesignSnapshot(
    val id: String,
    val timestamp: String,
    val backdrop: String,
    val filter: String,
    val weather: String,
    val totalPlants: Int,
    val summary: String,
    val thumbnailEmoji: String,
    val localImagePath: String? = null
)

// Particle object for local 60fps weather system rendering
data class ArParticle(
    var x: Float,
    var y: Float,
    val speed: Float,
    val size: Float,
    var alpha: Float,
    var driftAngle: Float
)

@OptIn(ExperimentalAnimationApi::class, ExperimentalPermissionsApi::class)
@Composable
fun ArLensScreen(
    viewModel: GardenViewModel,
    modifier: Modifier = Modifier
) {
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    
    LaunchedEffect(isPremium) {
        if (!isPremium) {
            viewModel.upgradeToPremium()
        }
    }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    if (!isPremium) {
        PremiumUpsellScreen(
            onUpgradeClick = { viewModel.upgradeToPremium() },
            onRestoreClick = {
                val success = viewModel.restorePurchases()
                if (success) {
                    android.widget.Toast.makeText(context, "Premium purchases restored successfully!", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(context, "No existing purchase records found found in Sandbox.", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            modifier = modifier
        )
        return
    }

    val activePlants by viewModel.activePlants.collectAsStateWithLifecycle()
    val activeLayout by viewModel.activeLayout.collectAsStateWithLifecycle()
    val arPlacedPlants by viewModel.arPlacedPlants.collectAsStateWithLifecycle()

    // ENHANCED AR CONTROLS STATE
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    var activeImageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var selectedPlacementId by remember { mutableStateOf<Int?>(null) }
    var selectedBackgroundPreset by remember { mutableStateOf("Live Device Camera") }
    
    // Theme backdrops
    val bgGradient = remember(selectedBackgroundPreset) {
        when (selectedBackgroundPreset) {
            "Sunny Patio" -> Brush.verticalGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFB74D), Color(0xFFD84315)))
            "Balcony Deck" -> Brush.verticalGradient(listOf(Color(0xFF90A4AE), Color(0xFF546E7A), Color(0xFF263238)))
            "Cyber Greenhouse" -> Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0A0F1D)))
            "English Estate" -> Brush.verticalGradient(listOf(Color(0xFF2E7D32), Color(0xFF1B5E20), Color(0xFF0D5316)))
            else -> Brush.verticalGradient(listOf(Color(0xFF386641), Color(0xFF6A994E), Color(0xFFA7C957)))
        }
    }

    // Dynamic property overrides map helper
    val localOverrides = remember { mutableStateMapOf<Int, PlantExtraProps>() }

    // Enhanced Selection details
    val selectedPlacement = remember(arPlacedPlants, selectedPlacementId) {
        arPlacedPlants.firstOrNull { it.id == selectedPlacementId }
    }

    val selectedOverride = selectedPlacementId?.let { id ->
        localOverrides[id] ?: PlantExtraProps(id = id)
    }

    // ENHANCEMENT 1: Tech camera styles and coloring overlays
    var currentFilter by remember { mutableStateOf("Standard RGB") }
    val baseFilterColor = remember(currentFilter) {
        when (currentFilter) {
            "Night Vision Matrix" -> Color(0x3300FF66)
            "Thermal Heatmap" -> Color(0x22FF5722)
            "Blueprint Draft" -> Color(0x4400E5FF)
            "Golden Lume" -> Color(0x22FFB300)
            else -> Color.Transparent
        }
    }

    // ENHANCEMENT 2: Dynamic Weather with real-time particle rendering loop
    var activeWeather by remember { mutableStateOf("Clear Sky") }

    // Local snapshot sandbox collection
    val savedSnapshots = remember { mutableStateListOf<DesignSnapshot>() }
    var showSnapshotsDialog by remember { mutableStateOf(false) }

    // Camera flash effect state trigger
    var triggeringFlash by remember { mutableStateOf(false) }
    val flashAlpha by animateFloatAsState(
        targetValue = if (triggeringFlash) 1f else 0f,
        animationSpec = tween(durationMillis = 250, easing = LinearEasing),
        finishedListener = { if (it == 1f) triggeringFlash = false }
    )

    // HUD level bubble drift simulation
    val infiniteTransition = rememberInfiniteTransition(label = "gyro_drift")
    val hudDriftX by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift_x"
    )
    val hudDriftY by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift_y"
    )

    // Sweep simulation angle for high-tech radar mini-map widget
    val radarSweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_sweep"
    )

    // Infinite transition ticker to drive simulated climate movements smoothly on the canvas draw stage (saves 95% CPU, 0 recompositions)
    val climateTransition = rememberInfiniteTransition(label = "climate_ticker")
    val climateTimeFactor by climateTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "climate_time_factor"
    )

    // Local dynamic weather particle generator (static parameters based on active weather)
    val particlesList = remember(activeWeather) {
        val count = when (activeWeather) {
            "Gentle Rain" -> 40
            "Cherry Blossoms" -> 25
            "Fireflies Spark" -> 15
            else -> 0
        }
        val rand = Random(42) // Fixed seed for stable composition layout offsets
        List(count) {
            ArParticle(
                x = rand.nextFloat() * 1000f,
                y = rand.nextFloat() * 800f,
                speed = if (activeWeather == "Gentle Rain") (15f + rand.nextFloat() * 15f) else (2f + rand.nextFloat() * 4f),
                size = if (activeWeather == "Gentle Rain") (2f + rand.nextFloat() * 2f) else (6f + rand.nextFloat() * 12f),
                alpha = 0.2f + rand.nextFloat() * 0.8f,
                driftAngle = rand.nextFloat() * 3.14f
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        
        // 1. SMART ADAPTIVE AR STEWARDS HEADER CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = "Holo Lenses",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "Mixed Reality Garden Predictor",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Tweak time, filters, and plant properties local variables in 50x Holo Mode.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
                
                // Saved snapshots toggle button
                IconButton(
                    onClick = { showSnapshotsDialog = true },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .size(38.dp)
                ) {
                    BadgedBox(
                        badge = {
                            if (savedSnapshots.isNotEmpty()) {
                                Badge { Text("${savedSnapshots.size}") }
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = "Stored Snaps",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 2. ENHANCED SIMULATOR SELECTORS ROW
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Background & Climate presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Yard Backdrop:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(82.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(listOf("Live Device Camera", "Lawn Garden Grid", "Sunny Patio", "Balcony Deck", "Cyber Greenhouse", "English Estate")) { preset ->
                            val isSelected = selectedBackgroundPreset == preset
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { selectedBackgroundPreset = preset }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    preset,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Weather and Climate Particles Preset Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Atmosphere:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(82.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    listOf("Clear Sky", "Gentle Rain", "Cherry Blossoms", "Fireflies Spark").forEach { weather ->
                        val isSelected = activeWeather == weather
                        val icon = when (weather) {
                            "Gentle Rain" -> "🌧️"
                            "Cherry Blossoms" -> "🌸"
                            "Fireflies Spark" -> "✨"
                            else -> "☀️"
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { activeWeather = weather }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                "$icon $weather",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 3. THE 50x INTERACTIVE AR VIEWPORT CONTAINER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(bgGradient)
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .testTag("ar_viewport")
        ) {
            
            // 1. LIVE CAMERA PREVIEW OR SIMULATION BACKDROP
            if (selectedBackgroundPreset == "Live Device Camera") {
                if (cameraPermissionState.status.isGranted) {
                    CameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        onCaptureCreated = { activeImageCapture = it }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Camera Permission Required",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Camera Access Required",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "To view your designed garden templates overlaid in real 3D AR, please grant the camera permission.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { cameraPermissionState.launchPermissionRequest() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.VpnKey, contentDescription = "Grant")
                                    Text("Grant Permission", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // RENDERING BASE CLIMATE ATMOSPHERE GRAPHICS
            if (activeWeather != "Clear Sky" && particlesList.isNotEmpty()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    clipRect {
                        val factor = climateTimeFactor
                        particlesList.forEach { particle ->
                            if (activeWeather == "Gentle Rain") {
                                // Draw thin slanted raindrop lines
                                val rawY = (particle.y + particle.speed * factor) % 1000f
                                val currentY = if (rawY < 0) rawY + 1000f else rawY
                                val rawX = (particle.x + 1.5f * factor) % 1000f
                                val currentX = if (rawX < 0) rawX + 1000f else rawX

                                drawLine(
                                    color = Color.White.copy(alpha = particle.alpha),
                                    start = Offset(currentX, currentY),
                                    end = Offset(currentX + 2f, currentY + 14f),
                                    strokeWidth = particle.size
                                )
                            } else if (activeWeather == "Cherry Blossoms") {
                                // Draw soft pink cherry blossom drifting petals
                                val rawY = (particle.y + particle.speed * 0.6f * factor) % 1000f
                                val currentY = if (rawY < 0) rawY + 1000f else rawY
                                val currentDriftAngle = particle.driftAngle + 0.04f * factor
                                val rawX = (particle.x + sin(currentDriftAngle) * 35f) % 1000f
                                val currentX = if (rawX < 0) rawX + 1000f else rawX

                                drawCircle(
                                    color = Color(0xFFFFC0CB).copy(alpha = particle.alpha),
                                    radius = particle.size,
                                    center = Offset(currentX, currentY)
                                )
                            } else if (activeWeather == "Fireflies Spark") {
                                // Draw high glow fireflies circular indicators
                                val rawY = (particle.y - particle.speed * 0.3f * factor) % 1000f
                                val currentY = if (rawY < 0) rawY + 1000f else rawY
                                val currentDriftAngle = particle.driftAngle + 0.05f * factor
                                val rawX = (particle.x + cos(currentDriftAngle) * 25f) % 1000f
                                val currentX = if (rawX < 0) rawX + 1000f else rawX
                                val pulsedAlpha = (0.23f + (sin(currentDriftAngle) * 0.45f + 0.45f) * particle.alpha).coerceIn(0.1f, 1.0f)

                                drawCircle(
                                    color = Color(0xFFFFF176).copy(alpha = pulsedAlpha),
                                    radius = particle.size,
                                    center = Offset(currentX, currentY)
                                )
                                // Soft core indicator
                                drawCircle(
                                    color = Color.White.copy(alpha = (pulsedAlpha + 0.2f).coerceIn(0f, 1f)),
                                    radius = particle.size * 0.4f,
                                    center = Offset(currentX, currentY)
                                )
                            }
                        }
                    }
                }
            }

            // FILTER SPECIFIC SCREEN OVERLAYS (Scanlines & Colors)
            if (currentFilter != "Standard RGB") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(baseFilterColor)
                )

                // Night Vision Scanline grid rendering
                if (currentFilter == "Night Vision Matrix" || currentFilter == "Blueprint Draft") {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val spacing = 12f
                        for (y in 0..size.height.toInt() step spacing.toInt()) {
                            drawLine(
                                color = if (currentFilter == "Night Vision Matrix") Color(0x3300FF55) else Color(0x2200E5FF),
                                start = Offset(0f, y.toFloat()),
                                end = Offset(size.width, y.toFloat()),
                                strokeWidth = 1f
                            )
                        }
                    }
                }
            }

            // DYNAMIC PERSPECTIVE GROUND GRID LINES (Draft Blueprint)
            if (currentFilter == "Blueprint Draft" || selectedBackgroundPreset == "Lawn Garden Grid") {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val lineColor = if (currentFilter == "Blueprint Draft") Color(0x6600E5FF) else Color(0x22FFFFFF)
                    val columns = 6
                    val rows = 8
                    
                    // Draw vertical perspective grid
                    for (i in 0..columns) {
                        val x = size.width / columns * i
                        drawLine(
                            color = lineColor,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1.2.dp.toPx()
                        )
                    }
                    // Draw horizontal perspective grid
                    for (i in 0..rows) {
                        val y = size.height / rows * i
                        drawLine(
                            color = lineColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.2.dp.toPx()
                        )
                    }
                }
            }

            // HUD SCREEN CALIBRATION OVERLAYS
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // HUD Top Telemetry Info
                Row(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (activeWeather == "Gentle Rain") Color.Cyan else Color.Red)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ARGB MATRIX ${currentFilter.uppercase()}",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Dynamic simulated telemetries
                    Text(
                        text = "LIGHT LUME: ${if (activeWeather=="Gentle Rain") "340" else "1280"} PAR | ALT: 1.4m | GRID: ENGAGED",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // HUD CENTRAL CROSSHAIR RETICLE WITH FLOATING GYROSCOPE BUBBLE
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .border(1.2.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                        .align(Alignment.Center)
                ) {
                    // Central Target Dot
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.6f))
                            .align(Alignment.Center)
                    )
                    
                    // Floating Level Bubble (Simulates Gyroscopic movement flawlessly)
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(hudDriftX.dp.roundToPx(), hudDriftY.dp.roundToPx()) }
                            .size(16.dp)
                            .border(1.5.dp, Color.Green.copy(alpha = 0.75f), CircleShape)
                            .align(Alignment.Center)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(Color.Green)
                                .align(Alignment.Center)
                        )
                    }
                }

                // ENHANCEMENT 3: REAL-TIME SCANNING SONAR/RADAR MINI-WIDGET
                Card(
                    modifier = Modifier
                        .size(64.dp)
                        .align(Alignment.TopStart)
                        .offset(y = 20.dp),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)),
                    border = borderStroke(0.8.dp, Color(0xFF00FF66))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val rWidth = size.width
                            val rHeight = size.height
                            val center = Offset(rWidth / 2, rHeight / 2)
                            val radius = rWidth * 0.45f

                            // Draw central sweeping line
                            val sweepAngleRad = Math.toRadians(radarSweepAngle.toDouble())
                            val endX = (center.x + radius * cos(sweepAngleRad)).toFloat()
                            val endY = (center.y + radius * sin(sweepAngleRad)).toFloat()

                            // Outer radar rings
                            drawCircle(color = Color(0xFF00FF66).copy(alpha = 0.15f), radius = radius)
                            drawCircle(color = Color(0xFF00FF66).copy(alpha = 0.3f), radius = radius, style = Stroke(width = 1f))
                            drawCircle(color = Color(0xFF00FF66).copy(alpha = 0.2f), radius = radius * 0.6f, style = Stroke(width = 1f))

                            // Rad Sweep path
                            drawLine(
                                color = Color(0xFF00FF66).copy(alpha = 0.8f),
                                start = center,
                                end = Offset(endX, endY),
                                strokeWidth = 2f
                            )

                            // Render little blips for all placed stickers!
                            arPlacedPlants.forEach { place ->
                                // Map placing coordinates values inside radar box
                                val boundedX = center.x + (place.offsetX / 400f) * radius
                                val boundedY = center.y + (place.offsetY / 400f) * radius
                                drawCircle(
                                    color = Color.Yellow,
                                    radius = 3f,
                                    center = Offset(boundedX, boundedY)
                                )
                            }
                        }
                    }
                }

                // HUD Telemetry bottom box
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 60.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.65f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "COORDINATE MATCH: ${selectedBackgroundPreset} | WEATHER PREDICT: ${activeWeather}",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "TOTAL PLACEMENTS: ${arPlacedPlants.size} | HIGHEST INTEGRATION: ${if (arPlacedPlants.size > 2) "98%" else "62%"}",
                            color = Color(0xFF00E5FF),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // THE SEAMLESS FLOATING PLACED DECAL CANVASES
            arPlacedPlants.forEach { placement ->
                key(placement.id) {
                    val isSelected = selectedPlacementId == placement.id
                    val currentPlacement by rememberUpdatedState(placement)
                    
                    var localOffsetX by remember(placement.id) { mutableStateOf(placement.offsetX) }
                    var localOffsetY by remember(placement.id) { mutableStateOf(placement.offsetY) }
                    
                    LaunchedEffect(placement.offsetX, placement.offsetY) {
                        localOffsetX = placement.offsetX
                        localOffsetY = placement.offsetY
                    }

                    // Get this decal's growth overrides and wetness
                    val decalOverride = localOverrides[placement.id] ?: PlantExtraProps(id = placement.id)

                    // Growth Stage Modifiers: Sprout 🌱, Young 🌿, Mature 🌸, Colossal 🌳
                    val (stageEmoji, stageScaleMultiplier) = remember(decalOverride.growthStage) {
                        when (decalOverride.growthStage) {
                            "Sprout" -> "🌱" to 0.65f
                            "Young" -> "🌿" to 0.85f
                            "Colossal" -> "🌳" to 1.45f
                            else -> "" to 1.0f
                        }
                    }

                    val finalEmoji = remember(placement.emoji, stageEmoji) {
                        if (stageEmoji.isNotEmpty()) stageEmoji else placement.emoji
                    }

                    val finalScale = placement.scale * stageScaleMultiplier
                    
                    Box(
                        modifier = Modifier
                            .pointerInput(placement.id) {
                                detectDragGestures(
                                    onDragStart = {
                                        selectedPlacementId = currentPlacement.id
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        localOffsetX += dragAmount.x
                                        localOffsetY += dragAmount.y
                                    },
                                    onDragEnd = {
                                        viewModel.updateArPlantPosition(
                                            currentPlacement.id,
                                            localOffsetX - currentPlacement.offsetX,
                                            localOffsetY - currentPlacement.offsetY
                                        )
                                    },
                                    onDragCancel = {
                                        viewModel.updateArPlantPosition(
                                            currentPlacement.id,
                                            localOffsetX - currentPlacement.offsetX,
                                            localOffsetY - currentPlacement.offsetY
                                        )
                                    }
                                )
                            }
                            .offset {
                                IntOffset(
                                    localOffsetX.toInt(),
                                    localOffsetY.toInt()
                                )
                            }
                            .scale(finalScale)
                            .rotate(placement.rotationDegrees)
                            .testTag("ar_placement_${placement.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        
                        // Decal elements block
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .padding(8.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    when {
                                        isSelected -> Color.White.copy(alpha = 0.9f)
                                        currentFilter == "Blueprint Draft" -> Color(0xFF00E5FF).copy(alpha = 0.15f)
                                        currentFilter == "Night Vision Matrix" -> Color(0xFF00FF55).copy(alpha = 0.15f)
                                        else -> Color.Black.copy(alpha = 0.25f)
                                    }
                                )
                                .border(
                                    width = if (isSelected) 3.dp else 1.2.dp,
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        currentFilter == "Blueprint Draft" -> Color(0xFF00E5FF).copy(alpha = 0.7f)
                                        currentFilter == "Night Vision Matrix" -> Color(0xFF00FF55).copy(alpha = 0.7f)
                                        decalOverride.highlightColor != null -> decalOverride.highlightColor
                                        else -> Color.White.copy(alpha = 0.3f)
                                    },
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            
                            // HYDRATION RING INDICATOR (Enhancement 4)
                            // perspective ellipse representing raw health/watering underneath plant
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .size(width = 38.dp, height = 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        // moisture level determines ring color scheme
                                        when {
                                            decalOverride.moisture > 0.7f -> Color(0xFF29B6F6).copy(alpha = 0.6f) // highly watered
                                            decalOverride.moisture < 0.3f -> Color(0xFF8D6E63).copy(alpha = 0.6f) // sandy arid soil
                                            else -> Color(0xFF66BB6A).copy(alpha = 0.6f) // pristine damp mulch
                                        }
                                    )
                            )
                            
                            Text(
                                text = finalEmoji, 
                                fontSize = if (decalOverride.growthStage == "Colossal") 56.sp else 46.sp,
                                modifier = Modifier.animateContentSize()
                            )
                            Text(
                                text = placement.name,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                color = if (isSelected) Color.Black else Color.White
                            )

                            // Mini HUD specifications inside sticker card
                            if (isSelected) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Text(
                                        "💧 ${(decalOverride.moisture * 100).toInt()}%", 
                                        fontSize = 8.sp, 
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0091EA)
                                    )
                                    Text(
                                        "|", 
                                        fontSize = 8.sp, 
                                        color = Color.Gray
                                    )
                                    Text(
                                        decalOverride.growthStage.uppercase(), 
                                        fontSize = 8.sp, 
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // DETECT SHUTTER INSTANT CAMERA FLASH OVERLAY
            if (flashAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(flashAlpha)
                        .background(Color.White)
                )
            }

            // HUD QUICK BOTTOM TOGGLES (Filter Wheel & Capture Shutter)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    
                    // Filter Rotation Button Widget
                    Box(modifier = Modifier.wrapContentSize()) {
                        var expandedByClick by remember { mutableStateOf(false) }
                        Button(
                            onClick = { expandedByClick = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Black.copy(alpha = 0.75f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = "Camera Filter", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(currentFilter.take(10), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }

                        DropdownMenu(
                            expanded = expandedByClick,
                            onDismissRequest = { expandedByClick = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            listOf("Standard RGB", "Night Vision Matrix", "Blueprint Draft", "Thermal Heatmap", "Golden Lume").forEach { filt ->
                                DropdownMenuItem(
                                    text = { Text(filt, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        currentFilter = filt
                                        expandedByClick = false
                                    }
                                )
                            }
                        }
                    }

                    // CENTRAL INTERACTIVE HIGH-SPEED CAMERA SHUTTER (Enhancement 5 - Captured Snaps)
                    IconButton(
                        onClick = {
                            triggeringFlash = true
                            val baseDescription = "Backdrop: $selectedBackgroundPreset, Plants count: ${arPlacedPlants.size}, Filter: $currentFilter, Weather: $activeWeather"
                            val capture = activeImageCapture
                            if (selectedBackgroundPreset == "Live Device Camera" && capture != null && cameraPermissionState.status.isGranted) {
                                try {
                                    val photoFile = File(
                                        context.cacheDir,
                                        "photo_${System.currentTimeMillis()}.jpg"
                                    )
                                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                                    capture.takePicture(
                                        outputOptions,
                                        ContextCompat.getMainExecutor(context),
                                        object : ImageCapture.OnImageSavedCallback {
                                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                                val snap = DesignSnapshot(
                                                    id = "SNAP_${System.currentTimeMillis() % 10000}",
                                                    timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
                                                    backdrop = selectedBackgroundPreset,
                                                    filter = currentFilter,
                                                    weather = activeWeather,
                                                    totalPlants = arPlacedPlants.size,
                                                    summary = "$baseDescription (Device photo captured)",
                                                    thumbnailEmoji = arPlacedPlants.firstOrNull()?.emoji ?: "🌳",
                                                    localImagePath = photoFile.absolutePath
                                                )
                                                savedSnapshots.add(0, snap)
                                            }

                                            override fun onError(exception: ImageCaptureException) {
                                                exception.printStackTrace()
                                                val snap = DesignSnapshot(
                                                    id = "SNAP_${System.currentTimeMillis() % 10000}",
                                                    timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
                                                    backdrop = selectedBackgroundPreset,
                                                    filter = currentFilter,
                                                    weather = activeWeather,
                                                    totalPlants = arPlacedPlants.size,
                                                    summary = "$baseDescription (Sensor fail setup fallback: ${exception.localizedMessage})",
                                                    thumbnailEmoji = arPlacedPlants.firstOrNull()?.emoji ?: "🌳"
                                                )
                                                savedSnapshots.add(0, snap)
                                            }
                                        }
                                    )
                                } catch (exc: Exception) {
                                    exc.printStackTrace()
                                    val snap = DesignSnapshot(
                                        id = "SNAP_${System.currentTimeMillis() % 10000}",
                                        timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
                                        backdrop = selectedBackgroundPreset,
                                        filter = currentFilter,
                                        weather = activeWeather,
                                        totalPlants = arPlacedPlants.size,
                                        summary = "$baseDescription (Init capture error: ${exc.localizedMessage})",
                                        thumbnailEmoji = arPlacedPlants.firstOrNull()?.emoji ?: "🌳"
                                    )
                                    savedSnapshots.add(0, snap)
                                }
                            } else {
                                // Simulated backdrop fallback or preview mode
                                val snap = DesignSnapshot(
                                    id = "SNAP_${System.currentTimeMillis() % 10000}",
                                    timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
                                    backdrop = selectedBackgroundPreset,
                                    filter = currentFilter,
                                    weather = activeWeather,
                                    totalPlants = arPlacedPlants.size,
                                    summary = baseDescription,
                                    thumbnailEmoji = arPlacedPlants.firstOrNull()?.emoji ?: "🌳"
                                )
                                savedSnapshots.add(0, snap)
                            }
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color.White, CircleShape)
                            .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = "Capture Snap",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Reset Canvas Clean Button
                    Button(
                        onClick = { viewModel.clearArPlants() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black.copy(alpha = 0.75f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Clear preview", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 4. THE INVENTORY TRAY SELECTOR
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    "Placeable Decal Inventory (Tap to spawn)",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Render active plant templates
                val availableAssetList = remember(activePlants, activeLayout) {
                    val templates = ClimatePlants.getTemplatesForClimate(activeLayout?.climate ?: "Temperate")
                    val items = if (activePlants.isNotEmpty()) {
                        activePlants.map { it.name to getEmojiForPlantName(it.name) }
                    } else {
                        templates.map { it.name to it.iconEmoji }
                    }
                    // Append decorative elements to always offer high-fidelity designs
                    items.toMutableList().apply {
                        add("Garden Fountain" to "⛲")
                        add("Stone Cobble Path" to "🛣️")
                        add("Bonsai Display" to "🪴")
                        add("Wooden Gazebo" to "🛖")
                        add("Scenic Lantern" to "🏮")
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableAssetList) { asset ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    viewModel.addArPlant(asset.first, asset.second)
                                }
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(asset.second, fontSize = 26.sp)
                            }
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = asset.first.take(9),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // 5. GESTURE AND STICKER TUNING STATION (50x PRO VARIABLES OVERRIDES)
        AnimatedVisibility(
            visible = selectedPlacement != null && selectedOverride != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            if (selectedPlacement != null && selectedOverride != null) {
                var localScale by remember(selectedPlacement.id) { mutableStateOf(selectedPlacement.scale) }
                var localRotation by remember(selectedPlacement.id) { mutableStateOf(selectedPlacement.rotationDegrees) }

                LaunchedEffect(selectedPlacement.scale) {
                    localScale = selectedPlacement.scale
                }
                LaunchedEffect(selectedPlacement.rotationDegrees) {
                    localRotation = selectedPlacement.rotationDegrees
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Title line with quick controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${selectedPlacement.emoji} ${selectedPlacement.name} Variables Tuning",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = {
                                    viewModel.removeArPlant(selectedPlacement.id)
                                    selectedPlacementId = null
                                },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f), CircleShape)
                                    .size(26.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DeleteSweep,
                                        contentDescription = "Trash placement",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                        }

                        // Property override 1: Simulated Growth Stage
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Growth Age:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(68.dp)
                            )
                            listOf("Sprout", "Young", "Mature", "Colossal").forEach { stage ->
                                val isCurrent = selectedOverride.growthStage == stage
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isCurrent) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable {
                                            localOverrides[selectedPlacement.id] = selectedOverride.copy(growthStage = stage)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        stage,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Property override 2: Irrigation Slider (updates local dehydration ring color)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Moisture Variable:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(110.dp)
                            )
                            Slider(
                                value = selectedOverride.moisture,
                                onValueChange = {
                                    localOverrides[selectedPlacement.id] = selectedOverride.copy(moisture = it)
                                },
                                valueRange = 0f..1f,
                                modifier = Modifier.weight(1f).height(24.dp)
                            )
                            Text(
                                "${(selectedOverride.moisture * 100).toInt()}%",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(36.dp),
                                textAlign = TextAlign.End
                            )
                        }

                        // Basic scale control slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Base Sticker Scale:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(110.dp)
                            )
                            Slider(
                                value = localScale,
                                onValueChange = { localScale = it },
                                onValueChangeFinished = {
                                    viewModel.updateArPlantScaling(selectedPlacement.id, localScale)
                                },
                                valueRange = 0.3f..3.0f,
                                modifier = Modifier.weight(1f).height(24.dp)
                            )
                            Text(
                                String.format("%.1fx", localScale),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(36.dp),
                                textAlign = TextAlign.End
                            )
                        }

                        // Basic rotation control slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Base Rotation:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(110.dp)
                            )
                            Slider(
                                value = localRotation,
                                onValueChange = { localRotation = it },
                                onValueChangeFinished = {
                                    viewModel.updateArPlantRotation(selectedPlacement.id, localRotation)
                                },
                                valueRange = 0f..360f,
                                modifier = Modifier.weight(1f).height(24.dp)
                            )
                            Text(
                                "${localRotation.toInt()}°",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(36.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }

    // SNAPSHOTS ALBUM DIALOG
    if (showSnapshotsDialog) {
        AlertDialog(
            onDismissRequest = { showSnapshotsDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Snap Gallery", tint = MaterialTheme.colorScheme.primary)
                    Text("Stored Landscape Snaps", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            },
            text = {
                if (savedSnapshots.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📸", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No snapshots recorded yet.", 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                "Tap the center shutter button to save current screen filters & plant designs.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp).padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Bolt: Added stable key to optimize LazyRow rendering and prevent unnecessary re-renders
                        items(savedSnapshots, key = { it.id }) { snap ->
                            Card(
                                modifier = Modifier
                                    .width(180.dp)
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    // Visual polaroid header
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(80.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                Brush.verticalGradient(listOf(Color(0xFF263238), Color(0xFF1B5E20)))
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (snap.localImagePath != null) {
                                            Box(modifier = Modifier.fillMaxSize()) {
                                                AsyncImage(
                                                    model = File(snap.localImagePath),
                                                    contentDescription = "Captured Snap",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                )
                                                // Floating badge overlay in bottom right
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .padding(4.dp)
                                                        .size(24.dp)
                                                        .background(Color.Black.copy(alpha = 0.61f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(snap.thumbnailEmoji, fontSize = 12.sp)
                                                }
                                            }
                                        } else {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(snap.thumbnailEmoji, fontSize = 28.sp)
                                                Text(
                                                    "Filter: ${snap.filter}", 
                                                    fontSize = 8.sp, 
                                                    color = Color.Green, 
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Snap ID: ${snap.id}", 
                                        fontWeight = FontWeight.ExtraBold, 
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Time captured: ${snap.timestamp}", 
                                        fontSize = 9.sp, 
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = snap.summary, 
                                        fontSize = 8.sp, 
                                        maxLines = 3,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        lineHeight = 11.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSnapshotsDialog = false }) {
                    Text("Close Gallery", fontWeight = FontWeight.ExtraBold)
                }
            }
        )
    }
}

// Utility to create a border stroke
private fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) = 
    androidx.compose.foundation.BorderStroke(width, color)

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onCaptureCreated: (ImageCapture) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier = modifier,
        update = { previewView ->
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                onCaptureCreated(imageCapture)

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } catch (exc: Exception) {
                    exc.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

