package com.example

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import android.content.Context
import org.jsoup.nodes.Document

@CloudstreamPlugin
class BLVietsubPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(BLVietsubProvider())
    }
}

class BLVietsubProvider : MainAPI() {
    override var mainUrl = "https://blvietsub.org"
    override var name = "BLVietsub"
    override val hasMainPage = true
    override var lang = "vi"
    override val supportedTypes = setOf(
        TvType.AsianDrama,
        TvType.Movie
    )

    override val mainPage = mainPageOf(
        "$mainUrl/phim-moi/" to "Phim Mới",
        "$mainUrl/the-loai/phim-bo/" to "Phim Bộ",
        "$mainUrl/the-loai/phim-le/" to "Phim Lẻ"
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val url = if (page <= 1) request.data else "${request.data}page/$page/"
        val document = app.get(url).document
        val home = document.select("article.item, div.item").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Document.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h3.title, h2.entry-title, a.title")?.text()?.trim() ?: return null
        val href = fixUrl(this.selectFirst("a")?.attr("href") ?: return null)
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src") ?: this.selectFirst("img")?.attr("src"))
        return newAnimeSearchResponse(title, href, TvType.AsianDrama) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchUrl = "$mainUrl/?s=$query"
        val doc = app.get(searchUrl).document
        return doc.select("article.item, div.item").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1.entry-title, h1.title")?.text()?.trim() ?: "Không rõ tiêu đề"
        val poster = fixUrlNull(doc.selectFirst("div.poster img, img.thumb")?.attr("src"))
        val description = doc.selectFirst("div.description, div.entry-content")?.text()?.trim()

        val episodes = doc.select("ul.episodes a, div.les-content a").mapIndexed { index, element ->
            val epHref = fixUrl(element.attr("href"))
            val epName = element.text().trim().ifEmpty { "Tập ${index + 1}" }
            Episode(epHref, epName)
        }

        return newTvSeriesLoadResponse(title, url, TvType.AsianDrama, episodes) {
            this.posterUrl = poster
            this.plot = description
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCdn: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document
        val iframeSrc = doc.selectFirst("iframe")?.attr("src")
        if (!iframeSrc.isNullExEmpty()) {
            loadExtractor(fixUrl(iframeSrc), subtitleCallback, callback)
        }
        return true
    }
}
