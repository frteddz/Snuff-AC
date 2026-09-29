package dev.snuffac.core.enforcement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PreventionSignalTest {

    @Test
    void startsWithNothingPending() {
        PreventionSignal signal = new PreventionSignal();
        assertFalse(signal.peek().cancelAttack());
        assertFalse(signal.peek().requestSetback());
    }

    @Test
    void anAttackCancellationIsVisibleToTheGate() {
        PreventionSignal signal = new PreventionSignal();
        signal.cancelAttack("reach", "too far");
        assertTrue(signal.peek().cancelAttack());
        assertEquals("reach", signal.peek().checkKey());
        assertEquals("too far", signal.peek().reason());
    }

    @Test
    void takingClearsTheVerdictSoOnePacketIsCancelledOnce() {
        PreventionSignal signal = new PreventionSignal();
        signal.cancelAttack("reach", "too far");
        assertTrue(signal.take().cancelAttack());
        assertFalse(signal.take().cancelAttack(), "a second packet must not be cancelled");
    }

    @Test
    void separateSignalKindsDoNotLeakIntoEachOther() {
        PreventionSignal signal = new PreventionSignal();
        signal.cancelPlacement("fastplace", "two blocks in one tick");
        var verdict = signal.take();
        assertTrue(verdict.cancelPlacement());
        assertFalse(verdict.cancelAttack());
        assertFalse(verdict.cancelInteraction());
    }

    @Test
    void clearDiscardsAPendingVerdict() {
        PreventionSignal signal = new PreventionSignal();
        signal.cancelAttack("reach", "too far");
        signal.clear();
        assertFalse(signal.take().cancelAttack());
    }

    @Test
    void aSetbackRequestSurvivesThePacketThatTriggeredIt() {
        PreventionSignal signal = new PreventionSignal();
        signal.requestSetback("speed", "excess 0.4");
        var peeked = signal.peek();
        assertTrue(peeked.requestSetback());
        assertTrue(signal.peek().requestSetback(), "peeking must not consume the verdict");
    }

    @Test
    void laterVerdictKindsDoNotDiscardAnEarlierOne() {
        PreventionSignal signal = new PreventionSignal();
        signal.cancelAttack("reach", "too far");
        signal.requestSetback("reach", "too far");
        var verdict = signal.take();
        assertTrue(verdict.cancelAttack());
        assertTrue(verdict.requestSetback());
    }

    @Test
    void countersRecordWhatTheGateActuallyBlocked() {
        PreventionSignal signal = new PreventionSignal();
        signal.recordAttackBlock();
        signal.recordAttackBlock();
        signal.recordPlacementBlock();
        signal.recordSetback();
        assertEquals(2L, signal.attackBlocks());
        assertEquals(1L, signal.placementBlocks());
        assertEquals(0L, signal.interactionBlocks());
        assertEquals(1L, signal.setbacks());
    }

    @Test
    void aNewSignalStartsWithZeroedCounters() {
        PreventionSignal signal = new PreventionSignal();
        assertEquals(0L, signal.attackBlocks());
        assertEquals(0L, signal.setbacks());
    }
}
