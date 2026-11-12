package app.kestrel.core.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** `$removeparam` (AA3-3b): the parser, what a navigation address loses, the compiled file. */
class QueryRulesTest {
    private fun build(vararg lines: String): FilterSet {
        val b = FilterSetBuilder()
        b.addAbpList("t", Category.PRIVACY, lines.asSequence())
        return b.build()
    }

    private fun strip(url: String, vararg lines: String): String? = build(*lines).stripQuery(url)

    // ------------------------------------------------------------------ parser

    private fun parse(line: String) = QueryRuleParser.parse(line)

    @Test fun aGenericNameRuleIsAcceptedAndKeepsTheCaseOfTheName() {
        val r = parse("\$removeparam=pjClid").rule!!
        assertEquals(QueryRule.REMOVE, r.kind); assertEquals("pjClid", r.name); assertEquals("", r.host); assertEquals("", r.path)
    }

    @Test fun hostAndHostPathRulesKeepTheirHostAndPathPrefix() {
        val a = parse("||Auto.DE^\$removeparam=source").rule!!
        assertEquals("auto.de", a.host); assertEquals("", a.path)
        val b = parse("||cdn.modrinth.com/data/\$removeparam=mr_download_reason").rule!!
        assertEquals("cdn.modrinth.com", b.host); assertEquals("/data/", b.path)
        assertEquals("a slash alone is any path", "", parse("||x.example/\$removeparam=a").rule!!.path)
    }

    @Test fun documentConstraintsAndExceptionsAreRead() {
        assertNotNull(parse("||bandsintown.com^\$doc,removeparam=app_id").rule)
        assertNotNull(parse("||soundcore.com^\$document,removeparam=ref").rule)
        val d = parse("\$removeparam=clid,domain=a.example|~b.a.example").rule!!
        assertEquals(listOf("a.example"), d.only.toList()); assertEquals(listOf("b.a.example"), d.except.toList())
        val t = parse("\$removeparam=elqTrackId,to=~one.example|~two.example").rule!!
        assertTrue(t.only.isEmpty()); assertEquals(listOf("one.example", "two.example"), t.except.toList())
        assertEquals(QueryRule.KEEP, parse("@@||usprobioticguide.com^\$removeparam=utm_campaign").rule!!.kind)
        assertEquals(QueryRule.ALLOW_ALL, parse("@@||chaynikam.info^\$removeparam").rule!!.kind)
        assertEquals(QueryRule.REMOVE_ALL, parse("||777casino.top^\$removeparam").rule!!.kind)
    }

    @Test fun whatCannotBeAppliedToANavigationIsLeftOutAndSaysWhy() {
        assertEquals(Skip.REGEX, parse("||reddit.com^\$doc,removeparam=/web_only=/").skip)
        assertEquals(Skip.REGEX, parse("\$removeparam=/^utm_/").skip)
        assertEquals(Skip.ENTITY_DOMAIN, parse("\$removeparam=clid,domain=adguard-vpn.*|adguardvpn-help.*").skip)
        assertEquals(Skip.ENTITY_DOMAIN, parse("||www.amazon.*/page/\$doc,removeparam=lp_context_asin").skip)
        for (l in listOf("||alia-cloudflare.com/launcher/\$removeparam=url,xhr", "||popin.cc/popin_discovery/\$script,removeparam=info", "||edge.curalate.com/v1/img/\$image,removeparam=spatialTags", "||connatix.com^\$3p,removeparam=cid"))
            assertEquals(l, Skip.UNSUPPORTED_OPTION, parse(l).skip)
        assertEquals("a document AND an image rule is still a document rule", true, parse("||search.naver.com/p/crd/rd\$removeparam=px,document,image").rule != null)
        assertEquals(Skip.UNSUPPORTED_OPTION, parse("?from=glia^\$removeparam=from").skip)
        assertEquals(Skip.UNSUPPORTED_OPTION, parse("||x.example/a*b/\$removeparam=a").skip)
        assertEquals(Skip.UNSUPPORTED_OPTION, parse("||x.example^\$removeparam=a,csp=script-src 'none'").skip)
        assertEquals(Skip.UNSUPPORTED_OPTION, parse("||x.example^\$removeparam=a,redirect=noopjs").skip)
        assertEquals("not a removeparam rule at all", Skip.BAD_SYNTAX, parse("||x.example^\$script").skip)
    }

