package ani.saikou.others

import ani.saikou.client
import ani.saikou.media.Media
import ani.saikou.tryWithSuspend

object Mal {
    suspend fun loadMedia(media: Media): Media {
        tryWithSuspend {
            if (media.anime != null) {
                val res = client.get("https://myanimelist.net/anime/${media.idMAL}").document
                val a = res.select(".title-english").text()
                media.nameMAL = if (a != "") a else res.select(".title-name").text()
                media.typeMAL =
                    if (res.select("div.spaceit_pad > a").isNotEmpty()) res.select("div.spaceit_pad > a")[0].text() else null
                media.anime.op = arrayListOf()
                res.select(".opnening > table > tbody > tr").forEach {
                    val text = it.text()
                    if (!text.contains("Help improve our database"))
                        media.anime.op.add(it.text())
                }
                media.anime.ed = arrayListOf()
                res.select(".ending > table > tbody > tr").forEach {
                    val text = it.text()
                    if (!text.contains("Help improve our database"))
                        media.anime.ed.add(it.text())
                }

            } else {
                val res = client.get("https://myanimelist.net/manga/${media.idMAL}").document
                val b = res.select(".title-english").text()
                val a = res.select(".h1-title").text().removeSuffix(b)
                media.nameMAL = a
                media.typeMAL =
                    if (res.select("div.spaceit_pad > a").isNotEmpty()) res.select("div.spaceit_pad > a")[0].text() else null
            }
        }
        return media
    }
}