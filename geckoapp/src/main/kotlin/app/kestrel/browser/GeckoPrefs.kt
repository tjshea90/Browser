package app.kestrel.browser

import android.content.Context
import java.io.File

/**
 * The Gecko preference list (assets/gecko-prefs.txt) turned into the YAML config that GeckoRuntimeSettings.Builder.configFilePath reads before the engine starts
 * (research/GECKO_SPEED.md section 1). Rewritten at every start, but only when its content changed.
 */
object GeckoPrefs {
    /** `name = value` lines -> `prefs:` YAML. Blank lines and `#` comments are skipped; a malformed line is skipped (never fatal). */
    fun yaml(text: String): String {
        val sb = StringBuilder("prefs:\n")
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val eq = line.indexOf('=')
            if (eq <= 0) continue
            val name = line.substring(0, eq).trim()
            val value = line.substring(eq + 1).trim()
            if (!NAME.matches(name)) continue
            val ok = value == "true" || value == "false" || INT.matches(value) || (value.length >= 2 && value.startsWith("\"") && value.endsWith("\"") && !value.substring(1, value.length - 1).contains('"'))
            if (ok) sb.append("  ").append(name).append(": ").append(value).append('\n')
        }
        return sb.toString()
    }

    /** Writes filesDir/gecko-config.yaml (if the content differs) and returns its path. */
    fun writeConfig(ctx: Context): String {
        val text = try { ctx.assets.open("gecko-prefs.txt").bufferedReader().use { it.readText() } } catch (e: Exception) { "" }
        val yaml = yaml(text)
        val f = File(ctx.filesDir, "gecko-config.yaml")
        if (runCatching { f.readText() }.getOrNull() != yaml) f.writeText(yaml)
        return f.path
    }

    private val NAME = Regex("[A-Za-z0-9_.-]+")
    private val INT = Regex("-?[0-9]+")
}
