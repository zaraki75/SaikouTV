package ani.saikou.parsers.anime

import android.net.Uri
import android.util.Log
import ani.saikou.*
import ani.saikou.parsers.*
import ani.saikou.parsers.anime.extractors.FPlayer
import ani.saikou.parsers.anime.extractors.OK
import com.fasterxml.jackson.module.kotlin.readValue

class Jkanime : AnimeParser() {
    override val name = "Jkanime"
    override val saveName = "jkanime"
    override val hostUrl = "https://jkanime.net"
    override val isDubAvailableSeparately = false
    override val language = "Spanish"

    override suspend fun loadEpisodes(animeLink: String, extra: Map<String, String>?): List<Episode> {
        val pageBody = client.get(animeLink).document
        val animeId =
            pageBody.select("div.anime__details__text div.anime__details__title div#guardar-anime.btn.btn-light.btn-sm.ml-2")
                .attr("data-anime")
        return episodesParser(animeLink,animeId)
        }

    private suspend fun episodesParser(animeLink: String, animeId:String):List<Episode>{
        val pageBody = client.get(animeLink).document
        val episodes = mutableListOf<Episode>()
        val lastPage = pageBody.select("div.anime__pagination a").last()?.attr("href")
            ?.replace("#pag","")
        val firstPage = pageBody.select("div.anime__pagination a").first()?.attr("href")
            ?.replace("#pag","")


        if(firstPage != lastPage) {
                var checkLast = 0;
                for (i in 1 until lastPage?.toInt()!!) {
                    for (j in 1..12){
                        Log.i("bruh",(j + checkLast).toString())
                        episodes.add(
                            Episode(
                                (j + checkLast).toString(),
                                "$animeLink/${j + checkLast}"
                            )
                        )

                    }
                    checkLast += 12
                }
            client.get("https://jkanime.net/ajax/pagination_episodes/$animeId/$lastPage").parsed<List<ResponseElement>>().forEach{
                episodes.add(Episode(it.number,"$animeLink/${it.number}"))
            }
        }
        if(firstPage == lastPage){
            client.get("https://jkanime.net/ajax/pagination_episodes/$animeId/$lastPage").parsed<List<ResponseElement>>().forEach{
                Log.i("bruh",it.number)
                episodes.add(Episode(it.number,"$animeLink/${it.number}"))
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
                    url = hostUrl + script.data().substringAfter("video[$serverId] = '<iframe class=\"player_conte\" src=\"")
                        .substringBefore("\"")


                }
            }
            if(!url.contains("jk.php") && url.contains(".php?u")){
                url = client.get(hostUrl+url).document.select("iframe").attr("src")
            }
            Log.i("bruh",url)
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
