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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.GardenViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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
    val restorationLogs by viewModel.allRestorationLogs.collectAsStateWithLifecycle()

    // Local checklist state
    val availableTasks = remember(activePlants) {
        val tasks = mutableListOf(
            "🫁 Practise 4-7-8 rhythmic breathing (inhale 4s, hold 7s, exhale 8s)",
            "🍃 Close your eyes and focus on the natural breeze or ambient wind chimes"
        )
        val plantNames = activePlants.map { it.name.lowercase() }
        if (plantNames.any { it.contains("lavender") }) {
            tasks.add("🌿 Inhale your Lavender's sweet aroma to trigger direct parasympathetic calming")
        }
        if (plantNames.any { it.contains("rose") }) {
            tasks.add("🌹 Trace a Rose petal's geometry slowly to rest your visual attention filters")
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

    // Soundscape tracks definition
    val tracks = listOf(
        SoundscapeTrackInfo("Alpha Focus", 200f, 10f, "Alpha waves (10Hz) promote alert relaxation, ideal for deep focus or workspace design."),
        SoundscapeTrackInfo("Theta Meditate", 200f, 6f, "Theta waves (6Hz) facilitate deep sensory visualization, creativity, and mental stillness."),
        SoundscapeTrackInfo("Delta Sleep", 150f, 2.5f, "Delta waves (2.5Hz) slow brainwaves down for heavy physical healing and deep sleep states.")
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

            Spacer(modifier = Modifier.height(20.dp))

            // NRI Index Gauge or Onboarding state
            if (activeLayout == null || activePlants.isEmpty()) {
                // Empty state card
                EmptyStateCard()
            } else {
                // NRI Progress gauge card
                NriGaugeCard(nriScore)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Soundscape Player Controller
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E352F).copy(alpha = 0.7f)),
                border = BorderStroke(1.dp, Color(0xFF81C784).copy(alpha = 0.15f)),
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
                        text = "Binaural beat synthesis overlaying natural ambient channels",
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
                                        Toast.makeText(context, "Switched to ${track.name}", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = track.name.split(" ")[0], // Alpha, Theta, Delta
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFFA8E6CF) else Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    // Track Description
                    val currentTrackDesc = tracks.find { it.name == currentTrack }?.description ?: ""
                    Text(
                        text = currentTrackDesc,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        lineHeight = 15.sp,
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Play/Pause button
                    Button(
                        onClick = { viewModel.toggleSoundscapePlay() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlaying) Color(0xFFC62828).copy(alpha = 0.8f) else Color(0xFF2E7D32).copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(50.dp),
                        modifier = Modifier.width(200.dp)
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mindfulness checklist
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E352F).copy(alpha = 0.7f)),
                border = BorderStroke(1.dp, Color(0xFF81C784).copy(alpha = 0.15f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "🧘 Today's Mindfulness Tasks",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA8E6CF)
                    )
                    Text(
                        text = "Complete layout tasks to train sensory grounding and log progress",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    availableTasks.forEach { task ->
                        val isChecked = completedTasksList.contains(task)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isChecked) Color(0xFF81C784).copy(alpha = 0.05f) else Color.Transparent)
                                .clickable {
                                    if (isChecked) {
                                        completedTasksList.remove(task)
                                    } else {
                                        completedTasksList.add(task)
                                    }
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    if (isChecked) {
                                        completedTasksList.remove(task)
                                    } else {
                                        completedTasksList.add(task)
                                    }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF81C784))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = task,
                                fontSize = 12.sp,
                                color = if (isChecked) Color.White.copy(alpha = 0.5f) else Color.White,
                                lineHeight = 16.sp
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

            Spacer(modifier = Modifier.height(16.dp))

            // Restoration logs list
            if (restorationLogs.isNotEmpty()) {
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
fun NriGaugeCard(nriScore: Int) {
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
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
    )

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

            // Canvas arc gauge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
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
                    // Progress arc
                    drawArc(
                        color = Color(0xFF81C784),
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
        }
    }
}

@Composable
fun RestorationLogItem(log: com.example.data.model.RestorationLog) {
    val formatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val dateString = formatter.format(Date(log.timestamp))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF162D27)),
        border = BorderStroke(0.5.dp, Color(0xFF81C784).copy(alpha = 0.1f)),
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
    val baseFreq: Float,
    val diffFreq: Float,
    val description: String
)
