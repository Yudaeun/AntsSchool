package com.day.antsschool.network

import com.day.antsschool.network.dto.ArticleContentResponse
import com.day.antsschool.network.dto.BookmarkRequest
import com.day.antsschool.network.dto.BookmarkResponse
import com.day.antsschool.network.dto.ChapterContentResponse
import com.day.antsschool.network.dto.DailyQuizResponse
import com.day.antsschool.network.dto.DailyQuizResultResponse
import com.day.antsschool.network.dto.DailyQuizSubmitRequest
import com.day.antsschool.network.dto.MarketItem
import com.day.antsschool.network.dto.NewsItem
import com.day.antsschool.network.dto.QuizQuestionResponse
import com.day.antsschool.network.dto.TodayQuoteResponse
import com.day.antsschool.network.dto.QuizResultRequest
import com.day.antsschool.network.dto.QuizResultResponse
import com.day.antsschool.network.dto.StageResponse
import com.day.antsschool.network.dto.UserProgressResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("api/chapters")
    suspend fun getChapters(@Query("uid") uid: String?): List<StageResponse>

    @GET("api/chapters/{chapterId}/content")
    suspend fun getChapterContent(@Path("chapterId") chapterId: String): ChapterContentResponse

    @POST("api/quiz/result")
    suspend fun saveQuizResult(@Body request: QuizResultRequest): QuizResultResponse

    @GET("api/quiz/daily")
    suspend fun getDailyQuiz(@Query("uid") uid: String?): DailyQuizResponse

    @POST("api/quiz/daily/result")
    suspend fun submitDailyQuiz(@Body request: DailyQuizSubmitRequest): DailyQuizResultResponse

    @GET("api/quiz/review")
    suspend fun getReviewQuiz(@Query("uid") uid: String): List<QuizQuestionResponse>

    @GET("api/progress/{uid}")
    suspend fun getUserProgress(@Path("uid") uid: String): UserProgressResponse

    @POST("api/progress/{uid}/checkin")
    suspend fun checkIn(@Path("uid") uid: String): UserProgressResponse

    @GET("api/quiz/ai")
    suspend fun getAiQuiz(@Query("topic") topic: String): List<QuizQuestionResponse>

    @GET("api/quotes/today")
    suspend fun getTodayQuote(): TodayQuoteResponse

    @GET("api/market")
    suspend fun getMarket(): List<MarketItem>

    @GET("api/news")
    suspend fun getNews(
        @Query("source") source: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): List<NewsItem>

    @GET("api/news/content")
    suspend fun getNewsContent(
        @Query("url") url: String,
        @Query("source") source: String = ""
    ): ArticleContentResponse

    @GET("api/news/bookmarks")
    suspend fun getBookmarks(@Query("uid") uid: String): List<BookmarkResponse>

    @POST("api/news/bookmarks")
    suspend fun addBookmark(@Body request: BookmarkRequest): BookmarkResponse

    @DELETE("api/news/bookmarks")
    suspend fun removeBookmark(@Query("uid") uid: String, @Query("link") link: String)
}
