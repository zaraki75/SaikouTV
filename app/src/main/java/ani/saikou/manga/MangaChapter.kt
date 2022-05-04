package ani.saikou.manga

import ani.saikou.parsers.MangaChapter
import ani.saikou.parsers.MangaImage
import java.io.Serializable

data class MangaChapter(
    val number: String,
    var link: String,
    var title: String? = null,
    var description : String?= null,
    var images: List<MangaImage>? = null
) : Serializable {
    constructor(chapter: MangaChapter) :this(chapter.number,chapter.link,chapter.title,chapter.description)
}
