package app.kestrel.core.media

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * RX3-V1 (research/REVIEW-EXTERNAL-3.md part V, research/FINDER_LAB.md section 1): what a page's own <video> element is for. The page sends facts with every metadata report; a hover preview, a muted
 * looping hero video, a GIF-style loop and an ad must not count as "a video the owner may want to save".
 */
class VideoRoleTest {
    private fun facts(muted: Boolean = false, loop: Boolean = false, autoplay: Boolean = false, controls: Boolean = true, visible: Boolean = true, ad: Boolean = false, w: Int = 640, h: Int = 360, vp: Int = 100) =
        VideoFacts(muted, loop, autoplay, controls, visible, ad, w, h, vp)

    @Test fun noFactsMeansAPlayerBecauseNothingIsKnown() {
        assertEquals(VideoRole.PLAYER, VideoRoleRules.decide(null, 25_000))
    }

    @Test fun aNormalPlayerIsAPlayer() {
        assertEquals(VideoRole.PLAYER, VideoRoleRules.decide(facts(), 5_400_000))
        assertEquals(VideoRole.PLAYER, VideoRoleRules.decide(facts(muted = false, autoplay = true, controls = false), 600_000))
    }

    @Test fun aMutedLoopWithoutControlsIsDecoration() {
        assertEquals(VideoRole.DECOR, VideoRoleRules.decide(facts(muted = true, loop = true, controls = false), 120_000))     // a hero loop, however long
        assertEquals(VideoRole.PLAYER, VideoRoleRules.decide(facts(muted = true, loop = true, controls = true), 120_000))     // with controls somebody may use it
        assertEquals(VideoRole.PLAYER, VideoRoleRules.decide(facts(muted = false, loop = true, controls = false), 120_000))   // with sound it is content
    }

    @Test fun aShortMutedAutoplayWithoutControlsIsAPreview() {
        assertEquals(VideoRole.DECOR, VideoRoleRules.decide(facts(muted = true, autoplay = true, controls = false), 30_000))
        assertEquals(VideoRole.PLAYER, VideoRoleRules.decide(facts(muted = true, autoplay = true, controls = false), 30_001))   // over 30 s it may be the film
        assertEquals(VideoRole.PLAYER, VideoRoleRules.decide(facts(muted = true, autoplay = true, controls = true), 20_000))
        assertEquals(VideoRole.PLAYER, VideoRoleRules.decide(facts(muted = true, autoplay = true, controls = false), 0))        // length unknown (a live stream): no verdict from it
    }

    @Test fun aTinyBoxIsDecorationEvenWithControls() {
        assertEquals(VideoRole.DECOR, VideoRoleRules.decide(facts(w = 100, h = 60), 600_000))
        assertEquals(VideoRole.DECOR, VideoRoleRules.decide(facts(w = 159, h = 90), 600_000))
        assertEquals(VideoRole.PLAYER, VideoRoleRules.decide(facts(w = 160, h = 90), 600_000))                                  // exactly 160 x 90 is not under it
        assertEquals(VideoRole.PLAYER, VideoRoleRules.decide(facts(w = 0, h = 0), 600_000))                                      // not laid out: the box says nothing
    }

    @Test fun anAdStateWinsOverEverythingElse() {
        assertEquals(VideoRole.AD, VideoRoleRules.decide(facts(ad = true), 25_000))
        assertEquals(VideoRole.AD, VideoRoleRules.decide(facts(ad = true, muted = true, loop = true, controls = false), 25_000))
        assertEquals(VideoRole.AD, VideoRoleRules.decide(facts(ad = true, w = 100, h = 50), 25_000))
    }

    @Test fun theBitsFromThePageBecomeFacts() {
        val f = VideoFacts.fromBits(VideoFacts.MUTED or VideoFacts.LOOP or VideoFacts.VISIBLE, 300, 169, 80)
        assertEquals(true, f.muted); assertEquals(true, f.loop); assertEquals(false, f.autoplay); assertEquals(false, f.controls); assertEquals(true, f.visible); assertEquals(false, f.ad)
        assertEquals(300, f.widthPx); assertEquals(169, f.heightPx); assertEquals(80, f.viewPercent)
        val all = VideoFacts.fromBits(63, -5, 99999, 400)
        assertEquals(true, all.ad); assertEquals(true, all.controls); assertEquals(true, all.autoplay)
        assertEquals(0, all.widthPx); assertEquals(20000, all.heightPx); assertEquals(100, all.viewPercent)                   // nonsense is clamped, never trusted
    }
}
