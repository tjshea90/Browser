package app.kestrel.browser.ui

import android.app.AlertDialog
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import app.kestrel.browser.R
import app.kestrel.browser.data.Prefs
import app.kestrel.core.shield.DataSaver
import app.kestrel.core.url.HttpsPolicy

interface SettingsListener {
    fun closeSettings()
    fun prefsChanged()
    fun clearBrowsingData()
    fun resetSiteShields()
    fun updateFiltersNow()
    fun resetDataCounter()
    fun filterStatus(): String
    fun dataUsed(): String
    fun aboutLines(): List<Pair<String, String>>
    /** Settings > Neon glow > Test the pulse: close Settings and make every glow breathe for 10 seconds (Kestrel G). */
    fun testNeon() {}
    /** Settings > Back up & restore (owner AA3-7): write the backup file now / pick a backup file to bring back. */
    fun backupNow() {}
    fun restoreFromFile() {}
}

/** Settings: plain scrolling rows, no AndroidX preference library. */
class SettingsView(private val ctx: Context, private val pal: Pal, private val prefs: Prefs, private val l: SettingsListener) : LinearLayout(ctx) {
    private val body = LinearLayout(ctx)

    init {
        orientation = VERTICAL
        setBackgroundColor(pal.windowBg)
        isClickable = true
        val header = LinearLayout(ctx).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(pal.chromeBg); setPadding(ctx.dp(4), 0, ctx.dp(8), 0) }
        header.addView(iconButton(ctx, R.drawable.ic_back, "Back", pal.fg) { l.closeSettings() })
        header.addView(label(ctx, "Settings", 20f, pal.fg, bold = true).apply { setPadding(ctx.dp(8), 0, 0, 0) })
        addView(header, LayoutParams(LayoutParams.MATCH_PARENT, ctx.dp(56)))
        body.orientation = VERTICAL
        addView(ScrollView(ctx).apply { addView(body); isFillViewport = true }, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        rebuild()
    }

