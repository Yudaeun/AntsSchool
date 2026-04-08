package com.day.antsschool.ui.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.day.antsschool.network.dto.AnswerDetail
import com.day.antsschool.network.dto.DailyAnswer
import com.day.antsschool.network.dto.DailyQuizResultResponse
import com.day.antsschool.network.dto.QuizQuestionResponse
import com.day.antsschool.ui.common.AdBanner

private val XpPurple = Color(0xFF7C4DFF)
private val AiGold = Color(0xFFFF8F00)
private val CorrectGreen = Color(0xFF4CAF50)
private val WrongRed = Color(0xFFF44336)

private sealed class QuizRoute {
    object Home : QuizRoute()
    data class Play(
        val questions: List<QuizQuestionResponse>,
        val isReview: Boolean,
        val isAiQuiz: Boolean = false
    ) : QuizRoute()
    data class Result(val result: DailyQuizResultResponse) : QuizRoute()
}

@Composable
fun QuizScreen(
    modifier: Modifier = Modifier,
    uid: String?
) {
    val viewModel: QuizViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var route by remember { mutableStateOf<QuizRoute>(QuizRoute.Home) }

    LaunchedEffect(uid) { viewModel.load(uid) }

    // 퀴즈 제출 완료 → 결과 화면
    LaunchedEffect(uiState.lastResult) {
        uiState.lastResult?.let { route = QuizRoute.Result(it) }
    }

    when (val r = route) {
        is QuizRoute.Home -> QuizHomeScreen(
            modifier = modifier,
            uiState = uiState,
            onStartDaily = { route = QuizRoute.Play(uiState.dailyQuestions, isReview = false) },
            onStartReview = { route = QuizRoute.Play(uiState.reviewQuestions, isReview = true) }
        )
        is QuizRoute.Play -> QuizPlayScreen(
            modifier = modifier,
            questions = r.questions,
            isReview = r.isReview,
            isAiQuiz = r.isAiQuiz,
            submitting = uiState.submitting,
            onSubmit = { answers ->
                when {
                    r.isAiQuiz -> viewModel.gradeAiQuiz(r.questions, answers)
                    uid != null -> viewModel.submitQuiz(uid, answers)
                    else -> viewModel.gradeAiQuiz(r.questions, answers) // 비로그인 시 로컬 채점으로 결과 표시
                }
            },
            onBack = { route = QuizRoute.Home }
        )
        is QuizRoute.Result -> QuizResultScreen(
            modifier = modifier,
            result = r.result,
            onDone = {
                viewModel.clearResult()
                viewModel.load(uid)
                route = QuizRoute.Home
            }
        )
    }
}

// ── 퀴즈 홈 ──────────────────────────────────────────────────────

