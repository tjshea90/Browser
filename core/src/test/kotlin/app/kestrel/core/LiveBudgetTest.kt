package app.kestrel.core

import app.kestrel.core.tabs.LiveBudget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveBudgetTest {
    @Test fun moreFreeMemoryKeepsMoreTabsLive() {
        assertEquals(2, LiveBudget.limit(100, true, false))
        assertEquals(2, LiveBudget.limit(249, true, false))
        assertEquals(3, LiveBudget.limit(250, true, false))
        assertEquals(4, LiveBudget.limit(350, true, false))
        assertEquals(5, LiveBudget.limit(500, true, false))
        assertEquals(6, LiveBudget.limit(700, true, false)) // the owner's usual 800 MB to 1 GB free
        assertEquals(6, LiveBudget.limit(900, true, false))
        assertEquals(7, LiveBudget.limit(1000, false, false))
        assertEquals(8, LiveBudget.limit(5000, false, false))
    }

    @Test fun neverBelowTheFloorAndNeverAboveTheCap() {
        for (mb in listOf(0, 1, 100, 400, 800, 2000, 100000)) {
            val n = LiveBudget.limit(mb, true, false)
            assertTrue("limit($mb) = $n", n in LiveBudget.FLOOR..LiveBudget.MAX)
        }
    }

    @Test fun systemLowMemoryAlwaysWinsEvenOverAFixedSetting() {
        assertEquals(2, LiveBudget.limit(900, true, true))
        assertEquals(2, LiveBudget.limit(900, true, true, setting = 6))
    }

    @Test fun aFixedSettingIsUsedAsIs() {
        assertEquals(6, LiveBudget.limit(100, true, false, setting = 6))
        assertEquals(2, LiveBudget.limit(900, true, false, setting = 2))
        assertEquals(8, LiveBudget.limit(900, true, false, setting = 99)) // capped
        assertEquals(6, LiveBudget.limit(900, true, false, setting = 1))  // 1 is not a valid choice: automatic
    }

    @Test fun unknownMemoryFallsBackToTheOldConstants() {
        assertEquals(2, LiveBudget.limit(-1, true, false))
        assertEquals(4, LiveBudget.limit(-1, false, false))
    }

    @Test fun quickAppSwitchesDoNotCloseTabs() {
        // UI_HIDDEN (20) and RUNNING_LOW (10) are not memory facts on a Go device
        assertEquals(6, LiveBudget.afterTrim(20, 900, 6))
        assertEquals(6, LiveBudget.afterTrim(10, 900, 6))
        assertEquals(6, LiveBudget.afterTrim(20, 100, 6))
    }

    @Test fun pressureShrinksOnlyWhenTheFreeMemoryAgrees() {
        assertEquals(6, LiveBudget.afterTrim(15, 900, 6))
        assertEquals(2, LiveBudget.afterTrim(15, 300, 6))
        assertEquals(6, LiveBudget.afterTrim(40, 800, 6))
        assertEquals(2, LiveBudget.afterTrim(40, 500, 6))
        assertEquals(6, LiveBudget.afterTrim(15, -1, 6)) // unknown: do not guess
    }

    @Test fun aProcessNearKillKeepsOnlyTheTabInFrontWhenTheFreeMemoryReallyIsShort() {
        assertEquals(1, LiveBudget.afterTrim(80, 100, 2))
        assertEquals(1, LiveBudget.afterTrim(60, 250, 6))
        assertEquals(1, LiveBudget.afterTrim(80, -1, 6))          // unknown free memory: the careful answer
    }

    /** PERF15 (owner 2026-11-23: "When i go between tabs it is very laggy and has to reload"): Android's 80 within a second of leaving the app, with 400 MB or more free, must not put the second tab to sleep. */
    @Test fun leavingTheAppWithPlentyOfFreeMemoryKeepsTheTabsAndTheFloorIsKeptInBetween() {
        assertEquals("the real alpha97 numbers: level 80, 402 MB free, Few = 2", 2, LiveBudget.afterTrim(80, 402, 2))
        assertEquals(2, LiveBudget.afterTrim(80, 438, 2))
        assertEquals(2, LiveBudget.afterTrim(80, 540, 2))
        assertEquals("a bigger setting is cut back to the floor between 300 and 450 MB", 2, LiveBudget.afterTrim(80, 400, 6))
        assertEquals("and stands above 450 MB", 6, LiveBudget.afterTrim(80, 500, 6))
        assertEquals(6, LiveBudget.afterTrim(60, 900, 6))
        assertEquals("below 300 MB only the tab in front stays", 1, LiveBudget.afterTrim(80, 299, 2))
    }

    @Test fun trimNeverRaisesTheBudget() {
        assertEquals(2, LiveBudget.afterTrim(15, 100, 2))
        for (level in listOf(5, 10, 15, 20, 40, 60, 80)) for (mb in listOf(-1, 100, 400, 900)) for (b in 2..8)
            assertTrue("afterTrim($level,$mb,$b)", LiveBudget.afterTrim(level, mb, b) <= b)
    }

    // ANS58c + ANS59 (owner messages 58 and 59: "When I logged in, switched to a different app and came back to Kestrel, it reloaded the page and I was logged out" - "It should work for regular tabs in addition to private tabs")
    @Test fun aPrivateTabIsNeverPutToSleepAndARegularTabInFrontNotByAQuickTripToAnotherApp() {
        val b = app.kestrel.core.tabs.LiveBudget
        org.junit.Assert.assertTrue("an old regular tab may sleep by the budget", b.mayHibernate(isCurrent = false, hasLiveChild = false, isPrivate = false))
        org.junit.Assert.assertFalse("the tab in front never (by the budget)", b.mayHibernate(isCurrent = true, hasLiveChild = false, isPrivate = false))
        org.junit.Assert.assertFalse("one with a live pop-up child never", b.mayHibernate(isCurrent = false, hasLiveChild = true, isPrivate = false))
        org.junit.Assert.assertFalse("a private tab never: it would come back logged out", b.mayHibernate(isCurrent = false, hasLiveChild = false, isPrivate = true))
        val half = 10L * 60_000L; val long = 40L * 60_000L
        org.junit.Assert.assertFalse("a private tab in front never, however long the app was away", b.mayHibernateInBackground(isPrivate = true, awayMs = long, level = 80))
        org.junit.Assert.assertFalse("a REGULAR tab in front is not put to sleep by Android's moderate trim after a short trip to another app", b.mayHibernateInBackground(isPrivate = false, awayMs = 20_000L, level = 60))
        org.junit.Assert.assertFalse("not even at the worst trim after a short trip", b.mayHibernateInBackground(isPrivate = false, awayMs = half, level = 80))
        org.junit.Assert.assertFalse("not after a long absence either while Android only says moderate", b.mayHibernateInBackground(isPrivate = false, awayMs = long, level = 60))
        org.junit.Assert.assertTrue("a regular tab after a long absence when Android is about to end background apps: it may go to sleep (its history and cookies are kept)", b.mayHibernateInBackground(isPrivate = false, awayMs = long, level = 80))
    }

    /** PERF24 (owner 2026-11-24: "Switching between tabs has to reload sites and this is slow"): the alpha103 uploads said "tabs hibernate tab=74 live=3 max=2 avail=534MB": the heavy chat tab slept for two private tabs. */
    private fun c(id: Long, last: Long, priv: Boolean = false, cur: Boolean = false, ms: Long = 0L, child: Boolean = false) = LiveBudget.SleepCand(id, last, cur, child, priv, ms)

    @Test fun privateTabsDoNotCountAgainstTheOwnersNumberSoTheHeavyRegularTabStaysAwake() {
        val live = listOf(c(74, 100, ms = 22_000), c(75, 200, priv = true), c(76, 300, priv = true, cur = true))
        // Few (2): one regular tab and two private tabs: nothing sleeps while the memory allows three
        assertEquals(emptyList<Long>(), LiveBudget.sleepPlan(live, totalMax = 5, regularMax = LiveBudget.regularCap(2)))
        // the old rule (all three counted against 2) put tab 74 to sleep
        assertEquals(listOf(74L), LiveBudget.sleepPlan(live, totalMax = 2, regularMax = 2))
    }

    @Test fun theMemoryRuleStillLimitsEveryLiveTabAndASettingNoLongerOverridesIt() {
        assertEquals(5, LiveBudget.memoryLimit(534, true, false))
        assertEquals(3, LiveBudget.memoryLimit(280, true, false))
        assertEquals(2, LiveBudget.memoryLimit(100, true, false))
        assertEquals(2, LiveBudget.memoryLimit(900, true, true))             // the system says low: the floor
        assertEquals(6, LiveBudget.regularCap(6)); assertEquals(2, LiveBudget.regularCap(2)); assertEquals(Int.MAX_VALUE, LiveBudget.regularCap(0))
        val live = (1L..5L).map { c(it, it * 10) }
        assertEquals("Many (6) with 5 regular tabs and little memory: the memory rule wins", listOf(1L, 2L), LiveBudget.sleepPlan(live, totalMax = LiveBudget.memoryLimit(280, true, false), regularMax = LiveBudget.regularCap(6)))
    }

    @Test fun theCheapestTabToBringBackSleepsFirstAndAHeavyPageIsALastResort() {
        val live = listOf(c(1, 10, ms = 22_000), c(2, 20, ms = 3_000), c(3, 30, ms = 1_000), c(4, 40, ms = 9_000), c(5, 50, cur = true))
        assertEquals("cheap tabs first, oldest of them first, heavy ones last", listOf(2L, 3L, 1L), LiveBudget.sleepPlan(live, totalMax = 2, regularMax = 9))
        assertEquals(listOf(2L), LiveBudget.sleepPlan(live, totalMax = 4, regularMax = 9))
        assertEquals("an unknown cost counts as cheap", listOf(7L), LiveBudget.sleepPlan(listOf(c(7, 1, ms = 0), c(8, 2, ms = 20_000), c(9, 3, cur = true)), totalMax = 2, regularMax = 9))
    }

    @Test fun theTabInFrontAPrivateTabAndAnOpenerNeverSleepAndThePlanStopsWhenNobodyIsLeft() {
        val live = listOf(c(1, 10, cur = true), c(2, 20, priv = true), c(3, 30, child = true), c(4, 40, priv = true))
        assertEquals(emptyList<Long>(), LiveBudget.sleepPlan(live, totalMax = 1, regularMax = 1))
        assertEquals(emptyList<Long>(), LiveBudget.sleepPlan(emptyList(), totalMax = 1, regularMax = 1))
    }
}
