package ani.saikou.anime.source.extractors

import android.net.Uri
import ani.saikou.anilist.httpClient
import ani.saikou.anime.Episode
import ani.saikou.anime.source.Extractor
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.Request
import org.jsoup.Jsoup

class VizCloud(val referer: String) : Extractor() {

    override fun getStreamLinks(name: String, url: String): Episode.StreamLinks {
        val embedded = httpClient.newCall(
            Request.Builder().url(url)
                .header("Referer", referer).build()
        ).execute().body!!.string()
        val skey = Jsoup.parse(embedded).selectFirst("script:containsData(window.skey = )")!!.data()
            .substringAfter("window.skey = \'").substringBefore("\'")
        val sourceObject = Json.decodeFromString<JsonObject>(
            httpClient.newCall(
                Request.Builder().url(
                    "${ url
                        .replace("/e/", "/info/")
                        .replace("/embed/", "/info/")
                    }?skey=$skey"
                )
                    .header("Referer", referer).build()
            ).execute().body!!.string()
        )
        val mediaSources = sourceObject["media"]!!.jsonObject["sources"]!!.jsonArray.jsonArray[0].jsonObject["file"].toString().trim('"')
        return Episode.StreamLinks(name, listOf(Episode.Quality(mediaSources, "Multi Quality", null)),mutableMapOf("referer" to "https://${Uri.parse(url).host}/"))
    }
}