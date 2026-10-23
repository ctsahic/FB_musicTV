# -*- coding: utf-8 -*-
"""
Curated seed list of Israeli stations for RadioBox TV / export_stations.py.

Every stream below was verified live (HTTP 200 + audio/* or m3u8) on 26/09/2026.
Sources: radio-browser.info, RLive station pages (sitemap scan of 194 pages),
and direct station/CDN endpoints. Tags are OUR editorial genre tags (the same
vocabulary as Genre.kt), so category placement is decided by us, not by the
radio-browser community. `hls: true` marks HLS (m3u8) streams.
"""
from dataclasses import dataclass
from typing import Optional


@dataclass(frozen=True)
class SeedStation:
    slug: str
    name: str
    stream: str
    tags: tuple
    hls: bool = False
    # Verified station logo (HTTP 200 image on 27/09/2026); None = initials fallback.
    logo: Optional[str] = None


TW = "https://playerservices.streamtheworld.com/api/livestream-redirect"
WC = "https://upload.wikimedia.org/wikipedia"

SEED = [
    # --- KAN (תאגיד השידור) — official streamtheworld endpoints ---
    SeedStation("kan-bet", "כאן רשת ב'", f"{TW}/KAN_BET.mp3", ("news", "talk", "israeli", "hebrew"),
                logo=f"{WC}/commons/thumb/5/50/KanBet.svg/500px-KanBet.svg.png"),
    SeedStation("kan-gimmel", "כאן רשת ג'", f"{TW}/KAN_GIMMEL.mp3", ("israeli", "hebrew", "mizrahi"),
                logo=f"{WC}/commons/thumb/3/39/KanGimel.svg/500px-KanGimel.svg.png"),
    SeedStation("kan-88", "כאן 88FM", f"{TW}/KAN_88.mp3", ("rock", "israeli", "alternative"),
                logo=f"{WC}/commons/thumb/0/0b/Kan88.svg/500px-Kan88.svg.png"),
    SeedStation("kan-kol-hamusica", "כאן קול המוסיקה", f"{TW}/KAN_KOL_HAMUSICA.mp3", ("classical", "opera", "symphony")),
    SeedStation("kan-moreshet", "כאן מורשת", f"{TW}/KAN_MORESHET.mp3", ("news", "talk", "israeli", "culture"),
                logo=f"{WC}/commons/thumb/e/e7/KanMoreshet.svg/500px-KanMoreshet.svg.png"),
    SeedStation("kan-reka", "כאן רק\"ע (רשת בערבית)", f"{TW}/KAN_REKA.mp3", ("world", "ethnic", "arabic"),
                logo=f"{WC}/commons/thumb/e/ee/KanReka.svg/500px-KanReka.svg.png"),
    SeedStation("kan-makan", "כאן רדיו מכאן", f"{TW}/RADIO_MAKAN.mp3", ("world", "ethnic", "arabic"),
                logo=f"{WC}/commons/thumb/a/a9/Kan_makan.svg/500px-Kan_makan.svg.png"),

    # --- גלי צהל (bynetcdn, official) ---
    SeedStation("glglz", "גלגלצ", "http://glzwizzlv.bynetcdn.com/glglz_mp3", ("pop", "top 40", "israeli", "hebrew"),
                logo=f"{WC}/commons/thumb/e/e2/GLGLZ.svg/500px-GLGLZ.svg.png"),
    SeedStation("glz", "גלצ", "http://glzwizzlv.bynetcdn.com/glz_mp3", ("news", "talk", "israeli", "hebrew"),
                logo=f"{WC}/commons/thumb/3/30/GaltzLogo.svg/500px-GaltzLogo.svg.png"),

    # --- רדיו אזורי מסחרי (cybercdn) ---
    SeedStation("galei-israel", "גלי ישראל", "https://cdn.cybercdn.live/Galei_Israel/Live/icecast.audio", ("news", "talk", "israeli"),
                logo=f"{WC}/he/1/12/GaleyIsraelLOGO.jpg"),
    SeedStation("lev-hamedina-91", "לב המדינה 91FM", "https://cdn.cybercdn.live/Lev_Hamedina/Audio/icecast.audio", ("israeli", "mizrahi", "hebrew"),
                logo="https://www.xoox.co.il/service/radio/img/radio_m14.jpg"),
    SeedStation("kol-rega-96", "קול רגע 96FM", "https://cdn.cybercdn.live/Kol_Rega/Live_Audio/icecast.audio", ("israeli", "pop", "hebrew"),
                logo="https://96fm.co.il/wp-content/uploads/2022/10/cropped-Logo-192x192.png"),
    SeedStation("radio-90", "רדיו 90", "https://cdn.cybercdn.live/Emtza_Haderech/Live_Audio/icecast.audio", ("israeli", "hebrew")),
    SeedStation("103fm", "103FM", "https://cdn.cybercdn.live/103FM/Live/icecast.audio", ("news", "talk", "israeli"),
                logo=f"{WC}/he/thumb/7/72/103FM.svg/500px-103FM.svg.png"),
    SeedStation("100fm", "100FM רדיו ללא הפסקה", "https://cdn.cybercdn.live/Radios_100FM/Audio/playlist.m3u8", ("rock", "alternative", "israeli"), hls=True,
                logo="https://digital.100fm.co.il/logo192.png"),
    SeedStation("tzafon-1045", "104.5FM צפון ללא הפסקה", "https://cdn.cybercdn.live/Tzafon_NonStop/Live_Audio/icecast.audio", ("news", "talk")),
    SeedStation("radio-2000", "רדיו 2000", "https://cdn.cybercdn.live/Radio2000/MP3/icecast.audio", ("israeli", "mizrahi", "nostalgia")),
    SeedStation("kol-barama", "קול ברמה", "https://cdn.cybercdn.live/Kol_Barama/Live_Audio/icecast.audio", ("mizrahi", "israeli"),
                logo=f"{WC}/he/thumb/4/48/%D7%9C%D7%95%D7%92%D7%95_%D7%A7%D7%95%D7%9C_%D7%91%D7%A8%D7%9E%D7%94.png/500px-%D7%9C%D7%95%D7%92%D7%95_%D7%A7%D7%95%D7%9C_%D7%91%D7%A8%D7%9E%D7%94.png"),
    SeedStation("hatahana-1015", "התחנה 101.5FM", "https://cdn.cybercdn.live/Hatahana_1015/Live_Audio/icecast.audio", ("mizrahi", "israeli"),
                logo="https://hatahana.co.il/favicon.ico"),
    SeedStation("darom-97", "רדיו דרום 97FM", "https://cdn.cybercdn.live/Darom_97FM/Live/icecast.audio", ("israeli", "hebrew"),
                logo="https://odsv.org/wp-content/uploads/2020/03/%D7%A8%D7%93%D7%99%D7%95-%D7%93%D7%A8%D7%95%D7%9D-1.png"),
    SeedStation("darom-1015", "רדיו דרום 101.5FM", "https://cdn.cybercdn.live/Darom_1015FM/Live/icecast.audio", ("israeli", "hebrew")),
    SeedStation("jerusalem-101", "101FM ירושלים", "https://cdn.cybercdn.live/JerusalemRadio/Live/icecast.audio", ("israeli", "news", "hebrew"),
                logo="https://www.101fm.co.il/favicon.ico"),

    # --- ecast ---
    SeedStation("lolfm", "רדיו צחוק FM (LOLfm)", "https://live.ecast.co.il/stream/69fm/lolfm", ("humor", "comedy", "israeli", "hebrew")),
    SeedStation("radio-sahar", "רדיו סהר", "https://live.ecast.co.il/stream/sahar", ("israeli", "hebrew", "community"),
                logo="https://radiosahar.co.il/images/logo.png"),
    SeedStation("dudifm", "דודי FM", "https://live.ecast.co.il/stream/dudifm", ("mizrahi", "israeli")),
    SeedStation("hamizrahit", "המזרחית FM", "https://live.ecast.co.il/stream/mizrahit", ("mizrahi", "israeli")),
    SeedStation("kol-rishonim", "קול ראשונים", "https://live.ecast.co.il/stream/1062", ("mizrahi", "israeli")),
    SeedStation("radio-idan", "רדיו אידן", "https://live.ecast.co.il/stream/radioneto1", ("mizrahi", "israeli")),
    SeedStation("69fm", "69FM", "https://live.ecast.co.il/stream/69fm", ("israeli", "hebrew")),
    SeedStation("kol-hatzafon", "קול הצפון", "https://live.ecast.co.il/stream/khz", ("israeli", "hebrew")),
    SeedStation("kol-galim", "קול גלים", "https://live.ecast.co.il/stream/galimlive", ("israeli", "hebrew")),
    SeedStation("krayotfm", "קריות FM", "https://live.ecast.co.il/stream/krayotfm", ("israeli", "hebrew")),
    SeedStation("harokdim", "הרוקדים", "https://live.ecast.co.il:8022/stream", ("electronic", "dance", "party")),
    SeedStation("hevratifm", "רדיו חברתי FM", "https://live.ecast.co.il/stream/hevrati", ("news", "talk", "israeli")),

    # --- streamgates / misc ---
    SeedStation("n12", "חדשות 13 (רדיו)", "http://ff_engine.streamgates.net/Ch10News.mp3", ("news", "talk"),
                logo=f"{WC}/he/thumb/2/24/News13_new2022.svg/500px-News13_new2022.svg.png"),
    SeedStation("nostalgia-963", "נוסטלגיה 96.3FM", "https://cdna.streamgates.net/radios-audio/Nostalgia_963fm/playlist.m3u8", ("israeli", "nostalgia", "mizrahi"), hls=True),
    SeedStation("the-best", "The Best רדיו", "https://cdna.streamgates.net/TheBest/live/playlist.m3u8", ("mizrahi", "israeli"), hls=True),

    # --- עצמאיות / קהילתיות ---
    SeedStation("kzradio", "KZ רדיו קול הקמפוס", "https://kzradio.mediacast.co.il/kzradio_live/kzradio/icecast.audio", ("rock", "alternative", "indie")),
    SeedStation("zerock", "זה רוק", "https://icecast.live/zerock", ("rock", "hebrew"),
                logo="https://zerockradio.com/wp-content/uploads/2024/03/logo-flat.png"),
    SeedStation("up2dance", "Up2Dance", "https://icecast.live/up2dance", ("electronic", "dance"),
                logo="https://up2dance.com/wp-content/uploads/2021/01/Up2Dance-Radio-01-01-768x742.png"),
    SeedStation("hitsil", "Hits IL", "https://icecast.live/hitsil", ("pop", "hits", "israeli")),
    SeedStation("eol", "רדיו EOL ירושלים", "https://eol-live.cdnwiz.com/eol/eolsite/icecast.audio", ("israeli", "news", "talk"),
                logo="https://eol.co.il/wp-content/uploads/2023/02/15144351/logo_eol_1200.png"),
    SeedStation("kol-ness-ziona", "קול נס ציונה", "http://kolness.livecast.co.il/liveness", ("israeli", "hebrew")),
    SeedStation("radio106-ariel", "רדיו אריאל 106FM", "https://ariel.icecast.live/hls/1/stream.m3u8", ("israeli", "hebrew"), hls=True),
    SeedStation("kol-netanya", "רדיו קול נתניה", "https://icecast.live/hls/kol_netanya/rkn_hls.m3u8", ("israeli", "hebrew"), hls=True),
    SeedStation("neurim-fm", "נעים FM", "https://icecast.live/hls/neurimfm/live.m3u8", ("israeli", "hebrew"), hls=True),
    SeedStation("pervoe", "פרבוה רדיו (רוסית)", "https://cdn.cybercdn.live/Pervoia/Audio/playlist.m3u8", ("russian", "world", "ethnic"), hls=True),
]


