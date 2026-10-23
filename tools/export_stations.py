# -*- coding: utf-8 -*-
"""
Export stations from the radio-browser.info API into one HTML file,
grouped by the same categories the RadioBox TV app uses.

Queries mirror StationsRepository: one /json/stations/search request per genre
tag (OR-merged), deduped by uuid / stream URL / normalized name, then ranked
with the same StationRank logic (tag-spam demotion, core-tag match, relevance,
votes, bitrate). "אזוריות" = IL stations that report a state/region.

Usage:  python tools/export_stations.py
Output: radio-browser-il-stations.html  (project root)
"""

import html
import json
import re
import sys
import time
import urllib.parse
import urllib.request
from datetime import date

from israeli_seed import SEED, ALIASES, seed_as_rbstation, seed_slug_for

IMAGE_RE = re.compile(r"\.(jpg|jpeg|png|webp|gif|svg)(\?|$)", re.I)

MIRRORS = [
    "https://de1.api.radio-browser.info",
    "https://de2.api.radio-browser.info",
    "https://fi1.api.radio-browser.info",
]
COUNTRY = "IL"
LIMIT_PER_TAG = 100     # per single tag query (app uses 40)
REGIONAL_LIMIT = 200    # like the app's regional query
SECTION_CAP = 100       # max stations listed per category
UA = "RadioBoxTV-export/1.0 (radio-browser.info community list)"

# Same genres/tags as app/src/main/java/com/radiobox/tv/core/Genre.kt
GENRES = [
    ("electronic", "אלקטרונית", "🎛", ["electronic", "dance", "techno", "house", "trance", "edm"]),
    ("classical", "קלאסית", "🎻", ["classical", "opera", "symphony"]),
    ("news", "חדשות", "📰", ["news", "talk", "information"]),
    ("israeli", "ישראלי", "🇮🇱", ["israeli", "hebrew", "mizrahi"]),
    ("pop", "פופ", "🎤", ["pop", "top 40"]),
    ("rock", "רוק", "🎸", ["rock", "alternative", "metal"]),
    ("jazz", "ג'אז ובלוז", "🎷", ["jazz", "blues", "soul"]),
    ("world", "מוזיקת עולם", "🌍", ["world", "folk", "ethnic"]),
    ("regional", "אזוריות", "📍", []),
]

# Same NOISE_TAGS as core/StationRank.kt
NOISE_TAGS = {
    "music", "musica", "musik", "muzika", "various", "various artists", "radio", "web radio",
    "hits", "hit music", "charts", "top 100", "mix", "mixed", "party", "live",
    "stream", "audio", "fm", "am", "dab", "internet", "online", "web",
    "local", "community", "student", "public", "commercial", "news talk",
    "interviews", "schedule", "entertainment", "culture", "lifestyle",
    "israel", "israeli music", "hebrew music", "80s", "90s", "70s", "60s", "2000s",
}


def fetch(path, params):
    query = urllib.parse.urlencode(params)
    last_err = None
    for attempt in range(2):
        for base in MIRRORS:
            url = f"{base}{path}?{query}"
            try:
                req = urllib.request.Request(url, headers={"User-Agent": UA})
                with urllib.request.urlopen(req, timeout=20) as resp:
                    return json.loads(resp.read().decode("utf-8"))
            except Exception as e:  # noqa: BLE001 - try next mirror
                last_err = e
                time.sleep(0.4)
    raise RuntimeError(f"all mirrors failed for {path}: {last_err}")


# ---- Port of core/StationRank.kt -------------------------------------------

def tag_list(st):
    return [t.strip().lower() for t in (st.get("tags") or "").split(",") if t.strip()]


def stream_key(url):
    k = (url or "").strip().lower()
    for p in ("https://", "http://"):
        if k.startswith(p):
            k = k[len(p):]
            break
    if k.startswith("www."):
        k = k[4:]
    return k.rstrip("/")


def name_key(name):
    k = "".join(c for c in name.strip().lower() if c.isalnum())
    return k or name.strip().lower()


