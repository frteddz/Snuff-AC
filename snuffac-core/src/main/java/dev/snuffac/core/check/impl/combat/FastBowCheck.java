package dev.snuffac.core.check.impl.combat;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.ArmAnimationPacket;
import dev.snuffac.core.packet.BlockPlacePacket;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.player.EquipmentState.HeldKind;
import java.util.Map;
import java.util.Set;

public final class FastBowCheck implements Check {

    public static final long VANILLA_DRAW_MILLIS = 200L;
    public static final long MIN_SHOT_MILLIS = 40L;
    public static final int REQUIRED = 3;
    public static final int EXPIRY_MILLIS = 3_000;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.ARM_ANIMATION, PacketType.BLOCK_PLACE, PacketType.MOVEMENT);
    }

    @Override
    public String key() {
        return "fastbow";
    }

    @Override
    public String name() {
        return "FastBow";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.COMBAT;
    }

    @Override
    public String description() {
        return "Enforces the vanilla bow draw time before a shot can be released.";
    }

    @Override
    public Object createState() {
        return new BowState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        var state = (BowState) state(context.player());
        if (state == null) {
            return;
        }

        var player = context.player();
        if (player.equipment().held() != HeldKind.BOW) {
            state.reset();
            return;
        }
        if (player.movement().ticksSinceTeleport() <= 2 || player.movement().inVehicle()) {
            state.reset();
            return;
        }

        if (packet instanceof MovementPacket) {
            long now = packet.arrivalNanos() / 1_000_000L;
            if (state.drawStartMillis > 0L && now - state.drawStartMillis > EXPIRY_MILLIS) {
                state.reset();
            }
            return;
        }

        boolean release;
        if (packet instanceof ArmAnimationPacket) {
            release = true;
        } else if (packet instanceof BlockPlacePacket) {
            release = true;
        } else {
            return;
        }
        if (!release) {
            return;
        }

        long now = packet.arrivalNanos() / 1_000_000L;
        if (state.drawStartMillis == 0L) {
            state.drawStartMillis = now;
            return;
        }

        long held = now - state.drawStartMillis;
        state.shots++;
        state.shortest = Math.min(state.shortest, held);
        state.lastShotMillis = now;
        if (held >= VANILLA_DRAW_MILLIS) {
            state.reset();
            return;
        }
        if (held < MIN_SHOT_MILLIS) {
            state.drawStartMillis = now;
            return;
        }

        state.fastShots++;
        if (state.fastShots < REQUIRED) {
            state.drawStartMillis = now;
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("shortestDrawMs", state.shortest);
        evidence.put("vanillaDrawMs", VANILLA_DRAW_MILLIS);
        evidence.put("fastShots", state.fastShots);
        evidence.put("shots", state.shots);
        evidence.put("heldItem", player.equipment().held().name());
        evidence.put("attacks", player.combat().attacks());

        context.flag("released a bow after " + state.shortest + "ms, the vanilla draw is "
                + VANILLA_DRAW_MILLIS + "ms", evidence, 7.0);
        state.reset();
    }

    static final class BowState {

        private long drawStartMillis;
        private long lastShotMillis;
        private long shortest = Long.MAX_VALUE;
        private int fastShots;
        private int shots;

        private void reset() {
            drawStartMillis = 0L;
            lastShotMillis = 0L;
            shortest = Long.MAX_VALUE;
            fastShots = 0;
            shots = 0;
        }
    }
}