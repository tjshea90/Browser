package app.kestrel.core.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** AW1b: which `+js(...)` lines are understood, how a call is stored, filed for its sites (entity domains, exceptions, exclusions) and sent to the page. */
class ScriptletRulesTest {
    private fun enc(inner: String) = ScriptletRules.encode(inner)
    private fun call(inner: String) = ScriptletRules.decode(enc(inner)!!)!!
    private fun set(vararg lines: String): FilterSet { val b = FilterSetBuilder(); b.addAbpList("t", Category.ADS, lines.asSequence()); return b.build(1L) }
    private fun names(fs: FilterSet, host: String) = fs.scriptletsFor(host).map { ScriptletRules.decode(it)!!.name }

    @Test fun namesAndAliasesFoldToOneCanonicalName() {
        assertEquals("set", call("set-constant, a.b, true").name)
        assertEquals("set", call("set.js, a.b, true").name)
        assertEquals("acs", call("abort-current-inline-script, x").name)
        assertEquals("acs", call("ACIS, x").name)
        assertEquals("aeld", call("addEventListener-defuser, click, popup").name)
        assertEquals("nostif", call("setTimeout-defuser, ads").name)
        assertEquals("nowoif", call("window.open-defuser").name)
        assertEquals("nano-sib", call("adjust-setInterval, count").name)
        assertEquals("rmnt", call("remove-node-text, script, ads").name)
        assertNull("a scriptlet we do not have is not half applied", enc("trusted-click-element, button"))
        assertNull(enc("no-such-thing, x"))
        assertNull(enc(""))
    }

    @Test fun argumentsFollowTheListsOwnSyntax() {
        assertEquals(listOf("aeld", "", "_0x"), ScriptletRules.splitArgs("aeld, , _0x"))
        assertEquals(listOf("rpnt", "script", "a, b", "c"), ScriptletRules.splitArgs("rpnt, script, 'a, b', c"))
        assertEquals(listOf("rpnt", "script", "say \"hi\", ok"), ScriptletRules.splitArgs("rpnt, script, \"say \\\"hi\\\", ok\""))
        assertEquals(listOf("x", "a,b"), ScriptletRules.splitArgs("x, a\\,b"))
        assertEquals(listOf("x", "/a\\d+/", "b"), ScriptletRules.splitArgs("x,  /a\\d+/ ,b"))
        assertEquals("a quote that is not an argument's wrapper is just text", listOf("x", "it's", "y"), ScriptletRules.splitArgs("x, it's, y"))
        assertEquals(listOf("set", "a", "true", ""), ScriptletRules.splitArgs("set, a, true,"))
        assertEquals(listOf("a.b", "c d"), call("set, a.b, 'c d'").args)
        assertEquals("trailing empty arguments carry nothing", listOf("click"), call("aeld, click, ").args)
        assertEquals(listOf("", "_0x"), call("aeld, , _0x").args)
    }

    @Test fun aCallThatWouldActOnEverythingMustNameSomething() {
        for (bad in listOf("aeld", "aeld, ,", "nostif", "nosiif, ,", "no-fetch-if", "no-xhr-if", "set", "set, a.b", "aopr", "aopw", "acs", "aost", "noeval-if", "json-prune", "ra", "rc", "rmnt", "rmnt, script", "set-cookie, a"))
            assertNull("must be refused: $bad", enc(bad))
        for (good in listOf("nowoif", "nowoif, _blank", "noeval", "nowebrtc", "aeld, click", "aeld, , x", "nostif, , 5000", "nano-stb", "nano-sib, , 6000", "set, a.b, true", "aopr, a", "json-prune, a b"))
            assertNotNull("must be accepted: $good", enc(good))
    }

    @Test fun limitsAndControlCharactersAreRefused() {
        assertNull(enc("set, a, " + "x".repeat(501)))
        assertNull(enc("aopr, " + (1..11).joinToString(",") { "a$it" }))
        assertNull(enc("aopr, a\u0001b"))
        assertNull(enc("aopr, a\u0006b"))
        assertNotNull(enc("aopr, " + (1..10).joinToString(",") { "a$it" }))
    }

