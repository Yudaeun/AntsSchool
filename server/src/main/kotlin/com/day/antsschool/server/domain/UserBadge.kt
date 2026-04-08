package com.day.antsschool.server.domain

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "user_badges")
data class UserBadge(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val uid: String = "",
    val badgeId: String = "",   // "FIRST_STUDY", "PERFECT_QUIZ", "STREAK_7", "INTRO_COMPLETE"
    val earnedAt: String = ""   // "2026-03-28"
)
