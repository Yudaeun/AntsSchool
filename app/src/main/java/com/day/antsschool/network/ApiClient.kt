package com.day.antsschool.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // 에뮬레이터에서 PC localhost 접근: 10.0.2.2
    // Railway 배포 후: "https://your-app.up.railway.app/"
    private const val BASE_URL = "https://antsschool-production.up.railway.app/"

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
        )
        // 연결 실패는 빠르게 감지 (서버 미기동 시 UI 블로킹 방지)
        .connectTimeout(5, TimeUnit.SECONDS)
        // RSS 수집은 서버가 응답 시작 후 최대 20초 허용
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
