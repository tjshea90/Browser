package app.kestrel.core.prompt

import java.io.File
import java.io.InputStream

/**
 * PERF14 (owner 2026-11-23: "Photos still won't attach to claude website"; research/ATTACH_ROOT_CAUSE.md): GeckoView's file prompt turns a picked `content://` address into a RAW FILE PATH and the engine opens that path itself,
 * which Android 10 and later allow only with the storage permission, so the page got a File it could not read. The app therefore copies every picked file, with the grant the picker gave for exactly that file, into its OWN cache
 * and answers the prompt with the address of the copy. This is the pure part: a safe file name, the copy with a size cap, and the clean-up of old copies. No Android classes (java.io only), so it is tested on the JVM.
 */
object PickStore {
    /** Longest file name kept (the extension is kept whole). */
    const val MAX_NAME = 80
    /** One file may not be bigger than this (bytes); a bigger one is refused, nothing is left behind. */
    const val MAX_BYTES = 1_610_612_736L

    private val EXT = mapOf(
        "image/png" to "png", "image/jpeg" to "jpg", "image/jpg" to "jpg", "image/webp" to "webp", "image/gif" to "gif", "image/heic" to "heic", "image/heif" to "heif", "image/avif" to "avif", "image/bmp" to "bmp",
        "image/svg+xml" to "svg", "application/pdf" to "pdf", "text/plain" to "txt", "text/csv" to "csv", "text/html" to "html", "application/json" to "json", "application/zip" to "zip",
        "application/msword" to "doc", "application/vnd.openxmlformats-officedocument.wordprocessingml.document" to "docx", "application/vnd.ms-excel" to "xls",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" to "xlsx", "application/vnd.openxmlformats-officedocument.presentationml.presentation" to "pptx",
        "audio/mpeg" to "mp3", "audio/mp4" to "m4a", "audio/ogg" to "ogg", "audio/wav" to "wav", "video/mp4" to "mp4", "video/webm" to "webm", "video/quicktime" to "mov",
    )

    /** The extension (no dot) a file of this type should have, or "" when the type is unknown. */
    fun extFor(mime: String?): String = EXT[(mime ?: "").substringBefore(';').trim().lowercase()] ?: ""

    /**
     * A name that is safe to create under the copy's own folder: no path separators or control characters, no leading dots, at most [MAX_NAME] characters with the extension kept, and an extension from [mime]
     * when the name has none (the engine takes the page's content type from the extension). Never empty.
     */
    fun safeName(display: String?, mime: String?): String {
        var n = (display ?: "").substringAfterLast('/').substringAfterLast('\\')
        n = n.filter { it.code >= 32 && it.code != 127 && it !in "<>:\"|?*" }.trim().trimStart('.')
        var ext = ""
        val dot = n.lastIndexOf('.')
        if (dot in 1 until n.length - 1 && n.length - dot <= 9) { ext = n.substring(dot); n = n.substring(0, dot) }
        if (ext.isEmpty()) extFor(mime).let { if (it.isNotEmpty()) ext = ".$it" }
        if (n.isEmpty()) n = "file"
        val room = (MAX_NAME - ext.length).coerceAtLeast(8)
        if (n.length > room) n = n.substring(0, room)
        return n + ext
    }

    /**
     * Copies [input] to `<root>/<id>/<safeName>` and returns that file; null when [input] is bigger than [maxBytes] or the copy failed (the folder is removed again, nothing is left behind). [id] must be a plain token
     * (letters and digits): the caller makes it unique.
     */
    fun copy(root: File, id: String, input: InputStream, display: String?, mime: String?, maxBytes: Long = MAX_BYTES): File? {
        if (id.isEmpty() || !id.all { it.isLetterOrDigit() }) return null
        val dir = File(root, id)
        try {
            if (!dir.mkdirs() && !dir.isDirectory) return null
            val out = File(dir, safeName(display, mime))
            var total = 0L
            out.outputStream().use { o ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    total += n
                    if (total > maxBytes) { dir.deleteRecursively(); return null }
                    o.write(buf, 0, n)
                }
            }
            return out
        } catch (e: Exception) {
            try { dir.deleteRecursively() } catch (x: Exception) { /* nothing more to do */ }
            return null
        }
    }

    /** Removes the copies whose folder was last changed more than [olderThanMs] before [now]; returns how many folders went. */
    fun sweep(root: File, olderThanMs: Long, now: Long): Int {
        var n = 0
        for (d in root.listFiles() ?: return 0) if (now - d.lastModified() > olderThanMs && d.deleteRecursively()) n++
        return n
    }

    /** Removes every copy under [root] (the next start, or the last private tab closing); returns how many folders went. */
    fun wipe(root: File): Int {
        var n = 0
        for (d in root.listFiles() ?: return 0) if (d.deleteRecursively()) n++
        return n
    }
}
