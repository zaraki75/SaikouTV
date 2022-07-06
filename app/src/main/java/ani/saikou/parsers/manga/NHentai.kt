package ani.saikou.parsers.manga

import androidx.core.text.isDigitsOnly
import ani.saikou.client
import ani.saikou.parsers.MangaChapter
import ani.saikou.parsers.MangaImage
import ani.saikou.parsers.MangaParser
import ani.saikou.parsers.ShowResponse
import com.google.gson.annotations.SerializedName

class NHentai : MangaParser() {

    override val name = "NHentai"
    override val saveName = "n_hentai"
    override val hostUrl = "https://nhentai.net"
    override val isNSFW = true

    override suspend fun loadChapters(mangaLink: String, extra: Map<String, String>?): List<MangaChapter> {
        val id = mangaLink.substringAfter("g/")
        val json = client.get("https://nhentai.net/api/gallery/$id").parsed<MangaResponse>()
        // There's really no "chapter(s)" in nhentai afaik. So here it's being returned as if it's the first chapter.
        return listOf(
            MangaChapter(
                number = "1",
                link = "$hostUrl/g/$id",
                title = json.title.pretty
            )
        )
    }

    override suspend fun loadImages(chapterLink: String): List<MangaImage> {
        val id = chapterLink.substringAfter("g/")
        val json = client.get("$hostUrl/api/gallery/$id").parsed<MangaResponse>()
        val ext = ext(json.images.pages[0].t)
        return (1 until (json.images.pages.size - 1)).map {
            MangaImage("https://i.nhentai.net/galleries/${json.media_id}/$it.$ext")
        }
    }

    override suspend fun search(query: String): List<ShowResponse> {
        return if (query.startsWith("#") || query.isDigitsOnly()) {
            val id = query.replace("#", "")
            val document = client.get("$hostUrl/g/$id").document.select("div#content div#bigcontainer.container")
            val coverUrl = document.select("div#cover a img.lazyload").attr("data-src")
            val title = document.select("div#info-block div#info h1.title span.pretty").text()
            listOf(
                ShowResponse(
                    name = title,
                    link = "$hostUrl/g/$id",
                    coverUrl = coverUrl,
                )
            )
        } else {
            val finalQuery = "$query language:english"
            val json = client.get("$hostUrl/api/galleries/search?query=${encode(finalQuery)}").parsed<SearchResponse>()
            json.result.map {
                ShowResponse(
                    name = it.title.pretty,
                    link = "$hostUrl/g/${it.id}",
                    coverUrl = "https://t.nhentai.net/galleries/${it.media_id}/cover.jpg",
                )
            }
        }
    }


    // convert to proper extension from API
    private fun ext(t: String): String {
        return when (t) {
            "j"  -> "jpg"
            "p"  -> "png"
            "g"  -> "gif"
            else -> "jpg" // unreachable
        }
    }

    private data class SearchResponse(
        @SerializedName("result") val result: List<Result>,
    ) {
        data class Result(
            @SerializedName("id") val id: Int,
            @SerializedName("media_id") val media_id: Int,
            @SerializedName("title") val title: Title,
        ) {
            data class Title(
                @SerializedName("english") val english: String,
                @SerializedName("japanese") val japanese: String,
                @SerializedName("pretty") val pretty: String
            )
        }
    }

    private data class MangaResponse(
        @SerializedName("media_id") val media_id: Int,
        @SerializedName("title") val title: Title,
        @SerializedName("images") val images: Pages
    ) {
        data class Title(@SerializedName("pretty") val pretty: String)
        data class Pages(@SerializedName("pages") val pages: List<Page>) {
            data class Page(
                @SerializedName("t") val t: String, // extension
                @SerializedName("w") val w: Int,    // width
                @SerializedName("h") val h: Int     // height
            )
        }
    }
}
