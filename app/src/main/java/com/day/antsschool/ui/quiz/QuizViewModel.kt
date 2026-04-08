package com.day.antsschool.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.day.antsschool.network.ApiClient
import com.day.antsschool.network.dto.AnswerDetail
import com.day.antsschool.network.dto.DailyAnswer
import com.day.antsschool.network.dto.DailyQuizResultResponse
import com.day.antsschool.network.dto.DailyQuizSubmitRequest
import com.day.antsschool.network.dto.QuizQuestionResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

val AI_QUIZ_TOPICS = listOf("주식 기초", "금리와 채권", "ETF 투자", "환율과 외환", "경제 지표")

data class QuizUiState(
    val dailyQuestions: List<QuizQuestionResponse> = emptyList(),
    val reviewQuestions: List<QuizQuestionResponse> = emptyList(),
    val alreadyDoneToday: Boolean = false,
    val isLoading: Boolean = true,
    val submitting: Boolean = false,
    val lastResult: DailyQuizResultResponse? = null,
    val isGeneratingAi: Boolean = false,
    val generatedAiQuestions: List<QuizQuestionResponse>? = null,
    val aiError: String? = null
)

class QuizViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    fun load(uid: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val daily = ApiClient.apiService.getDailyQuiz(uid)
                val review = if (uid != null) {
                    try { ApiClient.apiService.getReviewQuiz(uid) } catch (e: Exception) { emptyList() }
                } else emptyList()

                _uiState.value = _uiState.value.copy(
                    dailyQuestions = daily.questions,
                    alreadyDoneToday = daily.alreadyDone,
                    reviewQuestions = review,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun submitQuiz(uid: String, answers: List<DailyAnswer>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(submitting = true)
            try {
                val result = ApiClient.apiService.submitDailyQuiz(
                    DailyQuizSubmitRequest(uid = uid, answers = answers)
                )
                _uiState.value = _uiState.value.copy(
                    lastResult = result,
                    alreadyDoneToday = true,
                    submitting = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(submitting = false)
            }
        }
    }

    // AI 퀴즈 채점 (클라이언트 로컬, 서버 제출 없음)
    fun gradeAiQuiz(questions: List<QuizQuestionResponse>, answers: List<DailyAnswer>) {
        val questionMap = questions.associateBy { it.id }
        val details = answers.map { answer ->
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
        _uiState.value = _uiState.value.copy(
            lastResult = DailyQuizResultResponse(
                correctCount = correctCount,
                totalQuestions = details.size,
                xpEarned = correctCount * 10,
                newBadges = if (correctCount == details.size) listOf("PERFECT_QUIZ") else emptyList(),
                details = details
            )
        )
    }

    fun generateAiQuiz(topic: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGeneratingAi = true, aiError = null)
            try {
                val questions = ApiClient.apiService.getAiQuiz(topic)
                if (questions.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isGeneratingAi = false,
                        aiError = "AI 퀴즈를 생성할 수 없어요"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isGeneratingAi = false,
                        generatedAiQuestions = questions,
                        aiError = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGeneratingAi = false,
                    aiError = "AI 퀴즈를 생성할 수 없어요"
                )
            }
        }
    }

    fun clearAiQuestions() {
        _uiState.value = _uiState.value.copy(generatedAiQuestions = null)
    }

    fun clearAiError() {
        _uiState.value = _uiState.value.copy(aiError = null)
    }

    fun clearResult() {
        _uiState.value = _uiState.value.copy(lastResult = null)
    }
}
