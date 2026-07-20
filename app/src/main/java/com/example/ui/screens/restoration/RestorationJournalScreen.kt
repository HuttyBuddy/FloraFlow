package com.example.ui.screens.restoration

import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.GardenViewModel
import com.example.ui.theme.extendedColors
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.draw.blur
import com.example.data.model.parseGridString
import com.example.data.model.GridPlantItem
import com.example.ui.screens.checkPlantSynergy
import kotlin.math.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RestorationJournalScreen(
    viewModel: GardenViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Permissions for Android 13+ Notifications
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notificationPermission = rememberPermissionState(
            permission = android.Manifest.permission.POST_NOTIFICATIONS
        )
        LaunchedEffect(Unit) {
            if (!notificationPermission.status.isGranted) {
                notificationPermission.launchPermissionRequest()
            }
        }
    }

    // Bind to Soundscape Service
    DisposableEffect(Unit) {
        viewModel.bindSoundscapeService()
        onDispose {
            viewModel.unbindSoundscapeService()
        }
    }

    // Active state flows from ViewModel
    val activeLayout by viewModel.activeLayout.collectAsStateWithLifecycle()
    val activePlants by viewModel.activePlants.collectAsStateWithLifecycle()
    val nriScore by viewModel.neuralRestorationIndex.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isSoundscapePlaying.collectAsStateWithLifecycle()
    val currentTrack by viewModel.currentSoundscapeTrack.collectAsStateWithLifecycle()
    val ambientVol by viewModel.ambientVolume.collectAsStateWithLifecycle()
    val binauralVol by viewModel.binauralVolume.collectAsStateWithLifecycle()
    val baseFreq by viewModel.baseFrequency.collectAsStateWithLifecycle()
    val diffFreq by viewModel.diffFrequency.collectAsStateWithLifecycle()
    val restorationLogs by viewModel.allRestorationLogs.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val restorationTrialCount by viewModel.restorationTrialCount.collectAsStateWithLifecycle()
    val sleepTimerEndTime by viewModel.sleepTimerEndTime.collectAsStateWithLifecycle()
    val pendingCareTasks by viewModel.pendingCareTasks.collectAsStateWithLifecycle()

    // Biophilic scores calculation for breakdown view
    val uniqueTypes = remember(activePlants) {
        activePlants.map { it.type.lowercase().trim() }.distinct()
    }
    val diversityScore = remember(uniqueTypes) {
        (uniqueTypes.size * 10).coerceAtMost(40)
    }

    val gridItems = remember(activeLayout) {
        activeLayout?.let { parseGridString(it.gridString) } ?: emptyList()
    }
    val synergyScore = remember(gridItems) {
        var synergyCount = 0
        for (i in gridItems.indices) {
            for (j in i + 1 until gridItems.size) {
                val item1 = gridItems[i]
                val item2 = gridItems[j]
                val dx = item1.x - item2.x
                val dy = item1.y - item2.y
                // Optimized: avoided expensive sqrt() for distance comparison (dist <= 1.5 -> distSq <= 2.25)
                val distSq = dx * dx + dy * dy
                if (distSq <= 2.25f && checkPlantSynergy(item1.plantName, item2.plantName)) {
                    synergyCount++
                }
            }
        }
        (synergyCount * 8).coerceAtMost(40)
    }

    val avgGrowth = remember(activePlants) {
        if (activePlants.isNotEmpty()) activePlants.map { it.growthProgress }.average() else 0.0
    }
    val careMultiplier = remember(avgGrowth) {
        0.8 + (if (activePlants.isNotEmpty()) (avgGrowth / 100.0) * 0.4 else 0.2)
    }

    val layoutPlantIds = remember(activePlants) {
        activePlants.map { it.id }.toSet()
    }
    val overdueTasksCount = remember(pendingCareTasks, layoutPlantIds) {
        pendingCareTasks.filter { it.plantId in layoutPlantIds && it.dueDate < System.currentTimeMillis() && it.completedDate == null }.size
    }
    val penaltyScore = remember(overdueTasksCount) {
        overdueTasksCount * 5
    }

    // Local checklist state
    val availableTasks = remember(activePlants) {
        val tasks = mutableListOf(
            "🫁 Practice 4-7-8 rhythmic breathing (inhale 4s, hold 7s, exhale 8s)",
            "🍃 Close your eyes and focus on the natural breeze or ambient wind chimes"
        )
        val plantNames = activePlants.map { it.name.lowercase() }
        if (plantNames.any { it.contains("lavender") }) {
            tasks.add("🌿 Inhale your Lavender's sweet aroma to trigger a natural sense of calm")
        }
        if (plantNames.any { it.contains("rose") }) {
            tasks.add("🌹 Trace a Rose petal's geometry slowly to gently rest your eyes and focus")
        }
        if (plantNames.any { it.contains("fern") || it.contains("ivy") }) {
            tasks.add("🌿 Visual scanning: Follow the veins of a fern leaf from base to tip")
        }
        if (plantNames.any { it.contains("mint") || it.contains("rosemary") }) {
            tasks.add("🌱 Rub a mint leaf gently between your fingers to release sensory essential oils")
        }
        tasks
    }

    var completedTasksList = remember { mutableStateListOf<String>() }

    // Soundscape tracks definition — each pairs a brainwave preset with its own living nature scene
    val tracks = listOf(
        SoundscapeTrackInfo("Alpha Focus", "Forest Breeze 🍃", 200f, 10f, "A living forest breeze with hand-struck wind chimes, under Alpha waves (10Hz) for alert, relaxed focus. Generated live — it never loops or repeats."),
        SoundscapeTrackInfo("Theta Meditate", "Gentle Rain 🌧️", 200f, 6f, "Soft rainfall with droplets scattered around you, under Theta waves (6Hz) for deep visualization and mental stillness. Generated live — it never loops or repeats."),
        SoundscapeTrackInfo("Delta Sleep", "Ocean Waves 🌊", 150f, 2.5f, "Slow ocean swells breaking in the distance, under Delta waves (2.5Hz) for physical healing and deep sleep. Generated live — it never loops or repeats.")
    )

    // Base background gradient
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F261D), // Dark forest green
                        Color(0xFF07120D)  // Almost black
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Screen title
            Text(
                text = "Restoration Journal",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFA8E6CF),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Eco-Acoustics & Sensory Mindfulness",
                fontSize = 13.sp,
                color = Color(0xFF81C784).copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (!isPremium) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (restorationTrialCount < 3) Color(0xFFFFF3E0).copy(alpha = 0.15f) else Color(0xFFFFEBEE).copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (restorationTrialCount < 3) Color(0xFFFFB74D) else Color(0xFFE57373)
                    ),
                    modifier = Modifier.testTag("restoration_trial_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (restorationTrialCount < 3) Icons.Default.Info else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (restorationTrialCount < 3) Color(0xFFFFB74D) else Color(0xFFE57373),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (restorationTrialCount < 3) "Free Trial: ${3 - restorationTrialCount}/3 plays remaining" else "Trial Exhausted (3/3) - Go PRO 👑",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (restorationTrialCount < 3) Color(0xFFFFB74D) else Color(0xFFE57373)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // NRI Index Gauge or Onboarding state
            if (activeLayout == null || activePlants.isEmpty()) {
                // Empty state card
                EmptyStateCard()
            } else {
                // NRI Progress gauge card
                NriGaugeCard(
                    nriScore = nriScore,
                    diversityScore = diversityScore,
                    synergyScore = synergyScore,
                    careMultiplier = careMultiplier,
                    penaltyScore = penaltyScore,
                    plantCount = activePlants.size
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                Column {
                    // Soundscape Player Controller
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Eco-Acoustic Soundscapes",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA8E6CF),
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Text(
                                text = "Living nature scenes generated in real time, layered with binaural brainwave tones",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .align(Alignment.Start)
                                    .padding(bottom = 16.dp)
                            )

                            // Track selector tabs
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F261D).copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                tracks.forEach { track ->
                                    val isSelected = currentTrack == track.name
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0xFF81C784).copy(alpha = 0.25f) else Color.Transparent)
                                            .clickable {
                                                viewModel.changeSoundscapeTrack(track.name, track.baseFreq, track.diffFreq)
                                                Toast.makeText(context, "Drifting into ${track.scene}", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = track.name.split(" ")[0], // Alpha, Theta, Delta
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color(0xFFA8E6CF) else Color.White.copy(alpha = 0.7f)
                                            )
                                            Text(
                                                text = track.scene,
                                                fontSize = 11.sp,
                                                color = if (isSelected) Color(0xFF81C784) else Color.White.copy(alpha = 0.72f)
                                            )
                                        }
                                    }
                                }
                            }

                            // Track Description
                            val currentTrackDesc = tracks.find { it.name == currentTrack }?.description ?: ""
                            Text(
                                text = currentTrackDesc,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.78f),
                                lineHeight = 17.sp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )

                            Divider(color = Color(0xFF81C784).copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

                            // Dual Vol Sliders
                            // 1. Ambient Vol
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("🍃 Ambient nature", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                                    Text("${(ambientVol * 100).toInt()}%", fontSize = 12.sp, color = Color(0xFFA8E6CF))
                                }
                                Slider(
                                    value = ambientVol,
                                    onValueChange = { viewModel.updateAmbientVolume(it) },
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF81C784),
                                        activeTrackColor = Color(0xFF81C784),
                                        inactiveTrackColor = Color(0xFF0F261D)
                                    )
                                )
                            }

                            // 2. Binaural Vol
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("🧠 Binaural brainwaves", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                                    Text("${(binauralVol * 100).toInt()}%", fontSize = 12.sp, color = Color(0xFFA8E6CF))
                                }
                                Slider(
                                    value = binauralVol,
                                    onValueChange = { viewModel.updateBinauralVolume(it) },
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF81C784),
                                        activeTrackColor = Color(0xFF81C784),
                                        inactiveTrackColor = Color(0xFF0F261D)
                                    )
                                )
                            }

                            Divider(color = Color(0xFF81C784).copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

                            // 3. Carrier/Base Frequency Slider
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("🧠 Carrier Frequency", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                                    Text("${baseFreq.toInt()} Hz", fontSize = 12.sp, color = Color(0xFFA8E6CF))
                                }
                                Slider(
                                    value = baseFreq,
                                    onValueChange = { viewModel.updateFrequencies(it, diffFreq) },
                                    valueRange = 100f..500f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF81C784),
                                        activeTrackColor = Color(0xFF81C784),
                                        inactiveTrackColor = Color(0xFF0F261D)
                                    )
                                )
                            }

                            // 4. Brainwave/Diff Frequency Slider
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                val brainwaveState = when {
                                    diffFreq < 4f -> "Delta (Sleep/Heal)"
                                    diffFreq < 8f -> "Theta (Meditation)"
                                    diffFreq < 12f -> "Alpha (Focus)"
                                    else -> "Beta (Active/Alert)"
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("🌀 Brainwave Frequency ($brainwaveState)", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                                    Text(String.format(java.util.Locale.US, "%.1f Hz", diffFreq), fontSize = 12.sp, color = Color(0xFFA8E6CF))
                                }
                                Slider(
                                    value = diffFreq,
                                    onValueChange = { viewModel.updateFrequencies(baseFreq, it) },
                                    valueRange = 0.5f..20f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF81C784),
                                        activeTrackColor = Color(0xFF81C784),
                                        inactiveTrackColor = Color(0xFF0F261D)
                                    )
                                )
                            }

                            // Interactive Audio Waveform Visualizer
                            AudioWaveformVisualizer(
                                isPlaying = isPlaying,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .padding(vertical = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Play/Pause button with custom gradient and shadow/glow
                            Button(
                                onClick = { viewModel.toggleSoundscapePlay() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .width(220.dp)
                                    .height(48.dp)
                                    .background(
                                        brush = if (isPlaying) {
                                            Brush.horizontalGradient(colors = listOf(Color(0xFFE57373), Color(0xFFC62828)))
                                        } else {
                                            Brush.horizontalGradient(colors = listOf(Color(0xFF81C784), Color(0xFF2E7D32)))
                                        },
                                        shape = RoundedCornerShape(24.dp)
                                    )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isPlaying) "Pause Soundscape" else "Play Soundscape",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            // Sleep timer — session ends with a long, gentle 15s fade
                            Spacer(modifier = Modifier.height(14.dp))
                            SleepTimerRow(
                                sleepTimerEndTime = sleepTimerEndTime,
                                isPlaying = isPlaying,
                                onSetTimer = { minutes -> viewModel.setSleepTimer(minutes) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Daily Reflection
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E352F).copy(alpha = 0.7f)),
                        border = BorderStroke(1.dp, Color(0xFF81C784).copy(alpha = 0.15f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "📝 Daily Reflection",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA8E6CF)
                            )
                            Text(
                                text = "How did your space feel today?",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                                    
                            TextField(
                                value = viewModel.dailyReflection.collectAsState().value,
                                onValueChange = { viewModel.updateDailyReflection(it) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text("Write your thoughts...", color = Color.White.copy(alpha = 0.3f))
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF0F261D).copy(alpha = 0.3f),
                                    unfocusedContainerColor = Color(0xFF0F261D).copy(alpha = 0.3f),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                singleLine = false
                            )
                                    
                            Spacer(modifier = Modifier.height(16.dp))
                                    
                            Text("Select your mood:", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(top = 8.dp)
                                    .padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Peaceful", "Energized", "Refreshed", "Stressed", "Overwhelmed").forEach { mood ->
                                    FilterChip(
                                        selected = viewModel.selectedMood.collectAsState().value == mood,
                                        onClick = { viewModel.updateSelectedMood(mood) },
                                        label = { Text(mood) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF81C784),
                                            selectedLabelColor = Color.Black,
                                            labelColor = Color.White
                                        )
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.saveRestorationLog(
                                        nriScore = if (activeLayout != null && activePlants.isNotEmpty()) nriScore else 30,
                                        completedTasks = completedTasksList.toList()
                                    )
                                    viewModel.logMood(
                                        mood = viewModel.selectedMood.value,
                                        score = when (viewModel.selectedMood.value) {
                                            "Peaceful" -> 5
                                            "Refreshed" -> 5
                                            "Energized" -> 4
                                            "Stressed" -> 2
                                            "Overwhelmed" -> 1
                                            else -> 3
                                        },
                                        duration = 15,
                                        notes = viewModel.dailyReflection.value
                                    )
                                    viewModel.updateDailyReflection("")
                                    Toast.makeText(context, "Reflection saved successfully!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81C784)),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Save Journal Entry", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                            
                    Spacer(modifier = Modifier.height(24.dp))
                            
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E352F).copy(alpha = 0.7f)),
                        border = BorderStroke(1.dp, Color(0xFF81C784).copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "🧘 Mindfulness Exercises",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA8E6CF)
                            )
                            Text(
                                text = "Complete these exercises to boost your Neural Restoration Index.",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            availableTasks.forEach { task ->
                                val isChecked = completedTasksList.contains(task)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isChecked) {
                                                completedTasksList.remove(task)
                                            } else {
                                                completedTasksList.add(task)
                                            }
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            if (checked == true) {
                                                completedTasksList.add(task)
                                            } else {
                                                completedTasksList.remove(task)
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = Color(0xFF81C784),
                                            uncheckedColor = Color.White.copy(alpha = 0.5f)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = task,
                                        fontSize = 13.sp,
                                        color = if (isChecked) Color.White.copy(alpha = 0.5f) else Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Complete / Log Journal entry button
                            Button(
                                onClick = {
                                    viewModel.logRestorationSession(
                                        nriScore = if (activeLayout != null && activePlants.isNotEmpty()) nriScore else 30,
                                        completedTasks = completedTasksList.toList(),
                                        trackName = currentTrack
                                    )
                                    Toast.makeText(context, "Restoration session logged to journal successfully!", Toast.LENGTH_SHORT).show()
                                    completedTasksList.clear()
                                },
                                enabled = completedTasksList.isNotEmpty() || isPlaying,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81C784)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Log Session & Lock In Progress",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F261D)
                                )
                            }
                        }
                    }
                }

                if (!isPremium && restorationTrialCount >= 3) {
                    val infiniteTransition = rememberInfiniteTransition(label = "paywall_pulse")
                    val paywallPulse by infiniteTransition.animateFloat(
                        initialValue = 0.96f,
                        targetValue = 1.04f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "paywall_pulse"
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color(0xFF0F261D).copy(alpha = 0.88f), RoundedCornerShape(24.dp))
                            .clickable(enabled = false) {},
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .padding(16.dp)
                                .testTag("restoration_premium_paywall_card"),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1F18)),
                            border = BorderStroke(
                                width = 2.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.extendedColors.premiumGold,
                                        MaterialTheme.extendedColors.premiumGold.copy(alpha = 0.6f),
                                        MaterialTheme.extendedColors.premiumGold
                                    )
                                )
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Premium locked",
                                    tint = MaterialTheme.extendedColors.premiumGold,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .scale(paywallPulse)
                                  )
                                  Text(
                                      text = "Restoration Journal (Premium 👑)",
                                      fontWeight = FontWeight.ExtraBold,
                                      fontSize = 18.sp,
                                      color = Color(0xFFA8E6CF),
                                      textAlign = TextAlign.Center
                                  )
                                  Text(
                                      text = "You have completed your 3 free trial sessions. Upgrade to FloraFlow PRO for unlimited Forest Breeze, Gentle Rain & Ocean Wave sessions — living soundscapes that never loop — plus sleep timer fade-outs and neural restoration tracking!",
                                      fontSize = 12.sp,
                                      color = Color.White.copy(alpha = 0.75f),
                                      textAlign = TextAlign.Center,
                                      lineHeight = 17.sp
                                  )
                                  Spacer(modifier = Modifier.height(4.dp))
                                  Button(
                                      onClick = { viewModel.upgradeToPremium() },
                                      colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                      contentPadding = PaddingValues(),
                                      shape = RoundedCornerShape(12.dp),
                                      modifier = Modifier
                                          .fillMaxWidth()
                                          .height(48.dp)
                                          .background(
                                              Brush.horizontalGradient(
                                                  colors = listOf(
                                                      MaterialTheme.extendedColors.premiumGold,
                                                      MaterialTheme.extendedColors.premiumGold.copy(alpha = 0.6f)
                                                  )
                                              ),
                                              shape = RoundedCornerShape(12.dp)
                                          )
                                ) {
                                    Text(
                                        text = "Upgrade to PRO for Unlimited Access",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F261D),
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Restoration logs list
            if (restorationLogs.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(bottom = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .run {
                                if (!isPremium) blur(4.dp) else this
                            }
                    ) {
                        NriHistoryChart(
                            logs = restorationLogs,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    if (!isPremium) {
                        com.example.ui.components.PremiumLockOverlay(
                            onUpgradeClick = { viewModel.setBillingDialogVisible(true, "restoration_nri_history") },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Text(
                    text = "Restoration Logs",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA8E6CF),
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = 8.dp, bottom = 8.dp)
                )

                restorationLogs.take(5).forEach { log ->
                    RestorationLogItem(log)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun EmptyStateCard() {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E352F).copy(alpha = 0.7f)),
        border = BorderStroke(1.dp, Color(0xFF81C784).copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Spa,
                contentDescription = "Spa",
                tint = Color(0xFF81C784),
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Garden Layout Detected",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFA8E6CF),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your custom stress-relief index depends on your plants. Go to 'My Plot' to place some greenery and calculate your first score.",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun NriGaugeCard(
    nriScore: Int,
    diversityScore: Int,
    synergyScore: Int,
    careMultiplier: Double,
    penaltyScore: Int,
    plantCount: Int
) {
    val ratingText = when {
        nriScore >= 75 -> "Optimal Restoration Potential"
        nriScore >= 50 -> "Moderate Cognitive Recovery"
        else -> "Mild Biophilic Restoration"
    }

    val ratingDesc = when {
        nriScore >= 75 -> "Your layout possesses excellent diversity and synergy, perfect for stress recovery and focus."
        nriScore >= 50 -> "Adding more unique companion plant varieties can boost cognitive restoration properties."
        else -> "Place more companion synergy pairs next to each other to lower sensory fatigue."
    }

    // Animated score rotation/scale
    val animatedNri = animateIntAsState(
        targetValue = nriScore,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "nri"
    )

    var isExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E352F).copy(alpha = 0.7f)),
        border = BorderStroke(1.dp, Color(0xFF81C784).copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Neural Restoration Index (NRI)",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFA8E6CF)
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Canvas arc gauge with pulsing glow
            val infiniteTransition = rememberInfiniteTransition(label = "gauge_glow")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.98f,
                targetValue = 1.02f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse"
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(160.dp)
                    .scale(pulseScale)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Track circle
                    drawArc(
                        color = Color(0xFF0F261D),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Progress arc with gradient
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFF81C784),
                                Color(0xFF4DB6AC),
                                Color(0xFF81C784)
                            )
                        ),
                        startAngle = 135f,
                        sweepAngle = (270f * (animatedNri.value / 100f)),
                        useCenter = false,
                        style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${animatedNri.value}%",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "RESTORED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF81C784)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = ratingText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF81C784)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = ratingDesc,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color(0xFF81C784).copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))

            // Expandable Diagnostics Toggle Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "View Score Diagnostics",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA8E6CF)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse diagnostics" else "Expand diagnostics",
                    tint = Color(0xFFA8E6CF),
                    modifier = Modifier
                        .size(16.dp)
                        .scale(scaleX = 1f, scaleY = if (isExpanded) -1f else 1f)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Plant Diversity
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("🌿 Unique Species Diversity", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                            Text("$diversityScore / 40 pts", fontSize = 11.sp, color = Color(0xFFA8E6CF), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = diversityScore / 40f,
                            color = Color(0xFF81C784),
                            trackColor = Color(0xFF0F261D),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    }

                    // 2. Companion Synergy
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("🤝 Adjacency Synergy Score", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                            Text("$synergyScore / 40 pts", fontSize = 11.sp, color = Color(0xFFA8E6CF), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = synergyScore / 40f,
                            color = Color(0xFF81C784),
                            trackColor = Color(0xFF0F261D),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    }

                    // 3. Care & Maturity Multiplier
                    Column {
                        val multiplierProgress = ((careMultiplier - 0.8) / 0.4).toFloat().coerceIn(0f, 1f)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("📈 Plant Health & Growth Multiplier", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                            Text(String.format(Locale.US, "%.2fx", careMultiplier), fontSize = 11.sp, color = Color(0xFFA8E6CF), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = multiplierProgress,
                            color = Color(0xFF4DB6AC),
                            trackColor = Color(0xFF0F261D),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    }

                    // 4. Overdue Task Penalty
                    if (penaltyScore > 0) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("⚠️ Neglected Care Penalty", fontSize = 11.sp, color = Color(0xFFE57373))
                                Text("-$penaltyScore pts", fontSize = 11.sp, color = Color(0xFFE57373), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = (penaltyScore / 50f).coerceIn(0f, 1f),
                                color = Color(0xFFE57373),
                                trackColor = Color(0xFF0F261D),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SleepTimerRow(
    sleepTimerEndTime: Long?,
    isPlaying: Boolean,
    onSetTimer: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Tick once a second while a timer is armed so the countdown stays live
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(sleepTimerEndTime) {
        while (sleepTimerEndTime != null) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }

    var selectedMinutes by remember { mutableStateOf(0) }
    // Timer finished (or was cleared by pause) — snap the chips back to Off
    LaunchedEffect(sleepTimerEndTime) {
        if (sleepTimerEndTime == null) selectedMinutes = 0
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🌙 Sleep timer",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            listOf(0, 15, 30, 60).forEach { minutes ->
                val isSelected = selectedMinutes == minutes
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) Color(0xFF81C784).copy(alpha = 0.25f)
                            else Color(0xFF0F261D).copy(alpha = 0.8f)
                        )
                        .clickable(enabled = isPlaying || minutes == 0) {
                            selectedMinutes = minutes
                            onSetTimer(minutes)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (minutes == 0) "Off" else "${minutes}m",
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFFA8E6CF) else Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
        if (sleepTimerEndTime != null && isPlaying) {
            val remainingSecs = ((sleepTimerEndTime - now) / 1000L).coerceAtLeast(0L)
            Text(
                text = "Fading out in %d:%02d".format(remainingSecs / 60, remainingSecs % 60),
                fontSize = 10.sp,
                color = Color(0xFF81C784).copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun AudioWaveformVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // Smooth transition for amplitude when play state changes
    val amplitudeMultiplier by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.08f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "amplitude"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2

        // Draw 3 overlaying waves with different properties (color, speed, amplitude)
        val waves = listOf(
            Triple(Color(0xFF81C784).copy(alpha = 0.4f), 1.5f, 14.dp.toPx()),     // Wave 1
            Triple(Color(0xFFA8E6CF).copy(alpha = 0.5f), 2.2f, 8.dp.toPx()),      // Wave 2
            Triple(Color(0xFF4DB6AC).copy(alpha = 0.3f), 0.9f, 18.dp.toPx())      // Wave 3
        )

        waves.forEachIndexed { index, (color, frequencyFactor, maxAmplitude) ->
            val path = androidx.compose.ui.graphics.Path()
            val currentAmplitude = maxAmplitude * amplitudeMultiplier
            
            for (x in 0..width.toInt() step 4) {
                val progress = x.toFloat() / width
                val edgeTaper = sin(progress * Math.PI.toFloat())
                val angle = (progress * 2 * Math.PI.toFloat() * frequencyFactor) + phase * (if (index % 2 == 0) 1 else -1)
                val y = centerY + sin(angle) * currentAmplitude * edgeTaper

                if (x == 0) {
                    path.moveTo(x.toFloat(), y)
                } else {
                    path.lineTo(x.toFloat(), y)
                }
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun NriHistoryChart(
    logs: List<com.example.data.model.RestorationLog>,
    modifier: Modifier = Modifier
) {
    val recentLogs = remember(logs) {
        logs.take(5).reversed()
    }

    if (recentLogs.size < 2) return

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Stress Relief Progress (NRI History)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFA8E6CF),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val width = size.width
                val height = size.height
                val paddingLeft = 35.dp.toPx()
                val paddingRight = 20.dp.toPx()
                val paddingTop = 15.dp.toPx()
                val paddingBottom = 20.dp.toPx()

                val chartWidth = width - paddingLeft - paddingRight
                val chartHeight = height - paddingTop - paddingBottom

                val minScore = 0f
                val maxScore = 100f

                val points = recentLogs.mapIndexed { index, log ->
                    val x = paddingLeft + (index.toFloat() / (recentLogs.size - 1)) * chartWidth
                    val yPercent = (log.nriScore - minScore) / (maxScore - minScore)
                    val y = height - paddingBottom - yPercent * chartHeight
                    Offset(x, y)
                }

                // Draw grid lines
                val gridLines = 4
                for (i in 0..gridLines) {
                    val yPercent = i.toFloat() / gridLines
                    val y = height - paddingBottom - yPercent * chartHeight
                    
                    drawLine(
                        color = Color(0xFF81C784).copy(alpha = 0.1f),
                        start = Offset(paddingLeft, y),
                        end = Offset(width - paddingRight, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    
                    drawContext.canvas.nativeCanvas.drawText(
                        "${(minScore + yPercent * (maxScore - minScore)).toInt()}%",
                        10f,
                        y + 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#66A8E6CF")
                            textSize = 9.sp.toPx()
                            textAlign = android.graphics.Paint.Align.LEFT
                        }
                    )
                }

                val strokePath = androidx.compose.ui.graphics.Path()
                val fillPath = androidx.compose.ui.graphics.Path()

                if (points.isNotEmpty()) {
                    strokePath.moveTo(points[0].x, points[0].y)
                    fillPath.moveTo(points[0].x, points[0].y)

                    for (i in 0 until points.size - 1) {
                        val p1 = points[i]
                        val p2 = points[i + 1]
                        val controlPointX1 = p1.x + (p2.x - p1.x) / 2
                        val controlPointY1 = p1.y
                        val controlPointX2 = p1.x + (p2.x - p1.x) / 2
                        val controlPointY2 = p2.y

                        strokePath.cubicTo(
                            controlPointX1, controlPointY1,
                            controlPointX2, controlPointY2,
                            p2.x, p2.y
                        )
                        fillPath.cubicTo(
                            controlPointX1, controlPointY1,
                            controlPointX2, controlPointY2,
                            p2.x, p2.y
                        )
                    }

                    fillPath.lineTo(points.last().x, height - paddingBottom)
                    fillPath.lineTo(points.first().x, height - paddingBottom)
                    fillPath.close()

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF81C784).copy(alpha = 0.25f),
                                Color(0xFF81C784).copy(alpha = 0.0f)
                            )
                        )
                    )

                    drawPath(
                        path = strokePath,
                        color = Color(0xFF81C784),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    points.forEachIndexed { index, point ->
                        val log = recentLogs[index]
                        
                        drawCircle(
                            color = Color(0xFF0F261D),
                            radius = 6.dp.toPx(),
                            center = Offset(point.x, point.y)
                        )
                        drawCircle(
                            color = Color(0xFF81C784),
                            radius = 4.dp.toPx(),
                            center = Offset(point.x, point.y)
                        )

                        drawContext.canvas.nativeCanvas.drawText(
                            "${log.nriScore}%",
                            point.x,
                            point.y - 8.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 10.sp.toPx()
                                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )

                        val trackLabel = log.soundscapeTrack.split(" ")[0]
                        drawContext.canvas.nativeCanvas.drawText(
                            trackLabel,
                            point.x,
                            height - 4.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#99FFFFFF")
                                textSize = 8.sp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RestorationLogItem(log: com.example.data.model.RestorationLog) {
    val formatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val dateString = formatter.format(Date(log.timestamp))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF81C784).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${log.nriScore}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF81C784)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.soundscapeTrack,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (log.completedTasks.isNotBlank()) "Tasks: ${log.completedTasks}" else "Soundscape listening only",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 1
                )
            }
            Text(
                text = dateString,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}

data class SoundscapeTrackInfo(
    val name: String,
    val scene: String,
    val baseFreq: Float,
    val diffFreq: Float,
    val description: String
)

data class TaskCategory(val name: String, val color: Color)

fun getTaskCategory(task: String): TaskCategory {
    val t = task.lowercase()
    return when {
        t.contains("breathing") -> TaskCategory("BREATHING", Color(0xFF81C784))
        t.contains("breeze") || t.contains("chimes") -> TaskCategory("AUDITORY", Color(0xFF4DB6AC))
        t.contains("aroma") || t.contains("smell") -> TaskCategory("OLFACTORY", Color(0xFFFFB74D))
        t.contains("geometry") || t.contains("veins") || t.contains("visual") -> TaskCategory("VISUAL", Color(0xFF64B5F6))
        t.contains("rub") || t.contains("fingers") || t.contains("touch") -> TaskCategory("TACTILE", Color(0xFFBA68C8))
        else -> TaskCategory("SENSORY", Color(0xFFA8E6CF))
    }
}
