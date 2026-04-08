package com.day.antsschool.server.api

import com.day.antsschool.server.api.dto.AnswerDetail
import com.day.antsschool.server.api.dto.DailyAnswer
import com.day.antsschool.server.api.dto.DailyQuizResponse
import com.day.antsschool.server.api.dto.DailyQuizResultResponse
import com.day.antsschool.server.api.dto.DailyQuizSubmitRequest
import com.day.antsschool.server.api.dto.QuizQuestionResponse
import com.day.antsschool.server.api.dto.QuizResultRequest
import com.day.antsschool.server.api.dto.QuizResultResponse
import com.day.antsschool.server.domain.ChapterProgress
import com.day.antsschool.server.domain.UserBadge
import com.day.antsschool.server.domain.UserProgress
import com.day.antsschool.server.domain.WrongAnswer
import com.day.antsschool.server.repository.ChapterProgressRepository
import com.day.antsschool.server.repository.QuizRepository
import com.day.antsschool.server.repository.UserBadgeRepository
import com.day.antsschool.server.repository.UserProgressRepository
import com.day.antsschool.server.repository.WrongAnswerRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

private val INTRO_CHAPTERS = setOf("입문01", "입문02", "입문03", "입문04", "입문05", "입문06")
private val BEGINNER_CHAPTERS = (1..30).map { "초급%02d".format(it) }.toSet()
private val INTERMEDIATE_CHAPTERS = (1..30).map { "중급%02d".format(it) }.toSet()
private val ADVANCED_CHAPTERS = (1..30).map { "고급%02d".format(it) }.toSet()

