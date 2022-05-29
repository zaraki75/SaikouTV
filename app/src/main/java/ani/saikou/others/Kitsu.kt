package ani.saikou.others

import ani.saikou.FileUrl
import ani.saikou.anime.Episode
import ani.saikou.client
import ani.saikou.logger
import ani.saikou.media.Media
import ani.saikou.tryWithSuspend
import com.fasterxml.jackson.annotation.JsonProperty

object Kitsu {
    private suspend fun getKitsuData(query: String): KitsuResponse? {
        val headers = mapOf(
            "Content-Type" to "application/json",
            "Accept" to "application/json",
            "Connection" to "keep-alive",
            "DNT" to "1",
            "Origin" to "https://kitsu.io"
        )
        val json = tryWithSuspend { client.post("https://kitsu.io/api/graphql", headers, data = mapOf("query" to query)) }
        return json?.parsed()
    }

    suspend fun getKitsuEpisodesDetails(media: Media): Map<String, Episode>? {
        val print = false
        logger("Kitsu : title=${media.mainName()}", print)
        val query =
            """
query {
  lookupMapping(externalId: ${media.id}, externalSite: ANILIST_ANIME) {
    __typename
    ... on Anime {
      id
      episodes(first: 2000) {
        nodes {
          number
          titles {
            canonical
          }
          description
          thumbnail {
            original {
              url
            }
          }
        }
      }
    }
  }
}"""


        val result = getKitsuData(query) ?: return null
        logger("Kitsu : result=$result", print)
        return (result.data?.lookupMapping?.episodes?.nodes?:return null).mapNotNull { ep ->
            val num = ep?.num?.toString()?:return@mapNotNull null
            num to Episode(
                number = num,
                title = ep.titles?.canonical,
                desc = ep.description?.en,
                thumb = FileUrl[ep.thumbnail?.original?.url],
            )
        }.toMap()
    }

    private data class KitsuResponse(
        val data: Data? = null
    ) {
        data class Data (
            val lookupMapping: LookupMapping? = null
        )

        data class LookupMapping (
            val id: String? = null,
            val episodes: Episodes? = null
        )

        data class Episodes (
            val nodes: List<Node?>? = null
        )

        data class Node (
            @JsonProperty("number")
            val num: Long? = null,
            val titles: Titles? = null,
            val description: Description? = null,
            val thumbnail: Thumbnail? = null
        )

        data class Description (
            val en: String? = null
        )

        data class Thumbnail (
            val original: Original? = null
        )

        data class Original (
            val url: String? = null
        )

        data class Titles (
            val canonical: String? = null
        )

    }

}