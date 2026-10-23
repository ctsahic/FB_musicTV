package com.radiobox.tv.core

/**
 * Curated list of Israeli stations whose stream URLs were verified live
 * (HTTP 200 + an audio MIME type) on 26/09/2026. Genre tags are OURS — decided by this
 * app, not by the radio-browser.info community, so stations like גלגלצ or
 * רדיו צחוק land in the right category even when the directory mislabels
 * or omits them. Seed entries are injected before API results and sort first.
 */
object IsraeliSeed {

    data class SeedStation(
        val slug: String,
        val name: String,
        val stream: String,
        val tags: List<String>,
        val hls: Boolean = false,
        /**
         * Station logo (verified HTTP 200 image on 27/09/2026 — official favicons,
         * Wikipedia/Commons PNG renders, or station sites). Null = fall back to
         * the initials box. SVG sources are fetched via the PNG render URLs below,
         * so no SVG decoder is needed.
         */
        val logo: String? = null,
    )

    private const val WC = "https://upload.wikimedia.org/wikipedia"

    private const val TW = "https://playerservices.streamtheworld.com/api/livestream-redirect"

    val ALL = listOf(
        // --- KAN (תאגיד השידור) — official streamtheworld endpoints ---
        SeedStation(
            "kan-bet", "כאן רשת ב'", "$TW/KAN_BET.mp3", listOf("news", "talk", "israeli", "hebrew"),
            logo = "$WC/commons/thumb/5/50/KanBet.svg/500px-KanBet.svg.png",
        ),
        SeedStation(
            "kan-gimmel", "כאן רשת ג'", "$TW/KAN_GIMMEL.mp3", listOf("israeli", "hebrew", "mizrahi"),
            logo = "$WC/commons/thumb/3/39/KanGimel.svg/500px-KanGimel.svg.png",
        ),
        SeedStation(
            "kan-88", "כאן 88FM", "$TW/KAN_88.mp3", listOf("rock", "israeli", "alternative"),
            logo = "$WC/commons/thumb/0/0b/Kan88.svg/500px-Kan88.svg.png",
        ),
        SeedStation("kan-kol-hamusica", "כאן קול המוסיקה", "$TW/KAN_KOL_HAMUSICA.mp3", listOf("classical", "opera", "symphony")),
        SeedStation(
            "kan-moreshet", "כאן מורשת", "$TW/KAN_MORESHET.mp3", listOf("news", "talk", "israeli", "culture"),
            logo = "$WC/commons/thumb/e/e7/KanMoreshet.svg/500px-KanMoreshet.svg.png",
        ),
        SeedStation(
            "kan-reka", "כאן רק\"ע (ערבית)", "$TW/KAN_REKA.mp3", listOf("world", "ethnic", "arabic"),
            logo = "$WC/commons/thumb/e/ee/KanReka.svg/500px-KanReka.svg.png",
        ),
        SeedStation(
            "kan-makan", "כאן רדיו מכאן", "$TW/RADIO_MAKAN.mp3", listOf("world", "ethnic", "arabic"),
            logo = "$WC/commons/thumb/a/a9/Kan_makan.svg/500px-Kan_makan.svg.png",
        ),

        // --- גלי צהל (bynetcdn, official) ---
        SeedStation(
            "glglz", "גלגלצ", "http://glzwizzlv.bynetcdn.com/glglz_mp3", listOf("pop", "top 40", "israeli", "hebrew"),
            logo = "$WC/commons/thumb/e/e2/GLGLZ.svg/500px-GLGLZ.svg.png",
        ),
        SeedStation(
            "glz", "גלצ", "http://glzwizzlv.bynetcdn.com/glz_mp3", listOf("news", "talk", "israeli", "hebrew"),
            logo = "$WC/commons/thumb/3/30/GaltzLogo.svg/500px-GaltzLogo.svg.png",
        ),

        // --- רדיו אזורי מסחרי (cybercdn) ---
        SeedStation(
            "galei-israel", "גלי ישראל", "https://cdn.cybercdn.live/Galei_Israel/Live/icecast.audio", listOf("news", "talk", "israeli"),
            logo = "$WC/he/1/12/GaleyIsraelLOGO.jpg",
        ),
        SeedStation(
            "lev-hamedina-91", "לב המדינה 91FM", "https://cdn.cybercdn.live/Lev_Hamedina/Audio/icecast.audio", listOf("israeli", "mizrahi", "hebrew"),
            logo = "https://www.xoox.co.il/service/radio/img/radio_m14.jpg",
        ),
        SeedStation(
            "kol-rega-96", "קול רגע 96FM", "https://cdn.cybercdn.live/Kol_Rega/Live_Audio/icecast.audio", listOf("israeli", "pop", "hebrew"),
            logo = "https://96fm.co.il/wp-content/uploads/2022/10/cropped-Logo-192x192.png",
        ),
        SeedStation("radio-90", "רדיו 90", "https://cdn.cybercdn.live/Emtza_Haderech/Live_Audio/icecast.audio", listOf("israeli", "hebrew")),
        SeedStation(
            "103fm", "103FM", "https://cdn.cybercdn.live/103FM/Live/icecast.audio", listOf("news", "talk", "israeli"),
            logo = "$WC/he/thumb/7/72/103FM.svg/500px-103FM.svg.png",
        ),
        SeedStation(
            "100fm", "100FM רדיו ללא הפסקה", "https://cdn.cybercdn.live/Radios_100FM/Audio/playlist.m3u8", listOf("rock", "alternative", "israeli"), hls = true,
            logo = "https://digital.100fm.co.il/logo192.png",
        ),
        SeedStation("tzafon-1045", "104.5FM צפון ללא הפסקה", "https://cdn.cybercdn.live/Tzafon_NonStop/Live_Audio/icecast.audio", listOf("news", "talk")),
        SeedStation("radio-2000", "רדיו 2000", "https://cdn.cybercdn.live/Radio2000/MP3/icecast.audio", listOf("israeli", "mizrahi", "nostalgia")),
        SeedStation(
            "kol-barama", "קול ברמה", "https://cdn.cybercdn.live/Kol_Barama/Live_Audio/icecast.audio", listOf("mizrahi", "israeli"),
            logo = "$WC/he/thumb/4/48/%D7%9C%D7%95%D7%92%D7%95_%D7%A7%D7%95%D7%9C_%D7%91%D7%A8%D7%9E%D7%94.png/500px-%D7%9C%D7%95%D7%92%D7%95_%D7%A7%D7%95%D7%9C_%D7%91%D7%A8%D7%9E%D7%94.png",
        ),
        SeedStation(
            "hatahana-1015", "התחנה 101.5FM", "https://cdn.cybercdn.live/Hatahana_1015/Live_Audio/icecast.audio", listOf("mizrahi", "israeli"),
            logo = "https://hatahana.co.il/favicon.ico",
        ),
        SeedStation(
            "darom-97", "רדיו דרום 97FM", "https://cdn.cybercdn.live/Darom_97FM/Live/icecast.audio", listOf("israeli", "hebrew"),
            logo = "https://odsv.org/wp-content/uploads/2020/03/%D7%A8%D7%93%D7%99%D7%95-%D7%93%D7%A8%D7%95%D7%9D-1.png",
        ),
        SeedStation("darom-1015", "רדיו דרום 101.5FM", "https://cdn.cybercdn.live/Darom_1015FM/Live/icecast.audio", listOf("israeli", "hebrew")),
        SeedStation("jerusalem-101", "101FM ירושלים", "https://cdn.cybercdn.live/JerusalemRadio/Live/icecast.audio", listOf("israeli", "news", "hebrew"), logo = "https://www.101fm.co.il/favicon.ico"),

        // --- ecast ---
        SeedStation("lolfm", "רדיו צחוק FM (LOLfm)", "https://live.ecast.co.il/stream/69fm/lolfm", listOf("humor", "comedy", "israeli", "hebrew")),
        SeedStation(
            "radio-sahar", "רדיו סהר", "https://live.ecast.co.il/stream/sahar", listOf("israeli", "hebrew", "community"),
            logo = "https://radiosahar.co.il/images/logo.png",
        ),
        SeedStation("dudifm", "דודי FM", "https://live.ecast.co.il/stream/dudifm", listOf("mizrahi", "israeli")),
        SeedStation("hamizrahit", "המזרחית FM", "https://live.ecast.co.il/stream/mizrahit", listOf("mizrahi", "israeli")),
        SeedStation("kol-rishonim", "קול ראשונים", "https://live.ecast.co.il/stream/1062", listOf("mizrahi", "israeli")),
        SeedStation("radio-idan", "רדיו אידן", "https://live.ecast.co.il/stream/radioneto1", listOf("mizrahi", "israeli")),
        SeedStation("69fm", "69FM", "https://live.ecast.co.il/stream/69fm", listOf("israeli", "hebrew")),
        SeedStation("kol-hatzafon", "קול הצפון", "https://live.ecast.co.il/stream/khz", listOf("israeli", "hebrew")),
        SeedStation("kol-galim", "קול גלים", "https://live.ecast.co.il/stream/galimlive", listOf("israeli", "hebrew")),
        SeedStation("krayotfm", "קריות FM", "https://live.ecast.co.il/stream/krayotfm", listOf("israeli", "hebrew")),
        SeedStation("harokdim", "הרוקדים", "https://live.ecast.co.il:8022/stream", listOf("electronic", "dance", "party")),
        SeedStation("hevratifm", "רדיו חברתי FM", "https://live.ecast.co.il/stream/hevrati", listOf("news", "talk", "israeli")),

        // --- streamgates / misc ---
        SeedStation(
            "n12", "חדשות 13 (רדיו)", "http://ff_engine.streamgates.net/Ch10News.mp3", listOf("news", "talk"),
            logo = "$WC/he/thumb/2/24/News13_new2022.svg/500px-News13_new2022.svg.png",
        ),
        SeedStation("nostalgia-963", "נוסטלגיה 96.3FM", "https://cdna.streamgates.net/radios-audio/Nostalgia_963fm/playlist.m3u8", listOf("israeli", "nostalgia", "mizrahi"), hls = true),
        SeedStation("the-best", "The Best רדיו", "https://cdna.streamgates.net/TheBest/live/playlist.m3u8", listOf("mizrahi", "israeli"), hls = true),

        // --- עצמאיות / קהילתיות ---
        SeedStation("kzradio", "KZ רדיו קול הקמפוס", "https://kzradio.mediacast.co.il/kzradio_live/kzradio/icecast.audio", listOf("rock", "alternative", "indie")),
        SeedStation(
            "zerock", "זה רוק", "https://icecast.live/zerock", listOf("rock", "hebrew"),
            logo = "https://zerockradio.com/wp-content/uploads/2024/03/logo-flat.png",
        ),
        SeedStation(
            "up2dance", "Up2Dance", "https://icecast.live/up2dance", listOf("electronic", "dance"),
            logo = "https://up2dance.com/wp-content/uploads/2021/01/Up2Dance-Radio-01-01-768x742.png",
        ),
        SeedStation("hitsil", "Hits IL", "https://icecast.live/hitsil", listOf("pop", "hits", "israeli")),
        SeedStation(
            "eol", "רדיו EOL ירושלים", "https://eol-live.cdnwiz.com/eol/eolsite/icecast.audio", listOf("israeli", "news", "talk"),
            logo = "https://eol.co.il/wp-content/uploads/2023/02/15144351/logo_eol_1200.png",
        ),
        SeedStation("kol-ness-ziona", "קול נס ציונה", "http://kolness.livecast.co.il/liveness", listOf("israeli", "hebrew")),
        SeedStation("radio106-ariel", "רדיו אריאל 106FM", "https://ariel.icecast.live/hls/1/stream.m3u8", listOf("israeli", "hebrew"), hls = true),
        SeedStation("kol-netanya", "רדיו קול נתניה", "https://icecast.live/hls/kol_netanya/rkn_hls.m3u8", listOf("israeli", "hebrew"), hls = true),
        SeedStation("neurim-fm", "נעים FM", "https://icecast.live/hls/neurimfm/live.m3u8", listOf("israeli", "hebrew"), hls = true),
        SeedStation("pervoe", "פרבוה רדיו (רוסית)", "https://cdn.cybercdn.live/Pervoia/Audio/playlist.m3u8", listOf("russian", "world", "ethnic"), hls = true),
    )

