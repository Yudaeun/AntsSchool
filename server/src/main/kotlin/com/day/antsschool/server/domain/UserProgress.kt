package com.day.antsschool.server.domain

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "user_progress")
data class UserProgress(
    @Id
    val uid: String = "",          // Firebase UID
    val level: Int = 1,
    val xp: Int = 0,
    val streak: Int = 0,
    val lastLoginDate: String = "",
    val lastDailyQuizDate: String = "",  // 오늘 데일리 퀴즈 완료 여부 체크용
    @jakarta.persistence.Column(length = 4000)
    val checkinDates: String = "",       // "|" 구분 출석 날짜 목록 (최근 90일)
    val totalCorrect: Int = 0,           // 퀴즈 총 정답 수
    val totalAnswered: Int = 0           // 퀴즈 총 응답 수
)
