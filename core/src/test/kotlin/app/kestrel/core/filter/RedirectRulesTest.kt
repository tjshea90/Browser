package app.kestrel.core.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** AW1c: `$redirect=` / `$redirect-rule=` rules: which are understood, what the verdict says, and that the first match keeps deciding the category. */
class RedirectRulesTest {
    private val p = RuleParser()
    private fun set(vararg lines: String): FilterSet { val b = FilterSetBuilder(); b.addAbpList("t", Category.ADS, lines.asSequence()); return b.build(1L) }
    private fun v(fs: FilterSet, url: String, type: Int = RequestType.SCRIPT, page: String? = "page.example") = fs.check(url, page, type)

    @Test fun resourceNamesFoldAndIgnoreThePrioritySuffix() {
        assertEquals(1, RedirectResources.idOf("noopjs")); assertEquals(1, RedirectResources.idOf("noop.js")); assertEquals(1, RedirectResources.idOf("noopjs:10"))
        assertEquals(12, RedirectResources.idOf("google-ima.js:5")); assertEquals(6, RedirectResources.idOf("1X1.GIF"))
        assertEquals(15, RedirectResources.idOf("google-analytics.com/analytics.js"))
        assertEquals(0, RedirectResources.idOf("no-such-resource")); assertEquals(0, RedirectResources.idOf(""))
        for (i in 1 until RedirectResources.FILES.size) assertTrue("every file has a name that finds it: ${RedirectResources.FILES[i]}", RedirectResources.idOf(RedirectResources.FILES[i].lowercase()) != 0 || RedirectResources.FILES[i] == "noop-vast.xml" || RedirectResources.FILES[i] == "noop.json")
        assertTrue(RedirectResources.FILES.size - 1 <= RedirectResources.MAX_ID)
    }

    @Test fun theParserKeepsTheRuleAsABlockAndRemembersTheStandIn() {
        val a = p.parseNetwork("||ads.example/tag.js\$script,redirect=noopjs")!!
        assertEquals(1, a.redirect); assertFalse(a.redirectOnly); assertEquals(RequestType.SCRIPT, a.types)
        val unknown = p.parseNetwork("||ads.example/tag.js\$script,redirect=some-new-thing")!!
        assertEquals("a resource we do not have: still the block the rule was written for", 0, unknown.redirect)
        val only = p.parseNetwork("*\$image,redirect-rule=32x32.png,domain=site.example")!!
        assertEquals(9, only.redirect); assertTrue(only.redirectOnly)
        assertNull("redirect-rule with a resource we do not have changes nothing", p.parseNetwork("*\$image,redirect-rule=some-new-thing,domain=site.example"))
        assertEquals(Skip.UNSUPPORTED_OPTION, p.skip)
        assertNull("an exception that cancels a stand-in is not understood", p.parseNetwork("@@||ads.example/tag.js\$script,redirect=noopjs"))
        assertNull(p.parseNetwork("||ads.example/tag.js\$script,redirect"))
    }

    @Test fun aBlockedRequestIsAnsweredWithTheStandInOfTheRuleThatNamesIt() {
        val fs = set("||ads.example/tag.js\$script,redirect=noopjs", "||pix.example^\$image,redirect=1x1.gif", "||ads.example/other.js\$script")
        val a = v(fs, "https://ads.example/tag.js")
        assertTrue(Verdict.isBlocked(a)); assertEquals(1, Verdict.redirect(a))
        val b = v(fs, "https://pix.example/t.gif?x=1", RequestType.IMAGE)
        assertTrue(Verdict.isBlocked(b)); assertEquals("a pure host rule carries it in its flags", 6, Verdict.redirect(b))
        val c = v(fs, "https://ads.example/other.js")
        assertTrue(Verdict.isBlocked(c)); assertEquals("a plain block has no stand-in", 0, Verdict.redirect(c))
        assertEquals("the stand-in rule is for scripts", Verdict.NONE, v(fs, "https://ads.example/tag.js", RequestType.IMAGE))
    }

    @Test fun aRedirectRuleOnlyChoosesTheStandInForARequestSomethingElseBlocks() {
        val fs = set("||ads.example^", "*\$script,redirect-rule=noopjs,domain=page.example")
        val onPage = v(fs, "https://ads.example/x.js")
        assertTrue(Verdict.isBlocked(onPage)); assertEquals(1, Verdict.redirect(onPage))
        val elsewhere = v(fs, "https://ads.example/x.js", page = "other.example")
        assertTrue(Verdict.isBlocked(elsewhere)); assertEquals(0, Verdict.redirect(elsewhere))
        assertEquals("nothing blocks it: the redirect-rule alone does not", Verdict.NONE, v(fs, "https://fine.example/x.js"))
    }

    @Test fun aStandInIsFoundEvenWhenAPlainRuleBlockedFirst() {
        // the host rule is looked at first and blocks; the pattern rule that names the stand-in still has to be found
        val fs = set("||ads.example^", "||ads.example/player.js\$script,redirect=google-ima.js")
        val r = v(fs, "https://ads.example/player.js")
        assertEquals(12, Verdict.redirect(r)); assertTrue(Verdict.isBlocked(r))
        assertEquals(0, Verdict.redirect(v(fs, "https://ads.example/else.js")))
    }

    @Test fun theCategoryStaysTheFirstMatchesAndImportantAndExceptionsStillWin() {
        val b = FilterSetBuilder()
        b.addAbpLine("||ads.example^", Category.ADS)
        b.addAbpLine("||ads.example/t.js\$script,redirect=noopjs", Category.PRIVACY)
        b.addAbpLine("||trk.example/t.js\$script,redirect=noopjs,important", Category.PRIVACY)
        b.addAbpLine("@@||trk.example/t.js", Category.ADS)
        b.addAbpLine("@@||ads.example/ok.js", Category.ADS)
        b.addAbpLine("||ads.example/ok.js\$script,redirect=noopjs", Category.ADS)
        val fs = b.build(1L)
        val r = v(fs, "https://ads.example/t.js")
        assertEquals("category of the first block, not of the stand-in rule", Category.ADS, Verdict.category(r)); assertEquals(1, Verdict.redirect(r))
        val i = v(fs, "https://trk.example/t.js")
        assertTrue("important beats the exception, and carries its stand-in", (i and Verdict.IMPORTANT) != 0); assertEquals(1, Verdict.redirect(i)); assertEquals(Category.PRIVACY, Verdict.category(i))
        val e = v(fs, "https://ads.example/ok.js")
        assertEquals("an exception allows it: no stand-in either", Verdict.EXCEPTED, e)
    }

    @Test fun theStandInSurvivesSavingAndReading() {
        val fs = set("||ads.example/tag.js\$script,redirect=noopjs", "||pix.example^\$image,redirect=1x1.gif", "*\$script,redirect-rule=google-ima.js,domain=page.example", "||ima.example^")
        val back = FilterSetCodec.read(FilterSetCodec.write(fs))
        for ((url, type) in listOf("https://ads.example/tag.js" to RequestType.SCRIPT, "https://pix.example/a" to RequestType.IMAGE, "https://ima.example/x.js" to RequestType.SCRIPT))
            assertEquals(url, v(fs, url, type), v(back, url, type))
        assertEquals(12, Verdict.redirect(v(back, "https://ima.example/x.js")))
    }

    @Test fun badfilterCancelsAStandInRuleLikeAnyOther() {
        val fs = set("||ads.example/tag.js\$script,redirect=noopjs", "||ads.example/tag.js\$script,redirect=noopjs,badfilter")
        assertEquals(Verdict.NONE, v(fs, "https://ads.example/tag.js"))
    }
}
