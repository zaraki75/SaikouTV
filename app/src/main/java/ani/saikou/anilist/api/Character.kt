package ani.saikou.anilist.api

import com.google.gson.annotations.SerializedName

data class Character(
    // The id of the character
    @SerializedName("id") var id: Int,

    // The names of the character
    @SerializedName("name") var name: CharacterName?,

    // Character images
    @SerializedName("image") var image: CharacterImage?,

    // A general description of the character
    @SerializedName("description") var description: String?,

    // The character's gender. Usually Male, Female, or Non-binary but can be any string.
    @SerializedName("gender") var gender: String?,

    // The character's birth date
    @SerializedName("dateOfBirth") var dateOfBirth: FuzzyDate?,

    // The character's age. Note this is a string, not an int, it may contain further text and additional ages.
    @SerializedName("age") var age: String?,

    // The characters blood type
    @SerializedName("bloodType") var bloodType: String?,

    // If the character is marked as favourite by the currently authenticated user
    @SerializedName("isFavourite") var isFavourite: Boolean?,

    // If the character is blocked from being added to favourites
    @SerializedName("isFavouriteBlocked") var isFavouriteBlocked: Boolean?,

    // The url for the character page on the AniList website
    @SerializedName("siteUrl") var siteUrl: String?,

    // Media that includes the character
    @SerializedName("media") var media: MediaConnection?,

    // The amount of user's who have favourited the character
    @SerializedName("favourites") var favourites: Int?,

    // Notes for site moderators
    @SerializedName("modNotes") var modNotes: String?,
)

data class CharacterConnection(
    @SerializedName("edges") var edges: List<CharacterEdge>?,

    @SerializedName("nodes") var nodes: List<Character>?,

    // The pagination information
    // @SerializedName("pageInfo") var pageInfo: PageInfo?,
)

data class CharacterEdge(
    @SerializedName("node") var node: Character?,

    // The id of the connection
    @SerializedName("id") var id: Int?,

    // The characters role in the media
    @SerializedName("role") var role: String?,

    // Media specific character name
    @SerializedName("name") var name: String?,

    // The voice actors of the character
    // @SerializedName("voiceActors") var voiceActors: List<Staff>?,

    // The voice actors of the character with role date
    // @SerializedName("voiceActorRoles") var voiceActorRoles: List<StaffRoleType>?,

    // The media the character is in
    @SerializedName("media") var media: List<Media>?,

    // The order the character should be displayed from the users favourites
    @SerializedName("favouriteOrder") var favouriteOrder: Int?,
)

data class CharacterName(
    // The character's given name
    @SerializedName("first") var first: String?,

    // The character's middle name
    @SerializedName("middle") var middle: String?,

    // The character's surname
    @SerializedName("last") var last: String?,

    // The character's first and last name
    @SerializedName("full") var full: String?,

    // The character's full name in their native language
    @SerializedName("native") var native: String?,

    // Other names the character might be referred to as
    @SerializedName("alternative") var alternative: List<String>?,

    // Other names the character might be referred to as but are spoilers
    @SerializedName("alternativeSpoiler") var alternativeSpoiler: List<String>?,

    // The currently authenticated users preferred name language. Default romaji for non-authenticated
    @SerializedName("userPreferred") var userPreferred: String?,
)

data class CharacterImage(
    // The character's image of media at its largest size
    @SerializedName("large") var large: String?,

    // The character's image of media at medium size
    @SerializedName("medium") var medium: String?,
)