package app.kestrel.core.player

import app.kestrel.core.player.DurationView.Kind
import org.junit.Assert.assertEquals
import org.junit.Test

/** VP5 (owner message 49: "falsely reported that the video was 4 seconds long when it was over one minute"). */
class DurationViewTest {
    private fun DurationView.at(now: Long, d: Double, t: Double = 0.0, b: Double = 0.0, mse: Boolean = true, live: Boolean = false) = feed(d, t, b, mse, live, now)

    @Test fun aMediaSourceStreamWhoseLengthIsStillGrowingShowsNoLength() {
        val v = DurationView()
        assertEquals("the first look: 4 s is just what has been appended", Kind.UNKNOWN, v.at(0, 4.0, b = 4.0).kind)
        assertEquals(Kind.UNKNOWN, v.at(900, 8.0, t = 1.0, b = 8.0).kind)
        assertEquals(Kind.UNKNOWN, v.at(1800, 12.0, t = 2.0, b = 12.0).kind)
        assertEquals("the page then sets the whole length: not believed at once", Kind.UNKNOWN, v.at(2000, 65.0, t = 2.5, b = 12.0).kind)
        val s = v.at(4100, 65.0, t = 4.5, b = 20.0)
        assertEquals("the same 2 s later: believed", Kind.KNOWN, s.kind); assertEquals(65.0, s.seconds, 0.0)
    }

    @Test fun aFilmThatReallyIsFourSecondsLongIsBelievedOnceItHasStayedSo() {
        val v = DurationView()
        assertEquals(Kind.UNKNOWN, v.at(0, 4.0, b = 4.0).kind)
        val s = v.at(2100, 4.0, t = 1.5, b = 4.0)
        assertEquals(Kind.KNOWN, s.kind); assertEquals(4.0, s.seconds, 0.0)
    }

    @Test fun aPlainFileIsBelievedAtOnce() {
        val s = DurationView().at(0, 65.0, mse = false)
        assertEquals(Kind.KNOWN, s.kind); assertEquals(65.0, s.seconds, 0.0)
    }

    @Test fun aLengthSmallerThanWhatWasPlayedOrBufferedIsWrongHoweverLongItStays() {
        val v = DurationView()
        v.at(0, 4.0, t = 9.0, b = 12.0)
        assertEquals(Kind.UNKNOWN, v.at(5000, 4.0, t = 9.0, b = 12.0).kind)
        assertEquals("rounding is allowed", Kind.KNOWN, DurationView().at(0, 10.0, t = 10.5, b = 10.4, mse = false).kind)
    }

    @Test fun anInfiniteOrLiveStreamSaysLive() {
        assertEquals(Kind.LIVE, DurationView().at(0, Double.POSITIVE_INFINITY).kind)
        assertEquals(Kind.LIVE, DurationView().at(0, 120.0, live = true).kind)
    }

    @Test fun noLengthYetIsUnknownAndANewFilmStartsOverTheBelief() {
        val v = DurationView()
        assertEquals(Kind.UNKNOWN, v.at(0, Double.NaN).kind)
        assertEquals(Kind.UNKNOWN, v.at(0, 0.0).kind)
        v.at(0, 65.0); assertEquals(Kind.KNOWN, v.at(2500, 65.0).kind)
        v.reset()
        assertEquals("the next film's number is not believed at once", Kind.UNKNOWN, v.at(2600, 30.0).kind)
    }
}
