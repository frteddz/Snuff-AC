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
    public static final long VANILLA_COOLDOWN_MILLIS = 50L;
    private static final long VANILLA_COOLDOWN_NANOS = 50_000_000L;
    public static final double COOLDOWN_TOLERANCE = 0.35;
    public static final int REQUIRED_FAST = 4;
    private static final long COOLDOWN_WINDOW_NANOS = 3_000_000_000L;

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
        return "Rejects block placement rates above the maximum vanilla client can emit, and "
                + "placement cadence that ignores the vanilla four tick cooldown.";
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

        accountCooldown(context, packet.arrivalNanos());

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

    private void accountCooldown(CheckContext context, long nowNanos) {
        var state = (PlaceState) state(context.player());
        if (state == null) {
            return;
        }
        if (state.lastPlaceNanos != 0L) {
            long gap = nowNanos - state.lastPlaceNanos;
            if (gap >= 0L && gap <= COOLDOWN_WINDOW_NANOS) {
                state.gaps.add(gap);
                while (state.gaps.size() > 16) {
                    state.gaps.remove(0);
                }
            }
        }
        state.lastPlaceNanos = nowNanos;

        java.util.List<Long> gaps = state.gaps;
        if (gaps.size() < REQUIRED_FAST) {
            return;
        }
        int fast = 0;
        for (long gap : gaps) {
            if (gap < VANILLA_COOLDOWN_NANOS * (1.0 - COOLDOWN_TOLERANCE)) {
                fast++;
            }
        }
        if (fast < REQUIRED_FAST) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("mode", "cooldown");
        evidence.put("fastPlacements", fast);
        evidence.put("required", REQUIRED_FAST);
        evidence.put("vanillaCooldownMs", VANILLA_COOLDOWN_MILLIS);
        evidence.put("gaps", gaps);
        evidence.put("sample", gaps.get(gaps.size() - 1));
        evidence.put("onGround", context.player().movement().onGround());
        evidence.put("sneaking", context.player().movement().sneaking());

        context.preventPlacement("placed faster than the vanilla cooldown allows");
        context.flag("placed " + fast + " times faster than the " + VANILLA_COOLDOWN_MILLIS
                + "ms cooldown allows", evidence, 6.0);
        gaps.clear();
    }

    @Override
    public Object createState() {
        return new PlaceState();
    }

    static final class PlaceState {

        private final java.util.ArrayList<Long> gaps = new java.util.ArrayList<>(16);
        private long lastPlaceNanos;
    }

    static BlockPos positionOf(BlockPlacePacket packet) {
        return new BlockPos(
                BlockPos.unpackX(packet.packedPosition()),
                BlockPos.unpackY(packet.packedPosition()),
                BlockPos.unpackZ(packet.packedPosition()));
    }
}
