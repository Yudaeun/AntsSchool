package com.day.antsschool.server.domain

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "wrong_answers")
data class WrongAnswer(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val uid: String = "",
    val questionId: Long = 0,
    val question: String = "",
    val options: String = "",       // "|" 구분자로 저장
    val correctIndex: Int = 0,
    val explanation: String = "",
    val wrongCount: Int = 1,
    val lastWrongAt: String = ""
)
