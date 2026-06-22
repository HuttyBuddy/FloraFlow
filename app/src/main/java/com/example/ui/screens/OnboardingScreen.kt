package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.GardenViewModel
import kotlinx.coroutines.delay
import androidx.compose.foundation.Image
import com.example.R

enum class AssessmentScreenState {
    SPLASH, QUESTION, CALCULATING, RESULT, STEPS
}

data class AssessmentQuestion(
    val category: String,
    val text: String,
    val options: List<String> = listOf("Never or rarely", "Sometimes", "Always or almost always")
)

data class NextStepInfo(
    val category: String,
    val title: String,
    val detail: String,
    val cta: String,
    val targetTab: Int
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: GardenViewModel,
    modifier: Modifier = Modifier
) {
    var screenState by remember { mutableStateOf(AssessmentScreenState.SPLASH) }
    var currentQuestionIdx by remember { mutableIntStateOf(0) }
    val answers = remember { mutableStateMapOf<Int, Int>() } // question index to score (0, 1, or 2)

    val questions = remember {
        listOf(
            AssessmentQuestion("NATURE VIEWS", "I can see trees, plants, or open sky from where I most often sit or work."),
            AssessmentQuestion("LIVING PLANTS", "There are living plants within my immediate indoor workspace or living area."),
            AssessmentQuestion("NATURAL LIGHT", "My primary space is illuminated by natural daylight rather than artificial light."),
            AssessmentQuestion("ACOUSTIC CALM", "My space is free from disruptive background noise (traffic, hums) and feels acoustically calm."),
            AssessmentQuestion("NATURAL MATERIALS", "I am surrounded by natural materials like wood, stone, wool, or clay in my space."),
            AssessmentQuestion("AIR & VENTILATION", "I feel a gentle breeze or have access to fresh outdoor air circulation in my room."),
            AssessmentQuestion("ORGANIC FORMS", "My furniture or decor features curved, organic shapes and patterns instead of sharp, rigid angles."),
            AssessmentQuestion("WATER FEATURES", "I can see or hear water (such as a fountain, rain, or stream) in or near my space."),
            AssessmentQuestion("SENSORY RICHNESS", "My space includes natural scents (like wood, soil, or flowers) or tactile natural textures."),
            AssessmentQuestion("SEASONAL AWARENESS", "I feel connected to the current season and weather changes from inside my space.")
        )
    }

    val totalScore = remember(answers.size, screenState) {
        answers.values.sum()
    }

    val lowestCategories = remember(answers.size, screenState) {
        questions.mapIndexed { idx, q -> q.category to (answers[idx] ?: 0) }
            .sortedBy { it.second }
            .map { it.first }
    }

    val stepsMapping = remember {
        mapOf(
            "NATURE VIEWS" to NextStepInfo(
                "NATURE VIEWS",
                "Optimize your outdoor view",
                "You scored low on Nature Views. Clear window blockages or place plants in your direct line of sight to simulate natural depth.",
                "Design my layout →",
                1
            ),
            "LIVING PLANTS" to NextStepInfo(
                "LIVING PLANTS",
                "Add living material to your work area",
                "You scored 0 on Living Plants. Adding 2-3 plants to your primary space is the single highest-impact change for your score.",
                "Find plants for my space →",
                3
            ),
            "NATURAL LIGHT" to NextStepInfo(
                "NATURAL LIGHT",
                "Reposition toward natural light",
                "You scored 1 on Natural Light. Even partial repositioning toward a window helps lower stress and restore calm.",
                "Design my layout →",
                1
            ),
            "ACOUSTIC CALM" to NextStepInfo(
                "ACOUSTIC CALM",
                "Introduce acoustic masking",
                "You scored low on Acoustic Calm. Mask distracting background noise to quiet your mind.",
                "Find soothing soundscapes →",
                3
            ),
            "NATURAL MATERIALS" to NextStepInfo(
                "NATURAL MATERIALS",
                "Introduce one natural texture",
                "You scored 0 on Natural Materials. A wood surface, woven rug, or stone object changes your sensory baseline immediately.",
                "Browse material ideas →",
                2
            ),
            "AIR & VENTILATION" to NextStepInfo(
                "AIR & VENTILATION",
                "Enhance active airflow",
                "You scored low on Air & Ventilation. Open windows for 10 minutes twice daily, or use a gentle oscillating fan to mimic natural wind.",
                "Ask Advisor for advice →",
                3
            ),
            "ORGANIC FORMS" to NextStepInfo(
                "ORGANIC FORMS",
                "Introduce organic patterns",
                "You scored low on Organic Forms. Incorporate curved decor or botanical prints to soften sharp, institutional room angles.",
                "Browse decoration ideas →",
                2
            ),
            "WATER FEATURES" to NextStepInfo(
                "WATER FEATURES",
                "Add sound of moving water",
                "You scored low on Water Features. A small tabletop fountain or rain sound machine helps soothe stress and slow down a racing mind.",
                "Explore water elements →",
                3
            ),
            "SENSORY RICHNESS" to NextStepInfo(
                "SENSORY RICHNESS",
                "Stimulate with natural scents",
                "You scored low on Sensory Richness. Use natural cedarwood, pine, or lavender oils to signal calm and safety to your brain.",
                "Get aromatic tips →",
                3
            ),
            "SEASONAL AWARENESS" to NextStepInfo(
                "SEASONAL AWARENESS",
                "Align with current season",
                "You scored low on Seasonal Awareness. Bring seasonal flowers indoors or adjust light cycles to stay synced with external rhythms.",
                "Browse seasonal plants →",
                2
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1D3C28)) // Dark green default
    ) {
        AnimatedContent(
            targetState = screenState,
            transitionSpec = {
                fadeIn(animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium)) togetherWith fadeOut(animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium))
            },
            label = "AssessmentFlowAnimation"
        ) { state ->
            when (state) {
                AssessmentScreenState.SPLASH -> {
                    SplashWelcomeScreen(
                        onStart = {
                            answers.clear()
                            currentQuestionIdx = 0
                            screenState = AssessmentScreenState.QUESTION
                        },
                        onSkip = { viewModel.skipAssessment() }
                    )
                }
                AssessmentScreenState.QUESTION -> {
                    QuestionFlowScreen(
                        questions = questions,
                        currentIndex = currentQuestionIdx,
                        onAnswer = { score ->
                            answers[currentQuestionIdx] = score
                            if (currentQuestionIdx < questions.size - 1) {
                                currentQuestionIdx++
                            } else {
                                screenState = AssessmentScreenState.CALCULATING
                            }
                        },
                        onBack = {
                            if (currentQuestionIdx > 0) {
                                currentQuestionIdx--
                            } else {
                                screenState = AssessmentScreenState.SPLASH
                            }
                        }
                    )
                }
                AssessmentScreenState.CALCULATING -> {
                    CalculatingScreen(
                        onComplete = {
                            screenState = AssessmentScreenState.RESULT
                        }
                    )
                }
                AssessmentScreenState.RESULT -> {
                    ResultScreen(
                        score = totalScore,
                        onSeeSteps = {
                            screenState = AssessmentScreenState.STEPS
                        }
                    )
                }
                AssessmentScreenState.STEPS -> {
                    StepsScreen(
                        lowestCategories = lowestCategories,
                        stepsMapping = stepsMapping,
                        onFinish = { targetTab ->
                            viewModel.saveAssessmentResult(totalScore, lowestCategories.take(3))
                            viewModel.setCurrentTab(targetTab)
                            viewModel.completeOnboarding()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SplashWelcomeScreen(
    onStart: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        // centered FloraFlow logo top
        Image(
            painter = painterResource(id = R.drawable.ic_logo_heart),
            contentDescription = "FloraFlow Logo",
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(24.dp))
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "FloraFlow",
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Cultivating Calm through Mindful Gardening",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            text = "How much stress is your space creating?",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE8C998), // sandy warm cream
                lineHeight = 40.sp,
                textAlign = TextAlign.Center
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Take a 2-minute Neural Load assessment — find out if your environment is helping or hurting your nervous system.",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            ),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onStart,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE8C998),
                contentColor = Color(0xFF1D3C28)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("onboarding_start_assessment_btn")
        ) {
            Text(
                text = "Start My Assessment",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Skip for now →",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.7f)
            ),
            modifier = Modifier
                .clickable { onSkip() }
                .padding(8.dp)
                .testTag("onboarding_skip_assessment_btn")
        )
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun QuestionFlowScreen(
    questions: List<AssessmentQuestion>,
    currentIndex: Int,
    onAnswer: (Int) -> Unit,
    onBack: () -> Unit
) {
    val currentQuestion = questions[currentIndex]
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        // Top Bar: Back navigation & 10-segmented progress bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 10 segments progress bar
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (i in 0 until questions.size) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                if (i <= currentIndex) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Question ${currentIndex + 1} of 10",
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Category Badge
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = currentQuestion.category,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.ExtraBold
                )
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Question Text
        Text(
            text = currentQuestion.text,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = 32.sp
            ),
            modifier = Modifier.weight(1f)
        )
        
        // Options full-width touch targets
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            currentQuestion.options.forEachIndexed { score, text ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAnswer(score) }
                        .testTag("question_${currentIndex}_option_$score"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = borderStroke()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun borderStroke() = androidx.compose.foundation.BorderStroke(
    width = 1.dp,
    color = MaterialTheme.colorScheme.outlineVariant
)

@Composable
fun CalculatingScreen(
    onComplete: () -> Unit
) {
    var textToShow by remember { mutableStateOf("Analyzing your environment...") }
    
    LaunchedEffect(Unit) {
        delay(800)
        textToShow = "Calculating Neural Load..."
        delay(800)
        textToShow = "Generating your results..."
        delay(900)
        onComplete()
    }
    
    // pulsing leaf animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulsing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1D3C28)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_logo_heart),
                contentDescription = "FloraFlow Logo",
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.CenterHorizontally)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .clip(RoundedCornerShape(20.dp))
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = textToShow,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f)
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ResultScreen(
    score: Int,
    onSeeSteps: () -> Unit
) {
    val zoneInfo = remember(score) {
        when (score) {
            in 15..20 -> Triple("GREEN ZONE", "LOW NEURAL LOAD", Color(0xFF1B4A2F))
            in 8..14 -> Triple("YELLOW ZONE", "MODERATE NEURAL LOAD", Color(0xFF825E1B))
            else -> Triple("RED ZONE", "HIGH NEURAL LOAD", Color(0xFF702123))
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(zoneInfo.third)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "Your Neural Load Score",
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.7f)
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Large score number 48pt+ bold
        Text(
            text = "$score / 20",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 56.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        )

        val context = androidx.compose.ui.platform.LocalContext.current
        val sharedPrefs = remember { context.getSharedPreferences("floraflow_prefs", android.content.Context.MODE_PRIVATE) }
        val prevScore = remember { sharedPrefs.getInt("prev_assessment_score", -1) }

        if (prevScore != -1) {
            val delta = score - prevScore
            val textDelta = if (delta > 0) {
                "Your Neural Load improved from $prevScore/20 to $score/20 (+$delta)!"
            } else if (delta < 0) {
                "Your Neural Load went from $prevScore/20 to $score/20 ($delta)."
            } else {
                "Your Neural Load remained at $score/20."
            }
            Text(
                text = textDelta,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.95f)
                ),
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Zone label badge
        Box(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "${zoneInfo.first} — ${zoneInfo.second}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = when (score) {
                in 15..20 -> "Your environment is highly supportive of your nervous system. Biophilic cues are abundant, promoting natural calm, focus, and restoration. Maintain this healthy balance!"
                in 8..14 -> "Your space has a few natural elements missing, which might be quietly draining your energy and focus. The good news is that small, simple changes can make a big difference."
                else -> "Your space might be adding to your daily stress. Without enough natural light, plants, or fresh air, it's easy to feel tired and unfocused. Let's make a few simple adjustments to turn your room into a restorative sanctuary."
            },
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            ),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onSeeSteps,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = zoneInfo.third
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("result_see_steps_btn")
        ) {
            Text(
                text = "See My 3 Next Steps",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Fake share button
        var showShareToast by remember { mutableStateOf(false) }
        
        OutlinedButton(
            onClick = { showShareToast = true },
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White
            ),
            border = borderStrokeShare(),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("result_share_score_btn")
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    text = "Share My Score",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
        
        if (showShareToast) {
            AlertDialog(
                onDismissRequest = { showShareToast = false },
                confirmButton = {
                    TextButton(onClick = { showShareToast = false }) {
                        Text("Close", color = Color(0xFF1D3C28))
                    }
                },
                title = { Text("Share Score Card", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(zoneInfo.third, RoundedCornerShape(16.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_logo_heart),
                                    contentDescription = "FloraFlow Logo",
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("FloraFlow Neural Load", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("$score / 20", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(zoneInfo.second, color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Take your own assessment: floraflow.app", color = Color(0xFFE8C998), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Card generated successfully. Click share to post!", textAlign = TextAlign.Center, fontSize = 12.sp)
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun borderStrokeShare() = androidx.compose.foundation.BorderStroke(
    width = 1.dp,
    color = Color.White.copy(alpha = 0.5f)
)

@Composable
fun StepsScreen(
    lowestCategories: List<String>,
    stepsMapping: Map<String, NextStepInfo>,
    onFinish: (Int) -> Unit
) {
    // Take the 3 lowest categories that have steps mapped
    val finalSteps = remember(lowestCategories) {
        lowestCategories.mapNotNull { stepsMapping[it] }.take(3)
    }
    
    var expandedIndex by remember { mutableIntStateOf(0) } // initially expand step 1
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Text(
            text = "Your Personalized Next Steps",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                lineHeight = 32.sp
            ),
            modifier = Modifier.padding(top = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Click a step below to expand details and begin styling your environment.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            finalSteps.forEachIndexed { index, step ->
                val isExpanded = expandedIndex == index
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedIndex = if (isExpanded) -1 else index }
                        .testTag("step_card_$index"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isExpanded) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = borderStroke()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Numbered Badge
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                
                                Text(
                                    text = step.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse details" else "Expand details",
                                tint = Color(0xFF1D3C28)
                            )
                        }
                        
                        AnimatedVisibility(visible = isExpanded) {
                            Column {
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = step.detail,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 22.sp
                                    )
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Button(
                                    onClick = { onFinish(step.targetTab) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("step_card_${index}_cta")
                                ) {
                                    Text(
                                        text = step.cta,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // general Skip/Finish button
        TextButton(
            onClick = { onFinish(0) }, // Default to home (tab 0)
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .testTag("steps_finish_all_btn")
        ) {
            Text(
                text = "Go to Floral Space Dashboard",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
