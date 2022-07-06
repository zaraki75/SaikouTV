@file:Suppress("unused")

package ani.saikou.anilist.api

import com.google.gson.annotations.SerializedName

data class Media(
    // The id of the media
    @SerializedName("id") var id: Int,

    // The mal id of the media
    @SerializedName("idMal") var idMal: Int?,

    // The official titles of the media in various languages
    @SerializedName("title") var title: MediaTitle?,

    // The type of the media; anime or manga
    @SerializedName("type") var type: MediaType?,

    // The format the media was released in
    @SerializedName("format") var format: MediaFormat?,

    // The current releasing status of the media
    @SerializedName("status") var status: MediaStatus?,

    // Short description of the media's story and characters
    @SerializedName("description") var description: String?,

    // The first official release date of the media
    @SerializedName("startDate") var startDate: FuzzyDate?,

    // The last official release date of the media
    @SerializedName("endDate") var endDate: FuzzyDate?,

    // The season the media was initially released in
    @SerializedName("season") var season: MediaSeason?,

    // The season year the media was initially released in
    @SerializedName("seasonYear") var seasonYear: Int?,

    // The year & season the media was initially released in
    @SerializedName("seasonInt") var seasonInt: Int?,

    // The amount of episodes the anime has when complete
    @SerializedName("episodes") var episodes: Int?,

    // The general length of each anime episode in minutes
    @SerializedName("duration") var duration: Int?,

    // The amount of chapters the manga has when complete
    @SerializedName("chapters") var chapters: Int?,

    // The amount of volumes the manga has when complete
    @SerializedName("volumes") var volumes: Int?,

    // Where the media was created. (ISO 3166-1 alpha-2)
    // Originally a "CountryCode"
    @SerializedName("countryOfOrigin") var countryOfOrigin: String?,

    // If the media is officially licensed or a self-published doujin release
    @SerializedName("isLicensed") var isLicensed: Boolean?,

    // Source type the media was adapted from.
    @SerializedName("source") var source: MediaSource?,

    // Official Twitter hashtags for the media
    @SerializedName("hashtag") var hashtag: String?,

    // Media trailer or advertisement
    @SerializedName("trailer") var trailer: MediaTrailer?,

    // When the media's data was last updated
    @SerializedName("updatedAt") var updatedAt: Int?,

    // The cover images of the media
    @SerializedName("coverImage") var coverImage: MediaCoverImage?,

    // The banner image of the media
    @SerializedName("bannerImage") var bannerImage: String?,

    // The genres of the media
    @SerializedName("genres") var genres: List<String>?,

    // Alternative titles of the media
    @SerializedName("synonyms") var synonyms: List<String>?,

    // A weighted average score of all the user's scores of the media
    @SerializedName("averageScore") var averageScore: Int?,

    // Mean score of all the user's scores of the media
    @SerializedName("meanScore") var meanScore: Int?,

    // The number of users with the media on their list
    @SerializedName("popularity") var popularity: Int?,

    // Locked media may not be added to lists our favorited. This may be due to the entry pending for deletion or other reasons.
    @SerializedName("isLocked") var isLocked: Boolean?,

    // The amount of related activity in the past hour
    @SerializedName("trending") var trending: Int?,

    // The amount of user's who have favourited the media
    @SerializedName("favourites") var favourites: Int?,

    // List of tags that describes elements and themes of the media
    @SerializedName("tags") var tags: List<MediaTag>?,

    // Other media in the same or connecting franchise
    @SerializedName("relations") var relations: MediaConnection?,

    // The characters in the media
    @SerializedName("characters") var characters: CharacterConnection?,

    // The staff who produced the media
    // @SerializedName("staff") var staff: StaffConnection?,

    // The companies who produced the media
    @SerializedName("studios") var studios: StudioConnection?,

    // If the media is marked as favourite by the current authenticated user
    @SerializedName("isFavourite") var isFavourite: Boolean?,

    // If the media is blocked from being added to favourites
    @SerializedName("isFavouriteBlocked") var isFavouriteBlocked: Boolean?,

    // If the media is intended only for 18+ adult audiences
    @SerializedName("isAdult") var isAdult: Boolean?,

    // The media's next episode airing schedule
    @SerializedName("nextAiringEpisode") var nextAiringEpisode: AiringSchedule?,

    // The media's entire airing schedule
    // @SerializedName("airingSchedule") var airingSchedule: AiringScheduleConnection?,

    // The media's daily trend stats
    // @SerializedName("trends") var trends: MediaTrendConnection?,

    // External links to another site related to the media
    @SerializedName("externalLinks") var externalLinks: List<MediaExternalLink>?,

    // Data and links to legal streaming episodes on external sites
    // @SerializedName("streamingEpisodes") var streamingEpisodes: List<MediaStreamingEpisode>?,

    // The ranking of the media in a particular time span and format compared to other media
    // @SerializedName("rankings") var rankings: List<MediaRank>?,

    // The authenticated user's media list entry for the media
    @SerializedName("mediaListEntry") var mediaListEntry: MediaList?,

    // User reviews of the media
    // @SerializedName("reviews") var reviews: ReviewConnection?,

    // User recommendations for similar media
    @SerializedName("recommendations") var recommendations: RecommendationConnection?,

    //
    // @SerializedName("stats") var stats: MediaStats?,

    // The url for the media page on the AniList website
    @SerializedName("siteUrl") var siteUrl: String?,

    // If the media should have forum thread automatically created for it on airing episode release
    @SerializedName("autoCreateForumThread") var autoCreateForumThread: Boolean?,

    // If the media is blocked from being recommended to/from
    @SerializedName("isRecommendationBlocked") var isRecommendationBlocked: Boolean?,

    // If the media is blocked from being reviewed
    @SerializedName("isReviewBlocked") var isReviewBlocked: Boolean?,

    // Notes for site moderators
    @SerializedName("modNotes") var modNotes: String?,
)


