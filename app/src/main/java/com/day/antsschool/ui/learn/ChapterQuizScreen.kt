package com.day.antsschool.ui.learn

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.day.antsschool.network.dto.QuizResultResponse

private val XpPurple = Color(0xFF7C4DFF)
private val CorrectGreen = Color(0xFF4CAF50)
private val WrongRed = Color(0xFFF44336)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterQuizScreen(
    content: ChapterContent,
    quizResult: QuizResultResponse?,   // 서버 응답 (로그인 시 실제 XP/배지 표시용)
    onBack: () -> Unit,
    onComplete: () -> Unit,
    onSaveResult: ((correctCount: Int, totalQuestions: Int) -> Unit)? = null
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }
    var correctCount by remember { mutableIntStateOf(0) }
    var showResult by remember { mutableStateOf(false) }

    if (showResult) {
        QuizResultScreen(
            totalQuestions = content.quiz.size,
            correctCount = correctCount,
            serverResult = quizResult,
            onComplete = onComplete
        )
        return
    }

    val question = content.quiz[currentIndex]
    val isAnswered = selectedAnswer != null

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                title = {
                    Text(
                        text = "퀴즈 · ${content.chapterTitle}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    Text(
                        text = "${currentIndex + 1} / ${content.quiz.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LinearProgressIndicator(
                progress = { (currentIndex + 1).toFloat() / content.quiz.size },
                modifier = Modifier.fillMaxWidth(),
                color = XpPurple,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // 문제 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Text(
                    text = question.question,
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 28.sp
                )
            }

            // 보기 버튼들
            question.options.forEachIndexed { index, option ->
                AnswerButton(
                    text = option,
                    index = index,
                    selectedAnswer = selectedAnswer,
                    correctIndex = question.correctIndex,
                    isAnswered = isAnswered,
                    onClick = {
                        selectedAnswer = index
                        if (index == question.correctIndex) correctCount++
                    }
                )
            }

            // 정답 해설 + 다음 버튼
            if (isAnswered) {
                val isCorrect = selectedAnswer == question.correctIndex

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCorrect) CorrectGreen.copy(alpha = 0.1f)
                        else WrongRed.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = if (isCorrect) "✅" else "❌", fontSize = 18.sp)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (isCorrect) "정답!" else "오답!",
                                fontWeight = FontWeight.Bold,
                                color = if (isCorrect) CorrectGreen else WrongRed
                            )
                            Text(
                                text = question.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        if (currentIndex < content.quiz.size - 1) {
                            currentIndex++
                            selectedAnswer = null
                        } else {
                            onSaveResult?.invoke(correctCount, content.quiz.size)
                            showResult = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (currentIndex < content.quiz.size - 1) "다음 문제" else "결과 보기",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AnswerButton(
    text: String,
    index: Int,
    selectedAnswer: Int?,
    correctIndex: Int,
    isAnswered: Boolean,
    onClick: () -> Unit
) {
    val containerColor = when {
        !isAnswered -> MaterialTheme.colorScheme.surface
        index == correctIndex -> CorrectGreen.copy(alpha = 0.12f)
        index == selectedAnswer -> WrongRed.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when {
        !isAnswered -> MaterialTheme.colorScheme.outline
        index == correctIndex -> CorrectGreen
        index == selectedAnswer -> WrongRed
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }
    val textColor = when {
        !isAnswered -> MaterialTheme.colorScheme.onSurface
        index == correctIndex -> CorrectGreen
        index == selectedAnswer -> WrongRed
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    }

    Card(
        onClick = onClick,
        enabled = !isAnswered,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.5.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            fontWeight = if (index == correctIndex && isAnswered) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// 배지 ID → (이모지, 이름)
private val BADGE_LABEL = mapOf(
    "FIRST_STUDY"    to ("🎓" to "첫 걸음"),
    "PERFECT_QUIZ"   to ("💯" to "완벽 점수"),
    "STREAK_7"       to ("🔥" to "7일 연속"),
    "INTRO_COMPLETE" to ("🏅" to "입문 완료")
)

@Composable
private fun QuizResultScreen(
    totalQuestions: Int,
    correctCount: Int,
    serverResult: QuizResultResponse?,
    onComplete: () -> Unit
) {
    // 서버 응답이 있으면 실제 XP, 없으면 로컬 계산
    val xpEarned = serverResult?.xpEarned ?: (correctCount * 10)
    val isPerfect = correctCount == totalQuestions
    val newBadges = serverResult?.newBadges ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = if (isPerfect) "🎉" else "📝", fontSize = 72.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isPerfect) "완벽해요!" else "잘 했어요!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$correctCount / $totalQuestions 정답",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        // XP 획득 카드
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = XpPurple.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "⭐", fontSize = 24.sp)
                Text(
                    text = "+${xpEarned} XP 획득!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = XpPurple
                )
            }
        }

        // 신규 배지 표시
        if (newBadges.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            newBadges.forEach { badgeId ->
                val (emoji, name) = BADGE_LABEL[badgeId] ?: ("🏆" to badgeId)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = emoji, fontSize = 22.sp)
                        Column {
                            Text(
                                text = "새 배지 획득!",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = XpPurple)
        ) {
            Text("학습 목록으로", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
