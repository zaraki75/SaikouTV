package ani.saikou.parsers.anime.extractors

import ani.saikou.client
import ani.saikou.parsers.Video
import ani.saikou.parsers.VideoContainer
import ani.saikou.parsers.VideoExtractor
import ani.saikou.parsers.VideoServer

class VidVard(override val server: VideoServer) : VideoExtractor() {

    private val mainUrl = "https://videovard.sx"

    override suspend fun extract(): VideoContainer {
        val url = server.embed.url
        val id = url.substringAfter("/e/").substringBefore("/")
        val hash = client.get("$mainUrl/api/make/hash/$id").parsed<HashResponse>().hash
            ?: throw NoSuchElementException("Hash not found")
        val res = client.post(
            "$mainUrl/api/player/setup",
            data = mapOf(
                "cmd" to "get_stream",
                "file_code" to id,
                "hash" to hash
            )
        ).parsed<SetupResponse>()
        val m3u8 = decode(res.src, res.seed)
        return VideoContainer(listOf(Video(null, true, m3u8)))
    }

    private fun decode(string: String, seed: String): String {
        return ""
    }

    private data class HashResponse(
        val hash: String? = null
    )

    private data class SetupResponse(
        val seed: String,
        val src: String
    )
}