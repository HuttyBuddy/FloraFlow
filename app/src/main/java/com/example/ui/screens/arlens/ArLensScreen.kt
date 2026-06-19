package com.example.ui.screens.arlens

import io.github.sceneview.ar.ARSceneView
import com.google.ar.core.HitResult
import android.view.MotionEvent

import androidx.compose.animation.*
import java.util.Locale
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ClimatePlants
import com.example.ui.viewmodel.ArPlantPlacement
import com.example.ui.viewmodel.GardenViewModel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle

// Imports for Camera permission and CameraX integration
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

// Imports from the original package namespace
import com.example.ui.screens.checkPlantSynergy
import com.example.ui.screens.checkPlantConflict
import com.example.ui.screens.getEmojiForPlantName
import com.example.ui.screens.PremiumUpsellScreen

// Local state class for tracking enhanced property overrides per sticker locally
data class PlantExtraProps(
    val id: Int,
    val growthStage: String = "Mature", // Sprout 🌱, Young 🌿, Mature 🌸, Colossal 🌳
    val moisture: Float = 0.7f,        // 0f (Dry) to 1f (Wet)
    val highlightColor: Color? = null,  // Cyber highlights
    val vitalityCondition: String = "Perfect Hydration",
    val customLabel: String = ""
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
    val plantPlacements: List<Pair<String, androidx.compose.ui.geometry.Offset>> = emptyList()
)

