package app.kestrel.core.download

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** DM1: only a real tap on a download notification opens the Downloads list; the replay of its intent after a restart does not. */
class ManagerIntentTest {
    @Test fun aTapOnTheNotificationOpensTheList() = assertTrue(ManagerIntent.shouldOpen(extra = true, restoredState = false, fromHistory = false))
    @Test fun anIntentWithoutTheFlagNeverOpensIt() = assertFalse(ManagerIntent.shouldOpen(extra = false, restoredState = false, fromHistory = false))
    @Test fun aRestartFromSavedStateDoesNotOpenIt() = assertFalse(ManagerIntent.shouldOpen(extra = true, restoredState = true, fromHistory = false))
    @Test fun aLaunchFromRecentsDoesNotOpenIt() = assertFalse(ManagerIntent.shouldOpen(extra = true, restoredState = false, fromHistory = true))
}
