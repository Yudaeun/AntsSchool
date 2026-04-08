package com.day.antsschool.server.domain

import com.day.antsschool.server.config.StringListConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(name = "quiz_questions", uniqueConstraints = [UniqueConstraint(columnNames = ["chapter_id", "question_order"])])
data class QuizQuestion(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val chapterId: String = "",
    val questionOrder: Int = 0,
    @Column(length = 1000)
    val question: String = "",
    @Convert(converter = StringListConverter::class)
    @Column(length = 2000)
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0,
    @Column(length = 1000)
    val explanation: String = ""
)
