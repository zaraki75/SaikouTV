package ani.saikou.manga

import java.io.Serializable

data class MangaChapter(
    val number: String,
    var title : String?=null,
    var link : String? = null,
    var headers : MutableMap<String,String>?=null,
    var images : ArrayList<String>?=null
):Serializable
