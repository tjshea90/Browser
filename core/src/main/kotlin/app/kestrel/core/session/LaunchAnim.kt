package app.kestrel.core.session

/**
 * SP1 (owner message 41, item 3): "when Kestrel first loads, it shows a shield icon. Instead of this shield, make it match the symbol from the app drawer. If possible, give it a very quick lightweight animation, such as
 * spreading wings or glowing neon or something cool but very fast so I don't have to wait."
 * The curves of the short logo flourish at the start of the app: the app's own symbol glows once and fades while the screen is revealed under it. [t] is the share of [TOTAL_MS] that has passed (0..1); every value is 0..1
 * (or a size factor). The owner runs Android with animations turned OFF, so the view that draws this keeps its own clock instead of a system animator. Pure, no Android: LaunchAnimTest.
 */
object LaunchAnim {
    /** The whole flourish. Short on purpose: nothing waits for it, it plays while the screen comes up. */
    const val TOTAL_MS = 380L

    private fun clamp(v: Float): Float = if (v < 0f) 0f else if (v > 1f) 1f else v
    private fun smooth(v: Float): Float { val x = clamp(v); return x * x * (3f - 2f * x) }

    /** How black the cover over the screen still is: full for the first moments (the launch window was black), then it clears and the screen shows. */
    fun cover(t: Float): Float = 1f - smooth((t - 0.18f) / 0.62f)

    /** The symbol's own opacity: full while it glows, gone with the cover. */
    fun logoAlpha(t: Float): Float = 1f - smooth((t - 0.5f) / 0.5f)

    /** The neon halo behind the symbol: it swells up to its peak a little before half time and fades out. */
    fun glow(t: Float): Float {
        val x = clamp(t)
        return if (x < 0.38f) smooth(x / 0.38f) else 1f - smooth((x - 0.38f) / 0.62f)
    }

    /** The symbol's size relative to the launch window's picture: it settles in from a touch smaller (the window showed it at 1.0) and opens out a little as it fades. */
    fun scale(t: Float): Float {
        val x = clamp(t)
        return 1f + 0.07f * smooth(x) - 0.03f * (1f - smooth(x / 0.35f))
    }

    /** The halo's radius relative to the symbol's half size. */
    fun haloRadius(t: Float): Float = 0.9f + 0.7f * smooth(clamp(t))
}
