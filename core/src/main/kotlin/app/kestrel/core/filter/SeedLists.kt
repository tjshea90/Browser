package app.kestrel.core.filter

/**
 * Tiny built-in protection used (a) on a brand-new install before the first list update and (b) as the fail-safe when a
 * downloaded set is unreadable. Deliberately conservative: obvious ad/tracking endpoints only — nothing whose blocking
 * breaks mainstream sites (no Google Tag Manager, reCAPTCHA, fonts, CDNs, payment or login providers).
 */
object SeedLists {
    val ADS = """
||doubleclick.net^
||googlesyndication.com^
||googleadservices.com^
||googletagservices.com^
||adservice.google.com^
||2mdn.net^
||amazon-adsystem.com^
||adnxs.com^
||adsrvr.org^
||criteo.com^
||criteo.net^
||taboola.com^
||outbrain.com^
||rubiconproject.com^
||pubmatic.com^
||openx.net^
||casalemedia.com^
||indexww.com^
||smartadserver.com^
||moatads.com^
||doubleverify.com^
||adsafeprotected.com^
||advertising.com^
||adform.net^
||adition.com^
||serving-sys.com^
||flashtalking.com^
||yieldmo.com^
||sharethrough.com^
||triplelift.com^
||33across.com^
||lijit.com^
||sovrn.com^
||bidswitch.net^
||contextweb.com^
||spotxchange.com^
||teads.tv^
||mgid.com^
||revcontent.com^
||zemanta.com^
||adcolony.com^
||inmobi.com^
||mopub.com^
||smaato.net^
||popads.net^
||popcash.net^
||propellerads.com^
||exoclick.com^
||juicyads.com^
||trafficjunky.net^
||adcash.com^
||clickadu.com^
||hilltopads.net^
||ads.yahoo.com^
||ads.twitter.com^
||static.ads-twitter.com^
||ads.linkedin.com^
||ads.pinterest.com^
||ads-api.tiktok.com^
||an.facebook.com^
||pagead2.googlesyndication.com^
||adsystem.amazon.com^
""".trimIndent()

    val TRACKERS = """
||google-analytics.com^
||stats.g.doubleclick.net^
||scorecardresearch.com^
||quantserve.com^
||quantcount.com^
||chartbeat.net^
||chartbeat.com^
||hotjar.com^
||hotjar.io^
||fullstory.com^
||mouseflow.com^
||crazyegg.com^
||clarity.ms^
||bat.bing.com^
||analytics.twitter.com^
||analytics.tiktok.com^
||analytics.yahoo.com^
||px.ads.linkedin.com^
||snap.licdn.com^
||ct.pinterest.com^
||api.segment.io^
||cdn.segment.com^
||api.mixpanel.com^
||api.amplitude.com^
||demdex.net^
||everesttech.net^
||omtrdc.net^
||2o7.net^
||mathtag.com^
||agkn.com^
||liadm.com^
||tapad.com^
||krxd.net^
||bluekai.com^
||rlcdn.com^
||bounceexchange.com^
||branch.io^
||adjust.com^
||appsflyer.com^
||kochava.com^
||singular.net^
||tiqcdn.com^
||facebook.com/tr/
||facebook.com/tr?
||connect.facebook.net/en_US/fbevents.js
||google.com/pagead/
||google.com/ads/
||googleadservices.com/pagead/
||youtube.com/api/stats/ads
||youtube.com/pagead/
||youtube.com/ptracking
""".trimIndent()

    /** Generic element-hiding selectors for the commonest ad containers (all valid in any WebView). */
    val COSMETIC = """
##.adsbygoogle
##ins.adsbygoogle
##.ad-banner
##.ad-slot
##.ad-container
##.advert
##.advertisement
##.sponsored-ad
##[id^="google_ads_"]
##[id^="div-gpt-ad"]
##[id^="taboola-"]
##[id^="outbrain_"]
##.OUTBRAIN
##.taboola
##[data-ad-slot]
##[data-google-query-id]
##iframe[src*="doubleclick.net"]
##iframe[src*="googlesyndication.com"]
##iframe[id^="google_ads_iframe"]
##a[href*="doubleclick.net/"]
##a[href*="googleadservices.com/"]
""".trimIndent()

    /** Builds the fail-safe set (fast: ~0.3 ms on a desktop, a few ms on the tablet). */
    @JvmStatic
    fun build(): FilterSet {
        val b = FilterSetBuilder()
        b.addAbpList("kestrel-seed-ads", Category.ADS, ADS.lineSequence())
        b.addAbpList("kestrel-seed-trackers", Category.PRIVACY, TRACKERS.lineSequence())
        b.addAbpList("kestrel-seed-cosmetic", Category.ADS, COSMETIC.lineSequence())
        return b.build(0L)
    }
}