    @Test fun aRuleIsFiledForItsDomainsAndAppliesToTheirSubdomains() {
        val fs = set("a.example.com,b.example.org##+js(aopr, adsOn)", "example.com##+js(nowoif)")
        assertEquals(listOf("nowoif", "aopr"), names(fs, "a.example.com").sortedByDescending { it == "nowoif" })
        assertEquals(setOf("aopr", "nowoif"), names(fs, "www.a.example.com").toSet())
        assertEquals(listOf("nowoif"), names(fs, "example.com"))
        assertEquals(listOf("nowoif"), names(fs, "sub.example.com"))
        assertEquals(listOf("aopr"), names(fs, "b.example.org"))
        assertTrue(names(fs, "example.net").isEmpty())
        assertTrue("a rule for example.com is not a rule for notexample.com", names(fs, "notexample.com").isEmpty())
    }

    @Test fun exceptionsAndExclusionsTurnACallOffForASite() {
        val fs = set(
            "example.com##+js(aopr, a)", "example.com##+js(aopr, b)", "sub.example.com#@#+js(aopr, a)",
            "other.com##+js(nowoif)", "other.com##+js(aopr, c)", "quiet.other.com#@#+js()",
            "wide.org,~not.wide.org##+js(nostif, x)",
        )
        assertEquals(listOf("a", "b"), fs.scriptletsFor("example.com").map { ScriptletRules.decode(it)!!.args[0] })
        assertEquals("one call off on the subdomain", listOf("b"), fs.scriptletsFor("sub.example.com").map { ScriptletRules.decode(it)!!.args[0] })
        assertEquals(2, fs.scriptletsFor("other.com").size)
        assertTrue("#@#+js() turns them all off", fs.scriptletsFor("quiet.other.com").isEmpty())
        assertEquals(1, fs.scriptletsFor("wide.org").size)
        assertTrue("~site means off there", fs.scriptletsFor("not.wide.org").isEmpty())
    }

    @Test fun entityDomainsMatchTheNameWhateverThePublicSuffix() {
        val fs = set("lookcam.*##+js(aopr, a)", "www.movie.*,tube.*##+js(nowoif)")
        for (h in listOf("lookcam.com", "www.lookcam.co.uk", "lookcam.tv", "a.b.lookcam.org")) assertEquals("for $h", listOf("aopr"), names(fs, h))
        assertTrue(names(fs, "notlookcam.com").isEmpty())
        assertTrue("another site that merely contains the name before ITS suffix", names(fs, "lookcam.evil.com").isEmpty())
        assertTrue(names(fs, "co.uk").isEmpty())
        assertEquals(listOf("nowoif"), names(fs, "www.movie.to"))
        assertTrue("www.movie.* is not movie.to", names(fs, "movie.to").isEmpty())
        assertEquals(listOf("nowoif"), names(fs, "tube.de"))
        assertTrue("an address has no public suffix", names(fs, "10.0.2.2").isEmpty())
        // the same keys serve ordinary element hiding
        val css = set("lookcam.*##.banner", "example.com##.x").cosmeticCss("www.lookcam.co.uk")
        assertEquals(".banner{display:none!important}\n", css)
    }

    @Test fun thePayloadIsAsciiJsonThatCanNeverEndTheComment() {
        val calls = listOf(enc("rpnt, script, /a*/g, 'x*/y'")!!, enc("set, a.b, \"café\"")!!)
        val json = ScriptletRules.payload(calls)
        assertEquals("[[\"rpnt\",\"script\",\"/a\\u002a/g\",\"x\\u002a/y\"],[\"set\",\"a.b\",\"caf\\u00e9\"]]", json)
        assertFalse(json.contains("*/") || json.contains('*'))
        assertTrue(json.all { it.code in 0x20..0x7e })
        assertEquals("", ScriptletRules.payload(emptyList()))
    }

    @Test fun thePayloadIsBounded() {
        val many = (1..100).map { enc("aopr, prop$it")!! }
        val j = ScriptletRules.payload(many)
        assertEquals(ScriptletRules.MAX_CALLS, j.split("],[").size)
        val fat = (1..60).map { enc("set, p$it, " + "v".repeat(400))!! }
        assertTrue(ScriptletRules.payload(fat).length <= ScriptletRules.MAX_PAYLOAD)
    }

