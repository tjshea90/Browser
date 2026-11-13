package app.kestrel.core.diag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageReportsTest {
    private fun rep(page: String, at: Long = 1000, failed: List<String> = emptyList(), errors: List<String> = emptyList(), blocked: List<String> = emptyList(), blockedCount: Int = blocked.size) =
        PageReport(page, 32, blockedCount, blocked, failed, errors, at)

    @Test fun anEmptyHolderSaysWhatToDo() {
        val t = PageReports().text(5000)
        assertTrue(t.contains("nothing yet")); assertTrue(t.contains("Open a page"))
    }

    @Test fun aFailedLoadAndAScriptErrorAreNamedPlainly() {
        val r = PageReports()
        r.add(rep("https://doraby.com/", failed = listOf("script cdn.jsdelivr.net/npm/swiper@5.4.4/js/swiper.min.js?.. CONNECTION_RESET", "stylesheet doraby.com/x.css HTTP 404"), errors = listOf("ReferenceError: Swiper is not defined @ doraby.com/script.js:1"), blocked = listOf("script googletagmanager.com/gtag/js?..")))
        val t = r.text(13000)
        assertTrue(t.contains("doraby.com/ (12 s ago): 32 requests, 1 blocked by Shields, 2 FAILED, 1 script error"))
        assertTrue(t.contains("FAILED: script cdn.jsdelivr.net/npm/swiper@5.4.4/js/swiper.min.js?.. CONNECTION_RESET; stylesheet doraby.com/x.css HTTP 404"))
        assertTrue(t.contains("script errors: ReferenceError: Swiper is not defined"))
        assertTrue(t.contains("blocked: script googletagmanager.com/gtag/js?.."))
    }

    @Test fun aPageWithNothingWrongSaysSo() {
        val r = PageReports(); r.add(rep("http://example.org/"))
        val t = r.text(2000)
        assertTrue(t.contains("no failed loads")); assertTrue(t.contains("no script errors")); assertFalse(t.contains("FAILED:"))
    }

    @Test fun aSecondReportOfTheSamePageReplacesTheFirstAndTheOldestPageFallsOff() {
        val r = PageReports(max = 3)
        r.add(rep("https://a.example/", failed = listOf("image a")))
        r.add(rep("https://b.example/")); r.add(rep("https://c.example/"))
        r.add(rep("https://a.example/", failed = emptyList(), at = 2000))
        assertEquals(listOf("https://a.example/", "https://c.example/", "https://b.example/"), r.snapshot().map { it.page })
        assertTrue(r.snapshot().first().failed.isEmpty())
        r.add(rep("https://d.example/"))
        assertEquals(3, r.snapshot().size); assertFalse(r.snapshot().any { it.page == "https://b.example/" })
    }

    @Test fun longListsAreCappedAndSayHowManyMoreWereBlocked() {
        val r = PageReports()
        val many = (1..30).map { "script t$it.example/x.js" }
        r.add(rep("https://p.example/", blocked = many, failed = many, errors = many))
        val t = r.text(1000)
        assertTrue(t.contains("... 20 more")); assertTrue(t.contains("t10.example")); assertFalse(t.contains("t11.example/x.js; "))
    }
}
