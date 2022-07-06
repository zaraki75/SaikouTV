package ani.saikou.others

import ani.saikou.anime.Episode
import ani.saikou.client
import ani.saikou.tryWithSuspend
import com.google.gson.annotations.SerializedName

object AnimeFillerList {
    suspend fun getFillers(malId: Int): Map<String, Episode>? {
        return tryWithSuspend {
            val json = client.get("https://raw.githubusercontent.com/saikou-app/mal-id-filler-list/main/fillers/$malId.json")
            return@tryWithSuspend if (json.text != "404: Not Found") json.parsed<AnimeFillerListValue>().episodes?.associate {
                val num = it.number.toString()
                num to Episode(
                    num,
                    it.title,
                    filler = it.fillerBool == true
                )
            } else null
        }
    }

    data class AnimeFillerListValue (
        @SerializedName("MAL-id")
        val malID: Int? = null,

        @SerializedName("Anilist-id")
        val anilistID: Int? = null,

        @SerializedName("episodes")
        val episodes: List<AFLEpisode>? = null
    )

    data class AFLEpisode (
        @SerializedName("number") val number: Int? = null,
        @SerializedName("title") val title: String? = null,
        @SerializedName("desc") val desc: String? = null,
        @SerializedName("filler") val filler: String? = null,
        @SerializedName("filler-bool") val fillerBool : Boolean?=null,
        @SerializedName("airDate") val airDate: String? = null
    )
}


