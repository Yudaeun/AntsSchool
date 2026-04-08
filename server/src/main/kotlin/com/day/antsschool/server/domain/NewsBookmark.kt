package com.day.antsschool.server.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "news_bookmarks")
data class NewsBookmark(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val uid: String = "",
    val title: String = "",
    val link: String = "",
    @Column(length = 1000)
    val description: String = "",
    val pubDate: String = "",
    val source: String = "",
    val savedAt: String = ""
)
