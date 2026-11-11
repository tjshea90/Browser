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
        toggle("Block cross-site cookies", null, prefs.shieldCookies) { prefs.shieldCookies = it }
        toggle("Block fingerprinting", null, prefs.shieldFingerprint) { prefs.shieldFingerprint = it }
        toggle("Clear browsing data when I quit", "Cookies, cache, history and saved site settings", prefs.clearOnExit) { prefs.clearOnExit = it }
        button("Clear browsing data now", "Cookies, cache, history, site permissions") { confirm("Clear all browsing data?", "Cookies, cache, history and saved logins will be deleted. Bookmarks are kept.") { l.clearBrowsingData() } }
        button("Reset per-site Shields", "Forget the choices you made in the Shields panel") { confirm("Reset per-site Shields?", "Every site goes back to the defaults above.") { l.resetSiteShields() } }

        section("Data saver (this tablet uses a phone hotspot)")
        choice("Level", listOf("Off", "Standard (recommended)", "Strict: no images until tapped, no web fonts"), prefs.dataSaver.ordinal) { prefs.dataSaver = DataSaver.values()[it] }
        info("Data used by this browser", l.dataUsed())
        button("Reset data counter", null) { l.resetDataCounter(); rebuild() }

        section("Appearance")
        choice("Dark pages", listOf("Automatic", "Always", "Never"), prefs.webDark) { prefs.webDark = it }
        choice("Text size", listOf("75%", "100%", "125%", "150%", "175%", "200%"), listOf(75, 100, 125, 150, 175, 200).indexOfFirst { it >= prefs.textZoom }.let { if (it < 0) 1 else it }) {
            prefs.textZoom = listOf(75, 100, 125, 150, 175, 200)[it]
        }

        section("Filter lists")
        info("Status", l.filterStatus())
        button("Update now", "Downloads only what changed (usually a few MB at most)") { l.updateFiltersNow() }
        choice("Update schedule", listOf("Weekly", "Monthly", "Only when I tap Update"), prefs.updatePolicy) { prefs.updatePolicy = it }
        toggle("Only on unmetered networks", "A hotspot counts as metered, so leave this off if you want updates over it", prefs.updateUnmeteredOnly) { prefs.updateUnmeteredOnly = it }

        section("Secure DNS")
        if (app.kestrel.browser.Flavor.secureDnsBuiltIn) info("Encrypted DNS", "This browser encrypts its DNS lookups itself (DNS over HTTPS through Cloudflare, so the mobile carrier cannot read or tamper with them) and quietly falls back to the tablet's normal DNS if that service cannot be reached.")
        else info("Private DNS", "This browser cannot encrypt DNS lookups by itself. For that, set it for the whole tablet: Settings → Network & internet → Advanced → Private DNS (for example dns.quad9.net or dns.adguard-dns.eu).")

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

    private fun confirm(title: String, message: String, ok: () -> Unit) {
        AlertDialog.Builder(ctx).setTitle(title).setMessage(message)
            .setPositiveButton("Clear") { _, _ -> ok() }.setNegativeButton("Cancel", null).show()
    }
}
