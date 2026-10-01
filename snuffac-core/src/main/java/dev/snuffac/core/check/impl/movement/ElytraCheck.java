package dev.snuffac.core.check.impl.movement;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.impl.movement.AbstractMovementCheck;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.SnuffPacket;
import java.util.Map;

public final class ElytraCheck extends AbstractMovementCheck {

    public static final int MIN_GLIDE_TICKS = 12;
    public static final double MAX_UNBOOSTED = 3.25;
    public static final double BOOSTED = 8.0;
    public static final double HORIZONTAL_MARGIN = 0.35;
    public static final int SPEED_TICKS = 20;

    @Override
    public String key() {
        return "elytrafly";
    }

    @Override
    public String name() {
        return "ElytraFly";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.MOVEMENT;
    }

    @Override
    public String description() {
        return "Reports gliding with no elytra equipped, and gliding faster than a rocket can "
                + "legitimately carry the player.";
    }

    @Override
    public Object createState() {
        return new ElytraState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!isPositionUpdate(packet)) {
            return;
        }
        var state = (ElytraState) state(context.player());
        if (state == null) {
            return;
        }

        var player = context.player();
        var movement = player.movement();

        if (!dev.snuffac.core.check.CheckDispatcher.globalGate(
                player, context.tps(), context.ping())) {
            return;
        }
        if (movement.ticksSinceTeleport() <= 2 || movement.pendingSetback()) {
            state.reset();
            return;
        }

        if (!movement.gliding()) {
            state.reset();
            return;
        }

        state.glideTicks++;

        if (!player.equipment().wearingElytra()) {
            state.fakeTicks++;
            if (state.fakeTicks < MIN_GLIDE_TICKS) {
                return;
            }
            Map<String, Object> fake = context.newEvidence();
            fake.put("mode", "no elytra");
            fake.put("glideTicks", state.glideTicks);
            fake.put("fakeTicks", state.fakeTicks);
            fake.put("airTicks", movement.ticksSinceGround());
            fake.put("positionY", round(movement.position().y()));
            fake.put("velocityY", round(movement.velocity().y()));
            context.requestSetback("gliding with no elytra");
            context.flag("gliding for " + state.glideTicks + " ticks with no elytra equipped", fake, 10.0);
            state.reset();
            return;
        }
        state.fakeTicks = 0;

        double horizontal = movement.delta().horizontalLength();
        state.speedSum += horizontal;
        state.speedTicks++;
        if (state.speedTicks < SPEED_TICKS) {
            return;
        }
        state.speedAverage = state.speedSum / state.speedTicks;

        double limit = movement.boosted() ? BOOSTED : MAX_UNBOOSTED;
        if (state.speedAverage < limit + HORIZONTAL_MARGIN) {
            state.speedTicks = 0;
            state.speedSum = 0.0;
            return;
        }

        Map<String, Object> fast = context.newEvidence();
        fast.put("mode", "too fast");
        fast.put("averageSpeed", round(state.speedAverage));
        fast.put("limit", limit);
        fast.put("boosted", movement.boosted());
        fast.put("wearingElytra", true);
        fast.put("glideTicks", state.glideTicks);
        fast.put("airTicks", movement.ticksSinceGround());

        context.requestSetback("gliding faster than " + limit + " blocks per tick");
        context.flag("gliding at " + round(state.speedAverage) + " blocks per tick, "
                + "the limit with no rocket is " + limit, fast, 9.0);
        state.reset();
    }

    @Override
    public void onTick(CheckContext context) {
        var state = (ElytraState) state(context.player());
        if (state != null && !context.player().movement().gliding()) {
            state.reset();
        }
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    static final class ElytraState {

        private int glideTicks;
        private int fakeTicks;
        private int speedTicks;
        private double speedSum;
        private double speedAverage;

        private void reset() {
            glideTicks = 0;
            fakeTicks = 0;
            speedTicks = 0;
            speedSum = 0.0;
            speedAverage = 0.0;
        }
    }
}
