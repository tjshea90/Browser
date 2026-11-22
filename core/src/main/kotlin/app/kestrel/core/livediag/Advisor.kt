package app.kestrel.core.livediag

/**
 * One suggested change, in the words Settings uses. [key] / [value] are set when one tap can make the change (a switch or a choice in the app's own preferences, only those in [Advisor.APPLICABLE]); null when it is
 * advice the person has to act on (close tabs, pair the phone). [why] always carries the numbers it rests on.
 */
class Advice(val id: String, val severity: Int, val where: String, val setting: String, val now: String, val change: String, val key: String?, val value: Any?, val why: String) {
    fun text(): String = if (key != null) "Settings > $where > $setting: change $now to $change. Why: $why" else "$where > $setting: $change. Why: $why"
}

/**
 * LD2b: the rules that turn what the tablet measured into "change THIS setting to THAT". Pure: the app gives it the settings the owner really changed (key -> value as text), the event histogram and the site table of
 * [DiagStats], and a map of plain numbers about the device and the last playback. It never sees a page, an address or a name. Every rule needs enough evidence (a count or a size) before it speaks, and says its numbers,
 * so a suggestion can be judged and a wrong one found. Defaults are those of Prefs; a setting the owner never touched is read as its default.
 */
object Advisor {
    /** The only keys a suggestion may change by itself, with their allowed values (type: B = boolean, I = int range). The app refuses anything else. */
    class Allowed(val bool: Boolean, val min: Int = 0, val max: Int = 0)
    val APPLICABLE: Map<String, Allowed> = mapOf(
        "tabs_kept" to Allowed(false, 0, 2), "neon" to Allowed(false, 0, 2), "scroll_feel" to Allowed(false, 0, 2), "instant_back" to Allowed(true),
        "pages_via_phone" to Allowed(true), "vid_fps30" to Allowed(true), "video_cap" to Allowed(false, 0, 4), "net_h3" to Allowed(true),
    )

    fun canApply(key: String, value: Any?): Boolean {
        val a = APPLICABLE[key] ?: return false
        return if (a.bool) value is Boolean else value is Number && value.toLong() in a.min.toLong()..a.max.toLong()
    }

    private val TABS = listOf("Automatic", "Few (2)", "Many (6)")
    private val NEON = listOf("Off", "Steady", "Subtle pulse")
    private val FEEL = listOf("Standard", "Smoother", "Smoothest")
    private val CAP = listOf("Automatic", "Off (whatever the site offers)", "At most 480p", "At most 720p", "At most 1080p")

