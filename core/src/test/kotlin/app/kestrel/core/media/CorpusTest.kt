package app.kestrel.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Stress run over a corpus of ffmpeg-made transport-stream variants (tools/mux-corpus.sh). Only runs when KESTREL_CORPUS points at the corpus folder, so the normal suite never needs ffmpeg:
 *   bash tools/mux-corpus.sh /some/dir && KESTREL_CORPUS=/some/dir ./gradlew :core:test --tests 'app.kestrel.core.media.CorpusTest' -i
 * Every `refuse_*` variant must be turned down by the probe with a reason; every other variant must become an MP4 that ffmpeg decodes without a message and that holds as many
 * pictures and sound frames as the source.
 */
class CorpusTest {
    private fun probeCount(file: File, stream: String): Int {
        val p = ProcessBuilder("ffprobe", "-v", "error", "-count_frames", "-select_streams", stream, "-show_entries", "stream=nb_read_frames", "-of", "default=nw=1:nk=1", file.absolutePath).redirectErrorStream(true).start()
        val out = p.inputStream.readBytes().toString(Charsets.UTF_8).trim(); p.waitFor()
        return out.lines().firstOrNull { it.toIntOrNull() != null }?.toInt() ?: 0
    }

    @Test fun everyVariantBecomesACleanMp4OrIsRefusedWithAReason() {
        val root = System.getenv("KESTREL_CORPUS")?.let { File(it) } ?: return
        if (!Fixtures.ffmpeg) return
        val report = StringBuilder("\nCORPUS (${root.path})\n%-18s %9s %8s %8s %8s %9s %s\n".format("variant", "in bytes", "v in", "v out", "a out", "MB/s", "result"))
        val problems = ArrayList<String>()
        for (dir in root.listFiles { f -> f.isDirectory }!!.sortedBy { it.name }) {
            val segs = dir.listFiles { f -> f.name.matches(Regex("seg\\d+\\.ts")) }!!.sortedBy { it.name.removePrefix("seg").removeSuffix(".ts").toInt() }
            if (segs.isEmpty()) continue
            val first = segs[0].readBytes()
            val why = TsToMp4.probe(first, 0, first.size)
            if (dir.name.startsWith("refuse_")) {
                if (why == null) problems.add("${dir.name}: should have been refused") else report.append("%-18s %9s %8s %8s %8s %9s REFUSED: %s\n".format(dir.name, "-", "-", "-", "-", "-", why))
                continue
            }
            if (why != null) { problems.add("${dir.name}: refused: $why"); continue }
            val all = File(dir, "all.ts"); all.outputStream().use { o -> for (s in segs) o.write(s.readBytes()) }
            val vIn = probeCount(all, "v:0"); val aIn = probeCount(all, "a:0")
            val out = MemOut(); val journal = MemJournal()
            val m = TsToMp4(out, journal, {}, 2000L * segs.size, { 1_700_000_000_000L })
            m.startFresh()
            val t0 = System.nanoTime()
            var total = 0L
            for ((i, s) in segs.withIndex()) {
                val d = s.readBytes(); total += d.size
                m.beginSegment(i, false, if (i == 0) 2000L * segs.size else 0L)
                var p = 0; while (p < d.size) { val n = minOf(65536, d.size - p); m.feed(d, p, n); p += n }
                m.endSegment()
            }
            val fin = m.finish()
            val secs = (System.nanoTime() - t0) / 1e9
            val file = File(dir, "out.mp4"); file.writeBytes(out.bytes())
            val r = Mp4Reader(file.readBytes())
            val v = r.track("vide"); val a = r.track("soun")
            val said = Fixtures.ffmpegDecode(file.readBytes())
            val vOut = v?.samples?.size ?: 0; val aOut = a?.samples?.size ?: 0
            val vProbe = probeCount(file, "v:0"); val aProbe = probeCount(file, "a:0")
            var res = "ok"
            if (said.isNotEmpty()) res = "DECODE: ${said.lines().first().take(80)}"
            else if (vOut != vIn || vProbe != vIn) res = "FRAMES video in=$vIn tables=$vOut ffprobe=$vProbe"
            else if (aIn > 0 && (aOut < aIn - 1 || aProbe < aIn - 1)) res = "FRAMES audio in=$aIn tables=$aOut ffprobe=$aProbe"
            if (fin.warning != null) res += " (warning: ${fin.warning})"
            report.append("%-18s %9d %8d %8d %8d %9.1f %s\n".format(dir.name, total, vIn, vOut, aOut, total / 1e6 / secs, res))
            if (res != "ok") problems.add("${dir.name}: $res")
        }
        println(report)
        assertTrue("corpus problems:\n" + problems.joinToString("\n") + "\n" + report, problems.isEmpty())
    }
}
