package app.kestrel.core.optimizer

/**
 * One thing an experiment changes: an engine preference (`name` and a value; [FLIP] means "the opposite of the engine's own boolean value", read back at run time, for names whose default is compiled
 * into the engine and not written anywhere) or an app setting (a Prefs key with an Int value).
 */
sealed class Change {
    abstract val key: String
    class Pref(val name: String, val value: String) : Change() { override val key get() = name }
    class Flip(val name: String) : Change() { override val key get() = name }
    class Setting(val prefKey: String, val value: Int, val bool: Boolean = false) : Change() { override val key get() = prefKey }
}

enum class Group(val label: String) { DRAW("Drawing"), SCRIPT("Scripts"), CPU("Processor use"), UI("Scrolling and clips"), MEMORY("Memory") }

/**
 * @param id stable name (logs, history, the digest)
 * @param restart true = an engine preference read at start, so the engine process is restarted for it
 * @param hardwareOnly only meaningful while the graphics chip draws (the hardware trial was accepted)
 * @param askGlitch after measuring, the owner is shown the text page and asked whether anything is missing (only for experiments that change how the page is drawn)
 * @param prior how much it is expected to help (0..1), used only to order the plan (cost-aware: expected gain per minute)
 * @param memLimitMb the most extra memory the experiment may use and still be kept
 */
class Experiment(
    val id: String, val title: String, val why: String, val group: Group, val changes: List<Change>,
    val restart: Boolean, val prior: Double, val hardwareOnly: Boolean = false, val askGlitch: Boolean = false, val memLimitMb: Int = 60, val needsSoftware: Boolean = false,
) {
    fun engineChanges(): List<Change> = changes.filter { it !is Change.Setting }
    fun settingChanges(): List<Change.Setting> = changes.filterIsInstance<Change.Setting>()
}

object Catalog {
    /** Bumped whenever the list changes: the history only trusts results of the same catalog version. */
    const val VERSION = 2          // 2 (alpha108): the clip test really plays its clips now, the outlier guard and the real-clock budget changed: every setting is measured again

    /**
     * Experiments that were measured on the owner's tablet and are dropped for good (never planned, not even by 'test everything again'):
     * hw2 (hardware drawing without the helper process): 3.5 times the processor time (25 s against 7 s per pass) and WebGL stopped working (first real run, 2026-11-25).
     */
    val RETIRED: Set<String> = setOf("hw2")

    private fun p(n: String, v: String) = Change.Pref(n, v)

