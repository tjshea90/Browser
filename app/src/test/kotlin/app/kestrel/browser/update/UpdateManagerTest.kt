package app.kestrel.browser.update

import app.kestrel.core.update.UpdateLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The JSON half of the updater (org.json is the framework's): a realistic GitHub Releases answer, including the fields GitHub may send as null. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class UpdateManagerTest {
    private val sha = "b47fdf4cb02d3a2f5fe0a0761d503a0e81abf636dce81260a81aa5a9b668e8ce"

    private val json = """
        [
          {"url":"https://api.github.com/repos/o/r/releases/1","tag_name":"kestrel-g-v0.2.0-alpha4","name":"Kestrel G 0.2.0-alpha4","draft":false,"prerelease":true,
           "html_url":"https://github.com/o/r/releases/tag/kestrel-g-v0.2.0-alpha4",
           "body":"New dock.\r\n\r\n**Kestrel G 0.2.0-alpha4** (versionCode 103). About.\r\n\r\n- APK SHA-256: `$sha`\r\n",
           "assets":[
             {"name":"kestrel-g-0.2.0-alpha4.apk","size":96468992,"digest":null,"browser_download_url":"https://github.com/o/r/releases/download/kestrel-g-v0.2.0-alpha4/kestrel-g-0.2.0-alpha4.apk"},
             {"name":"kestrel-g-0.2.0-alpha4.apk.sha256","size":100,"digest":"sha256:$sha","browser_download_url":"https://github.com/o/r/releases/download/kestrel-g-v0.2.0-alpha4/kestrel-g-0.2.0-alpha4.apk.sha256"}
           ]},
          {"tag_name":"kestrel-v0.1.1","name":"Kestrel 0.1.1","draft":false,"prerelease":false,"html_url":"x","body":"**Kestrel 0.1.1** (versionCode 2).","assets":[]}
        ]
    """.trimIndent()

    @Test fun parsesAGithubAnswer() {
        val rs = UpdateManager.parseReleases(json)
        assertEquals(2, rs.size)
        assertEquals("kestrel-g-v0.2.0-alpha4", rs[0].tag)
        assertEquals(true, rs[0].prerelease)
        assertEquals(2, rs[0].assets.size)
        assertNull(rs[0].assets[0].digest) // JSON null must not turn into the text "null"
        assertEquals("sha256:$sha", rs[0].assets[1].digest)
        assertEquals(96468992L, rs[0].assets[0].size)
    }

    @Test fun offersTheUpdateOnlyToTheRightAppAndVersion() {
        val rs = UpdateManager.parseReleases(json)
        val o = UpdateLogic.choose(rs, "kestrel-g-v", 102)
        assertNotNull(o)
        assertEquals(103L, o!!.versionCode)
        assertEquals("kestrel-g-0.2.0-alpha4.apk", o.apkName)
        assertEquals(sha, o.sha256)
        assertEquals("New dock.", o.notes)
        assertNull(UpdateLogic.choose(rs, "kestrel-g-v", 103))
        assertNull(UpdateLogic.choose(rs, "kestrel-v", 2)) // web app is current
    }
}