def relevance_fraction(tags, genre_tags):
    meaningful = [t for t in tags if t not in NOISE_TAGS]
    if not meaningful:
        return 0.0
    return sum(1 for t in meaningful if t in genre_tags) / len(meaningful)


def is_tag_spam(tags, genre_tags):
    if len(tags) < 8:
        return False
    meaningful = [t for t in tags if t not in NOISE_TAGS]
    frac = relevance_fraction(tags, genre_tags)
    return len(meaningful) == 0 or frac < 0.3


def rank_by_genre(stations, core, genre_tags):
    def sort_key(st):
        tags = tag_list(st)
        return (
            1 if is_tag_spam(tags, genre_tags) else 0,
            -(1 if core in tags else 0),
            -relevance_fraction(tags, genre_tags),
            -(st.get("votes") or 0),
            -(st.get("bitrate") or 0),
        )
    return sorted(stations, key=sort_key)


def merge_and_rank(raw, genre_tags):
    """Same pipeline as StationsRepository.stationsForGenre (+ seed priority)."""
    seen_uuid, seen_stream, seen_name, out = set(), set(), set(), []
    for st in raw:
        url = (st.get("url_resolved") or st.get("url") or "").strip()
        if not url or IMAGE_RE.search(url):
            continue
        if st.get("stationuuid") in seen_uuid:
            continue
        seen_uuid.add(st["stationuuid"])
        out.append(st)
    # Curated seed entries win over API entries; then votes, then bitrate.
    out.sort(key=lambda s: (0 if str(s.get("stationuuid", "")).startswith("seed-") else 1,
                            -(s.get("votes") or 0), -(s.get("bitrate") or 0)))
    merged = []
    for st in out:
        sk = stream_key(st.get("url_resolved") or st.get("url") or "")
        nk = name_key(st.get("name") or "")
        if sk in seen_stream or nk in seen_name:
            continue
        # API twin of a curated seed station under an alternate spelling?
        if not str(st.get("stationuuid", "")).startswith("seed-") and seed_slug_for(nk):
            continue
        seen_stream.add(sk)
        seen_name.add(nk)
        merged.append(st)
    return rank_by_genre(merged, genre_tags[0], set(genre_tags))[:SECTION_CAP]


# ---- HTML -------------------------------------------------------------------

CSS = """
:root { --bg:#0d1117; --card:#161b22; --line:#2d333b; --txt:#e6edf3; --dim:#8b949e; --acc:#4cc2ff; }
* { box-sizing:border-box; }
body { margin:0; padding:24px 20px 60px; background:var(--bg); color:var(--txt);
       font-family:"Segoe UI",system-ui,-apple-system,Arial,sans-serif; direction:rtl; }
header { max-width:1200px; margin:0 auto 18px; }
h1 { font-size:26px; margin:0 0 6px; }
.sub { color:var(--dim); font-size:13px; line-height:1.6; }
nav { max-width:1200px; margin:14px auto 26px; display:flex; flex-wrap:wrap; gap:8px; position:sticky;
      top:0; background:var(--bg); padding:10px 0; border-bottom:1px solid var(--line); z-index:5; }
nav a { color:var(--txt); text-decoration:none; background:var(--card); border:1px solid var(--line);
        border-radius:999px; padding:6px 14px; font-size:14px; }
nav a:hover { border-color:var(--acc); }
nav .n { color:var(--dim); font-size:12px; }
section { max-width:1200px; margin:0 auto 40px; }
h2 { font-size:20px; margin:0 0 12px; border-inline-start:4px solid var(--acc); padding-inline-start:10px; }
.grid { display:grid; grid-template-columns:repeat(auto-fill,minmax(330px,1fr)); gap:12px; }
.card { background:var(--card); border:1px solid var(--line); border-radius:12px; padding:12px 14px; }
.card .name { font-weight:600; font-size:15px; margin-bottom:4px; overflow-wrap:anywhere; }
.card .meta { color:var(--dim); font-size:12px; margin-bottom:6px; }
.tags { display:flex; flex-wrap:wrap; gap:5px; margin-bottom:8px; }
.tags span { background:#21262d; border:1px solid var(--line); color:var(--dim); font-size:11px;
             border-radius:999px; padding:1px 8px; }
.links a { color:var(--acc); text-decoration:none; font-size:13px; }
.links a:hover { text-decoration:underline; }
.links .sep { color:var(--line); margin:0 6px; }
.hls { color:#e3b341; font-size:11px; border:1px solid #5c4a1e; border-radius:4px; padding:0 5px; }
footer { max-width:1200px; margin:0 auto; color:var(--dim); font-size:12px; line-height:1.7;
         border-top:1px solid var(--line); padding-top:14px; }
a { color:var(--acc); }
"""


