package ani.saikou.parsers.anime.extractors

import android.net.Uri
import ani.saikou.FileUrl
import ani.saikou.client
import ani.saikou.findBetween
import ani.saikou.parsers.Video
import ani.saikou.parsers.VideoContainer
import ani.saikou.parsers.VideoExtractor
import ani.saikou.parsers.VideoServer

class VizCloud(override val server: VideoServer) : VideoExtractor() {

    private data class Sources(val file: String, val label: String?)
    private data class Media(val sources: List<Sources>)
    private data class Data(val media: Media)
    private data class Response(val success: Boolean, val data: Data)

    override suspend fun extract(): VideoContainer {

        val url = server.embed.url
        val headers = server.embed.headers

        val head = client.get(url, headers).document.select("head")[0]
        val skey = head.data().findBetween("window.skey = '","';")!!
        val response = client.get(
            url.replace("/embed/", "/info/").replace("/e/", "/info/"),
            headers,
            params = mapOf("skey" to skey)
        )

        return VideoContainer(response.parsed<Response>().data.media.sources.map {
            val file = FileUrl(it.file, mapOf("referer" to "https://${Uri.parse(url).host}/"))
            Video(null, true, file, null, it.label)
        })
    }
}