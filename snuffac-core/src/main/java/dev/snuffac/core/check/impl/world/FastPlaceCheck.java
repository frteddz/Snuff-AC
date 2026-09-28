package dev.snuffac.core.check.impl.world;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.BlockPlacePacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.util.BlockPos;
import java.util.Map;
import java.util.Set;

public final class FastPlaceCheck implements Check {

    private static final int MAX_PLACES_PER_SECOND = 22;
    private static final long WINDOW_MILLIS = 1000L;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.BLOCK_PLACE);
    }

    @Override
    public String key() {
        return "fastplace";
    }

    @Override
    public String name() {
        return "FastPlace";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.WORLD;
    }

    @Override
    public String description() {
        return "Rejects block placement rates above the maximum vanilla client can emit.";
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!(packet instanceof BlockPlacePacket place)) {
            return;
        }

        long now = System.currentTimeMillis();
        var world = context.player().world();
        world.place(now,
                BlockPos.unpackX(place.packedPosition()),
                BlockPos.unpackY(place.packedPosition()),
                BlockPos.unpackZ(place.packedPosition()));

        int inWindow = world.placesInWindow(now, WINDOW_MILLIS);
        if (inWindow <= MAX_PLACES_PER_SECOND) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("placesPerSecond", inWindow);
        evidence.put("maximum", MAX_PLACES_PER_SECOND);
        evidence.put("holdingPlaceable", context.player().equipment().holdingPlaceable());
        evidence.put("onGround", context.player().movement().onGround());
        evidence.put("sneaking", context.player().movement().sneaking());
        context.flag("placed " + inWindow + " blocks in one second", evidence, 6.0);
    }

    static BlockPos positionOf(BlockPlacePacket packet) {
        return new BlockPos(
                BlockPos.unpackX(packet.packedPosition()),
                BlockPos.unpackY(packet.packedPosition()),
                BlockPos.unpackZ(packet.packedPosition()));
    }
}
