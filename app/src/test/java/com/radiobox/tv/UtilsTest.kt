package com.radiobox.tv

import com.radiobox.tv.core.Utils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UtilsTest {

    @Test
    fun `flag emoji for IL`() {
        assertEquals("🇮🇱", Utils.flagEmoji("IL"))
    }

    @Test
    fun `flag emoji for US`() {
        assertEquals("🇺🇸", Utils.flagEmoji("us"))
    }

    @Test
    fun `flag emoji fallback for null or bad code`() {
        assertEquals("🏳", Utils.flagEmoji(null))
        assertEquals("🏳", Utils.flagEmoji(""))
        assertEquals("🏳", Utils.flagEmoji("1"))
        assertEquals("🏳", Utils.flagEmoji("IZ1"))
    }

    @Test
    fun `duration seconds only`() {
        assertEquals(3661L, Utils.parseDurationSec("3661"))
    }

    @Test
    fun `duration mm ss`() {
        assertEquals(65L, Utils.parseDurationSec("1:05"))
    }

    @Test
    fun `duration hh mm ss`() {
        assertEquals(3661L, Utils.parseDurationSec("1:01:01"))
    }

    @Test
    fun `duration invalid returns null`() {
        assertNull(Utils.parseDurationSec(null))
        assertNull(Utils.parseDurationSec(""))
        assertNull(Utils.parseDurationSec("abc"))
        assertNull(Utils.parseDurationSec("1:xx:05"))
    }

    @Test
    fun `format duration`() {
        assertEquals("23:45", Utils.formatDuration(1425))
        assertEquals("1:01:01", Utils.formatDuration(3661))
        assertEquals("", Utils.formatDuration(null))
        assertEquals("0:00", Utils.formatDuration(0))
    }

    @Test
    fun `format pub date RFC822`() {
        val out = Utils.formatPubDate("Wed, 02 Oct 2019 13:00:00 +0000")
        assertTrue(out == "02/10/2019" || out == "02/10/2019".let { it }) // timezone shift possible; presence check
    }

    @Test
    fun `format pub date fallback returns raw`() {
        assertEquals("מחר", Utils.formatPubDate("מחר"))
    }
}
