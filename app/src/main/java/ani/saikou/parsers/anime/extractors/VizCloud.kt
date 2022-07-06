package ani.saikou.parsers.anime.extractors

import ani.saikou.FileUrl
import ani.saikou.client
import ani.saikou.parsers.Video
import ani.saikou.parsers.VideoContainer
import ani.saikou.parsers.VideoExtractor
import ani.saikou.parsers.VideoServer
import ani.saikou.parsers.anime.NineAnime.Companion.encrypt
import com.google.gson.annotations.SerializedName

class VizCloud(override val server: VideoServer) : VideoExtractor() {

    private data class Sources(@SerializedName("file") val file: String)
    private data class Media(@SerializedName("sources") val sources: List<Sources>)
    private data class Data(@SerializedName("media") val media: Media)
    private data class Response(@SerializedName("data") val data: Data)

    private val regex = Regex("(.+?/)e(?:mbed)?/([a-zA-Z0-9]+)")

    private fun cipher(key: String, text: String): String {
        val arr = IntArray(256) { it }
        var u = 0

        arr.indices.forEach { 
            u = (u + arr[it] + key[it % key.length].code) % 256
            val r = arr[it]
            arr[it] = arr[u]
            arr[u] = r
        }

        u = 0
        var c = 0

        return text.indices.map {
            c = (c + 1) % 256
            u = (u + arr[c]) % 256
            val r = arr[c]
            arr[c] = arr[u]
            arr[u] = r
            (text[it].code xor arr[(arr[c] + arr[u]) % 256]).toChar()
        }.joinToString("")
    }

    override suspend fun extract(): VideoContainer {

        val embed = server.embed
        val group = regex.find(embed.url)?.groupValues!!

        val host = group[1]
        val viz = getKey()
        val id = encrypt(
            cipher(
                viz.cipherKey,
                encrypt(group[2], viz.encryptKey)
            ),
            viz.encryptKey
        ).replace("/", "_").replace("=", "")

        val link = "${host}mediainfo/${dashify(id, viz.dashTable)}?key=${viz.mainKey}"
        val response = client.get(link, embed.headers)
        println(link)
        if (!response.text.startsWith("{")) throw Exception("Seems like 9Anime kiddies changed stuff again, Go touch some grass for bout an hour Or use a different Server")
        return VideoContainer(response.parsed<Response>().data.media.sources.map {
            val file = FileUrl(it.file, mapOf("referer" to host))
            Video(null, true, file)
        })
    }

    companion object {
        private var lastChecked = 0L
        private const val jsonLink = "https://raw.githubusercontent.com/chenkaslowankiya/BruhFlow/main/keys.json"
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
            @SerializedName("cipherKey") val cipherKey: String,
            @SerializedName("mainKey") val mainKey: String,
            @SerializedName("encryptKey") val encryptKey: String,
            @SerializedName("dashTable") val dashTable: String
        )

        private const val baseTable = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/_"

        private fun dashify(id: String, dashTable: String): String {
            val table = dashTable.split(" ")
            return id.mapIndexedNotNull { i, c ->
                table.getOrNull((baseTable.indexOf(c) * 16) + (i % 16))
            }.joinToString("-")
        }
    }
}

