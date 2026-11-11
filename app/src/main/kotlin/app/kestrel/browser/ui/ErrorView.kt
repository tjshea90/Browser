package app.kestrel.browser.ui

import android.content.Context
import android.view.Gravity
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import app.kestrel.browser.R
import app.kestrel.browser.net.NetworkMonitor
import app.kestrel.browser.web.ErrorScreen
import app.kestrel.core.filter.Category
import app.kestrel.core.url.UrlParts

/**
 * Native error / warning screen drawn over the page area. Native (not an HTML page) so it needs no network, no scripts, cannot be
 * spoofed by a website, and can distinguish the hotspot situations that matter on this tablet.
 */
class ErrorView(ctx: Context, private val pal: Pal, private val l: Listener) : ScrollView(ctx) {
    interface Listener {
        fun retry()
        fun openSavedCopy()
        fun backToSafety()
        fun proceedUnsafeSsl(host: String)
        fun continueHttp(httpUrl: String, host: String)
        fun proceedDangerous(url: String, host: String)
        fun openWifiSettings()
    }

    private val box = LinearLayout(ctx)

    init {
        setBackgroundColor(pal.windowBg)
        isFillViewport = true
        box.orientation = LinearLayout.VERTICAL
        box.gravity = Gravity.CENTER_HORIZONTAL
        box.setPadding(ctx.dp(28), ctx.dp(56), ctx.dp(28), ctx.dp(28))
        addView(box, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    fun show(e: ErrorScreen) {
        val ctx = context
        box.removeAllViews()
        val (title, body, details) = texts(e)
        val danger = e is ErrorScreen.Ssl || e is ErrorScreen.Dangerous || e is ErrorScreen.HttpsUnavailable
        box.addView(ImageView(ctx).apply {
            setImageResource(R.drawable.ic_warning)
            setColorFilter(if (danger) pal.danger else pal.muted)
            layoutParams = LinearLayout.LayoutParams(ctx.dp(56), ctx.dp(56))
        })
        box.addView(label(ctx, title, 22f, if (danger) pal.danger else pal.fg, bold = true).apply {
            gravity = Gravity.CENTER; setPadding(0, ctx.dp(20), 0, ctx.dp(10)); includeFontPadding = true
        })
        box.addView(label(ctx, body, 15f, pal.fg).apply { gravity = Gravity.CENTER; includeFontPadding = true; setLineSpacing(0f, 1.15f) })
        if (details.isNotEmpty()) {
            box.addView(label(ctx, details, 12f, pal.muted).apply { gravity = Gravity.CENTER; setPadding(0, ctx.dp(14), 0, 0); includeFontPadding = true })
        }
        val actions = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; setPadding(0, ctx.dp(28), 0, 0) }
        fun primary(text: String, f: () -> Unit) = actions.addView(pillButton(ctx, text, pal.accent, pal.accentOn, f))
        fun secondary(text: String, f: () -> Unit) = actions.addView(textButton(ctx, text, if (danger) pal.danger else pal.muted, f))
        when (e) {
            is ErrorScreen.Network -> {
                primary("Try again") { l.retry() }
                val net = NetworkMonitor.state
                // OFFLINE already tried the saved copy first; for "Android says no internet" it is the user's call (the guess may be wrong).
                if (net == NetworkMonitor.State.NO_INTERNET) secondary("Open saved copy") { l.openSavedCopy() }
                if (net != NetworkMonitor.State.ONLINE) secondary("Wi-Fi settings") { l.openWifiSettings() }
            }
            is ErrorScreen.Ssl -> {
                primary("Back to safety") { l.backToSafety() }
                secondary("Proceed to ${e.host} (unsafe)") { l.proceedUnsafeSsl(e.host) }
            }
            is ErrorScreen.HttpsUnavailable -> {
                primary("Go back") { l.backToSafety() }
                secondary("Continue to the HTTP site (unsafe)") { l.continueHttp(e.httpUrl, e.host) }
            }
            is ErrorScreen.Dangerous -> {
                primary("Back to safety") { l.backToSafety() }
                secondary("Visit ${e.host} anyway (unsafe)") { l.proceedDangerous(e.url, e.host) }
            }
            is ErrorScreen.Crashed -> primary("Reload") { l.retry() }
        }
        box.addView(actions)
    }

    private fun texts(e: ErrorScreen): Triple<String, String, String> = when (e) {
        is ErrorScreen.Network -> networkTexts(e)
        is ErrorScreen.Ssl -> Triple(
            "Your connection isn't private",
            "${e.summary}\n\nSomeone on the network between this tablet and ${e.host} (this includes a Wi-Fi hotspot and the mobile carrier) may be trying to read or change what you send.",
            "Site: ${e.host}" + if (e.issuer.isNotEmpty()) "\nIssued by: ${e.issuer}" else "",
        )
        is ErrorScreen.HttpsUnavailable -> Triple(
            "No secure connection",
            "${e.host} doesn't support HTTPS. Anyone between this tablet and the site, including your mobile carrier, could read or alter this page and anything you type into it.",
            "",
        )
        is ErrorScreen.Dangerous -> Triple(
            "Dangerous site blocked",
            "Kestrel blocked ${e.host} because it is on a list of sites that ${if (e.category == Category.MALWARE) "spread malware or run phishing scams" else "are known to be harmful"}.",
            "",
        )
        is ErrorScreen.Crashed -> Triple("This page stopped working", "The tablet ran low on memory and the page was closed. Reloading usually works; close some tabs if it keeps happening.", "")
    }

    private fun networkTexts(e: ErrorScreen.Network): Triple<String, String, String> {
        val host = UrlParts.hostOf(e.failingUrl) ?: e.failingUrl.substringAfter("://").substringBefore('/').substringAfterLast('@').substringBefore(':')
        val detail = "$host  ·  ${e.description.ifEmpty { "error ${e.code}" }}"
        return when (NetworkMonitor.state) {
            NetworkMonitor.State.OFFLINE -> Triple(
                "No Wi-Fi connection",
                "This tablet isn't connected to a network. Turn on the hotspot on your phone, then connect this tablet to it. Pages you already visited can still open from the saved copy.",
                detail,
            )
            NetworkMonitor.State.NO_INTERNET -> Triple(
                "Can't reach this page",
                "The tablet is connected to Wi-Fi, but Android says no internet is getting through. Check that your phone has mobile data and signal and that its hotspot is on. If other pages load, this site may simply be down. This page reloads by itself when Android confirms the connection.",
                detail,
            )
            NetworkMonitor.State.ONLINE -> when (e.code) {
                WebViewClient.ERROR_HOST_LOOKUP -> Triple("Can't find that website", "Check the address for typos. If it is spelled right, the site may be down or your connection's DNS may be having trouble.", detail)
                WebViewClient.ERROR_TIMEOUT, WebViewClient.ERROR_CONNECT, WebViewClient.ERROR_IO ->
                    Triple("The website isn't responding", "The site took too long to answer. On a hotspot this is often a weak mobile signal. Try again in a moment.", detail)
                WebViewClient.ERROR_REDIRECT_LOOP -> Triple("Too many redirects", "This website keeps sending you in circles. Clearing its cookies may help.", detail)
                WebViewClient.ERROR_BAD_URL -> Triple("That address isn't valid", "Check the address and try again.", detail)
                WebViewClient.ERROR_UNSUPPORTED_SCHEME -> Triple("Can't open this link", "This kind of link isn't supported by the browser.", detail)
                else -> Triple("This page can't be loaded", "Something went wrong while loading the page. Trying again often fixes it.", detail)
            }
        }
    }
}
