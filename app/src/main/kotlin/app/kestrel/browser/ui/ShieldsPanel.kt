package app.kestrel.browser.ui

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.Switch
import app.kestrel.browser.shield.Shields
import app.kestrel.browser.web.Tab
import app.kestrel.core.filter.Category
import app.kestrel.core.shield.ShieldFlag
import app.kestrel.core.shield.ShieldSettings
import app.kestrel.core.shield.SiteShields
import app.kestrel.core.url.HttpsPolicy

/** Brave-style "Shields" popup for the current site: master switch, what was blocked, and per-site toggles. */
class ShieldsPanel(private val ctx: Context, private val pal: Pal, private val onChanged: (host: String) -> Unit) {

    private class Spec(val flag: Int, val title: String, val subtitle: String, val globalValue: (ShieldSettings) -> Boolean)

    private val specs = listOfNotNull(
        Spec(ShieldFlag.ADS, "Block ads & trackers", "Stops known ad and tracking requests (saves data too)") { it.blockAds },
        Spec(ShieldFlag.COSMETIC, "Hide ad spaces & cookie banners", "Removes leftover empty ad boxes") { it.cosmetic },
        Spec(ShieldFlag.HTTPS, "Upgrade to HTTPS", "Prefers encrypted connections; warns before using plain HTTP") { it.https != HttpsPolicy.Mode.OFF },
        Spec(ShieldFlag.COOKIES, "Block cross-site cookies", "Stops sites from following you through embedded content") { it.blockThirdPartyCookies },
        Spec(ShieldFlag.FINGERPRINT, "Block fingerprinting", "Adds noise to canvas/audio readouts used to identify this device") { it.blockFingerprinting },
        if (app.kestrel.browser.Flavor.hasEmbedWait) Spec(ShieldFlag.EMBEDS, "Embeds wait for a tap", "Video and social players load when you tap them: the page opens faster and nothing in them runs unseen") { it.waitEmbeds } else null,
        Spec(ShieldFlag.SCRIPTS, "Block scripts", "Turns JavaScript off on this site (many pages stop working)") { it.blockScripts },
    )

    /** The panel as a bottom sheet (the Gecko build: the navigation is at the bottom, so this rises from the bottom too). */
    fun showIn(sheet: SheetHost, host: String, tab: Tab, global: ShieldSettings) {
        sheet.show("Shields", content(host, tab, global))
    }

    /** The panel as a drop-down under [anchor] (the WebView build, whose toolbar is at the top). */
    fun show(anchor: View, host: String, tab: Tab, global: ShieldSettings) {
        val col = content(host, tab, global)
        val width = minOf(ctx.dp(340), ctx.resources.displayMetrics.widthPixels - ctx.dp(16))
        val popup = PopupWindow(ScrollView(ctx).apply { addView(col) }, width, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popup.setBackgroundDrawable(roundedBg(pal.card, ctx.dpf(14f), pal.divider, ctx.dp(1)))
        popup.elevation = ctx.dpf(12f)
        popup.isOutsideTouchable = true
        popup.animationStyle = 0   // no fade: everything must feel instant
        popup.showAsDropDown(anchor, -(width - anchor.width), 0)
    }

    private fun content(host: String, tab: Tab, global: ShieldSettings): LinearLayout {
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(ctx.dp(18), ctx.dp(8), ctx.dp(18), ctx.dp(14)) }
        var current = Shields.overrideFor(host) ?: SiteShields(0, 0)
        val effective = { current.applyTo(global) }
        val rows = ArrayList<Pair<Spec, Switch>>()

        val master = Switch(ctx).apply { isChecked = effective().shieldsUp }
        col.addView(row(label(ctx, "Shields for $host", 16f, pal.fg, bold = true).apply { maxLines = 2; includeFontPadding = true }, null, master))

        val blocked = tab.blocked
        val lines = ArrayList<String>()
        val ads = blocked.get(Category.ADS); val trk = blocked.get(Category.PRIVACY); val mal = blocked.get(Category.MALWARE)
        if (ads + trk > 0) lines.add("${ads + trk} ads & trackers blocked on this page")
        if (mal > 0) lines.add("$mal harmful requests blocked")
        if (tab.pageAllowed) lines.add("This site is on the lists' allow list")
        col.addView(label(ctx, if (lines.isEmpty()) "Nothing blocked on this page yet" else lines.joinToString("\n"), 13f, pal.muted).apply { setPadding(0, ctx.dp(6), 0, ctx.dp(10)); includeFontPadding = true })

        fun refreshDim() { val up = master.isChecked; for ((_, sw) in rows) { sw.isEnabled = up; (sw.parent as View).alpha = if (up) 1f else 0.4f } }
        fun commit(flag: Int, value: Boolean, global: Boolean) {
            current = if (value == global) current.without(flag) else current.with(flag, value)
            Shields.setOverride(host, current)
            onChanged(host)
        }

        master.setOnCheckedChangeListener { _, checked -> commit(ShieldFlag.SHIELDS, checked, global.shieldsUp); refreshDim() }
        for (s in specs) {
            val sw = Switch(ctx)
            sw.isChecked = when (s.flag) {
                ShieldFlag.ADS -> effective().blockAds
                ShieldFlag.COSMETIC -> effective().cosmetic
                ShieldFlag.HTTPS -> effective().https != HttpsPolicy.Mode.OFF
                ShieldFlag.COOKIES -> effective().blockThirdPartyCookies
                ShieldFlag.FINGERPRINT -> effective().blockFingerprinting
                ShieldFlag.EMBEDS -> effective().waitEmbeds
                else -> effective().blockScripts
            }
            sw.setOnCheckedChangeListener { _, checked -> commit(s.flag, checked, s.globalValue(global)) }
            rows.add(s to sw)
            col.addView(row(label(ctx, s.title, 15f, pal.fg).apply { includeFontPadding = true }, s.subtitle, sw))
        }
        refreshDim()
        col.addView(label(ctx, "Changes reload the page. These are this site's settings; defaults are in Settings." + (if (app.kestrel.browser.Flavor.hasEmbedWait) " With Shields off, Firefox's own tracker blocking is off for this site too." else ""), 12f, pal.muted).apply { setPadding(0, ctx.dp(8), 0, 0); includeFontPadding = true })
        return col
    }

    private fun row(title: View, subtitle: String?, sw: Switch): View {
        val r = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, ctx.dp(6), 0, ctx.dp(6)); minimumHeight = ctx.dp(48) }
        val text = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        text.addView(title)
        if (subtitle != null) text.addView(label(ctx, subtitle, 12f, pal.muted).apply { setPadding(0, ctx.dp(2), 0, 0); includeFontPadding = true })
        r.addView(text, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        r.addView(sw)
        return r
    }
}
