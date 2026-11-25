package app.kestrel.core.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashGuardTest {
    private class Mem : CrashGuard.Store { var text = ""; var failing = false; override fun read() = if (failing) throw RuntimeException("disk") else text; override fun write(text: String) { if (failing) throw RuntimeException("disk"); this.text = text } }
    private var t = 1_000_000L
    private fun guard(m: Mem = Mem()) = CrashGuard(m, { t })

    @Test fun aHealthyStartIsNeverSafeMode() { val g = guard(); repeat(10) { assertFalse(g.onStart()); g.healthy(); t += 5_000 } }
    @Test fun threeStartsThatNeverBecameHealthyWithinAMinuteMeanSafeMode() {
        val g = guard(); assertFalse(g.onStart()); t += 10_000; assertFalse(g.onStart()); t += 10_000; assertTrue(g.onStart())
    }
    @Test fun slowCrashesDoNotTripIt() { val g = guard(); repeat(6) { assertFalse(g.onStart()); t += 61_000 } }
    @Test fun oneHealthyStartForgivesTheEarlierCrashes() { val g = guard(); g.onStart(); t += 1000; g.onStart(); t += 1000; g.healthy(); assertFalse(g.onStart()) }
    @Test fun safeModeKeepsGoingUntilAHealthyStart() { val g = guard(); g.onStart(); g.onStart(); assertTrue(g.onStart()); assertTrue(g.onStart()); g.healthy(); assertFalse(g.onStart()) }
    @Test fun aBrokenStoreNeverBreaksTheKeyboard() { val m = Mem().also { it.failing = true }; val g = guard(m); assertFalse(g.onStart()); g.healthy() }
    @Test fun garbageInTheFileIsIgnored() { val m = Mem().also { it.text = "abc\n??? 12x" }; assertFalse(guard(m).onStart()) }
    @Test fun theFileHoldsOnlyNumbersAndStaysSmall() { val m = Mem(); val g = guard(m); repeat(30) { g.onStart(); t += 100 }; assertTrue(m.text.all { it.isDigit() || it == ' ' }); assertEquals(true, m.text.split(' ').size <= 8) }
}
