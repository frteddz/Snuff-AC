package dev.snuffac.core.check.impl.movement;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.SnuffPacket;
import java.util.Map;

public final class GroundSpoofCheck extends AbstractMovementCheck {

    private static final int MIN_AIR_TICKS = 2;
    private static final double MAX_GROUND_MOVE = 0.65;

    @Override
    public String key() {
        return "groundspoof";
    }

    @Override
    public String name() {
        return "GroundSpoof";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.MOVEMENT;
    }

    @Override
    public String description() {
        return "Detects claiming ground contact while the server block view shows no supporting block.";
    }

    @Override
    public Object createState() {
        return new SpoofState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!isPositionUpdate(packet)) {
            return;
        }
        var state = (SpoofState) state(context.player());
        if (state == null) {
            return;
        }
        if (!dev.snuffac.core.check.CheckDispatcher.globalGate(
                context.player(), context.tps(), context.ping())) {
            return;
        }

        var player = context.player();
        var movement = player.movement();
        var cache = player.worldCache();

        if (!movement.onGround() || !cache.chunkLoaded()) {
            state.groundClaims = 0;
            return;
        }
        if (movement.ticksSinceTeleport() <= 1 || movement.flying() || movement.gliding()
                || movement.inVehicle() || movement.inWaterOrLava() || movement.onClimbable()) {
            state.groundClaims = 0;
            return;
        }
        if (movement.ticksSinceGround() < MIN_AIR_TICKS && cache.onGroundBelow()) {
            state.groundClaims = 0;
            return;
        }

        double horizontal = movement.delta().horizontalLength();
        if (cache.onGroundBelow() || horizontal <= MAX_GROUND_MOVE) {
            state.groundClaims = 0;
            return;
        }

        state.groundClaims++;
        if (state.groundClaims < 3) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("groundClaims", state.groundClaims);
        evidence.put("airTicks", movement.ticksSinceGround());
        evidence.put("horizontal", round(horizontal));
        evidence.put("groundBelow", false);
        evidence.put("fallDistance", round(movement.fallDistance()));
        context.flag("ground spoof over " + state.groundClaims + " packets", evidence, 8.0);
        state.groundClaims = 0;
    }

    @Override
    public void onTick(CheckContext context) {
        var state = (SpoofState) state(context.player());
        if (state != null && !context.player().movement().onGround()) {
            state.groundClaims = 0;
        }
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    static final class SpoofState {

        private int groundClaims;
    }
}
