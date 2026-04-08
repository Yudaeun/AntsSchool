package com.day.antsschool.server.api

import com.day.antsschool.server.api.dto.BookmarkRequest
import com.day.antsschool.server.api.dto.BookmarkResponse
import com.day.antsschool.server.api.dto.NewsItem
import com.day.antsschool.server.domain.NewsBookmark
import com.day.antsschool.server.repository.NewsBookmarkRepository
import org.jsoup.Jsoup
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.time.LocalDate
import java.util.concurrent.CompletableFuture
import javax.xml.parsers.DocumentBuilderFactory

@RestController
@RequestMapping("/api/news")
class NewsController(
    private val newsBookmarkRepository: NewsBookmarkRepository
) {

    private val RSS_FEEDS = listOf(
        "https://www.yna.co.kr/rss/economy.xml" to "연합뉴스",
        "https://www.mk.co.kr/rss/economy/" to "매일경제",
        "https://www.hankyung.com/feed/economy" to "한국경제",
        "https://rss.mt.co.kr/rss/economy.xml" to "머니투데이"
    )

    private val SOURCE_SELECTORS = mapOf(
        "연합뉴스"   to listOf(".article-txt", ".story-news article"),
        "매일경제"   to listOf(".news_cnt_detail_wrap", ".art_txt"),
        "한국경제"   to listOf(".article-body", ".article__body"),
        "머니투데이" to listOf("#textBody", ".news_cnt_detail_wrap")
    )

    // 5분 캐시 — RSS 피드를 매 요청마다 새로 가져오면 앱 첫 로드 시 10~12초 대기 발생
    @Volatile private var cacheTime = 0L
    @Volatile private var cachedNews: List<NewsItem> = emptyList()

    // ── 메인 뉴스 목록 — RSS만 빠르게 반환 (본문 fetching 없음) ──────────

    @GetMapping
    fun getNews(
        @RequestParam source: String? = null,
        @RequestParam page: Int = 0,
        @RequestParam size: Int = 20
    ): List<NewsItem> {
        val now = System.currentTimeMillis()
        // 전체 소스 요청일 때만 캐시 사용 (소스 필터 요청은 항상 새로 가져옴)
        val allNews = if (source == null && now - cacheTime < 5 * 60_000L && cachedNews.isNotEmpty()) {
            cachedNews
        } else {
            val feeds = if (source != null) RSS_FEEDS.filter { it.second == source } else RSS_FEEDS
            val futures = feeds.map { (url, src) ->
                CompletableFuture.supplyAsync { fetchRss(url, src) }
            }
            val fetched = futures.mapNotNull { runCatching { it.join() }.getOrNull() }
                .flatten()
                .sortedByDescending { it.pubDate }
            if (source == null && fetched.isNotEmpty()) {
                cachedNews = fetched
                cacheTime = now
            }
            fetched
        }
        val fromIndex = page * size
        if (fromIndex >= allNews.size) return emptyList()
        return allNews.subList(fromIndex, minOf(fromIndex + size, allNews.size))
    }

    // ── 북마크 목록 조회 ──────────────────────────────────────────────────

    @GetMapping("/bookmarks")
    fun getBookmarks(@RequestParam uid: String): List<BookmarkResponse> =
        newsBookmarkRepository.findByUidOrderBySavedAtDesc(uid).map { it.toResponse() }

    // ── 북마크 추가 ──────────────────────────────────────────────────────

    @PostMapping("/bookmarks")
    fun addBookmark(@RequestBody req: BookmarkRequest): BookmarkResponse {
        val existing = newsBookmarkRepository
            .findByUidOrderBySavedAtDesc(req.uid)
            .firstOrNull { it.link == req.link }
        if (existing != null) return existing.toResponse()
        return newsBookmarkRepository.save(
            NewsBookmark(
                uid = req.uid,
                title = req.title,
                link = req.link,
                description = req.description,
                pubDate = req.pubDate,
                source = req.source,
                savedAt = LocalDate.now().toString()
            )
        ).toResponse()
    }

    // ── 북마크 삭제 ──────────────────────────────────────────────────────

    @DeleteMapping("/bookmarks")
    fun removeBookmark(@RequestParam uid: String, @RequestParam link: String) {
        newsBookmarkRepository.deleteByUidAndLink(uid, link)
    }

    // ── 기사 본문 — 모달 열릴 때 별도 호출 ────────────────────────────────

    @GetMapping("/content")
    fun getArticleContent(
        @RequestParam url: String,
        @RequestParam(defaultValue = "") source: String
    ): Map<String, String> {
        val body = fetchArticleBody(url, source).formatParagraphs()
        return mapOf("body" to body)
    }

    // ── RSS 파싱 (Jsoup으로 HTTP 연결, JAXP로 XML 파싱) ───────────────────

    private fun fetchRss(rssUrl: String, source: String): List<NewsItem> {
        return try {
            // Jsoup으로 연결: HTTPS/리다이렉트 처리가 HttpURLConnection보다 안정적
            val bytes = Jsoup.connect(rssUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "application/rss+xml, application/xml, text/xml, */*")
                .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8")
                .timeout(12000)
                .ignoreContentType(true)
                .execute()
                .bodyAsBytes()

            val factory = DocumentBuilderFactory.newInstance().also { it.isNamespaceAware = true }
            val doc = factory.newDocumentBuilder().parse(java.io.ByteArrayInputStream(bytes))

            val items: NodeList = doc.getElementsByTagName("item")
            (0 until items.length).mapNotNull { i ->
                val item = items.item(i) as? Element ?: return@mapNotNull null
                val encoded = item.getEncodedContent().cleanHtml()
                val description = item.getText("description").cleanHtml()
                val body = if (encoded.length > description.length) encoded else description
                NewsItem(
                    title = item.getText("title").cleanHtml(),
                    link = item.getText("link").trim(),
                    description = body,
                    pubDate = item.getText("pubDate"),
                    source = source
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── Jsoup 기사 본문 파싱 (/content 엔드포인트 전용) ──────────────────

    private fun fetchArticleBody(url: String, source: String): String {
        if (url.isEmpty()) return ""
        return try {
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .timeout(8000)
                .get()
            doc.select("script, style, nav, header, footer, .ad, .advertisement, .banner, .related").remove()

            val selectors = (SOURCE_SELECTORS[source] ?: emptyList()) + listOf(
                "[itemprop='articleBody']", "article", ".article-content",
                ".article-body", ".news-content", ".news_body", "main"
            )
            for (selector in selectors) {
                val el = doc.select(selector).firstOrNull() ?: continue
                val text = el.text().trim()
                if (text.length > 200) return text
            }
            // 셀렉터 실패 시 <p> 합치기
            doc.select("p")
                .map { it.text().trim() }
                .filter { it.length > 30 }
                .joinToString("\n\n")
        } catch (e: Exception) {
            ""
        }
    }

    private fun Element.getText(tag: String): String =
        getElementsByTagName(tag).item(0)?.textContent?.trim() ?: ""

    private fun Element.getEncodedContent(): String {
        val nodes = childNodes
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            if (node.localName == "encoded" || node.nodeName == "content:encoded")
                return node.textContent?.trim() ?: ""
        }
        return ""
    }

    private fun String.cleanHtml(): String =
        replace(Regex("<[^>]+>"), " ")
            .replace("&lt;", "<").replace("&gt;", ">")
            .replace("&amp;", "&").replace("&nbsp;", " ")
            .replace("&quot;", "\"").replace("&apos;", "'")
            .replace(Regex("\\s{2,}"), " ").trim()

    private fun String.formatParagraphs(n: Int = 3): String {
        val sentences = split(Regex("(?<=[.!?])(?=\\s)")).map { it.trim() }.filter { it.isNotEmpty() }
        if (sentences.size <= n) return this
        return sentences.chunked(n).joinToString("\n\n") { it.joinToString(" ") }
    }
}

private fun NewsBookmark.toResponse() = BookmarkResponse(
    id = id,
    title = title,
    link = link,
    description = description,
    pubDate = pubDate,
    source = source,
    savedAt = savedAt
)
