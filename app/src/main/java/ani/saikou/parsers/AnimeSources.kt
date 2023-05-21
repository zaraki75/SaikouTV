package ani.saikou.parsers

import ani.saikou.Lazier
import ani.saikou.lazyList
import ani.saikou.parsers.anime.*

object AnimeSources : WatchSources() {
    override val list: List<Lazier<BaseParser>> = lazyList(
        "AllAnime" to ::AllAnime,
        "Gogo" to ::Gogo,
        "Zoro" to ::Zoro,
        "Marin" to ::Marin,
        "AnimePahe" to ::AnimePahe,
        "Monoschinos" to ::Monoschinos,
        "Animefenix" to ::Animefenix,
        "Jkanime" to ::Jkanime,
        "AnimeLatinoHD" to ::Animelatinohd,
        "Animeflv" to ::TioAnime,
        "ConsumeBili" to ::ConsumeBili,
        "KickAssAnime" to ::KickAssAnime,
//        "9Anime" to ::NineAnime,        
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
        "Jkanime" to ::Jkanime,
    )

    override val list = listOf(aList,AnimeSources.list).flatten()
}
