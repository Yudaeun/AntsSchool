package com.day.antsschool.server.api

import com.day.antsschool.server.api.dto.TodayQuoteResponse
import com.day.antsschool.server.repository.EconomicQuoteRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/quotes")
class QuoteController(private val quoteRepository: EconomicQuoteRepository) {

    @GetMapping("/today")
    fun getTodayQuote(): TodayQuoteResponse {
        val all = quoteRepository.findAll()
        if (all.isEmpty()) return TodayQuoteResponse("투자의 첫 번째 원칙은 절대 돈을 잃지 않는 것이다.", "워런 버핏")
        val q = all.random()
        return TodayQuoteResponse(q.quote, q.author)
    }
}
