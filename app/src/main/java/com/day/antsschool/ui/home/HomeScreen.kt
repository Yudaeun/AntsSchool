package com.day.antsschool.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.day.antsschool.network.dto.MarketItem
import com.day.antsschool.network.dto.NewsItem
import com.day.antsschool.network.dto.TodayQuoteResponse
import com.day.antsschool.network.dto.UserProgressResponse
import com.day.antsschool.ui.common.AdBanner
import com.day.antsschool.ui.news.NewsModal
import com.day.antsschool.ui.news.NewsViewModel

private val HeaderBackground = Color(0xFF1A1A2E)
private val StreakOrange = Color(0xFFFF6B35)
private val XpPurple = Color(0xFF7C4DFF)
private val UpGreen = Color(0xFF4CAF50)
private val DownRed = Color(0xFFE53935)

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    userName: String?,
    userProgress: UserProgressResponse?,
    homeViewModel: HomeViewModel,
    nextChapterId: String? = null,
    nextChapterTitle: String? = null,
    onGoToLearn: (chapterId: String) -> Unit = {},
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    val newsViewModel: NewsViewModel = viewModel(key = "home_news")
    val newsUiState by newsViewModel.uiState.collectAsStateWithLifecycle()
    val todayQuote by homeViewModel.todayQuote.collectAsStateWithLifecycle()
    val marketItems by homeViewModel.marketItems.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        homeViewModel.loadTodayQuote()
        homeViewModel.loadMarket()
        if (newsViewModel.uiState.value.news.isEmpty()) newsViewModel.loadNews(reset = true)
    }

    // 모달 — newsViewModel 상태로 통합
    newsUiState.selectedNews?.let { news ->
        NewsModal(
            item = news,
            body = newsUiState.selectedNewsBody,
            isLoadingBody = newsUiState.isLoadingBody,
            onDismiss = { newsViewModel.selectNews(null) }
            // 홈 화면 뉴스 미리보기는 북마크 버튼 미표시
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        HomeHeader(
            isLoggedIn = userName != null,
            userName = userName ?: "",
            userProgress = userProgress,
            onSignInClick = onSignInClick,
            onSignOutClick = onSignOutClick
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { TodayCard(quote = todayQuote) }
            if (nextChapterId != null && nextChapterTitle != null) {
                item {
                    TodayTodoCard(
                        chapterTitle = nextChapterTitle,
                        onStart = { onGoToLearn(nextChapterId) }
                    )
                }
            }
            if (marketItems.isNotEmpty()) {
                item { MarketTickerSection(items = marketItems) }
            }
            item {
                NewsSection(
                    news = newsUiState.news.take(3),
                    isLoading = newsUiState.isLoading,
                    hasError = newsUiState.error != null || (!newsUiState.isLoading && newsUiState.news.isEmpty()),
                    onRetry = { newsViewModel.loadNews(reset = true) },
                    onNewsClick = { newsViewModel.selectNews(it) }
                )
            }
            item { AdBanner() }
        }
    }
}

@Composable
private fun HomeHeader(
    isLoggedIn: Boolean,
    userName: String,
    userProgress: UserProgressResponse?,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderBackground)
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        if (isLoggedIn) {
            Text(text = "좋은 하루예요!", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "${userName}님", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val streak = userProgress?.streak ?: 0
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(StreakOrange)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (streak > 0) "🔥 ${streak}일 연속" else "🔥 오늘 시작!",
                        color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable(onClick = onSignOutClick)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = "로그아웃", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            val level = userProgress?.level ?: 1
            val xpInLevel = (userProgress?.xp ?: 0) % 100
            val xpPercent = (xpInLevel / 100f).coerceIn(0f, 1f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "Lv.$level", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .weight(1f).height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(xpPercent)
                            .clip(RoundedCornerShape(4.dp))
                            .background(XpPurple)
                    )
                }
                Text(text = "$xpInLevel / 100 XP", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        } else {
            Text(text = "개미의 경제학교", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "로그인하고 학습 기록을 저장해보세요", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSignInClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = HeaderBackground)
            ) { Text("Google로 로그인", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun TodayCard(quote: TodayQuoteResponse?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "오늘의 한 줄",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (quote != null) {
                Text(
                    text = "\"${quote.quote}\"",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 24.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "— ${quote.author}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "오늘의 경제 명언을 불러오는 중...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MarketTickerSection(items: List<MarketItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "시장 지수",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                items.take(3).forEach { item ->
                    MarketCell(item = item, modifier = Modifier.weight(1f))
                }
            }
            if (items.size > 3) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    items.drop(3).take(3).forEach { item ->
                        MarketCell(item = item, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketCell(item: MarketItem, modifier: Modifier = Modifier) {
    val color = if (item.isUp) UpGreen else DownRed
    val arrow = if (item.isUp) "▲" else "▼"
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = item.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = item.price,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "$arrow ${"%.2f".format(Math.abs(item.changePercent))}%",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NewsSection(
    news: List<NewsItem>,
    isLoading: Boolean,
    hasError: Boolean,
    onRetry: () -> Unit,
    onNewsClick: (NewsItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "최신 뉴스", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        when {
            isLoading -> Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.height(24.dp))
            }
            hasError -> androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "뉴스를 불러오지 못했어요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                androidx.compose.material3.TextButton(onClick = onRetry) {
                    Text("다시 시도", style = MaterialTheme.typography.bodySmall)
                }
            }
            else -> news.forEach { item ->
                HomeNewsCard(item = item, onClick = { onNewsClick(item) })
            }
        }
    }
}

@Composable
private fun HomeNewsCard(item: NewsItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.source,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text("·", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = item.pubDate.take(10),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TodayTodoCard(chapterTitle: String, onStart: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = XpPurple.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "📖", fontSize = 36.sp)
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(XpPurple.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "오늘 할 일",
                        style = MaterialTheme.typography.labelSmall,
                        color = XpPurple,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = chapterTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "지금 바로 학습을 시작해보세요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            androidx.compose.material3.Button(
                onClick = onStart,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = XpPurple),
                shape = RoundedCornerShape(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("시작", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

