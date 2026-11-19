package app.kestrel.core.input.glide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** R24b: the straight-line price follows the key size: tuned on real swipes for phone-sized keys, the earlier value for big (tablet) keys, a straight slide between. */
class GlideParamsPitchTest {
    @Test fun aPhoneKeyboardUsesTheValueTunedOnRealSwipes() {
        assertEquals(1.0f, GlideParams.forPitchDp(30f).shapeW, 1e-6f); assertEquals(5.0f, GlideParams.forPitchDp(30f).visitCap, 1e-6f)
        assertEquals(1.0f, GlideParams.forPitchDp(GlideParams.SMALL_PITCH_DP).shapeW, 1e-6f)
    }

    @Test fun aTabletKeyboardKeepsTheEarlierValue() {
        assertEquals(2.0f, GlideParams.forPitchDp(GlideParams.LARGE_PITCH_DP).shapeW, 1e-6f)
        assertEquals(2.0f, GlideParams.forPitchDp(96f).shapeW, 1e-6f)
        assertEquals("the plain defaults are the earlier values", GlideParams().shapeW, GlideParams.forPitchDp(96f).shapeW, 1e-6f)
        assertEquals(GlideParams().visitCap, GlideParams.forPitchDp(96f).visitCap, 1e-6f)
    }

    @Test fun inBetweenItSlidesWithoutAJump() {
        var last = 1.0f
        var p = GlideParams.SMALL_PITCH_DP
        while (p <= GlideParams.LARGE_PITCH_DP) { val v = GlideParams.forPitchDp(p).shapeW; assertTrue("never down as keys grow: $p", v >= last - 1e-6f); assertTrue(v - last < 0.1f); last = v; p += 1f }
        assertEquals(1.5f, GlideParams.forPitchDp((GlideParams.SMALL_PITCH_DP + GlideParams.LARGE_PITCH_DP) / 2).shapeW, 1e-4f)
    }

    @Test fun nothingElseChanges() {
        val a = GlideParams.forPitchDp(33f); val d = GlideParams()
        assertEquals(d.lmA, a.lmA, 0f); assertEquals(d.endW, a.endW, 0f); assertEquals("R27a: a higher floor for the language weight", 0.45f, a.lmMin, 0f); assertEquals(0.45f, GlideParams.forPitchDp(96f).lmMin, 0f); assertEquals(d.lengthExpect, a.lengthExpect, 0f); assertEquals(d.samples, a.samples)
    }

    @Test fun keysKnowTheirPitchInDpAndDefaultToThePixelPitch() {
        val k = GlideEval.keysFor()
        assertEquals(k.pitch, k.pitchDp, 0f)
        k.pitchDp = 41f; assertEquals(41f, k.pitchDp, 0f)
    }
}
