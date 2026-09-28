package dev.snuffac.core.tolerance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snuffac.api.Vec3d;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ToleranceModelTest {

    @Test
    @DisplayName("a fresh model forgives nothing")
    void startsEmpty() {
        ToleranceModel model = ToleranceModel.defaults();
        assertEquals(Vec3d.ZERO, model.total());
        assertEquals(0.0, model.toleranceBox().x(), 1.0E-9);
    }

    @Test
    @DisplayName("contributions from a source are added together")
    void accumulatesPerSource() {
        ToleranceModel model = ToleranceModel.defaults();
        model.add(ToleranceSource.ITEM_USE_SLOWDOWN, 0.05);
        model.add(ToleranceSource.ITEM_USE_SLOWDOWN, 0.05);
        assertEquals(0.10, model.toleranceFor(ToleranceSource.ITEM_USE_SLOWDOWN).x(), 1.0E-6);
    }

    @Test
    @DisplayName("tolerance from different sources sums into the total")
    void totalsAcrossSources() {
        ToleranceModel model = ToleranceModel.defaults();
        model.add(ToleranceSource.ITEM_USE_SLOWDOWN, 0.05);
        model.add(ToleranceSource.ATTACK_SLOWDOWN, 0.05);
        assertEquals(0.10, model.total().x(), 1.0E-6);
    }

    @Test
    @DisplayName("contributions decay towards zero over successive ticks")
    void decaysOverTicks() {
        ToleranceModel model = new ToleranceModel(0.5, 1.0, 1.0, 0.4);
        model.add(ToleranceSource.EXTERNAL_PUSH, 0.10);
        model.advanceTick(1L);
        assertEquals(0.05, model.total().x(), 1.0E-6);
        model.advanceTick(2L);
        assertEquals(0.025, model.total().x(), 1.0E-6);
    }

    @Test
    @DisplayName("the accumulated tolerance is capped so it cannot grow without bound")
    void clampsTotal() {
        ToleranceModel model = new ToleranceModel(1.0, 0.20, 1.0, 0.4);
        model.add(ToleranceSource.EXTERNAL_PUSH, 5.0);
        model.clampTotal();
        assertTrue(model.total().length() <= 0.2001,
                "total tolerance should never exceed the configured maximum");
    }

    @Test
    @DisplayName("leniency carry over is scaled and capped")
    void carryOverIsCapped() {
        ToleranceModel model = new ToleranceModel(1.0, 1.0, 1.0, 0.4);
        model.beginLeniencyCarryOver(new Vec3d(10.0, 10.0, 10.0));
        assertEquals(1.0, model.carryOver().length(), 1.0E-6);
    }

    @Test
    @DisplayName("carry over can be consumed after one prediction")
    void carryOverIsConsumable() {
        ToleranceModel model = ToleranceModel.defaults();
        model.beginLeniencyCarryOver(new Vec3d(0.1, 0.0, 0.0));
        assertTrue(model.carryOver().length() > 0.0);
        model.consumeCarryOver();
        assertEquals(Vec3d.ZERO, model.carryOver());
    }

    @Test
    @DisplayName("reset clears every source")
    void resetClearsAll() {
        ToleranceModel model = ToleranceModel.defaults();
        model.add(ToleranceSource.PISTON, 0.2);
        model.beginLeniencyCarryOver(new Vec3d(0.2, 0.0, 0.0));
        model.reset();
        assertEquals(Vec3d.ZERO, model.total());
    }

    @Test
    @DisplayName("clamping a vector to the tolerance box removes forgiven offset")
    void clampToBoxRemovesForgiven() {
        ToleranceModel model = ToleranceModel.defaults();
        model.addAxis(ToleranceSource.HIGH_PING, 0.10, 0.0, 0.0);
        Vec3d forgiven = ToleranceModel.clampToBox(new Vec3d(0.05, 0.5, -0.02), model.toleranceBox());
        assertEquals(0.05, forgiven.x(), 1.0E-6, "x is within tolerance so it is fully forgiven");
        assertEquals(0.0, forgiven.y(), 1.0E-6, "y has no tolerance so nothing is forgiven");
        assertEquals(0.0, forgiven.z(), 1.0E-6, "z has no tolerance so nothing is forgiven");
        Vec3d residual = new Vec3d(0.05, 0.5, -0.02).subtract(forgiven);
        assertEquals(0.0, residual.x(), 1.0E-6, "forgiven offsets are removed from the residual");
        assertEquals(0.5, residual.y(), 1.0E-6, "unforgiven offsets remain in the residual");
    }

    @Test
    @DisplayName("recently triggered sources are reported within a window")
    void reportsRecentTriggers() {
        ToleranceModel model = ToleranceModel.defaults();
        model.advanceTick(100L);
        model.add(ToleranceSource.BOUNCY_BLOCK, 0.1);
        assertTrue(model.recentlyTriggered(ToleranceSource.BOUNCY_BLOCK, 5L));
        model.advanceTick(200L);
        assertTrue(!model.recentlyTriggered(ToleranceSource.BOUNCY_BLOCK, 5L));
    }
}
