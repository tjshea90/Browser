package app.kestrel.core.prompt

/**
 * IMG1 (owner 2026-11-21: "I'm trying to upload screenshots to claude and it says can't process images"): what a picked file LOOKS like to Android before GeckoView copies it for the page: its type, its size, whether Android can read
 * the picture's header, whether its name has an extension (the engine takes the page's content type from the extension of the copy), and how much room the app's cache has (the copy goes there). Pure, so the verdict words are tested.
 * Never a file name, never a path, never from a private tab (the caller checks).
 */
object FilePickFacts {
    /** One picked file: [type] as the content provider says (may be empty), [bytes] (-1 = unknown), [w] x [h] from the picture's header (0 = could not read), [nameExt] the display name has an extension, [freeMb] free room in the cache (-1 = unknown). */
    data class Picked(val type: String, val bytes: Long, val w: Int, val h: Int, val nameExt: Boolean, val freeMb: Long)

    private val MIME = Regex("^[a-z0-9]+/[a-z0-9.+-]{1,40}$")

    /** A real "kind/subtype" is kept; nothing else is (a provider's odd answer could carry a path or a name): "none" for empty, "odd" otherwise. */
    fun typeWord(t: String): String { val x = t.trim().lowercase(); return if (x.isEmpty()) "none" else if (MIME.matches(x)) x else "odd" }

    /** The one-line account for the Health ring / the live diagnostics: numbers and a type, nothing else. */
    fun logLine(p: Picked): String =
        "type=" + typeWord(p.type) +
            " kb=" + (if (p.bytes < 0) -1 else (p.bytes + 1023) / 1024) + " size=${p.w}x${p.h} decodes=" + (if (p.w > 0 && p.h > 0) 1 else 0) + " ext=" + (if (p.nameExt) 1 else 0) + " freeMb=${p.freeMb}"

    /**
     * PERF10c (owner 2026-11-22: "I can't attach photos to claude.ai"): what the PAGE got when a file input changed (pagetime.js measured it inside the page, background.js checked it). [type] is the type the page's File reports,
     * [mg] the first bytes (png, jpeg, gif, webp, avif, heic, bmp, pdf, other, none), [dec] 0 not tried / 1 decoded / 2 failed / 3 timed out, [px] 0 not tried / 1 drawn / 2 all transparent / 3 one flat colour / 4 canvas refused,
     * [enc] 0 not tried / 1 re-encoded / 2 empty / 4 refused, [encKb] the re-encoded size, [ms] how long the whole test took.
     */
    data class PageGot(val type: String, val kb: Long, val mg: String, val dec: Int, val w: Int, val h: Int, val px: Int, val enc: Int, val encKb: Long, val ms: Long)

    private val MAGIC = setOf("png", "jpeg", "gif", "webp", "avif", "heic", "bmp", "pdf", "other", "none")
    private fun mg(s: String): String = if (s in MAGIC) s else "other"
    private fun decWord(d: Int) = when (d) { 1 -> "ok"; 2 -> "failed"; 3 -> "timeout"; else -> "-" }
    private fun pxWord(d: Int) = when (d) { 1 -> "ok"; 2 -> "transparent"; 3 -> "flat"; 4 -> "refused"; else -> "-" }
    private fun encWord(d: Int, kb: Long) = when (d) { 1 -> "ok(${kb}kb)"; 2 -> "empty"; 4 -> "refused"; else -> "-" }

    /** One entry of the `filepick page-got` line: only fixed words and numbers (the type goes through [typeWord], the first bytes through a fixed list): no name, no pixel. */
    fun pageLine(host: String, total: Int, files: List<PageGot>): String =
        "host=$host n=$total " + files.take(3).joinToString(" ; ") { f ->
            "type=" + typeWord(f.type) + " kb=${f.kb} bytes=" + mg(f.mg) + " decode=" + decWord(f.dec) + (if (f.w > 0) " ${f.w}x${f.h}" else "") + " canvas=" + pxWord(f.px) + " reencode=" + encWord(f.enc, f.encKb) + " ${f.ms}ms"
        }

    /** The plain-words verdict of the PAGE's own copy of the first file: which side is at fault. Said for the Diagnostics screen and the digest. */
    fun pageVerdict(f: PageGot): String {
        val pic = f.type.lowercase().startsWith("image/") || f.mg in setOf("png", "jpeg", "gif", "webp", "avif", "heic", "bmp")
        return when {
            f.kb == 0L -> "the page's file is EMPTY (0 KB): the engine's copy of the picked file was cut short or the picker gave nothing"
            !pic -> "the page received a file of type ${typeWord(f.type)} (first bytes: ${mg(f.mg)}), not a picture"
            f.mg == "heic" || f.mg == "avif" -> "the file is a ${mg(f.mg)} picture, which many sites cannot process: the picture's own format is the problem, not Kestrel"
            f.dec == 2 -> "the engine could NOT decode the picture the page received (type ${typeWord(f.type)}, first bytes ${mg(f.mg)}): the engine or the copy is at fault"
            f.dec == 3 -> "decoding the picture took over 6 s: the tablet was too busy for the page"
            f.px == 4 || f.enc == 4 -> "the page's canvas REFUSED to draw or re-encode the picture: the engine's canvas protection blocks the step a site needs before an upload"
            f.px == 2 || f.px == 3 -> "the picture drew as ${if (f.px == 2) "all transparent" else "one flat colour"}: the engine's canvas returns blank or noise data (fingerprint protection or the graphics driver)"
            f.enc == 2 -> "the canvas could not re-encode the picture (empty result): the engine's canvas is at fault"
            f.dec == 1 && f.px == 1 && f.enc == 1 -> "the page received a good, decodable picture and its canvas drew and re-encoded it: the file and the engine are fine, so claude.ai or its server refused it"
            else -> "the page received the file (decode ${decWord(f.dec)}, canvas ${pxWord(f.px)}, re-encode ${encWord(f.enc, f.encKb)})"
        }
    }

    /** The plain-words verdict the Diagnostics screen shows for the last pick, said so a person (or the next session) can tell the file from the page. */
    fun verdict(p: Picked): String {
        val img = p.type.lowercase().startsWith("image/") || (p.type.isEmpty() && p.w > 0)
        return when {
            p.bytes == 0L -> "the picker gave an EMPTY file (0 bytes): the file or the picker is the problem, not the page"
            p.freeMb in 0..49 -> "the app's cache has only ${p.freeMb} MB free: the copy made for the page may be cut short (free some storage)"
            img && (p.w <= 0 || p.h <= 0) -> "Android cannot read this picture's header (${p.type.ifEmpty { "no type" }}, ${if (p.bytes < 0) "unknown size" else (p.bytes + 1023) / 1024} KB): the file may be damaged or not a picture"
            !p.nameExt && img -> "the picture has no file extension, so the engine may tell the page it is no picture"
            img -> "an ordinary picture (${p.type.ifEmpty { "type unknown" }}, ${p.w}x${p.h}, ${(p.bytes + 1023) / 1024} KB): if a site still says it cannot process it, the page or Kestrel's page protections are the cause"
            else -> "a file of type ${p.type.ifEmpty { "unknown" }}, ${if (p.bytes < 0) "unknown size" else "${(p.bytes + 1023) / 1024} KB"}"
        }
    }
}
