package dev.snuffac.core.check.impl.movement;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import java.util.Map;
import java.util.Set;

public final class PitchLockCheck implements Check {

    public static final double DOWN_PITCH = 90.0;
    public static final double ELYTRA_PITCH = 40.0;
    public static final double TOLERANCE = 0.01;
    public static final int MIN_HELD_TICKS = 6;
    public static final int REQUIRED = 4;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.MOVEMENT);
    }

    @Override
    public String key() {
        return "pitchlock";
    }

    @Override
    public String name() {
        return "PitchLock";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.MOVEMENT;
    }

    @Override
    public String description() {
        return "Detects pitch pinned to an exact constant, which cheat modules use to defeat server heuristics while placing or gliding.";
    }

    @Override
    public Object createState() {
        return new PitchState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!(packet instanceof MovementPacket movement) || !movement.rotationChanged()) {
            return;
        }
        var state = (PitchState) state(context.player());
        if (state == null) {
            return;
        }
        var movementState = context.player().movement();
        if (movementState.ticksSinceTeleport() <= 2 || movementState.riding() || movementState.inVehicle()) {
            state.reset();
            return;
        }

        float pitch = Math.abs(movement.pitch());
        boolean down = Math.abs(pitch - DOWN_PITCH) <= TOLERANCE;
        boolean elytra = Math.abs(pitch - ELYTRA_PITCH) <= TOLERANCE;

        if (!down && !elytra) {
            state.reset();
            return;
        }

        if (down && movementState.flying() && movementState.onGround()) {
            state.reset();
            return;
        }
        if (elytra && !movementState.gliding() && movementState.ticksSinceGlide() > 20) {
            state.reset();
            return;
        }

        state.held++;
        state.lastPitch = pitch;
        if (state.held < MIN_HELD_TICKS) {
            return;
        }
        state.violations++;
        if (state.violations < REQUIRED) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("pinnedPitch", state.lastPitch);
        evidence.put("mode", down ? "straight down" : "fixed elytra angle");
        evidence.put("heldTicks", state.held);
        evidence.put("violations", state.violations);
        context.flag("pitch pinned to exactly " + state.lastPitch + " degrees", evidence, 7.0);
        state.reset();
    }

    @Override
    public void onTick(CheckContext context) {
        var state = (PitchState) state(context.player());
        if (state != null && state.held > 0) {
            state.held--;
        }
    }

    static final class PitchState {

        private int held;
        private int violations;
        private double lastPitch;

        private void reset() {
            held = 0;
            violations = 0;
        }
    }
}
