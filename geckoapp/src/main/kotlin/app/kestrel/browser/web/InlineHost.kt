package app.kestrel.browser.web

import android.os.SystemClock
import android.view.MotionEvent
import app.kestrel.browser.BrowserActivity
import app.kestrel.browser.NativePlayerActivity
import app.kestrel.browser.diag.Health
import app.kestrel.core.media.InlineBox
import org.mozilla.geckoview.GeckoSession

/**
 * ANS55f (owner message 55: the player inside the website): the owner of the [InlinePlayer] in the page area. It knows which tab's page the film belongs to, follows that page's scroll with the engine's COMPOSITOR
 * updates (GeckoSession.CompositorScrollDelegate: the scroll in CSS pixels and the zoom, called as soon as the scroll is composited; no message from the page's script, which a busy page would delay) through the pure rules
 * of core [InlineBox.Track], and ends the film whenever the page it plays over is no longer what the person sees (another tab or page, the Home screen, the app left, the screen turned, a pinch, the box scrolled out
 * of view, Back). Never more than one. Everything here runs on the UI thread.
 */
class InlineHost(private val a: BrowserActivity, private val engine: KestrelGeckoView) : InlinePlayer.Listener {
    val view = InlinePlayer(a).also { it.host = this }
    /** ANS60c (owner message 60: the quality list in the regular player size): drawn over the whole page area (the film's box is too small for it), above the film; a pick goes to the film, Back and a tap outside close it. */
    val sheet = QualitySheet(a).apply { listener = object : QualitySheet.Listener { override fun onPick(row: QualitySheet.Row) { view.pickQuality(row) } override fun onDismiss() { view.qualityClosed() } } }
    private var tab: Tab? = null
    private var session: GeckoSession? = null
    private var track: InlineBox.Track? = null
    private var box: InlineBox.Rect? = null
    private var headers: List<String> = emptyList()
    private var playing = ""                                       // the address of the film that is up (for the full-screen button)
    private var pageUrl = ""                                       // the page it lies over (without #fragment): another address without a page load ends it
    private var priv = false
    private var lastLogAt = 0L
    private var updates = 0

    val active: Boolean get() = view.active
    val url: String get() = view.url
    val tabOrNull: Tab? get() = tab

    private val scrollDelegate = object : GeckoSession.CompositorScrollDelegate {
        override fun onScrollChanged(session: GeckoSession, update: GeckoSession.ScrollPositionUpdate) { onScroll(session, update) }
    }

    fun viewSize(): String = "${engine.width}x${engine.height}"

    /** Where [page] sits in the view now (null: it cannot be told: the film plays in the full-screen screen). */
    fun boxFor(page: InlineBox.Page): InlineBox.Rect? = InlineBox.toView(page, engine.width, engine.height)

    /** Plays [url] over [boxNow] of [tab]'s page. false = it did not start (the caller opens the full-screen screen). */
    fun start(tab: Tab, url: String, headerLines: List<String>, startMs: Long, page: InlineBox.Page, boxNow: InlineBox.Rect): Boolean {
        val s = tab.session ?: return false
        if (view.active) view.stop("a new film")
        this.tab = tab; this.session = s; this.headers = headerLines; this.priv = tab.isPrivateTab
        this.box = boxNow; this.track = InlineBox.Track(boxNow, page.pageX, page.pageY); this.playing = url; this.pageUrl = tab.url.substringBefore('#')
        updates = 0; lastLogAt = 0L
        try { s.compositorScrollDelegate = scrollDelegate } catch (e: Throwable) { if (!tab.isPrivateTab) Health.log("inline", "scroll-delegate-failed", e.toString().take(80)); cleanup(); return false }
        val cap = 480                                                                                          // a box is small: 480p is plenty and a slow link thanks it (a size the person picked in the list inside the page is remembered and wins)
        if (!view.start(url, headerLines, startMs, tab.isPrivateTab, boxNow, cap)) { cleanup(); return false }
        a.inlineChanged()
        if (!tab.isPrivateTab) Health.log("inline", "placed", "page box ${page.w.toInt()}x${page.h.toInt()} css at ${page.x.toInt()},${page.y.toInt()} scroll ${page.pageX.toInt()},${page.pageY.toInt()} -> view $boxNow in ${engine.width}x${engine.height}")
        return true
    }

    /** Ends the film for [why] (a log word); a film that is not up is left alone. */
    fun stop(why: String) { if (view.active) view.stop(why) }

