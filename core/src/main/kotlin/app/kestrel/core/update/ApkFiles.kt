package app.kestrel.core.update

/**
 * Which installer files lying around are used up (AO4, owner message 29: "make sure the app properly deletes old install files and apks from storage after updates and once the update is installed,
 * delete the latest apk"). A release file is named after its tag: `kestrel-g-0.3.0-alpha32.apk` belongs to the tag `kestrel-g-v0.3.0-alpha32`. An installer that is the version running now, or an older one,
 * has nothing left to do; a newer one is an update the owner may still want to install and stays.
 */
object ApkFiles {
    /** "kestrel-g-0.3.0-alpha32.apk" (also "kestrel-g-0.3.0-alpha32 (1).apk", the name Android gives a second copy) -> the part after the app's name. */
    private val NAME = Regex("""^(kestrel(?:-[a-z]+)?)-(\d[^\s]*?)(?:\s*\(\d+\))?\.apk$""", RegexOption.IGNORE_CASE)

    /** The release tag a downloaded installer belongs to given the tag prefix of THIS app ("kestrel-g-v"), or null for a file that is not one of this app's installers. */
    fun tagOf(fileName: String, tagPrefix: String): String? {
        val m = NAME.find(fileName.trim()) ?: return null
        val tag = m.groupValues[1].lowercase() + "-v" + m.groupValues[2]
        return if (tag.startsWith(tagPrefix)) tag else null
    }

    /** True when [fileName] is an installer of this app that is [installedVersionName] or older: nothing is left for it to do. Not an installer of this app, or a newer one: false. */
    fun isUsedUp(fileName: String, tagPrefix: String, installedVersionName: String): Boolean {
        if (installedVersionName.isBlank()) return false
        val tag = tagOf(fileName, tagPrefix) ?: return false
        return UpdateLogic.compareTags(tag, tagPrefix + installedVersionName) <= 0
    }
}
