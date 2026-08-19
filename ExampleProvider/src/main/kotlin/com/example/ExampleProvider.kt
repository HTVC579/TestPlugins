package com.example

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.TvType
import android.content.Context

@CloudstreamPlugin
class BLVietsubPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(BLVietsubProvider())
    }
}

class BLVietsubProvider : MainAPI() {
    override var name = "BLVietsub"
    override var mainUrl = "https://example.com"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang = "vi"
    override val hasMainPage = true
}
