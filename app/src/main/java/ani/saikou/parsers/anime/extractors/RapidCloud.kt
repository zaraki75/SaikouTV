package ani.saikou.parsers.anime.extractors

import android.net.Uri
import android.util.Base64
import ani.saikou.FileUrl
import ani.saikou.client
import ani.saikou.findBetween
import ani.saikou.okHttpClient
import ani.saikou.parsers.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.*

@Suppress("BlockingMethodInNonBlockingContext")
class RapidCloud(override val server: VideoServer) : VideoExtractor() {

    override suspend fun extract(): VideoContainer {
        val videos = mutableListOf<Video>()
        val subtitles = mutableListOf<Subtitle>()

        val embed = server.embed

        val soup = client.get(embed.url, embed.headers).text.replace("\n", "")

        val key = soup.findBetween("var recaptchaSiteKey = '", "',")
        val number = soup.findBetween("recaptchaNumber = '", "';")

        val sId = wss()

        if (key != null && number != null && sId.isNotEmpty()) {
            captcha(embed.url, key)?.apply {

                val jsonLink = "https://rapid-cloud.co/ajax/embed-6/getSources?id=${
                    embed.url.findBetween("/embed-6/", "?z=")!!
                }&_token=${this}&_number=$number&sId=$sId"

                val json = client.get(jsonLink).parsed<SourceResponse>()

                json.sources?.forEach {
                    videos.add(Video(0, true, FileUrl(it.file ?: return@forEach)))
                }
                json.sourcesBackup?.forEach {
                    videos.add(Video(0, true, FileUrl(it.file ?: return@forEach), extraNote = "Backup"))
                }
                json.tracks?.forEach {
                    if (it.kind == "captions" && it.label != null && it.file != null)
                        subtitles.add(Subtitle(it.label, it.file))
                }
            }
        }

        return VideoContainer(videos, subtitles)
    }

    companion object{
        private suspend fun captcha(url: String, key: String): String? {
            val uri = Uri.parse(url)
            val domain = (Base64.encodeToString(
                (uri.scheme + "://" + uri.host + ":443").encodeToByteArray(),
                Base64.NO_PADDING
            ) + ".").replace("\n", "")
            val vToken =
                client.get("https://www.google.com/recaptcha/api.js?render=$key", referer = (uri.scheme + "://" + uri.host))
                    .text.replace("\n", "")
                    .findBetween("/releases/", "/recaptcha") ?: return null
            val recapToken =
                client.get("https://www.google.com/recaptcha/api2/anchor?ar=1&hl=en&size=invisible&cb=kr60249sk&k=$key&co=$domain&v=$vToken")
                    .document.selectFirst("#recaptcha-token")?.attr("value") ?: return null
            return client.post(
                "https://www.google.com/recaptcha/api2/reload?k=$key",
                data = mutableMapOf("v" to vToken, "k" to key, "c" to recapToken, "co" to domain, "sa" to "", "reason" to "q")
            )
                .text.replace("\n", "").findBetween("rresp\",\"", "\",null")
        }

        private suspend fun wss(): String {
            var sId = client.get("https://api.enime.moe/tool/rapid-cloud/server-id").text
            if(sId.isEmpty()){
                val latch = CountDownLatch(1)
                val listener = object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        webSocket.send("40")
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        when {
                            text.startsWith("40") -> {
                                sId = text.findBetween("40{\"sid\":\"", "\"}")?:""
                                latch.countDown()
                            }
                            text == "2"           -> webSocket.send("3")
                        }
                    }
                }
                okHttpClient.newWebSocket(
                    Request.Builder().url("wss://ws1.rapid-cloud.co/socket.io/?EIO=4&transport=websocket").build(),
                    listener
                )
                latch.await(30, TimeUnit.SECONDS)
            }
            return sId
        }
    }




    @Serializable
    private data class SourceResponse(
        @SerialName("sources") val sources: List<Source>? = null,
        @SerialName("sourcesBackup") val sourcesBackup: List<Source>? = null,
        @SerialName("tracks") val tracks: List<Source>? = null
    ) {

        @Serializable
        data class Source(
            @SerialName("file") val file: String? = null,
            @SerialName("label") val label: String? = null,
            @SerialName("kind") val kind: String? = null
        )
    }

}
