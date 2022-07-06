package ani.saikou.anilist.api

import com.google.gson.annotations.SerializedName

data class Staff(
    // The id of the staff member
    @SerializedName("id") var id: Int,

    // The names of the staff member
    // @SerializedName("name") var name: StaffName?,

    // The primary language of the staff member. Current values: Japanese, English, Korean, Italian, Spanish, Portuguese, French, German, Hebrew, Hungarian, Chinese, Arabic, Filipino, Catalan, Finnish, Turkish, Dutch, Swedish, Thai, Tagalog, Malaysian, Indonesian, Vietnamese, Nepali, Hindi, Urdu
    @SerializedName("languageV2") var languageV2: String?,

    // The staff images
    // @SerializedName("image") var image: StaffImage?,

    // A general description of the staff member
    @SerializedName("description") var description: String?,

    // The person's primary occupations
    @SerializedName("primaryOccupations") var primaryOccupations: List<String>?,

    // The staff's gender. Usually Male, Female, or Non-binary but can be any string.
    @SerializedName("gender") var gender: String?,

    @SerializedName("dateOfBirth") var dateOfBirth: FuzzyDate?,

    @SerializedName("dateOfDeath") var dateOfDeath: FuzzyDate?,

    // The person's age in years
    @SerializedName("age") var age: Int?,

    // [startYear, endYear] (If the 2nd value is not present staff is still active)
    @SerializedName("yearsActive") var yearsActive: List<Int>?,

    // The persons birthplace or hometown
    @SerializedName("homeTown") var homeTown: String?,

    // The persons blood type
    @SerializedName("bloodType") var bloodType: String?,

    // If the staff member is marked as favourite by the currently authenticated user
    @SerializedName("isFavourite") var isFavourite: Boolean?,

    // If the staff member is blocked from being added to favourites
    @SerializedName("isFavouriteBlocked") var isFavouriteBlocked: Boolean?,

    // The url for the staff page on the AniList website
    @SerializedName("siteUrl") var siteUrl: String?,

    // Media where the staff member has a production role
    @SerializedName("staffMedia") var staffMedia: MediaConnection?,

    // Characters voiced by the actor
    @SerializedName("characters") var characters: CharacterConnection?,

    // Media the actor voiced characters in. (Same data as characters with media as node instead of characters)
    @SerializedName("characterMedia") var characterMedia: MediaConnection?,

    // Staff member that the submission is referencing
    @SerializedName("staff") var staff: Staff?,

    // Submitter for the submission
    @SerializedName("submitter") var submitter: User?,

    // Status of the submission
    @SerializedName("submissionStatus") var submissionStatus: Int?,

    // Inner details of submission status
    @SerializedName("submissionNotes") var submissionNotes: String?,

    // The amount of user's who have favourited the staff member
    @SerializedName("favourites") var favourites: Int?,

    // Notes for site moderators
    @SerializedName("modNotes") var modNotes: String?,
)

data class StaffConnection(
    // @SerializedName("edges") var edges: List<StaffEdge>?,

    @SerializedName("nodes") var nodes: List<Staff>?,

    // The pagination information
    // @SerializedName("pageInfo") var pageInfo: PageInfo?,
)