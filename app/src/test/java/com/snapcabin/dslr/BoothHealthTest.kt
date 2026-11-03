package com.snapcabin.dslr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The DSLR failure pause: guests must not wait out a dead DSLR on every shot all night. */
class BoothHealthTest {

    private val t0 = 1_000_000L

    @Test
    fun singleMissDoesNotPause() {
        val h = DslrManager.BoothHealth().afterFailure("AF", t0)
        assertFalse(h.isPaused(t0))
        assertEquals(1, h.consecutiveFailures)
        assertEquals(1, h.fallbacks)
        assertEquals("AF", h.lastFailure)
    }

    @Test
    fun repeatedMissesPauseThenExpire() {
        var h = DslrManager.BoothHealth()
        repeat(DslrManager.FAILURES_BEFORE_PAUSE) { h = h.afterFailure("timeout", t0) }
        assertTrue(h.isPaused(t0))
        assertTrue(h.isPaused(t0 + DslrManager.PAUSE_MS - 1))
        assertFalse(h.isPaused(t0 + DslrManager.PAUSE_MS))
    }

    @Test
    fun missAfterPauseExpiresPausesAgainImmediately() {
        var h = DslrManager.BoothHealth()
        repeat(DslrManager.FAILURES_BEFORE_PAUSE) { h = h.afterFailure("x", t0) }
        val later = t0 + DslrManager.PAUSE_MS
        h = h.afterFailure("still broken", later)
        assertTrue("one more miss after the pause re-pauses", h.isPaused(later))
    }

    @Test
    fun successClearsPauseAndCounts() {
        var h = DslrManager.BoothHealth()
        repeat(DslrManager.FAILURES_BEFORE_PAUSE) { h = h.afterFailure("x", t0) }
        h = h.afterSuccess()
        assertFalse(h.isPaused(t0))
        assertEquals(0, h.consecutiveFailures)
        assertEquals(1, h.dslrShots)
        assertEquals("fallback history is kept for the operator", DslrManager.FAILURES_BEFORE_PAUSE, h.fallbacks)
    }

    @Test
    fun operatorResetGivesFreshChanceButKeepsTonightsCounts() {
        var h = DslrManager.BoothHealth().afterSuccess()
        repeat(DslrManager.FAILURES_BEFORE_PAUSE) { h = h.afterFailure("x", t0) }
        h = h.cleared()
        assertFalse(h.isPaused(t0))
        assertEquals(0, h.consecutiveFailures)
        assertEquals(1, h.dslrShots)
        assertEquals(DslrManager.FAILURES_BEFORE_PAUSE, h.fallbacks)
    }
}
