package ani.saikou.parsers.anime

import android.util.Base64
import ani.saikou.*
import android.net.Uri
import android.util.Log
import ani.saikou.FileUrl
import ani.saikou.client
import ani.saikou.parsers.*
import ani.saikou.parsers.anime.extractors.FPlayer
import ani.saikou.parsers.anime.extractors.GogoCDN
import ani.saikou.parsers.anime.extractors.OK
import ani.saikou.parsers.anime.extractors.StreamSB




   /* override suspend fun getStream(episode: Episode, server: String): Episode {
        episode.streamLinks = let {
            val linkForVideos = mutableMapOf<String,Episode.StreamLinks?>()
            try{
                withContext(Dispatchers.Default) {
                    val document = Jsoup.connect(episode.link!!).ignoreHttpErrors(true).get()
                    document.select("ul.is-borderless.episode-page__servers-list li").forEach { it ->
                        launch {
                            val serverName = it.select("a").attr("title")
                            val serverId = it.select("a").attr("href").replace("#vid", "").toInt()
                            val serverCode =
                                document.select("div.player-container script").toString()
                                    .substringAfter("tabsArray['$serverId'] =")
                                    .substringBefore("&amp;thumbnail")
                                    .substringAfter("code=")
                                    .substringBefore("&amp")



                            val directLinks = directLinkify(serverName, serverCode)
                            if(serverName==server){
                                if(directLinks != null){linkForVideos[directLinks.server] = directLinks}
                        }

                    }}

                }}catch (e:Exception){
                toastString(e.toString())
            }
            linkForVideos
        }
        return episode
    }




    override suspend fun getStreams(episode: Episode): Episode {
//        try {
        episode.streamLinks = let {
            val linkForVideos = mutableMapOf<String,Episode.StreamLinks?>()
            try{
                withContext(Dispatchers.Default) {
                    val document = Jsoup.connect(episode.link!!).ignoreHttpErrors(true).get()
                    document.select("ul.is-borderless.episode-page__servers-list li").forEach { it ->
                        launch {
                            val serverName = it.select("a").attr("title")
                            val serverId = it.select("a").attr("href").replace("#vid", "").toInt()
                            val serverCode =
                                document.select("div.player-container script").toString()
                                    .substringAfter("tabsArray['$serverId'] =")
                                    .substringBefore("&amp;thumbnail")
                                    .substringAfter("code=")
                                    .substringBefore("&amp")
                            val thumb = document.select("div.player-container script").toString()
                                .substringAfter("&amp;thumbnail")
                                .substringBefore("")
                            val directLinks = directLinkify(serverName, serverCode)
                            if(directLinks != null){linkForVideos[directLinks.server] = directLinks}

                        }}

                }}catch (e:Exception){
                toastString(e.toString())
            }
            linkForVideos
        }
        return episode
    }



*/


class Animefenix : AnimeParser() {
    override val name = "Animefenix"
    override val saveName = "animefenix"
    override val hostUrl = "https://www.animefenix.com"
    override val isDubAvailableSeparately = false
    override val language = "Spanish"


       class amazonExtractor(override val server: VideoServer): VideoExtractor() {

           override suspend fun extract(): VideoContainer {
               val url = server.embed.url
               val videos = mutableListOf<Video>()
               val jsoup = client.get(url).document
               val videoUrl = jsoup.select("body script").toString()
                   .substringAfter("[{\"file\":\"")
                   .substringBefore("\",").replace("\\", "")
               videos.add(Video(720, false, videoUrl))

               return VideoContainer(videos)
           }
       }






    private suspend fun thumbResolver(url:String): String{
       val script =  client.get(url).toString()
        val thumbUrl = script
            .substringAfter("&amp;thumbnail=")
            .substringBefore("'")
            .replace("%2F","/")
            .replace("%3A",":")
        Log.i("bruh","ThubURL: $thumbUrl")

        return thumbUrl

    }




    override suspend fun loadEpisodes(
        animeLink: String,
        extra: Map<String, String>?
    ): List<Episode> {
        val list = mutableListOf<ani.saikou.parsers.Episode>()

        val pageBody = client.get(animeLink).document
        val thumbUrl = thumbResolver(
            pageBody.select("ul.anime-page__episode-list.is-size-6 li a").first()?.attr("href").toString())
        pageBody.select("ul.anime-page__episode-list.is-size-6 li").reversed().forEach { it ->
            val epNum = it.select("a span").text().replace("Episodio", "")
            val url = it.select("a").attr("href")

            list.add(ani.saikou.parsers.Episode(number = epNum,link = url,thumbnail = thumbUrl))
        }
        return list
    }



    private fun httpsIfy(text: String): String {
        return if (text.take(2) == "//") "https:$text"
        else text
    }

    override suspend fun loadVideoServers(episodeLink: String, extra: Any?): List<VideoServer> {
        val list = mutableListOf<VideoServer>()
       val document = client.get(episodeLink).document
        document.select("ul.is-borderless.episode-page__servers-list li").forEach {
            val serverName = it.select("a").attr("title")
            val serverId = it.select("a").attr("href").replace("#vid", "").toInt()
            val serverCode =
                document.select("div.player-container script").toString()
                    .substringAfter("tabsArray['$serverId'] =")
                    .substringBefore("&amp;thumbnail")
                    .substringAfter("code=")
                    .substringBefore("&amp")

            var url = ""

            if(serverName == "fembed" || serverName == "Fembed" ) url = "https://www.fembed.com/v/$serverCode"
            if(serverName == "ru" || serverName == "RU" ) url = "https://ok.ru/videoembed/$serverCode"
            if(serverName == "Amazon" || serverName == "AMAZON" || serverName == "amazon" ) url = "https://www.animefenix.com/stream/amz.php?v=$serverCode"
            if(serverName == "AmazonEs" || serverName == "AMAZONES" || serverName == "amazones" ) url = "https://www.animefenix.com/stream/amz.php?v=$serverCode&ext=es"

            val embed = FileUrl(url,mapOf("referer" to hostUrl))
            list.add(VideoServer(serverName,embed))

        }
        return list
    }

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor? {
        val domain = Uri.parse(server.embed.url).host ?: return null
        val extractor: VideoExtractor? = when {
            "fembed" in domain -> FPlayer(server)
            "ok" in domain      -> OK(server)
            "animefenix" in domain -> amazonExtractor(server)

            else                -> null
        }
        return extractor
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val encoded = encode(query + if(selectDub) " (Sub)" else "")
        val list = mutableListOf<ShowResponse>()
        client.get("$hostUrl/animes?q=$encoded").document
            .select("div.container div.container div.list-series article.serie-card").forEach {
                val link = it.select("figure.image a").attr("href")
                val title = it.select("div.title h3 a").text()
                val cover = it.select("figure.image a img").attr("src")
                list.add(ShowResponse(title, link, cover))
            }
        return list
    }
}