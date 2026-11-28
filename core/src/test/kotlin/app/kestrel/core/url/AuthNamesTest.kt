package app.kestrel.core.url

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthNamesTest {
    @Test fun theServerThatAsksNamesTheDialogNotThePageTheTabIsOn() {
        val l = AuthNames.label("https://evil.example/login", "https://bank.example/account")
        assertEquals("evil.example", l.host)
        assertEquals("This sign-in is asked for by evil.example, not by the page you are on (bank.example). ", l.note)
    }

    @Test fun theSameHostAddsNoNoteAndMissingPartsFallBack() {
        assertEquals("", AuthNames.label("https://router.local/admin", "http://router.local/").note)
        assertEquals("router.local", AuthNames.label("https://Router.Local/admin", "http://router.local/").host)
        assertEquals("bank.example", AuthNames.label(null, "https://bank.example/a").host)
        assertEquals("", AuthNames.label(null, "https://bank.example/a").note)
        assertEquals("evil.example", AuthNames.label("https://evil.example/x", null).host)
        assertEquals("this site", AuthNames.label(null, null).host)
        assertEquals("", AuthNames.label("not a url", "also not").note)
    }
}
