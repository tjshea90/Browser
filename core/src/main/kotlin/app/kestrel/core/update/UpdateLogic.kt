package app.kestrel.core.update

/** One file attached to a GitHub Release. [digest] is GitHub's own "sha256:<hex>" field when it sent one. */
class ReleaseAsset(val name: String, val url: String, val size: Long, val digest: String?)

/** A GitHub Release as the in-app updater needs it (the Android side fills this from the Releases API JSON). */
class Release(
    val tag: String,
    val title: String,
    val body: String,
    val prerelease: Boolean,
    val draft: Boolean,
    val pageUrl: String,
    val assets: List<ReleaseAsset>,
)

/** A release that is newer than the installed app and has an APK to install. */
class UpdateOffer(
    val tag: String,
    val versionName: String,
    val versionCode: Long,
    val apkName: String,
    val apkUrl: String,
    val size: Long,
    val sha256: String?,
    val notes: String,
    val pageUrl: String,
)

/**
 * The decisions of the in-app updater, free of Android and of any network code so they are unit-tested.
 *
 * What GitHub Actions publishes (`.github/scripts/publish-release.sh`): tag `kestrel-g-v<versionName>` (Kestrel G) or `kestrel-v<versionName>` (Kestrel),
 * an APK asset, and a body of the form
 *
 *     <change note>
 *
 *     **Kestrel G 0.2.0-alpha4** (versionCode 103). <about text>
 *
 *     - Install: ...
 *     - APK SHA-256: `<64 hex>`
 *
 * The versionCode in the body is what is compared with the installed app's versionCode; the SHA-256 in the body is what the downloaded file is checked against.
 * Whatever is installed is ALSO checked by Android itself: an update is accepted only when it carries the same signing certificate as the installed app.
 */
object UpdateLogic {
    private val CODE = Regex("""\(versionCode\s+(\d+)\)""")
    private val SHA = Regex("""APK SHA-256:\s*`?([0-9a-fA-F]{64})`?""")

    fun versionCode(body: String): Long? = CODE.find(body)?.groupValues?.get(1)?.toLongOrNull()

    /** The checksum of the APK: the body line we wrote ourselves first, GitHub's per-asset digest as the fallback. Lower case, or null. */
    fun sha256(body: String, asset: ReleaseAsset?): String? {
        SHA.find(body)?.groupValues?.get(1)?.let { return it.lowercase() }
        val d = asset?.digest ?: return null
        if (!d.startsWith("sha256:")) return null
        return d.substring(7).takeIf { it.length == 64 && it.all { c -> c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F' } }?.lowercase()
    }

    /** The human change note: everything before the generated "**Title version** (versionCode N)" paragraph. */
    fun notes(body: String): String {
        val at = Regex("""(?m)^\*\*.+\*\*\s+\(versionCode""").find(body)?.range?.first ?: body.length
        return body.substring(0, at).trim().take(700)
    }

    /**
     * The newest release of THIS app (its [tagPrefix]) whose versionCode is above [currentCode] and that has an APK; null when the app is up to date.
     * Drafts are never offered. Pre-releases are offered (every Kestrel G build so far is an alpha).
     */
    fun choose(releases: List<Release>, tagPrefix: String, currentCode: Long): UpdateOffer? {
        var best: UpdateOffer? = null
        for (r in releases) {
            if (r.draft || !r.tag.startsWith(tagPrefix)) continue
            val code = versionCode(r.body) ?: continue
            if (code <= currentCode || (best != null && code <= best.versionCode)) continue
            val apk = r.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) } ?: continue
            best = UpdateOffer(r.tag, r.tag.removePrefix(tagPrefix), code, apk.name, apk.url, apk.size, sha256(r.body, apk), notes(r.body), r.pageUrl)
        }
        return best
    }

    /** 92.4 MB style size for the confirm dialog and progress line. */
    fun formatSize(bytes: Long): String = when {
        bytes < 0 -> "unknown size"
        bytes < 1024 -> "$bytes B"
        bytes < 1024L * 1024 -> "${bytes / 1024} KB"
        bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(java.util.Locale.ROOT, bytes / 1048576.0)
        else -> "%.2f GB".format(java.util.Locale.ROOT, bytes / 1073741824.0)
    }

    /**
     * What Android's installer needs before it will finish an install or an update. MEASURED (research/UPDATE_STORAGE.md): it refuses (INSTALL_FAILED_INSUFFICIENT_STORAGE, installd "Failed to free up <10 % of /data>")
     * unless, once the install has taken its own room, 10 % of the data partition ([reserve]) is still free. The install takes the staged copy of the APK, the native libraries it unpacks and the optimised code.
     * [free] = free space + the caches the system may clear (StorageStatsManager.getFreeBytes). Our own downloaded copy is already counted as used; it is deleted once the installer holds its own copy
     * ([StoragePlan.fitsDropping]), which hands one APK of room back.
     * This is why "uninstall first" always worked: the uninstall frees the old install and its data (about a gigabyte), so the same APK then clears the bar. In place and fresh need the same free space BEFORE.
     */
    class StoragePlan(val needKeep: Long, val needDrop: Long, val free: Long, val reserve: Long, val installTakes: Long) {
        /** Starts with our downloaded file kept (so a retry needs no new download). */
        val fitsKeeping: Boolean get() = free >= needKeep
        /** Fits only if our own copy is deleted right after the installer has taken its copy. */
        val fitsDropping: Boolean get() = !fitsKeeping && free >= needDrop
        /** Bytes that must still be freed (the copy being dropped); 0 = it fits. */
        val shortBy: Long get() = if (free >= needDrop) 0L else needDrop - free
    }

    /** [marginBytes]: the optimised code the installer compiles (measured about 100 MB for a debug build with a big dex; a release build needs far less). */
    fun storagePlan(apkBytes: Long, nativeLibBytes: Long, freeBytes: Long, reserveBytes: Long, marginBytes: Long = 64L shl 20): StoragePlan {
        val needDrop = reserveBytes + nativeLibBytes + marginBytes // the staged copy takes what our deleted copy gives back
        return StoragePlan(needKeep = needDrop + apkBytes, needDrop = needDrop, free = freeBytes, reserve = reserveBytes, installTakes = apkBytes + nativeLibBytes + marginBytes)
    }

    /** Round a byte count UP to whole tens of megabytes (what the owner is told to free). */
    fun roundUpTenMb(bytes: Long): Long { val ten = 10L shl 20; return ((bytes + ten - 1) / ten) * ten }

    /** A download may continue from byte [have] only when the server answered 206 for exactly that offset (else the partial file must be restarted). */
    fun canResume(have: Long, status: Int, contentRangeStart: Long?): Boolean = have > 0 && status == 206 && contentRangeStart == have
}
