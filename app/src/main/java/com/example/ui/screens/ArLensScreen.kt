package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ClimatePlants
import com.example.ui.viewmodel.ArPlantPlacement
import com.example.ui.viewmodel.GardenViewModel

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
                    android.widget.Toast.makeText(context, "No existing purchase records found in Sandbox.", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            modifier = modifier
        )
        return
    }

    val activePlants by viewModel.activePlants.collectAsStateWithLifecycle()
    val activeLayout by viewModel.activeLayout.collectAsStateWithLifecycle()
    val arPlacedPlants by viewModel.arPlacedPlants.collectAsStateWithLifecycle()

    var selectedPlacementId by remember { mutableStateOf<Int?>(null) }
    var selectedBackgroundPreset by remember { mutableStateOf("Lawn Garden Grid") }

    val bgGradient = when (selectedBackgroundPreset) {
        "Sunny Patio" -> Brush.verticalGradient(listOf(Color(0xFF81C784), Color(0xFFFFD54F), Color(0xFFFFB74D)))
        "Balcony Deck" -> Brush.verticalGradient(listOf(Color(0xFF90A4AE), Color(0xFFCFD8DC), Color(0xFF78909C)))
        else -> Brush.verticalGradient(listOf(Color(0xFF386641), Color(0xFF6A994E), Color(0xFFA7C957)))
    }

    val selectedPlacement = arPlacedPlants.firstOrNull { it.id == selectedPlacementId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Core AR Header Info
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = "Lens",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "AR Simulated Yards Preview",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Select backdrop preset below & drag or scale plant stickers to preview garden density.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // Lens Backdrop controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Backdrop Presets:",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            listOf("Lawn Garden Grid", "Sunny Patio", "Balcony Deck").forEach { preset ->
                val isSelected = selectedBackgroundPreset == preset
                ElevatedFilterChip(
                    selected = isSelected,
                    onClick = { selectedBackgroundPreset = preset },
                    label = { Text(preset, fontSize = 10.sp) }
                )
            }
        }

        // THE AR INTERACTIVE VIEWPORT VIEW
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(bgGradient)
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .testTag("ar_viewport")
        ) {
            // HUD Scanning Overlay
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // HUD Top Info Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "ARGB LENS LIVE",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Text(
                        "PITCH: 0.1°  DEPTH: 2.4m",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Central Camera Reticle Focus
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.6f))
                    )
                }

                // Bottom Sync indicators
                Text(
                    text = "Topographic calibration complete. Touch decal to move/pinch.",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            // PLACED AR PLANT STICKERS CANVAS
            arPlacedPlants.forEach { placement ->
                val isSelected = selectedPlacementId == placement.id
                var dragOffset by remember(placement.id) { mutableStateOf(Offset.Zero) }
                
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (placement.offsetX + dragOffset.x).toInt(),
                                (placement.offsetY + dragOffset.y).toInt()
                            )
                        }
                        .pointerInput(placement.id) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                selectedPlacementId = placement.id
                                var currentDragOffset = Offset.Zero
                                
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val anyPressed = event.changes.any { it.pressed }
                                    if (!anyPressed) {
                                        break
                                    }
                                    
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                    if (change != null && change.pressed) {
                                        val dragAmount = change.position - change.previousPosition
                                        if (dragAmount != Offset.Zero) {
                                            change.consume()
                                            currentDragOffset += dragAmount
                                            dragOffset = currentDragOffset
                                        }
                                    }
                                }
                                
                                if (dragOffset != Offset.Zero) {
                                    viewModel.updateArPlantPosition(
                                        placement.id,
                                        dragOffset.x,
                                        dragOffset.y
                                    )
                                    dragOffset = Offset.Zero
                                }
                            }
                        }
                        .scale(placement.scale)
                        .rotate(placement.rotationDegrees)
                        .testTag("ar_placement_${placement.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .padding(8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) Color.White.copy(alpha = 0.8f)
                                else Color.Black.copy(alpha = 0.15f)
                            )
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(placement.emoji, fontSize = 48.sp)
                        Text(
                            placement.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isSelected) Color.Black else Color.White
                        )
                    }
                }
            }
        }

        // Seed Inventory Tray
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
                        "Garden Inventory Tray",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (arPlacedPlants.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearArPlants() }) {
                            Text("Reset", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val templates = ClimatePlants.getTemplatesForClimate(activeLayout?.climate ?: "Temperate")
                val availableAssetList = if (activePlants.isNotEmpty()) {
                    activePlants.map { it.name to getEmojiForPlantName(it.name) }
                } else {
                    templates.map { it.name to it.iconEmoji }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(availableAssetList) { asset ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    viewModel.addArPlant(asset.first, asset.second)
                                }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(asset.second, fontSize = 28.sp)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = asset.first.take(8),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // GESTURE PANEL WIDGETS
        if (selectedPlacement != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Tactile Decal Tools: ${selectedPlacement.name}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = {
                                viewModel.removeArPlant(selectedPlacement.id)
                                selectedPlacementId = null
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Remove sticker", tint = MaterialTheme.colorScheme.error)
                        }
                    }

                    // Scaler slider
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Scale:", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp))
                        Slider(
                            value = selectedPlacement.scale,
                            onValueChange = {
                                viewModel.updateArPlantScaling(selectedPlacement.id, it)
                            },
                            valueRange = 0.3f..3.0f,
                            modifier = Modifier.weight(1f)
                        )
                        Text(String.format("%.1fx", selectedPlacement.scale), fontSize = 11.sp, modifier = Modifier.width(36.dp))
                    }

                    // Rotation slider
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Rotate:", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp))
                        Slider(
                            value = selectedPlacement.rotationDegrees,
                            onValueChange = {
                                viewModel.updateArPlantRotation(selectedPlacement.id, it)
                            },
                            valueRange = 0f..360f,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${selectedPlacement.rotationDegrees.toInt()}°", fontSize = 11.sp, modifier = Modifier.width(36.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}
