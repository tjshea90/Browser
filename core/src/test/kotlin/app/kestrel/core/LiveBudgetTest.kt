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

    @Test fun aProcessNearKillKeepsOnlyTheTabInFront() {
        assertEquals(1, LiveBudget.afterTrim(60, 900, 6))
        assertEquals(1, LiveBudget.afterTrim(80, 100, 2))
    }

    @Test fun trimNeverRaisesTheBudget() {
        assertEquals(2, LiveBudget.afterTrim(15, 100, 2))
        for (level in listOf(5, 10, 15, 20, 40, 60, 80)) for (mb in listOf(-1, 100, 400, 900)) for (b in 2..8)
            assertTrue("afterTrim($level,$mb,$b)", LiveBudget.afterTrim(level, mb, b) <= b)
    }

    // ANS58c (owner message 58: "When I logged in, switched to a different app and came back to Kestrel, it reloaded the page and I was logged out", in a private tab): the engine forgets private logins when its last private session closes
    @Test fun aPrivateTabIsNeverPutToSleepAndTheTabInFrontOnlyWhenItIsNormal() {
        org.junit.Assert.assertTrue("an old normal tab may sleep", app.kestrel.core.tabs.LiveBudget.mayHibernate(isCurrent = false, hasLiveChild = false, isPrivate = false))
        org.junit.Assert.assertFalse("the tab in front never (by the budget)", app.kestrel.core.tabs.LiveBudget.mayHibernate(isCurrent = true, hasLiveChild = false, isPrivate = false))
        org.junit.Assert.assertFalse("one with a live pop-up child never", app.kestrel.core.tabs.LiveBudget.mayHibernate(isCurrent = false, hasLiveChild = true, isPrivate = false))
        org.junit.Assert.assertFalse("a private tab never: it would come back logged out", app.kestrel.core.tabs.LiveBudget.mayHibernate(isCurrent = false, hasLiveChild = false, isPrivate = true))
        org.junit.Assert.assertTrue("the app was left and Android wants memory: a normal tab in front may sleep (its history and cookies are kept)", app.kestrel.core.tabs.LiveBudget.mayHibernateInBackground(isPrivate = false))
        org.junit.Assert.assertFalse("a private tab in front may not (the owner was logged out by exactly this)", app.kestrel.core.tabs.LiveBudget.mayHibernateInBackground(isPrivate = true))
    }
}
