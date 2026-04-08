package com.day.antsschool

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Alignment
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.ads.MobileAds
import com.day.antsschool.auth.AuthViewModel
import com.day.antsschool.auth.GoogleAuthClient
import com.day.antsschool.navigation.AntsSchoolBottomBar
import com.day.antsschool.navigation.BottomTab
import com.day.antsschool.ui.home.HomeScreen
import com.day.antsschool.ui.home.HomeViewModel
import com.day.antsschool.ui.learn.CardSlideScreen
import com.day.antsschool.ui.learn.ChapterQuizScreen
import com.day.antsschool.ui.learn.ChapterStatus
import com.day.antsschool.ui.learn.LearnRoute
import com.day.antsschool.ui.learn.LearnScreen
import com.day.antsschool.ui.learn.LearnViewModel
import com.day.antsschool.ui.learn.hardcodedChapterContent
import com.day.antsschool.ui.news.NewsScreen
import com.day.antsschool.ui.quiz.QuizScreen
import com.day.antsschool.ui.record.RecordScreen
import com.day.antsschool.ui.theme.AntsSchoolTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MobileAds.initialize(this)
        setContent {
            AntsSchoolTheme {
                val authViewModel: AuthViewModel = viewModel()
                val learnViewModel: LearnViewModel = viewModel()
                val homeViewModel: HomeViewModel = viewModel()
                val googleAuthClient = remember { GoogleAuthClient(context = applicationContext) }

                val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
                val errorMessage by authViewModel.errorMessage.collectAsStateWithLifecycle()
                val learnUiState by learnViewModel.uiState.collectAsStateWithLifecycle()
                val userProgress by homeViewModel.userProgress.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }

                // 로그인 상태 변경 시: 챕터 로드 + 출석 체크
                LaunchedEffect(currentUser?.uid) {
                    learnViewModel.loadChapters(currentUser?.uid)
                    currentUser?.uid?.let { homeViewModel.checkIn(it) }
                }

                LaunchedEffect(errorMessage) {
                    errorMessage?.let { snackbarHostState.showSnackbar(it) }
                }

                // 퀴즈 완료 후 홈 진도 갱신
                LaunchedEffect(learnUiState.lastQuizResult) {
                    if (learnUiState.lastQuizResult != null) {
                        currentUser?.uid?.let { homeViewModel.refreshProgress(it) }
                    }
                }

                val signInLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        authViewModel.onSignInResult(googleAuthClient, result.data)
                    }
                }

                var selectedTab by rememberSaveable { mutableStateOf(BottomTab.HOME) }
                var learnRoute by remember { mutableStateOf<LearnRoute>(LearnRoute.ChapterList) }

                // 다음 학습할 챕터 (UNLOCKED 또는 IN_PROGRESS 중 첫 번째)
                val nextChapter = learnUiState.stages
                    .flatMap { it.chapters }
                    .firstOrNull { it.status == ChapterStatus.UNLOCKED || it.status == ChapterStatus.IN_PROGRESS }

                // 홈/내 기록 탭 진입 시 XP 갱신 (퀴즈·학습 후 최신 값 표시)
                LaunchedEffect(selectedTab) {
                    if (selectedTab == BottomTab.HOME || selectedTab == BottomTab.RECORD) {
                        currentUser?.uid?.let { homeViewModel.refreshProgress(it) }
                    }
                }

                val isInLesson = selectedTab == BottomTab.LEARN && learnRoute != LearnRoute.ChapterList
                if (isInLesson) {
                    BackHandler {
                        learnRoute = when (val r = learnRoute) {
                            is LearnRoute.Quiz -> LearnRoute.CardSlide(r.chapterId)
                            else -> LearnRoute.ChapterList
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (!isInLesson) {
                            AntsSchoolBottomBar(
                                selectedTab = selectedTab,
                                onTabSelected = { selectedTab = it }
                            )
                        }
                    }
                ) { innerPadding ->
                    when (selectedTab) {
                        BottomTab.HOME -> HomeScreen(
                            modifier = Modifier.padding(innerPadding),
                            userName = currentUser?.displayName,
                            userProgress = userProgress,
                            homeViewModel = homeViewModel,
                            nextChapterId = nextChapter?.id,
                            nextChapterTitle = nextChapter?.title,
                            onGoToLearn = { chapterId ->
                                learnViewModel.loadChapterContent(chapterId)
                                learnRoute = LearnRoute.CardSlide(chapterId)
                                selectedTab = BottomTab.LEARN
                            },
                            onSignInClick = { signInLauncher.launch(googleAuthClient.getSignInIntent()) },
                            onSignOutClick = { authViewModel.signOut(googleAuthClient) }
                        )
                        BottomTab.LEARN -> when (val route = learnRoute) {
                            is LearnRoute.ChapterList -> LearnScreen(
                                modifier = Modifier.padding(innerPadding),
                                stages = learnUiState.stages,
                                isLoading = learnUiState.isLoading,
                                onChapterClick = { chapterId ->
                                    learnViewModel.loadChapterContent(chapterId)
                                    learnRoute = LearnRoute.CardSlide(chapterId)
                                },
                                onRetry = { learnViewModel.retryLoad() }
                            )
                            is LearnRoute.CardSlide -> {
                                val content = learnUiState.chapterContent
                                    ?.takeIf { it.chapterId == route.chapterId }
                                    ?: hardcodedChapterContent[route.chapterId]

                                when {
                                    learnUiState.isLoadingContent -> Box(
                                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                                        contentAlignment = Alignment.Center
                                    ) { CircularProgressIndicator() }
                                    content != null -> CardSlideScreen(
                                        content = content,
                                        onBack = { learnRoute = LearnRoute.ChapterList },
                                        onQuizStart = { learnRoute = LearnRoute.Quiz(route.chapterId) }
                                    )
                                    else -> {
                                        // 콘텐츠 로드 실패 시 챕터 목록으로 복귀
                                        LaunchedEffect(Unit) { learnRoute = LearnRoute.ChapterList }
                                    }
                                }
                            }
                            is LearnRoute.Quiz -> {
                                val content = learnUiState.chapterContent
                                    ?.takeIf { it.chapterId == route.chapterId }
                                    ?: hardcodedChapterContent[route.chapterId]

                                when {
                                    learnUiState.isLoadingContent -> Box(
                                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                                        contentAlignment = Alignment.Center
                                    ) { CircularProgressIndicator() }
                                    content != null -> ChapterQuizScreen(
                                        content = content,
                                        quizResult = learnUiState.lastQuizResult,
                                        onBack = { learnRoute = LearnRoute.CardSlide(route.chapterId) },
                                        onComplete = { learnRoute = LearnRoute.ChapterList },
                                        onSaveResult = { correctCount, totalQuestions ->
                                            learnViewModel.saveQuizResult(
                                                uid = currentUser?.uid,
                                                chapterId = route.chapterId,
                                                correctCount = correctCount,
                                                totalQuestions = totalQuestions
                                            )
                                        }
                                    )
                                    else -> {
                                        LaunchedEffect(Unit) { learnRoute = LearnRoute.ChapterList }
                                    }
                                }
                            }
                        }
                        BottomTab.NEWS -> NewsScreen(
                            modifier = Modifier.padding(innerPadding),
                            uid = currentUser?.uid
                        )
                        BottomTab.QUIZ -> QuizScreen(
                            modifier = Modifier.padding(innerPadding),
                            uid = currentUser?.uid
                        )
                        BottomTab.RECORD -> RecordScreen(
                            modifier = Modifier.padding(innerPadding),
                            userProgress = userProgress
                        )
                    }
                }
            }
        }
    }
}