    @Test fun aListCanNeverMakeTheBrowserStripEverythingEverywhere() {
        assertEquals("bare and generic", Skip.BAD_SYNTAX, parse("\$removeparam").skip)
        assertEquals("a bare exception everywhere", Skip.BAD_SYNTAX, parse("@@\$removeparam").skip)
        assertEquals("a generic exception without a site", Skip.BAD_SYNTAX, parse("@@\$removeparam=utm_source").skip)
        for (n in listOf("q", "id", "v", "url", "code", "state", "token", "page", "Q", "redirect_uri"))
            assertEquals("a generic rule for $n", Skip.UNSUPPORTED_OPTION, parse("\$removeparam=$n").skip)
        assertNotNull("but a rule tied to one site may name it", parse("||example.com^\$removeparam=id").rule)
        assertNotNull("or to a domain list", parse("\$removeparam=id,domain=example.com").rule)
    }

    @Test fun badNamesAndMessyLinesAreRefused() {
        assertEquals(Skip.BAD_SYNTAX, parse("\$removeparam=a b").skip)
        assertEquals(Skip.BAD_SYNTAX, parse("\$removeparam=").skip)
        assertEquals(Skip.BAD_SYNTAX, parse("\$removeparam=" + "n".repeat(65)).skip)
        assertEquals(Skip.BAD_SYNTAX, parse("||nodot^\$removeparam=a").skip)
        assertEquals(Skip.BAD_SYNTAX, parse("||x.example^\$removeparam=a,removeparam=b").skip)
        assertEquals(Skip.BAD_SYNTAX, parse("||x.example^\$removeparam=a," + "domain=" + (1..400).joinToString("|") { "d$it.example" }).skip)
        assertEquals(Skip.BAD_SYNTAX, parse("||x.example^\$removeparam=" + "a".repeat(5000)).skip)
    }

    // ------------------------------------------------------------------ what an address loses

    @Test fun aGenericRuleRemovesTheNameEverywhereAndKeepsTheRestInOrder() {
        assertEquals("https://example.com/p?a=1&c=3", strip("https://example.com/p?a=1&pjclid=zz&c=3", "\$removeparam=pjclid"))
        assertEquals("https://example.com/p", strip("https://example.com/p?pjclid=zz", "\$removeparam=pjclid"))
        assertEquals("the fragment stays", "https://example.com/p#top", strip("https://example.com/p?pjclid=zz#top", "\$removeparam=pjclid"))
        assertEquals("https://example.com/p?x=1#a?b", strip("https://example.com/p?x=1&pjclid#a?b", "\$removeparam=pjclid"))
        assertEquals("every copy of the name", "https://e.example/?k=1", strip("https://e.example/?pjclid=1&k=1&pjclid=2", "\$removeparam=pjclid"))
        assertNull(strip("https://example.com/p?a=1", "\$removeparam=pjclid"))
        assertNull("no query at all", strip("https://example.com/p", "\$removeparam=pjclid"))
        assertNull("an empty query", strip("https://example.com/p?", "\$removeparam=pjclid"))
        assertNull("a name that only contains the rule's name", strip("https://example.com/p?xpjclid=1&pjclid2=2", "\$removeparam=pjclid"))
    }

    @Test fun namesAreMatchedExactlyIncludingCase() {
        assertNull(strip("https://example.com/?PJCLID=1", "\$removeparam=pjclid"))
        assertEquals("https://example.com/", strip("https://example.com/?pjClid=1", "\$removeparam=pjClid"))
    }

    @Test fun aHostRuleCoversTheHostAndItsSubdomainsButNotLookAlikes() {
        val rule = "||auto.de^\$removeparam=source"
        assertEquals("https://auto.de/", strip("https://auto.de/?source=x", rule))
        assertEquals("https://www.auto.de/x", strip("https://www.auto.de/x?source=x", rule))
        assertEquals("https://a.b.auto.de/", strip("https://a.b.auto.de/?source=x", rule))
        assertNull(strip("https://notauto.de/?source=x", rule))
        assertNull(strip("https://auto.de.evil.example/?source=x", rule))
        assertNull(strip("https://other.example/?source=x", rule))
    }

