package app.kestrel.browser

import android.os.Process
import app.kestrel.browser.download.DownloadController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** EX-9 (research/REVIEW-EXTERNAL-1.md 6.2): download threads stay out of Android's background scheduling group, which starts at nice 10, while the browser is on screen. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class DownloadThreadPriorityTest {
    @Test fun withTheBrowserOnScreenTheThreadsRunAtNice9NotInTheBackgroundGroup() {
        val p = DownloadController.priorityFor(true)
        assertEquals(9, p)
        assertTrue("nice 10 or more lands in the background group", p < Process.THREAD_PRIORITY_BACKGROUND)
        assertTrue("but still yields to the UI (normal priority is 0)", p > Process.THREAD_PRIORITY_DEFAULT)
    }

    @Test fun withTheScreenOffTheyRunAtNormalPriority() {
        assertEquals(Process.THREAD_PRIORITY_DEFAULT, DownloadController.priorityFor(false))
    }
}
