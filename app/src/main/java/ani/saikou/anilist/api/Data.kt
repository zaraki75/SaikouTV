package ani.saikou.anilist.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Query(
    var data : Data?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Data(
    // Page
    var Page: Page?,

    // Media query
    var Media: Media?,

    // Airing schedule query
    var AiringSchedule: AiringSchedule?,

    // Character query
    var Character: Character?,

    // Staff query
    var Staff: Staff?,

    // Media list query
    var MediaList: MediaList?,

    // Media list collection query, provides list pre-grouped by status & custom lists. User ID and Media Type arguments required.
    var MediaListCollection: MediaListCollection?,

    // Collection of all the possible media genres
    var GenreCollection: List<String>?,

    // Collection of all the possible media tags
    var MediaTagCollection: List<MediaTag>?,

    // User query
    var User: User?,

    // Get the currently authenticated user
    var Viewer: User?,

    // Studio query
    var Studio: Studio?,

    // Follow query
    var Following: User?,

    // Follow query
    var Follower: User?,

    // Thread query
    var Thread: Thread?,

    // Recommendation query
    var Recommendation: Recommendation?,

    // Like query
    var Like: User?,

//    // Review query
//    var Review: Review?,
//
//    // Activity query
//    var Activity: ActivityUnion?,
//
//    // Activity reply query
//    var ActivityReply: ActivityReply?,

//    // Comment query
//    var ThreadComment: List<ThreadComment>?,

//    // Notification query
//    var Notification: NotificationUnion?,

//    // Media Trend query
//    var MediaTrend: MediaTrend?,

//    // Provide AniList markdown to be converted to html (Requires auth)
//    var Markdown: ParsedMarkdown?,

//    // SiteStatistics: SiteStatistics
//    var AniChartUser: AniChartUser?,

    // ExternalLinkSource collection query
    var ExternalLinkSourceCollection: List<MediaExternalLink>?,

)
