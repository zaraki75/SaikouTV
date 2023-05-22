package ani.saikou.parsers.anime

import ani.saikou.FileUrl
import ani.saikou.Mapper
import ani.saikou.client
import ani.saikou.levenshtein
import ani.saikou.media.Media
import ani.saikou.parsers.*
import ani.saikou.parsers.anime.extractors.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs

class KickAssAnime : AnimeParser() {

    override val name = "KickAssAnime"
    override val saveName = "kick_ass_anime"
    override val isDubAvailableSeparately = true

    override val hostUrl = "https://kaas.am"
    private val apiUrl = "$hostUrl/api"
    private val thumbnailUrl = "$hostUrl/images/thumbnail"

    override suspend fun loadEpisodes(animeLink: String, extra: Map<String, String>?): List<Episode> {
        // Sometimes we get back valid JSON, sometimes not :think:
        val json = client.get(animeLink, headers = mapOf("accept" to "application/json"))
                .parsed<EpisodesJSON>()

        val episodes = mutableListOf(json.episodes)

        // Smaller page number == more recent episodes
        for (page in 1..(json.pages.toIntOrNull() ?: 1)) {
            val url = animeLink.substringBefore("&page")
            val pageEpisodes =
                client.get("$url&page=$page", headers = mapOf("accept" to "application/json")).parsed<EpisodesJSON>().episodes
            episodes += pageEpisodes
        }

        return episodes.flatten()
          .map { episode ->
                Episode(
                    number = episode.episodeNumber ?: "Movie",
                    link = "$apiUrl/watch/${episode.slug}",
                    title =
                    when (episode.episodeNumber) {
                        null -> "${extra?.get("query") ?: ""} Movie".trim()
                        else -> null // We want to use title from MALSync
                    },
                    thumbnail = FileUrl(
                        "$thumbnailUrl/${episode.thumbnail.hq?.slug}.${episode.thumbnail.hq?.formats?.last()}",
                        // For some reason the thumbnails don't load using the default user-agent
                        mapOf(
                            "user-agent" to
                            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36"
                        )
                    ),
                )
            }.asReversed()
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val eps = client.post("$apiUrl/search",json = "{\"query\":\"$query\"}").parsed<JsonArray>().mapNotNull { res ->
            val link =  "$apiUrl/season/${res.jsonObject["_id"]?.jsonPrimitive?.content?:return@mapNotNull null}"
            val slice = "{\"seasons\":${client.get(link).body.string()}}"
            Mapper.parse<SeasonsJSON>(slice).seasons
        }.flatten().sortedBy { levenshtein(it.title, query) }

        setUserText("Found : ${eps[0].title}")

        val posterRegex = "^(.+?)(?:-season-\\d|-ep\\d+).*\$".toRegex()

        return eps.map { season ->
            val locale = if (!selectDub && season.languages.contains("ja-JP")) "ja-JP"
            else if (selectDub && season.languages.contains("en-US")) "en-US"
            else season.languages.last()

            val fixed = season.title
                .split(" ")
                .joinToString(separator = "-") { s -> s.trim { it in "\":/" }}
                .lowercase()

            val match = posterRegex.matchEntire(fixed)
            val coverUrl = if (match != null) {
                match.groupValues[1].lowercase()
            } else fixed

            ShowResponse(
                name = season.title,
                link = "$apiUrl/episodes/${season.seasonId}?lh=$locale&page=1",
                coverUrl = FileUrl(
                    coverUrl,
                    mapOf(
                        "user-agent" to
                                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36"
                    )
                ),
                extra = mapOf("season" to season.seasonNumber)
            )
        }
    }

    override suspend fun loadVideoServers(episodeLink: String, extra: Map<String, String>?): List<VideoServer> {
    val res = client.get(episodeLink).body.string()
        val json = Mapper.parse<EpisodeJSON>(res)
        return json.servers.map { serverLink ->
            VideoServer(
                name = "Sapphire Duck",
                embed = FileUrl(serverLink)
            )
        }    
    }

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor = SapphireDuck(server)

    override suspend fun autoSearch(mediaObj: Media): ShowResponse? {
        val response = loadSavedShowResponse(mediaObj.id)
        if (response != null) {
            saveShowResponse(mediaObj.id, response, true)
        } else {
            var seasonNumber = 1
            var currentMediaObj: Media? = mediaObj
            while (true) {
                if (currentMediaObj?.prequel == null && seasonNumber == 1 && mediaObj.typeMAL == "Movie") {
                    // The Movie we are searching for is a prequel (e.g JJK 0 -> JJK)
                    currentMediaObj = currentMediaObj?.sequel
                    break
                } else if (currentMediaObj?.prequel == null) {
                    break
                }
                seasonNumber += 1
                currentMediaObj = currentMediaObj.prequel
            }
            setUserText("Searching : ${mediaObj.mainName()}")
            // Search using the root
            if (currentMediaObj == null) {
                setUserText("Not found")
                return null
            }

            // Try and get the result with the closest number of seasons
            val eps = search(
                currentMediaObj.name ?: currentMediaObj.nameMAL ?: currentMediaObj.nameRomaji
            ).sortedBy { seasonNumber - abs(it.extra?.get("season")?.toIntOrNull() ?: 0) }

            println("Eps are $eps")
            println("Search for ${currentMediaObj.mainName()} was ${search(currentMediaObj.mainName())}")
            return eps.find {
                it.name == mediaObj.name ||
                        it.otherNames.contains(mediaObj.nameMAL) ||
                        it.otherNames.contains(mediaObj.nameRomaji)
            } ?: eps.getOrNull(0)
        }
        return response
    }

    @Serializable
    data class SeasonsJSON(
        val seasons: List<SeasonResponse>
    ) {
        @Serializable
        data class SeasonResponse(
            @SerialName("id") val seasonId: String,
            @SerialName("number") val seasonNumber: String,
            val title: String,
            val languages: List<String>
        )
    }

    @Serializable
    data class EpisodesJSON(
        @SerialName("limit") val pages: String,
        @SerialName("result") val episodes: List<EpisodeResponse>
    ) {
        @Serializable
        data class EpisodeResponse(
            val episodeNumber: String?, // Movies have this as null
            val slug: String,
            val thumbnail: Thumbnail
        )

        @Serializable
        data class Thumbnail(
            val sm: Image?,
            val hq: Image?
        ) {
            @Serializable
            data class Image(
                @SerialName("name") val slug: String,
                val formats: List<String>,
                val width: String,
                val height: String,
            )
        }
    }



    @Serializable
    data class EpisodeJSON(
        //        val audioLocale: String,
        //        val isDubbed: Boolean // These are also available
        //        val isSubbed: Boolean
        val servers: List<String>
    )
}