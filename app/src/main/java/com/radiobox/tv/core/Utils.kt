package com.radiobox.tv.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Utils {

    /** ISO 3166-1 alpha-2 country code -> regional-indicator flag emoji. */
    fun flagEmoji(iso2: String?): String {
        if (iso2.isNullOrBlank() || iso2.length != 2) return "🏳"
        val sb = StringBuilder()
        for (ch in iso2.uppercase()) {
            if (ch !in 'A'..'Z') return "🏳"
            sb.append(Character.toChars(0x1F1E6 + (ch.code - 'A'.code)))
        }
        return sb.toString()
    }

    /** Parses podcast durations given as seconds ("3600") or clock form ("23:45", "1:02:03"). */
    fun parseDurationSec(raw: String?): Long? {
        val s = raw?.trim().orEmpty()
        if (s.isEmpty()) return null
        if (s.all { it.isDigit() }) return s.toLongOrNull()
        val parts = s.split(":").map { it.trim() }
        if (parts.any { it.isEmpty() || !it.all { c -> c.isDigit() } }) return null
        return when (parts.size) {
            2 -> parts[0].toLong() * 60 + parts[1].toLong()
            3 -> parts[0].toLong() * 3600 + parts[1].toLong() * 60 + parts[2].toLong()
            else -> null
        }
    }

    /** Formats seconds as m:ss or h:mm:ss. */
    fun formatDuration(totalSec: Long?): String {
        if (totalSec == null || totalSec < 0) return ""
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    fun formatMs(ms: Long): String = formatDuration(ms / 1000)

    fun formatKbps(bitrate: Int): String = if (bitrate > 0) "$bitrate kbps" else ""

    /** RFC-822 pubDate (e.g. "Wed, 02 Oct 2019 13:00:00 +0000") -> dd/MM/yyyy. */
    fun formatPubDate(raw: String?): String? {
        val s = raw?.trim().orEmpty()
        if (s.isEmpty()) return null
        return try {
            val p = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
            p.timeZone = TimeZone.getTimeZone("UTC")
            val d = p.parse(s) ?: return null
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(d)
        } catch (_: Exception) {
            try {
                val p = SimpleDateFormat("EEE, dd MMM yyyy", Locale.US)
                val d = p.parse(s) ?: return null
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(d)
            } catch (_: Exception) {
                s
            }
        }
    }

    fun todayLabel(): String = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
        .format(Date())

    /** Hebrew display name for a country code (falls back to the code itself). */
    fun countryLabel(code: String?): String = COUNTRY_NAMES[code?.uppercase()] ?: code.orEmpty()

    private val COUNTRY_NAMES = mapOf(
        "IL" to "ישראל", "US" to "ארה\"ב", "GB" to "בריטניה", "FR" to "צרפת",
        "DE" to "גרמניה", "IT" to "איטליה", "ES" to "ספרד", "NL" to "הולנד",
        "RU" to "רוסיה", "TR" to "טורקיה", "GR" to "יוון", "EG" to "מצרים",
        "JO" to "ירדן", "LB" to "לבנון", "MA" to "מרוקו", "TN" to "תוניסיה",
        "AE" to "אמירויות", "CA" to "קנדה", "BR" to "ברזיל", "MX" to "מקסיקו",
        "AR" to "ארגנטינה", "AU" to "אוסטרליה", "IN" to "הודו", "JP" to "יפן",
        "CN" to "סין", "KR" to "קוריאה הדרומית", "PL" to "פולין", "UA" to "אוקראינה",
        "RO" to "רומניה", "HU" to "הונגריה", "CZ" to "צ\"כיה", "SE" to "שוודיה",
        "NO" to "נורווגיה", "DK" to "דנמרק", "FI" to "פינלנד", "IE" to "אירלנד",
        "PT" to "פורטוגל", "BE" to "בלגיה", "AT" to "אוסטריה", "CH" to "שווייץ",
        "ZA" to "דרום אפריקה", "NG" to "ניגריה", "KE" to "קניה", "ET" to "אתיופיה",
        "SA" to "ערב הסעודית", "QA" to "קטאר", "KW" to "כווית", "IQ" to "עיראק",
        "IR" to "איראן", "YE" to "תימן", "ID" to "אינדונזיה",
    )
}