@OptIn(ExperimentalAnimationApi::class, ExperimentalPermissionsApi::class, ExperimentalLayoutApi::class)
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

    val coroutineScope = rememberCoroutineScope()
    var generatingArImage by remember { mutableStateOf(false) }
    var generationProgress by remember { mutableFloatStateOf(0f) }
    var generationStatusText by remember { mutableStateOf("") }

    val activePlants by viewModel.activePlants.collectAsStateWithLifecycle()
    val activeLayout by viewModel.activeLayout.collectAsStateWithLifecycle()
    val arPlacedPlants by viewModel.arPlacedPlants.collectAsStateWithLifecycle()

    val synergyPairs = remember(arPlacedPlants) {
        val pairs = mutableListOf<Pair<Int, Int>>()
        for (i in arPlacedPlants.indices) {
            for (j in i + 1 until arPlacedPlants.size) {
                val p1 = arPlacedPlants[i]
                val p2 = arPlacedPlants[j]
                val dx = p1.positionX - p2.positionX
                val dy = p1.positionY - p2.positionY
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                if (dist <= 180f && checkPlantSynergy(p1.name, p2.name)) {
                    pairs.add(Pair(p1.id, p2.id))
                }
            }
        }
        pairs
    }

    val conflictPairs = remember(arPlacedPlants) {
        val pairs = mutableListOf<Pair<Int, Int>>()
        for (i in arPlacedPlants.indices) {
            for (j in i + 1 until arPlacedPlants.size) {
                val p1 = arPlacedPlants[i]
                val p2 = arPlacedPlants[j]
                val dx = p1.positionX - p2.positionX
                val dy = p1.positionY - p2.positionY
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                if (dist <= 180f && checkPlantConflict(p1.name, p2.name)) {
                    pairs.add(Pair(p1.id, p2.id))
                }
            }
        }
        pairs
    }

    // Bulletproof Emulator Detection
    val isEmulator = remember {
        val model = android.os.Build.MODEL.lowercase()
        val hardware = android.os.Build.HARDWARE.lowercase()
        val brand = android.os.Build.BRAND.lowercase()
        val device = android.os.Build.DEVICE.lowercase()
        val product = android.os.Build.PRODUCT.lowercase()
        val finger = android.os.Build.FINGERPRINT.lowercase()
        val board = android.os.Build.BOARD.lowercase()
        val manufacturer = android.os.Build.MANUFACTURER.lowercase()
        
        model.contains("emulator") ||
        model.contains("simulator") ||
        model.contains("virtual") ||
        model.contains("gphone") ||
        model.contains("vbox") ||
        model.contains("sdk") ||
        brand.contains("generic") ||
        brand.startsWith("generic") ||
        (brand.contains("google") && (device.startsWith("emu") || device.contains("gphone") || device.contains("pixel_gphone") || product.contains("emu") || product.contains("sdk"))) ||
        device.contains("generic") ||
        device.contains("emu") ||
        device.contains("vbox") ||
        device.contains("nox") ||
        product.contains("sdk_gphone") ||
        product.contains("gphone") ||
        product.contains("ranchu") ||
        product.contains("goldfish") ||
        product.contains("vbox") ||
        product.contains("simulator") ||
        product.contains("virtual") ||
        product.contains("sdk") ||
        finger.startsWith("generic") ||
        finger.startsWith("unknown") ||
        finger.contains("vbox") ||
        finger.contains("test-keys") ||
        hardware.contains("goldfish") ||
        hardware.contains("ranchu") ||
        hardware.contains("nox") ||
        hardware.contains("vbox") ||
        hardware.contains("cutf_cvm") ||
        hardware.contains("kvm") ||
        hardware.contains("virtio") ||
        hardware.contains("qemu") ||
        board.contains("goldfish") ||
        board.contains("ranchu") ||
        board.contains("vbox") ||
        board.contains("qemu") ||
        manufacturer.contains("genymotion") ||
        manufacturer.contains("nox") ||
        manufacturer.contains("vbox") ||
        manufacturer.contains("google_sdk") ||
        (manufacturer.contains("google") && (model.contains("gphone") || device.startsWith("emu") || product.contains("gphone") || product.contains("sdk")))
    }

    var viewportSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    // Camera Permission State
    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )

    // ENHANCED AR CONTROLS STATE
    var selectedPlacementId by remember { mutableStateOf<Int?>(null) }
    
    // Retraction states for full screen UI
    var isTuningExpanded by remember { mutableStateOf(true) }
    var activePanel by remember { mutableStateOf<String?>(null) } // null, "backdrop", "atmosphere", "filter", "inventory"
    
    // ENHANCEMENT 2: Dynamic Weather with real-time particle rendering loop
    var activeWeather by remember { mutableStateOf("Clear Sky") }
    var arFrame by remember { mutableStateOf<com.google.ar.core.Frame?>(null) }

    val isRunningInTest = remember {
        try {
            Class.forName("org.robolectric.Robolectric")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    var selectedBackgroundPreset by remember {
        mutableStateOf(
            if (isRunningInTest) "Simulated Live Yard"
            else "Live Camera"
        )
    }

    LaunchedEffect(selectedBackgroundPreset) {
        if (selectedBackgroundPreset == "Live Camera" && !cameraPermissionState.status.isGranted && !isRunningInTest) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    var useGyroscope by remember { mutableStateOf(!isEmulator) }

    val useVirtualBackdrop = remember(selectedBackgroundPreset) {
        selectedBackgroundPreset != "Live Camera" && selectedBackgroundPreset != "Simulated Live Yard"
    }

    // Theme backdrops
    val bgGradient = remember(selectedBackgroundPreset, activeWeather) {
        when (selectedBackgroundPreset) {
            "Sunny Patio" -> Brush.verticalGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFB74D), Color(0xFFD84315)))
            "Balcony Deck" -> Brush.verticalGradient(listOf(Color(0xFF90A4AE), Color(0xFF546E7A), Color(0xFF263238)))
            "Cyber Greenhouse" -> Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0A0F1D)))
            "English Estate" -> Brush.verticalGradient(listOf(Color(0xFF2E7D32), Color(0xFF1B5E20), Color(0xFF0D5316)))
            "Simulated Live Yard" -> {
                when (activeWeather) {
                    "Gentle Rain" -> Brush.verticalGradient(listOf(Color(0xFF37474F), Color(0xFF263238), Color(0xFF1A237E)))
                    "Blossom Shower" -> Brush.verticalGradient(listOf(Color(0xFFF8BBD0), Color(0xFFE1BEE7), Color(0xFFD1C4E9)))
                    "Fireflies Spark" -> Brush.verticalGradient(listOf(Color(0xFF0D1B2A), Color(0xFF1B263B), Color(0xFF000814)))
                    else -> Brush.verticalGradient(listOf(Color(0xFF29B6F6), Color(0xFF26A69A), Color(0xFF1B5E20))) // Clear Sky / sunny yard
                }
            }
            "Live Camera" -> Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
            else -> Brush.verticalGradient(listOf(Color(0xFF386641), Color(0xFF6A994E), Color(0xFFA7C957)))
        }
    }

    // Dynamic property overrides map helper
    val localOverrides = remember { mutableStateMapOf<Int, PlantExtraProps>() }

    val updateArOverride = { id: Int, transform: (PlantExtraProps) -> PlantExtraProps ->
        localOverrides[id] = transform(localOverrides[id] ?: PlantExtraProps(id = id))
    }

    val updateArPlantTransform = { id: Int, scale: Float, rotationDegrees: Float ->
        viewModel.updateArPlantScaling(id, scale)
        viewModel.updateArPlantRotation(id, rotationDegrees)
    }

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

    // Holographic laser sweep animation
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_sweep"
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
            "Blossom Shower" -> 25
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

    val headerBlock = @Composable {
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
                            "Tweak time, filters, and plant properties in 50x Holo Spatial Mode.",
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
    }

    val selectorsBlock = @Composable {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tracking Row (as seen in premium designs)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Tracking:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterVertically).padding(end = 4.dp)
                    )
                    
                    // Gyro Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (useGyroscope) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { useGyroscope = !useGyroscope }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            if (useGyroscope) "📡 Gyro ON" else "📴 Gyro OFF",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (useGyroscope) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Live Camera Button
                    val isLiveCameraSelected = selectedBackgroundPreset == "Live Camera" || selectedBackgroundPreset == "Simulated Live Yard"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isLiveCameraSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { 
                                selectedBackgroundPreset = if (isRunningInTest) "Simulated Live Yard" else "Live Camera"
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            "📷 Live Camera Active",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLiveCameraSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Use Backdrop Button
                    val isVirtualSelected = !isLiveCameraSelected
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isVirtualSelected) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { 
                                if (isLiveCameraSelected) {
                                    selectedBackgroundPreset = "Lawn Garden Grid"
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            "Use Backdrop",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isVirtualSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Atmosphere Row
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Atmosphere:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterVertically).padding(end = 4.dp)
                    )
                    listOf("Clear Sky", "Gentle Rain", "Blossom Shower", "Fireflies Spark").forEach { weather ->
                        val isSelected = activeWeather == weather
                        val icon = when (weather) {
                            "Gentle Rain" -> "🌧️"
                            "Blossom Shower" -> "🌸"
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

                // Backdrop Presets Row
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Yard Backdrop:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterVertically).padding(end = 4.dp)
                    )
                    listOf("Simulated Live Yard", "Lawn Garden Grid", "Sunny Patio", "Balcony Deck", "Cyber Greenhouse", "English Estate", "Live Camera").forEach { preset ->
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
        }
    }

    val viewportBlock = @Composable { viewportModifier: Modifier ->
        Box(
            modifier = viewportModifier
                .background(bgGradient)
                .onGloballyPositioned { viewportSize = it.size }
                .testTag("ar_viewport")
        ) {
            // Background Layer: Camera Preview or Simulated Live Yard
            val drawSimulatedYard = selectedBackgroundPreset == "Simulated Live Yard" || (selectedBackgroundPreset == "Live Camera" && isRunningInTest)
            if (selectedBackgroundPreset == "Live Camera" && !isRunningInTest) {
                if (cameraPermissionState.status.isGranted) {
                    ARSceneView(
                        modifier = Modifier.fillMaxSize().testTag("real_ar_lens_view"),
                        onSessionUpdated = { _, frame ->
                            arFrame = frame
                        },
                        onTouchEvent = { motionEvent, _ ->
                            if (motionEvent.action == android.view.MotionEvent.ACTION_UP) {
                                val frame = arFrame
                                if (frame != null) {
                                    val hits = frame.hitTest(motionEvent.x, motionEvent.y)
                                    val planeHit = hits.firstOrNull { hit ->
                                        hit.trackable is com.google.ar.core.Plane && (hit.trackable as com.google.ar.core.Plane).isPoseInPolygon(hit.hitPose)
                                    } ?: hits.firstOrNull()
                                    planeHit?.let { hit ->
                                        val pose = hit.hitPose

                                        val plantName: String
                                        val plantEmoji: String
                                        when (activeLayout?.style) {
                                            "Zen Garden" -> {
                                                plantName = "Bonsai Juniper"
                                                plantEmoji = "🪴"
                                            }
                                            else -> {
                                                plantName = "English Lavender"
                                                plantEmoji = "🪻"
                                            }
                                        }

                                        viewModel.addArPlant(
                                            name = plantName,
                                            emoji = plantEmoji,
                                            customX = pose.tx(),
                                            customY = pose.ty(),
                                            customZ = pose.tz()
                                        )
                                    }
                                }
                            }
                            true
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = "Camera Permission Required",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Camera Permission Required",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { cameraPermissionState.launchPermissionRequest() }
                            ) {
                                Text("Grant Permission")
                            }
                        }
                    }
                }
            } else if (drawSimulatedYard) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    clipRect {
                        val factor = climateTimeFactor
                        
                        // Sun or moon based on activeWeather
                        if (activeWeather == "Fireflies Spark") {
                            // Crescent moon
                            drawCircle(
                                color = Color(0xFFFFFDE7).copy(alpha = 0.15f),
                                radius = 45f,
                                center = Offset(size.width * 0.8f, size.height * 0.2f)
                            )
                            drawCircle(
                                color = Color(0xFFFFFDE7),
                                radius = 30f,
                                center = Offset(size.width * 0.8f, size.height * 0.2f)
                            )
                            // Moon bite shadow to form crescent
                            drawCircle(
                                color = Color(0xFF0D1B2A), // matches sky base dark blue color
                                radius = 28f,
                                center = Offset(size.width * 0.77f, size.height * 0.18f)
                            )
                        } else {
                            // Sun
                            val sunColor = when (activeWeather) {
                                "Gentle Rain" -> Color(0xFFECEFF1).copy(alpha = 0.6f)
                                "Blossom Shower" -> Color(0xFFFFD54F).copy(alpha = 0.8f)
                                else -> Color(0xFFFFEB3B) // Clear Sky
                            }
                            // Sun aura glow
                            drawCircle(
                                color = sunColor.copy(alpha = 0.25f),
                                radius = 70f + (sin(factor * 0.05f) * 5f),
                                center = Offset(size.width * 0.75f, size.height * 0.25f)
                            )
                            drawCircle(
                                color = sunColor,
                                radius = 40f,
                                center = Offset(size.width * 0.75f, size.height * 0.25f)
                            )
                        }

                        // Draw Cedar Fence
                        val fenceColor = when (activeWeather) {
                            "Gentle Rain" -> Color(0xFF3E2723) // Wet dark mahogany
                            "Blossom Shower" -> Color(0xFF8D6E63).copy(alpha = 0.7f) // Pale cedar
                            "Fireflies Spark" -> Color(0xFF1A120B) // Silhouetted fence
                            else -> Color(0xFFA1887F) // Warm cedar wood privacy fence
                        }
                        val fenceY = size.height * 0.6f
                        val fenceHeight = size.height * 0.25f
                        val plankWidth = 45f
                        val plankGap = 4f
                        
                        var currentPlankX = 0f
                        while (currentPlankX < size.width) {
                            drawRect(
                                color = fenceColor,
                                topLeft = Offset(currentPlankX, fenceY),
                                size = Size(plankWidth, fenceHeight)
                            )
                            // Draw fence plank tips
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(currentPlankX, fenceY)
                                lineTo(currentPlankX + plankWidth / 2, fenceY - 12f)
                                lineTo(currentPlankX + plankWidth, fenceY)
                                close()
                            }
                            drawPath(path = path, color = fenceColor)
                            currentPlankX += plankWidth + plankGap
                        }
                        
                        // Horizontal support beams
                        drawRect(
                            color = fenceColor.copy(alpha = 0.85f),
                            topLeft = Offset(0f, fenceY + fenceHeight * 0.2f),
                            size = Size(size.width, 12f)
                        )
                        drawRect(
                            color = fenceColor.copy(alpha = 0.85f),
                            topLeft = Offset(0f, fenceY + fenceHeight * 0.7f),
                            size = Size(size.width, 12f)
                        )

                        // Draw Grass / Lawn
                        val grassColor = when (activeWeather) {
                            "Gentle Rain" -> Color(0xFF1B5E20) // Deep wet-green
                            "Blossom Shower" -> Color(0xFF81C784).copy(alpha = 0.8f) // Pastel mossy
                            "Fireflies Spark" -> Color(0xFF0F2C11) // Silhouetted dark green
                            else -> Color(0xFF4CAF50) // Vibrant rich green
                        }
                        drawRect(
                            color = grassColor,
                            topLeft = Offset(0f, fenceY + fenceHeight),
                            size = Size(size.width, size.height - (fenceY + fenceHeight))
                        )

                        // Draw Bush Clusters with Flowers
                        val bushColor = when (activeWeather) {
                            "Gentle Rain" -> Color(0xFF0D5316)
                            "Blossom Shower" -> Color(0xFF4CAF50).copy(alpha = 0.75f)
                            "Fireflies Spark" -> Color(0xFF051C06)
                            else -> Color(0xFF2E7D32)
                        }
                        val bushY = fenceY + fenceHeight - 15f
                        val bushRadius1 = 55f
                        val bushRadius2 = 70f
                        val bushRadius3 = 50f
                        
                        // Bush cluster left
                        drawCircle(color = bushColor, radius = bushRadius1, center = Offset(size.width * 0.15f, bushY))
                        drawCircle(color = bushColor, radius = bushRadius2, center = Offset(size.width * 0.22f, bushY - 10f))
                        drawCircle(color = bushColor, radius = bushRadius3, center = Offset(size.width * 0.3f, bushY))

                        // Bush cluster right
                        drawCircle(color = bushColor, radius = bushRadius3, center = Offset(size.width * 0.7f, bushY))
                        drawCircle(color = bushColor, radius = bushRadius2, center = Offset(size.width * 0.78f, bushY - 15f))
                        drawCircle(color = bushColor, radius = bushRadius1, center = Offset(size.width * 0.85f, bushY))

                        // Decorative flower clusters
                        if (activeWeather != "Fireflies Spark") {
                            val flowerColor1 = Color(0xFFEC407A) // Pink
                            val flowerColor2 = Color(0xFFFFCA28) // Yellow
                            
                            // Left flowers
                            drawCircle(color = flowerColor1, radius = 5f, center = Offset(size.width * 0.15f, bushY - 15f))
                            drawCircle(color = flowerColor2, radius = 4f, center = Offset(size.width * 0.18f, bushY + 5f))
                            drawCircle(color = flowerColor1, radius = 6f, center = Offset(size.width * 0.23f, bushY - 25f))
                            drawCircle(color = flowerColor2, radius = 5f, center = Offset(size.width * 0.26f, bushY))
                            drawCircle(color = flowerColor1, radius = 4f, center = Offset(size.width * 0.29f, bushY - 10f))

                            // Right flowers
                            drawCircle(color = flowerColor2, radius = 5f, center = Offset(size.width * 0.72f, bushY - 5f))
                            drawCircle(color = flowerColor1, radius = 6f, center = Offset(size.width * 0.77f, bushY - 30f))
                            drawCircle(color = flowerColor2, radius = 4f, center = Offset(size.width * 0.81f, bushY - 10f))
                            drawCircle(color = flowerColor1, radius = 5f, center = Offset(size.width * 0.85f, bushY + 10f))
                        }
                    }
                }
            }
            
            // RENDERING BASE CLIMATE ATMOSPHERE GRAPHICS (Refactored to ArWeatherSystem helper)
            if (activeWeather != "Clear Sky" && particlesList.isNotEmpty()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    clipRect {
                        drawWeatherParticles(activeWeather, particlesList, climateTimeFactor)
                    }
                }
            }

            // BOTANICAL SYNERGY GRAPHICS (Refactored to ArHoloScanner helper)
            if (synergyPairs.isNotEmpty()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    clipRect {
                        val synergizedIds = synergyPairs.flatMap { listOf(it.first, it.second) }.toSet()
                        
                        // 1. Draw glowing green visual aura beneath synergized decals
                        arPlacedPlants.forEach { placement ->
                            if (synergizedIds.contains(placement.id)) {
                                val pulse = 0.85f + 0.15f * sin(climateTimeFactor * 0.2f + placement.id.hashCode() * 0.1f)
                                val auraCenter = Offset(placement.positionX + 75f, placement.positionY + 80f)
                                val visuals = getBotanicalVisuals(localOverrides[placement.id]?.moisture ?: 0.7f, localOverrides[placement.id]?.growthStage ?: "Mature")
                                val scale = placement.scale * visuals.scale
                                val radius = 100f * scale * pulse
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF00FF66).copy(alpha = 0.35f * pulse),
                                            Color(0xFF00FF66).copy(alpha = 0.08f * pulse),
                                            Color.Transparent
                                        ),
                                        center = auraCenter,
                                        radius = radius
                                    ),
                                    radius = radius,
                                    center = auraCenter
                                )
                            }
                        }

                        // 2. Draw pulsing neon-green laser connection lines with flowing yellow particle dots
                        synergyPairs.forEach { pair ->
                            val p1 = arPlacedPlants.firstOrNull { it.id == pair.first }
                            val p2 = arPlacedPlants.firstOrNull { it.id == pair.second }
                            if (p1 != null && p2 != null) {
                                val start = Offset(p1.positionX + 75f, p1.positionY + 80f)
                                val end = Offset(p2.positionX + 75f, p2.positionY + 80f)
                                drawVitalityFlow(start, end, climateTimeFactor, Color(0xFF00FF66))
                            }
                        }
                    }
                }
            }

            // BOTANICAL CONFLICT/INCOMPATIBILITY GRAPHICS
            if (conflictPairs.isNotEmpty()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    clipRect {
                        val conflictedIds = conflictPairs.flatMap { listOf(it.first, it.second) }.toSet()
                        
                        // 1. Draw glowing red visual aura beneath conflicted decals
                        arPlacedPlants.forEach { placement ->
                            if (conflictedIds.contains(placement.id)) {
                                val pulse = 0.85f + 0.15f * sin(climateTimeFactor * 0.2f + placement.id.hashCode() * 0.1f)
                                val auraCenter = Offset(placement.positionX + 75f, placement.positionY + 80f)
                                val visuals = getBotanicalVisuals(localOverrides[placement.id]?.moisture ?: 0.7f, localOverrides[placement.id]?.growthStage ?: "Mature")
                                val scale = placement.scale * visuals.scale
                                val radius = 100f * scale * pulse
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFF3333).copy(alpha = 0.35f * pulse),
                                            Color(0xFFFF3333).copy(alpha = 0.08f * pulse),
                                            Color.Transparent
                                        ),
                                        center = auraCenter,
                                        radius = radius
                                    ),
                                    radius = radius,
                                    center = auraCenter
                                )
                            }
                        }

                        // 2. Draw pulsing neon-red connection lines to indicate conflict
                        conflictPairs.forEach { pair ->
                            val p1 = arPlacedPlants.firstOrNull { it.id == pair.first }
                            val p2 = arPlacedPlants.firstOrNull { it.id == pair.second }
                            if (p1 != null && p2 != null) {
                                val start = Offset(p1.positionX + 75f, p1.positionY + 80f)
                                val end = Offset(p2.positionX + 75f, p2.positionY + 80f)
                                drawVitalityFlow(start, end, climateTimeFactor, Color(0xFFFF3333))
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

            // DYNAMIC PERSPECTIVE GROUND GRID LINES (Refactored to ArHoloScanner helper)
            if (currentFilter == "Blueprint Draft" || selectedBackgroundPreset == "Lawn Garden Grid") {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val lineColor = if (currentFilter == "Blueprint Draft") Color(0x6600E5FF) else Color(0x22FFFFFF)
                    drawPerspectiveGrid(lineColor, climateTimeFactor)
                }
            }

            // HUD SCREEN CALIBRATION OVERLAYS
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
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
                    
                    // Floating Level Bubble (Simulates Gyroscopic movement flawlessly if enabled)
                    Box(
                        modifier = Modifier
                            .offset {
                                if (useGyroscope) {
                                    IntOffset(hudDriftX.dp.roundToPx(), hudDriftY.dp.roundToPx())
                                } else {
                                    IntOffset(0, 0)
                                }
                            }
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


            }

            // THE SEAMLESS FLOATING PLACED DECAL CANVASES
            arPlacedPlants.forEach { placement ->
                key(placement.id) {
                    val isSelected = selectedPlacementId == placement.id
                    val currentPlacement by rememberUpdatedState(placement)
                    
                    // Get this decal's growth overrides and wetness
                    val decalOverride = localOverrides[placement.id] ?: PlantExtraProps(id = placement.id)

                    // Get visual overlays and filters using the new ArPlantVisuals helper
                    val visuals = getBotanicalVisuals(decalOverride.moisture, decalOverride.growthStage)

                    val finalEmoji = remember(placement.emoji, decalOverride.growthStage) {
                        if (decalOverride.growthStage == "Sprout") "🌱"
                        else if (decalOverride.growthStage == "Young") "🌿"
                        else if (decalOverride.growthStage == "Colossal") "🌳"
                        else placement.emoji
                    }

                    val finalScale = placement.scale * visuals.scale
                    
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    placement.positionX.toInt(),
                                    placement.positionY.toInt()
                                )
                            }
                            .pointerInput(placement.id) {
                                detectDragGestures(
                                    onDragStart = {
                                        selectedPlacementId = currentPlacement.id
                                        activePanel = null
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        viewModel.updateArPlantPosition(
                                            currentPlacement.id,
                                            dragAmount.x,
                                            dragAmount.y
                                        )
                                    }
                                )
                            }
                            .scale(finalScale)
                            .rotate(placement.rotationDegrees + visuals.rotationOffset)
                            .testTag("ar_placement_${placement.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        // Holographic Target Scanner Overlay (Refactored to ArHoloScanner helper)
                        if (isSelected) {
                            Canvas(modifier = Modifier.matchParentSize()) {
                                drawHoloReticle(
                                    center = Offset(size.width / 2f, size.height / 2f),
                                    radius = size.width * 0.7f,
                                    rotationAngle = radarSweepAngle * 1.5f,
                                    glowColor = Color(0xFF00FFCC)
                                )
                            }
                        }

                        ProceduralArPlant(
                            decalOverride = decalOverride,
                            emoji = finalEmoji,
                            name = placement.name,
                            isSelected = isSelected,
                            climateTimeFactor = climateTimeFactor,
                            onClick = {
                                selectedPlacementId = currentPlacement.id
                                activePanel = null
                            }
                        )
                    }
                }
            }

            // HIGH-TECH GARDEN INTELLIGENCE SCANNING HUD OVERLAY
            if (generatingArImage) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center
                ) {
                    // Moving horizontal laser scan line
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val scanY = maxHeight * laserProgress
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .offset(y = scanY)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF00FF66).copy(alpha = 0.1f),
                                            Color(0xFF00FF66),
                                            Color(0xFF00FF66).copy(alpha = 0.1f)
                                        )
                                    )
                                )
                        )
                    }

                    // Tech corner brackets / crosshairs
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val lineLen = 40f
                        val strokeW = 4f
                        val color = Color(0xFF00FF66).copy(alpha = 0.8f)

                        // Top-left
                        drawLine(color, Offset(20f, 20f), Offset(20f + lineLen, 20f), strokeW)
                        drawLine(color, Offset(20f, 20f), Offset(20f, 20f + lineLen), strokeW)

                        // Top-right
                        drawLine(color, Offset(w - 20f, 20f), Offset(w - 20f - lineLen, 20f), strokeW)
                        drawLine(color, Offset(w - 20f, 20f), Offset(w - 20f, 20f + lineLen), strokeW)

                        // Bottom-left
                        drawLine(color, Offset(20f, h - 20f), Offset(20f + lineLen, h - 20f), strokeW)
                        drawLine(color, Offset(20f, h - 20f), Offset(20f, h - 20f - lineLen), strokeW)

                        // Bottom-right
                        drawLine(color, Offset(w - 20f, h - 20f), Offset(w - 20f - lineLen, h - 20f), strokeW)
                        drawLine(color, Offset(w - 20f, h - 20f), Offset(w - 20f, h - 20f - lineLen), strokeW)
                    }

                    // Central Status Column
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        // Outer glowing rotating ring
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(140.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { generationProgress },
                                strokeWidth = 6.dp,
                                color = Color(0xFF00FF66),
                                trackColor = Color(0xFF00FF66).copy(alpha = 0.15f),
                                modifier = Modifier.fillMaxSize()
                            )
                            
                            // Pulse circle indicator
                            val pulseScale by animateFloatAsState(
                                targetValue = if (laserProgress > 0.5f) 1.05f else 0.95f,
                                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
                            )
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.scale(pulseScale)
                            ) {
                                // Monospace typeface using standard Kotlin font mapping
                                Text(
                                    text = "${(generationProgress * 100).toInt()}%",
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                                Text(
                                    text = "SYNTHESIZING",
                                    color = Color(0xFF00FF66),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // High-tech terminal status lines
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
                            border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00FF66))
                                    )
                                    Text(
                                        text = "NEURAL AI COMPOSITION",
                                        color = Color(0xFF00FF66),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(Color(0xFF00FF66).copy(alpha = 0.2f))
                                        .padding(vertical = 4.dp)
                                )
                                
                                Text(
                                    text = "⚡ STATUS: $generationStatusText",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    maxLines = 1
                                )
                                
                                Text(
                                    text = "📊 MODEL: FLORAFLOW-NEURAL-V4",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 8.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                                Text(
                                    text = "🌐 DEVICE CAMERA LINKED: SECURE",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 8.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
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

                    // CAMERA SHUTTER Button
                    IconButton(
                        onClick = {
                            if (!generatingArImage) {
                                coroutineScope.launch {
                                    generatingArImage = true
                                    generationProgress = 0f
                                    generationStatusText = "Initializing Neural AR Engine..."
                                    
                                    delay(300)
                                    generationProgress = 0.25f
                                    generationStatusText = "Analyzing Spatial Depth & Geometry..."
                                    
                                    delay(300)
                                    generationProgress = 0.50f
                                    generationStatusText = "Mapping Light Angles & Shadows..."
                                    
                                    delay(300)
                                    generationProgress = 0.75f
                                    generationStatusText = "Synthesizing Companion Synergy Aesthetics..."
                                    
                                    delay(300)
                                    generationProgress = 0.90f
                                    generationStatusText = "Rendering Final Neural AR Image..."
                                    
                                    delay(300)
                                    generationProgress = 1.0f
                                    generationStatusText = "AR Snapshot Generated!"
                                    
                                    // Trigger camera flash
                                    triggeringFlash = true
                                    
                                    // Capture coordinates normalized by viewport size
                                    val w = if (viewportSize.width > 0) viewportSize.width.toFloat() else 1f
                                    val h = if (viewportSize.height > 0) viewportSize.height.toFloat() else 1f
                                    val placements = arPlacedPlants.map {
                                        Pair(it.emoji, Offset(it.positionX / w, it.positionY / h))
                                    }
                                    val description = "Backdrop: $selectedBackgroundPreset, Plants count: ${arPlacedPlants.size}, Filter: $currentFilter, Weather: $activeWeather"
                                    val snap = DesignSnapshot(
                                        id = "SNAP_${System.currentTimeMillis() % 10000}",
                                        timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
                                        backdrop = selectedBackgroundPreset,
                                        filter = currentFilter,
                                        weather = activeWeather,
                                        totalPlants = arPlacedPlants.size,
                                        summary = description,
                                        thumbnailEmoji = arPlacedPlants.firstOrNull()?.emoji ?: "🌳",
                                        plantPlacements = placements
                                    )
                                    savedSnapshots.add(0, snap)
                                    
                                    delay(200)
                                    generatingArImage = false
                                }
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
    }

    val inventoryBlock = @Composable { isLandscapeGrid: Boolean ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Placeable Decal Inventory (Tap to spawn)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    IconButton(
                        onClick = { activePanel = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Collapse Inventory",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

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

                if (isLandscapeGrid) {
                    val chunkedAssets = remember(availableAssetList) { availableAssetList.chunked(5) }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        chunkedAssets.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { asset ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                val count = arPlacedPlants.size
                                                val centerX = if (viewportSize.width > 0) viewportSize.width / 2f else 300f
                                                val centerY = if (viewportSize.height > 0) viewportSize.height / 2f else 200f
                                                val staggerX = ((count % 3) - 1) * 80f
                                                val staggerY = ((count / 3) % 3 - 1) * 80f
                                                val finalX = centerX + staggerX - 100f
                                                val finalY = centerY + staggerY - 100f
                                                viewModel.addArPlant(asset.first, asset.second, finalX, finalY)
                                            }
                                            .padding(2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .aspectRatio(1f)
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(asset.second, fontSize = 24.sp)
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = asset.first.take(9),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                if (rowItems.size < 5) {
                                    repeat(5 - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableAssetList) { asset ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        val count = arPlacedPlants.size
                                        val centerX = if (viewportSize.width > 0) viewportSize.width / 2f else 300f
                                        val centerY = if (viewportSize.height > 0) viewportSize.height / 2f else 200f
                                        val staggerX = ((count % 3) - 1) * 80f
                                        val staggerY = ((count / 3) % 3 - 1) * 80f
                                        val finalX = centerX + staggerX - 100f
                                        val finalY = centerY + staggerY - 100f
                                        viewModel.addArPlant(asset.first, asset.second, finalX, finalY)
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
        }
    }

    val tuningBlock = @Composable {
        AnimatedVisibility(
            visible = selectedPlacement != null && selectedOverride != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            if (selectedPlacement != null && selectedOverride != null) {
                var localScale by remember(selectedPlacement.id) { mutableFloatStateOf(selectedPlacement.scale) }
                var localRotation by remember(selectedPlacement.id) { mutableFloatStateOf(selectedPlacement.rotationDegrees) }

                LaunchedEffect(selectedPlacement.scale) {
                    localScale = selectedPlacement.scale
                }
                LaunchedEffect(selectedPlacement.rotationDegrees) {
                    localRotation = selectedPlacement.rotationDegrees
                }

                if (isTuningExpanded) {
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Collapse button
                                    IconButton(
                                        onClick = { isTuningExpanded = false },
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                            .size(26.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Collapse Tuning",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
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
                                                updateArOverride(selectedPlacement.id) {
                                                    it.copy(growthStage = stage)
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = stage,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Property override 2: Moisture slider (Enhancement 2)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Moisture:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(68.dp)
                                )
                                Slider(
                                    value = selectedOverride.moisture,
                                    onValueChange = { newMoisture ->
                                        updateArOverride(selectedPlacement.id) {
                                            it.copy(moisture = newMoisture)
                                        }
                                    },
                                    valueRange = 0f..1f,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${(selectedOverride.moisture * 100).toInt()}%",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.width(32.dp)
                                )
                            }

                            // Property override 3: Custom label override
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Tag Note:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(68.dp)
                                )
                                BasicTextField(
                                    value = selectedOverride.customLabel,
                                    onValueChange = { newLabel ->
                                        updateArOverride(selectedPlacement.id) {
                                            it.copy(customLabel = newLabel)
                                        }
                                    },
                                    textStyle = TextStyle(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }

                            // Scale slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Scaling:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(68.dp)
                                )
                                Slider(
                                    value = localScale,
                                    onValueChange = {
                                        localScale = it
                                        updateArPlantTransform(selectedPlacement.id, it, localRotation)
                                    },
                                    valueRange = 0.3f..3.0f,
                                    modifier = Modifier.weight(1f)
                                )
                                val locale = LocalConfiguration.current.locales[0]
                                Text(
                                    text = String.format(locale, "%.1fx", localScale),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.width(32.dp)
                                )
                            }

                            // Rotation slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Rotation:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(68.dp)
                                )
                                Slider(
                                    value = localRotation,
                                    onValueChange = {
                                        localRotation = it
                                        updateArPlantTransform(selectedPlacement.id, localScale, it)
                                    },
                                    valueRange = 0f..360f,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${localRotation.toInt()}°",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.width(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            val activeSynergies = synergyPairs
                                .filter { it.first == selectedPlacement.id || it.second == selectedPlacement.id }
                                .map { pair ->
                                    val otherId = if (pair.first == selectedPlacement.id) pair.second else pair.first
                                    arPlacedPlants.firstOrNull { it.id == otherId }
                                }
                                .filterNotNull()

                        if (activeSynergies.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFE8F5E9).copy(alpha = 0.15f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("✨", fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "ACTIVE BOTANICAL SYNERGY",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 9.sp,
                                                color = Color(0xFF00FF66),
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF00FF66).copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                "PROXIMITY SYNERGY MATCH",
                                                fontSize = 7.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF00FF66)
                                            )
                                        }
                                    }
                                    
                                    Text(
                                        text = "Connecting vectors indicate perfect companion proximity (<180px) and compatibility.",
                                        fontSize = 9.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                    
                                    activeSynergies.forEach { companion ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                                .padding(6.dp)
                                        ) {
                                            Text(
                                                text = getEmojiForPlantName(companion.name),
                                                fontSize = 18.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = companion.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp,
                                                    color = Color.White
                                                )
                                                val synergyEffect = when {
                                                    selectedPlacement.name.lowercase().contains("rose") -> "Provides natural scent and attracts pollinators"
                                                    selectedPlacement.name.lowercase().contains("lavender") -> "Repels mosquitoes and flies, boosts beneficial insect activity"
                                                    selectedPlacement.name.lowercase().contains("marigold") -> "Deters nematodes and garden pests with root secretions"
                                                    selectedPlacement.name.lowercase().contains("ivy") -> "Enhances vertical space utilization and soil ground cover"
                                                    else -> "Improves soil biodiversity and nutrient extraction synergy"
                                                }
                                                Text(
                                                    text = "Effect: $synergyEffect",
                                                    fontSize = 8.sp,
                                                    color = Color(0xFF00FF66).copy(alpha = 0.9f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            val recommendations = getCompanionRecommendations(selectedPlacement.name)
                            if (recommendations.isNotEmpty()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFE0F7FA).copy(alpha = 0.1f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("💡", fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "COMPANION RECOMMENDATION",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 9.sp,
                                                color = Color(0xFF00E5FF),
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                        
                                        Text(
                                            text = "Place these companion plants within 180 pixels to trigger active companion synergies:",
                                            fontSize = 9.sp,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                        
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            recommendations.forEach { recName ->
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = getEmojiForPlantName(recName),
                                                        fontSize = 12.sp
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = recName,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 8.sp,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Card(
                        modifier = Modifier
                            .wrapContentSize()
                            .clickable { isTuningExpanded = true },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "⚙️ Tweak: ${selectedPlacement.emoji} ${selectedPlacement.name}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Expand Tuning",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Root Viewport occupies the entire edge-to-edge area!
        viewportBlock(Modifier.fillMaxSize())

        // 2. Sleek Minimalist Top HUD Bar
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(14.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00FF66))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FLORAFLOW HOLOGRAPHIC HUD",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "TRACKING: ${if (useGyroscope) "GYRO" else "STATIC"} | WEATHER: ${activeWeather.uppercase()}",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // 3. Floating Right-side Collapsible Options Panel
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Backdrop Presets selector trigger
            FloatingActionButton(
                onClick = {
                    activePanel = if (activePanel == "backdrop") null else "backdrop"
                    selectedPlacementId = null
                },
                containerColor = if (activePanel == "backdrop") MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.7f),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(46.dp)
            ) {
                Text("🌄", fontSize = 18.sp)
            }

            // Weather/Atmosphere selector trigger
            FloatingActionButton(
                onClick = {
                    activePanel = if (activePanel == "atmosphere") null else "atmosphere"
                    selectedPlacementId = null
                },
                containerColor = if (activePanel == "atmosphere") MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.7f),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(46.dp)
            ) {
                Text("⛈️", fontSize = 18.sp)
            }

            // Camera Filter selector trigger
            FloatingActionButton(
                onClick = {
                    activePanel = if (activePanel == "filter") null else "filter"
                    selectedPlacementId = null
                },
                containerColor = if (activePanel == "filter") MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.7f),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(46.dp)
            ) {
                Text("🎨", fontSize = 18.sp)
            }

            // Inventory / Add Decal selector trigger
            FloatingActionButton(
                onClick = {
                    activePanel = if (activePanel == "inventory") null else "inventory"
                    selectedPlacementId = null
                },
                containerColor = if (activePanel == "inventory") MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.7f),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(46.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Inventory", tint = Color.White)
            }

            // Gyroscope toggle button
            FloatingActionButton(
                onClick = { useGyroscope = !useGyroscope },
                containerColor = if (useGyroscope) MaterialTheme.colorScheme.secondary else Color.Black.copy(alpha = 0.7f),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(46.dp)
            ) {
                Text(if (useGyroscope) "📡" else "📴", fontSize = 18.sp)
            }

            // Snapshots Gallery button
            FloatingActionButton(
                onClick = { showSnapshotsDialog = true },
                containerColor = Color.Black.copy(alpha = 0.7f),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(46.dp)
            ) {
                BadgedBox(
                    badge = {
                        if (savedSnapshots.isNotEmpty()) {
                            Badge(containerColor = MaterialTheme.colorScheme.error) {
                                Text("${savedSnapshots.size}", color = Color.White, fontSize = 9.sp)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Stored Snaps",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 4. Floating Bottom Collapsible Sheet Container & Action Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(if (isLandscape) 0.65f else 1.0f)
                .padding(horizontal = 14.dp)
                .padding(bottom = 16.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Collapsible active selection parameters configuration panel
            if (selectedPlacement != null && selectedOverride != null) {
                tuningBlock()
            }

            // Collapsible assets inventory selection panel
            if (activePanel == "inventory") {
                inventoryBlock(isLandscape)
            }

            // Collapsible backdrop preset selection panel
            if (activePanel == "backdrop") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🌄 Select Backdrop Preset", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = { activePanel = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Simulated Live Yard", "Lawn Garden Grid", "Sunny Patio", "Balcony Deck", "Cyber Greenhouse", "English Estate", "Live Camera").forEach { preset ->
                                val isSelected = selectedBackgroundPreset == preset
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { selectedBackgroundPreset = preset }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(preset, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // Collapsible weather climate parameter selection panel
            if (activePanel == "atmosphere") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⛈️ Environmental Atmosphere", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = { activePanel = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Clear Sky", "Gentle Rain", "Blossom Shower", "Fireflies Spark").forEach { weather ->
                                val isSelected = activeWeather == weather
                                val icon = when (weather) {
                                    "Gentle Rain" -> "🌧️"
                                    "Blossom Shower" -> "🌸"
                                    "Fireflies Spark" -> "✨"
                                    else -> "☀️"
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { activeWeather = weather }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text("$icon $weather", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // Collapsible camera view overlay HUD filter parameter selection panel
            if (activePanel == "filter") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎨 Camera HUD Filters", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = { activePanel = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Standard RGB", "Night Vision Matrix", "Blueprint Draft", "Thermal Heatmap", "Golden Lume").forEach { filt ->
                                val isSelected = currentFilter == filt
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { currentFilter = filt }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(filt, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
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
                        items(savedSnapshots) { snap ->
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
                                             )
                                     ) {
                                         if (snap.plantPlacements.isEmpty()) {
                                             Box(
                                                 modifier = Modifier.fillMaxSize(),
                                                 contentAlignment = Alignment.Center
                                             ) {
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
                                         } else {
                                             // Render custom relative coordinates
                                             BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                                 val w = maxWidth
                                                 val h = maxHeight
                                                 snap.plantPlacements.forEach { (emoji, offset) ->
                                                     val xDp = w * offset.x
                                                     val yDp = h * offset.y
                                                     Text(
                                                         text = emoji,
                                                         fontSize = 14.sp,
                                                         modifier = Modifier.offset(
                                                             x = xDp - 8.dp,
                                                             y = yDp - 8.dp
                                                         )
                                                     )
                                                 }
                                                 // Overlay filter text at bottom
                                                 Text(
                                                     text = snap.filter,
                                                     fontSize = 7.sp,
                                                     color = Color.White.copy(alpha = 0.9f),
                                                     fontWeight = FontWeight.Bold,
                                                     modifier = Modifier
                                                         .align(Alignment.BottomCenter)
                                                         .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                         .padding(horizontal = 6.dp, vertical = 2.dp)
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
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
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
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (exc: Exception) {
                    android.util.Log.e("ArLensScreen", "Use camera binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

private fun getCompanionRecommendations(plantName: String): List<String> {
    val name = plantName.lowercase()
    val companions = mutableListOf<String>()
    if (name.contains("rose") && !name.contains("rosemary")) {
        companions.add("Maple Tree")
        companions.add("Lavender")
    }
    if (name.contains("maple")) {
        companions.add("Rose Bush")
    }
    if (name.contains("lavender")) {
        companions.add("Rose Bush")
        companions.add("Bonsai Juniper")
        companions.add("Rosemary")
    }
    if (name.contains("juniper")) {
        companions.add("Lavender")
    }
    if (name.contains("cactus") || name.contains("aloe")) {
        if (name.contains("cactus")) companions.add("Aloe Vera")
        if (name.contains("aloe")) companions.add("Cactus")
    }
    if (name.contains("marigold")) {
        companions.add("English Ivy")
    }
    if (name.contains("ivy")) {
        companions.add("Marigold")
        companions.add("Golden Pothos")
    }
    if (name.contains("pothos")) {
        companions.add("English Ivy")
    }
    if (name.contains("aster")) {
        companions.add("Thyme")
    }
    if (name.contains("thyme")) {
        companions.add("Aster")
        companions.add("Rosemary")
    }
    if (name.contains("columbine")) {
        companions.add("Fern")
    }
    if (name.contains("fern")) {
        companions.add("Columbine")
    }
    if (name.contains("rosemary")) {
        companions.add("Lavender")
        companions.add("Thyme")
    }
    return companions
}
