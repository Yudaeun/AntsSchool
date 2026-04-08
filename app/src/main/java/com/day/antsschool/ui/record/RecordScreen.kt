package com.day.antsschool.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.day.antsschool.network.dto.UserProgressResponse
import java.time.LocalDate

private val XpPurple = Color(0xFF7C4DFF)
private val StreakOrange = Color(0xFFFF6B35)

// 배지 ID → (이모지, 이름, 설명)
private val BADGE_INFO = linkedMapOf(
    "FIRST_STUDY"           to Triple("🎓", "첫 걸음",     "첫 챕터를 완료했어요"),
    "STREAK_3"              to Triple("🔥", "3일 연속",    "3일 연속 출석했어요"),
    "STREAK_7"              to Triple("🌟", "7일 연속",    "7일 연속 출석했어요"),
    "STREAK_30"             to Triple("👑", "30일 연속",   "30일 연속 출석했어요"),
    "PERFECT_QUIZ"          to Triple("💯", "완벽 점수",   "퀴즈를 전부 맞혔어요"),
    "CHAPTER_10"            to Triple("📚", "열공러",      "챕터 10개를 완료했어요"),
    "INTRO_COMPLETE"        to Triple("🏅", "입문 완료",   "입문 단계를 모두 마쳤어요"),
    "BEGINNER_COMPLETE"     to Triple("🥈", "초급 완료",   "초급 단계를 모두 마쳤어요"),
    "INTERMEDIATE_COMPLETE" to Triple("🥇", "중급 완료",   "중급 단계를 모두 마쳤어요"),
    "ADVANCED_COMPLETE"     to Triple("🏆", "고급 완료",   "고급 단계를 모두 마쳤어요")
)

@Composable
fun RecordScreen(
    modifier: Modifier = Modifier,
    userProgress: UserProgressResponse?
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "내 기록",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        item { LevelCard(userProgress) }
        item { StreakCard(userProgress) }
        item { CalendarCard(userProgress?.checkinDates ?: emptyList()) }
        item { AccuracyCard(userProgress?.totalCorrect ?: 0, userProgress?.totalAnswered ?: 0) }
        item { BadgeSection(userProgress?.badges ?: emptyList()) }
    }
}

@Composable
private fun LevelCard(userProgress: UserProgressResponse?) {
    val level = userProgress?.level ?: 1
    val xp = userProgress?.xp ?: 0
    val xpInLevel = xp % 100
    val xpPercent = (xpInLevel / 100f).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = XpPurple.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "레벨",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Lv.$level",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = XpPurple
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "누적 경험치",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$xp XP",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "다음 레벨까지 $xpInLevel / 100 XP",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(XpPurple.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(xpPercent)
                        .clip(RoundedCornerShape(5.dp))
                        .background(XpPurple)
                )
            }
        }
    }
}

@Composable
private fun StreakCard(userProgress: UserProgressResponse?) {
    val streak = userProgress?.streak ?: 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StreakOrange.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "🔥", fontSize = 40.sp)
            Column {
                Text(
                    text = "${streak}일 연속 출석",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (streak >= 7) "7일 스트릭 달성! 계속 유지해요"
                    else "${7 - streak}일 더 출석하면 배지를 받아요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CalendarCard(checkinDates: List<String>) {
    val today = remember { LocalDate.now() }
    // 이번 주 월요일 기준 5주(35일) — 헤더 "월화수목금토일"과 날짜 열이 항상 일치
    val startOfThisWeek = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val startDay = startOfThisWeek.minusWeeks(4)
    val days = (0 until 35).map { startDay.plusDays(it.toLong()) }
    val checkinSet = checkinDates.toHashSet()
    val dayLabels = listOf("월", "화", "수", "목", "금", "토", "일")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StreakOrange.copy(alpha = 0.07f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "출석 캘린더",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            // 요일 헤더
            Row(modifier = Modifier.fillMaxWidth()) {
                dayLabels.forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            // 날짜 셀 (5행 × 7열)
            days.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        val isToday = day == today
                        val isCheckin = day.toString() in checkinSet
                        val isFuture = day.isAfter(today)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(2.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isToday -> StreakOrange
                                        isCheckin -> StreakOrange.copy(alpha = 0.25f)
                                        else -> Color.Transparent
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isCheckin && !isToday) "🔥" else day.dayOfMonth.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = if (isCheckin && !isToday) 14.sp else 11.sp,
                                color = when {
                                    isToday -> Color.White
                                    isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                },
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccuracyCard(totalCorrect: Int, totalAnswered: Int) {
    val accuracy = if (totalAnswered > 0) totalCorrect.toFloat() / totalAnswered else 0f
    val accuracyPct = (accuracy * 100).toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = XpPurple.copy(alpha = 0.07f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "퀴즈 정답률",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "$accuracyPct%",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = XpPurple
                )
                Text(
                    text = "${totalCorrect}문제 정답 / ${totalAnswered}문제 풀이",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(XpPurple.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(accuracy.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(5.dp))
                        .background(XpPurple)
                )
            }
            if (totalAnswered == 0) {
                Text(
                    text = "아직 풀이한 퀴즈가 없어요. 퀴즈를 풀어보세요!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BadgeSection(earnedBadgeIds: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "배지",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${earnedBadgeIds.size} / ${BADGE_INFO.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 2열 배지 그리드
        BADGE_INFO.entries.toList().chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { (badgeId, info) ->
                    BadgeCard(
                        modifier = Modifier.weight(1f),
                        emoji = info.first,
                        name = info.second,
                        description = info.third,
                        earned = badgeId in earnedBadgeIds
                    )
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BadgeCard(
    modifier: Modifier = Modifier,
    emoji: String,
    name: String,
    description: String,
    earned: Boolean
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (earned) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = if (earned) emoji else "🔒", fontSize = 32.sp)
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (earned) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = if (earned) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        }
    }
}
