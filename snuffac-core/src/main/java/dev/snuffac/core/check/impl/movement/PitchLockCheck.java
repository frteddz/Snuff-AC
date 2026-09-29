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
    public static final int MIN_HELD_TICKS = 20;
    public static final int REQUIRED = 3;
    public static final int RELEVANT_TICKS = 12;

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
        return "Detects pitch held bit constant while placing, mining, gliding or airborne, which cheat modules do to defeat server heuristics.";
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
        boolean interesting = pitchIsLocked(pitch)
                || relevantContext(movementState);

        if (!interesting) {
            state.reset();
            return;
        }
        if (!relevantContext(movementState)) {
            state.reset();
            return;
        }

        if (state.lastPitch != pitch) {
            state.lastPitch = pitch;
            state.held = 1;
            return;
        }

        state.held++;
        if (state.held < MIN_HELD_TICKS) {
            return;
        }
        state.violations++;
        if (state.violations < REQUIRED) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("pinnedPitch", state.lastPitch);
        evidence.put("heldTicks", state.held);
        evidence.put("violations", state.violations);
        evidence.put("sincePlace", movementState.ticksSincePlace());
        evidence.put("sinceBreak", movementState.ticksSinceBreak());
        evidence.put("ticksSinceGround", movementState.ticksSinceGround());
        evidence.put("gliding", movementState.gliding());
        context.flag("pitch held at exactly " + state.lastPitch
                + " degrees while placing, mining, gliding or airborne", evidence, 7.0);
        state.reset();
    }

    private static boolean pitchIsLocked(float pitch) {
        return Math.abs(pitch - DOWN_PITCH) <= 0.0001 || Math.abs(pitch - ELYTRA_PITCH) <= 0.0001;
    }

    private static boolean relevantContext(dev.snuffac.core.player.MovementState movement) {
        if (movement.ticksSincePlace() <= RELEVANT_TICKS) {
            return true;
        }
        if (movement.ticksSinceBreak() <= RELEVANT_TICKS) {
            return true;
        }
        if (movement.gliding() || movement.ticksSinceGlide() <= 4) {
            return true;
        }
        return !movement.onGround() && movement.ticksSinceGround() > 2;
    }

    static final class PitchState {

        private int held;
        private int violations;
        private float lastPitch;

        private void reset() {
            held = 0;
            violations = 0;
        }
    }
}
