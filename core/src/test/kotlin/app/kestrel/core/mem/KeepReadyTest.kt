package app.kestrel.core.mem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeepReadyTest {
    @Test fun theChoicesMeanWhatSettingsSays() {
        assertEquals(0, KeepReady.minutes(KeepReady.OFF)); assertEquals(15, KeepReady.minutes(KeepReady.QUARTER)); assertEquals(60, KeepReady.minutes(KeepReady.HOUR)); assertEquals(180, KeepReady.minutes(KeepReady.LONG))
        assertEquals(60 * 60_000L, KeepReady.millis(KeepReady.HOUR))
        assertEquals("a saved number from a newer or broken backup stays inside the choices", 180, KeepReady.minutes(99)); assertEquals(0, KeepReady.minutes(-4))
        assertEquals("off", KeepReady.words(0)); assertEquals("15 minutes", KeepReady.words(1)); assertEquals("1 hour", KeepReady.words(2)); assertEquals("3 hours", KeepReady.words(3))
    }

    @Test fun oneHourIsTheDefaultBecauseTheOwnersKillsCame15To69MinutesAfterLeaving() {
        assertEquals(KeepReady.HOUR, KeepReady.DEFAULT)
        for (afterLeaving in listOf(15, 33, 44, 47, 62, 69)) if (afterLeaving < 60) assertFalse("a return within the hour finds the page: $afterLeaving min", KeepReady.expired(KeepReady.DEFAULT, afterLeaving * 60_000L))
        assertTrue(KeepReady.expired(KeepReady.DEFAULT, 60 * 60_000L)); assertFalse(KeepReady.expired(KeepReady.OFF, 10 * 60 * 60_000L))
    }

    @Test fun itHoldsOnlyForARealPageWhenMemoryIsNotShortAndTheScreenIsNotClosing() {
        assertTrue(KeepReady.applies(KeepReady.HOUR, hasPage = true, lowMemory = false, finishing = false))
        assertFalse("off", KeepReady.applies(KeepReady.OFF, true, false, false))
        assertFalse("nothing to keep on the Home screen", KeepReady.applies(KeepReady.HOUR, false, false, false))
        assertFalse("the system itself says memory is short: the tablet needs the room more", KeepReady.applies(KeepReady.HOUR, true, true, false))
        assertFalse("the person closed the screen", KeepReady.applies(KeepReady.HOUR, true, false, true))
    }

    @Test fun whyNotNamesTheFirstReasonAndIsEmptyWhenItHolds() {
        assertEquals("", KeepReady.whyNot(2, true, false, false))
        assertEquals("off", KeepReady.whyNot(0, true, false, false)); assertEquals("closing", KeepReady.whyNot(2, true, false, true))
        assertEquals("no page", KeepReady.whyNot(2, false, false, false)); assertEquals("memory short", KeepReady.whyNot(2, true, true, false))
    }
}