    @Test fun aPathRuleOnlyAppliesBelowItsPathAndIgnoresCase() {
        val rule = "||cdn.modrinth.com/data/\$removeparam=mr_download_reason"
        assertEquals("https://cdn.modrinth.com/data/x/file.jar", strip("https://cdn.modrinth.com/data/x/file.jar?mr_download_reason=1", rule))
        assertEquals("https://cdn.modrinth.com/DATA/x", strip("https://cdn.modrinth.com/DATA/x?mr_download_reason=1", rule))
        assertNull(strip("https://cdn.modrinth.com/other/x?mr_download_reason=1", rule))
        assertNull(strip("https://cdn.modrinth.com?mr_download_reason=1", rule))
    }

    @Test fun domainAndToConstraintsDecideWhereAGenericNameGoes() {
        val only = "\$removeparam=clid,domain=a.example"
        assertEquals("https://a.example/", strip("https://a.example/?clid=1", only))
        assertEquals("https://sub.a.example/", strip("https://sub.a.example/?clid=1", only))
        assertNull(strip("https://b.example/?clid=1", only))
        val except = "\$removeparam=elqTrackId,to=~one.example|~two.example"
        assertEquals("https://x.example/", strip("https://x.example/?elqTrackId=1", except))
        assertNull(strip("https://www.one.example/?elqTrackId=1", except))
        assertNull(strip("https://two.example/?elqTrackId=1", except))
        val both = "\$removeparam=clid,domain=a.example|~keep.a.example"
        assertEquals("https://a.example/", strip("https://a.example/?clid=1", both))
        assertNull(strip("https://keep.a.example/?clid=1", both))
    }

    @Test fun anExceptionLetsASiteKeepOneNameOrEverything() {
        val rules = arrayOf("\$removeparam=pjclid", "\$removeparam=utm_foo", "@@||shop.example^\$removeparam=pjclid")
        assertNull("kept on that site", strip("https://shop.example/?pjclid=1", *rules))
        assertEquals("https://shop.example/?pjclid=1", strip("https://shop.example/?pjclid=1&utm_foo=2", *rules))
        assertEquals("https://other.example/", strip("https://other.example/?pjclid=1", *rules))
        val all = arrayOf("\$removeparam=pjclid", "||shop.example^\$removeparam=a", "@@||shop.example^\$removeparam")
        assertNull(strip("https://shop.example/?pjclid=1&a=2", *all))
        assertEquals("https://other.example/", strip("https://other.example/?pjclid=1", *all))
    }

    @Test fun aBareHostRuleDropsTheWholeQuery() {
        assertEquals("https://777casino.top/x#f", strip("https://777casino.top/x?a=1&b=2#f", "||777casino.top^\$removeparam"))
        assertNull(strip("https://other.example/x?a=1", "||777casino.top^\$removeparam"))
    }

    @Test fun badfilterSwitchesARuleOff() {
        val lines = arrayOf("||auto.de^\$removeparam=source", "||auto.de^\$removeparam=source,badfilter")
        assertNull(strip("https://auto.de/?source=x", *lines))
    }

    @Test fun portsUserInfoUppercaseHostsAndTrailingDotsAreHandled() {
        val rule = "||auto.de^\$removeparam=source"
        assertEquals("https://AUTO.de:8443/x", strip("https://AUTO.de:8443/x?source=1", rule))
        assertEquals("https://user:pw@auto.de/x", strip("https://user:pw@auto.de/x?source=1", rule))
        assertEquals("http://auto.de./x", strip("http://auto.de./x?source=1", rule))
        assertEquals("host straight before the query", "https://auto.de", strip("https://auto.de?source=1", rule))
        assertNull("an '@' in the query is not user info", strip("https://other.example/x?mail=a@auto.de&source=1", rule))
        assertNull("IPv6 literal", strip("http://[::1]/x?source=1", rule))
        assertNull("no scheme", strip("auto.de/x?source=1", rule))
    }

    @Test fun veryLongAddressesAndEmptyRuleSetsAreLeftAlone() {
        assertNull(strip("https://example.com/?pjclid=1&" + "x".repeat(QueryRules.MAX_URL), "\$removeparam=pjclid"))
        assertNull(build().stripQuery("https://example.com/?pjclid=1"))
        assertEquals(0, build().queryRuleCount)
    }