    fun advise(s: Map<String, String>, kinds: Map<String, Long>, sites: List<SiteStat>, f: Map<String, Long>): List<Advice> {
        fun i(k: String, d: Int) = s[k]?.toIntOrNull() ?: d
        fun b(k: String, d: Boolean) = s[k]?.let { it == "true" } ?: d
        fun n(k: String) = kinds[k] ?: 0L
        fun v(k: String) = f[k] ?: 0L
        fun mb(x: Long) = x.toString() + " MB"
        fun sec(ms: Long) = (ms / 100) / 10.0
        val out = ArrayList<Advice>()

        // memory: the engine was killed or crashed
        val kills = n("gecko onKill") + n("gecko onCrash")
        val tk = i("tabs_kept", 0)
        if (kills >= 2 && tk != 1) out.add(Advice("kills-tabs", 3, "Appearance", "Tabs kept ready", TABS[tk.coerceIn(0, 2)], TABS[1], "tabs_kept", 1,
            "the page engine was killed or crashed $kills times since the last report (on a 2 GB tablet that is usually memory); fewer tabs kept ready leave it more room"))
        if (kills >= 4 && tk == 1) out.add(Advice("kills-close", 3, "Everyday use", "Open tabs", "", "close the tabs you are not using", null, null,
            "the engine was killed or crashed $kills times even with Few (2) tabs kept ready: memory is the limit"))
        val hib = n("tabs hibernate")
        if (tk == 1 && kills == 0L && hib >= 12 && v("ramAvailMb") >= 450) out.add(Advice("hibernate-auto", 1, "Appearance", "Tabs kept ready", TABS[1], TABS[0], "tabs_kept", 0,
            "tabs were put to sleep $hib times with no engine kill, and about ${mb(v("ramAvailMb"))} of memory is free: Automatic keeps more tabs ready, so switching stays instant"))

        // slow and failing pages
        val slow = sites.filter { it.loads >= 3 && it.medianDoneMs >= 12_000 && !it.site.startsWith("<") }
        if (slow.size >= 2) {
            val worst = slow.maxByOrNull { it.medianDoneMs }!!
            val why = "${slow.size} sites took 12 s or more to finish loading (slowest: ${worst.site}, median ${sec(worst.medianDoneMs)} s over ${worst.loads} loads); the phone fetches pages with its own connection, which the carrier does not cap"
            if (v("phonePaired") == 1L && !b("pages_via_phone", false)) out.add(Advice("slow-phone", 2, "Phone helper", "Load pages through the phone", "off", "on", "pages_via_phone", true, why))
            else if (v("phonePaired") != 1L && !b("pages_via_phone", false)) out.add(Advice("slow-pair", 1, "Phone helper", "Pair with the phone", "", "pair Kestrel Courier on the phone, then turn on Load pages through the phone", null, null, why))
        }
        val total = sites.sumOf { it.loads }; val failed = sites.sumOf { it.failed }
        if (total >= 12 && failed * 4 >= total && b("net_h3", true)) out.add(Advice("h3-off", 2, "Network lab (for slow pages)", "HTTP/3 (QUIC)", "on", "off", "net_h3", false,
            "$failed of $total page loads failed; a carrier hotspot that slows UDP breaks HTTP/3 first, and sites then use HTTP/2"))
        for (x in sites.filter { it.loads >= 3 && it.failed * 2 >= it.loads && !it.site.startsWith("<") }.sortedByDescending { it.failed }.take(3))
            out.add(Advice("site-fails-" + x.site, 2, "The shield button in the address bar", "Shields for " + x.site, "on", "turn Shields off for this site and reload", null, null,
                "${x.site} failed ${x.failed} of ${x.loads} loads; a blocking rule may be breaking it (turn it off for that site only)"))

        // smoothness
        val slowFrames = v("slowFrames"); val worstFrame = v("worstFrameMs"); val neon = i("neon", 2)
        if ((slowFrames >= 40 || worstFrame >= 400) && neon == 2) out.add(Advice("jank-neon", 2, "Appearance", "Neon glow", NEON[2], NEON[1], "neon", 1, "$slowFrames slow screen updates, the worst $worstFrame ms; the pulsing glow is an animation the weak CPU also has to draw"))
        else if (worstFrame >= 600 && neon == 1) out.add(Advice("jank-neon-off", 1, "Appearance", "Neon glow", NEON[1], NEON[0], "neon", 0, "the worst screen update took $worstFrame ms with the glow on"))
        if (slowFrames >= 40 && i("scroll_feel", 1) == 2) out.add(Advice("jank-feel", 1, "Appearance", "Scroll feel", FEEL[2], FEEL[1], "scroll_feel", 1, "$slowFrames slow screen updates; Smoothest pre-draws more and uses more memory"))

        // video
        val shown = v("framesShown"); val dropped = v("framesDropped")
        if (shown >= 600 && dropped * 20 >= shown && !b("vid_fps30", false)) out.add(Advice("video-drops", 2, "Video & page animation", "At most 30 frames a second", "off", "on", "vid_fps30", true,
            "Kestrel's player dropped $dropped of ${shown + dropped} frames (${dropped * 100 / (shown + dropped)} %); 30 frames a second is half the work for the decoder and the screen"))
        val stalls = v("playStalls"); val cap = i("video_cap", 0)
        if (stalls >= 3 && v("plays") >= 1 && cap != 2) out.add(Advice("video-stalls", 2, "Video & page animation", "Picture size from sites", CAP[cap.coerceIn(0, 4)], CAP[2], "video_cap", 2,
            "the player waited for the network $stalls times (${v("playStalledMs") / 1000} s in all) on a link of about ${(v("linkKbps") + 50) / 100 / 10.0} Mbit/s; a smaller picture needs less of it"))

        // back and forward
        if (!b("instant_back", true) && n("tabs back-picture") >= 6) out.add(Advice("back-off", 2, "Privacy & security", "Instant Back", "off", "on", "instant_back", true,
            "Back showed a waiting picture ${n("tabs back-picture")} times; Instant Back keeps those pages so Back is immediate"))

        // housekeeping
        if (v("storageFreeMb") in 1..599) out.add(Advice("storage-low", 2, "Privacy & security", "Tidy storage now", "", "tap it", null, null,
            "only ${mb(v("storageFreeMb"))} of storage is free; Android refuses to install an update below about 1 GB"))
        if (v("filterAgeDays") >= 10) out.add(Advice("filters-old", 1, "Filter lists", "Update now", "", "tap it", null, null, "the blocking lists are ${v("filterAgeDays")} days old"))
        if (v("liveFailsInRow") >= 3) out.add(Advice("diag-token", 1, "Live diagnostics (for Claude)", "GitHub token", "", "paste a new token", null, null, "GitHub refused the last ${v("liveFailsInRow")} sends; the token may have expired"))

        return out.sortedByDescending { it.severity }.take(8)
    }
}
