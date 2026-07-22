package com.example.ui.screens.restorativevalidation

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert

@Composable
fun RestorativeValidationRoute(
    viewModel: RestorativeValidationViewModel,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val shareLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    val documentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        viewModel.onIntent(RestorativeIntent.DocumentDestinationSelected(uri?.toString()))
    }
    RestorativeValidationScreen(
        state = state,
        onStart = viewModel::startJourney,
        onIntent = viewModel::onIntent,
        onExit = onExit,
        launchShare = { json ->
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_TEXT, json)
            }
            shareLauncher.launch(Intent.createChooser(sendIntent, "Share FloraFlow test record"))
        },
        launchCreateDocument = documentLauncher::launch,
        modifier = modifier,
    )
}

@Composable
internal fun RestorativeValidationScreen(
    state: RestorativeUiState,
    onStart: () -> Unit,
    onIntent: (RestorativeIntent) -> Unit,
    onExit: () -> Unit = {},
    launchShare: (String) -> Unit = {},
    launchCreateDocument: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showCloseDialog by rememberSaveable { mutableStateOf(false) }
    val hasMeaningfulDraft = state.draft.experimentId != null && state.step != RestorativeStep.SAVED
    val terminalFailure = state.error is RestorativeError.TerminalEvidenceFailure

    BackHandler(enabled = hasMeaningfulDraft && !state.isBusy && !terminalFailure) {
        onIntent(RestorativeIntent.Back)
    }
    LaunchedEffect(state.exitRequested) {
        if (state.exitRequested) onExit()
    }
    LaunchedEffect(state.researchExport.status, state.researchExport.preparedJson) {
        when (state.researchExport.status) {
            ResearchExportStatus.READY_TO_SHARE -> {
                val json = state.researchExport.preparedJson ?: return@LaunchedEffect
                try {
                    launchShare(json)
                    onIntent(RestorativeIntent.ShareLaunchSucceeded)
                } catch (_: ActivityNotFoundException) {
                    onIntent(RestorativeIntent.ShareLaunchFailed("SHARE_TARGET_UNAVAILABLE"))
                } catch (_: SecurityException) {
                    onIntent(RestorativeIntent.ShareLaunchFailed("SHARE_LAUNCH_DENIED"))
                }
            }
            ResearchExportStatus.AWAITING_DOCUMENT -> try {
                launchCreateDocument(state.researchExport.suggestedFileName)
            } catch (_: ActivityNotFoundException) {
                onIntent(RestorativeIntent.DocumentLaunchFailed("DOCUMENT_PICKER_UNAVAILABLE"))
            } catch (_: SecurityException) {
                onIntent(RestorativeIntent.DocumentLaunchFailed("DOCUMENT_PICKER_DENIED"))
            }
            else -> Unit
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding()
                .fillMaxSize(),
        ) {
            if (hasMeaningfulDraft && !terminalFailure) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { showCloseDialog = true }, modifier = Modifier.height(48.dp)) {
                        Text("Close", fontSize = 16.sp)
                    }
                }
            }
            if (state.step != RestorativeStep.PROMISE) {
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().sizeIn(maxWidth = 720.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    val terminalFailure = state.error as? RestorativeError.TerminalEvidenceFailure
                    if (terminalFailure != null) {
                        TerminalEvidenceStage(terminalFailure)
                    } else {
                        when (state.step) {
                            RestorativeStep.PROMISE -> PromiseStage(state.isBusy, onStart)
                            RestorativeStep.LIGHT -> LightStage(state, onIntent)
                            RestorativeStep.SPACE -> SpaceStage(state, onIntent)
                            RestorativeStep.PLANTS -> PlantsStage(state, onIntent)
                            RestorativeStep.PLAN -> PlanStage(state, onIntent)
                            RestorativeStep.SAVED -> SavedStage(state, onIntent)
                        }
                        state.error?.let { ErrorNotice(it, state.step, onIntent) }
                    }
                }
            }
        }
    }

    if (showCloseDialog) {
        AlertDialog(
            onDismissRequest = { showCloseDialog = false },
            title = { Text("Leave your corner plan?", modifier = Modifier.semantics { heading() }) },
            text = { Text("You can keep this draft for later or discard this test attempt.", fontSize = 16.sp) },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showCloseDialog = false
                            onIntent(RestorativeIntent.SaveDraftAndExit)
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) { Text("Save draft and exit", fontSize = 16.sp) }
                    OutlinedButton(
                        onClick = {
                            showCloseDialog = false
                            onIntent(RestorativeIntent.DiscardDraftAndExit)
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) { Text("Discard", fontSize = 16.sp) }
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseDialog = false }, modifier = Modifier.height(48.dp)) {
                    Text("Cancel", fontSize = 16.sp)
                }
            },
        )
    }
    if (state.researchExport.disclosureVisible) {
        ResearchExportDisclosure(state.researchExport, onIntent)
    }
}

