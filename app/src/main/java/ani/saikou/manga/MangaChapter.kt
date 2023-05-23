package ani.saikou.manga

import ani.saikou.parsers.MangaChapter
import ani.saikou.parsers.MangaImage
import java.io.Serializable
import kotlin.math.floor

data class MangaChapter(
    val number: String,
    var link: String,
    var title: String? = null,
    var description: String? = null,
) : Serializable {
    constructor(chapter: MangaChapter) : this(chapter.number, chapter.link, chapter.title, chapter.description)

    private val images = mutableListOf<MangaImage>()
    fun images(): List<MangaImage> = images
    fun addImages(image: List<MangaImage>) {
        if (images.isNotEmpty()) return
        image.forEach { images.add(it) }
        (0..floor((images.size.toFloat() - 1f) / 2).toInt()).forEach {
            val i = it * 2
            dualPages.add(images[i] to images.getOrNull(i + 1))
        }
    }

    private val dualPages = mutableListOf<Pair<MangaImage, MangaImage?>>()
    fun dualPages(): List<Pair<MangaImage, MangaImage?>> = dualPages

}
