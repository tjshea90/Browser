package app.kestrel.core.filter

import org.junit.Assert.assertEquals
import org.junit.Test

class BridgeLaneTest {
    @Test fun theImageChunksHaveTheirOwnLane() {
        assertEquals(BridgeLane.IMAGE, BridgeLane.of("{\"k\":\"img\",\"g\":3,\"i\":0}"))
    }

    @Test fun elementHidingAndItsLazyBatchesShareTheCosmeticLane() {
        assertEquals(BridgeLane.COSMETIC, BridgeLane.of("{\"k\":\"css\",\"h\":\"example.com\",\"top\":true}"))
        assertEquals(BridgeLane.COSMETIC, BridgeLane.of("{\"k\":\"gen\",\"h\":\"example.com\",\"t\":[\"ad\"]}"))
    }

    @Test fun everythingElseIsQuick() {
        // "sc" (the scriptlet calls of a host, asked before its document loads) is a cheap lookup that must never queue behind the slow first build of the generic hiding set
        for (k in listOf("req", "cfg", "cnt", "st", "pop", "media", "vid", "sc", "nope")) assertEquals(k, BridgeLane.QUICK, BridgeLane.of("{\"k\":\"$k\",\"u\":\"https://a.example/\"}"))
    }

    @Test fun aKindThatOnlyStartsLikeAnotherIsNotMistakenForIt() {
        assertEquals(BridgeLane.QUICK, BridgeLane.of("{\"k\":\"imgx\"}"))
        assertEquals(BridgeLane.QUICK, BridgeLane.of("{\"k\":\"cssy\"}"))
        assertEquals(BridgeLane.QUICK, BridgeLane.of("{\"k\":\"ge\"}"))
    }

    @Test fun anythingThatIsNotOurShapeIsQuickAndNeverThrows() {
        assertEquals(BridgeLane.QUICK, BridgeLane.of(null))
        assertEquals(BridgeLane.QUICK, BridgeLane.of(42))
        assertEquals(BridgeLane.QUICK, BridgeLane.of(""))
        assertEquals(BridgeLane.QUICK, BridgeLane.of("{\"g\":1,\"k\":\"css\"}"))
        assertEquals(BridgeLane.QUICK, BridgeLane.of("{\"k\":\"cs"))
    }
}
