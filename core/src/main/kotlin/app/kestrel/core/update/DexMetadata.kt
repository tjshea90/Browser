package app.kestrel.core.update

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/**
 * LAB6 (owner message "Go", 2026-11-17; research/LAB5_PACKAGING.md): compile the app WHILE IT INSTALLS.
 *
 * A sideloaded update is only verified by Android; its code then runs in the interpreter until the idle-and-charging dexopt compiles it (and every update resets that). Android 10 compiles an install with the
 * start-up profile when the PackageInstaller session carries a dex-metadata file next to the APK (`<apk name>.dm`, a tiny zip with `primary.prof`, what Google Play does). Measured on the emulator (aot lab run
 * 44074456934): `[reason=install-dm]`, 3.75 MB of compiled code instead of 158 KB, 0.3 s longer install. AGP writes the file next to the release build (`outputs/apk/release/baselineProfiles/1/` is the format of
 * Android 9 to 11; the `0` one is ignored there); the release workflow publishes it as `<apk name without .apk>.dm`, the updater fetches it, checks it, and writes it into the session.
 *
 * Everything here is a pure rule so it is unit-tested; the Android side (fetch, session, retry) is app/.../update/UpdateManager.kt and InstallResultReceiver.kt.
 */
object DexMetadata {
    /** The AGP file of this app is about 7 KB; anything near a megabyte is not it. */
    const val MAX_BYTES = 1_000_000L

    /** The profile format of `baselineProfiles/1` (Android 9 to 11). Other versions get no file: the install is exactly what it was. */
    const val MIN_SDK = 28
    const val MAX_SDK = 30

    /** After the installer refused a file, none is attached for this long (a platform that does not take it would refuse it at every update, each time costing a retry). */
    const val REFUSED_FOR_MS = 14L * 24 * 60 * 60 * 1000

    /** `kestrel-g-0.3.0-alpha65.apk` -> `kestrel-g-0.3.0-alpha65.dm`. */
    fun nameFor(apkName: String): String = (if (apkName.endsWith(".apk", ignoreCase = true)) apkName.dropLast(4) else apkName) + ".dm"

    /** May a file be attached to an install now: only on the Android versions the profile format fits, and not within [REFUSED_FOR_MS] after the installer refused one ([refusedAtMs] <= 0 = never). */
    fun mayAttach(sdk: Int, refusedAtMs: Long, nowMs: Long): Boolean {
        if (sdk !in MIN_SDK..MAX_SDK) return false
        if (refusedAtMs <= 0L) return true
        return nowMs - refusedAtMs !in 0L until REFUSED_FOR_MS
    }

    /**
     * A cheap look before the file is handed to Android: a flat zip (a few entries, no folders) that holds `primary.prof`, small enough. Android validates the real content itself
     * (INSTALL_FAILED_BAD_DEX_METADATA for anything it does not take); this only keeps an obviously wrong file (an error page saved as .dm, a truncated download) out of the session.
     */
    fun isPlausible(bytes: ByteArray): Boolean {
        if (bytes.size < 30 || bytes.size > MAX_BYTES) return false
        if (bytes[0] != 'P'.code.toByte() || bytes[1] != 'K'.code.toByte() || bytes[2] != 3.toByte() || bytes[3] != 4.toByte()) return false
        return try {
            var entries = 0
            var prof = false
            ZipInputStream(ByteArrayInputStream(bytes)).use { z ->
                while (true) {
                    val e = z.nextEntry ?: break
                    entries++
                    if (entries > 6 || e.isDirectory || e.name.contains('/') || e.name.contains('\\') || e.name.contains("..")) return false
                    if (e.name == "primary.prof") prof = true
                }
            }
            prof
        } catch (e: Exception) { false }
    }

    /** The installer's failure text names the dex metadata (`INSTALL_FAILED_BAD_DEX_METADATA: ...`): the file, not the app, was refused. */
    fun isRefusal(message: String?): Boolean = message != null && (message.contains("DEX_METADATA", ignoreCase = true) || message.contains("dex metadata", ignoreCase = true) || message.contains("dexmetadata", ignoreCase = true))
}
