package ani.saikou.anilist.api

import com.google.gson.annotations.SerializedName

class Query{
    data class Viewer(
        @SerializedName("data")
        val data : Data?
    ){
        data class Data(
            @SerializedName("Viewer")
            val user: ani.saikou.anilist.api.User?
        )
    }
    data class Media(
        @SerializedName("data")
        val data :  Data?
    ){
        data class Data(
            @SerializedName("Media")
            val media: ani.saikou.anilist.api.Media?
        )
    }

    data class Page(
        @SerializedName("data")
        val data : Data?
    ){
        data class Data(
            @SerializedName("Page")
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
        @SerializedName("data")
        val data :  Data?
    ){
        data class Data(
            @SerializedName("Character")
            val character: ani.saikou.anilist.api.Character?
        )
    }

    data class Studio(
        @SerializedName("data")
        val data: Data?
    ){
        data class Data(
            @SerializedName("Studio")
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
        @SerializedName("data")
        val data : Data?
    ){
        data class Data(
            @SerializedName("MediaListCollection")
            val mediaListCollection: ani.saikou.anilist.api.MediaListCollection?
        )
    }

    data class GenreCollection(
        @SerializedName("data")
        val data: Data
    ){
        data class Data(
            @SerializedName("GenreCollection")
            val genreCollection: List<String>?
        )
    }

    data class MediaTagCollection(
        @SerializedName("data")
        val data: Data
    ){
        data class Data(
            @SerializedName("MediaTagCollection")
            val mediaTagCollection: List<MediaTag>?
        )
    }

    data class User(
        @SerializedName("data")
        val data: Data
    ){
        data class Data(
            @SerializedName("User")
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
