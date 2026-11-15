package app.kestrel.core.player

/**
 * VP2: single tap, double tap and the burst of taps after a double tap on the picture, pure and testable with a fake clock.
 *
 *  - one tap: after [PlayerRules.DOUBLE_TAP_MS] with no second tap, [Host.single] (the controls toggle). The wait is why a double tap never flashes the controls (Chrome and YouTube do the same);
 *  - two taps within the window on the same side: left / right third = [Host.seek] backwards / forwards by [PlayerRules.SKIP_S] (burst 1), the middle third = [Host.playPause];
 *  - after a seek, every further tap on that side within [PlayerRules.BURST_MS] seeks again at once (burst 2, 3 ...), like YouTube.
 */
class TapMachine(private val h: Host) {
    interface Host {
        fun now(): Long
        fun schedule(delayMs: Long)
        fun cancel()
        fun single()
        /** [zone] LEFT = backwards, RIGHT = forwards; [burst] = 1 for the double tap itself, 2, 3 ... for the taps that follow. */
        fun seek(zone: PlayerRules.Zone, burst: Int)
        fun playPause()
    }

    private var pending = false
    private var lastAt = 0L
    private var lastZone = PlayerRules.Zone.CENTER
    private var burstUntil = 0L
    private var burstZone = PlayerRules.Zone.CENTER
    private var burstCount = 0

    /** A tap at [xFraction] (0..1 of the picture's width). */
    fun tap(xFraction: Double) {
        val n = h.now()
        val z = PlayerRules.zone(xFraction)
        if (n < burstUntil && z == burstZone && z != PlayerRules.Zone.CENTER) {          // the burst goes on
            burstCount++; burstUntil = n + PlayerRules.BURST_MS
            h.seek(z, burstCount)
            return
        }
        if (pending && n - lastAt <= PlayerRules.DOUBLE_TAP_MS && z == lastZone) {      // the second tap of a double tap
            pending = false; h.cancel()
            if (z == PlayerRules.Zone.CENTER) { h.playPause(); return }
            burstZone = z; burstCount = 1; burstUntil = n + PlayerRules.BURST_MS
            h.seek(z, 1)
            return
        }
        if (pending) { pending = false; h.cancel(); h.single() }                           // a tap on another side: the first one was a single tap after all
        pending = true; lastAt = n; lastZone = z
        h.schedule(PlayerRules.DOUBLE_TAP_MS)
    }

    /** The timer the host was asked for. */
    fun tick() { if (pending) { pending = false; h.single() } }

    /** Full screen ended: nothing pending. */
    fun reset() { pending = false; burstUntil = 0L; burstCount = 0; h.cancel() }
}
