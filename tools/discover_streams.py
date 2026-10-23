# -*- coding: utf-8 -*-
"""Discover real stream URLs for Israeli stations missing from radio-browser.

Sources:
  1. RLive (rlive.co.il) station pages - they embed the station's direct stream URL.
  2. Known CDN candidates (streamgates / streamtheworld / cybercdn / ecast),
     each validated with a live HTTP probe.

Usage:
  python tools/discover_streams.py            # scrape RLive + probe candidates
Writes: tools/discovered_streams.json
"""
import io
import json
import re
import sys
import time
import urllib.request

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
UA = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/124.0 Safari/537.36")

STREAM_RE = re.compile(
    r"https?://[^\s\"'<>\\]+", re.I)
GOOD_HOST_RE = re.compile(
    r"(ecast\.co\.il|streamgates\.net|streamtheworld\.com|cybercdn\.live|"
    r"bynetcdn\.com|rlive\.co\.il|live\.cloud|radiocdn|icecast)", re.I)
AUDIO_EXT_RE = re.compile(r"\.(mp3|aac|m3u8|pls)(\?|$)", re.I)


def fetch(url, timeout=15):
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return resp.read().decode("utf-8", "replace")


def probe(url):
    """Return (ok, content_type) - HEAD first, then a small ranged GET."""
    for method, headers in (("HEAD", {}), ("GET", {"Range": "bytes=0-40000"})):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": UA, **headers})
            with urllib.request.urlopen(req, timeout=10) as resp:
                ctype = (resp.headers.get("Content-Type") or "").lower()
                if "audio" in ctype or "mpegurl" in ctype or method == "GET":
                    return True, ctype.split(";")[0]
        except Exception:
            time.sleep(0.2)
    return False, ""


def rlive_all_slugs():
    """Collect every /station/<slug> link from RLive's sitemap."""
    slugs = []
    try:
        xml = fetch("https://www.rlive.co.il/sitemap.xml", timeout=25)
        slugs = re.findall(r"https://www\.rlive\.co\.il/station/([^<]+)</loc>", xml)
    except Exception as e:
        print(f"  (sitemap failed: {e})")
    seen, out = set(), []
    for s in slugs:
        s = s.strip()
        if s and s not in seen:
            seen.add(s)
            out.append(s)
    return out


def scrape_rlive(slug):
    """Extract the stream URL from one RLive station page (or None)."""
    for s in (slug, urllib.parse.unquote(slug)):
        url = f"https://www.rlive.co.il/station/{s}"
        try:
            html = fetch(url, timeout=15)
        except Exception:
            continue
        cands = []
        for u in STREAM_RE.findall(html):
            u = u.replace("\\u0026", "&").replace("\\/", "/").rstrip("\\")
            if GOOD_HOST_RE.search(u) and "rlive.co.il/station" not in u:
                cands.append(u)
        if cands:
            # prefer explicit audio extensions, then ecast/streamgates
            cands.sort(key=lambda u: (not AUDIO_EXT_RE.search(u), "ecast" not in u))
            return cands[0]
    return None


# Extra candidates to validate even if RLive doesn't list them.
CANDIDATES = {
    "kan_gimel_guess": "https://28563.live.streamtheworld.com/KAN_GIMEL.mp3",
    "glglz_guess": "https://28563.live.streamtheworld.com/GLGLZ.mp3",
    "glz_bynet": "http://glzwizzlv.bynetcdn.com/glz_mp3",
    "galei_israel": "https://cdn.cybercdn.live/Galei_Israel/Live/icecast.audio",
    "lev_hamedina": "https://cdn.cybercdn.live/Lev_Hamedina/Audio/icecast.audio",
    "kol_rega": "https://cdn.cybercdn.live/Kol_Rega/Live_Audio/icecast.audio",
    "101fm": "https://radio.streamgates.net/stream/101fm",
    "1045fm": "https://radio.streamgates.net/stream/1045fm",
    "103fm": "https://radio.streamgates.net/stream/103fm",
    "100fm": "https://radio.streamgates.net/stream/100fm",
    "102fm": "https://radio.streamgates.net/stream/102fm",
    "lolfm_ecast": "https://live.ecast.co.il/stream/69fm/lolfm",
}


def main():
    import os
    result = {"rlive": {}, "candidates": {}}
    if os.path.exists("tools/discovered_streams.json"):
        with open("tools/discovered_streams.json", encoding="utf-8") as f:
            result = json.load(f)

    def save():
        with open("tools/discovered_streams.json", "w", encoding="utf-8") as f:
            json.dump(result, f, ensure_ascii=False, indent=2)

    print("Scanning RLive sitemap ...")
    slugs = rlive_all_slugs()
    print(f"  found {len(slugs)} station pages")
    done = sum(1 for s in slugs if s in result["rlive"])
    print(f"  resuming: {done} already scanned")
    for i, slug in enumerate(slugs, 1):
        if slug in result["rlive"]:
            continue
        stream = scrape_rlive(slug)
        if stream:
            ok, ctype = probe(stream)
            result["rlive"][slug] = {"stream": stream, "probe": "OK" if ok else "FAIL",
                                     "content_type": ctype}
            print(f"  [{i}/{len(slugs)}] {slug}: {stream} -> {'OK' if ok else 'FAIL'}")
        else:
            result["rlive"][slug] = {"stream": None, "probe": "NOT_FOUND", "content_type": ""}
            print(f"  [{i}/{len(slugs)}] {slug}: (no stream found)")
        if i % 10 == 0:
            save()
        time.sleep(0.1)
    save()

    print("\nProbing known candidates ...")
    for name, url in CANDIDATES.items():
        if name in result["candidates"]:
            continue
        ok, ctype = probe(url)
        result["candidates"][name] = {"stream": url, "probe": "OK" if ok else "FAIL",
                                      "content_type": ctype}
        print(f"  {name}: {'OK ' + ctype if ok else 'FAIL'}  {url}")

    with open("tools/discovered_streams.json", "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)
    ok_rlive = sum(1 for v in result["rlive"].values() if v["probe"] == "OK")
    ok_cand = sum(1 for v in result["candidates"].values() if v["probe"] == "OK")
    print(f"\nDone: {ok_rlive} RLive streams verified, {ok_cand}/{len(CANDIDATES)} "
          f"candidates verified. -> tools/discovered_streams.json")


if __name__ == "__main__":
    import urllib.parse
    main()
