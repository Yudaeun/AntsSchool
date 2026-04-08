package com.day.antsschool.network.dto

// ── 챕터 목록 ─────────────────────────────────────────────────

data class StageResponse(
    val name: String,
    val completedCount: Int,
    val chapters: List<ChapterResponse>
)

data class ChapterResponse(
    val id: String,
    val title: String,
    val status: String,
    val progressPercent: Float
)

// ── 챕터 콘텐츠 ───────────────────────────────────────────────

data class ChapterContentResponse(
    val chapterId: String,
    val chapterTitle: String,
    val cards: List<CardResponse>,
    val quiz: List<QuizQuestionResponse>
)

data class CardResponse(
    val id: Long,
    val order: Int,
    val emoji: String,
    val title: String,
    val content: String
)

data class QuizQuestionResponse(
    val id: Long,
    val order: Int,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

// ── 데일리 퀴즈 ──────────────────────────────────────────────

data class DailyQuizResponse(
    val questions: List<QuizQuestionResponse>,
    val alreadyDone: Boolean
)

data class DailyQuizSubmitRequest(
    val uid: String,
    val answers: List<DailyAnswer>
)

data class DailyAnswer(
    val questionId: Long,
    val selectedIndex: Int
)

data class DailyQuizResultResponse(
    val correctCount: Int,
    val totalQuestions: Int,
    val xpEarned: Int,
    val newBadges: List<String>,
    val details: List<AnswerDetail>
)

data class AnswerDetail(
    val questionId: Long,
    val question: String,
    val options: List<String>,
    val selectedIndex: Int,
    val correctIndex: Int,
    val isCorrect: Boolean,
    val explanation: String
)

// ── 챕터 퀴즈 결과 ────────────────────────────────────────────

data class QuizResultRequest(
    val uid: String,
    val chapterId: String,
    val correctCount: Int,
    val totalQuestions: Int
)

data class QuizResultResponse(
    val xpEarned: Int,
    val newTotalXp: Int,
    val newLevel: Int,
    val chapterCompleted: Boolean,
    val newBadges: List<String> = emptyList()
)

// ── 뉴스 ──────────────────────────────────────────────────────

data class NewsItem(
    val title: String,
    val link: String,
    val description: String,
    val pubDate: String,
    val source: String
)

// ── 오늘의 한 줄 ─────────────────────────────────────────────

data class TodayQuoteResponse(
    val quote: String,
    val author: String
)

// ── 시장 지수 ─────────────────────────────────────────────────

data class MarketItem(
    val symbol: String,
    val name: String,
    val price: String,
    val changePercent: Double,
    val isUp: Boolean
)

// ── 기사 본문 (모달 전용) ──────────────────────────────────────

data class ArticleContentResponse(val body: String)

// ── 사용자 진도 ───────────────────────────────────────────────

data class UserProgressResponse(
    val uid: String,
    val level: Int,
    val xp: Int,
    val xpForNextLevel: Int,
    val streak: Int,
    val badges: List<String> = emptyList(),
    val checkinDates: List<String> = emptyList(),
    val totalCorrect: Int = 0,
    val totalAnswered: Int = 0
)

// ── 뉴스 북마크 ──────────────────────────────────────────────────

data class BookmarkRequest(
    val uid: String,
    val title: String,
    val link: String,
    val description: String,
    val pubDate: String,
    val source: String
)

data class BookmarkResponse(
    val id: Long,
    val title: String,
    val link: String,
    val description: String,
    val pubDate: String,
    val source: String,
    val savedAt: String
)
