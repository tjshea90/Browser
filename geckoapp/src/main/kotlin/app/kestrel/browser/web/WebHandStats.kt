package app.kestrel.browser.web

/**
 * ANS54e (owner message 54: "I want the web video players to use this"): what became of the questions the pages asked ("may Kestrel's own player take this film?"): numbers and short words only, never an
 * address, nothing for a private tab. One process, plain fields, written on the main thread, read by the Diagnostics screen.
 */
object WebHandStats {
    @Volatile var asked = 0
    @Volatile var byPlay = 0
    @Volatile var byPress = 0
    @Volatile var byKestrel = 0
    @Volatile var byFullscreen = 0
    @Volatile var late = 0
    @Volatile var repeats = 0          // asked again while Kestrel's player was up or had just been started: answered yes without a second player
    @Volatile var declined = 0
    @Volatile var failed = 0
    @Volatile var lastDeclined = ""
    @Volatile var lastTaken = ""

    fun line(mode: Int): String {
        val m = when (mode) { 0 -> "Kestrel's player whenever it can"; 1 -> "Kestrel's player in full screen only"; else -> "the page's own player" }
        val taken = byPlay + byPress + byKestrel + byFullscreen + late
        if (asked == 0 && taken == 0 && failed == 0) return "Web videos in Kestrel's own player (Settings > Web videos play in: $m): no film asked yet"
        return "Web videos in Kestrel's own player (Settings > Web videos play in: $m): $asked questions from pages, $taken films taken over (by the page's play() $byPlay, a press on the player $byPress, Kestrel's own bar $byKestrel, full screen $byFullscreen, a playlist that came late $late), " +
            "$repeats asked again while it was up, $declined left to the page's own player (last reason: ${lastDeclined.ifEmpty { "none" }}), $failed failed before the first picture and went back to the page; last taken: ${lastTaken.ifEmpty { "none" }}"
    }
}
