package app.kestrel.tools

import app.kestrel.core.filter.Category
import app.kestrel.core.filter.FilterSetBuilder
import app.kestrel.core.filter.FilterSetCodec
import app.kestrel.core.filter.Skip
import java.io.File
import kotlin.system.exitProcess

/**
 * Desktop list compiler. Same parser/builder/codec the tablet's updater uses, so what is tested here is what ships.
 *
 *   ListCompiler --out default.kfs [--psl-in effective_tld_names.dat --psl-out psl.txt] SPEC...
 *   SPEC := abp:<category>:<file>  |  hosts:<category>:<file>        category := ads | privacy | malware | annoyance
 *
 * Inputs are treated as untrusted text: they are only parsed, never executed.
 */
fun main(args: Array<String>) {
    var out: String? = null
    var pslIn: String? = null
    var pslOut: String? = null
    val specs = ArrayList<String>()
    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--out" -> out = args[++i]
            "--psl-in" -> pslIn = args[++i]
            "--psl-out" -> pslOut = args[++i]
            else -> specs.add(args[i])
        }
        i++
    }
    if (out == null || specs.isEmpty()) {
        System.err.println("usage: ListCompiler --out FILE [--psl-in IN --psl-out OUT] abp|hosts:<ads|privacy|malware|annoyance>:<file> ...")
        exitProcess(2)
    }

    if (pslIn != null && pslOut != null) {
        val lines = File(pslIn).readLines(Charsets.UTF_8).map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("//") }
        File(pslOut).writeText(lines.joinToString("\n") + "\n", Charsets.UTF_8)
        println("psl: ${lines.size} rules -> $pslOut")
    }

    val b = FilterSetBuilder()
    for (spec in specs) {
        val parts = spec.split(':', limit = 3)
        require(parts.size == 3) { "bad spec: $spec" }
        val category = when (parts[1]) {
            "ads" -> Category.ADS
            "privacy" -> Category.PRIVACY
            "malware" -> Category.MALWARE
            "annoyance" -> Category.ANNOYANCE
            else -> error("unknown category ${parts[1]}")
        }
        val file = File(parts[2])
        val t0 = System.nanoTime()
        file.bufferedReader(Charsets.UTF_8).use { r ->
            when (parts[0]) {
                "abp" -> b.addAbpList(file.name, category, r.lineSequence())
                "hosts" -> b.addHostsList(file.name, category, r.lineSequence())
                else -> error("unknown format ${parts[0]}")
            }
        }
        println("%-34s %6d ms   %s".format(file.name, (System.nanoTime() - t0) / 1_000_000, b.report))
    }

    val t0 = System.nanoTime()
    val fs = b.build()
    val bytes = FilterSetCodec.write(fs)
    File(out).writeBytes(bytes)
    println("built in ${(System.nanoTime() - t0) / 1_000_000} ms -> $out (${bytes.size / 1024} KB)")
    println("meta: host=${fs.meta.hostRules} hostExc=${fs.meta.hostExceptions} pattern=${fs.meta.patternRules} site=${fs.meta.siteRules} cosSite=${fs.meta.cosmeticSite} cosGeneric=${fs.meta.cosmeticGeneric}")
    println("skipped:")
    for (s in Skip.values()) if (s != Skip.NONE && b.report.skippedCount(s) > 0) println("  %-22s %d".format(s, b.report.skippedCount(s)))
}
