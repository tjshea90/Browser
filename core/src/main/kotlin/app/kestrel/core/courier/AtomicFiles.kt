package app.kestrel.core.courier

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Small state files that must survive a kill in the middle of a write: the new text goes to `<name>.tmp` and is synced, the current file is kept as `<name>.bak`, then the temp file
 * is renamed into place. A reader takes the first of [candidates] that parses, so whatever moment the process died, one complete version is there.
 */
object AtomicFiles {
    @Throws(IOException::class)
    fun write(file: File, text: String) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, file.name + ".tmp")
        FileOutputStream(tmp).use { o -> o.write(text.toByteArray(Charsets.UTF_8)); o.fd.sync() }
        if (file.isFile) Files.move(file.toPath(), bak(file).toPath(), StandardCopyOption.REPLACE_EXISTING)
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    fun bak(file: File): File = File(file.parentFile, file.name + ".bak")

    /** The text of each existing version, newest first. */
    fun candidates(file: File): List<String> {
        val out = ArrayList<String>()
        for (f in listOf(file, bak(file))) if (f.isFile) try { out.add(String(Files.readAllBytes(f.toPath()), Charsets.UTF_8)) } catch (_: IOException) {}
        return out
    }
}
