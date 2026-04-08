package com.day.antsschool.server.domain

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "chapter_progress")
data class ChapterProgress(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val uid: String = "",
    val chapterId: String = "",
    val status: String = "IN_PROGRESS",  // "IN_PROGRESS" | "COMPLETED"
    val progressPercent: Float = 0f
)