data class MediaTitle(
    // The romanization of the native language title
    @SerializedName("romaji") var romaji: String,

    // The official english title
    @SerializedName("english") var english: String?,

    // Official title in it's native language
    @SerializedName("native") var native: String?,

    // The currently authenticated users preferred title language. Default romaji for non-authenticated
    @SerializedName("userPreferred") var userPreferred: String,
)

enum class MediaType {
    ANIME, MANGA;

    override fun toString(): String {
        return super.toString().replace("_", " ")
    }
}

enum class MediaStatus {
    FINISHED, RELEASING, NOT_YET_RELEASED, CANCELLED, HIATUS;

    override fun toString(): String {
        return super.toString().replace("_", " ")
    }
}

data class AiringSchedule(
    // The id of the airing schedule item
    @SerializedName("id") var id: Int?,

    // The time the episode airs at
    @SerializedName("airingAt") var airingAt: Int?,

    // Seconds until episode starts airing
    @SerializedName("timeUntilAiring") var timeUntilAiring: Int?,

    // The airing episode number
    @SerializedName("episode") var episode: Int?,

    // The associate media id of the airing episode
    @SerializedName("mediaId") var mediaId: Int?,

    // The associate media of the airing episode
    @SerializedName("media") var media: Media?,
)

data class MediaCoverImage(
    // The cover image url of the media at its largest size. If this size isn't available, large will be provided instead.
    @SerializedName("extraLarge") var extraLarge: String?,

    // The cover image url of the media at a large size
    @SerializedName("large") var large: String?,

    // The cover image url of the media at medium size
    @SerializedName("medium") var medium: String?,

    // Average #hex color of cover image
    @SerializedName("color") var color: String?,
)

data class MediaList(
    // The id of the list entry
    @SerializedName("id") var id: Int?,

    // The id of the user owner of the list entry
    @SerializedName("userId") var userId: Int?,

    // The id of the media
    @SerializedName("mediaId") var mediaId: Int?,

    // The watching/reading status
    @SerializedName("status") var status: MediaListStatus?,

    // The score of the entry
    @SerializedName("score") var score: Float?,

    // The amount of episodes/chapters consumed by the user
    @SerializedName("progress") var progress: Int?,

    // The amount of volumes read by the user
    @SerializedName("progressVolumes") var progressVolumes: Int?,

    // The amount of times the user has rewatched/read the media
    @SerializedName("repeat") var repeat: Int?,

    // Priority of planning
    @SerializedName("priority") var priority: Int?,

    // If the entry should only be visible to authenticated user
    @SerializedName("private") var private: Boolean?,

    // Text notes
    @SerializedName("notes") var notes: String?,

    // If the entry shown be hidden from non-custom lists
    @SerializedName("hiddenFromStatusLists") var hiddenFromStatusLists: Boolean?,

    // Map of booleans for which custom lists the entry are in
    // @SerializedName("customLists") var customLists: Json?,

    // Map of advanced scores with name keys
    // @SerializedName("advancedScores") var advancedScores: Json?,

    // When the entry was started by the user
    @SerializedName("startedAt") var startedAt: FuzzyDate?,

    // When the entry was completed by the user
    @SerializedName("completedAt") var completedAt: FuzzyDate?,

    // When the entry data was last updated
    @SerializedName("updatedAt") var updatedAt: Int?,

    // When the entry data was created
    @SerializedName("createdAt") var createdAt: Int?,

    @SerializedName("media") var media: Media?,

    @SerializedName("user") var user: User?
)

enum class MediaListStatus {
    CURRENT, PLANNING, COMPLETED, DROPPED, PAUSED, REPEATING;

    override fun toString(): String {
        return super.toString().replace("_", " ")
    }
}

enum class MediaSource {
    ORIGINAL, MANGA, LIGHT_NOVEL, VISUAL_NOVEL, VIDEO_GAME, OTHER, NOVEL, DOUJINSHI, ANIME, WEB_NOVEL, LIVE_ACTION, GAME, COMIC, MULTIMEDIA_PROJECT, PICTURE_BOOK;

    override fun toString(): String {
        return super.toString().replace("_", " ")
    }
}

enum class MediaFormat {
    TV, TV_SHORT, MOVIE, SPECIAL, OVA, ONA, MUSIC, MANGA, NOVEL, ONE_SHOT;