@Composable
private fun PromiseStage(isBusy: Boolean, onStart: () -> Unit) {
    Spacer(Modifier.height(24.dp))
    Text("FLORAFLOW", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    Text(
        "Create your meditation corner",
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.semantics { heading() },
    )
    Text(
        "Turn the light and space you already have into a small, nature-supported place to pause and restore.",
        fontSize = 18.sp,
        lineHeight = 27.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("A starter plan in about 3 minutes", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text("Choose your light, your available space, and up to three plants—or let FloraFlow suggest them.", fontSize = 16.sp, lineHeight = 24.sp)
        }
    }
    PrimaryAction("Start my corner", enabled = !isBusy, onClick = onStart)
}

@Composable
private fun LightStage(state: RestorativeUiState, onIntent: (RestorativeIntent) -> Unit) {
    StageHeader("1 of 3", "What light reaches your space?", "Think about the area during most of the day.")
    ChoiceGrid(
        choices = listOf(
            LightChoice.LOW to ("Low, indirect light" to "Away from windows or softly lit"),
            LightChoice.MEDIUM to ("Bright, indirect light" to "Close to a bright window without direct rays"),
            LightChoice.BRIGHT to ("Some direct sun" to "Sunlight reaches the space for part of the day"),
            LightChoice.UNSURE to ("Not sure" to "We’ll choose flexible plants"),
        ),
        selected = state.draft.light,
        onSelect = { onIntent(RestorativeIntent.SelectLight(it)) },
    )
    NavigationActions(state, onIntent)
}

@Composable
private fun SpaceStage(state: RestorativeUiState, onIntent: (RestorativeIntent) -> Unit) {
    StageHeader("2 of 3", "How much room can you give it?", "A calm corner can begin with a single surface.")
    ChoiceGrid(
        choices = listOf(
            AvailableSpace.TABLETOP to ("Shelf or tabletop" to "A desk, shelf, or side table"),
            AvailableSpace.SMALL_CORNER to ("Small floor corner" to "Room for one floor plant and accents"),
            AvailableSpace.OPEN_CORNER to ("Larger corner" to "A chair-side or floor arrangement"),
        ),
        selected = state.draft.availableSpace,
        onSelect = { onIntent(RestorativeIntent.SelectSpace(it)) },
    )
    NavigationActions(state, onIntent)
}

@Composable
private fun PlantsStage(state: RestorativeUiState, onIntent: (RestorativeIntent) -> Unit) {
    StageHeader("3 of 3", "Choose how to begin", "Use plants you own or get a simple starting suggestion.")
    ChoiceCard(
        title = "Use plants I own",
        detail = "Choose up to three from this small starter list",
        selected = state.draft.inputMode == InputMode.OWNED_PLANTS,
        onClick = { onIntent(RestorationIntentAlias.ownedMode) },
    )
    if (state.draft.inputMode == InputMode.OWNED_PLANTS) {
        ValidationPlantCatalog.entries.forEach { plant ->
            PlantToggle(
                plant = plant,
                selected = plant.slug in state.draft.ownedPlantSlugs,
                enabled = plant.slug in state.draft.ownedPlantSlugs || state.draft.ownedPlantSlugs.size < MAX_VALIDATION_PLANTS,
                onToggle = { onIntent(RestorativeIntent.ToggleOwnedPlant(plant.slug)) },
            )
        }
    }
    ChoiceCard(
        title = "Recommend plants for me",
        detail = "Match easy-care options to my light and space",
        selected = state.draft.inputMode == InputMode.RECOMMEND_FROM_SCRATCH,
        onClick = { onIntent(RestorativeIntent.SelectInputMode(InputMode.RECOMMEND_FROM_SCRATCH)) },
    )
    NavigationActions(state, onIntent, continueLabel = "Create my starter plan")
}

private object RestorationIntentAlias {
    val ownedMode = RestorativeIntent.SelectInputMode(InputMode.OWNED_PLANTS)
}

@Composable
private fun PlanStage(state: RestorativeUiState, onIntent: (RestorativeIntent) -> Unit) {
    StageHeader("YOUR STARTER PLAN", "A quiet corner, shaped around you", "Keep it simple. Give each plant a clear role in the space.")
    state.plan?.let { plan ->
        Text("My meditation corner", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "${state.draft.light?.plainLanguage()}  •  ${state.draft.availableSpace?.plainLanguage()}",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BotanicalCornerComposition(plan)
        Card(shape = RoundedCornerShape(20.dp)) {
            Column {
                plan.placements.forEachIndexed { index, placement ->
                    val plant = plan.plants.first { it.slug == placement.plantSlug }
                    Row(
                        Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            placement.number.toString(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(plant.canonicalName, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                            Text(placement.role.plainLanguage(), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            ValidationPlantCatalog.find(plant.slug)?.let {
                                Text(it.suitabilityReason, fontSize = 16.sp, lineHeight = 23.sp)
                                Text(
                                    "Safety: ${it.safetyStatus.presentation}",
                                    fontSize = 16.sp,
                                    lineHeight = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    if (index < plan.placements.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 20.dp))
                }
            }
        }
    }
    Text(
        "This starter plan does not screen for pet, child, allergy, or medical safety.",
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text("Your first pause", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    Text("Sit beside the space for two quiet minutes. Notice the leaves, light, and your breathing—nothing to complete.", fontSize = 17.sp, lineHeight = 25.sp)
    HorizontalDivider()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = { onIntent(RestorativeIntent.EditSpace) },
            enabled = !state.isBusy,
            modifier = Modifier.weight(1f).height(52.dp),
        ) { Text("Adjust space", fontSize = 15.sp, textAlign = TextAlign.Center) }
        OutlinedButton(
            onClick = { onIntent(RestorativeIntent.EditPlants) },
            enabled = !state.isBusy,
            modifier = Modifier.weight(1f).height(52.dp),
        ) { Text("Change plants", fontSize = 15.sp, textAlign = TextAlign.Center) }
    }
    PrimaryAction(
        label = "Save my corner",
        enabled = state.canContinue,
        onClick = { onIntent(RestorativeIntent.SavePlan) },
        busy = state.isBusy,
    )
}

@Composable
private fun BotanicalCornerComposition(plan: StarterPlan) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(24.dp))
            .semantics {
                contentDescription = "Meditation corner composition with ${plan.placements.size} numbered plant positions"
            },
    ) {
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(52.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer),
        )
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 22.dp, vertical = 30.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
        ) {
            plan.placements.forEachIndexed { index, placement ->
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = if (index % 2 == 0) 92.dp else 68.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), RoundedCornerShape(50)),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .sizeIn(minWidth = 36.dp, minHeight = 36.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            placement.number.toString(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedStage(state: RestorativeUiState, onIntent: (RestorativeIntent) -> Unit) {
    Spacer(Modifier.height(40.dp))
    Text("Saved", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
    Text("Your restorative corner has a starting point", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
    Text("You can return here when you’re ready to place the plants. Start with one small move; the space does not need to be perfect.", fontSize = 18.sp, lineHeight = 27.sp)
    Text("What feels doable next?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
    Text("FloraFlow will remember this as your next step.", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    listOf(
        "Move a chair into the corner",
        "Place one plant beside where I sit",
        "Clear a nearby surface",
    ).forEach { option ->
        ChoiceCard(
            title = option,
            detail = "A small physical start",
            selected = state.nextStepDecisionRecorded && state.intendedNextStep == option,
            onClick = { onIntent(RestorativeIntent.SelectNextStep(option)) },
        )
    }
    ChoiceCard(
        title = "I’ll decide later",
        detail = "Your saved plan will still be here",
        selected = state.nextStepDecisionRecorded && state.intendedNextStep == null,
        onClick = { onIntent(RestorativeIntent.SelectNextStep(null)) },
    )
    state.intendedNextStep?.let {
        Text("Next step: $it", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    }
    OutlinedButton(
        onClick = { onIntent(RestorativeIntent.OpenPlacementGuidance) },
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) { Text(if (state.placementGuidanceExpanded) "Placement guidance open" else "View placement guidance", fontSize = 16.sp) }
    if (state.placementGuidanceExpanded) SavedPlacementGuidance(state)
    ResearchExportStatusPanel(state.researchExport, onIntent)
    var showResearchMenu by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        IconButton(
            onClick = { showResearchMenu = true },
            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
        ) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options")
        }
        DropdownMenu(expanded = showResearchMenu, onDismissRequest = { showResearchMenu = false }) {
            DropdownMenuItem(
                text = { Text("Export test record", fontSize = 16.sp) },
                onClick = {
                    showResearchMenu = false
                    onIntent(RestorativeIntent.OpenExportDisclosure)
                },
            )
        }
    }
}

@Composable
private fun ResearchExportStatusPanel(
    export: ResearchExportState,
    onIntent: (RestorativeIntent) -> Unit,
) {
    when (export.status) {
        ResearchExportStatus.PREPARING,
        ResearchExportStatus.WRITING_DOCUMENT -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(Modifier.width(24.dp).height(24.dp), strokeWidth = 2.dp)
            Text(
                if (export.status == ResearchExportStatus.PREPARING) "Preparing test record" else "Saving test record",
                fontSize = 16.sp,
            )
        }

        ResearchExportStatus.SHARE_OPTIONS_OPENED -> ResearchStatusMessage(
            title = "Share options opened",
            detail = "Confirm the destination outside FloraFlow before clearing this phone.",
            onDismiss = { onIntent(RestorativeIntent.DismissExportStatus) },
        )

        ResearchExportStatus.DOCUMENT_SAVED -> ResearchStatusMessage(
            title = "Test record saved",
            detail = export.destinationDisplay ?: "The selected document was written.",
            onDismiss = { onIntent(RestorativeIntent.DismissExportStatus) },
        )

        ResearchExportStatus.PICKER_CANCELLED -> ResearchStatusMessage(
            title = "No file was created",
            detail = "Both local records remain on this phone.",
            primaryLabel = "Choose destination",
            onPrimary = { onIntent(RestorativeIntent.RetryExport) },
            onDismiss = { onIntent(RestorativeIntent.DismissExportStatus) },
        )

        ResearchExportStatus.FAILED -> ResearchStatusMessage(
            title = "Test record was not saved",
            detail = "Local code: ${export.stableCode}. Both local records remain on this phone.",
            primaryLabel = if (export.retryAllowed) "Try again" else null,
            onPrimary = { onIntent(RestorativeIntent.RetryExport) },
            onDismiss = { onIntent(RestorativeIntent.DismissExportStatus) },
        )

        else -> Unit
    }
}

@Composable
private fun ResearchStatusMessage(
    title: String,
    detail: String,
    primaryLabel: String? = null,
    onPrimary: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, fontSize = 16.sp, lineHeight = 23.sp)
            primaryLabel?.let {
                Button(onClick = onPrimary, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text(it, fontSize = 16.sp)
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) {
                Text("Cancel", fontSize = 16.sp)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ResearchExportDisclosure(
    export: ResearchExportState,
    onIntent: (RestorativeIntent) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { onIntent(RestorativeIntent.CancelExportDisclosure) },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("RESEARCH TOOL", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                "Export test record",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                "Choose a system destination only after the participant agrees.",
                fontSize = 16.sp,
                lineHeight = 23.sp,
            )
            ExportDisclosureGroup(
                title = "Included",
                items = listOf(
                    "Journey choices and selected plant slugs",
                    "Optional next step",
                    "Local experiment events and return classification",
                    "Schema and experiment identifiers",
                    "Timestamps and integrity hash",
                ),
            )
            ExportDisclosureGroup(
                title = "Not included",
                items = listOf(
                    "Participant name or email",
                    "Journal, health text, photos, or location",
                    "Device identifiers or Firebase data",
                    "Production garden data, stack traces, or unrelated preferences",
                ),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = 48.dp)
                    .toggleable(
                        value = export.consentConfirmed,
                        role = Role.Checkbox,
                        onValueChange = { onIntent(RestorativeIntent.SetExportConsent(it)) },
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Checkbox(
                    checked = export.consentConfirmed,
                    onCheckedChange = null,
                )
                Text("The participant has agreed to this export.", fontSize = 16.sp, lineHeight = 22.sp)
            }
            if (!export.consentConfirmed) {
                Text(
                    "Confirm participant agreement to choose a destination.",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = { onIntent(RestorativeIntent.PrepareExport) },
                enabled = export.consentConfirmed && export.status == ResearchExportStatus.IDLE,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(
                    if (export.status == ResearchExportStatus.PREPARING) "Preparing test record" else "Choose destination",
                    fontSize = 17.sp,
                )
            }
            TextButton(
                onClick = { onIntent(RestorativeIntent.CancelExportDisclosure) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("Cancel", fontSize = 16.sp) }
        }
    }
}

@Composable
private fun ExportDisclosureGroup(title: String, items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        items.forEach { Text("• $it", fontSize = 16.sp, lineHeight = 22.sp) }
    }
}

@Composable
private fun SavedPlacementGuidance(state: RestorativeUiState) {
    val plan = state.plan ?: return
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("My meditation corner", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            plan.placements.forEach { placement ->
                val plant = plan.plants.first { it.slug == placement.plantSlug }
                Text(
                    "${placement.number}. ${plant.canonicalName} — ${placement.role.plainLanguage()}",
                    fontSize = 16.sp,
                    lineHeight = 23.sp,
                )
            }
        }
    }
}

@Composable
private fun TerminalEvidenceStage(error: RestorativeError.TerminalEvidenceFailure) {
    var showResearcherDetails by rememberSaveable { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    Spacer(Modifier.height(40.dp))
    Text("FLORAFLOW TEST", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    if (showResearcherDetails) {
        Text("Researcher details", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
        Text("Do not restart or clear app data.", fontSize = 18.sp, lineHeight = 27.sp)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Record preserved", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("Retry unavailable", fontSize = 16.sp)
                Text("Local code: ${error.stableCode}", fontSize = 16.sp)
            }
        }
        OutlinedButton(
            onClick = {
                clipboardManager.setText(
                    AnnotatedString(
                        "FloraFlow test record preserved. Retry unavailable. Local code: ${error.stableCode}"
                    )
                )
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("Copy details", fontSize = 16.sp) }
        OutlinedButton(
            onClick = { showResearcherDetails = false },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("Back", fontSize = 16.sp) }
    } else {
        Text("Please hand the phone back to the researcher.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
        Text("Your answers were not erased. This test cannot continue safely on this phone.", fontSize = 18.sp, lineHeight = 27.sp)
        PrimaryAction("Show researcher details", enabled = true, onClick = { showResearcherDetails = true })
    }
}

@Composable
private fun StageHeader(eyebrow: String, title: String, detail: String) {
    Text(eyebrow, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
    Text(detail, fontSize = 17.sp, lineHeight = 25.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun <T> ChoiceGrid(choices: List<Pair<T, Pair<String, String>>>, selected: T?, onSelect: (T) -> Unit) {
    BoxWithConstraints {
        if (maxWidth >= 600.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                choices.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { (value, copy) ->
                            Box(Modifier.weight(1f)) { ChoiceCard(copy.first, copy.second, value == selected) { onSelect(value) } }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                choices.forEach { (value, copy) -> ChoiceCard(copy.first, copy.second, value == selected) { onSelect(value) } }
            }
        }
    }
}

@Composable
private fun ChoiceCard(title: String, detail: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 76.dp).selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, fontSize = 16.sp, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PlantToggle(plant: ValidationPlant, selected: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 72.dp).toggleable(value = selected, enabled = enabled, role = Role.Checkbox, onValueChange = { onToggle() }),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(plant.canonicalName, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(plant.suitabilityReason, fontSize = 16.sp, lineHeight = 22.sp)
        }
    }
}

@Composable
private fun NavigationActions(
    state: RestorativeUiState,
    onIntent: (RestorativeIntent) -> Unit,
    continueLabel: String = "Continue",
) {
    HorizontalDivider()
    if (!state.canContinue && !state.isBusy) {
        Text(
            "Choose one option above to continue.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = { onIntent(RestorativeIntent.Back) },
            enabled = !state.isBusy,
            modifier = Modifier.weight(1f).height(52.dp),
        ) { Text("Back", fontSize = 16.sp) }
        PrimaryAction(
            label = continueLabel,
            enabled = state.canContinue,
            onClick = {
                onIntent(if (state.step == RestorativeStep.PLAN) RestorativeIntent.SavePlan else RestorativeIntent.GeneratePlan)
            },
            modifier = Modifier.weight(2f),
            busy = state.isBusy,
        )
    }
}

@Composable
private fun PrimaryAction(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    busy: Boolean = false,
) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.height(54.dp)) {
        if (busy) CircularProgressIndicator(Modifier.width(22.dp), strokeWidth = 2.dp) else Text(label, fontSize = 17.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ErrorNotice(
    error: RestorativeError,
    step: RestorativeStep,
    onIntent: (RestorativeIntent) -> Unit,
) {
    val message = when (error) {
        RestorativeError.NoMatch -> "We couldn’t find a simple match yet. Go back and try another light or space option."
        is RestorativeError.PersistenceFailure -> "Your progress could not be saved just now. Please try again."
        is RestorativeError.TerminalEvidenceFailure -> "Your saved validation record cannot be opened on this version."
        is RestorativeError.InternalFailure -> "Something interrupted this step. Please go back and try again."
    }
    Text(message, fontSize = 16.sp, color = MaterialTheme.colorScheme.error, lineHeight = 23.sp)
    if (error is RestorativeError.PersistenceFailure) {
        OutlinedButton(
            onClick = {
                onIntent(if (step == RestorativeStep.PLAN) RestorativeIntent.SavePlan else RestorativeIntent.RetryLoad)
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text("Try again", fontSize = 16.sp) }
    }
}

private fun PlacementRole.plainLanguage(): String = when (this) {
    PlacementRole.FLOOR_ANCHOR -> "Place on the floor to give the corner a calm visual anchor."
    PlacementRole.TABLETOP_ACCENT -> "Place within easy view on a table, shelf, or stand."
    PlacementRole.TRAILING_EDGE -> "Place on a raised edge so the leaves can soften the space."
}

private fun LightChoice.plainLanguage(): String = when (this) {
    LightChoice.LOW -> "Low, indirect light"
    LightChoice.MEDIUM -> "Bright, indirect light"
    LightChoice.BRIGHT -> "Some direct sun"
    LightChoice.UNSURE -> "Light not yet known"
}

private fun AvailableSpace.plainLanguage(): String = when (this) {
    AvailableSpace.TABLETOP -> "Shelf or tabletop"
    AvailableSpace.SMALL_CORNER -> "Small floor corner"
    AvailableSpace.OPEN_CORNER -> "Larger corner"
}