    private val bySlug = ALL.associateBy { it.slug }

    /**
     * Alternate spellings under which the same station appears in the
     * radio-browser directory. Used to drop the API duplicate when the seed
     * entry is already in the list (e.g. "Kan Bet " vs "כאן רשת ב'").
     */
    val ALIASES: Map<String, Set<String>> = mapOf(
        "kan-bet" to setOf("kanbet"),
        "kan-gimmel" to setOf("kangimel", "reshetgimmel", "רשתג"),
        "kan-88" to setOf("kan88", "88fm"),
        "kan-kol-hamusica" to setOf("kankolhamusica", "kolhamusica"),
        "kan-moreshet" to setOf("kanmoreshet", "moreshet"),
        "kan-reka" to setOf("kanreka", "reka"),
        "kan-makan" to setOf("kanmakan", "radiomakan"),
        "glglz" to setOf("glglz", "גלגלץ", "גלגלצ", "galgalatz", "galgalatz918fm", "91.8fm"),
        "glz" to setOf("glz", "גלצ", "galeitzahal"),
        "kan-bet" to setOf("kanbet", "כאןב", "כאןרשתב"),
        "kan-gimmel" to setOf("kangimel", "reshetgimmel", "רשתג", "כאןג", "כאןרשתג"),
        "kan-88" to setOf("kan88", "88fm", "כאן88"),
        "103fm" to setOf("103fm", "103fmרדיוללאהפסקה"),
        "radio-sahar" to setOf("radiosahar", "רדיוסהר"),
        "galei-israel" to setOf("galeiyisrael", "94fmרדיוגליישראל"),
        "lev-hamedina-91" to setOf("levhamedina", "radiolevhamedina", "91fm"),
        "kol-rega-96" to setOf("kolrega", "kolrega91596", "96fm"),
        "radio-90" to setOf("radio90", "90fm"),
        "100fm" to setOf("100fmרדיוללאהפסקה", "radios100fm"),
        "jerusalem-101" to setOf("101fmradiojerusalem"),
        "lolfm" to setOf("lolfm", "רדיוצחוק"),
        "dudifm" to setOf("dudifm"),
        "nostalgia-963" to setOf("nostalgia963fm", "nostalgia"),
        "tzafon-1045" to setOf("1045fmradioזשכםמ", "1045fm"),
    )

