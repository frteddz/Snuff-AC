package dev.snuffac.core.prediction;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snuffac.core.player.MovementState;
import org.junit.jupiter.api.Test;

class PredictionGracesTest {

    private static MovementState newState() {
        return new MovementState(new dev.snuffac.core.tolerance.ToleranceModel(0.5, 1.0, 1.0, 0.4));
    }

    @Test
    void joinGraceCoversTheFirstFiveSeconds() {
        long join = 1_000_000L;
        assertTrue(PredictionGraces.joinGraceActive(join, join),
                "a player is graced the instant they join");
        assertTrue(PredictionGraces.joinGraceActive(join, join + 4_999L));
        assertFalse(PredictionGraces.joinGraceActive(join, join + 5_000L),
                "grace must end at exactly five seconds");
        assertFalse(PredictionGraces.joinGraceActive(join, join + 60_000L));
    }

    @Test
    void lowTpsSuppressesChecks() {
        assertTrue(PredictionGraces.serverLagged(17.9));
        assertFalse(PredictionGraces.serverLagged(18.0), "18 is the documented floor");
        assertFalse(PredictionGraces.serverLagged(20.0));
    }

    @Test
    void unknownTpsIsNotTreatedAsLag() {
        assertFalse(PredictionGraces.serverLagged(0.0),
                "an unmeasured tps must not disable every check");
        assertFalse(PredictionGraces.serverLagged(-1.0));
    }

    @Test
    void highPingSuppressesChecks() {
        assertTrue(PredictionGraces.pingUnreliable(301.0));
        assertFalse(PredictionGraces.pingUnreliable(300.0));
        assertFalse(PredictionGraces.pingUnreliable(45.0));
    }

    @Test
    void windChargeGracesThreeSecondsAfterUse() {
        MovementState state = newState();
        state.markWindCharge();
        assertTrue(PredictionGraces.windChargeActive(state));
        for (int i = 0; i < PredictionGraces.WIND_CHARGE_TICKS - 1; i++) {
            state.tickWindChargeCounters();
        }
        assertTrue(PredictionGraces.windChargeActive(state),
                "still inside the three second window");
        state.tickWindChargeCounters();
        assertFalse(PredictionGraces.windChargeActive(state),
                "grace must expire after three seconds");
    }

    @Test
    void windChargeHitGracesOneSecond() {
        MovementState state = newState();
        state.markWindChargeHit();
        assertTrue(PredictionGraces.windChargeActive(state));
        for (int i = 0; i < PredictionGraces.WIND_CHARGE_HIT_TICKS; i++) {
            state.tickWindChargeCounters();
        }
        state.tickWindChargeCounters();
        assertFalse(PredictionGraces.windChargeActive(state),
                "the shorter receive window must also expire");
    }

    @Test
    void windChargeReasonNamesTheSource() {
        MovementState state = newState();
        state.markWindCharge();
        assertTrue(PredictionGraces.windChargeReason(state).contains("wind charge used"));
        MovementState other = newState();
        other.markWindChargeHit();
        assertTrue(PredictionGraces.windChargeReason(other).contains("wind charged"));
    }

    @Test
    void aCleanPlayerIsNotInAnyGrace() {
        MovementState state = newState();
        for (int i = 0; i < 200; i++) {
            state.tickWindChargeCounters();
        }
        assertFalse(PredictionGraces.windChargeActive(state));
        assertTrue(PredictionGraces.windChargeReason(state).isEmpty());
    }
}
