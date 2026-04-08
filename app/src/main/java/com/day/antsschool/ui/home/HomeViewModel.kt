package com.day.antsschool.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.day.antsschool.network.ApiClient
import com.day.antsschool.network.LearnRepository
import com.day.antsschool.network.dto.MarketItem
import com.day.antsschool.network.dto.TodayQuoteResponse
import com.day.antsschool.network.dto.UserProgressResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: LearnRepository = LearnRepository()
) : ViewModel() {

    private val _userProgress = MutableStateFlow<UserProgressResponse?>(null)
    val userProgress: StateFlow<UserProgressResponse?> = _userProgress.asStateFlow()

    private val _todayQuote = MutableStateFlow<TodayQuoteResponse?>(null)
    val todayQuote: StateFlow<TodayQuoteResponse?> = _todayQuote.asStateFlow()

    // 앱 시작 시 출석 체크 — 스트릭 업데이트 후 최신 진도 반영
    fun checkIn(uid: String) {
        viewModelScope.launch {
            _userProgress.value = repository.checkIn(uid)
        }
    }

    // 퀴즈 완료 후 또는 탭 전환 시 진도 갱신
    fun refreshProgress(uid: String) {
        viewModelScope.launch {
            _userProgress.value = repository.getUserProgress(uid)
        }
    }

    // 홈 화면 진입 시 오늘의 한 줄 갱신
    fun loadTodayQuote() {
        viewModelScope.launch {
            val q = repository.getTodayQuote()
            if (q != null) _todayQuote.value = q
        }
    }

    private val _marketItems = MutableStateFlow<List<MarketItem>>(emptyList())
    val marketItems: StateFlow<List<MarketItem>> = _marketItems.asStateFlow()

    // 시장 지수 로드
    fun loadMarket() {
        viewModelScope.launch {
            try {
                val items = ApiClient.apiService.getMarket()
                if (items.isNotEmpty()) _marketItems.value = items
            } catch (e: Exception) {
                // 실패 시 빈 목록 유지
            }
        }
    }
}
