package dev.snuffac.core.check.impl.movement;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.api.Vec3d;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.SnuffPacket;
import java.util.Map;

public final class NoSlowCheck extends AbstractMovementCheck {

    private static final int REQUIRED_TICKS = 6;
    public static final double EXPECTED_SLOWDOWN = 0.2;
    public static final double MARGIN_ALLOWED = 1.15;

    @Override
    public String key() {
        return "noslow";
    }

    @Override
    public String name() {
        return "NoSlow";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.MOVEMENT;
    }

    @Override
    public String description() {
        return "Enforces the vanilla movement slowdown while eating, drinking, drawing a bow, "
                + "blocking with a shield, sneaking or standing on soul sand.";
    }

    @Override
    public Object createState() {
        return new NoSlowState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!isPositionUpdate(packet)) {
            return;
        }
        var state = (NoSlowState) state(context.player());
        if (state == null) {
            return;
        }

        var player = context.player();
        var movement = player.movement();

        if (!dev.snuffac.core.check.CheckDispatcher.globalGate(
                player, context.tps(), context.ping())) {
            return;
        }
        if (dev.snuffac.core.prediction.PredictionGraces.windChargeActive(movement)) {
            return;
        }
        if (!canPredict(player) || movement.ticksSinceTeleport() <= 2
                || movement.pendingSetback()) {
            state.reset();
            return;
        }

        var environment = environment(context);
        if (!measurable(environment)) {
            state.reset();
            return;
        }

        double actualHorizontal = movement.delta().horizontalLength();
        if (actualHorizontal < NoSlowState.MIN_HORIZONTAL) {
            state.reset();
            return;
        }


        if (!environment.usingItem()) {
            state.freeTicks++;
            state.freeSum += actualHorizontal;
            if (state.freeTicks >= REQUIRED_TICKS) {
                state.freeAverage = state.freeSum / state.freeTicks;
            }
        }

        if (state.freeAverage < NoSlowState.MIN_HORIZONTAL) {
            state.reset();
            return;
        }

        if (!applicable(environment)) {
            state.reset();
            return;
        }

        double expectedRatio = EXPECTED_SLOWDOWN;
        double ratio = actualHorizontal / state.freeAverage;
        if (ratio <= expectedRatio * MARGIN_ALLOWED) {
            state.cleanTicks++;
            if (state.cleanTicks >= REQUIRED_TICKS) {
                state.overTicks = 0;
            }
            return;
        }

        state.cleanTicks = 0;
        state.overTicks++;
        if (state.overTicks < REQUIRED_TICKS) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("expectedSlowdown", EXPECTED_SLOWDOWN);
        evidence.put("freeAverage", round(state.freeAverage));
        evidence.put("expectedRatio", round(expectedRatio));
        evidence.put("observedRatio", round(ratio));
        evidence.put("ratioLimit", round(expectedRatio * MARGIN_ALLOWED));
        evidence.put("actualHorizontal", round(actualHorizontal));
        evidence.put("freeSamples", state.freeTicks);
        evidence.put("ticksUsingItem", movement.ticksUsingItem());
        evidence.put("actualX", round(movement.delta().x()));
        evidence.put("actualZ", round(movement.delta().z()));
        evidence.put("sneaking", movement.sneaking());
        evidence.put("onSoulSand", movement.onSoulSand());
        evidence.put("sprinting", movement.sprinting());
        evidence.put("onGround", movement.onGround());

        context.requestSetback("item use slowdown not applied");
        context.flag("moved " + round(ratio) + " of the player's own free walking speed while using "
                + "an item, the observed slowdown allows " + round(expectedRatio), evidence, 8.0);
        state.forget();
    }

    public static boolean measurable(dev.snuffac.core.physics.MovementEnvironment environment) {
        if (environment.inWaterOrLava() || environment.gliding() || environment.flying()
                || environment.riding() || environment.onClimbable()
                || environment.riptiding() || environment.levitating()
                || environment.slowFalling() || environment.submerged()) {
            return false;
        }
        return environment.onGroundFromClient();
    }

    public static boolean applicable(dev.snuffac.core.physics.MovementEnvironment environment) {
        if (!environment.usingItem() && !environment.onSoulSand()) {
            return false;
        }
        return measurable(environment);
    }

    @Override
    public void onTick(CheckContext context) {
        var state = (NoSlowState) state(context.player());
        if (state != null && !context.player().movement().usingItem()) {
            state.reset();
        }
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    static final class NoSlowState {

        public static final double MIN_HORIZONTAL = 0.02;

        private int overTicks;
        private int cleanTicks;
        private int freeTicks;
        private double freeSum;
        private double freeAverage;

        private void reset() {
            overTicks = 0;
            cleanTicks = 0;
        }

        private void forget() {
            reset();
            freeTicks = 0;
            freeSum = 0.0;
            freeAverage = 0.0;
        }
    }
}