    override fun toString(): String {
        return super.toString().replace("_", " ")
    }
}

data class MediaTrailer(
    // The trailer video id
    @SerializedName("id") var id: String?,

    // The site the video is hosted by (Currently either youtube or dailymotion)
    @SerializedName("site") var site: String?,

    // The url for the thumbnail image of the video
    @SerializedName("thumbnail") var thumbnail: String?,
)

data class MediaTagCollection(
    @SerializedName("tags") var tags : List<MediaTag>?
)

data class MediaTag(
    // The id of the tag
    @SerializedName("id") var id: Int?,

    // The name of the tag
    @SerializedName("name") var name: String,

    // A general description of the tag
    @SerializedName("description") var description: String?,

    // The categories of tags this tag belongs to
    @SerializedName("category") var category: String?,

    // The relevance ranking of the tag out of the 100 for this media
    @SerializedName("rank") var rank: Int?,

    // If the tag could be a spoiler for any media
    @SerializedName("isGeneralSpoiler") var isGeneralSpoiler: Boolean?,

    // If the tag is a spoiler for this media
    @SerializedName("isMediaSpoiler") var isMediaSpoiler: Boolean?,

    // If the tag is only for adult 18+ media
    @SerializedName("isAdult") var isAdult: Boolean?,

    // The user who submitted the tag
    @SerializedName("userId") var userId: Int?,
)


data class MediaConnection(
    @SerializedName("edges") var edges: List<MediaEdge>?,

    @SerializedName("nodes") var nodes: List<Media>?,

    // The pagination information
    @SerializedName("pageInfo") var pageInfo: PageInfo?,
)

data class MediaEdge(
    //
    @SerializedName("node") var node: Media?,

    // The id of the connection
    @SerializedName("id") var id: Int?,

    // The type of relation to the parent model
    @SerializedName("relationType") var relationType: MediaRelation?,

    // If the studio is the main animation studio of the media (For Studio->MediaConnection field only)
    @SerializedName("isMainStudio") var isMainStudio: Boolean?,

    // The characters in the media voiced by the parent actor
    @SerializedName("characters") var characters: List<Character>?,

    // The characters role in the media
    @SerializedName("characterRole") var characterRole: String?,

    // Media specific character name
    @SerializedName("characterName") var characterName: String?,

    // Notes regarding the VA's role for the character
    @SerializedName("roleNotes") var roleNotes: String?,

    // Used for grouping roles where multiple dubs exist for the same language. Either dubbing company name or language variant.
    @SerializedName("dubGroup") var dubGroup: String?,

    // The role of the staff member in the production of the media
    @SerializedName("staffRole") var staffRole: String?,

    // The voice actors of the character
    // @SerializedName("voiceActors") var voiceActors: List<Staff>?,

    // The voice actors of the character with role date
    // @SerializedName("voiceActorRoles") var voiceActorRoles: List<StaffRoleType>?,

    // The order the media should be displayed from the users favourites
    @SerializedName("favouriteOrder") var favouriteOrder: Int?,
)

enum class MediaRelation {
    ADAPTATION, PREQUEL, SEQUEL, PARENT, SIDE_STORY, CHARACTER, SUMMARY, ALTERNATIVE, SPIN_OFF, OTHER, SOURCE, COMPILATION, CONTAINS;

    override fun toString(): String {
        return super.toString().replace("_", " ")
    }
}

enum class MediaSeason {
    WINTER, SPRING, SUMMER, FALL;
}

data class MediaExternalLink(
    // The id of the external link
    @SerializedName("id") var id: Int?,

    // The url of the external link or base url of link source
    @SerializedName("url") var url: String?,

    // The links website site name
    @SerializedName("site") var site: String,

    // The links website site id
    @SerializedName("siteId") var siteId: Int?,

    @SerializedName("type") var type: ExternalLinkType?,

    // Language the site content is in. See Staff language field for values.
    @SerializedName("language") var language: String?,

    @SerializedName("color") var color: String?,

    // The icon image url of the site. Not available for all links. Transparent PNG 64x64
    @SerializedName("icon") var icon: String?,

    // isDisabled: Boolean
    @SerializedName("notes") var notes: String?,
)

enum class ExternalLinkType {
    INFO, STREAMING, SOCIAL;

    override fun toString(): String {
        return super.toString().replace("_", " ")
    }
}

data class MediaListCollection(
    // Grouped media list entries
    @SerializedName("lists") var lists: List<MediaListGroup>?,

    // The owner of the list
    @SerializedName("user") var user: User?,

    // If there is another chunk
    @SerializedName("hasNextChunk") var hasNextChunk: Boolean?,

    )

data class MediaListGroup(
    // Media list entries
    @SerializedName("entries") var entries: List<MediaList>?,

    @SerializedName("name") var name: String?,

    @SerializedName("isCustomList") var isCustomList: Boolean?,

    @SerializedName("isSplitCompletedList") var isSplitCompletedList: Boolean?,

    @SerializedName("status") var status: MediaListStatus?,
)