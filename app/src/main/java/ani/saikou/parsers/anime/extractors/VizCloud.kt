package ani.saikou.parsers.anime.extractors

import ani.saikou.FileUrl
import ani.saikou.client
import ani.saikou.parsers.Video
import ani.saikou.parsers.VideoContainer
import ani.saikou.parsers.VideoExtractor
import ani.saikou.parsers.VideoServer
import ani.saikou.parsers.anime.NineAnime.Companion.cipher
import ani.saikou.parsers.anime.NineAnime.Companion.encrypt
import com.fasterxml.jackson.annotation.JsonProperty

class VizCloud(override val server: VideoServer) : VideoExtractor() {

    private data class Sources(val file: String)
    private data class Media(val sources: List<Sources>)
    private data class Data(val media: Media)
    private data class Response(val data: Data)

    private val regex = Regex("(.+?/)e(?:mbed)?/([a-zA-Z0-9]+)")

    override suspend fun extract(): VideoContainer {

        val embed = server.embed
        val group = regex.find(embed.url)?.groupValues!!

        val host = group[1]
        val id = encrypt(cipher(getKey(), encrypt(group[2], key)), key).replace("/", "_")

        val response = client.get(
            "${host}info/$id",
            embed.headers
        )
        if(!response.text.startsWith("{")) throw Exception("Seems like 9Anime kiddies changed keys again,Go touch some grass for bout an hour Or use a different Server")
        return VideoContainer(response.parsed<Response>().data.media.sources.map {
            val file = FileUrl(it.file, mapOf("referer" to host))
            Video(null, true, file)
        })
    }

    companion object {
        private var key = "51wJ0FDq/UVCefLopEcmK3ni4WIQztMjZdSYOsbHr9R2h7PvxBGAuglaN8+kXT6y"
        private var defaultKey = "PmfGc5uJ7V0a5Wfy"
        private var lastChecked = 0L
        private const val jsonLink =
            "https://raw.githubusercontent.com/justfoolingaround/animdl-provider-benchmarks/master/api/selgen.json"
        private var cipherKey: String? = null
        suspend fun getKey(): String {
            cipherKey = if (cipherKey != null && (lastChecked - System.currentTimeMillis()) < 1000 * 60 * 30) cipherKey?: defaultKey
            else {
                lastChecked = System.currentTimeMillis()
                client.get(jsonLink).parsed<VizCloudKey>().cipherKey
            }
            return cipherKey?: defaultKey
        }

        data class VizCloudKey(
            @JsonProperty("cipher_key")
            val cipherKey: String?=null,
        )
    }
}