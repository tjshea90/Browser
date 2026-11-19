package app.kestrel.core.input.glide

import app.kestrel.core.input.ImeLayout
import app.kestrel.core.input.MiniKeyboardGeometry
import app.kestrel.core.input.MiniKeyboardLayout
import app.kestrel.core.input.Page
import app.kestrel.core.input.editor.FieldKind
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/**
 * Owner M1 changed the system keyboard's geometry to the photo's (ten key pitches across, Z under S, rows further apart). The glide decoder was tuned on the browser's reference geometry, which has the same
 * columns, so the photo layout must decode glides at least as well as that one does at the same width: a layout change must never quietly cost accuracy. A simulated thumb (dp noise, so a bigger keyboard helps
 * it, exactly as it helps a real thumb).
 */
class ImeGeometryAccuracyTest {
    private fun imeKeys(widthDp: Float, rowDp: Float, gapX: Float, gapY: Float): GlideKeys {
        val rows = ImeLayout.rows(Page.LETTERS, false, FieldKind.TEXT)
        return GlideKeys.from(rows, MiniKeyboardGeometry.slots(rows, widthDp, 4 * rowDp + 3 * gapY, gapX, gapY, 0f, ImeLayout.rowWeights(rows)))
    }

    @Test fun thePhotoLayoutDecodesAsWellAsTheReferenceLayoutAtTheSameWidth() {
        val lex = GlideEval.loadLexicon()
        val picks = GlideEval.frequencyPick(lex, 1200, Random(11))
        for (noise in listOf(SimNoise.NORMAL, SimNoise.SLOPPY)) for (w in listOf(564f, 360f)) {
            val rowDp = (w / 10f * 1.35f).coerceIn(44f, 56f); val gx = (w / 10f * 0.14f).coerceIn(3f, 8f)
            val rows = MiniKeyboardLayout.rows(false)
            val reference = GlideKeys.from(rows, MiniKeyboardGeometry.boxes(rows, w, 4 * rowDp + 3 * gx, gx))
            val photo = imeKeys(w, rowDp, gx, gx * 1.5f)
            val a = GlideEval.run(lex, reference, GlideParams(), noise, picks, 5)
            val b = GlideEval.run(lex, photo, GlideParams(), noise, picks, 5)
            println("geometry ${noise.name} width $w dp: reference top1 %.1f%% top3 %.1f%%  photo layout top1 %.1f%% top3 %.1f%%".format(a.pct(a.top1), a.pct(a.top3), b.pct(b.top1), b.pct(b.top3)))
            assertTrue("${noise.name} at $w dp: the photo layout's top-1 ${b.pct(b.top1)} fell more than 2 points below the reference's ${a.pct(a.top1)}", b.pct(b.top1) >= a.pct(a.top1) - 2.0)
            assertTrue("${noise.name} at $w dp: top-3 ${b.pct(b.top3)} vs ${a.pct(a.top3)}", b.pct(b.top3) >= a.pct(a.top3) - 2.0)
        }
    }
}