    fun rebuild() {
        body.removeAllViews()
        section("Search")
        choice("Search engine", Prefs.ENGINES.map { it.name }, Prefs.ENGINES.indexOfFirst { it.id == prefs.searchId }.coerceAtLeast(0)) { prefs.searchId = Prefs.ENGINES[it].id }

        section("Privacy & security (defaults for every site)")
        choice("HTTPS", listOf("Off", "Upgrade when possible", "Strict: warn before plain HTTP"), prefs.httpsMode.ordinal) { prefs.httpsMode = HttpsPolicy.Mode.values()[it] }
        toggle("Block ads & trackers", "Network blocking with EasyList, EasyPrivacy and uBlock lists", prefs.shieldAds) { prefs.shieldAds = it }
        toggle("Hide ad spaces & cookie banners", null, prefs.shieldCosmetic) { prefs.shieldCosmetic = it }
        if (app.kestrel.browser.Flavor.hasPopupGuard) toggle("Block pop-ups & forced redirects", "Refuses new tabs and page jumps you did not ask for, such as the ad tab a video site opens when you press play. A notice offers to open one anyway", prefs.blockPopups) { prefs.blockPopups = it }
        toggle("Block cross-site cookies", null, prefs.shieldCookies) { prefs.shieldCookies = it }
        toggle("Block fingerprinting", null, prefs.shieldFingerprint) { prefs.shieldFingerprint = it }
        toggle("Clear browsing data when the app is closed", "Cookies, cache, history and saved site settings, the next time it opens after you swipe it away", prefs.clearOnExit) { prefs.clearOnExit = it }
        button("Clear browsing data now", "Cookies, cache, history, site permissions") { confirm("Clear all browsing data?", "Cookies, cache, history and saved logins will be deleted. Bookmarks are kept.") { l.clearBrowsingData() } }
        button("Reset per-site Shields", "Forget the choices you made in the Shields panel") { confirm("Reset per-site Shields?", "Every site goes back to the defaults above.") { l.resetSiteShields() } }

        section("Data saver (off: this hotspot is unmetered)")
        choice("Level", listOf("Off (recommended: no data limit)", "Standard: confirm big downloads, no autoplay", "Strict: no images until tapped, no web fonts"), prefs.dataSaver.ordinal) { prefs.dataSaver = DataSaver.values()[it] }
        info("Data used by this browser", l.dataUsed())
        button("Reset data counter", null) { l.resetDataCounter(); rebuild() }

        section("Downloads")
        val limits = listOf(0, 2048, 1024, 500, 250)
        choice("Speed limit (all downloads together)", listOf("No limit", "2 MB/s", "1 MB/s", "500 KB/s", "250 KB/s"), limits.indexOf(prefs.dlLimitKBps).let { if (it < 0) 0 else it }) {
            prefs.dlLimitKBps = limits[it]; app.kestrel.browser.download.DownloadController.setSpeedLimit(limits[it])
        }
        toggle("Slow downloads while a page loads", "Downloads drop to about 150 KB/s while the page you are looking at is loading (about 1 MB/s while LinkMeter's Fast Wi-Fi is on, which has room for both), then speed up again: pages open faster during a big download", prefs.dlYield) { prefs.dlYield = it; app.kestrel.browser.download.DownloadController.setYieldToPages(it) }
        info("Why a limit", "A phone hotspot has one radio: a big download that takes all of it makes browsing crawl. A limit leaves room for the page you are reading; it also saves data when a download is not urgent.")
        choice("Downloads at the same time", listOf("1", "2 (default)", "3"), prefs.dlActive - 1) { prefs.dlActive = it + 1 }
        choice("Connections per download", listOf("Automatic (recommended)", "1", "2", "3"), prefs.dlConns) { prefs.dlConns = it }
        info("When it applies", "The speed limit changes at once. The other two apply the next time the browser starts.")

        app.kestrel.browser.download.PageProxy.refresh()                    // fresh numbers from the phone for the next time this screen is built
        section("Phone helper (Kestrel Courier: off by default, finished work)")
        info("When you need it", "Not with LinkMeter's Fast Wi-Fi: while a VPN is on, this helper rests by itself (a second relay on top of it only adds a hop). It is for the normal hotspot, where the phone's own connection is faster than the capped hotspot.")
        info("What it does", "Your phone downloads the file over its own connection and sends it to this tablet over the hotspot wifi when it has it. The download looks and works the same here, with live progress. You need the small Kestrel Courier app on the phone, with the hotspot on.")
        // ONE switch for everything (the main menu has the same one): off = downloads, videos and pages all go through the hotspot only; on = all of it. The fine choices exist only while it is on.
        toggle("Phone helper", "One switch for everything the phone does for this tablet: downloads, videos and pages. Off: nothing goes through the phone. The main menu has the same switch once a phone is paired.", app.kestrel.browser.download.PhoneBridge.masterOn(ctx)) { on ->
            if (on && !app.kestrel.browser.download.PhoneBridge.paired(ctx)) { rebuild(); pairDialog(thenOn = true) } // it cannot be on without a phone: pair first, then it comes on
            else { app.kestrel.browser.download.PhoneBridge.setMaster(ctx, on); rebuild() }
        }
        if (app.kestrel.browser.download.PhoneBridge.masterOn(ctx)) {
            choice("Downloads and videos", listOf("When the phone is there", "Always: wait for the phone"), (prefs.phoneMode - 1).coerceIn(0, 1)) { prefs.phoneMode = it + 1; app.kestrel.browser.download.PhoneBridge.modeChanged(ctx) }
            toggle("Load pages through the phone", "Pages are fetched by the phone with its own connection, which the carrier does not slow down like the hotspot. Needs Kestrel Courier 0.1.0-alpha3 or newer on the phone. If the phone stops answering, pages load directly by themselves.", prefs.pagesViaPhone) { prefs.pagesViaPhone = it; app.kestrel.browser.download.PageProxy.refresh(); rebuild() }
            info("Pages", app.kestrel.browser.download.PageProxy.line(ctx))
        }
        button("Pair with the phone", if (app.kestrel.browser.download.PhoneBridge.paired(ctx)) "Paired. Tap to type another code or to forget this one." else "Type the code the Courier app shows") { pairDialog() }
        button("Test the phone link", "Measures the wifi to the phone, the phone's internet and this tablet's internet, and says whether it is worth it (uses about 40 MB)") { linkTest() }
        info("Phone link", app.kestrel.browser.download.PhoneBridge.statusLine(ctx))
        info("Good to know", "The phone's data plan pays for these downloads, and the phone works harder (battery, heat). Private tabs' downloads and app updates are always done by the tablet itself; pages in private tabs also go through the phone (it sees which sites, not what you read). If the phone cannot do a download (or a video it cannot convert), the tablet does it and the row says why.")

        section("Appearance")
        choice("Neon glow", listOf("Off", "Steady", "Subtle pulse"), prefs.neon) { prefs.neon = it; app.kestrel.browser.ui.Neon.mode = it; app.kestrel.browser.ui.Neon.refresh(); rebuild() }
        info("The glow right now", app.kestrel.browser.ui.Neon.status().replaceFirstChar { it.uppercase() })
        if (app.kestrel.browser.Flavor.hasPullRefresh) button("Test the pulse (10 s)", "Closes Settings and makes every glow breathe for 10 seconds, whatever else is going on") { l.testNeon() }
        if (app.kestrel.browser.Flavor.hasScrollFeel) {
            choice("Scroll feel", listOf("Standard (Firefox's own)", "Smoother (default): pre-draws about twice the screen around your finger, answers touches sooner", "Smoothest: pre-draws even more (more memory), lower top fling speed"), prefs.scrollFeel) {
                prefs.scrollFeel = it; l.prefsChanged()
            }
        }
        if (app.kestrel.browser.Flavor.hasTabBudget) {
            choice("Tabs kept ready", listOf("Automatic (default): as many as the free memory allows (2 to 8), so switching tabs and Back stay instant", "Few (2): least memory; the others load again when you open them", "Many (6): more tabs stay ready; Android may then close the browser sooner when memory runs out"), prefs.tabsKept) {
                prefs.tabsKept = it; l.prefsChanged()
            }
        }
        if (app.kestrel.browser.Flavor.hasScrollFeel) {
            toggle("Double-tap to zoom", "Off (default): a tap opens a link at once. On: double-tap a page to zoom in, but on pages wider than the screen (Desktop site) every tap then waits about a quarter of a second. Pinching always works.", prefs.doubleTapZoom) { prefs.doubleTapZoom = it; l.prefsChanged() }
            toggle("Zoom to the text box you tap", "On (default): the page zooms to a text box when you tap it. Turn it off if typing starts slowly: small boxes then stay small until you pinch.", prefs.inputAutoZoom) { prefs.inputAutoZoom = it; l.prefsChanged() }
        }
        if (app.kestrel.browser.Flavor.hasPullRefresh) toggle("Pull down to refresh", "Drag a page down from its top edge to reload it", prefs.pullRefresh) { prefs.pullRefresh = it }
        choice("Dark pages", listOf("Automatic", "Always", "Never"), prefs.webDark) { prefs.webDark = it }
        choice("Text size", listOf("75%", "100%", "125%", "150%", "175%", "200%"), listOf(75, 100, 125, 150, 175, 200).indexOfFirst { it >= prefs.textZoom }.let { if (it < 0) 1 else it }) {
            prefs.textZoom = listOf(75, 100, 125, 150, 175, 200)[it]
        }

        if (app.kestrel.browser.Flavor.hasPullRefresh) {
            section("App updates")
            toggle("Look for updates by itself", "Once a day, also when the app is closed: one tiny request", prefs.autoUpdateCheck) { prefs.autoUpdateCheck = it }
            toggle("Download updates in the background", "Uses the download service, so it continues with the screen off; about 92 MB per release on your connection", prefs.autoUpdateDownload) { prefs.autoUpdateDownload = it }
            toggle("Install by itself when Android allows", "Android 12 and newer only; Android 10 always shows its own Install screen, so you get a notification to tap", prefs.autoUpdateInstall) { prefs.autoUpdateInstall = it }
        }

        section("Filter lists")
        info("Status", l.filterStatus())
        button("Update now", "Downloads only what changed (usually a few MB at most)") { l.updateFiltersNow() }
        choice("Ad-blocking lists: update", listOf("Every day (recommended)", "Every week", "Only when I tap Update"), prefs.updatePolicy) { prefs.updatePolicy = it }
        toggle("Only on unmetered networks", "A hotspot counts as metered, so leave this off if you want updates over it", prefs.updateUnmeteredOnly) { prefs.updateUnmeteredOnly = it }

        if (app.kestrel.browser.Flavor.hasNetworkLab) {
            section("Network lab (for slow pages)")
            info("Right now", app.kestrel.core.net.NetProfile.effective(app.kestrel.browser.download.PhoneBridge.profileInput(ctx)).text + if (app.kestrel.browser.net.NetworkMonitor.vpn) " Your switches below come back when the VPN goes away." else "")
            info("How to use it", "Change ONE switch, load the same site twice, then open Diagnostics > Page loads and compare the times. Keep what is faster.")
            toggle("Encrypted DNS (DNS over HTTPS)", "On: your carrier cannot read or rewrite which sites you look up. Off: the tablet's own DNS, which can be quicker on a hotspot", prefs.dohOn) { prefs.dohOn = it; l.prefsChanged() }
            toggle("Warm up the top suggestion", "While you type in the address bar, the connection to the first suggested site (one you visited before) is opened ahead, which saves about a second on a slow link. A few KB; never in private tabs", prefs.preconnect) { prefs.preconnect = it }
            toggle("HTTP/3 (QUIC)", "Off can help when the hotspot's carrier slows down UDP traffic; sites then use HTTP/2", prefs.http3On) { prefs.http3On = it; l.prefsChanged() }
            toggle("Look up links ahead", "Firefox looks up the web addresses of the links on a page before you tap one, so a tap saves one lookup (maybe 50 to 150 ms). The DNS server also sees addresses of links you never tap. Stays off while Data saver is on.", prefs.labDnsPrefetch) { prefs.labDnsPrefetch = it; l.prefsChanged() }
            toggle("Bigger network buffer", "Passes the downloaded bytes on in bigger pieces. Helps a fast link (Fast Wi-Fi) a little; no effect on the slow hotspot.", prefs.labBuffer) { prefs.labBuffer = it; l.prefsChanged() }
            toggle("Show text sooner while a web font loads", "Text in a web font appears after half a second in a plain font instead of staying invisible for up to 3 seconds, then swaps. Pages may jump a little when the font arrives.", prefs.labFontDelay) { prefs.labFontDelay = it; l.prefsChanged() }
            choice("Fetch the next page early (experimental)", listOf("Off", "Dry run: only note what would be fetched", "On: sites that ask for it get their next page fetched when you press down"), prefs.labSpec) { prefs.labSpec = it; l.prefsChanged() }
            toggle("Keep Wi-Fi awake while Kestrel is open", "The tablet's Wi-Fi stays out of power save while the browser is on screen, so the first tap after a pause is not slowed by a sleeping radio. Uses more battery.", prefs.labWifiBoost) { prefs.labWifiBoost = it; l.prefsChanged() }
            toggle("Keep the phone helper under Fast Wi-Fi", "While LinkMeter's Fast Wi-Fi is on, the phone helper (Kestrel Courier) normally rests. On: it keeps working as set above, so you can compare page times and download speed through the helper with the VPN's. Needs the Courier running on the phone.", prefs.labHelperUnderVpn) { prefs.labHelperUnderVpn = it; l.prefsChanged() }
        }

        section("Secure DNS")
        if (app.kestrel.browser.Flavor.secureDnsBuiltIn) info("Encrypted DNS", "This browser encrypts its DNS lookups itself (DNS over HTTPS through Cloudflare, so the mobile carrier cannot read or tamper with them) and quietly falls back to the tablet's normal DNS if that service cannot be reached.")
        else info("Private DNS", "This browser cannot encrypt DNS lookups by itself. For that, set it for the whole tablet: Settings → Network & internet → Advanced → Private DNS (for example dns.quad9.net or dns.adguard-dns.eu).")

        section("Back up & restore")
        info("What it keeps", "Bookmarks, history, your settings, the per-site Shields choices and your open tabs. Not cookies, logins or private tabs.")
        button("Back up now", "Saves one file in Downloads. Do this before you uninstall Kestrel; Downloads is not erased with the app") { l.backupNow() }
        button("Restore from a backup file", "Pick the file in Downloads. Bookmarks, history and tabs are added; settings are replaced") { l.restoreFromFile() }

        section("About")
        for ((k, v) in l.aboutLines()) info(k, v)
        val pad = View(ctx); body.addView(pad, LayoutParams(1, ctx.dp(32)))
    }

