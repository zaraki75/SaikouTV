package ani.saikou.parsers

/**
 * Used to extract videos from a specific video host,
 * A new instance is created for every embeds/iframes of that Episode
 * **/
abstract class VideoExtractor {
    lateinit var name: String
    lateinit var url: String
    lateinit var videos: List<Video>
    lateinit var subtitles: List<Subtitle>

    /**
     * Loads "Video/s" from a given Url
     * & returns itself with the data loaded
     * **/
    abstract suspend fun extractFromUrl(url: String): VideoExtractor

    /**
     * Gets called when a Video from this extractor starts playing.
     * Useful for Extractor that require Polling
     * **/
    open suspend fun onVideoPlayed(video: Video) {}

    /**
     * Called when a particular video has been stopped playing
     **/
    open suspend fun onVideoStopped(video: Video) {}
}

data class Video(
    val name: String,
    val url: String,
    val isM3U8: Boolean,
    val size: Long? = null,
    val extraNote: String? = null,
)

data class Subtitle(
    val language: String,
    val url: String,
)