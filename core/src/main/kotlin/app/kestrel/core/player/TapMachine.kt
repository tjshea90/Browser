package app.kestrel.core.player

/**
 * VP2 / ANS53c (owner message 53: "when I press on the video to bring up the player controls, there is a delay. Make them pop up immediately, zero delay. When I press on a video that is playing, the
 * controls should come up instantly. When I press on the video again, the controls should disappear instantly."): single tap, double tap on a side and the burst after it, pure and testable with a fake clock.
 *
 *  - EVERY tap toggles the controls at once ([Host.single]): nothing waits for a possible second tap (until alpha56 the first tap waited the 300 ms double-tap window, and on a busy page a message from the
 *    page script came on top: the owner's "there is a delay");
 *  - the second tap of a double tap on the LEFT or RIGHT side (within [PlayerRules.DOUBLE_TAP_MS] of the first, same side) is the seek: backwards / forwards by [PlayerRules.SKIP_S] (burst 1) instead of a second toggle. The
 *    first tap had toggled the controls already, and they stay as that left them (hidden by the first tap while they showed, so the "10 s" flash is alone on the picture; shown by it otherwise);
 *  - in the MIDDLE there is no double-tap meaning any more (it used to play / pause): two quick taps are two toggles, the owner's own test of "instant, then instantly away" never pauses the film by accident;
 *  - after a seek, every further tap on that side within [PlayerRules.BURST_MS] seeks again at once (burst 2, 3 ...), like YouTube, and does not toggle.
 */
class TapMachine(private val h: Host) {
    interface Host {
        fun now(): Long
        /** The controls toggle (shown <-> hidden), at once. */
        fun single()
        /** [zone] LEFT = backwards, RIGHT = forwards; [burst] = 1 for the double tap itself, 2, 3 ... for the taps that follow. */
        fun seek(zone: PlayerRules.Zone, burst: Int)
    }

    private var lastAt = -1L
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
        if (lastAt >= 0L && n - lastAt <= PlayerRules.DOUBLE_TAP_MS && z == lastZone && z != PlayerRules.Zone.CENTER) {   // the second tap of a double tap on a side
            lastAt = -1L
            burstZone = z; burstCount = 1; burstUntil = n + PlayerRules.BURST_MS
            h.seek(z, 1)
            return
        }
        lastAt = n; lastZone = z
        h.single()
    }

    /** Full screen ended: nothing remembered. */
    fun reset() { lastAt = -1L; burstUntil = 0L; burstCount = 0 }
}
