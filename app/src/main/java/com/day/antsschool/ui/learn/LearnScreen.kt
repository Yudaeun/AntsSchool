package com.day.antsschool.ui.learn

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.day.antsschool.ui.common.AdBanner

private val CompletedGreen = Color(0xFF4CAF50)
private val XpPurple = Color(0xFF7C4DFF)

// ── 학습 탭 내 내비게이션 라우트 ──────────────────────────────

sealed class LearnRoute {
    object ChapterList : LearnRoute()
    data class CardSlide(val chapterId: String) : LearnRoute()
    data class Quiz(val chapterId: String) : LearnRoute()
}

// ── 데이터 모델 ──────────────────────────────────────────────

enum class ChapterStatus { COMPLETED, IN_PROGRESS, UNLOCKED, LOCKED }

data class Chapter(
    val id: String,
    val title: String,
    val status: ChapterStatus,
    val progressPercent: Float = 0f   // 0f ~ 1f, IN_PROGRESS 일 때만 사용
)

data class Stage(
    val name: String,
    val completedCount: Int,
    val chapters: List<Chapter>
)

// ── 화면 ─────────────────────────────────────────────────────

@Composable
fun LearnScreen(
    modifier: Modifier = Modifier,
    stages: List<Stage> = emptyList(),
    isLoading: Boolean = false,
    onChapterClick: (chapterId: String) -> Unit = {},
    onRetry: () -> Unit = {}
) {
    if (isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (stages.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "챕터를 불러올 수 없어요\n서버에 연결되어 있는지 확인해주세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRetry) {
                    Text("다시 시도")
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { AdBanner() }
        item {
            Text(
                text = "학습",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        stages.forEach { stage ->
            item { StageHeader(stage = stage) }
            items(stage.chapters) { chapter ->
                ChapterCard(
                    chapter = chapter,
                    onClick = { onChapterClick(chapter.id) }
                )
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

// ── 단계 헤더 ─────────────────────────────────────────────────

@Composable
private fun StageHeader(stage: Stage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stage.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${stage.completedCount}/${stage.chapters.size} 완료",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── 챕터 카드 ─────────────────────────────────────────────────

@Composable
private fun ChapterCard(chapter: Chapter, onClick: () -> Unit) {
    val isLocked = chapter.status == ChapterStatus.LOCKED

    Card(
        onClick = onClick,
        enabled = !isLocked,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isLocked) 0.5f else 1f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLocked) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 상태 아이콘
            ChapterStatusIcon(status = chapter.status)

            // 챕터 번호 + 제목 + 진도 바
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.id,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = chapter.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                // 진행 중인 챕터에만 진도 바 표시
                if (chapter.status == ChapterStatus.IN_PROGRESS) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ProgressBar(progress = chapter.progressPercent)
                }
            }
        }
    }
}

// ── 상태 아이콘 ───────────────────────────────────────────────

@Composable
private fun ChapterStatusIcon(status: ChapterStatus) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                when (status) {
                    ChapterStatus.COMPLETED -> CompletedGreen.copy(alpha = 0.15f)
                    ChapterStatus.IN_PROGRESS -> XpPurple.copy(alpha = 0.15f)
                    ChapterStatus.UNLOCKED -> MaterialTheme.colorScheme.primaryContainer
                    ChapterStatus.LOCKED -> MaterialTheme.colorScheme.surfaceVariant
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        when (status) {
            ChapterStatus.COMPLETED -> Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "완료",
                tint = CompletedGreen,
                modifier = Modifier.size(22.dp)
            )
            ChapterStatus.LOCKED -> Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "잠금",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            else -> Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (status == ChapterStatus.IN_PROGRESS) XpPurple
                        else MaterialTheme.colorScheme.primary
                    )
            )
        }
    }
}

// ── 진도 바 ───────────────────────────────────────────────────

@Composable
private fun ProgressBar(progress: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(RoundedCornerShape(3.dp))
                    .background(XpPurple)
            )
        }
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = XpPurple,
            fontWeight = FontWeight.SemiBold
        )
    }
}