    @Test fun theBuildReportCountsWhatWasAcceptedAndWhatWasNot() {
        val b = FilterSetBuilder()
        b.addAbpList("t", Category.PRIVACY, sequenceOf("\$removeparam=pjclid", "||a.example^\$removeparam=x", "||a.example^\$removeparam=x", "\$removeparam=/^utm_/", "||b.example^\$xhr,removeparam=y", "@@||c.example^\$removeparam"))
        val fs = b.build()
        assertEquals("duplicates collapse", 3, fs.queryRuleCount)
        assertEquals(3, b.report.queryRules)
        assertEquals(1, b.report.skippedCount(Skip.REGEX)); assertEquals(1, b.report.skippedCount(Skip.UNSUPPORTED_OPTION))
        assertTrue(b.report.toString(), b.report.toString().contains("query=3"))
    }

    @Test fun queryRulesDoNotDisturbTheNetworkRulesAroundThem() {
        val fs = build("||ads.example^", "||a.example^\$removeparam=x", "||tracker.example/pixel.gif", "! a comment")
        assertEquals(1, fs.queryRuleCount)
        assertTrue(Verdict.isBlocked(fs.check("https://ads.example/x.js", "page.example", RequestType.SCRIPT)))
        assertFalse(Verdict.isBlocked(fs.check("https://a.example/x.js?x=1", "page.example", RequestType.SCRIPT)))
        assertEquals(2, fs.meta.totalNetwork)
    }

    // ------------------------------------------------------------------ the compiled file

    @Test fun theRulesSurviveTheBinaryFileAndKeepWorking() {
        val fs = build("\$removeparam=pjclid", "||auto.de^\$removeparam=source", "||cdn.modrinth.com/data/\$removeparam=r", "@@||shop.example^\$removeparam=pjclid", "||x.example^\$removeparam=a,domain=x.example|~y.x.example")
        val back = FilterSetCodec.read(FilterSetCodec.write(fs))
        assertEquals(5, back.queryRuleCount)
        assertEquals("https://example.com/", back.stripQuery("https://example.com/?pjclid=1"))
        assertEquals("https://auto.de/", back.stripQuery("https://auto.de/?source=1"))
        assertEquals("https://cdn.modrinth.com/data/f", back.stripQuery("https://cdn.modrinth.com/data/f?r=1"))
        assertNull(back.stripQuery("https://shop.example/?pjclid=1"))
        assertNull(back.stripQuery("https://y.x.example/?a=1"))
    }

    @Test fun aFileWrittenBeforeTheRulesExistedStillLoadsWithoutThem() {
        val fs = build("||ads.example^", "\$removeparam=pjclid")
        val old = FilterSetCodec.write(fs, 2)
        val back = FilterSetCodec.read(old)
        assertEquals(0, back.queryRuleCount)
        assertNull(back.stripQuery("https://example.com/?pjclid=1"))
        assertTrue(Verdict.isBlocked(back.check("https://ads.example/x.js", "page.example", RequestType.SCRIPT)))
    }

    @Test fun aDamagedQueryPoolIsRejectedNotTrusted() {
        val fs = build("\$removeparam=pjclid")
        val good = FilterSetCodec.write(fs)
        // the pool is the last array before the 8-byte checksum: break one separator inside it, then fix the checksum like a careless writer would
        val pool = fs.query.pool()
        val tab = pool.indexOf('\t'.code.toByte())
        assertTrue(tab > 0)
        val bad = good.copyOf()
        val at = bad.size - 8 - pool.size + tab
        bad[at] = 'x'.code.toByte()
        val crc = java.util.zip.CRC32().also { it.update(bad, 0, bad.size - 8) }.value
        java.nio.ByteBuffer.wrap(bad, bad.size - 8, 8).putLong(crc)
        try { FilterSetCodec.read(bad); org.junit.Assert.fail("a malformed rule line must be refused") } catch (e: IllegalArgumentException) { /* expected */ }
        try { FilterSetCodec.read(good.copyOf().also { it[it.size - 20] = (it[it.size - 20] + 1).toByte() }); org.junit.Assert.fail("the checksum must catch a flipped byte") } catch (e: IllegalArgumentException) { /* expected */ }
    }
}
