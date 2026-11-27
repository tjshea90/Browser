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
        if (p.labSpec > 0) { append("  dom.speculation_rules.enabled: true\n"); append("  dom.speculation_rules.prefetch.dry_run: ").append(p.labSpec == 1).append('\n') }
    }

    /** Writes filesDir/gecko-config.yaml (if the content differs) and returns its path. */
    @Synchronized fun writeConfig(ctx: Context): String { // (the config warm-up thread of GeckoEngine.prewarmConfig and the UI thread both call it)
        val text = try { ctx.assets.open("gecko-prefs.txt").bufferedReader().use { it.readText() } } catch (e: Exception) { "" }
        // DEBUG builds only (the emulator's pref sweep, LAB5b): `setprop debug.kestrel.gp1 "javascript.options.parallel_marking=true"` .. gp8 (one pref per property: a system property holds 91 characters),
        // then a restart. The same grammar as gecko-prefs.txt; `name=-` removes a pref. A release build never reads a property (DebugProps.get answers null).
        val sweep = (1..8).mapNotNull { app.kestrel.browser.diag.DebugProps.get(ctx, "debug.kestrel.gp$it") }
        if (sweep.isNotEmpty()) app.kestrel.browser.diag.Health.log("engine", "sweep", sweep.joinToString(" ; "))
        var yaml = yaml(text, sweep) + labLines(app.kestrel.browser.data.Prefs.get(ctx))
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
