package com.day.antsschool.network

import com.day.antsschool.network.dto.ChapterContentResponse
import com.day.antsschool.network.dto.QuizResultRequest
import com.day.antsschool.network.dto.QuizResultResponse
import com.day.antsschool.network.dto.StageResponse
import com.day.antsschool.network.dto.TodayQuoteResponse
import com.day.antsschool.network.dto.UserProgressResponse
import com.day.antsschool.ui.learn.Chapter
import com.day.antsschool.ui.learn.ChapterContent
import com.day.antsschool.ui.learn.ChapterStatus
import com.day.antsschool.ui.learn.LearningCard
import com.day.antsschool.ui.learn.QuizQuestion
import com.day.antsschool.ui.learn.Stage
import com.day.antsschool.ui.learn.hardcodedChapterContent

class LearnRepository(
    private val api: ApiService = ApiClient.apiService
) {
    suspend fun getStages(uid: String?): List<Stage> {
        return try {
            api.getChapters(uid).map { it.toStage() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // 챕터 콘텐츠 — 서버 우선, 실패 시 하드코딩 폴백
    suspend fun getChapterContent(chapterId: String): ChapterContent? {
        return try {
            api.getChapterContent(chapterId).toChapterContent()
        } catch (e: Exception) {
            hardcodedChapterContent[chapterId]
        }
    }

    // 퀴즈 결과 저장 — 실패 시 무시 (오프라인 지원)
    suspend fun saveQuizResult(
        uid: String,
        chapterId: String,
        correctCount: Int,
        totalQuestions: Int
    ): QuizResultResponse? {
        return try {
            api.saveQuizResult(QuizResultRequest(uid, chapterId, correctCount, totalQuestions))
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserProgress(uid: String): UserProgressResponse? {
        return try {
            api.getUserProgress(uid)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun checkIn(uid: String): UserProgressResponse? {
        return try {
            api.checkIn(uid)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getTodayQuote(): TodayQuoteResponse? {
        return try {
            api.getTodayQuote()
        } catch (e: Exception) {
            null
        }
    }
}

// ── DTO → UI 모델 변환 ─────────────────────────────────────────

private fun StageResponse.toStage(): Stage = Stage(
    name = name,
    completedCount = completedCount,
    chapters = chapters.map { it.toChapter() }
)

private fun com.day.antsschool.network.dto.ChapterResponse.toChapter(): Chapter = Chapter(
    id = id,
    title = title,
    status = when (status) {
        "COMPLETED" -> ChapterStatus.COMPLETED
        "IN_PROGRESS" -> ChapterStatus.IN_PROGRESS
        "UNLOCKED" -> ChapterStatus.UNLOCKED
        else -> ChapterStatus.LOCKED
    },
    progressPercent = progressPercent
)

private fun ChapterContentResponse.toChapterContent(): ChapterContent = ChapterContent(
    chapterId = chapterId,
    chapterTitle = chapterTitle,
    cards = cards.map { LearningCard(it.emoji, it.title, it.content) },
    quiz = quiz.map { QuizQuestion(it.question, it.options, it.correctIndex, it.explanation) }
)