    /** Alias keys (normalized) → seed slug. */
    private val aliasToSlug: Map<String, String> = buildMap {
        for ((slug, aliases) in ALIASES) for (a in aliases) put(StationRank.nameKey(a), slug)
    }

    /** Seed slug for a normalized API-station name, or null. */
    fun seedSlugFor(nameKey: String): String? = aliasToSlug[nameKey]

    fun bySlugOrNull(slug: String): SeedStation? = bySlug[slug]

    /**
     * Stations hidden outright, no matter what the directory claims.
     * Host match = any stream URL containing one of these fragments;
     * name match = normalized name exactly in this set.
     * (ברסלב פוריו: שידורי שמע דתיים שהמשתמש ביקש להסיר לחלוטין.)
     */
    val BLOCKED_HOSTS = listOf("breslevforyou.co.il", "cast.breslevforyou.co.il")

    /** Normalized names of banned stations (nameKey-normalized). */
    val BLOCKED_NAME_KEYS = setOf(
        StationRank.nameKey("ברסלב פוריו"),
        StationRank.nameKey("ברסלב פוריו: דעת רבנו"),
        StationRank.nameKey("ברסלב פוריו: הלכות ודף יומי"),
        StationRank.nameKey("Breslev For You"),
        StationRank.nameKey("Breslevforyou"),
    )

    fun isHostBlocked(url: String): Boolean {
        val u = url.lowercase()
        return BLOCKED_HOSTS.any { it in u }
    }

    fun isNameBlocked(nameKey: String): Boolean = nameKey in BLOCKED_NAME_KEYS
}
