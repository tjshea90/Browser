package app.kestrel.browser.optimizer

import android.content.Context
import app.kestrel.core.optimizer.OptReport
import app.kestrel.core.optimizer.RunState
import app.kestrel.core.optimizer.RunStateCodec
import java.io.File

/** Where a run lives on the tablet: `files/optimizer/state.txt` while it is active (written after every step, atomically), `last.txt` + `last.json` for the run that finished, `history.txt` for what is settled. */
object OptimizerStore {
    private fun dir(ctx: Context) = File(ctx.filesDir, "optimizer").apply { mkdirs() }
    private fun f(ctx: Context, name: String) = File(dir(ctx), name)

    @Synchronized fun load(ctx: Context): RunState? = try { f(ctx, "state.txt").takeIf { it.exists() }?.readText()?.let { RunStateCodec.decode(it) } } catch (e: Exception) { null }

    /** Writes through a temporary file and a rename: a run killed in the middle of a save keeps its previous complete state. */
    @Synchronized fun save(ctx: Context, s: RunState) {
        val tmp = f(ctx, "state.tmp"); tmp.writeText(RunStateCodec.encode(s))
        if (!tmp.renameTo(f(ctx, "state.txt"))) { f(ctx, "state.txt").delete(); tmp.renameTo(f(ctx, "state.txt")) }
    }

    @Synchronized fun clearActive(ctx: Context) { f(ctx, "state.txt").delete(); f(ctx, "state.tmp").delete() }

    /** The run that finished (or stopped): kept for the report screen, for Undo and for the upload. */
    @Synchronized fun archive(ctx: Context, s: RunState, json: String) {
        f(ctx, "last.txt").writeText(RunStateCodec.encode(s)); f(ctx, "last.json").writeText(json)
        val old = f(ctx, "history.txt").takeIf { it.exists() }?.readLines().orEmpty()
        val keep = old.filter { l -> OptReport.settledLines(s).none { it.substringBefore('|') == l.substringBefore('|') } }
        f(ctx, "history.txt").writeText((keep + OptReport.settledLines(s)).joinToString("\n"))
    }
    @Synchronized fun last(ctx: Context): RunState? = try { f(ctx, "last.txt").takeIf { it.exists() }?.readText()?.let { RunStateCodec.decode(it) } } catch (e: Exception) { null }
    @Synchronized fun lastJson(ctx: Context): String? = try { f(ctx, "last.json").takeIf { it.exists() }?.readText() } catch (e: Exception) { null }
    @Synchronized fun history(ctx: Context): List<String> = try { f(ctx, "history.txt").takeIf { it.exists() }?.readLines().orEmpty() } catch (e: Exception) { emptyList() }
    @Synchronized fun forgetHistory(ctx: Context) { f(ctx, "history.txt").delete() }
}
