package ani.saikou.parsers

import ani.saikou.Lazier
import ani.saikou.lazyList
import ani.saikou.parsers.anime.*

object AnimeSources : WatchSources() {
    override val list: List<Lazier<BaseParser>> = lazyList(
        "Gogo" to ::Gogo,
        "9Anime" to ::NineAnime,
        "AllAnime" to ::AllAnime,
        "Zoro" to ::Zoro,
        "Tenshi" to ::Tenshi,
        "Kamyroll" to ::Kamyroll,
        "9Anime Backup" to ::AnimeKisa,
        "Monoschinos" to ::Monoschinos,
        "Animefenix" to ::Animefenix,
    )
}

object HAnimeSources : WatchSources() {
    val aList: List<Lazier<BaseParser>>  = lazyList(
        "HentaiMama" to ::HentaiMama,
        "Haho" to ::Haho,
        "HentaiStream" to ::HentaiStream,
        "HentaiFF" to ::HentaiFF,
        "Monoschinos" to ::Monoschinos,
        "Animefenix" to ::Animefenix,
    )

    override val list = listOf(aList,AnimeSources.list).flatten()
}
