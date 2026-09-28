package dev.snuffac.core.enforcement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snuffac.api.Vec3d;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EnforcementServiceTest {

    private EnforcementService service;
    private List<EnforcementRequest> handled;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        service = new EnforcementService();
        handled = new ArrayList<>();
        service.handler(handled::add);
        playerId = UUID.randomUUID();
    }

    @Test
    @DisplayName("a flag only request never counts as prevention but is still recorded")
    void flagOnlyIsNotPrevention() {
        assertFalse(service.apply(EnforcementRequest.flagOnly(playerId, "fly", "test")));
        assertEquals(1, service.countOf(EnforcementType.FLAGGED_ONLY));
        assertEquals(1, service.actionsFor(playerId));
    }

    @Test
    @DisplayName("a high confidence setback is applied")
    void highConfidenceSetbackApplies() {
        assertTrue(service.apply(EnforcementRequest.setback(
                playerId, "speed", "excess", new Vec3d(1.0, 64.0, 1.0), 0.9)));
        assertEquals(1, handled.size());
    }

    @Test
    @DisplayName("a low confidence request is recorded but not applied")
    void lowConfidenceIsGated() {
        assertFalse(service.apply(EnforcementRequest.cancel(
                EnforcementType.CANCEL_ATTACK, playerId, "reach", "too far", 0.1)));
        assertTrue(handled.isEmpty(), "a gated request must not reach the platform handler");
        assertEquals(1, service.countOf(EnforcementType.CANCEL_ATTACK),
                "but it must still be counted for diagnostics");
    }

    @Test
    @DisplayName("prevention can be disabled globally without losing records")
    void preventionCanBeDisabled() {
        service.preventionEnabled(false);
        assertFalse(service.apply(EnforcementRequest.cancel(
                EnforcementType.CANCEL_ATTACK, playerId, "reach", "too far", 1.0)));
        assertTrue(handled.isEmpty());
        assertEquals(1, service.countOf(EnforcementType.CANCEL_ATTACK));
    }

    @Test
    @DisplayName("the confidence gate is configurable")
    void confidenceGateConfigurable() {
        service.minConfidenceForPrevention(0.95);
        assertFalse(service.apply(EnforcementRequest.cancel(
                EnforcementType.CANCEL_ATTACK, playerId, "reach", "x", 0.9)));
        assertTrue(service.apply(EnforcementRequest.cancel(
                EnforcementType.CANCEL_ATTACK, playerId, "reach", "x", 0.99)));
    }

    @Test
    @DisplayName("actions are counted per player and per type")
    void countsByPlayerAndType() {
        service.apply(EnforcementRequest.setback(playerId, "fly", "a", Vec3d.ZERO, 1.0));
        service.apply(EnforcementRequest.setback(playerId, "fly", "b", Vec3d.ZERO, 1.0));
        assertEquals(2, service.actionsFor(playerId));
        assertEquals(2, service.countOf(EnforcementType.SETBACK_POSITION));
    }

    @Test
    @DisplayName("forgetting a player clears their counters")
    void forgetClearsCounters() {
        service.apply(EnforcementRequest.setback(playerId, "fly", "a", Vec3d.ZERO, 1.0));
        service.forget(playerId);
        assertEquals(0, service.actionsFor(playerId));
    }

    @Test
    @DisplayName("request types report whether they prevent anything")
    void preventionClassification() {
        assertTrue(EnforcementRequest.setback(playerId, "x", "y", Vec3d.ZERO, 1.0).preventsAnything());
        assertTrue(EnforcementRequest.cancel(
                EnforcementType.CANCEL_ATTACK, playerId, "x", "y", 1.0).preventsAnything());
        assertFalse(EnforcementRequest.flagOnly(playerId, "x", "y").preventsAnything());
    }
}