    // ------------------------------------------------------------------ row builders

    private fun section(text: String) {
        body.addView(label(ctx, text.uppercase(), 12f, pal.accent, bold = true).apply { setPadding(ctx.dp(20), ctx.dp(24), ctx.dp(20), ctx.dp(6)); includeFontPadding = true })
    }

    private fun rowBase(title: String, subtitle: String?): Pair<LinearLayout, LinearLayout> {
        val r = LinearLayout(ctx).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(ctx.dp(20), ctx.dp(10), ctx.dp(16), ctx.dp(10)); minimumHeight = ctx.dp(56) }
        val t = LinearLayout(ctx).apply { orientation = VERTICAL }
        t.addView(label(ctx, title, 16f, pal.fg).apply { includeFontPadding = true })
        if (!subtitle.isNullOrEmpty()) t.addView(label(ctx, subtitle, 13f, pal.muted).apply { setPadding(0, ctx.dp(2), 0, 0); includeFontPadding = true })
        r.addView(t, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        body.addView(r, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        return r to t
    }

    private fun toggle(title: String, subtitle: String?, value: Boolean, set: (Boolean) -> Unit) {
        val (row, _) = rowBase(title, subtitle)
        val sw = Switch(ctx).apply { isChecked = value }
        row.addView(sw)
        row.background = ripple(pal.fg, null, 0f)
        row.isClickable = true
        row.setOnClickListener { sw.toggle() }
        sw.setOnCheckedChangeListener { _, c -> set(c); l.prefsChanged() }
    }

    private fun choice(title: String, options: List<String>, selected: Int, set: (Int) -> Unit) {
        val (row, text) = rowBase(title, null)
        val valueView = label(ctx, options.getOrElse(selected) { "" }, 13f, pal.muted).apply { setPadding(0, ctx.dp(2), 0, 0); includeFontPadding = true }
        text.addView(valueView)
        row.background = ripple(pal.fg, null, 0f)
        row.isClickable = true
        row.setOnClickListener {
            AlertDialog.Builder(ctx).setTitle(title).setSingleChoiceItems(options.toTypedArray(), selected) { d, which ->
                d.dismiss(); set(which); l.prefsChanged(); rebuild()
            }.setNegativeButton("Cancel", null).show()
        }
    }

    private fun button(title: String, subtitle: String?, onClick: () -> Unit) {
        val (row, _) = rowBase(title, subtitle)
        row.background = ripple(pal.fg, null, 0f)
        row.isClickable = true
        row.setOnClickListener { onClick() }
    }

    private fun info(title: String, value: String) {
        val (row, _) = rowBase(title, value)
        row.setOnLongClickListener { true }
    }

    private fun pairDialog(thenOn: Boolean = false) {
        val bridge = app.kestrel.browser.download.PhoneBridge
        val input = android.widget.EditText(ctx).apply { hint = "ABCD-EFGH-JKLM-NPQR"; setSingleLine(); inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS; setPadding(ctx.dp(20), ctx.dp(12), ctx.dp(20), ctx.dp(12)) }
        val b = AlertDialog.Builder(ctx).setTitle("Pairing code").setMessage("Open Kestrel Courier on your phone. It shows 16 letters and numbers. Type them here once.").setView(input)
            .setPositiveButton("Pair") { _, _ ->
                if (bridge.savePairing(ctx, input.text.toString())) { if (thenOn) bridge.setMaster(ctx, true); rebuild() }
                else AlertDialog.Builder(ctx).setTitle("That is not a code").setMessage("The code has 16 letters and numbers, like ABCD-EFGH-JKLM-NPQR. The letters I and O and the digits 0 and 1 are never used. Check the phone's screen and try again.").setPositiveButton("OK", null).show()
            }.setNegativeButton("Cancel", null)
        if (bridge.paired(ctx)) b.setNeutralButton("Forget the phone") { _, _ -> bridge.forgetPairing(ctx); rebuild() }
        b.show()
    }

    private fun linkTest() {
        val bridge = app.kestrel.browser.download.PhoneBridge
        val wait = AlertDialog.Builder(ctx).setTitle("Testing the phone link").setMessage("This takes up to half a minute. Please keep the hotspot on.").setCancelable(false).show()
        bridge.testLink(ctx) { report, error ->
            wait.dismiss()
            AlertDialog.Builder(ctx).setTitle(if (report != null) "Phone link" else "No test").setMessage(if (report != null) bridge.describe(report) else error).setPositiveButton("OK", null).show()
            rebuild()
        }
    }

    private fun confirm(title: String, message: String, ok: () -> Unit) {
        AlertDialog.Builder(ctx).setTitle(title).setMessage(message)
            .setPositiveButton("Clear") { _, _ -> ok() }.setNegativeButton("Cancel", null).show()
    }
}
