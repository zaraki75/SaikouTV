package ani.saikou.anime.source.extractors

import ani.saikou.anime.Episode
import ani.saikou.anime.source.Extractor
import ani.saikou.logger
import org.jsoup.Jsoup
import ani.saikou.httpClient

class OK(): Extractor() {
    override suspend fun getStreamLinks(name: String, url: String): Episode.StreamLinks {
            val document = httpClient.get(url).document
            val tempQuality = mutableListOf<Episode.Quality>()
            val videosString = document.select("div[data-options]").attr("data-options")
                .substringAfter("\\\"videos\\\":[{\\\"name\\\":\\\"")
                .substringBefore("]")
            videosString.split("{\\\"name\\\":\\\"").reversed().forEach {
                val videoUrl = it.substringAfter("url\\\":\\\"")
                    .substringBefore("\\\"")
                    .replace("\\\\u0026", "&")
                val videoQuality = "Okru: " + it.substringBefore("\\\"")
                logger("$url")
                if (videoUrl.startsWith("https://")) {
                    tempQuality.add(Episode.Quality(videoUrl, videoQuality, null))
                }
            }
        return Episode.StreamLinks(
            name,
            tempQuality,
            null
        )

                }

}