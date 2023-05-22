package ani.saikou.parsers.anime.extractors

import android.util.Base64
import ani.saikou.*
import ani.saikou.parsers.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SapphireDuck(override val server: VideoServer) : VideoExtractor() {
    override suspend fun extract(): VideoContainer {
        val res = client.get(server.embed.url.replace("player.php", "config.php")).text
        val decoder = { s: String -> Base64.decode(s, Base64.NO_WRAP).decodeToString() }
        var parsed = decoder(res)
        var json: SapphireDuckJson? = null

        while (json == null) {
            if (parsed[0] != '{') {
                // It is not possible for it to be a JSON object.
                // Decode again.
                parsed = decoder(parsed)
            } else {
                json = try {
                    Mapper.parse(parsed)
                } catch(_: Exception) {null}
            }
        }

        val subs = json.subtitles.map { sub ->
            Subtitle(
                language = sub.language,
                url = sub.url,
                type = when(sub.format) {
                    "ass" -> SubtitleType.ASS
                    "srt" -> SubtitleType.SRT
                    else -> SubtitleType.VTT
                }
            )
        }

        val subtitleStr = { str: String ->
            when (str) {
                "ja-JP"  -> "[ja-JP] Japanese"
                "en-US"  -> "[en-US] English"
                "de-DE"  -> "[de-DE] German"
                "es-ES"  -> "[es-ES] Spanish"
                "es-419" -> "[es-419] Spanish"
                "fr-FR"  -> "[fr-FR] French"
                "it-IT"  -> "[it-IT] Italian"
                "pt-BR"  -> "[pt-BR] Portuguese (Brazil)"
                "pt-PT"  -> "[pt-PT] Portuguese (Portugal)"
                "ru-RU"  -> "[ru-RU] Russian"
                "zh-CN"  -> "[zh-CN] Chinese (Simplified)"
                "tr-TR"  -> "[tr-TR] Turkish"
                "ar-ME"  -> "[ar-ME] Arabic"
                "ar-SA"  -> "[ar-SA] Arabic (Saudi Arabia)"
                "uk-UK"  -> "[uk-UK] Ukrainian"
                "he-IL"  -> "[he-IL] Hebrew"
                "pl-PL"  -> "[pl-PL] Polish"
                "ro-RO"  -> "[ro-RO] Romanian"
                "sv-SE"  -> "[sv-SE] Swedish"
                else     -> if (str matches Regex("([a-z]{2})-([A-Z]{2}|\\d{3})")) "[${str}]" else str
            }
        }

        val videos = json.streams.mapNotNull { stream ->
            try {
                // It seems sometimes there are null entries.
                // We shouldn't give the user a prompt if there is not
                // an actual streamable link.
                if (stream.url.isBlank()) throw Exception("Blank URL")
                Video(
                    quality = null,
                    url = stream.url,
                    videoType = when (stream.format) {
                        "adaptive_hls" -> VideoType.M3U8
                        "adaptive_dash" -> VideoType.DASH
                        else -> VideoType.CONTAINER
                    },
                    size = getSize(stream.url),
                    extraNote = if (stream.audioLang == "ja-JP" && stream.audioLang != stream.hardsubLang) {
                        if (subs.isNotEmpty()) "Softsubbed ${subtitleStr(
                            stream.hardsubLang.ifEmpty { stream.audioLang }
                        )}"
                        else "Hardsubbed ${subtitleStr(stream.hardsubLang)}"
                    } else "Dubbed ${subtitleStr(stream.audioLang)}"
                )
            } catch (e: Exception) { return@mapNotNull null }
        }.sortedBy { it.extraNote }

        return VideoContainer(videos, subs)
    }

    @Serializable
    data class SapphireDuckJson(
        val subtitles: List<SapphireDuckSub>,
        val streams: List<SapphireDuckStream>,
    ) {
        @Serializable
        data class SapphireDuckSub(
            val language: String,
            val format: String,
            val url: String,
        )

        @Serializable
        data class SapphireDuckStream(
            val format: String,
            @SerialName("audio_lang") val audioLang: String,
            @SerialName("hardsub_lang") val hardsubLang: String,
            val url: String,
        )
    }
}