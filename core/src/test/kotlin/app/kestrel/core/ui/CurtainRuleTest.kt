package app.kestrel.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurtainRuleTest {
    @Test fun aSessionThatHasNotPaintedYetAlwaysGetsTheCurtain() {
        assertEquals(CurtainRule.Why.UNPAINTED, CurtainRule.needed(newPainted = false, prevPrivate = false, newPrivate = true))   // the owner's case: a new private tab over a regular tab's last frame
        assertEquals(CurtainRule.Why.UNPAINTED, CurtainRule.needed(false, true, false))
        assertEquals(CurtainRule.Why.UNPAINTED, CurtainRule.needed(false, null, false))
        assertEquals(CurtainRule.Why.UNPAINTED, CurtainRule.needed(false, false, false))
    }

    @Test fun aLiveTabComingBackNeedsNoneUnlessItIsOfTheOtherKind() {
        assertNull(CurtainRule.needed(true, false, false)); assertNull(CurtainRule.needed(true, true, true)); assertNull(CurtainRule.needed(true, null, true))
        assertEquals("a private tab's last frame must never be seen on a regular tab", CurtainRule.Why.KIND_CHANGED, CurtainRule.needed(true, true, false))
        assertEquals(CurtainRule.Why.KIND_CHANGED, CurtainRule.needed(true, false, true))
    }

    @Test fun anUnpaintedSessionWaitsForItsFirstPaintALiveOneOnlyForItsFirstFrame() {
        assertTrue(CurtainRule.untilPaint(CurtainRule.Why.UNPAINTED)); assertFalse(CurtainRule.untilPaint(CurtainRule.Why.KIND_CHANGED))
        assertTrue(CurtainRule.timeoutMs(CurtainRule.Why.UNPAINTED) > CurtainRule.timeoutMs(CurtainRule.Why.KIND_CHANGED))
    }
}
