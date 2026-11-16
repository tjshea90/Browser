package app.kestrel.browser.web

/**
 * ANS52i (research/VIDEO_ACCELERATION.md plan item 6): what Kestrel's OWN player (NativePlayerActivity: Media3 ExoPlayer on a SurfaceView, the hardware decoder, no page in the picture's way) did, for Diagnostics. Numbers
 * and a decoder name only: never an address, nothing for a private tab's film (the activity does not count those). One process, so plain fields; written on the main thread, read by the Diagnostics screen.
 */
object NativePlayerStats {
    @Volatile var plays = 0
    @Volatile var errors = 0
    @Volatile var framesShown = 0L
    @Volatile var framesDropped = 0L
    @Volatile var firstPictureMs = -1L
    @Volatile var decoder = ""
    @Volatile var decoderHardware: Boolean? = null
    @Volatile var width = 0
    @Volatile var height = 0
    @Volatile var playedMs = 0L
    @Volatile var last = "not used yet (Save video sheet > Play in Kestrel's player)"

    fun line(): String {
        if (plays == 0 && errors == 0) return "Kestrel's own player: $last"
        val pct = if (framesShown <= 0L) "-" else { val t = (framesDropped * 1000 + framesShown / 2) / framesShown; "${t / 10}.${t % 10} %" }
        val dec = if (decoder.isEmpty()) "no decoder yet" else decoder + when (decoderHardware) { true -> " (hardware)"; false -> " (software)"; else -> "" }
        return "Kestrel's own player: $plays plays, $errors errors; decoder $dec; last picture ${width}x$height, first picture ${if (firstPictureMs >= 0) "$firstPictureMs ms" else "-"} after the start; " +
            "${playedMs / 1000} s played, $framesShown frames shown, $framesDropped dropped ($pct); last: $last"
    }
}
