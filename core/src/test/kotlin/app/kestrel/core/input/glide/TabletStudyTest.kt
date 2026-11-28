package app.kestrel.core.input.glide

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.Random

/**
 * R27a: the owner's tablet has much bigger keys than the phones behind the real swipes (key pitch 60 to 100 dp against 30 to 45). Skipped unless REAL_TABLET=1. For key widths of a phone, a tablet in portrait and in landscape, and
 * three finger noises in ABSOLUTE dp (a finger is as wobbly on any glass), the commonest short words are glided with the simulated finger and the first choice is judged: how often it is right, and how often it is a
 * word 2+ letters LONGER (the -> there, and -> access). Compares today's defaults, the pitch-aware parameters of R24b and a higher language floor.
 */
class TabletStudyTest {
    @Test fun shortWordsOnBigKeys() {
        assumeTrue("set REAL_TABLET=1", System.getenv("REAL_TABLET") == "1")
        val lex = GlideEval.loadLexicon()
        val shorts = listOf("the", "and", "to", "of", "in", "is", "it", "for", "you", "was", "are", "his", "but", "not", "can", "all", "her", "one", "out", "had", "with", "that", "this", "have")
        val n = (System.getenv("REAL_TABLET_N")?.toIntOrNull() ?: 40)
        for (width in listOf(292f, 564f, 960f)) {
            val keys = GlideEval.keysFor(width, width * 0.55f)
            val variants = listOf(
                "defaults" to GlideParams(), "pitch-aware" to GlideParams.forPitchDp(keys.pitch),
                "defaults+lmMin .45" to GlideParams(lmMin = 0.45f), "pitch-aware+lmMin .45" to GlideParams(shapeW = GlideParams.forPitchDp(keys.pitch).shapeW, visitCap = GlideParams.forPitchDp(keys.pitch).visitCap, lmMin = 0.45f)
            )
            for (aim in listOf(8f, 12f, 16f)) {
                val noise = SimNoise.aim(aim)
                val sb = StringBuilder("TABLET width %.0f dp (key pitch %.0f dp), finger aim %.0f dp (%.2f pitch): ".format(width, keys.pitch, aim, aim / keys.pitch))
                for ((name, p) in variants) {
                    val dec = GlideDecoder(keys, p); val sim = GestureSim(keys, Random(11L + width.toLong() + aim.toLong()))
                    var tot = 0; var hit = 0; var s2l = 0
                    for (w in shorts) {
                        if (lex.idOf(w) < 0) continue
                        val key = String(GlideLetters.keyOf(w))
                        for (i in 0 until n) {
                            val path = sim.path(w, noise) ?: continue
                            val c = dec.decode(path.xs, path.ys, path.xs.size, listOf(lex), null, 3)
                            tot++
                            val f = c.firstOrNull() ?: continue
                            if (String(GlideLetters.keyOf(f.text)) == key) hit++ else if (f.text.length - w.length >= 2) s2l++
                        }
                    }
                    sb.append("\n    %-24s first %.1f%%  short->long %d of %d (%.1f%%)".format(name, 100.0 * hit / tot, s2l, tot, 100.0 * s2l / tot))
                }
                println(sb)
            }
        }
    }
}
