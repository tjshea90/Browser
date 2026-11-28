package app.kestrel.browser.web

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.hardware.SensorManager
import android.os.Build
import android.os.SystemClock
import android.view.OrientationEventListener
import android.view.View
import app.kestrel.browser.Io
import app.kestrel.browser.diag.Health
import app.kestrel.core.media.FullscreenTurn

/**
 * FS1 (owner message 43): "if I watch videos in full screen, then rotate the tablet or press a rotate button, the video switches to landscape mode just like regular video player apps".
 *
 * Applies the pure rules of [FullscreenTurn] with `Activity.requestedOrientation` while a page is full screen: the screen FOLLOWS THE SENSOR (Android's "full sensor" ignores the system rotation lock,
 * so turning the tablet turns the picture even with auto-rotate off), a wide video starts sideways when the tablet is held upright, a site's own `screen.orientation.lock()` wins while it lasts, the
 * small rotate button turns the screen the other way, and leaving full screen puts back what the activity had. A one-time turn is let go as soon as the tablet really is held that way (orientation
 * sensor, only while a turn is waiting to be let go), so turning the tablet back turns the picture back, as in a player app. UI thread only.
 *
 * Why it did not work before: the activity never asked for any orientation (so Android's rotation lock decided), full screen hides the system bars (so the bar's own rotate button was out of reach), and
 * GeckoView's `OrientationDelegate` had no answerer (so a site's lock request did nothing). Design and proof: research/VIDEO_SMOOTHNESS.md section "FS1".
 */
