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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.GardenViewModel
import com.example.ui.components.FloraFlowButton
import com.example.ui.components.FloraFlowCard
import com.example.ui.components.ButtonVariant
import com.example.ui.theme.BiophilicPrimary
import com.example.ui.theme.BiophilicSecondary
import kotlinx.coroutines.delay
import androidx.compose.foundation.Image
import android.app.Activity
import com.example.R
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.ui.graphics.Brush

enum class AssessmentScreenState {
    SPLASH, QUESTION, CALCULATING, RESULT, PERSONALIZED_PAYWALL, STEPS
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

    val categoryScores = remember(answers.size, screenState) {
        val scores = mutableMapOf<String, Int>()
        questions.forEachIndexed { index, question ->
            val valScore = answers[index] ?: 0
            scores[question.category] = (scores[question.category] ?: 0) + valScore
        }
        scores
    }

    val lowestCategories = remember(categoryScores) {
        categoryScores.toList()
            .sortedBy { it.second }
            .map { it.first }
    }

    val totalScore = remember(answers.size, screenState) {
        answers.values.sum()
    }

    val stepsMapping = remember(categoryScores) {
        mapOf(
            "NATURE VIEWS" to NextStepInfo(
                "NATURE VIEWS",
                "Optimize your outdoor view",
                "You scored ${categoryScores["NATURE VIEWS"] ?: 0} on Nature Views. Clear window blockages or place plants in your direct line of sight to simulate natural depth.",
                "Design my layout →",
                1
            ),
            "LIVING PLANTS" to NextStepInfo(
                "LIVING PLANTS",
                "Add living material to your work area",
                "You scored ${categoryScores["LIVING PLANTS"] ?: 0} on Living Plants. Adding 2-3 plants to your primary space is the single highest-impact change for your score.",
                "Find plants for my space →",
                3
            ),
            "NATURAL LIGHT" to NextStepInfo(
                "NATURAL LIGHT",
                "Reposition toward natural light",
                "You scored ${categoryScores["NATURAL LIGHT"] ?: 0} on Natural Light. Even partial repositioning toward a window helps lower stress and restore calm.",
                "Design my layout →",
                1
            ),
            "ACOUSTIC CALM" to NextStepInfo(
                "ACOUSTIC CALM",
                "Introduce acoustic masking",
                "You scored ${categoryScores["ACOUSTIC CALM"] ?: 0} on Acoustic Calm. Mask distracting background noise to quiet your mind.",
                "Find soothing soundscapes →",
                3
            ),
            "NATURAL MATERIALS" to NextStepInfo(
                "NATURAL MATERIALS",
                "Introduce one natural texture",
                "You scored ${categoryScores["NATURAL MATERIALS"] ?: 0} on Natural Materials. A wood surface, woven rug, or stone object changes your sensory baseline immediately.",
                "Browse material ideas →",
                2
            ),
            "AIR & VENTILATION" to NextStepInfo(
                "AIR & VENTILATION",
                "Enhance active airflow",
                "You scored ${categoryScores["AIR & VENTILATION"] ?: 0} on Air & Ventilation. Open windows for 10 minutes twice daily, or use a gentle oscillating fan to mimic natural wind.",
                "Ask Advisor for advice →",
                3
            ),
            "ORGANIC FORMS" to NextStepInfo(
                "ORGANIC FORMS",
                "Introduce organic patterns",
                "You scored ${categoryScores["ORGANIC FORMS"] ?: 0} on Organic Forms. Incorporate curved decor or botanical prints to soften sharp, institutional room angles.",
                "Browse decoration ideas →",
                2
            ),
            "WATER FEATURES" to NextStepInfo(
                "WATER FEATURES",
                "Add sound of moving water",
                "You scored ${categoryScores["WATER FEATURES"] ?: 0} on Water Features. A small tabletop fountain or rain sound machine helps soothe stress and slow down a racing mind.",
                "Explore water elements →",
                3
            ),
            "SENSORY RICHNESS" to NextStepInfo(
                "SENSORY RICHNESS",
                "Stimulate with natural scents",
                "You scored ${categoryScores["SENSORY RICHNESS"] ?: 0} on Sensory Richness. Use natural cedarwood, pine, or lavender oils to signal calm and safety to your brain.",
                "Get aromatic tips →",
                3
            ),
            "SEASONAL AWARENESS" to NextStepInfo(
                "SEASONAL AWARENESS",
                "Align with current season",
                "You scored ${categoryScores["SEASONAL AWARENESS"] ?: 0} on Seasonal Awareness. Bring seasonal flowers indoors or adjust light cycles to stay synced with external rhythms.",
                "Browse seasonal plants →",
                2
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BiophilicPrimary) // Soothing forest green brand color
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
                        onAnswer = { score: Int ->
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
                            if (totalScore < 15) {
                                screenState = AssessmentScreenState.PERSONALIZED_PAYWALL
                            } else {
                                screenState = AssessmentScreenState.STEPS
                            }
                        }
                    )
                }
                AssessmentScreenState.PERSONALIZED_PAYWALL -> {
                    PersonalizedPaywallScreen(
                        score = totalScore,
                        lowestCategories = lowestCategories,
                        onUpgradeClick = { isAnnual ->
                            viewModel.setBillingDialogVisible(true)
                            screenState = AssessmentScreenState.STEPS
                        },
                        onBypassClick = {
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
                color = BiophilicSecondary, // Soft Gold/Amber accent
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
        
        FloraFlowButton(
            text = "Start My Assessment",
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_start_assessment_btn")
        )
        
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
                FloraFlowCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAnswer(score) }
                        .testTag("question_${currentIndex}_option_$score"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    elevation = 2.dp,
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
            .background(BiophilicPrimary),
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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
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
                text = "See My Next Steps",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = {
                shareScoreCard(context, score, zoneInfo.first, zoneInfo.second, zoneInfo.third.toArgb())
            },
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
                    text = "Share My Score Card",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
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
fun PersonalizedPaywallScreen(
    score: Int,
    lowestCategories: List<String>,
    onUpgradeClick: (isAnnual: Boolean) -> Unit,
    onBypassClick: () -> Unit
) {
    val zoneInfo = remember(score) {
        when (score) {
            in 15..20 -> Triple("GREEN ZONE", "LOW NEURAL LOAD", Color(0xFF1B4A2F))
            in 8..14 -> Triple("YELLOW ZONE", "MODERATE NEURAL LOAD", Color(0xFF825E1B))
            else -> Triple("RED ZONE", "HIGH NEURAL LOAD", Color(0xFF702123))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        zoneInfo.third,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = "Premium Benefit",
                tint = Color(0xFFFFD54F),
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Your Personalized Restoration Plan",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Based on your score of $score/20 (${zoneInfo.second}), we have generated a customized environment restoration plan targeting your lowest areas:",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.85f)
                ),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            lowestCategories.take(3).forEach { category ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Target",
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Focus: $category",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = when (category) {
                                    "NATURAL LIGHT" -> "Critical light deprivation. Unlock tailored light-boost layouts to boost your melatonin & circadian rhythm."
                                    "LIVING PLANTS" -> "Low biophilic plant density. Unlock low-maintenance botanical layouts to oxygenate your room."
                                    "NOISE" -> "High cognitive noise pollution. Unlock custom binaural soundscapes to decrease cortisol levels."
                                    "NATURE VIEWS" -> "Sparse nature connectivity. Unlock spatial layouts designed to maximize nature views."
                                    else -> "Artificial texture dominance. Unlock recommendations for biophilic materials and natural fibers."
                                },
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            FloraFlowButton(
                text = "Start 14-Day Free Trial",
                onClick = { onUpgradeClick(true) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = Icons.Default.AutoAwesome
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(
                onClick = onBypassClick,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Continue with Basic Free Tips",
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

fun shareScoreCard(context: android.content.Context, score: Int, zoneName: String, zoneDesc: String, bgColorInt: Int) {
    val size = 512
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    
    // Draw background color
    canvas.drawColor(bgColorInt)
    
    // Draw decorative border
    val borderPaint = android.graphics.Paint().apply {
        color = 0xFFFFFFFF.toInt()
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 10f
        isAntiAlias = true
    }
    canvas.drawRect(20f, 20f, size - 20f, size - 20f, borderPaint)
    
    // Draw title
    val titlePaint = android.graphics.Paint().apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 28f
        isFakeBoldText = true
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText("FloraFlow Neural Load", size / 2f, 100f, titlePaint)
    
    // Draw score number "X / 20"
    val scorePaint = android.graphics.Paint().apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 80f
        isFakeBoldText = true
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText("$score / 20", size / 2f, 240f, scorePaint)
    
    // Draw zone name
    val zonePaint = android.graphics.Paint().apply {
        color = 0xFFFFD54F.toInt() // Gold highlight
        textSize = 22f
        isFakeBoldText = true
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText(zoneName, size / 2f, 320f, zonePaint)

    // Draw zone description
    val descPaint = android.graphics.Paint().apply {
        color = 0xDDFFFFFF.toInt()
        textSize = 16f
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText(zoneDesc, size / 2f, 380f, descPaint)
    
    // Draw bottom branding
    val footerPaint = android.graphics.Paint().apply {
        color = 0x88FFFFFF.toInt()
        textSize = 14f
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText("Find your restoration score with FloraFlow", size / 2f, size - 60f, footerPaint)
    
    try {
        val cachePath = java.io.File(context.cacheDir, "shared_scores")
        cachePath.mkdirs()
        val file = java.io.File(cachePath, "onboarding_scorecard.png")
        val stream = java.io.FileOutputStream(file)
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()
        
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "My Biophilic Assessment Score")
            putExtra(android.content.Intent.EXTRA_TEXT, "I just assessed my room's environment using FloraFlow. My Neural Load Score is $score/20 ($zoneDesc). Optimize your environment with FloraFlow!")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Score Card"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Failed to share scorecard: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}

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
                
                FloraFlowCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedIndex = if (isExpanded) -1 else index }
                        .testTag("step_card_$index"),
                    containerColor = if (isExpanded) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
                    elevation = 2.dp,
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
                                
                                 FloraFlowButton(
                                    text = step.cta,
                                    onClick = { onFinish(step.targetTab) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("step_card_${index}_cta")
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // general Skip/Finish button
        FloraFlowButton(
            text = "Go to Floral Space Dashboard",
            onClick = { onFinish(0) },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .testTag("steps_finish_all_btn"),
            variant = ButtonVariant.Text
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
