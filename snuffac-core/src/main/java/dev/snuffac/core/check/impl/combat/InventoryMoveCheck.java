package dev.snuffac.core.check.impl.combat;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.packet.WindowClickPacket;
import java.util.Map;
import java.util.Set;

public final class InventoryMoveCheck implements Check {

    public static final int OPEN_TICKS = 60;
    public static final double FULL_SPEED = 0.2;
    public static final double MIN_HORIZONTAL = 0.05;
    public static final int REQUIRED_TICKS = 8;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.INVENTORY_CLICK, PacketType.MOVEMENT);
    }

    @Override
    public String key() {
        return "inventorymove";
    }

    @Override
    public String name() {
        return "InventoryMove";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.MOVEMENT;
    }

    @Override
    public String description() {
        return "Reports movement at full speed while an inventory is open, which the vanilla "
                + "client does not allow.";
    }

    @Override
    public Object createState() {
        return new InventoryState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        var state = (InventoryState) state(context.player());
        if (state == null) {
            return;
        }

        if (packet instanceof WindowClickPacket click) {
            if (click.windowId() == 0) {
                return;
            }
            state.openTicks = OPEN_TICKS;
            state.lastWindow = click.windowId();
            state.clicks++;
            return;
        }
        if (!(packet instanceof MovementPacket movement) || !movement.positionChanged()) {
            return;
        }

        if (state.openTicks > 0) {
            state.openTicks--;
        }
        if (state.openTicks <= 0) {
            state.reset();
            return;
        }

        var movementState = context.player().movement();
        if (movementState.ticksSinceTeleport() <= 2 || movementState.inVehicle()
                || movementState.inWaterOrLava() || movementState.onClimbable()
                || movementState.gliding() || movementState.flying()) {
            state.reset();
            return;
        }
        if (movementState.delta().horizontalLength() < MIN_HORIZONTAL) {
            state.cleanTicks++;
            if (state.cleanTicks >= REQUIRED_TICKS) {
                state.movingTicks = 0;
                state.speedSum = 0.0;
            }
            return;
        }

        state.cleanTicks = 0;
        state.movingTicks++;
        state.speedSum += movementState.delta().horizontalLength();
        if (state.movingTicks < REQUIRED_TICKS) {
            return;
        }

        double average = state.speedSum / state.movingTicks;
        if (average < FULL_SPEED) {
            state.movingTicks = 0;
            state.speedSum = 0.0;
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("window", state.lastWindow);
        evidence.put("clicks", state.clicks);
        evidence.put("averageSpeed", round(average));
        evidence.put("fullSpeed", FULL_SPEED);
        evidence.put("ticksMoving", state.movingTicks);
        evidence.put("ticksSinceOpen", OPEN_TICKS - state.openTicks);
        evidence.put("onGround", movementState.onGround());
        evidence.put("sprinting", movementState.sprinting());
        evidence.put("airTicks", movementState.ticksSinceGround());

        context.flag("moved at " + round(average) + " blocks per tick while an inventory was open, "
                + "vanilla does not allow movement at all", evidence, 8.0);
        state.reset();
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    static final class InventoryState {

        private int openTicks;
        private int lastWindow;
        private int clicks;
        private int movingTicks;
        private int cleanTicks;
        private double speedSum;

        private void reset() {
            openTicks = 0;
            clicks = 0;
            movingTicks = 0;
            cleanTicks = 0;
            speedSum = 0.0;
        }
    }
}
