package app.kestrel.core.media

import app.kestrel.core.media.QualityLabel.Rung
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** ANS53d: the words of a quality row (resolution, frames a second, bit rate). */
class QualityLabelTest {
    @Test fun thePictureClassIsTheShorterSide() {
        assertEquals(1080, QualityLabel.pClass(1920, 1080)); assertEquals(1080, QualityLabel.pClass(1080, 1920))
        assertEquals(720, QualityLabel.pClass(0, 720)); assertEquals(0, QualityLabel.pClass(0, 0))
    }

    @Test fun bitratesAreReadInTheUnitsAPersonKnows() {
        assertEquals("4.8 Mbit/s", QualityLabel.bitrate(4_800_000L)); assertEquals("2.5 Mbit/s", QualityLabel.bitrate(2_460_000L))
        assertEquals("1.0 Mbit/s", QualityLabel.bitrate(1_000_000L)); assertEquals("850 kbit/s", QualityLabel.bitrate(850_000L))
        assertEquals("", QualityLabel.bitrate(0L)); assertEquals("", QualityLabel.bitrate(-1L))
    }

    @Test fun frameRatesRoundTheWayTheyAreSold() {
        assertEquals("30 fps", QualityLabel.fps(29.97f)); assertEquals("60 fps", QualityLabel.fps(59.94f)); assertEquals("25 fps", QualityLabel.fps(25f))
        assertEquals("", QualityLabel.fps(-1f)); assertEquals("", QualityLabel.fps(0f)); assertEquals("", QualityLabel.fps(9000f))
    }

    @Test fun aRowSaysWhateverIsKnownInOrder() {
        assertEquals("720p  ·  30 fps  ·  2.5 Mbit/s", QualityLabel.text(Rung(1280, 720, 29.97f, 2_500_000L)))
        assertEquals("1080p", QualityLabel.text(Rung(1920, 1080, -1f, 0L)))
        assertEquals("360p  ·  850 kbit/s", QualityLabel.text(Rung(640, 360, -1f, 850_000L)))
        assertEquals("30 fps", QualityLabel.text(Rung(0, 0, 30f, 0L)))
    }

    @Test fun theListHasTheBiggestFirstAndNoTwinRows() {
        val rows = QualityLabel.rows(listOf(
            Rung(640, 360, 30f, 1_000_000L), Rung(1920, 1080, 30f, 6_000_000L), Rung(1280, 720, 60f, 5_000_000L), Rung(1280, 720, 30f, 3_000_000L),
            Rung(1280, 720, 30f, 3_000_000L), Rung(0, 0, 0f, 100_000L)))
        assertEquals(listOf("1080p  ·  30 fps  ·  6.0 Mbit/s", "720p  ·  60 fps  ·  5.0 Mbit/s", "720p  ·  30 fps  ·  3.0 Mbit/s", "360p  ·  30 fps  ·  1.0 Mbit/s"), rows.map { QualityLabel.text(it) })
    }

    @Test fun theAutomaticRowSaysWhatPlaysNow() {
        assertEquals("now 720p  ·  30 fps", QualityLabel.now(Rung(1280, 720, 30f, 0L)))
        assertEquals("", QualityLabel.now(null)); assertEquals("", QualityLabel.now(Rung(0, 0, 0f, 0L)))
    }

    @Test fun aPickTravelsAsTextAndComesBackTheSame() {
        val r = Rung(1280, 720, 29.97f, 2_500_000L)
        val back = QualityLabel.decode(QualityLabel.encode(r))!!
        assertEquals(1280, back.w); assertEquals(720, back.h); assertEquals(2_500_000L, back.bps); assertEquals(29.97f, back.fps, 0.01f)
        val none = QualityLabel.decode(QualityLabel.encode(Rung(640, 360, -1f, 0L)))!!
        assertEquals(360, none.h); assertEquals(0L, none.bps); assertTrue("no rate stays no rate", none.fps <= 0f)
    }

    @Test fun textFromOutsideIsNeverTrusted() {
        for (bad in listOf<String?>(null, "", "1,2,3", "a,b,c,d", "1280,720,30000", "1280,720,30000,5,6", "-1,720,0,0", "99999,720,0,0", "1280,720,-5,0", "1280,720,0,-9", "1280,720,2000000,0"))
            assertNull("'$bad'", QualityLabel.decode(bad))
    }

    @Test fun theNearestPictureIsTheSameClassFirstThenTheFrameRateThenTheBitRate() {
        val offered = listOf(Rung(1920, 1080, 30f, 6_000_000L), Rung(1280, 720, 60f, 5_000_000L), Rung(1280, 720, 30f, 3_000_000L), Rung(1280, 720, 30f, 2_000_000L), Rung(640, 360, 30f, 1_000_000L))
        assertEquals("exactly it", 2, QualityLabel.nearest(Rung(1280, 720, 30f, 3_000_000L), offered))
        assertEquals("the frame rate decides inside a class", 1, QualityLabel.nearest(Rung(1280, 720, 60f, 0L), offered))
        assertEquals("then the bit rate", 3, QualityLabel.nearest(Rung(1280, 720, 30f, 2_100_000L), offered))
        assertEquals("a class the stream does not have: the biggest one below", 4, QualityLabel.nearest(Rung(854, 480, 30f, 1_500_000L), offered))
        assertEquals("below everything: the smallest one above", 4, QualityLabel.nearest(Rung(320, 180, 30f, 0L), offered))
        assertEquals("a portrait pick finds the same class", 0, QualityLabel.nearest(Rung(1080, 1920, 30f, 0L), offered))
        assertEquals("a rate nobody says is no difference", 2, QualityLabel.nearest(Rung(1280, 720, -1f, 3_000_000L), listOf(Rung(1920, 1080, 30f, 0L), Rung(640, 360, 30f, 0L), Rung(1280, 720, 30f, 2_900_000L))))
        assertEquals("nothing offered", -1, QualityLabel.nearest(Rung(1280, 720, 30f, 0L), emptyList()))
        assertEquals("nothing with a picture", -1, QualityLabel.nearest(Rung(1280, 720, 30f, 0L), listOf(Rung(0, 0, 0f, 100_000L))))
    }
}