    val all: List<Experiment> = listOf(
        Experiment("hw1", "Hardware drawing (graphics chip, Mali fixes)", "Browsers that draw on the graphics chip are far smoother than processor drawing; Mozilla's Mali bug reports name two engine paths this switches off.", Group.DRAW,
            listOf(Change.Setting("draw_trial", 1)), restart = true, prior = 0.9, askGlitch = true, memLimitMb = 80),
        Experiment("hw2", "Hardware drawing, no helper process", "The same, with the renderer inside the main process and no compile-everything at start.", Group.DRAW,
            listOf(Change.Setting("draw_trial", 2)), restart = true, prior = 0.5, askGlitch = true, memLimitMb = 80),
        Experiment("fps30", "Calmer redrawing (30 a second)", "Half the drawing work for animated pages.", Group.DRAW, listOf(Change.Setting("lab_fps30", 1, true)), restart = true, prior = 0.5),
        Experiment("dither", "Cheaper gradients", "No dithering of gradients: less work per pixel when the processor draws.", Group.DRAW, listOf(Change.Setting("lab_dither", 1, true)), restart = true, prior = 0.3),
        Experiment("wrpaths", "New path drawing (Firefox 161)", "Firefox 161 added switches for WebRender's own path drawing; the default on this tablet is read first.", Group.DRAW, listOf(Change.Flip("gfx.webrender.paths")), restart = true, prior = 0.2),
        Experiment("wrpaths_swgl", "New path drawing in the processor renderer (161)", "The software-renderer switch of the same feature.", Group.DRAW, listOf(Change.Flip("gfx.webrender.paths.swgl")), restart = true, prior = 0.25, needsSoftware = true),
        Experiment("fontations", "Rust font reader (161)", "Firefox 161's font reader: possibly cheaper text layout.", Group.DRAW, listOf(Change.Flip("gfx.font_rendering.fontations.enabled")), restart = true, prior = 0.15),
        Experiment("defer_decode", "No decoding of hidden pictures (161)", "Pictures that are not on screen are not decoded: less work and memory while scrolling.", Group.DRAW, listOf(Change.Flip("image.decode.defer_if_not_visible")), restart = true, prior = 0.3),
        Experiment("bytecode", "Compile scripts on first sight", "The compiled form of a script is saved the first time, not the second.", Group.SCRIPT, listOf(Change.Setting("lab_bytecode", 1, true)), restart = true, prior = 0.3),
        Experiment("gcpar", "Parallel script clean-up", "The script engine's memory clean-up may use more than one core.", Group.SCRIPT, listOf(Change.Setting("lab_gcpar", 1, true)), restart = true, prior = 0.3),
        Experiment("gcsmall", "Small-device script memory growth", "Mozilla's own growth factors for devices with under 512 MB: less memory held, more clean-up work.", Group.SCRIPT,
            listOf(p("javascript.options.mem.gc_high_frequency_small_heap_growth", "120"), p("javascript.options.mem.gc_high_frequency_large_heap_growth", "110"), p("javascript.options.mem.gc_low_frequency_heap_growth", "110")), restart = true, prior = 0.25),
        Experiment("nursery", "Smaller script nursery", "A smaller young-object area: less memory, more frequent small clean-ups.", Group.SCRIPT, listOf(p("javascript.options.mem.nursery.max_kb", "16384")), restart = true, prior = 0.2),
        Experiment("jitoff", "Compile scripts off the main thread (strategy 0)", "The first-level compiler runs on a helper thread.", Group.SCRIPT, listOf(p("javascript.options.baselinejit.offthread_compilation_strategy", "0")), restart = true, prior = 0.15),
        Experiment("stylo2", "Two style threads", "Fewer threads fighting for four slow cores.", Group.CPU, listOf(p("layout.css.stylo-threads", "2")), restart = true, prior = 0.3),
        Experiment("nocaptchaobs", "No captcha-frame watchers", "A Mozilla observer that only feeds counters runs inside every captcha and bot-check frame.", Group.CPU, listOf(p("captchadetection.actor.enabled", "false")), restart = true, prior = 0.25),
        Experiment("noformfill", "No password and form-history work", "Kestrel keeps no logins; the engine's per-keystroke form work is not needed.", Group.CPU, listOf(p("signon.rememberSignons", "false"), p("browser.formfill.enable", "false")), restart = true, prior = 0.2),
        // EVERY experiment starts in a fresh engine process (lab v12: after the first LIVE setting change of a run no benchmark page ever loaded again on the emulator; the engine applies the scroll feel itself when it starts)
        Experiment("scroll0", "Scroll feel: Standard", "The least pre-drawing: less memory and drawing work per scroll.", Group.UI, listOf(Change.Setting("scroll_feel", 0)), restart = true, prior = 0.4),
        Experiment("scroll2", "Scroll feel: Smoothest", "The most pre-drawing: fewer blank edges while scrolling.", Group.UI, listOf(Change.Setting("scroll_feel", 2)), restart = true, prior = 0.3, memLimitMb = 80),
        Experiment("clips_free", "No limit on looping clips", "Page clips play as the page wants (the limiter is switched off).", Group.UI, listOf(Change.Setting("limit_clips", 0, true)), restart = true, prior = 0.1),
        Experiment("proc2", "Second content process", "A second page process for the engine: more memory, possibly less waiting.", Group.MEMORY, listOf(p("dom.ipc.processCount", "2")), restart = true, prior = 0.2, memLimitMb = 140),
    )

    fun byId(id: String): Experiment? = all.firstOrNull { it.id == id }

    /** App settings that the engine reads only when it starts (the experiment says `restart`): changing one needs a new engine process, like an engine preference does. */
    fun restartSettingKeys(): Set<String> = all.filter { it.restart }.flatMap { it.settingChanges() }.map { it.prefKey }.toSet()

    /** Every app setting key the catalog can change, with whether it is stored as a boolean (the runner needs the type to write it). */
    fun settingKeys(): Map<String, Boolean> = all.flatMap { it.settingChanges() }.associate { it.prefKey to it.bool }

    /**
     * Names and key patterns an experiment may never touch (privacy, security, anything that sends data). A catalog test holds every change to this list, so a later edit cannot slip one in.
     */
    private val denied = Regex("(shield|tracking|trackingprotection|https|upgrade|fingerprint|resistfingerprinting|cookie|cert|security|sandbox|trr|doh|dns|safebrowsing|permission|telemetry|geo|webrtc|referrer|proxy|ech|tls|ssl|pkcs|privacy|private|datareporting|healthreport|upload|crash|collect|consent|report)", RegexOption.IGNORE_CASE)

    fun isAllowed(c: Change): Boolean = !denied.containsMatchIn(c.key)

    /** Every engine preference name the catalog can write or flip (read by tools/check-gecko-prefs.sh: each must exist in the pinned engine). */
    fun engineNames(): List<String> = all.flatMap { e -> e.changes.mapNotNull { c -> when (c) { is Change.Pref -> c.name; is Change.Flip -> c.name; is Change.Setting -> null } } }.distinct()
}
