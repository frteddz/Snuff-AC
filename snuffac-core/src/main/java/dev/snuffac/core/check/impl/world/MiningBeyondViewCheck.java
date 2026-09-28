package dev.snuffac.core.check.impl.world;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.api.Vec3d;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.mining.MiningEvidence;
import dev.snuffac.core.packet.BlockBreakPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.util.BlockPos;
import java.util.Map;
import java.util.Set;

public final class MiningBeyondViewCheck implements Check {

    public static final int REQUIRED_OUTSIDE = 3;
    public static final int ORE_TIER_THRESHOLD = 2;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.BLOCK_BREAK);
    }

    @Override
    public String key() {
        return "miningbeyondview";
    }

    @Override
    public String name() {
        return "MiningBeyondView";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.WORLD;
    }

    @Override
    public String description() {
        return "Detects targeting valuable ores the server never sent to the client, which implies knowledge the client could not legitimately have.";
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!(packet instanceof BlockBreakPacket dig)
                || dig.action() != BlockBreakPacket.BlockBreakAction.START) {
            return;
        }
        var state = (ViewState) state(context.player());
        if (state == null) {
            return;
        }
        var player = context.player();
        var environment = player.combatEnvironment();
        if (environment.count() == 0 && environment.sentChunkRadius() == 0) {
            return;
        }

        BlockPos position = BlockPos.unpack(dig.packedPosition());
        Vec3d playerPosition = player.position();
        int distance = (int) Math.round(Math.sqrt(
                Math.pow(position.x() + 0.5 - playerPosition.x(), 2)
                        + Math.pow(position.y() + 0.5 - playerPosition.y(), 2)
                        + Math.pow(position.z() + 0.5 - playerPosition.z(), 2)));

        int chunkDistance = Math.max(
                Math.abs(position.x() >> 4), Math.abs(position.z() >> 4));
        int playerChunkDistance = Math.max(
                Math.abs(MathUtilFloor(playerPosition.x()) >> 4),
                Math.abs(MathUtilFloor(playerPosition.z()) >> 4));
        int relativeChunkDistance = Math.abs(chunkDistance - playerChunkDistance);
        boolean withinRadius = relativeChunkDistance <= environment.sentChunkRadius();

        var cache = player.worldCache();
        String material = cache.materialAt(position);
        int tier = cache.oreTierAt(position);
        var evidence = context.newEvidence();
        long previousMillis = state.lastTargetMillis;
        long now = System.currentTimeMillis();
        double seconds = previousMillis == 0L ? 0.0 : (now - previousMillis) / 1000.0;

        player.mining().record(new MiningEvidence(
                0,
                position,
                now,
                withinRadius,
                distance,
                tier,
                material,
                seconds));

        if (withinRadius || tier < ORE_TIER_THRESHOLD) {
            return;
        }
        state.lastTargetMillis = now;

        long outside = player.mining().outsideSentChunks();
        if (outside < REQUIRED_OUTSIDE) {
            return;
        }

        evidence.put("outsideSentChunks", outside);
        evidence.put("valuableOutside", player.mining().valuableOutsideSentChunks());
        evidence.put("sentChunkRadius", environment.sentChunkRadius());
        evidence.put("blockX", position.x());
        evidence.put("blockY", position.y());
        evidence.put("blockZ", position.z());
        evidence.put("distance", distance);
        context.flag("targeted valuable ore outside the region sent to the client", evidence, 7.0);
        player.mining().clear();
    }

    private static int MathUtilFloor(double value) {
        return (int) Math.floor(value);
    }

    @Override
    public Object createState() {
        return new ViewState();
    }

    static final class ViewState {

        private long lastTargetMillis;
    }
}
