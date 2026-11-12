package app.kestrel.core.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * AA3-3b on the REAL lists: uBlock Origin's "URL Tracking Protection" list (AdGuard/uBO, 2,700+ `$removeparam` rules) and the rules inside uBO's Privacy list.
 * Needs the local uAssets checkout the list script uses (a sibling of the repository: ../ublockorigin/uassets); skipped when it is not there (CI has none).
 */
class RealQueryRulesTest {
    private fun uassets(): File? {
        var d: File? = File("").absoluteFile
        while (d != null) { val f = File(d, "ublockorigin/uassets/filters"); if (f.isDirectory) return f; d = d.parentFile }
        return null
    }

    private fun build(dir: File): Pair<FilterSet, BuildReport> {
        val b = FilterSetBuilder()
        for (name in listOf("privacy-removeparam.txt", "privacy.txt")) {
            val f = File(dir, name)
            if (f.isFile) b.addAbpList(name, Category.PRIVACY, f.readLines(Charsets.UTF_8).asSequence())
        }
        return Pair(b.build(1L), b.report)
    }

    @Test fun theRealRulesAreMostlyUnderstoodAndStripTheNamesPeopleKnow() {
        val dir = uassets()
        assumeTrue("uAssets checkout not found", dir != null)
        val (fs, report) = build(dir!!)
        println("QUERY real: rules=" + fs.queryRuleCount + " report=" + report)
        assertTrue("about 2,300 of the ~2,900 rules are plain names (got ${fs.queryRuleCount})", fs.queryRuleCount in 1800..4000)

        // generic names
        assertEquals("https://example.com/p?a=1", fs.stripQuery("https://example.com/p?gclid=Cj0&a=1&fbclid=IwAR&msclkid=9&mc_eid=1"))
        assertEquals("https://example.com/", fs.stripQuery("https://example.com/?utm_source=a&utm_medium=b&utm_campaign=c&utm_term=d&utm_content=e"))
        // generic names that are only for some sites
        assertEquals("https://www.instagram.com/p/x/", fs.stripQuery("https://www.instagram.com/p/x/?igshid=abc"))
        // host rules and path rules
        assertEquals("https://youtu.be/abc", fs.stripQuery("https://youtu.be/abc?is=ZZ"))
        assertEquals("https://www.youtube.com/shorts/abc", fs.stripQuery("https://www.youtube.com/shorts/abc?si=ZZ"))
        assertEquals("https://www.instagram.com/p/x/?hl=en", fs.stripQuery("https://www.instagram.com/p/x/?click_source=q&hl=en"))
        // things a navigation needs are never touched
        assertNull(fs.stripQuery("https://www.google.com/search?q=cats&hl=en"))
        assertNull(fs.stripQuery("https://www.youtube.com/watch?v=abc&list=PL1&t=30"))
        assertNull(fs.stripQuery("https://accounts.example/oauth?client_id=1&state=2&code=3&redirect_uri=https%3A%2F%2Fa.example"))
    }

    @Test fun noGenericRuleOfTheRealListsNamesAParameterSitesNeed() {
        val dir = uassets()
        assumeTrue("uAssets checkout not found", dir != null)
        val (fs, _) = build(dir!!)
        // none of the names every site needs may be removed on an arbitrary site
        for (n in QueryRules.PROTECTED) assertNull("a generic rule for '$n'", fs.stripQuery("https://arbitrary-site.example/p?$n=1"))
    }

    @Test fun compilingAndReadingTheRealRulesIsFast() {
        val dir = uassets()
        assumeTrue("uAssets checkout not found", dir != null)
        val (fs, _) = build(dir!!)
        val t0 = System.nanoTime()
        val back = FilterSetCodec.read(FilterSetCodec.write(fs))
        back.warmQuery()
        val ms = (System.nanoTime() - t0) / 1_000_000
        println("QUERY real: write+read+warm = $ms ms, pool = ${fs.query.pool().size / 1024} KB")
        assertEquals(fs.queryRuleCount, back.queryRuleCount)
        assertTrue("the table must be quick to build (ms): $ms", ms < 3000)
        val t1 = System.nanoTime()
        repeat(2000) { back.stripQuery("https://www.example.com/some/path?a=1&b=2&gclid=zz&c=3") }
        val perUs = (System.nanoTime() - t1) / 1000 / 2000
        println("QUERY real: strip = $perUs us per address")
        assertTrue("an address must cost microseconds, not milliseconds ($perUs us)", perUs < 500)
    }

    @Test fun printWhatWasLeftOut() {
        val dir = uassets()
        assumeTrue("uAssets checkout not found", dir != null)
        val skipped = HashMap<Skip, Int>()
        val sample = HashMap<Skip, ArrayList<String>>()
        for (name in listOf("privacy-removeparam.txt", "privacy.txt")) {
            for (line in File(dir, name).readLines(Charsets.UTF_8)) {
                val t = line.trim()
                if (t.isEmpty() || t.startsWith("!") || RuleParser.looksCosmetic(t) || !QueryRuleParser.looks(t)) continue
                val r = QueryRuleParser.parse(t)
                if (r.rule == null) { skipped.merge(r.skip, 1, Int::plus); val l = sample.getOrPut(r.skip) { ArrayList() }; if (l.size < 40) l.add(t.take(110)) }
            }
        }
        println("QUERY left out: $skipped")
        for ((k, v) in sample) println("QUERY left out $k e.g. " + v.joinToString("  ||  "))
    }
}
