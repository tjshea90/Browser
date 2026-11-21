package app.kestrel.core.prompt

/**
 * ATT1 (owner 2026-11-21: "I was trying to use Google gemini website but when i tried to add an attachment in kestrel browser it just got stuck and did nothing"): the rules of the file chooser a page opens
 * with `<input type=file accept="...">`. Pure, so the parts that can go wrong without anybody seeing it are tested: which kinds of file the system picker is told to show, and what the Diagnostics line says.
 */
object FilePickRules {
    private const val MAX_TYPES = 32
    private val EXTENSIONS = mapOf(
        "pdf" to "application/pdf", "txt" to "text/plain", "csv" to "text/csv", "json" to "application/json", "md" to "text/markdown", "rtf" to "application/rtf", "html" to "text/html", "xml" to "text/xml",
        "doc" to "application/msword", "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "xls" to "application/vnd.ms-excel", "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "ppt" to "application/vnd.ms-powerpoint", "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "png" to "image/png", "jpg" to "image/jpeg", "jpeg" to "image/jpeg", "gif" to "image/gif", "webp" to "image/webp", "heic" to "image/heic", "heif" to "image/heif", "bmp" to "image/bmp", "svg" to "image/svg+xml",
        "mp3" to "audio/mpeg", "wav" to "audio/x-wav", "m4a" to "audio/mp4", "ogg" to "audio/ogg", "flac" to "audio/flac", "aac" to "audio/aac",
        "mp4" to "video/mp4", "mov" to "video/quicktime", "webm" to "video/webm", "mkv" to "video/x-matroska", "avi" to "video/x-msvideo",
        "zip" to "application/zip", "epub" to "application/epub+zip",
    )

    /**
     * The kinds of file to show in the system picker for a page's `accept` list (MIME types, wildcards such as `image/*`, extensions such as `.pdf`), or null = show every file.
     * The old rule kept only the entries that contain a slash: an input that accepts `.pdf,.docx` was given an empty list (fine), but one that accepts `image/*,.pdf,.docx` was given ONLY pictures, so the
     * picker hid the documents the page asked for. Now every entry must be understood; one that is not (an unknown extension, a word) means no restriction rather than a wrong one. A page does not
     * depend on the filter (it checks what it was given), so showing too many files is harmless and hiding the right one is not.
     */
    fun mimeTypes(accept: Array<String>?): List<String>? {
        if (accept == null) return null
        val out = LinkedHashSet<String>()
        for (raw in accept) {
            val a = raw.trim().lowercase()
            if (a.isEmpty()) continue
            when {
                a == "*/*" || a == "*" -> return null
                a.contains('/') -> { if (a.length > 100 || a.any { it.isWhitespace() }) return null; out.add(a) }
                a.startsWith('.') -> out.add(EXTENSIONS[a.substring(1)] ?: return null)
                else -> return null
            }
        }
        return if (out.isEmpty() || out.size > MAX_TYPES) null else out.toList()
    }
}

/** ATT1: what the file chooser has done since the app started, in numbers, for the Diagnostics screen (nothing about the page or the file is kept). */
class FilePickStats {
    var asked = 0; private set
    var opened = 0; private set
    var noPicker = 0; private set
    var cancelled = 0; private set
    var gave = 0; private set
    var folders = 0; private set
    var failed = 0; private set
    var last = "no file chooser yet"; private set

    fun asked(detail: String) { asked++; last = "asked ($detail)" }
    fun opened(how: String) { opened++; last = "opened with $how" }
    fun noPicker() { noPicker++; last = "no app on this device can choose a file" }
    fun folder() { folders++; last = "the page asked for a whole folder: refused" }
    fun failed(why: String) { failed++; last = "could not open the picker: ${why.take(80)}" }
    fun result(files: Int, code: Int) { if (files > 0) { gave += files; last = "gave $files file(s) to the page" } else { cancelled++; last = "nothing chosen (result code $code)" } }

    fun line(): String = "File chooser (attach a file on a page): asked $asked, opened $opened, no picker app $noPicker, nothing chosen $cancelled, files given to the page $gave, whole-folder asks refused $folders, failed $failed; last: $last"
}
