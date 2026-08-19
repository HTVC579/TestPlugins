package com.blvietsub

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Document

class BLVietsubProvider : MainAPI() {
    override var mainUrl = "https://www.blvietsub.com"
    override var name = "BLVietsub"
    override val hasMainPage = true
    override var lang = "vi"
    override val supportedTypes = setOf(
        TvType.AsianDrama,
        TvType.Movie
    )

    override val mainPage = mainPageOf(
        "$mainUrl/phim-moi/" to "Phim Mới Cập Nhật",
        "$mainUrl/the-loai/phim-bo/" to "Phim Bộ",
        "$mainUrl/the-loai/phim-le/" to "Phim Lẻ"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) request.data else "${request.data}page/$page/"
        val document = app.get(url).document
        val home = document.select("div.list-filmes article, div.item").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Document.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h2, h3, .title")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src") ?: this.selectFirst("img")?.attr("data-src"))

        return newTvSeriesSearchResponse(title, href, TvType.AsianDrama) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchUrl = "$mainUrl/?s=$query"
        val document = app.get(searchUrl).document
        return document.select("div.list-filmes article, div.result-item").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        val title = document.selectFirst("h1.entry-title, h1")?.text()?.trim() ?: "Không rõ tên"
        val poster = fixUrlNull(document.selectFirst("div.poster img")?.attr("src"))
        val plot = document.selectFirst("div#info, div.entry-content, p.description")?.text()?.trim()

        val episodes = mutableListOf<Episode>()
        val episodeElements = document.select("ul.episodes-list li, div.les-content a")

        if (episodeElements.isNotEmpty()) {
            episodeElements.forEachIndexed { index, element ->
                val epHref = fixUrlNull(element.attr("href")) ?: return@forEachIndexed
                val epName = element.text().trim()
                episodes.add(
                    newEpisode(epHref) {
                        this.name = if (epName.isNotEmpty()) epName else "Tập ${index + 1}"
                        this.episode = index + 1
                    }
                )
            }
        } else {
            episodes.add(
                newEpisode(url) {
                    this.name = "Full"
                    this.episode = 1
                }
            )
        }

        return newTvSeriesLoadResponse(title, url, TvType.AsianDrama, episodes) {
            this.posterUrl = poster
            this.plot = plot
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document

        val iframes = document.select("iframe[src]")
        for (iframe in iframes) {
            val src = fixUrlNull(iframe.attr("src")) ?: continue
            loadExtractor(src, subtitleCallback, callback)
        }

        return true
    }
