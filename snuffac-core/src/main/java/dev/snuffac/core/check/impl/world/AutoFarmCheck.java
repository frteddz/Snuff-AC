package dev.snuffac.core.check.impl.world;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.api.Vec3d;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.BlockBreakPacket;
import dev.snuffac.core.packet.BlockPlacePacket;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.player.EquipmentState.HeldKind;
import dev.snuffac.core.util.BlockPos;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class AutoFarmCheck implements Check {

    public static final int WINDOW = 48;
    public static final int MIN_EVENTS = 24;
    public static final double PATH_SPREAD = 1.6;
    public static final double INTERVAL_JITTER = 0.25;
    public static final int REQUIRED_WINDOWS = 2;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.BLOCK_BREAK, PacketType.BLOCK_PLACE, PacketType.MOVEMENT);
    }

    @Override
    public String key() {
        return "autofarm";
    }

    @Override
    public String name() {
        return "AutoFarm";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.WORLD;
    }

    @Override
    public String description() {
        return "Detects farming, mining and fishing performed with no variance in timing or path, "
                + "which is what an automation module produces.";
    }

    @Override
    public Object createState() {
        return new FarmState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        var state = (FarmState) state(context.player());
        if (state == null) {
            return;
        }

        var player = context.player();
        var movement = player.movement();

        if (movement.ticksSinceTeleport() <= 2 || movement.pendingSetback()) {
            state.forget();
            return;
        }

        if (packet instanceof MovementPacket movementPacket) {
            if (movementPacket.positionChanged()) {
                state.path.add(new Vec3d(
                        Math.round(movementPacket.position().x() * 4.0) / 4.0,
                        0.0,
                        Math.round(movementPacket.position().z() * 4.0) / 4.0));
                while (state.path.size() > WINDOW) {
                    state.path.removeFirst();
                }
            }
            return;
        }

        BlockPos target;
        if (packet instanceof BlockBreakPacket dig
                && dig.action() == BlockBreakPacket.BlockBreakAction.FINISH) {
            target = BlockPos.unpack(dig.packedPosition());
        } else if (packet instanceof BlockPlacePacket place) {
            target = BlockPos.unpack(place.packedPosition());
        } else {
            return;
        }

        long now = packet.arrivalNanos() / 1_000_000L;
        if (state.lastEventMillis != 0L) {
            long interval = now - state.lastEventMillis;
            if (interval >= 10L && interval <= 5_000L) {
                state.intervals.add(interval);
                while (state.intervals.size() > WINDOW) {
                    state.intervals.removeFirst();
                }
            }
        }
        state.lastEventMillis = now;
        state.targets.add(target);
        while (state.targets.size() > WINDOW) {
            state.targets.removeFirst();
        }
        state.events++;

        if (state.events < MIN_EVENTS || state.targets.size() < MIN_EVENTS
                || state.intervals.size() < MIN_EVENTS) {
            return;
        }

        if (!pathIsTight(state.path)) {
            state.reset();
            return;
        }
        double jitterRatio = intervalJitter(state.intervals);
        if (jitterRatio > INTERVAL_JITTER) {
            state.reset();
            return;
        }

        state.rigidWindows++;
        if (state.rigidWindows < REQUIRED_WINDOWS) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("events", state.events);
        evidence.put("window", state.targets.size());
        evidence.put("pathSpread", round(pathSpread(state.path)));
        evidence.put("pathSpreadLimit", PATH_SPREAD);
        evidence.put("meanIntervalMs", round(mean(state.intervals)));
        evidence.put("intervalJitter", round(jitterRatio));
        evidence.put("jitterLimit", INTERVAL_JITTER);
        evidence.put("distinctTargets", distinct(state.targets));
        evidence.put("rigidWindows", state.rigidWindows);
        evidence.put("heldItem", player.equipment().held().name());

        context.flag("worked the same spot for " + state.events + " actions with no timing or path "
                + "variance", evidence, 6.0);
        state.forget();
    }

    public static double pathSpread(java.util.ArrayDeque<Vec3d> path) {
        if (path.size() < 2) {
            return 0.0;
        }
        double sumX = 0.0;
        double sumZ = 0.0;
        for (Vec3d point : path) {
            sumX += point.x();
            sumZ += point.z();
        }
        double meanX = sumX / path.size();
        double meanZ = sumZ / path.size();
        double total = 0.0;
        for (Vec3d point : path) {
            double dx = point.x() - meanX;
            double dz = point.z() - meanZ;
            total += Math.sqrt(dx * dx + dz * dz);
        }
        return total / path.size();
    }

    public static boolean pathIsTight(java.util.ArrayDeque<Vec3d> path) {
        return path.size() >= 8 && pathSpread(path) <= PATH_SPREAD;
    }

    public static double intervalJitter(java.util.ArrayDeque<Long> intervals) {
        long mean = mean(intervals);
        if (mean <= 0L) {
            return 1.0;
        }
        long total = 0L;
        for (long value : intervals) {
            total += Math.abs(value - mean);
        }
        return (double) (total / intervals.size()) / (double) mean;
    }

    private static long distinct(ArrayDeque<BlockPos> targets) {
        Set<String> unique = new HashSet<>();
        for (BlockPos target : targets) {
            unique.add(target.toString());
        }
        return unique.size();
    }

    private static long mean(ArrayDeque<Long> values) {
        long total = 0L;
        for (long value : values) {
            total += value;
        }
        return values.isEmpty() ? 0L : total / values.size();
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    static final class FarmState {

        private final ArrayDeque<Long> intervals = new ArrayDeque<>(WINDOW);
        private final ArrayDeque<BlockPos> targets = new ArrayDeque<>(WINDOW);
        private final ArrayDeque<Vec3d> path = new ArrayDeque<>(WINDOW);
        private long lastEventMillis;
        private int events;
        private int rigidWindows;

        private void reset() {
            intervals.clear();
            targets.clear();
            path.clear();
            lastEventMillis = 0L;
            events = 0;
        }

        private void forget() {
            reset();
            rigidWindows = 0;
        }
    }
}
