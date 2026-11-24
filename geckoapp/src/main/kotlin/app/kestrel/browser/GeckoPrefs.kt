package app.kestrel.browser

import android.content.Context
import java.io.File

/**
 * The Gecko preference list (assets/gecko-prefs.txt) turned into the YAML config that GeckoRuntimeSettings.Builder.configFilePath reads before the engine starts
 * (research/GECKO_SPEED.md section 1). Rewritten at every start, but only when its content changed.
 */
object GeckoPrefs {
    /**
     * `name = value` lines -> `prefs:` YAML. Blank lines and `#` comments are skipped; a malformed line is skipped (never fatal).
     * [overrides] (the emulator's pref sweep, DEBUG builds only, see [writeConfig]) are `name=value` lines in the same grammar: an override replaces the base line of the same name (a duplicate YAML key would
     * be an error), and the value `-` removes the pref (the engine's own default applies).
     */
    fun yaml(text: String, overrides: List<String> = emptyList()): String {
        val over = LinkedHashMap<String, String>()
        for (raw in overrides) {
            val eq = raw.indexOf('=')
            if (eq <= 0) continue
            over[raw.substring(0, eq).trim()] = raw.substring(eq + 1).trim()
        }
        val sb = StringBuilder("prefs:\n")
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val eq = line.indexOf('=')
            if (eq <= 0) continue
            val name = line.substring(0, eq).trim()
            if (over.containsKey(name)) continue
            val value = line.substring(eq + 1).trim()
            if (!NAME.matches(name)) continue
            if (valueOk(value)) sb.append("  ").append(name).append(": ").append(value).append('\n')
        }
        for ((name, value) in over) if (value != "-" && NAME.matches(name) && valueOk(value)) sb.append("  ").append(name).append(": ").append(value).append('\n')
        return sb.toString()
    }

    private fun valueOk(value: String): Boolean = value == "true" || value == "false" || INT.matches(value) || (value.length >= 2 && value.startsWith("\"") && value.endsWith("\"") && !value.substring(1, value.length - 1).contains('"'))

    /**
     * The Network lab switches that are ON, as `prefs:` entries: a preference that the engine reads only once is not changed by a live `setGeckoPref` after start-up, so the switches are also
     * part of the config file the engine reads BEFORE it starts (the live path in GeckoEngine.applyLab covers changes while it runs). Off = no line: the engine's own default.
     */
    internal fun labLines(p: app.kestrel.browser.data.Prefs): String = buildString {
        if (p.labBuffer) append("  network.buffer.cache.size: 32768\n")
        if (p.labFontDelay) append("  gfx.downloadable_fonts.fallback_delay: 500\n")
        if (p.labDnsPrefetch && p.dataSaver == app.kestrel.core.shield.DataSaver.OFF) append("  dom.prefetch_dns_for_anchor_https_document: true\n")
        if (p.labFps30) append("  layout.frame_rate: 30\n")      // PERF14: 30 redraws a second at most (the engine's own value is -1: every screen refresh); A/B in the digest: pages' own load timing split by this setting
        if (p.labSpec > 0) { append("  dom.speculation_rules.enabled: true\n"); append("  dom.speculation_rules.prefetch.dry_run: ").append(p.labSpec == 1).append('\n') }
    }

    /**
     * PERF22 (research/LOW_END_PLAYBOOK.md): the engine experiments of the Network lab, in the `name=value` grammar of [yaml]'s overrides (a value that is also in gecko-prefs.txt REPLACES it: a duplicate YAML key would
     * be an error). Off = nothing here: the engine's own value or the one in gecko-prefs.txt. Every name exists in GeckoView 160 (tools/check-gecko-prefs.sh reads these strings too).
     */
    internal fun labOverrides(p: app.kestrel.browser.data.Prefs): List<String> = buildList {
        if (p.labDither) add("gfx.webrender.dithering=false")                              // no dithering of gradients: less per-pixel work in CPU drawing, a little banding in smooth gradients
        if (p.labBytecode) add("dom.script_loader.bytecode_cache.strategy=-1")              // the compiled form of a script is saved the first time it is seen, not after the second load
        if (p.labGcPar) add("javascript.options.mem.gc_parallel_marking=true")              // the script engine's clean-up marks on more than one thread
    }

    /**
     * PERF7c (owner 2026-11-22, two screenshots: horizontal bands of text missing on claude.ai, the pornhub loading circle cut at the bottom; research/PAINT_GLITCH.md): the shortcuts of the engine's graphics
     * renderer (WebRender) that redraw or upload only PART of a frame or a texture. A driver that gets one of them wrong leaves exactly such bands undrawn. "Safe drawing" turns them off: whole clears, one
     * texture upload at a time, no partial presents. A little more graphics work per frame (the picture is 600 x 973), no change to what a page does. Every name exists in GeckoView 160 (tools/check-gecko-prefs.sh).
     */
    internal fun drawLines(p: app.kestrel.browser.data.Prefs): String = buildString {
        if (p.labDraw) {
            append("  gfx.webrender.scissored-cache-clears.enabled: false\n")
            append("  gfx.webrender.batched-texture-uploads: false\n")
            append("  gfx.webrender.max-partial-present-rects: 0\n")
            append("  gfx.webrender.allow-partial-present-buffer-age: false\n")
            // PERF10b: the alpha94 uploads proved the four above reached the engine (engine draw-prefs line) and the bands stayed. Texture uploads through mapped upload buffers (PBO) are the next shortcut that can
            // leave some rows of a glyph or tile not yet written when the page is busy: plain uploads instead (research/PAINT_GLITCH.md, 2026-11-22).
            append("  gfx.webrender.pbo-uploads: false\n")
        }
        // PERF10b: CPU drawing (software WebRender). Mozilla's own text-band bug threads name it as the one switch that always removes this fault family; slower, so OFF unless the person turns it on.
        if (p.labSoft) append("  gfx.webrender.software: true\n")
    }

    /** Writes filesDir/gecko-config.yaml (if the content differs) and returns its path. */
    @Synchronized fun writeConfig(ctx: Context): String { // (the config warm-up thread of GeckoEngine.prewarmConfig and the UI thread both call it)
        val text = try { ctx.assets.open("gecko-prefs.txt").bufferedReader().use { it.readText() } } catch (e: Exception) { "" }
        // DEBUG builds only (the emulator's pref sweep, LAB5b): `setprop debug.kestrel.gp1 "javascript.options.parallel_marking=true"` .. gp8 (one pref per property: a system property holds 91 characters),
        // then a restart. The same grammar as gecko-prefs.txt; `name=-` removes a pref. A release build never reads a property (DebugProps.get answers null).
        val sweep = (1..8).mapNotNull { app.kestrel.browser.diag.DebugProps.get(ctx, "debug.kestrel.gp$it") }
        if (sweep.isNotEmpty()) app.kestrel.browser.diag.Health.log("engine", "sweep", sweep.joinToString(" ; "))
        var yaml = yaml(text, sweep + labOverrides(app.kestrel.browser.data.Prefs.get(ctx))) + labLines(app.kestrel.browser.data.Prefs.get(ctx)) + drawLines(app.kestrel.browser.data.Prefs.get(ctx))
        // DEBUG builds only (the emulator rig): `setprop debug.kestrel.moz_log "nsHttp:5,nsSocketTransport:5"` and restart makes Gecko log those modules to files/gecko-moz.log
        // (adb: run-as <package> cat files/gecko-moz.log). The value is restricted to module:level pairs so nothing else can ride into the engine's environment.
        app.kestrel.browser.diag.DebugProps.get(ctx, "debug.kestrel.moz_log")?.takeIf { LOG_SPEC.matches(it) }?.let {
            yaml += "env:\n  MOZ_LOG: \"$it\"\n  MOZ_LOG_FILE: \"" + File(ctx.filesDir, "gecko-moz.log").path + "\"\n"
        }
        val f = File(ctx.filesDir, "gecko-config.yaml")
        if (runCatching { f.readText() }.getOrNull() != yaml) f.writeText(yaml)
        return f.path
    }

    private val LOG_SPEC = Regex("([A-Za-z0-9_]+:[0-9],?){1,12}")
    private val NAME = Regex("[A-Za-z0-9_.-]+")
    private val INT = Regex("-?[0-9]+")
}
