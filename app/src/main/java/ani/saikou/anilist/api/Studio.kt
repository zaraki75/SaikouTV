package ani.saikou.anilist.api

import com.google.gson.annotations.SerializedName

data class Studio(
    // The id of the studio
    @SerializedName("id") var id: Int,

    // The name of the studio
    // Originally non-nullable, needs to be nullable due to it not being always queried
    @SerializedName("name") var name: String?,

    // If the studio is an animation studio or a different kind of company
    @SerializedName("isAnimationStudio") var isAnimationStudio: Boolean?,

    // The media the studio has worked on
    @SerializedName("media") var media: MediaConnection?,

    // The url for the studio page on the AniList website
    @SerializedName("siteUrl") var siteUrl: String?,

    // If the studio is marked as favourite by the currently authenticated user
    @SerializedName("isFavourite") var isFavourite: Boolean?,

    // The amount of user's who have favourited the studio
    @SerializedName("favourites") var favourites: Int?,
)

data class StudioConnection(
    //@SerializedName("edges") var edges: List<StudioEdge>?,

    @SerializedName("nodes") var nodes: List<Studio>?,

    // The pagination information
    //@SerializedName("pageInfo") var pageInfo: PageInfo?,
)