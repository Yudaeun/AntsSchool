package com.day.antsschool.server.api

import com.day.antsschool.server.api.dto.QuizQuestionResponse
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClient

@RestController
@RequestMapping("/api/quiz/ai")
class AiQuizController {

    private val restClient = RestClient.create()
    private val objectMapper = jacksonObjectMapper()
    private val apiKey get() = System.getenv("ANTHROPIC_API_KEY") ?: ""

    private data class AiQuizItem(
        val question: String = "",
        val options: List<String> = emptyList(),
        val correctIndex: Int = 0,
        val explanation: String = ""
    )

    @GetMapping
    fun generateAiQuiz(@RequestParam topic: String): List<QuizQuestionResponse> {
        if (apiKey.isBlank()) return emptyList()

        val prompt = """
            당신은 한국 경제 교육 앱의 퀴즈 출제자입니다.
            '$topic' 주제에 대해 일반 투자자가 이해할 수 있는 경제 퀴즈 5문제를 만들어주세요.

            반드시 아래 JSON 배열 형식으로만 응답하세요. 다른 텍스트는 절대 포함하지 마세요:
            [{"question":"질문 내용","options":["보기1","보기2","보기3","보기4"],"correctIndex":0,"explanation":"정답 해설"}]
        """.trimIndent()

        return try {
            val requestBody = mapOf(
                "model" to "claude-haiku-4-5-20251001",
                "max_tokens" to 2048,
                "messages" to listOf(mapOf("role" to "user", "content" to prompt))
            )

            val response = restClient.post()
                .uri("https://api.anthropic.com/v1/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(Map::class.java) ?: return emptyList()

            val text = ((response["content"] as? List<*>)?.firstOrNull() as? Map<*, *>)
                ?.get("text") as? String ?: return emptyList()

            val start = text.indexOf('[')
            val end = text.lastIndexOf(']') + 1
            if (start == -1 || end <= start) return emptyList()

            val items: List<AiQuizItem> = objectMapper.readValue(text.substring(start, end))
            items.mapIndexed { i, item ->
                QuizQuestionResponse(
                    id = -(i + 1).toLong(),   // 음수 ID: AI 생성 문제 구분용
                    order = i + 1,
                    question = item.question,
                    options = item.options,
                    correctIndex = item.correctIndex,
                    explanation = item.explanation
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
