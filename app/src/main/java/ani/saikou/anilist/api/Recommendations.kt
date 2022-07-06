package ani.saikou.anilist.api

import com.google.gson.annotations.SerializedName

data class Recommendation(
    // The id of the recommendation
    @SerializedName("id") var id: Int,

    // Users rating of the recommendation
    @SerializedName("rating") var rating: Int?,

    // The rating of the recommendation by currently authenticated user
    // @SerializedName("userRating") var userRating: RecommendationRating?,

    // The media the recommendation is from
    @SerializedName("media") var media: Media?,

    // The recommended media
    @SerializedName("mediaRecommendation") var mediaRecommendation: Media?,

    // The user that first created the recommendation
    @SerializedName("user") var user: User?,
)

data class RecommendationConnection(
    //@SerializedName("edges") var edges: List<RecommendationEdge>?,

    @SerializedName("nodes") var nodes: List<Recommendation>?,

    // The pagination information
    //@SerializedName("pageInfo") var pageInfo: PageInfo?,

)