package app.kestrel.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/** How a stream's size is worked out before and while it downloads (AO2). */
class SizeEstimateTest {
    @Test fun samplesAreSpreadOverTheWholeTimeline() {
        assertEquals(emptyList<Int>(), SizeEstimate.sampleIndexes(0))
        assertEquals("a short stream is measured part by part", listOf(0, 1, 2), SizeEstimate.sampleIndexes(3))
        assertEquals((0 until 8).toList(), SizeEstimate.sampleIndexes(8))
        assertEquals(listOf(0, 1, 3, 4, 5, 6, 8, 9), SizeEstimate.sampleIndexes(10))
        val long = SizeEstimate.sampleIndexes(1200)
        assertEquals(8, long.size); assertTrue(long.first() in 50..100 && long.last() in 1100..1150)
        assertEquals(long, long.sorted().distinct())
        assertTrue("never the first or the last part of a long stream (logo, short tail)", 0 !in long && 1199 !in long)
    }

    @Test fun theMeasuredRateWeighsEachPartByItsLength() {
        // 6 s part of 600 KB and 2 s part of 100 KB: 700 KB over 8 s, not the plain average of the two rates
        assertEquals(87_500.0, SizeEstimate.measuredRate(listOf(600_000L, 100_000L), listOf(6000L, 2000L)), 0.001)
        assertEquals("a part that could not be measured is left out", 100_000.0, SizeEstimate.measuredRate(listOf(-1L, 200_000L), listOf(4000L, 2000L)), 0.001)
        assertEquals(-1.0, SizeEstimate.measuredRate(listOf(-1L, -1L), listOf(4000L, 4000L)), 0.0)
        assertEquals(-1.0, SizeEstimate.measuredRate(emptyList(), emptyList()), 0.0)
    }

    @Test fun aTransportStreamSavedAsMp4IsALittleSmaller() {
        assertEquals(1_000_000L, SizeEstimate.total(100_000.0, 10_000, "mp4"))
        assertEquals(950_000L, SizeEstimate.total(100_000.0, 10_000, "ts"))
        assertEquals(-1L, SizeEstimate.total(0.0, 10_000, "ts")); assertEquals(-1L, SizeEstimate.total(1.0, 0, "ts"))
    }

    @Test fun theCalibrationSaysHowFarTheDeclaredRateWasOffAndStaysSane() {
        assertEquals(0.7, SizeEstimate.calibration(70_000.0, 100_000), 1e-9)
        assertEquals("unknown on either side: no change", 1.0, SizeEstimate.calibration(-1.0, 100_000), 0.0)
        assertEquals(1.0, SizeEstimate.calibration(50_000.0, 0), 0.0)
        assertEquals("a wildly low reading (a failed measurement) cannot make a size nearly zero", 0.3, SizeEstimate.calibration(1_000.0, 100_000), 1e-9)
        assertEquals(1.25, SizeEstimate.calibration(900_000.0, 100_000), 1e-9)
    }

    @Test fun whileItRunsTheMeasurementTakesOverFromTheFirstGuess() {
        val total = 1_000_000L; val ms = 100_000L
        assertEquals("nothing in yet: the first guess", 900_000L, SizeEstimate.running(900_000, 0, 0, ms))
        assertEquals("no first guess: the measurement at once", 800_000L, SizeEstimate.running(-1, 40_000, 5_000, ms))
        // 6.25 % of the video in (half the share that earns full trust): half way between the guess and the measurement
        assertEquals((900_000 + 800_000) / 2L, SizeEstimate.running(900_000, 50_000, 6_250, ms))
        assertEquals("a quarter in: only the measurement", 800_000L, SizeEstimate.running(1_400_000, 200_000, 25_000, ms))
        assertTrue(total > 0)
    }

    @Test fun theCalibrationOfAQualityThatWasNotMeasuredIsInterpolatedBetweenTheOnesThatWere() {
        val pts = listOf(60_000L to 0.74, 520_000L to 0.66)
        assertEquals("below the lowest measured: its own", 0.74, SizeEstimate.factorFor(30_000, pts), 1e-9)
        assertEquals("above the highest measured: its own", 0.66, SizeEstimate.factorFor(900_000, pts), 1e-9)
        assertEquals(0.74, SizeEstimate.factorFor(60_000, pts), 1e-9); assertEquals(0.66, SizeEstimate.factorFor(520_000, pts), 1e-9)
        val mid = SizeEstimate.factorFor(176_000, pts)                         // the geometric middle of 60 000 and 520 000
        assertEquals(0.70, mid, 0.005)
        assertEquals("one point: that point everywhere", 0.8, SizeEstimate.factorFor(500, listOf(100L to 0.8)), 1e-9)
        assertEquals("nothing measured: no change", 1.0, SizeEstimate.factorFor(500, emptyList()), 0.0)
        assertEquals(1.0, SizeEstimate.factorFor(0, pts), 0.0)
    }

    /**
     * What the owner asked about, in numbers. A synthetic ladder of variable-rate video (parts differ by up to +-35 % around the average, as real encodes do): the playlist declares the PEAK rate, which is
     * 1.35 to 1.5 times the average and grows with the rendition, as on real sites. Before: declared rate x length. After: eight parts measured on the lowest and on the highest quality, the others scaled by
     * the interpolation between them. Errors are against the true size of each file.
     */
    @Test fun measuringEightPartsOfTwoQualitiesBeatsTrustingThePlaylistOnAVariableRateLadder() {
        val rnd = Random(42)
        val seconds = 3600; val partMs = 6000L; val n = seconds / 6
        // (label, true average bytes/s, peak-to-average ratio the encoder declared)
        val ladder = listOf(Triple("360p", 60_000.0, 1.35), Triple("480p", 110_000.0, 1.40), Triple("720p", 250_000.0, 1.45), Triple("1080p", 520_000.0, 1.50))
        val partsOf = ladder.map { (_, avg, _) -> LongArray(n) { (avg * 6 * (1.0 + (rnd.nextDouble() - 0.5) * 0.7)).toLong() } }
        val trueTotal = partsOf.map { it.sum() }
        val declared = ladder.mapIndexed { i, (_, _, peak) -> ((trueTotal[i] / seconds.toDouble()) * peak).toLong() }
        val before = declared.mapIndexed { i, d -> (d * seconds.toLong() - trueTotal[i]).toDouble() / trueTotal[i] }
        val idx = SizeEstimate.sampleIndexes(n)
        fun point(q: Int) = declared[q] to SizeEstimate.calibration(SizeEstimate.measuredRate(idx.map { partsOf[q][it] }, idx.map { partMs }), declared[q])
        val points = listOf(point(0), point(ladder.size - 1))
        val after = declared.mapIndexed { i, d -> (SizeEstimate.calibrated(d, SizeEstimate.factorFor(d, points), seconds * 1000L, "mp4") - trueTotal[i]).toDouble() / trueTotal[i] }
        val line = ladder.indices.joinToString("; ") { "${ladder[it].first}: before ${"%+.0f".format(before[it] * 100)} %, after ${"%+.0f".format(after[it] * 100)} %" }
        println("AO2 synthetic ladder (peak declared 1.35-1.50 x the average, calibrated on 360p and 1080p): $line")
        for (i in ladder.indices) {
            assertTrue("${ladder[i].first} before: ${before[i]}", before[i] > 0.3)       // the declared peak overshoots by a third or more
            assertTrue("${ladder[i].first} after: ${after[i]}", Math.abs(after[i]) < 0.08)
        }
    }
}
