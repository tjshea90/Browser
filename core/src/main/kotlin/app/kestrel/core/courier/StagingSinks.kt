package app.kestrel.core.courier

import app.kestrel.core.download.DEST_APP_PRIVATE
import app.kestrel.core.download.Reason
import app.kestrel.core.download.Sink
import app.kestrel.core.download.SinkException
import app.kestrel.core.download.SinkFactory
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Files in the courier's private staging folder: `<name>.part` while downloading, renamed to `<name>` by [Sink.publish]. Pure JDK (positional writes on one
 * [FileChannel], safe from several threads), so the phone app and the JVM tests use the same class. A name that is taken gets " (2)", " (3)" ...: two jobs
 * for `video.mp4` must never share a file. The uri is a plain `file:` URI; nothing outside [dir] is ever opened or deleted through it.
 */
class StagingSinks(private val dir: File) : SinkFactory {
    private val lock = Any()

    override fun create(name: String, mime: String?, total: Long, priv: Boolean): Sink = create(name, mime, total, priv, DEST_APP_PRIVATE)

    override fun create(name: String, mime: String?, total: Long, priv: Boolean, kind: Int): Sink {
        synchronized(lock) {
            dir.mkdirs()
            val base = safe(name)
            var final = File(dir, base)
            var n = 2
            while (final.exists() || File(dir, final.name + ".part").exists()) {
                val dot = base.lastIndexOf('.')
                final = File(dir, if (dot > 0) base.substring(0, dot) + " ($n)" + base.substring(dot) else "$base ($n)")
                n++
            }
            val part = File(dir, final.name + ".part")
            val raf = try { RandomAccessFile(part, "rw") } catch (e: IOException) { throw map(e, "could not create the file") }
            try { if (total > 0) raf.setLength(total) } catch (e: IOException) { try { raf.close() } catch (_: IOException) {}; part.delete(); throw map(e, "could not reserve the space") }
            return StagedSink(raf, part, final)
        }
    }

    override fun reopen(uri: String, total: Long): Sink? {
        val f = pathOf(uri) ?: return null
        val part = if (f.name.endsWith(".part")) f else File(f.parentFile, f.name + ".part")
        if (!part.isFile) return null
        val raf = try { RandomAccessFile(part, "rw") } catch (e: IOException) { return null }
        return StagedSink(raf, part, File(part.parentFile, part.name.removeSuffix(".part")))
    }

    override fun delete(uri: String): Boolean {
        val f = pathOf(uri) ?: return false
        val part = if (f.name.endsWith(".part")) f else File(f.parentFile, f.name + ".part")
        var ok = false
        for (x in listOf(f, part)) try { if (x.exists()) ok = x.delete() || ok } catch (_: SecurityException) {}
        return ok
    }

    /** The finished file behind a `file:` uri this factory handed out (or its `.part`), or null when the uri is not one of ours. */
    fun fileOf(uri: String?): File? = if (uri == null) null else pathOf(uri)

    private fun pathOf(uri: String): File? {
        if (!uri.startsWith("file:")) return null
        val f = try { File(java.net.URI(uri).path) } catch (e: Exception) { return null }
        return try { if (f.canonicalPath.startsWith(dir.canonicalPath + File.separator)) f else null } catch (e: IOException) { null }
    }

    private fun safe(name: String): String {
        var s = name.replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001f]"), "_").trim().trim('.')
        if (s.isEmpty()) s = "download"
        if (s.length > 120) { val dot = s.lastIndexOf('.'); s = if (dot in 1..12) s.substring(0, 120 - (s.length - dot)) + s.substring(dot) else s.substring(0, 120) }
        return s
    }

    private fun map(e: IOException, what: String): SinkException {
        val m = (e.message ?: "").lowercase()
        return when {
            m.contains("no space left") -> SinkException(Reason.NO_SPACE, "not enough free space", e)
            m.contains("file too large") -> SinkException(Reason.FS_LIMIT, "file too big for this storage", e)
            else -> SinkException(Reason.STORAGE, what, e)
        }
    }

    private inner class StagedSink(val raf: RandomAccessFile, val part: File, val final: File) : Sink {
        private val ch: FileChannel = raf.channel
        override val uri: String? = part.toURI().toString()
        override val displayName: String? = final.name
        override val readable: Boolean get() = true

        override fun pread(pos: Long, b: ByteArray, off: Int, len: Int): Int = try { ch.read(ByteBuffer.wrap(b, off, len), pos) } catch (e: IOException) { -1 }

        override fun pwrite(pos: Long, b: ByteArray, off: Int, len: Int) {
            try {
                val bb = ByteBuffer.wrap(b, off, len)
                var at = pos
                while (bb.hasRemaining()) { val n = ch.write(bb, at); if (n <= 0) throw SinkException(Reason.STORAGE, "write returned $n"); at += n }
            } catch (e: IOException) { throw map(e, "write failed") }
        }

        override fun sync() { try { ch.force(false) } catch (e: IOException) { throw map(e, "sync failed") } }
        override fun length(): Long = try { ch.size() } catch (e: IOException) { -1L }
        override fun truncate(len: Long) { try { if (len < ch.size()) ch.truncate(len) } catch (e: IOException) { /* stale bytes at the end are harmless to the engine */ } }

        override fun publish(): String? {
            try { ch.force(false) } catch (_: IOException) {}
            try { raf.close() } catch (_: IOException) {}
            try { Files.move(part.toPath(), final.toPath(), StandardCopyOption.REPLACE_EXISTING) } catch (e: IOException) { throw SinkException(Reason.STORAGE, "could not store the file", e) }
            return final.toURI().toString()
        }

        override fun discard() {
            try { raf.close() } catch (_: IOException) {}
            try { part.delete() } catch (_: SecurityException) {}
        }
    }
}
