package ani.saikou.others

import ani.saikou.anime.Episode
import ani.saikou.client
import ani.saikou.tryWithSuspend
import com.fasterxml.jackson.annotation.JsonProperty

object AnimeFillerList {
    suspend fun getFillers(malId: Int): MutableMap<String, Episode>? {
        tryWithSuspend {
            val map = mutableMapOf<String, Episode>()
            val json = client.get("https://raw.githubusercontent.com/saikou-app/mal-id-filler-list/main/fillers/$malId.json")
            if (json.text != "404: Not Found") json.parsed<AnimeFillerListValue>().episodes?.forEach {
                val num = it.number.toString()
                map[num] = Episode(
                    num,
                    it.title,
                    filler = it.fillerBool == true
                )
            }
            return@tryWithSuspend map
        }
        return null
    }
    data class AnimeFillerListValue (
        @JsonProperty("MAL-id")
        val malID: Int? = null,

        @JsonProperty("Anilist-id")
        val anilistID: Int? = null,

        val episodes: List<AFLEpisode>? = null
    )

    data class AFLEpisode (
        val number: Int? = null,
        val title: String? = null,
        val desc: String? = null,
        val filler: String? = null,
        @JsonProperty("filler-bool") val fillerBool : Boolean?=null,
        val airDate: String? = null
    )
}


