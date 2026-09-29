package dev.snuffac.core.check.impl.packet;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import java.util.Map;
import java.util.Set;

public final class ExtraPacketsCheck implements Check {

    public static final int SERVER_TICKS_PER_SECOND = 20;
    public static final int BURST_ALLOWANCE = 4;
    public static final int REPEAT_TICKS_REQUIRED = 6;
    public static final int REQUIRED_BURSTS = 3;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.MOVEMENT);
    }

    @Override
    public String key() {
        return "extrapackets";
    }

    @Override
    public String name() {
        return "ExtraPackets";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.PACKET;
    }

    @Override
    public String description() {
        return "Detects more than one position packet per server tick, the universal signature of packet replay cheats.";
    }

    @Override
    public Object createState() {
        return new BurstState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!(packet instanceof MovementPacket movement) || !movement.positionChanged()) {
            return;
        }
        var state = (BurstState) state(context.player());
        if (state == null) {
            return;
        }
        long tick = context.player().network().tickCounter();
        if (state.lastTick != tick) {
            state.lastTick = tick;
            state.packetsThisTick = 1;
            return;
        }
        state.packetsThisTick++;
        if (state.packetsThisTick <= BURST_ALLOWANCE) {
            state.overBudgetTicks = 0;
            return;
        }
        state.overBudgetTicks++;
        if (state.overBudgetTicks < REPEAT_TICKS_REQUIRED) {
            return;
        }

        state.bursts++;
        if (state.bursts < REQUIRED_BURSTS) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("packetsInTick", state.packetsThisTick);
        evidence.put("allowed", BURST_ALLOWANCE);
        evidence.put("consecutiveTicks", state.overBudgetTicks);
        evidence.put("bursts", state.bursts);
        evidence.put("position", context.position().x() + "," + context.position().y() + "," + context.position().z());
        context.flag("multiple position packets in one server tick", evidence, 9.0);
        state.bursts = 0;
    }

    @Override
    public void onTick(CheckContext context) {
        var state = (BurstState) state(context.player());
        if (state != null) {
            state.packetsThisTick = 0;
            if (state.bursts > 0) {
                state.bursts--;
            }
        }
    }

    static final class BurstState {

        private long lastTick = -1L;
        private int packetsThisTick;
        private int overBudgetTicks;
        private int bursts;
    }
}
