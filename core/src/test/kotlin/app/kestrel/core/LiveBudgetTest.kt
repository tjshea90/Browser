package app.kestrel.core

import app.kestrel.core.tabs.LiveBudget
import kotlin.test.Test
import kotlin.test.assertEquals

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
            assert(n in LiveBudget.FLOOR..LiveBudget.MAX) { "limit($mb) = $n" }
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

    @Test fun aProcessNearKillKeepsOnlyTheTabInFront() {
        assertEquals(1, LiveBudget.afterTrim(60, 900, 6))
        assertEquals(1, LiveBudget.afterTrim(80, 100, 2))
    }

    @Test fun trimNeverRaisesTheBudget() {
        assertEquals(2, LiveBudget.afterTrim(15, 100, 2))
        for (level in listOf(5, 10, 15, 20, 40, 60, 80)) for (mb in listOf(-1, 100, 400, 900)) for (b in 2..8)
            assert(LiveBudget.afterTrim(level, mb, b) <= b) { "afterTrim($level,$mb,$b)" }
    }
}
