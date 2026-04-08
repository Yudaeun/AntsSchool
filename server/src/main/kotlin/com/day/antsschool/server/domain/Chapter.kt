package com.day.antsschool.server.domain

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "chapters")
data class Chapter(
    @Id
    val id: String = "",           // "입문01", "초급02"
    val title: String = "",
    val stage: String = "",        // "입문", "초급", "중급"
    val stageOrder: Int = 0,       // 단계 내 순서
    val globalOrder: Int = 0       // 전체 순서 (잠금 해제 판단용)
)
