package app.kestrel.core.media

import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** RX3-V5 (research/REVIEW-EXTERNAL-3.md part V): the markers a stream playlist uses to say "advertising from here to there". */
class AdMarkersTest {
    // A real splice_insert from the SCTE-35 documentation: out of network, program splice at a given time, break duration 5426421 ticks of 90 kHz = 60.29 s, auto return.
    private val REAL_OUT = "/DAvAAAAAAAA///wFAVIAACPf+/+c2nALv4AUsz1AAAAAAAKAAhDVUVJAAABNWLwC/g="

    @Test fun aRealSpliceInsertOutIsReadWithItsBreakDuration() {
        val s = AdMarkers.scte35(REAL_OUT)
        assertNotNull(s); assertTrue(s!!.out)
        assertEquals(60_293L, s.durationMs)                                       // 5426421 ticks of 90 kHz = 60.29 s
    }

    @Test fun theSameSpliceWithTheOutFlagClearedIsAReturnToTheProgram() {
        val b = Base64.getDecoder().decode(REAL_OUT)
        b[19] = (b[19].toInt() and 0x7F).toByte()                                 // out_of_network_indicator = 0
        val s = AdMarkers.scte35(Base64.getEncoder().encodeToString(b))
        assertNotNull(s); assertFalse(s!!.out)
    }

    @Test fun aCancelledOrOtherOrBrokenPayloadSaysNothing() {
        val b = Base64.getDecoder().decode(REAL_OUT)
        val cancelled = b.clone().also { it[18] = (it[18].toInt() or 0x80).toByte() }                // splice_event_cancel_indicator = 1
        assertNull(AdMarkers.scte35(Base64.getEncoder().encodeToString(cancelled)))
        val timeSignal = b.clone().also { it[13] = 0x06 }                          // a time_signal: its meaning sits in a descriptor that is not read
        assertNull(AdMarkers.scte35(Base64.getEncoder().encodeToString(timeSignal)))
        assertNull(AdMarkers.scte35(Base64.getEncoder().encodeToString(b.copyOf(12))))             // cut short
        assertNull(AdMarkers.scte35("not base64 !!"))
        assertNull(AdMarkers.scte35(""))
        assertNull(AdMarkers.scte35(Base64.getEncoder().encodeToString(b.clone().also { it[0] = 0x00 })))   // not a splice_info_section
    }

    @Test fun cueOutDurationsInTheFormsPlaylistsUse() {
        assertEquals(30_000L, AdMarkers.cueOutMs("30"))
        assertEquals(30_500L, AdMarkers.cueOutMs("30.5"))
        assertEquals(30_000L, AdMarkers.cueOutMs("DURATION=30"))
        assertEquals(15_000L, AdMarkers.cueOutMs("DURATION=15.0,CUE=\"x\""))
        assertEquals(-1L, AdMarkers.cueOutMs(""))
        assertEquals(-1L, AdMarkers.cueOutMs("soon"))
        assertEquals(-1L, AdMarkers.cueOutMs("-5"))
        assertEquals(-1L, AdMarkers.cueOutMs("99999999"))                          // a day-long "ad" is a mistake, not a duration
    }

    @Test fun cueOutContGivesTheTimeLeft() {
        assertEquals(24_000L, AdMarkers.cueOutContLeftMs("ElapsedTime=6.0,Duration=30.0"))
        assertEquals(24_000L, AdMarkers.cueOutContLeftMs("6/30"))
        assertEquals(-1L, AdMarkers.cueOutContLeftMs("ElapsedTime=6.0"))
        assertEquals(0L, AdMarkers.cueOutContLeftMs("ElapsedTime=40.0,Duration=30.0"))
    }
}
