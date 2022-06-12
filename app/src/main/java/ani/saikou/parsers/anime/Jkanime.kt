package ani.saikou.parsers.anime

import android.net.Uri
import android.util.Base64
import android.util.Log
import ani.saikou.*
import ani.saikou.parsers.*
import ani.saikou.parsers.anime.extractors.FPlayer
import ani.saikou.parsers.anime.extractors.GogoCDN
import ani.saikou.parsers.anime.extractors.OK
import ani.saikou.parsers.anime.extractors.StreamSB
import com.fasterxml.jackson.module.kotlin.readValue

class Jkanime : AnimeParser() {
    override val name = "Jkanime"
    override val saveName = "jkanime"
    override val hostUrl = "https://jkanime.net"
    override val isDubAvailableSeparately = false
    override val language = "Spanish"

    override suspend fun loadEpisodes(animeLink: String, extra: Map<String, String>?): List<Episode> {
        val episodes = mutableListOf<Episode>()
        val pageBody = client.get(animeLink).document
        val animeId = pageBody.select("div.anime__details__text div.anime__details__title div#guardar-anime.btn.btn-light.btn-sm.ml-2")
            .attr("data-anime")
         pageBody.select("div.anime__pagination a").forEach {
            val pageNum = it.attr("href").replace("#pag","")
            val json = client.get("https://jkanime.net/ajax/pagination_episodes/$animeId/$pageNum").parsed<List<ResponseElement>>()
            json.forEach(){  result ->
                episodes.add(
                    if(result.image.isNotBlank()){
                        Episode(result.number,"$animeLink/${result.number}",thumbnail = "https://cdn.jkdesu.com/assets/images/animes/video/image_thumb/${result.image}")
                    }else{
                        Episode(result.number,"$animeLink/${result.number}")
                    }
                )
            }
        }
    return episodes
    }

    data class ResponseElement(
        val number: String,
        val title: String,
        val image: String
    )


    override suspend fun loadVideoServers(episodeLink: String, extra: Any?): List<VideoServer> {
        return client.get(episodeLink).document.select("div.col-lg-12.rounded.bg-servers.text-white.p-3.mt-2 a").map{ it ->
            val server = it.text()
            var url = ""
            val serverId = it.attr("data-id")
            client.get(episodeLink).document.select("script").forEach{script ->
                if(script.data().contains("var video = [];")){
                    url = script.data().substringAfter("video[$serverId] = '<iframe class=\"player_conte\" src=\"")
                        .substringBefore("\"")

                    if(url.contains(".php?u=") and !url.contains("php?u=stream")){
                        url = client.get(url).document.select("iframe").attr("src")
                    }
                }
            }
            VideoServer(server,url)

        }
    }



    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor? {
        val domain = Uri.parse(server.embed.url).host ?: return null
        val extractor: VideoExtractor? = when {
            "fembed" in domain  -> FPlayer(server)
            "ok" in domain      -> OK(server)
            "jkanime" in domain -> jkanimeExtractor(server)
            else                -> null
        }
        return extractor
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val encoded = encode(query + if(selectDub) " (Sub)" else "")
        return client.get("$hostUrl/buscar/$encoded/1/?filtro=nombre&tipo=none&estado=none&orden=desc").document.select("div.anime__page__content div.row div.col-lg-2").map {
            val link = it.select("div.anime__item a").attr("href")
            val title = it.select("div.anime__item div#ainfo div.title").text()
            val cover = it.select("div.anime__item a div.anime__item__pic.set-bg").attr("data-setbg")
            ShowResponse(title, link, cover)
        }
    }
}


class jkanimeExtractor(override val server: VideoServer): VideoExtractor() {
    override suspend fun extract(): VideoContainer {
        val videos = mutableListOf<Video>()
        val url = server.embed.url.replace("um2","um")
        if(url.contains("jk.php")){
            return VideoContainer(listOf(Video(null,false,url.replace("jk.php?u=",""))))
        }
        client.get(url).document.select("script").forEach{script ->
            if(script.data().contains("var parts = {")){
                val data = script.data().substringAfter("customType")
                    .findBetween("video: ","})")
                val json = mapper.readValue<List<VideoResponse>>(
                    "[${
                        data
                            ?.replace("url:", "\"url\":")
                            ?.replace("type:", "\"type\":")
                            ?.replace("'","\"")
                    }]"
                )
                json.forEach{
                    if(it.type.contains("hls")||it.type.contains("custom")){
                        videos.add( Video(null,true,it.url))
                    }else{
                        videos.add( Video(null,false,it.url))
                    }

                }
            }

        }
        return VideoContainer(
            videos
        )

    }
}

data class VideoResponse(
    val url: String,
    val type: String
)
