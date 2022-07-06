package ani.saikou.others

import ani.saikou.FileUrl
import ani.saikou.anime.Episode
import ani.saikou.client
import ani.saikou.logger
import ani.saikou.media.Media
import ani.saikou.tryWithSuspend
import com.google.gson.annotations.SerializedName

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
        @SerializedName("data") val data: Data? = null
    ) {
        data class Data (
            @SerializedName("lookupMapping") val lookupMapping: LookupMapping? = null
        )

        data class LookupMapping (
            @SerializedName("id") val id: String? = null,
            @SerializedName("episodes") val episodes: Episodes? = null
        )

        data class Episodes (
            @SerializedName("nodes") val nodes: List<Node?>? = null
        )

        data class Node (
            @SerializedName("number") val num: Long? = null,
            @SerializedName("titles") val titles: Titles? = null,
            @SerializedName("description") val description: Description? = null,
            @SerializedName("thumbnail") val thumbnail: Thumbnail? = null
        )

        data class Description (
            @SerializedName("en") val en: String? = null
        )

        data class Thumbnail (
            @SerializedName("original") val original: Original? = null
        )

        data class Original (
            @SerializedName("url") val url: String? = null
        )

        data class Titles (
            @SerializedName("canonical") val canonical: String? = null
        )

    }

}