    @Test fun theCssAnswerCarriesTheCallsAfterTheOtherMarkersOnlyWhenAsked() {
        val fs = set("example.com##+js(nowoif)", "example.com##.ad", "example.com##div:has-text(Sponsored)", "##.generic1")
        val off = fs.cosmeticCss("example.com", tokenMode = true)
        assertFalse("scriptlets are off unless asked for", off.contains("/*ks"))
        val on = fs.cosmeticCss("example.com", tokenMode = true, scriptlets = true)
        assertTrue(on, on.startsWith(CosmeticIndex.TOKEN_MARKER + ProceduralRules.MARKER + "/*ks[[\"nowoif\"]]*/"))
        assertTrue(on.endsWith(".ad{display:none!important}\n"))
        assertFalse("the plain (non-token) answer never carries them", fs.cosmeticCss("example.com", scriptlets = true).contains("/*ks"))
        assertFalse(fs.cosmeticCss("other.org", tokenMode = true, scriptlets = true).contains("/*ks"))
    }

    @Test fun shieldsSiteSwitchesStillWin() {
        val fs = set("example.com##+js(nowoif)", "@@||example.com^\$elemhide", "example.org##+js(nowoif)", "@@||example.org^\$document")
        assertTrue(fs.scriptletsFor("example.com").isEmpty()); assertTrue(fs.scriptletsFor("example.org").isEmpty())
        assertEquals("", fs.cosmeticCss("example.com", tokenMode = true, scriptlets = true))
    }

    @Test fun thePayloadForTheEarlyScriptIsTheSameJsonAsTheOneInTheCssAnswer() {
        val fs = set("example.com##+js(set, adblockDetected, false)", "example.com##+js(nostif, nag)", "example.com##.ad", "other.org##+js(nowoif)", "off.com##+js(nowoif)", "@@||off.com^\$document")
        val p = fs.scriptletPayload("example.com")
        assertEquals("[[\"set\",\"adblockDetected\",\"false\"],[\"nostif\",\"nag\"]]", p)
        assertEquals("a subdomain gets the calls of its parent", p, fs.scriptletPayload("www.example.com"))
        assertTrue("the late copy in the CSS answer carries exactly this text", fs.cosmeticCss("example.com", tokenMode = true, scriptlets = true).contains("/*ks$p*/"))
        assertEquals("[[\"nowoif\"]]", fs.scriptletPayload("other.org"))
        assertEquals("a host with no calls", "", fs.scriptletPayload("nothing.example"))
        assertEquals("an allow-listed page gets none", "", fs.scriptletPayload("off.com"))
        assertEquals("", fs.scriptletPayload(""))
    }

    @Test fun thePayloadNeverHoldsAStarOrANonAsciiCharacterSoItCanSitInAJsStringAndInACssComment() {
        val fs = set("example.com##+js(rpnt, script, /a*b\\.c/, \"x*y\")", "example.com##+js(set, name, café)")
        val p = fs.scriptletPayload("example.com")
        assertTrue(p, p.isNotEmpty())
        assertFalse(p.contains('*'))
        assertTrue(p.all { it.code in 0x20..0x7e })
    }

    @Test fun theBuildReportCountsWhatItTookAndWhatItSkipped() {
        val b = FilterSetBuilder()
        b.addAbpList("t", Category.ADS, sequenceOf("a.com##+js(nowoif)", "a.com##+js(aopr, x)", "##+js(nowoif)", "a.com##+js(unknown-one, x)", "a.com##+js(set, only)"))
        assertEquals(2, b.report.scriptlets)
        assertEquals(1, b.report.skippedCount(Skip.COSMETIC_UNSUPPORTED))
        assertEquals(2, b.report.skippedCount(Skip.SCRIPTLET_UNSUPPORTED))
    }

    @Test fun savedAndReadBackTheCallsSurvive() {
        val fs = set("example.com##+js(aopr, a)", "lookcam.*##+js(nowoif)", "x.com#@#+js()")
        val back = FilterSetCodec.read(FilterSetCodec.write(fs))
        assertEquals(fs.scriptletsFor("example.com"), back.scriptletsFor("example.com"))
        assertEquals(listOf("nowoif"), back.scriptletsFor("www.lookcam.co.uk").map { ScriptletRules.decode(it)!!.name })
    }
}