    /** The page's address changed without a page load (a single-page site, a #link): the film lay over the old content. */
    fun addressChanged(t: Tab, url: String) { if (view.active && tab === t && url.substringBefore('#') != pageUrl) view.stop("the page's address changed") }

    /** Is the screen point [rawX], [rawY] on the film inside the page? */
    fun covers(rawX: Float, rawY: Float): Boolean {
        if (!view.active) return false
        val loc = IntArray(2); view.getLocationOnScreen(loc)
        return rawX >= loc[0] && rawX <= loc[0] + view.width && rawY >= loc[1] && rawY <= loc[1] + view.height
    }

    /** Back closes the quality list first, then the film inside the page (true = it closed one of them). */
    fun backPressed(): Boolean {
        if (sheet.isOpen) { sheet.dismiss(); return true }
        if (!view.active) return false
        view.stop("Back"); return true
    }

    override fun openQuality(rows: List<QualitySheet.Row>) {
        sheet.show("Quality", rows, "Kestrel remembers your pick for the next films inside a page. A smaller picture and a lower frame rate are lighter for this tablet and for the hotspot.")
        if (!priv) sheet.post { Health.log("inline", "quality-rows", "rows at ${sheet.rowCentres().toList().chunked(2).joinToString(" ") { "${it[0]},${it[1]}" }} (screen pixels)") }     // (the emulator lab taps them)
    }
    override fun closeQuality() { sheet.dismiss() }
    override fun qualityOpen(): Boolean = sheet.isOpen

    private fun cleanup() {
        try { session?.compositorScrollDelegate = null } catch (e: Throwable) { /* the session is gone */ }
        session = null; tab = null; track = null; box = null
    }

    override fun onEnded(why: String, positionMs: Long, fullscreen: Boolean, failed: Boolean) {
        sheet.dismiss()                                                    // (a list of a film that is gone)
        val t = tab; val u = playing; val h = headers; val p = priv
        cleanup()
        a.inlineChanged()
        if (!p) { WebHandStats.lastInlineEnd = why; if (fullscreen) WebHandStats.inlineFull++ }
        if (fullscreen && u.isNotEmpty() && t != null && a.current === t) {
            // the film goes on in the full-screen screen at the same second (it is its own window: Back returns to the page, where the person presses play again)
            NativePlayerActivity.launch(a, u, h, positionMs, if (PlayerFactory.isHls(u)) 720 else 1080, p, 0, 0, fromPage = true)
        } else if (failed) {
            a.nativeFilmFailed()                                              // the page's own player plays its films for two minutes, and the person is told
        }
    }

    override fun onLongPress() { val t = tab ?: return; a.inlineLongPress(t, playing) }

    override fun toPage(e: MotionEvent) {
        val c = MotionEvent.obtain(e)
        try {
            c.offsetLocation(view.left + view.translationX - (engine.left + engine.translationX), view.top + view.translationY - (engine.top + engine.translationY))
            engine.dispatchTouchEvent(c)
        } finally { c.recycle() }
    }

    // the place the overlay came to rest at (350 ms after the last update): one line for the diagnostics and the emulator lab, whatever the rate limit of the 'scroll' lines skipped
    private val settled = Runnable { val b = box; if (b != null && view.active && !priv) Health.log("inline", "scroll-settled", "view $b") }

    private fun onScroll(s: GeckoSession, u: GeckoSession.ScrollPositionUpdate) {
        if (!view.active || s !== session) return
        val t = track ?: return
        val b = t.at(u.scrollX.toDouble(), u.scrollY.toDouble(), u.zoom.toDouble())
        val now = SystemClock.uptimeMillis()
        if (b == null) { if (!priv) Health.log("inline", "scroll-lost", "scroll ${u.scrollX},${u.scrollY} zoom ${u.zoom}: the page was pinched or the box cannot be told"); view.stop("the page was pinched or its scroll could not be followed"); return }
        box = b
        view.move(b)
        view.removeCallbacks(settled); view.postDelayed(settled, 350L)
        if (!priv && (updates++ < 4 || now - lastLogAt > 250L)) { lastLogAt = now; Health.log("inline", "scroll", "scroll ${"%.1f".format(u.scrollX)},${"%.1f".format(u.scrollY)} zoom ${"%.3f".format(u.zoom)} src ${u.source} -> view $b") }
        view.edge(InlineBox.edgeFor(b, engine.width, engine.height))
    }
}