def esc(s):
    return html.escape(str(s if s is not None else ""), quote=True)


def is_seed(st):
    return str(st.get("stationuuid", "")).startswith("seed-")


def station_json(st):
    return {
        "name": (st.get("name") or "").strip(),
        "stream": st.get("url_resolved") or st.get("url") or "",
        "uuid": st.get("stationuuid") or "",
        "seed": is_seed(st),
        "tags": tag_list(st),
        "votes": st.get("votes") or 0,
        "bitrate": st.get("bitrate") or 0,
        "codec": (st.get("codec") or "").strip(),
        "hls": bool(st.get("hls")),
        "homepage": (st.get("homepage") or "").strip(),
    }


def station_card(st):
    name = (st.get("name") or "").strip() or "(ללא שם)"
    seed_badge = ' <span class="hls">★ seed</span>' if is_seed(st) else ""
    stream = st.get("url_resolved") or st.get("url") or ""
    votes = st.get("votes") or 0
    br = st.get("bitrate") or 0
    codec = (st.get("codec") or "").strip()
    meta = f"👍 {votes:,}"
    if br:
        meta += f" · {br} kbps"
    if codec and codec.upper() != "UNKNOWN":
        meta += f" · {esc(codec)}"
    hls = ' <span class="hls">HLS</span>' if st.get("hls") else ""
    tags = [t for t in tag_list(st) if t not in NOISE_TAGS][:6]
    if not tags:
        tags = tag_list(st)[:2]
    tag_html = "".join(f"<span>{esc(t)}</span>" for t in tags)
    home = (st.get("homepage") or "").strip()
    home_html = f'<span class="sep">·</span><a href="{esc(home)}" target="_blank">אתר הבית</a>' if home else ""
    return (
        '<div class="card">'
        f'<div class="name">{esc(name)}{hls}{seed_badge}</div>'
        f'<div class="meta">{meta}</div>'
        f'<div class="tags">{tag_html}</div>'
        f'<div class="links"><a href="{esc(stream)}" target="_blank">▶ הסטרים</a>{home_html}</div>'
        "</div>"
    )


