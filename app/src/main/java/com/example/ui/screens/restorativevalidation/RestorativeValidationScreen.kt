package com.example.ui.screens.restorativevalidation

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun RestorativeValidationRoute(
    viewModel: RestorativeValidationViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RestorativeValidationScreen(
        state = state,
        onStart = viewModel::startJourney,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
internal fun RestorativeValidationScreen(
    state: RestorativeUiState,
    onStart: () -> Unit,
    onIntent: (RestorativeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding()
                .fillMaxSize(),
        ) {
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
                            RestorativeStep.SAVED -> SavedStage(state)
                        }
                        state.error?.let { ErrorNotice(it) }
                    }
                }
            }
        }
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
            LightChoice.LOW to ("Low light" to "Away from windows or softly lit"),
            LightChoice.MEDIUM to ("Medium light" to "Gentle, indirect daylight"),
            LightChoice.BRIGHT to ("Bright light" to "Close to a bright window"),
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
            AvailableSpace.TABLETOP to ("Tabletop" to "A desk, shelf, or side table"),
            AvailableSpace.SMALL_CORNER to ("Small corner" to "Room for one floor plant and accents"),
            AvailableSpace.OPEN_CORNER to ("Open corner" to "A chair-side or floor arrangement"),
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
                            }
                        }
                    }
                    if (index < plan.placements.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 20.dp))
                }
            }
        }
    }
    Text("Your first pause", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    Text("Sit beside the space for two quiet minutes. Notice the leaves, light, and your breathing—nothing to complete.", fontSize = 17.sp, lineHeight = 25.sp)
    NavigationActions(state, onIntent, continueLabel = "Save my corner")
}

@Composable
private fun SavedStage(state: RestorativeUiState) {
    Spacer(Modifier.height(40.dp))
    Text("Saved", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
    Text("Your restorative corner has a starting point", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
    Text("You can return here when you’re ready to place the plants. Start with one small move; the space does not need to be perfect.", fontSize = 18.sp, lineHeight = 27.sp)
    state.plan?.let { plan ->
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Your plan", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text(plan.plants.joinToString("  •  ") { it.canonicalName }, fontSize = 16.sp, lineHeight = 24.sp)
            }
        }
    }
}

@Composable
private fun TerminalEvidenceStage(error: RestorativeError.TerminalEvidenceFailure) {
    var showResearcherDetails by rememberSaveable { mutableStateOf(false) }
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
private fun ErrorNotice(error: RestorativeError) {
    val message = when (error) {
        RestorativeError.NoMatch -> "We couldn’t find a simple match yet. Go back and try another light or space option."
        is RestorativeError.PersistenceFailure -> "Your progress could not be saved just now. Please try again."
        is RestorativeError.TerminalEvidenceFailure -> "Your saved validation record cannot be opened on this version."
        is RestorativeError.InternalFailure -> "Something interrupted this step. Please go back and try again."
    }
    Text(message, fontSize = 16.sp, color = MaterialTheme.colorScheme.error, lineHeight = 23.sp)
}

private fun PlacementRole.plainLanguage(): String = when (this) {
    PlacementRole.FLOOR_ANCHOR -> "Place on the floor to give the corner a calm visual anchor."
    PlacementRole.TABLETOP_ACCENT -> "Place within easy view on a table, shelf, or stand."
    PlacementRole.TRAILING_EDGE -> "Place on a raised edge so the leaves can soften the space."
}
