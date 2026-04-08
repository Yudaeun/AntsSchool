package com.day.antsschool.server.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "economic_quotes")
data class EconomicQuote(
    @Id
    val id: Long = 0,
    @Column(length = 500)
    val quote: String = "",
    val author: String = ""
)
