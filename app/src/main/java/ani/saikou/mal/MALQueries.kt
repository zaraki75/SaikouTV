package ani.saikou.mal

import ani.saikou.anilist.api.FuzzyDate
import ani.saikou.client
import ani.saikou.tryWithSuspend
import kotlinx.serialization.Serializable

class MALQueries {
    private val apiUrl = "https://api.myanimelist.net/v2"
    private val authHeader: Map<String, String>?
        get() {
            return mapOf("Authorization" to "Bearer ${MAL.token ?: return null}")
        }

    @Serializable
    data class MalUser(
        val id: Int,
        val name: String,
        val picture: String?,
    )

    suspend fun getUserData(): Boolean {
        val res = tryWithSuspend {
            client.get(
                "$apiUrl/users/@me",
                authHeader ?: return@tryWithSuspend null
            ).parsed<MalUser>()
        } ?: return false
        MAL.userid = res.id
        MAL.username = res.name
        MAL.avatar = res.picture

        return true
    }

    suspend fun editList(
        idMAL: Int?,
        isAnime: Boolean,
        progress: Int?,
        score: Int?,
        status: String,
        rewatch: Int? = null,
        start: FuzzyDate? = null,
        end: FuzzyDate? = null
    ) {
        if(idMAL==null) return
        val data = mutableMapOf("status" to convertStatus(isAnime, status))
        if (progress != null)
            data[if (isAnime) "num_watched_episodes" else "num_chapters_read"] = progress.toString()
            data[if (isAnime) "is_rewatching" else "is_rereading"] = (status == "REPEATING").toString()
        if (score != null)
            data["score"] = score.div(10).toString()
        if(rewatch!=null)
            data[if(isAnime) "num_times_rewatched" else "num_times_reread"] = rewatch.toString()
        if(start!=null)
            data["start_date"] = start.toISOString()
        if(end!=null)
            data["finish_date"] = end.toISOString()      
        tryWithSuspend {
            client.put(
                "$apiUrl/${if (isAnime) "anime" else "manga"}/$idMAL/my_list_status",
                authHeader ?: return@tryWithSuspend null,
                data = data,
            ).also { println(it) }
        }
    }

    suspend fun deleteList(isAnime: Boolean, idMAL: Int?){
        if(idMAL==null) return
        tryWithSuspend {
            client.delete(
                "$apiUrl/${if (isAnime) "anime" else "manga"}/$idMAL/my_list_status",
                authHeader ?: return@tryWithSuspend null
            ).also { println(it) }
        }
    }

    private fun convertStatus(isAnime: Boolean, status: String): String {
        return when (status) {
            "PLANNING" -> if (isAnime) "plan_to_watch" else "plan_to_read"
            "COMPLETED" -> "completed"
            "PAUSED" -> "on_hold"
            "DROPPED" -> "dropped"
            "CURRENT" -> if (isAnime) "watching" else "reading"
            else -> if (isAnime) "watching" else "reading"

        }
    }

}