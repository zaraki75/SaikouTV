package ani.saikou.anilist.api

import com.google.gson.annotations.SerializedName


data class Page(
    // The pagination information
    @SerializedName("pageInfo") var pageInfo: PageInfo?,

    @SerializedName("users") var users: List<User>?,

    @SerializedName("media") var media: List<Media>?,

    @SerializedName("characters") var characters: List<Character>?,

    @SerializedName("staff") var staff: List<Staff>?,

    @SerializedName("studios") var studios: List<Studio>?,

    @SerializedName("mediaList") var mediaList: List<MediaList>?,

    @SerializedName("airingSchedules") var airingSchedules: List<AiringSchedule>?,

    // @SerializedName("mediaTrends") var mediaTrends: List<MediaTrend>?,

    // @SerializedName("notifications") var notifications: List<NotificationUnion>?,

    @SerializedName("followers") var followers: List<User>?,

    @SerializedName("following") var following: List<User>?,

    // @SerializedName("activities") var activities: List<ActivityUnion>?,

    // @SerializedName("activityReplies") var activityReplies: List<ActivityReply>?,

    @SerializedName("threads") var threads: List<Thread>?,

    // @SerializedName("threadComments") var threadComments: List<ThreadComment>?,

    // @SerializedName("reviews") var reviews: List<Review>?,

    @SerializedName("recommendations") var recommendations: List<Recommendation>?,

    @SerializedName("likes") var likes: List<User>?,
)

data class PageInfo(
    // The total number of items. Note: This value is not guaranteed to be accurate, do not rely on this for logic
    @SerializedName("total") var total: Int?,

    // The count on a page
    @SerializedName("perPage") var perPage: Int?,

    // The current page
    @SerializedName("currentPage") var currentPage: Int?,

    // The last page
    @SerializedName("lastPage") var lastPage: Int?,

    // If there is another page
    @SerializedName("hasNextPage") var hasNextPage: Boolean?,
)