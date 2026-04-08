package com.day.antsschool.server.api

import com.day.antsschool.server.api.dto.CardResponse
import com.day.antsschool.server.api.dto.ChapterContentResponse
import com.day.antsschool.server.api.dto.ChapterResponse
import com.day.antsschool.server.api.dto.QuizQuestionResponse
import com.day.antsschool.server.api.dto.StageResponse
import com.day.antsschool.server.repository.CardRepository
import com.day.antsschool.server.repository.ChapterProgressRepository
import com.day.antsschool.server.repository.ChapterRepository
import com.day.antsschool.server.repository.QuizRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/chapters")
class ChapterController(
    private val chapterRepository: ChapterRepository,
    private val cardRepository: CardRepository,
    private val quizRepository: QuizRepository,
    private val chapterProgressRepository: ChapterProgressRepository
) {
    // 챕터 목록 + 사용자 진도 조합
    @GetMapping
    fun getChapters(@RequestParam uid: String?): List<StageResponse> {
        val allChapters = chapterRepository.findAllByOrderByGlobalOrderAsc()
        val progressMap = if (uid != null) {
            chapterProgressRepository.findByUid(uid).associateBy { it.chapterId }
        } else emptyMap()

        val completedIds = progressMap.filter { it.value.status == "COMPLETED" }.keys

        // 완료된 챕터 중 가장 높은 globalOrder 기준으로 해금 범위 결정
        // ex) 입문05(order=5)가 완료되면 order 1~6 모두 해금
        val maxCompletedOrder = allChapters
            .filter { it.id in completedIds }
            .maxOfOrNull { it.globalOrder } ?: 0

        return allChapters
            .groupBy { it.stage }
            .entries
            .sortedBy { entry -> allChapters.first { it.stage == entry.key }.globalOrder }
            .map { (stage, chapters) ->
                val responses = chapters.map { chapter ->
                    val progress = progressMap[chapter.id]
                    val isUnlocked = chapter.globalOrder <= maxCompletedOrder + 1
                    ChapterResponse(
                        id = chapter.id,
                        title = chapter.title,
                        status = when {
                            progress?.status == "COMPLETED" -> "COMPLETED"
                            progress?.status == "IN_PROGRESS" -> "IN_PROGRESS"
                            isUnlocked -> "UNLOCKED"
                            else -> "LOCKED"
                        },
                        progressPercent = progress?.progressPercent ?: 0f
                    )
                }
                StageResponse(
                    name = "${stage} 단계",
                    completedCount = responses.count { it.status == "COMPLETED" },
                    chapters = responses
                )
            }
    }

    // 챕터 콘텐츠 (카드 + 퀴즈)
    @GetMapping("/{chapterId}/content")
    fun getChapterContent(@PathVariable chapterId: String): ResponseEntity<ChapterContentResponse> {
        val chapter = chapterRepository.findById(chapterId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        val cards = cardRepository.findByChapterIdOrderByCardOrderAsc(chapterId)
        val quiz = quizRepository.findByChapterIdOrderByQuestionOrderAsc(chapterId)

        return ResponseEntity.ok(
            ChapterContentResponse(
                chapterId = chapter.id,
                chapterTitle = chapter.title,
                cards = cards.map { CardResponse(it.id, it.cardOrder, it.emoji, it.title, it.content) },
                quiz = quiz.map { QuizQuestionResponse(it.id, it.questionOrder, it.question, it.options, it.correctIndex, it.explanation) }
            )
        )
    }

}
