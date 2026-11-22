package app.kestrel.core.filter

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** SCRATCH (PERF3a): what the real lists' generic rules would hide for class and id tokens that a loading spinner or a player could carry. Output only; deleted after use. */
class ScratchGenericProbeTest {
    @Test fun probe() {
        var d: File? = File("").absoluteFile; var fs: FilterSet? = null
        while (d != null) { val f = File(d, "app/src/main/assets/filters/default.kfs"); if (f.isFile) { fs = FilterSetCodec.read(f.readBytes()); break }; d = d.parentFile }
        assumeTrue("default.kfs not found", fs != null)
        fs!!
        val names = listOf("loader", "loading", "spinner", "preloader", "buffering", "circle", "progress", "wait", "player", "video", "overlay", "container", "wrapper", "poster", "play", "controls", "icon", "mgp_loader", "mgp_overlayContainer", "mgp_player", "mgp_container", "mgp_loadingIcon", "mgp_buffering", "mgp_poster", "mgp_controls", "mgp_videoWrapper", "preroll", "pause", "ad", "banner", "sponsor", "promo", "popup", "modal", "spin", "ring", "dots", "bounce")
        val tokens = names.flatMap { listOf(".$it", "#$it") }
        for (t in tokens) {
            val css = fs.genericCssForTokens("www.pornhub.com", listOf(t))
            if (css.isNotEmpty()) println("PROBE token=$t css=" + css.replace("\n", " ").take(500))
        }
        println("PROBE stats " + fs.cosmeticDebugStats())
    }
}