@RestController
@RequestMapping("/api/quiz")
class QuizController(
    private val quizRepository: QuizRepository,
    private val userProgressRepository: UserProgressRepository,
    private val chapterProgressRepository: ChapterProgressRepository,
    private val userBadgeRepository: UserBadgeRepository,
    private val wrongAnswerRepository: WrongAnswerRepository
) {
    // ── 데일리 퀴즈 조회 ──────────────────────────────────────────

    @GetMapping("/daily")
    fun getDailyQuiz(@RequestParam uid: String?): DailyQuizResponse {
        val today = LocalDate.now().toString()
        val alreadyDone = uid?.let {
            userProgressRepository.findById(it).orElse(null)?.lastDailyQuizDate == today
        } ?: false

        val all = quizRepository.findAll()
        val questions = all.shuffled().take(5).map { q ->
            QuizQuestionResponse(
                id = q.id,
                order = q.questionOrder,
                question = q.question,
                options = q.options,
                correctIndex = q.correctIndex,
                explanation = q.explanation
            )
        }
        return DailyQuizResponse(questions = questions, alreadyDone = alreadyDone)
    }

    // ── 복습 퀴즈 조회 (오답노트) ─────────────────────────────────

    @GetMapping("/review")
    fun getReviewQuiz(@RequestParam uid: String): List<QuizQuestionResponse> {
        val wrongAnswers = wrongAnswerRepository.findByUid(uid)
            .sortedByDescending { it.wrongCount }
            .take(10)

        return wrongAnswers.mapNotNull { wrong ->
            quizRepository.findById(wrong.questionId).orElse(null)?.let { q ->
                QuizQuestionResponse(
                    id = q.id,
                    order = q.questionOrder,
                    question = q.question,
                    options = q.options,
                    correctIndex = q.correctIndex,
                    explanation = q.explanation
                )
            }
        }
    }

    // ── 데일리/복습 퀴즈 결과 저장 ───────────────────────────────

    @PostMapping("/daily/result")
    fun saveDailyResult(@RequestBody req: DailyQuizSubmitRequest): DailyQuizResultResponse {
        val today = LocalDate.now().toString()

        // 채점
        val questionMap = quizRepository.findAllById(req.answers.map { it.questionId }).associateBy { it.id }
        val details = req.answers.map { answer ->
            val q = questionMap[answer.questionId]
            AnswerDetail(
                questionId = answer.questionId,
                question = q?.question ?: "",
                options = q?.options ?: emptyList(),
                selectedIndex = answer.selectedIndex,
                correctIndex = q?.correctIndex ?: 0,
                isCorrect = answer.selectedIndex == q?.correctIndex,
                explanation = q?.explanation ?: ""
            )
        }

        val correctCount = details.count { it.isCorrect }
        val xpEarned = correctCount * 10

        // 오답 기록
        details.filter { !it.isCorrect }.forEach { detail ->
            val q = questionMap[detail.questionId] ?: return@forEach
            val existing = wrongAnswerRepository.findByUidAndQuestionId(req.uid, detail.questionId)
            if (existing != null) {
                wrongAnswerRepository.save(
                    existing.copy(wrongCount = existing.wrongCount + 1, lastWrongAt = today)
                )
            } else {
                wrongAnswerRepository.save(
                    WrongAnswer(
                        uid = req.uid,
                        questionId = detail.questionId,
                        question = q.question,
                        options = q.options.joinToString("|"),
                        correctIndex = q.correctIndex,
                        explanation = q.explanation,
                        lastWrongAt = today
                    )
                )
            }
        }
        // 정답 맞힌 문제는 오답노트에서 제거
        details.filter { it.isCorrect }.forEach { detail ->
            wrongAnswerRepository.findByUidAndQuestionId(req.uid, detail.questionId)
                ?.let { wrongAnswerRepository.delete(it) }
        }

        // XP + 데일리 완료 날짜 + 정답률 누적 업데이트
        val current = userProgressRepository.findById(req.uid).orElse(UserProgress(uid = req.uid))
        val newXp = current.xp + xpEarned
        val newLevel = calculateLevel(newXp)
        userProgressRepository.save(
            current.copy(
                xp = newXp,
                level = newLevel,
                lastDailyQuizDate = today,
                totalCorrect = current.totalCorrect + correctCount,
                totalAnswered = current.totalAnswered + details.size
            )
        )

        // 배지 체크
        val newBadges = mutableListOf<String>()
        fun awardBadge(badgeId: String) {
            if (!userBadgeRepository.existsByUidAndBadgeId(req.uid, badgeId)) {
                userBadgeRepository.save(UserBadge(uid = req.uid, badgeId = badgeId, earnedAt = today))
                newBadges.add(badgeId)
            }
        }
        if (correctCount == details.size) awardBadge("PERFECT_QUIZ")

        return DailyQuizResultResponse(
            correctCount = correctCount,
            totalQuestions = details.size,
            xpEarned = xpEarned,
            newBadges = newBadges,
            details = details
        )
    }

    // ── 챕터 퀴즈 결과 저장 (기존) ──────────────────────────────

    @PostMapping("/result")
    fun saveResult(@RequestBody req: QuizResultRequest): QuizResultResponse {
        val xpEarned = req.correctCount * 10
        val chapterCompleted = req.correctCount == req.totalQuestions
        val today = LocalDate.now().toString()

        val current = userProgressRepository.findById(req.uid).orElse(UserProgress(uid = req.uid))
        val newXp = current.xp + xpEarned
        val newLevel = calculateLevel(newXp)
        val newStreak = when (current.lastLoginDate) {
            LocalDate.now().minusDays(1).toString() -> current.streak + 1
            today -> current.streak
            else -> 1
        }
        userProgressRepository.save(
            current.copy(
                xp = newXp,
                level = newLevel,
                streak = newStreak,
                lastLoginDate = today,
                totalCorrect = current.totalCorrect + req.correctCount,
                totalAnswered = current.totalAnswered + req.totalQuestions
            )
        )

        val chapterProgress = chapterProgressRepository
            .findByUidAndChapterId(req.uid, req.chapterId)
            ?: ChapterProgress(uid = req.uid, chapterId = req.chapterId)
        chapterProgressRepository.save(
            chapterProgress.copy(
                status = if (chapterCompleted) "COMPLETED" else "IN_PROGRESS",
                progressPercent = if (chapterCompleted) 1f else req.correctCount.toFloat() / req.totalQuestions
            )
        )

        val newBadges = mutableListOf<String>()
        fun awardBadge(badgeId: String) {
            if (!userBadgeRepository.existsByUidAndBadgeId(req.uid, badgeId)) {
                userBadgeRepository.save(UserBadge(uid = req.uid, badgeId = badgeId, earnedAt = today))
                newBadges.add(badgeId)
            }
        }

        if (chapterCompleted) {
            val allProgress = chapterProgressRepository.findByUid(req.uid)
            val completedIds = allProgress.filter { it.status == "COMPLETED" }.map { it.chapterId }.toSet()
            if (completedIds.size == 1) awardBadge("FIRST_STUDY")
            if (completedIds.size >= 10) awardBadge("CHAPTER_10")
            if (INTRO_CHAPTERS.all { it in completedIds }) awardBadge("INTRO_COMPLETE")
            if (BEGINNER_CHAPTERS.all { it in completedIds }) awardBadge("BEGINNER_COMPLETE")
            if (INTERMEDIATE_CHAPTERS.all { it in completedIds }) awardBadge("INTERMEDIATE_COMPLETE")
            if (ADVANCED_CHAPTERS.all { it in completedIds }) awardBadge("ADVANCED_COMPLETE")
        }
        if (req.correctCount == req.totalQuestions) awardBadge("PERFECT_QUIZ")
        if (newStreak >= 3) awardBadge("STREAK_3")
        if (newStreak >= 7) awardBadge("STREAK_7")
        if (newStreak >= 30) awardBadge("STREAK_30")

        return QuizResultResponse(
            xpEarned = xpEarned,
            newTotalXp = newXp,
            newLevel = newLevel,
            chapterCompleted = chapterCompleted,
            newBadges = newBadges
        )
    }

    private fun calculateLevel(xp: Int): Int = (xp / 100) + 1
}
