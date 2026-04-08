package com.day.antsschool.ui.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.day.antsschool.network.LearnRepository
import com.day.antsschool.network.dto.QuizResultResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LearnUiState(
    val stages: List<Stage> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingContent: Boolean = false,
    val chapterContent: ChapterContent? = null,
    val lastQuizResult: QuizResultResponse? = null
)

class LearnViewModel(
    private val repository: LearnRepository = LearnRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LearnUiState())
    val uiState: StateFlow<LearnUiState> = _uiState.asStateFlow()

    private var lastUid: String? = null

    fun loadChapters(uid: String?) {
        lastUid = uid
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val stages = repository.getStages(uid)
            _uiState.update { it.copy(stages = stages, isLoading = false) }
        }
    }

    // 서버 연결 실패 시 재시도
    fun retryLoad() = loadChapters(lastUid)

    fun loadChapterContent(chapterId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingContent = true, chapterContent = null) }
            val content = repository.getChapterContent(chapterId)
            _uiState.update { it.copy(chapterContent = content, isLoadingContent = false) }
        }
    }

    fun saveQuizResult(uid: String?, chapterId: String, correctCount: Int, totalQuestions: Int) {
        if (uid == null) return
        viewModelScope.launch {
            val result = repository.saveQuizResult(uid, chapterId, correctCount, totalQuestions)
            _uiState.update { it.copy(lastQuizResult = result) }
            // 퀴즈 완료 후 챕터 목록 갱신
            loadChapters(uid)
        }
    }
}