def seed_as_rbstation(s: SeedStation) -> dict:
    """Shape matching the RBStation dicts used by export_stations.py."""
    return {
        "stationuuid": f"seed-{s.slug}",
        "name": s.name,
        "url_resolved": s.stream,
        "url": s.stream,
        "homepage": "",
        "favicon": s.logo or "",
        "tags": ",".join(s.tags),
        "votes": 0,
        "bitrate": 0,
        "codec": "",
        "hls": 1 if s.hls else 0,
        "state": "",
    }


# Alternate spellings under which the same station appears in radio-browser
# (mirrors app/src/main/java/com/radiobox/tv/core/IsraeliSeed.kt ALIASES).
ALIASES = {
    "kan-bet": {"kanbet"},
    "kan-gimmel": {"kangimel", "reshetgimmel"},
    "kan-88": {"kan88", "88fm"},
    "kan-kol-hamusica": {"kankolhamusica", "kolhamusica"},
    "kan-moreshet": {"kanmoreshet", "moreshet"},
    "kan-reka": {"kanreka", "reka"},
    "kan-makan": {"kanmakan", "radiomakan"},
    "glglz": {"glglz"},
    "glz": {"glz"},
    "galei-israel": {"galeiyisrael", "94fmרדיוגליישראל"},
    "lev-hamedina-91": {"levhamedina", "radiolevhamedina", "91fm"},
    "kol-rega-96": {"kolrega", "kolrega91596", "96fm"},
    "radio-90": {"radio90", "90fm"},
    "103fm": {"103fm"},
    "100fm": {"100fmרדיוללאהפסקה", "radios100fm"},
    "jerusalem-101": {"101fmradiojerusalem"},
    "lolfm": {"lolfm"},
    "radio-sahar": {"radiosahar"},
    "dudifm": {"dudifm"},
    "nostalgia-963": {"nostalgia963fm", "nostalgia"},
    "tzafon-1045": {"1045fmradioזשכםמ", "1045fm"},
}


def _name_key(name: str) -> str:
    k = "".join(c for c in name.strip().lower() if c.isalnum())
    return k or name.strip().lower()


_ALIAS_TO_SLUG = {_name_key(a): slug for slug, aliases in ALIASES.items() for a in aliases}


def seed_slug_for(name_key: str):
    return _ALIAS_TO_SLUG.get(name_key)
