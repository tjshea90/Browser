package app.kestrel.core.prompt

/**
 * IMG1 (owner 2026-11-21: "I'm trying to upload screenshots to claude and it says can't process images"): what a picked file LOOKS like to Android before GeckoView copies it for the page: its type, its size, whether Android can read
 * the picture's header, whether its name has an extension (the engine takes the page's content type from the extension of the copy), and how much room the app's cache has (the copy goes there). Pure, so the verdict words are tested.
 * Never a file name, never a path, never from a private tab (the caller checks).
 */
object FilePickFacts {
    /** One picked file: [type] as the content provider says (may be empty), [bytes] (-1 = unknown), [w] x [h] from the picture's header (0 = could not read), [nameExt] the display name has an extension, [freeMb] free room in the cache (-1 = unknown). */
    data class Picked(val type: String, val bytes: Long, val w: Int, val h: Int, val nameExt: Boolean, val freeMb: Long)

    /** The one-line account for the Health ring / the live diagnostics: numbers and a type, nothing else. */
    fun logLine(p: Picked): String =
        "type=" + (p.type.lowercase().filter { it.isLetterOrDigit() || it == '/' || it == '+' || it == '.' || it == '-' }.take(40).ifEmpty { "none" }) +
            " kb=" + (if (p.bytes < 0) -1 else (p.bytes + 1023) / 1024) + " size=${p.w}x${p.h} decodes=" + (if (p.w > 0 && p.h > 0) 1 else 0) + " ext=" + (if (p.nameExt) 1 else 0) + " freeMb=${p.freeMb}"

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
