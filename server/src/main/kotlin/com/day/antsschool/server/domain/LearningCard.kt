package com.day.antsschool.server.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(name = "learning_cards", uniqueConstraints = [UniqueConstraint(columnNames = ["chapter_id", "card_order"])])
data class LearningCard(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val chapterId: String = "",
    val cardOrder: Int = 0,
    val emoji: String = "",
    val title: String = "",
    @Column(length = 1000)
    val content: String = ""
)
