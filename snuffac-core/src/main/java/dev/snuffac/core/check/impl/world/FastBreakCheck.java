package dev.snuffac.core.check.impl.world;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.BlockBreakPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.player.EquipmentState;
import dev.snuffac.core.util.BlockKind;
import dev.snuffac.core.util.BlockPos;
import java.util.Map;
import java.util.Set;

public final class FastBreakCheck implements Check {

    private static final double TIMING_TOLERANCE_MILLIS = 120.0;
    private static final double MIN_FLAG_MILLIS = 30.0;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.BLOCK_BREAK);
    }

    @Override
    public String key() {
        return "fastbreak";
    }

    @Override
    public String name() {
        return "FastBreak";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.WORLD;
    }

    @Override
    public String description() {
        return "Rejects blocks broken faster than the held tool and block hardness allow.";
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!(packet instanceof BlockBreakPacket dig)) {
            return;
        }

        var player = context.player();
        var world = player.world();
        long now = System.currentTimeMillis();

        int x = BlockPos.unpackX(dig.packedPosition());
        int y = BlockPos.unpackY(dig.packedPosition());
        int z = BlockPos.unpackZ(dig.packedPosition());

        switch (dig.action()) {
            case START -> world.digStart(now, x, y, z, dig.sequence());
            case CANCEL, ABORT -> world.digStop(now);
            case FINISH -> handleFinish(context, x, y, z, now);
            default -> {
            }
        }
    }

    private static void handleFinish(CheckContext context, int x, int y, int z, long now) {
        var player = context.player();
        var world = player.world();

        if (!world.digActive()) {
            return;
        }
        if (x != world.lastDigX() || y != world.lastDigY() || z != world.lastDigZ()) {
            world.digStop(now);
            return;
        }

        long duration = now - world.digStartMillis();
        world.digStop(now);

        BlockPos position = new BlockPos(x, y, z);
        BlockKind kind = player.worldCache().kindAt(position);
        if (kind == BlockKind.BEDROCK || kind == BlockKind.BARRIER || kind == BlockKind.AIR) {
            return;
        }

        EquipmentState equipment = player.equipment();
        double hardness = player.worldCache().hardnessAt(position);
        double minimum = EquipmentState.breakMillis(hardness, equipment.miningSpeed(), equipment.blockBreakSpeed());
        if (minimum <= 0.0) {
            return;
        }
        minimum += TIMING_TOLERANCE_MILLIS;

        if (duration >= minimum) {
            return;
        }
        if (minimum - duration < MIN_FLAG_MILLIS) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("durationMs", duration);
        evidence.put("minimumMs", round(minimum));
        evidence.put("shortfall", round(minimum - duration));
        evidence.put("hardness", round(hardness));
        evidence.put("miningSpeed", round(equipment.miningSpeed()));
        evidence.put("blockBreakSpeed", round(equipment.blockBreakSpeed()));
        evidence.put("blockKind", kind.name());
        evidence.put("distance", round(player.position().distanceTo(position.toVec())));

        context.flag("broke " + kind.name().toLowerCase(java.util.Locale.ROOT) + " in "
                        + duration + "ms, minimum " + Math.round(minimum) + "ms",
                evidence, Math.min((minimum - duration) * 0.06, 10.0));
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