def main():
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    print(f"Fetching stations for country={COUNTRY} ...")
    nav_items, sections, json_genres = [], [], {}

    for gid, label, emoji, tags in GENRES:
        if gid == "regional":
            raw = fetch("/json/stations/search", {
                "countrycode": COUNTRY, "hidebroken": "true",
                "order": "votes", "reverse": "true", "limit": str(REGIONAL_LIMIT),
            })
            raw = [s for s in raw if (s.get("state") or "").strip()]
            # same dedupe/rank-lite as the app's regionalStations()
            seen, uniq = set(), []
            for s in raw:
                if s["stationuuid"] in seen:
                    continue
                seen.add(s["stationuuid"])
                uniq.append(s)
            uniq.sort(key=lambda s: -(s.get("votes") or 0))
            seen_stream, seen_name, merged = set(), set(), []
            for s in uniq:
                sk = stream_key(s.get("url_resolved") or s.get("url") or "")
                nk = name_key(s.get("name") or "")
                if sk in seen_stream or nk in seen_name:
                    continue
                seen_stream.add(sk)
                seen_name.add(nk)
                merged.append(s)
            ranked = merged[:SECTION_CAP]
        else:
            raw = []
            for tag in tags:
                print(f"  [{label}] tag='{tag}' ...")
                raw += fetch("/json/stations/search", {
                    "countrycode": COUNTRY, "hidebroken": "true",
                    "order": "votes", "reverse": "true",
                    "limit": str(LIMIT_PER_TAG), "tag": tag,
                })
                time.sleep(0.15)
            # Prepend curated seed stations whose tags overlap this genre.
            seed_sts = [seed_as_rbstation(s) for s in SEED if set(tags) & set(s.tags)]
            raw = seed_sts + raw
            ranked = merge_and_rank(raw, tags)

        json_genres[gid] = {
            "label": label, "emoji": emoji, "tags": tags,
            "stations": [station_json(s) for s in ranked],
        }
        anchor = gid
        nav_items.append(
            f'<a href="#{anchor}">{emoji} {esc(label)} <span class="n">({len(ranked)})</span></a>'
        )
        cards = "".join(station_card(s) for s in ranked)
        sections.append(
            f'<section id="{anchor}"><h2>{emoji} {esc(label)} <span style="color:var(--dim);font-size:14px">'
            f'({len(ranked)} תחנות)</span></h2><div class="grid">{cards}</div></section>'
        )
        print(f"  => {label}: {len(ranked)} stations")

    total = sum(1 for _ in "".join(sections).split('class="card"')) - 1
    today = date.today().strftime("%d/%m/%Y")
    doc = f"""<!DOCTYPE html>
<html lang="he" dir="rtl">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>RadioBox TV — תחנות radio-browser.info לפי קטגוריה ({esc(COUNTRY)})</title>
<style>{CSS}</style>
</head>
<body>
<header>
<h1>📻 תחנות radio-browser.info לפי קטגוריה 🇮🇱</h1>
<div class="sub">
נאסף מה-API הרשמי של <a href="https://www.radio-browser.info" target="_blank">radio-browser.info</a>
ב-{today} · מדינה: ישראל · ~{total} כרטיסים (כפילויות מוזגו).<br>
תחנות מסומנות <b>★ seed</b> מגיעות מרשימה מקומית מאומתת (כולל רדיו צחוק, גלגלצ וכאן) — הז'אנר שלהן נקבע על ידינו ולא על ידי הקהילה.<br>
קיים גם קובץ <code>radio-browser-il-stations.json</code> עם אותם נתונים בפורמט מכונות.
קטגוריה = תגיות הקהילה של radio-browser, ממופות כמו באפליקציה: למשל "ישראלי" = תגיות
<code>israeli, hebrew, mizrahi</code>; "חדשות" = <code>news, talk, information</code>.
תחנה עם הרבה תגיות כלליות מדורגת נמוך (אותה לוגיקה כמו באפליקציה).
</div>
</header>
<nav>{''.join(nav_items)}</nav>
{''.join(sections)}
<footer>
מקור: radio-browser.info (רישיון CC-BY 4.0) — הנתונים מנוהלים על ידי הקהילה. ייתכנו תיוגים שגויים,
למשל תחנות שתויגו בטעות בכמה ז'אנרים. קישורי "הסטרים" פותחים את כתובת ה-URL הישירה של התחנה.
</footer>
</body>
</html>
"""
    out = "radio-browser-il-stations.html"
    with open(out, "w", encoding="utf-8") as f:
        f.write(doc)

    jout = "radio-browser-il-stations.json"
    with open(jout, "w", encoding="utf-8") as f:
        json.dump({
            "generated": today,
            "country": COUNTRY,
            "source": "radio-browser.info API + curated local seed (tools/israeli_seed.py)",
            "genres": json_genres,
        }, f, ensure_ascii=False, indent=2)

    n_seed = sum(1 for g in json_genres.values() for s in g["stations"] if s["seed"])
    print(f"\nWrote {out} ({total} cards) and {jout} "
          f"({sum(len(g['stations']) for g in json_genres.values())} entries, {n_seed} seed). Done.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