@Composable
private fun QuizHomeScreen(
    modifier: Modifier = Modifier,
    uiState: QuizUiState,
    onStartDaily: () -> Unit,
    onStartReview: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("퀴즈", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        if (uiState.isLoading) {
            item {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            return@LazyColumn
        }

        // 데일리 퀴즈
        item {
            QuizTypeCard(
                emoji = "📅",
                title = "데일리 퀴즈",
                description = "오늘의 퀴즈 ${uiState.dailyQuestions.size}문제",
                buttonLabel = if (uiState.alreadyDoneToday) "오늘 완료!" else "시작하기",
                enabled = !uiState.alreadyDoneToday && uiState.dailyQuestions.isNotEmpty(),
                containerColor = XpPurple,
                onClick = onStartDaily
            )
        }

        // 복습 퀴즈
        item {
            QuizTypeCard(
                emoji = "📝",
                title = "복습 퀴즈",
                description = if (uiState.reviewQuestions.isEmpty()) "오답노트가 비어있어요"
                              else "틀린 문제 ${uiState.reviewQuestions.size}개",
                buttonLabel = "복습하기",
                enabled = uiState.reviewQuestions.isNotEmpty(),
                containerColor = Color(0xFF00897B),
                onClick = onStartReview
            )
        }

        // AI 퀴즈 — 준비 중 안내 카드
        item { AiQuizComingSoonCard() }

        // 하단 광고 배너
        item { AdBanner() }
    }
}

@Composable
private fun AiQuizComingSoonCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AiGold.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("✨", fontSize = 40.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text("AI 퀴즈", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Claude AI 퀴즈는 곧 출시됩니다!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AiGold.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    "준비중",
                    style = MaterialTheme.typography.labelMedium,
                    color = AiGold,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun QuizTypeCard(
    emoji: String,
    title: String,
    description: String,
    buttonLabel: String,
    enabled: Boolean,
    containerColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = emoji, fontSize = 40.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = onClick,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(containerColor = containerColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(buttonLabel, fontSize = 13.sp)
            }
        }
    }
}

// ── 퀴즈 플레이 ───────────────────────────────────────────────────

@Composable
private fun QuizPlayScreen(
    modifier: Modifier = Modifier,
    questions: List<QuizQuestionResponse>,
    isReview: Boolean,
    isAiQuiz: Boolean,
    submitting: Boolean,
    onSubmit: (List<DailyAnswer>) -> Unit,
    onBack: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var answered by remember { mutableStateOf(false) }
    var combo by remember { mutableIntStateOf(0) }
    val answers = remember { mutableStateListOf<DailyAnswer>() }

    if (questions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("문제가 없어요") }
        return
    }

    val question = questions[currentIndex]
    val progress = (currentIndex + 1).toFloat() / questions.size
    val accentColor = if (isAiQuiz) AiGold else XpPurple

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 진행률 바
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = accentColor
                )
                Text(
                    "${currentIndex + 1}/${questions.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isAiQuiz) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AiGold.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("✨ AI 생성 퀴즈", style = MaterialTheme.typography.labelSmall, color = AiGold, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(question.question, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, lineHeight = 30.sp)

            question.options.forEachIndexed { index, option ->
                val bgColor = when {
                    !answered -> if (selectedIndex == index) accentColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                    index == question.correctIndex -> CorrectGreen.copy(alpha = 0.12f)
                    index == selectedIndex && selectedIndex != question.correctIndex -> WrongRed.copy(alpha = 0.12f)
                    else -> MaterialTheme.colorScheme.surface
                }
                val borderColor = when {
                    !answered && selectedIndex == index -> accentColor
                    answered && index == question.correctIndex -> CorrectGreen
                    answered && index == selectedIndex && selectedIndex != question.correctIndex -> WrongRed
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                        .clickable(enabled = !answered) { selectedIndex = index; answered = true }
                        .padding(16.dp)
                ) {
                    Text("${listOf("①","②","③","④")[index]} $option", style = MaterialTheme.typography.bodyLarge)
                }
            }

            if (answered) {
                // 콤보 표시 (2연속 이상 정답 시)
                val justCorrect = selectedIndex == question.correctIndex
                if (justCorrect && combo >= 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CorrectGreen.copy(alpha = 0.12f))
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🔥 ${combo + 1}연속 정답!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CorrectGreen
                        )
                    }
                }
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Text("💡 ${question.explanation}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(14.dp), lineHeight = 22.sp)
                }
            }
        }

        if (answered) {
            Button(
                onClick = {
                    val sel = selectedIndex ?: return@Button
                    val isCorrect = sel == question.correctIndex
                    if (isCorrect) combo++ else combo = 0
                    answers.add(DailyAnswer(questionId = question.id, selectedIndex = sel))
                    if (currentIndex < questions.size - 1) {
                        currentIndex++; selectedIndex = null; answered = false
                    } else {
                        onSubmit(answers.toList())
                    }
                },
                enabled = !submitting,
                modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 20.dp, vertical = 10.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                if (submitting) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                else Text(
                    if (currentIndex < questions.size - 1) "다음" else "결과 보기",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── 퀴즈 결과 ─────────────────────────────────────────────────────

@Composable
private fun QuizResultScreen(
    modifier: Modifier = Modifier,
    result: DailyQuizResultResponse,
    onDone: () -> Unit
) {
    val isPerfect = result.correctCount == result.totalQuestions

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Text(if (isPerfect) "🎉" else "📝", fontSize = 64.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "${result.correctCount} / ${result.totalQuestions} 정답",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier.clip(RoundedCornerShape(50)).background(XpPurple.copy(alpha = 0.1f)).padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text("+${result.xpEarned} XP 획득!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = XpPurple)
            }
        }

        if (result.newBadges.isNotEmpty()) {
            item {
                result.newBadges.forEach { badgeId ->
                    val (emoji, name) = badgeLabel(badgeId)
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(emoji, fontSize = 22.sp)
                            Column {
                                Text("새 배지 획득!", style = MaterialTheme.typography.labelSmall)
                                Text(name, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        item {
            Text("문제별 결과", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        }
        items(result.details) { detail -> AnswerDetailCard(detail) }

        item {
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = XpPurple)
            ) {
                Text("퀴즈 홈으로", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AnswerDetailCard(detail: AnswerDetail) {
    val color = if (detail.isCorrect) CorrectGreen else WrongRed
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.07f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (detail.isCorrect) "✅" else "❌", fontSize = 16.sp)
                Text(detail.question, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            }
            if (!detail.isCorrect) {
                Spacer(Modifier.height(6.dp))
                Text("정답: ${detail.options.getOrNull(detail.correctIndex) ?: ""}", style = MaterialTheme.typography.bodySmall, color = CorrectGreen, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(4.dp))
            Text(detail.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun badgeLabel(badgeId: String): Pair<String, String> = when (badgeId) {
    "FIRST_STUDY"    -> "🎓" to "첫 걸음"
    "PERFECT_QUIZ"   -> "💯" to "완벽 점수"
    "STREAK_7"       -> "🔥" to "7일 연속"
    "INTRO_COMPLETE" -> "🏅" to "입문 완료"
    else             -> "🏆" to badgeId
}
