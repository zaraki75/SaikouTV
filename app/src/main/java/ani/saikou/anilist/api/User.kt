package ani.saikou.anilist.api

import com.google.gson.annotations.SerializedName


data class User(
    // The id of the user
    @SerializedName("id") var id: Int,

    // The name of the user
    @SerializedName("name") var name: String?,

    // The bio written by user (Markdown)
    //    @SerializedName("about") var about: String?,

    // The user's avatar images
    @SerializedName("avatar") var avatar: UserAvatar?,

    // The user's banner images
    @SerializedName("bannerImage") var bannerImage: String?,

    // If the authenticated user if following this user
    //    @SerializedName("isFollowing") var isFollowing: Boolean?,

    // If this user if following the authenticated user
    //    @SerializedName("isFollower") var isFollower: Boolean?,

    // If the user is blocked by the authenticated user
    //    @SerializedName("isBlocked") var isBlocked: Boolean?,

    // FIXME: No documentation is provided for "Json"
    // @SerializedName("bans") var bans: Json?,

    // The user's general options
    @SerializedName("options") var options: UserOptions?,

    // The user's media list options
    @SerializedName("mediaListOptions") var mediaListOptions: MediaListOptions?,

    // The users favourites
    @SerializedName("favourites") var favourites: Favourites?,

    // The users anime & manga list statistics
    @SerializedName("statistics") var statistics: UserStatisticTypes?,

    // The number of unread notifications the user has
    //    @SerializedName("unreadNotificationCount") var unreadNotificationCount: Int?,

    // The url for the user page on the AniList website
    //    @SerializedName("siteUrl") var siteUrl: String?,

    // The donation tier of the user
    //    @SerializedName("donatorTier") var donatorTier: Int?,

    // Custom donation badge text
    //    @SerializedName("donatorBadge") var donatorBadge: String?,

    // The user's moderator roles if they are a site moderator
    // @SerializedName("moderatorRoles") var moderatorRoles: List<ModRole>?,

    // When the user's account was created. (Does not exist for accounts created before 2020)
    //    @SerializedName("createdAt") var createdAt: Int?,

    // When the user's data was last updated
    //    @SerializedName("updatedAt") var updatedAt: Int?,

    // The user's previously used names.
    // @SerializedName("previousNames") var previousNames: List<UserPreviousName>?,

)

data class UserOptions(
    // The language the user wants to see media titles in
    // @SerializedName("titleLanguage") var titleLanguage: UserTitleLanguage?,

    // Whether the user has enabled viewing of 18+ content
    @SerializedName("displayAdultContent") var displayAdultContent: Boolean?,

    // Whether the user receives notifications when a show they are watching aires
    //    @SerializedName("airingNotifications") var airingNotifications: Boolean?,
    //
    //    // Profile highlight color (blue, purple, pink, orange, red, green, gray)
    //    @SerializedName("profileColor") var profileColor: String?,
    //
    //    // Notification options
    //    // @SerializedName("notificationOptions") var notificationOptions: List<NotificationOption>?,
    //
    //    // The user's timezone offset (Auth user only)
    //    @SerializedName("timezone") var timezone: String?,
    //
    //    // Minutes between activity for them to be merged together. 0 is Never, Above 2 weeks (20160 mins) is Always.
    //    @SerializedName("activityMergeTime") var activityMergeTime: Int?,
    //
    //    // The language the user wants to see staff and character names in
    //    // @SerializedName("staffNameLanguage") var staffNameLanguage: UserStaffNameLanguage?,
    //
    //    // Whether the user only allow messages from users they follow
    //    @SerializedName("restrictMessagesToFollowing") var restrictMessagesToFollowing: Boolean?,

    // The list activity types the user has disabled from being created from list updates
    // @SerializedName("disabledListActivity") var disabledListActivity: List<ListActivityOption>?,
)

data class UserAvatar(
    // The avatar of user at its largest size
    @SerializedName("large") var large: String?,

    // The avatar of user at medium size
    @SerializedName("medium") var medium: String?,
)

data class UserStatisticTypes(
    @SerializedName("anime") var anime: UserStatistics?,
    @SerializedName("manga") var manga: UserStatistics?
)

data class UserStatistics(
    //
    @SerializedName("count") var count: Int?,
    @SerializedName("meanScore") var meanScore: Float?,
    @SerializedName("standardDeviation") var standardDeviation: Float?,
    @SerializedName("minutesWatched") var minutesWatched: Int?,
    @SerializedName("episodesWatched") var episodesWatched: Int?,
    @SerializedName("chaptersRead") var chaptersRead: Int?,
    @SerializedName("volumesRead") var volumesRead: Int?,
    //    @SerializedName("formats") var formats: List<UserFormatStatistic>?,
    //    @SerializedName("statuses") var statuses: List<UserStatusStatistic>?,
    //    @SerializedName("scores") var scores: List<UserScoreStatistic>?,
    //    @SerializedName("lengths") var lengths: List<UserLengthStatistic>?,
    //    @SerializedName("releaseYears") var releaseYears: List<UserReleaseYearStatistic>?,
    //    @SerializedName("startYears") var startYears: List<UserStartYearStatistic>?,
    //    @SerializedName("genres") var genres: List<UserGenreStatistic>?,
    //    @SerializedName("tags") var tags: List<UserTagStatistic>?,
    //    @SerializedName("countries") var countries: List<UserCountryStatistic>?,
    //    @SerializedName("voiceActors") var voiceActors: List<UserVoiceActorStatistic>?,
    //    @SerializedName("staff") var staff: List<UserStaffStatistic>?,
    //    @SerializedName("studios") var studios: List<UserStudioStatistic>?,
)

data class Favourites(
    // Favourite anime
    @SerializedName("anime") var anime: MediaConnection?,

    // Favourite manga
    @SerializedName("manga") var manga: MediaConnection?,

    // Favourite characters
    @SerializedName("characters") var characters: CharacterConnection?,

    // Favourite staff
    @SerializedName("staff") var staff: StaffConnection?,

    // Favourite studios
    @SerializedName("studios") var studios: StudioConnection?,
)

data class MediaListOptions(
    // The score format the user is using for media lists
    // @SerializedName("scoreFormat") var scoreFormat: ScoreFormat?,

    // The default order list rows should be displayed in
    @SerializedName("rowOrder") var rowOrder: String?,

    // The user's anime list options
    @SerializedName("animeList") var animeList: MediaListTypeOptions?,

    // The user's manga list options
    @SerializedName("mangaList") var mangaList: MediaListTypeOptions?,
)

data class MediaListTypeOptions(
    // The order each list should be displayed in
    @SerializedName("sectionOrder") var sectionOrder: List<String>?,

    //    // If the completed sections of the list should be separated by format
    //    @SerializedName("splitCompletedSectionByFormat") var splitCompletedSectionByFormat: Boolean?,
    //
    //    // The names of the user's custom lists
    //    @SerializedName("customLists") var customLists: List<String>?,
    //
    //    // The names of the user's advanced scoring sections
    //    @SerializedName("advancedScoring") var advancedScoring: List<String>?,
    //
    //    // If advanced scoring is enabled
    //    @SerializedName("advancedScoringEnabled") var advancedScoringEnabled: Boolean?,
)

