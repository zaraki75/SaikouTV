package ani.saikou.parsers.manga

import ani.saikou.asyncMap
import ani.saikou.client
import ani.saikou.parsers.MangaChapter
import ani.saikou.parsers.MangaImage
import ani.saikou.parsers.MangaParser
import ani.saikou.parsers.ShowResponse
import com.google.gson.annotations.SerializedName

class MangaDex : MangaParser() {

    override val name = "MangaDex"
    override val saveName = "manga_dex"
    override val hostUrl = "https://mangadex.org"

    val host = "https://api.mangadex.org"

    override suspend fun loadChapters(mangaLink: String, extra: Map<String, String>?): List<MangaChapter> {
        val old = showUserText
        setUserText("Getting Chapters...")
        val list = mutableListOf<MangaChapter>()

        val totalChapters = client.get("$host/manga/$mangaLink/feed?limit=0")
            .parsed<MangaResponse>().total ?: return list

        setUserText("Parsing Chapters...")
        (0..totalChapters step 200).reversed().toList().asyncMap { index ->
            val data = client
                .get("$host/manga/$mangaLink/feed?limit=200&order[volume]=desc&order[chapter]=desc&offset=$index")
                .parsed<MangaResponse>().data?.reversed()

            data?.forEach {
                if (it.attributes.translatedLanguage == "en" && it.attributes.externalUrl == null) {
                    val chapter = (it.attributes.chapter ?: return@forEach).toString()
                    val title = it.attributes.title
                    list.add(MangaChapter(chapter, it.id, title))
                }
            }
        }
        list.sortBy { it.number.toFloatOrNull() }
        setUserText(old)
        return list
    }

    override suspend fun loadImages(chapterLink: String): List<MangaImage> {
        val res = client.get("$host/at-home/server/${chapterLink}").parsed<ChapterResponse>().chapter
        return res?.data?.map {
            MangaImage("https://uploads.mangadex.org/data/${res.hash}/${it}")
        } ?: listOf()
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val json = client.get("$host/manga?limit=15&title=$query&order[followedCount]=desc&includes[]=cover_art").parsed<SearchResponse>()
        return json.data?.mapNotNull {
            val id = it.id ?: return@mapNotNull null
            val title = it.attributes?.title?.en ?: return@mapNotNull null
            val coverName = it.relationships?.find { i -> i.type == "cover_art" }?.attributes?.fileName
            val coverURL = if (coverName != null) "https://uploads.mangadex.org/covers/$id/$coverName.256.jpg" else ""
            ShowResponse(title, id, coverURL)
        } ?: listOf()
    }

    private data class SearchResponse(
        @SerializedName("result") val result: String? = null,
        @SerializedName("data") val data: List<Datum>? = null,
        @SerializedName("total") val total: Long? = null
    ) {
        data class Datum(
            @SerializedName("id") val id: String? = null,
            @SerializedName("attributes") val attributes: DatumAttributes? = null,
            @SerializedName("relationships") val relationships: List<Relationship>? = null
        )

        data class DatumAttributes(
            @SerializedName("title") val title: Title? = null
        )

        data class Title(
            @SerializedName("en") val en: String? = null
        )

        data class Relationship(
            @SerializedName("id") val id: String? = null,
            @SerializedName("type") val type: String? = null,
            @SerializedName("attributes") val attributes: RelationshipAttributes? = null
        )

        data class RelationshipAttributes(
            @SerializedName("fileName") val fileName: String? = null
        )
    }

    private data class MangaResponse(
        @SerializedName("result") val result: String? = null,
        @SerializedName("data") val data: List<Datum>? = null,
        @SerializedName("total") val total: Long? = null
    ) {
        data class Datum(
            @SerializedName("id") val id: String,
            @SerializedName("attributes") val attributes: Attributes
        )

        data class Attributes(
            @SerializedName("volume") val volume: Any? = null,
            @SerializedName("chapter") val chapter: Any? = null,
            @SerializedName("title") val title: String? = null,
            @SerializedName("translatedLanguage") val translatedLanguage: String? = null,
            @SerializedName("externalUrl") val externalUrl : String? = null
        )
    }

    private data class ChapterResponse(
        @SerializedName("result") val result: String? = null,
        @SerializedName("baseURL") val baseURL: String? = null,
        @SerializedName("chapter") val chapter: Chapter? = null
    ) {
        data class Chapter(
            @SerializedName("hash") val hash: String? = null,
            @SerializedName("data") val data: List<String>? = null,
            @SerializedName("dataSaver") val dataSaver: List<String>? = null
        )
    }
}
