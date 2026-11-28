package app.kestrel.core.download

import app.kestrel.core.download.DownloadGuardRules.Input
import app.kestrel.core.download.DownloadGuardRules.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadGuardRulesTest {
    private fun d(touch: Long = 1_000, nav: Boolean = false, risky: Boolean = false, big: Boolean = false, clear: Boolean = false, blob: Boolean = false, own: Boolean = false, declined: Long = Long.MAX_VALUE) =
        DownloadGuardRules.decide(Input(touch, nav, risky, big, clear, blob, own, declined))

    @Test fun aDownloadRightAfterATapGoesAheadAsBefore() { assertEquals(Verdict.GO, d(touch = 1_000)) }
    @Test fun theEdgeOfTheWindowStillCountsAsATap() { assertEquals(Verdict.GO, d(touch = DownloadGuardRules.TOUCH_WINDOW_MS)); assertEquals(Verdict.ASK, d(touch = DownloadGuardRules.TOUCH_WINDOW_MS + 1)) }
    @Test fun aDownloadNobodyTappedAsks() { assertEquals(Verdict.ASK, d(touch = 30_000)) }
    @Test fun aDownloadWithNoTouchAtAllAsks() { assertEquals(Verdict.ASK, d(touch = Long.MAX_VALUE)) }
    @Test fun anAddressTheOwnerAskedForNeverCountsAsByItself() { assertEquals(Verdict.GO, d(touch = Long.MAX_VALUE, nav = true)) }
    @Test fun theOwnersOwnButtonsNeverAsk() { assertEquals(Verdict.GO, d(touch = Long.MAX_VALUE, own = true)) }
    @Test fun riskyBigAndPlainHttpStillAskAfterATap() {
        assertEquals(Verdict.ASK, d(risky = true)); assertEquals(Verdict.ASK, d(big = true)); assertEquals(Verdict.ASK, d(clear = true))
    }
    @Test fun aCancelSilencesTheNextSelfStartedDownloadsForAMinute() {
        assertEquals(Verdict.REFUSE_QUIETLY, d(touch = 30_000, declined = 5_000))
        assertEquals(Verdict.REFUSE_QUIETLY, d(touch = 30_000, declined = DownloadGuardRules.REPEAT_WINDOW_MS - 1))
        assertEquals(Verdict.ASK, d(touch = 30_000, declined = DownloadGuardRules.REPEAT_WINDOW_MS))
    }
    @Test fun aCancelDoesNotBlockADownloadThePersonStartsNext() { assertEquals(Verdict.GO, d(touch = 1_000, declined = 5_000)) }
    private fun nav(touch: Long, toStart: Long, itself: Boolean) = DownloadGuardRules.decide(Input(touch, false, false, false, false, touchToPageStartMs = toStart, pageItself = itself))

    @Test fun aTapThatOnlyOpenedThePageDoesNotCoverWhatThePageStartsLater() {
        // the person tapped a link on page A 5 s ago; page B began 0.4 s after the tap; B starts a file by itself (not B's own address)
        assertEquals(Verdict.ASK, nav(5_000, 400, false))
    }
    @Test fun aTapInsideThePageAfterItBeganCounts() { assertEquals(Verdict.GO, nav(2_000, -3_000, false)) }
    @Test fun aLinkToAFileWhoseNavigationBeganRightAfterTheTapCounts() { assertEquals(Verdict.GO, nav(1_000, 300, true)) }
    @Test fun aTimerThatSetsTheAddressToAFileLongAfterTheTapDoesNot() { assertEquals(Verdict.ASK, nav(6_000, 2_600, true)) }
    @Test fun anUnknownPageStartKeepsTheSimpleRule() { assertEquals(Verdict.GO, d(touch = 1_000)); assertEquals(Verdict.ASK, d(touch = 30_000)) }

    @Test fun theQuestionSaysWhyOnlyWhenThePageStartedItAlone() {
        assertEquals(DownloadGuardRules.BY_ITSELF, DownloadGuardRules.note(Input(30_000, false, false, false, false)))
        assertEquals("", DownloadGuardRules.note(Input(1_000, false, false, false, false)))
        assertTrue(DownloadGuardRules.note(Input(30_000, false, false, false, false, fromBlob = true)).endsWith(DownloadGuardRules.BLOB_NOTE))
        assertFalse(DownloadGuardRules.note(Input(1_000, false, false, false, false, fromBlob = true)).contains("by itself"))
    }
}
