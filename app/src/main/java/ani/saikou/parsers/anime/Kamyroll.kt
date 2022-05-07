package ani.saikou.parsers.anime

import ani.saikou.client
import ani.saikou.parsers.*
import com.fasterxml.jackson.annotation.JsonProperty
import okhttp3.FormBody

class Kamyroll : AnimeParser() {

    override val name: String = "Kamyroll"
    override val saveName: String = "kamy_roll"
    override val hostUrl: String = apiUrl
    override val isDubAvailableSeparately: Boolean = false


    override suspend fun loadEpisodes(animeLink: String, extra: Map<String, String>?): List<Episode> {
        return if(extra?.get("type") =="series") {
            val eps = client.get(
                "$hostUrl/content/v1/seasons",
                getHeaders(),
                params = mapOf(
                    channel,
                    locale,
                    "id" to animeLink
                ),
                timeout = 100
            ).parsed<EpisodesResponse>()

            data class Temp(
                val type: String,
                val thumb : String?,
                val series : MutableMap<String,String> = mutableMapOf()
            )

            val epMap = mutableMapOf<Long,Temp>()
            val dataList = (eps.items?:return emptyList()).mapNotNull { item ->
                val tit = item.title?:return@mapNotNull null
                (item.episodes?:return@mapNotNull null).map {
                    Pair(it.sequenceNumber,Temp(it.type,it.images?.thumbnail?.getOrNull(5)?.source, mutableMapOf(tit to it.id)))
                }
            }
            dataList.flatten().forEach {
                epMap[it.first] = epMap[it.first]?:it.second
                epMap[it.first]?.series?.putAll(it.second.series)
            }
            epMap.map {
                if(it.value.thumb!=null)
                    Episode(it.key.toString(),it.value.type, thumbnail = it.value.thumb!!, extra = it.value.series.toMap())
                else
                    Episode(it.key.toString(),it.value.type, extra = it.value.series.toMap())
            }
        }
        else {
            emptyList()
        }
    }

    override suspend fun loadVideoServers(episodeLink: String, extra: Any?): List<VideoServer> {
        return if (extra is Map<*,*>) {
             extra.map {
                 VideoServer(it.key.toString(),it.value.toString())
             }
        }
        else emptyList()
    }

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor = KamyrollExtractor(server)

    class KamyrollExtractor(override val server: VideoServer) : VideoExtractor() {
        override suspend fun extract(): VideoContainer {
            val eps = client.get(
                "$apiUrl/videos/v1/streams",
                getHeaders(),
                params = mapOf(
                    channel,
                    locale,
                    type,
                    "id" to server.embed.url
                ),
                timeout = 100
            ).parsed<StreamsResponse>()

            val vid = listOf(Video(null,true,eps.streams?.find { it.hardsubLocale == "" }?.url?:return VideoContainer(listOf())))
            val subtitle = eps.subtitles?.find { it.locale == "en-US" || it.locale == "en-GB"}.let { listOf(Subtitle("English",it?.url?:return@let null,"ass")) }
            return VideoContainer(vid,subtitle?: emptyList())
        }

        private data class StreamsResponse (
            val subtitles: List<Subtitle>? = null,
            val streams: List<Stream>? = null
        ) {
            data class Stream(
                @JsonProperty("hardsub_locale")
                val hardsubLocale: String? = null,
                val url: String? = null
            )

            data class Subtitle(
                val locale: String? = null,
                val url: String? = null,
                val format: String? = null
            )
        }
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val res = client.get(
            "$hostUrl/content/v1/search",
            getHeaders(),
            params = mapOf(
                channel,
                locale,
                "limit" to "25",
                "query" to query
            )
        ).parsed<SearchResponse>()
        return (res.items?: emptyList()).map { item ->
            item.items.map {
                ShowResponse(
                    name = it.title,
                    link = it.id,
                    coverUrl = it.images?.posterTall?.getOrNull(5)?.source ?: "",
                    extra = mapOf("type" to it.type)
                )
            }
        }.flatten()
    }

    companion object {
        const val apiUrl = "https://api.kamyroll.tech"
        private var headers: Map<String, String>? = null
        private val channel = "channel_id" to "crunchyroll"
        private val locale = "locale" to "en-US"
        private val type = "type" to "adaptive_hls"

        private fun makeBody(vararg data: Pair<String, String>): FormBody {
            val body = FormBody.Builder()
            data.forEach {
                body.add(it.first, it.second)
            }
            return body.build()
        }

        suspend fun getHeaders(): Map<String, String> {
            headers = headers ?: let {
                val res = client.post(
                    "$apiUrl/auth/v1/token",
                    mapOf(
                        "authorization" to "Basic BCoB9f4m4lSlo+fp05PjlwWcplxQXDT+N+1FfvsyoF41YSy8nH+kuJBQowYrVkiZq6PvTvjFEoQQvzJOt3pJZA=="
                    ),
                    data = makeBody(
                        "refresh_token" to "oI1F8udW1uidoJDXn4qPu6vddl546+pLjjIhXnY1bWByOWcAT1HBjehn1HD847EQ2ue8Tl2qPk0FP7mgnS9U4yGP8OZp/ehMZ6rxx7Nx7OgD/Qrl8eMxPNsG3Q864MBMa/ZaWPl+tS4vc4NQ+rR36fWxoTMglxuFOKj4hDKHxGanNpHj7iR0XsgmZX9Fy967BSn9XhO9G7S+Ag7qE3Z7rcJiZOJfh3s4zyRPW7wXYVrEq3UTYQx2oZm2g4Efb7Gm",
                        "grant_type" to "refresh_token",
                        "scope" to "offline_access",
                    )
                ).parsed<AccessToken>()
                mapOf("authorization" to "${res.tokenType} ${res.accessToken}")
            }
            return headers!!
        }

        private data class AccessToken(
            @JsonProperty("access_token")
            val accessToken: String,
            @JsonProperty("token_type")
            val tokenType: String,
        )
    }

    data class EpisodesResponse (
        val total: Long? = null,
        val items: List<Item>? = null
    ) {

        data class Item(
            val title: String? = null,

            @JsonProperty("season_number")
            val seasonNumber: Long? = null,

            @JsonProperty("episode_count")
            val episodeCount: Long? = null,

            val episodes: List<Episode>? = null
        )

        data class Episode(
            val id: String,
            val type:String,

            @JsonProperty("season_number")
            val seasonNumber: Long? = null,

            val episode: String? = null,

            @JsonProperty("sequence_number")
            val sequenceNumber: Long,

            val title: String? = null,
            val description: String? = null,

            @JsonProperty("is_subbed")
            val isSubbed: Boolean? = null,

            @JsonProperty("is_dubbed")
            val isDubbed: Boolean? = null,

            val images: Images? = null
        )

        data class Images(
            val thumbnail: List<Thumbnail>? = null
        )

        data class Thumbnail(
            val width: Long? = null,
            val height: Long? = null,
            val source: String? = null
        )
    }
    private data class SearchResponse(
        val total: Long? = null,
        val items: List<ResponseItem>? = null
    ) {
        data class ResponseItem(val items: List<ItemItem>)

        data class ItemItem(
            val id: String,
            @JsonProperty("media_type")
            val type : String,
            val title: String,
            val images: Images? = null,
        )

        data class Images(
            @JsonProperty("poster_tall")
            val posterTall: List<PosterTall>
        )

        data class PosterTall(
            val source: String,
        )
    }
}