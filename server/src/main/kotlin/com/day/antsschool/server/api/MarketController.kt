package com.day.antsschool.server.api

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URL
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/api/market")
class MarketController {

    data class MarketItem(
        val symbol: String,
        val name: String,
        val price: String,
        val changePercent: Double,
        @get:JsonProperty("isUp")   // Boolean "is" prefix → Jackson이 "up"으로 직렬화하는 문제 방지
        val isUp: Boolean
    )

    private val INDICES = listOf(
        "^KS11" to "코스피",
        "^KQ11" to "코스닥",
        "^IXIC" to "나스닥",
        "^GSPC" to "S&P500",
        "^N225" to "닛케이",
        "GC=F"  to "금(USD)"
    )

    private val mapper = ObjectMapper()

    // 3분 캐시 — Yahoo Finance 과호출 방지
    @Volatile private var cacheTime = 0L
    @Volatile private var cachedItems: List<MarketItem> = emptyList()

    @GetMapping
    fun getMarket(): List<MarketItem> {
        val now = System.currentTimeMillis()
        if (now - cacheTime < 3 * 60_000L && cachedItems.isNotEmpty()) return cachedItems

        val futures = INDICES.map { (symbol, name) ->
            CompletableFuture.supplyAsync { fetchQuote(symbol, name) }
        }
        val items = futures.mapNotNull { runCatching { it.join() }.getOrNull() }
        if (items.isNotEmpty()) { cachedItems = items; cacheTime = now }
        return if (items.isNotEmpty()) items else cachedItems
    }

    private fun fetchQuote(symbol: String, name: String): MarketItem? {
        return try {
            val encoded = java.net.URLEncoder.encode(symbol, "UTF-8")
            val url = "https://query2.finance.yahoo.com/v8/finance/chart/$encoded?interval=1d&range=1d"
            val conn = URL(url).openConnection() as java.net.HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            val json = conn.inputStream.bufferedReader().readText()
            val meta = mapper.readTree(json)
                .path("chart").path("result").get(0)?.path("meta") ?: return null

            val price = meta.path("regularMarketPrice").asDouble()
            val prevClose = meta.path("previousClose").asDouble()
            if (price <= 0) return null

            val changePct = if (prevClose > 0) (price - prevClose) / prevClose * 100.0 else 0.0
            val formatted = when {
                price >= 1000 -> String.format("%,.0f", price)
                price >= 1    -> String.format("%.2f", price)
                else          -> String.format("%.4f", price)
            }
            MarketItem(symbol, name, formatted, changePct, changePct >= 0)
        } catch (e: Exception) {
            null
        }
    }
}
