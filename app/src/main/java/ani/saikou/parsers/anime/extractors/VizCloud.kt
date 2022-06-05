package ani.saikou.parsers.anime.extractors

import ani.saikou.FileUrl
import ani.saikou.client
import ani.saikou.parsers.Video
import ani.saikou.parsers.VideoContainer
import ani.saikou.parsers.VideoExtractor
import ani.saikou.parsers.VideoServer
import ani.saikou.parsers.anime.NineAnime.Companion.cipher
import ani.saikou.parsers.anime.NineAnime.Companion.encrypt

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
        val viz = getKey()
        val id = encrypt(cipher(viz.cipherKey, encrypt(group[2], viz.encryptKey)),viz.encryptKey).replace("/", "_").replace("=","")

        val response = client.get(
            "${host}mediainfo/$id?key=${viz.mainKey}",
            embed.headers
        )

        println("${host}mediainfo/$id?key=${viz.mainKey}")

        if(!response.text.startsWith("{")) throw Exception("Seems like 9Anime kiddies changed keys again,Go touch some grass for bout an hour Or use a different Server")
        return VideoContainer(response.parsed<Response>().data.media.sources.map {
            val file = FileUrl(it.file, mapOf("referer" to host))
            Video(null, true, file)
        })
    }

    companion object {
        private var lastChecked = 0L
        private const val jsonLink = "https://raw.githubusercontent.com/chekaslowakiya/BruhFlow/main/keys.json"
        private var cipherKey: VizCloudKey? = null
        suspend fun getKey(): VizCloudKey {
            cipherKey = if (cipherKey != null && (lastChecked - System.currentTimeMillis()) < 1000 * 60 * 30) cipherKey!!
            else {
                lastChecked = System.currentTimeMillis()
                client.get(jsonLink).parsed()
            }
            return cipherKey!!
        }

        data class VizCloudKey(
            val cipherKey: String,
            val mainKey: String,
            val encryptKey: String,
        )
    }
}