class FullscreenTurner(
    private val a: Activity,
    /** The Settings choice (0 automatic, 1 follow the tablet only, 2 only what a site asks for). */
    private val mode: () -> Int,
    /** The picture of the video that plays on the front tab, "0 x 0" when nobody knows yet. */
    private val picture: () -> Pair<Int, Int>,
    /** The front tab is a private tab: its film's size is not written down (review of alpha56: a private tab keeps no line about its page). */
    private val isPrivate: () -> Boolean = { false },
) {
    private fun shape(w: Int, h: Int): String = if (isPrivate()) (if (w > 0 && h > 0) "size known" else "size unknown") else "${w}x$h"

    var on = false; private set
    private var saved = FullscreenTurn.UNSPECIFIED     // what the activity asked for before full screen
    private var requested = FullscreenTurn.UNSPECIFIED // what we asked for last (Android's number)
    private var siteLock: Int? = null                  // the page's own screen.orientation.lock, in force now
    private var pendingSite: Int? = null               // asked a moment before the engine said "full screen": kept for 3 s
    private var pendingAt = 0L
    private var armed = false                          // a one-time turn waits for the tablet to be held that way
    private var sensor: OrientationEventListener? = null
    private var button: RotateButton? = null
    private var lastDegrees = -1                       // the newest reading of the orientation sensor
    private var releasePending = false                 // the tablet is held the way a one-time turn asked for; the hand-back waits for it to stay so for [RELEASE_SETTLE_MS]
    private val releaseNow = Runnable { releaseIfStillHeld() }

    // what Diagnostics says
    private var fullScreens = 0
    private var turnedByUs = 0
    private var bySite = 0
    private var byButton = 0
    private var handedBack = 0
    private var last = "no full screen yet"

    private val hideButton = Runnable { button?.let { b -> b.animate().cancel(); b.animate().alpha(0f).setDuration(220L).withEndAction { b.visibility = View.GONE }.start() } }

    fun attachButton(b: RotateButton) { button = b; b.visibility = View.GONE; b.setOnClickListener { rotatePressed() } }

    // ---------------------------------------------------------------------------------------- the page's full screen starts and ends

    fun enter() {
        if (on) return
        on = true
        saved = a.requestedOrientation
        fullScreens++
        siteLock = pendingSite?.takeIf { SystemClock.uptimeMillis() - pendingAt < PENDING_MS }
        pendingSite = null
        val (w, h) = picture()
        val plan = FullscreenTurn.onEnter(mode(), siteLock, w, h, screenLandscape())
        apply(plan.request, plan.release, "full screen starts (video ${shape(w, h)}, screen ${if (screenLandscape()) "landscape" else "portrait"}, setting ${mode()})")
        if (plan.request == FullscreenTurn.SENSOR_LANDSCAPE && siteLock == null) turnedByUs++
        showButton()
    }

    fun exit() {
        if (!on) return
        on = false; siteLock = null; pendingSite = null; armed = false
        cancelRelease()
        stopSensor()
        Io.main.removeCallbacks(hideButton)
        button?.let { it.animate().cancel(); it.visibility = View.GONE }
        setRequested(saved, "full screen ends: put back what Android had")
        requested = FullscreenTurn.UNSPECIFIED
    }

    /** The picture of the playing video became known after the full screen had begun (the page told us late): an automatic turn may still be right. */
    fun pictureKnown() {
        if (!on || siteLock != null || armed || requested != FullscreenTurn.FULL_SENSOR || FullscreenTurn.cleanMode(mode()) != FullscreenTurn.MODE_AUTO) return
        val (w, h) = picture()
        val plan = FullscreenTurn.onEnter(mode(), null, w, h, screenLandscape())
        if (plan.request != FullscreenTurn.FULL_SENSOR) { turnedByUs++; apply(plan.request, plan.release, "the video's picture became known (${shape(w, h)})") }
    }

    // ---------------------------------------------------------------------------------------- what the engine tells us (UI thread)

    /** GeckoView's OrientationDelegate: a page called screen.orientation.lock(...); [orientation] is an ANDROID orientation number (RV1). true = allowed. Only ever applied while full screen; a request that comes a moment before "full screen" is kept briefly. */
    fun siteLocked(orientation: Int): Boolean {
        val o = FullscreenTurn.fromSite(orientation)
        if (o == null) { Health.log("fullscreen", "site-lock-refused", "orientation=$orientation"); return false }
        if (!on) { pendingSite = o; pendingAt = SystemClock.uptimeMillis(); Health.log("fullscreen", "site-lock-early", "orientation=$orientation"); return true }
        siteLock = o; bySite++
        apply(o, false, "the page asked: screen.orientation.lock (orientation $orientation)")
        return true
    }

    fun siteUnlocked() {
        pendingSite = null
        if (!on || siteLock == null) return
        siteLock = null
        apply(base(), false, "the page let go of its lock")
    }

    // ---------------------------------------------------------------------------------------- the rotate button and touches

    fun rotatePressed() {
        if (!on) return
        val plan = FullscreenTurn.rotateButton(screenLandscape())
        byButton++; siteLock = null
        apply(plan.request, plan.release, "the rotate button")
        showButton()
    }

    /** A finger lifted while a page is full screen: the button shows again for a moment (like a player's controls). */
    fun touched() { if (on) showButton() }

    /** VP1: while Kestrel's own full-screen controls are on, the button shows and hides WITH them (a tap that hides the controls must not leave a button on the picture). */
    var ownedByPlayer: () -> Boolean = { false }

    fun controlledShow(show: Boolean) {
        val b = button ?: return
        Io.main.removeCallbacks(hideButton)
        b.animate().cancel()
        if (show && on && FullscreenTurn.cleanMode(mode()) != FullscreenTurn.MODE_OFF) { b.alpha = 1f; b.visibility = View.VISIBLE } else b.visibility = View.GONE
    }

    private fun showButton() {
        if (ownedByPlayer()) return
        val b = button ?: return
        if (FullscreenTurn.cleanMode(mode()) == FullscreenTurn.MODE_OFF) return // "off": only what a site asks for
        b.animate().cancel(); b.alpha = 1f; b.visibility = View.VISIBLE
        Io.main.removeCallbacks(hideButton); Io.main.postDelayed(hideButton, BUTTON_MS)
    }

    // ---------------------------------------------------------------------------------------- applying

    /** What to go back to when nothing special is asked: follow the tablet, or (setting "off") whatever Android had. */
    private fun base() = if (FullscreenTurn.cleanMode(mode()) == FullscreenTurn.MODE_OFF) FullscreenTurn.UNSPECIFIED else FullscreenTurn.FULL_SENSOR

    private fun apply(request: Int, release: Boolean, why: String) {
        setRequested(if (request == FullscreenTurn.UNSPECIFIED) saved else request, why)
        requested = request
        armed = release
        if (on && armed) startSensor() else stopSensor()
    }

    private fun setRequested(o: Int, why: String) {
        val was = a.requestedOrientation
        try { if (was != o) a.requestedOrientation = o } catch (e: Exception) { Health.log("fullscreen", "turn-failed", e.toString().take(80)) }
        last = "${FullscreenTurn.name(o)} - $why"
        Health.log("fullscreen", "turn", "to=$o (${FullscreenTurn.name(o)}) was=$was why=$why")
    }

    private fun startSensor() {
        val s = sensor ?: object : OrientationEventListener(a, SensorManager.SENSOR_DELAY_NORMAL) { override fun onOrientationChanged(degrees: Int) { moved(degrees) } }.also { sensor = it }
        if (s.canDetectOrientation()) s.enable() else { Health.log("fullscreen", "no-sensor"); armed = false } // no sensor: the turn stays (the rotate button turns it back)
    }

    private fun stopSensor() { sensor?.disable() }

    private fun moved(degrees: Int) {
        if (!on || !armed) return
        lastDegrees = degrees
        val posture = FullscreenTurn.postureLandscape(degrees, naturalPortrait())
        if (FullscreenTurn.release(requested, posture)) {
            // ROT2 (owner message 48; lab run 43763822027: two turns 0.45 s apart, a flash of PORTRAIT before the landscape): this listener sees the tablet's new position at once, Android's own rotation
            // sensing needs about half a second to agree, and a "follow the tablet" request that comes earlier makes the screen turn to the position Android still believes in. So the hand-back waits until
            // the tablet has stayed that way for a moment (a turn back meanwhile cancels it).
            if (!releasePending) { releasePending = true; Io.main.postDelayed(releaseNow, RELEASE_SETTLE_MS) }
        } else cancelRelease()
    }

    private fun cancelRelease() { releasePending = false; Io.main.removeCallbacks(releaseNow) }

    private fun releaseIfStillHeld() {
        releasePending = false
        if (!on || !armed || lastDegrees < 0) return
        val posture = FullscreenTurn.postureLandscape(lastDegrees, naturalPortrait())
        if (FullscreenTurn.release(requested, posture)) {
            armed = false; handedBack++
            stopSensor()
            apply(base(), false, "the tablet has been held that way for a moment: follow it again (sensor $lastDegrees)")
        }
    }

    /** The activity is leaving the screen: the sensor sleeps; it wakes again when the activity returns while a turn is still waiting. */
    fun paused() { cancelRelease(); stopSensor() }
    fun resumed() { if (on && armed) startSensor() }

    // ---------------------------------------------------------------------------------------- the screen as it is

    fun screenLandscape(): Boolean = a.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    /** The tablet's built-in position is portrait (this one: 600 x 1024): derived from the display's rotation and how the window is shaped now. */
    private fun naturalPortrait(): Boolean {
        @Suppress("DEPRECATION")
        val rot = if (Build.VERSION.SDK_INT >= 30) (a.display?.rotation ?: 0) else a.windowManager.defaultDisplay.rotation
        val portraitNow = !screenLandscape()
        return if (rot == 0 || rot == 2) portraitNow else !portraitNow
    }

    /** One Diagnostics line. */
    fun line(): String {
        val m = listOf("Automatic", "follow the tablet only", "off (only what a site asks)")[FullscreenTurn.cleanMode(mode())]
        return "Full-screen turning ($m): $fullScreens full screens, turned sideways by Kestrel $turnedByUs, by the page's own request $bySite, by the rotate button $byButton, handed back to the tablet $handedBack; last: $last"
    }

    // ---------------------------------------------------------------------------------------- the button

    /** 52 dp round, dark glass with a white phone and a turning arrow; drawn here (no picture files). Shown for 3.5 s after every touch while a page is full screen. */
    class RotateButton(ctx: Context) : View(ctx) {
        private val d = ctx.resources.displayMetrics.density
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x99000000.toInt() }
        private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55FFFFFF; style = Paint.Style.STROKE; strokeWidth = 1f * d }
        private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.STROKE; strokeWidth = 2f * d; strokeCap = Paint.Cap.ROUND }
        private val head = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.FILL }
        private val tri = Path()

        init { contentDescription = "Rotate the screen"; isClickable = true; isFocusable = false }

        override fun onMeasure(w: Int, h: Int) { val s = (SIZE_DP * d).toInt(); setMeasuredDimension(s, s) }

        override fun onDraw(c: Canvas) {
            val cx = width / 2f; val cy = height / 2f; val r = width / 2f - 1f * d
            c.drawCircle(cx, cy, r, fill); c.drawCircle(cx, cy, r, edge)
            // the phone (upright), then an arrow that goes round it
            val pw = 5.5f * d; val ph = 9f * d
            c.drawRoundRect(RectF(cx - pw, cy - ph, cx + pw, cy + ph), 2f * d, 2f * d, ink)
            val ar = 15f * d; val start = 215f; val sweep = 250f
            c.drawArc(RectF(cx - ar, cy - ar, cx + ar, cy + ar), start, sweep, false, ink)
            val e = Math.toRadians((start + sweep).toDouble()); val cs = Math.cos(e).toFloat(); val sn = Math.sin(e).toFloat()
            val px = cx + ar * cs; val py = cy + ar * sn           // the arc's end
            val len = 5.5f * d; val wid = 4f * d
            tri.reset()
            tri.moveTo(px - sn * len, py + cs * len)                // tip: forward (clockwise) along the arc
            tri.lineTo(px + cs * wid, py + sn * wid)
            tri.lineTo(px - cs * wid, py - sn * wid)
            tri.close()
            c.drawPath(tri, head)
        }

        companion object { const val SIZE_DP = 52 }
    }

    companion object {
        /** A site's lock that arrives before the engine's "full screen" is kept this long. */
        const val PENDING_MS = 3000L
        /** The rotate button stays up this long after a touch. */
        const val BUTTON_MS = 3500L
        /** ROT2: a one-time turn is handed back to the tablet only after the tablet has stayed in that position this long (Android's own rotation sensing needs about half a second to agree). */
        const val RELEASE_SETTLE_MS = 650L
    }
}
