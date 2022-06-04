package ani.saikou.anilist.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

class Query{
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Viewer(
        val data : Data?
    ){
        data class Data(
            @JsonProperty("Viewer")
            val user: ani.saikou.anilist.api.User?
        )
    }
    data class Media(
        val data :  Data?
    ){
        data class Data(
            @JsonProperty("Media")
            val media: ani.saikou.anilist.api.Media?
        )
    }

    data class Page(
        val data : Data?
    ){
        data class Data(
            @JsonProperty("Page")
            val page : ani.saikou.anilist.api.Page?
        )
    }
//    data class AiringSchedule(
//        val data : Data?
//    ){
//        data class Data(
//            val AiringSchedule: ani.saikou.anilist.api.AiringSchedule?
//        )
//    }

    data class Character(
        val data :  Data?
    ){
        data class Data(
            @JsonProperty("Character")
            val character: ani.saikou.anilist.api.Character?
        )
    }

    data class Studio(
        val data: Data?
    ){
        data class Data(
            @JsonProperty("Studio")
            val studio: ani.saikou.anilist.api.Studio?
        )
    }

//    data class MediaList(
//        val data: Data?
//    ){
//        data class Data(
//            val MediaList: ani.saikou.anilist.api.MediaList?
//        )
//    }

    data class MediaListCollection(
        val data : Data?
    ){
        data class Data(
            @JsonProperty("MediaListCollection")
            val mediaListCollection: ani.saikou.anilist.api.MediaListCollection?
        )
    }

    data class GenreCollection(
        val data: Data
    ){
        data class Data(
            @JsonProperty("GenreCollection")
            val genreCollection: List<String>?
        )
    }

    data class MediaTagCollection(
        val data: Data
    ){
        data class Data(
            @JsonProperty("MediaTagCollection")
            val mediaTagCollection: List<MediaTag>?
        )
    }

    data class User(
        val data: Data
    ){
        data class Data(
            @JsonProperty("User")
            val user: ani.saikou.anilist.api.User?
        )
    }
}

//data class WhaData(
//    val Studio: Studio?,
//
//    // Follow query
//    val Following: User?,
//
//    // Follow query
//    val Follower: User?,
//
//    // Thread query
//    val Thread: Thread?,
//
//    // Recommendation query
//    val Recommendation: Recommendation?,
//
//    // Like query
//    val Like: User?,

//    // Review query
//    val Review: Review?,
//
//    // Activity query
//    val Activity: ActivityUnion?,
//
//    // Activity reply query
//    val ActivityReply: ActivityReply?,

//    // Comment query
//    val ThreadComment: List<ThreadComment>?,

//    // Notification query
//    val Notification: NotificationUnion?,

//    // Media Trend query
//    val MediaTrend: MediaTrend?,

//    // Provide AniList markdown to be converted to html (Requires auth)
//    val Markdown: ParsedMarkdown?,

//    // SiteStatistics: SiteStatistics
//    val AniChartUser: AniChartUser?,
//)
