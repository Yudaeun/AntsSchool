package com.day.antsschool.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.day.antsschool.network.ApiClient
import com.day.antsschool.network.dto.BookmarkRequest
import com.day.antsschool.network.dto.BookmarkResponse
import com.day.antsschool.network.dto.NewsItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val NEWS_SOURCES = listOf("전체", "연합뉴스", "매일경제", "한국경제", "머니투데이", "북마크")
private const val PAGE_SIZE = 20

data class NewsUiState(
    val news: List<NewsItem> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null,
    val selectedSource: String = "전체",
    val selectedNews: NewsItem? = null,
    val selectedNewsBody: String? = null,
    val isLoadingBody: Boolean = false,
    val bookmarkedLinks: Set<String> = emptySet(),  // 북마크된 기사 link 목록
    val bookmarks: List<BookmarkResponse> = emptyList()  // 북마크 목록
)

class NewsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(NewsUiState())
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    private var currentPage = 0

    // 뉴스 탭 진입 시 NewsScreen에서 명시적으로 호출
    // init에서 자동 로드 제거 → 앱 시작 시 불필요한 네트워크 요청 방지

    fun selectSource(source: String) {
        if (_uiState.value.selectedSource == source) return
        _uiState.update { it.copy(selectedSource = source) }
        if (source != "북마크") loadNews(reset = true)
    }

    // 북마크 목록 로드
    fun loadBookmarks(uid: String) {
        viewModelScope.launch {
            try {
                val bookmarks = ApiClient.apiService.getBookmarks(uid)
                _uiState.update {
                    it.copy(
                        bookmarks = bookmarks,
                        bookmarkedLinks = bookmarks.map { b -> b.link }.toHashSet()
                    )
                }
            } catch (e: Exception) { /* 실패 시 유지 */ }
        }
    }

    // 북마크 토글 (추가/삭제)
    fun toggleBookmark(uid: String, item: NewsItem) {
        val alreadyBookmarked = item.link in _uiState.value.bookmarkedLinks
        viewModelScope.launch {
            try {
                if (alreadyBookmarked) {
                    ApiClient.apiService.removeBookmark(uid, item.link)
                    _uiState.update {
                        it.copy(
                            bookmarkedLinks = it.bookmarkedLinks - item.link,
                            bookmarks = it.bookmarks.filter { b -> b.link != item.link }
                        )
                    }
                } else {
                    val saved = ApiClient.apiService.addBookmark(
                        BookmarkRequest(
                            uid = uid,
                            title = item.title,
                            link = item.link,
                            description = item.description,
                            pubDate = item.pubDate,
                            source = item.source
                        )
                    )
                    _uiState.update {
                        it.copy(
                            bookmarkedLinks = it.bookmarkedLinks + item.link,
                            bookmarks = listOf(saved) + it.bookmarks
                        )
                    }
                }
            } catch (e: Exception) { /* 실패 시 유지 */ }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        loadNews(reset = false)
    }

    /** 뉴스 항목 선택 — 동시에 기사 본문을 서버에서 가져옴 */
    fun selectNews(item: NewsItem?) {
        if (item == null) {
            _uiState.update { it.copy(selectedNews = null, selectedNewsBody = null, isLoadingBody = false) }
            return
        }
        _uiState.update { it.copy(selectedNews = item, selectedNewsBody = null, isLoadingBody = true) }
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.getNewsContent(item.link, item.source)
                val body = response.body.ifEmpty { item.description }
                _uiState.update { it.copy(selectedNewsBody = body, isLoadingBody = false) }
            } catch (e: Exception) {
                // 실패 시 RSS description 표시
                _uiState.update { it.copy(selectedNewsBody = item.description, isLoadingBody = false) }
            }
        }
    }

    fun loadNews(reset: Boolean = true) {
        if (reset) currentPage = 0
        viewModelScope.launch {
            if (reset) {
                _uiState.update { it.copy(isLoading = true, error = null, news = emptyList()) }
            } else {
                _uiState.update { it.copy(isLoadingMore = true) }
            }
            try {
                val source = _uiState.value.selectedSource.takeIf { it != "전체" }
                val newItems = ApiClient.apiService.getNews(
                    source = source,
                    page = currentPage,
                    size = PAGE_SIZE
                )
                val combined = if (reset) newItems else _uiState.value.news + newItems
                currentPage++
                _uiState.update {
                    it.copy(
                        news = combined,
                        isLoading = false,
                        isLoadingMore = false,
                        hasMore = newItems.size >= PAGE_SIZE,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = if (reset) "뉴스를 불러올 수 없어요" else null
                    )
                }
            }
        }
    }
}
