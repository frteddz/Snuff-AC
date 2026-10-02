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

public final class ScaffoldCheck implements Check {

    private static final int REQUIRED_CONSECUTIVE = 4;
    private static final double MAX_REACH = 6.0;
    private static final double ABOVE_EYES = -1.0;
    public static final double FACING_LIMIT = 62.0;
    private static final int FACING_LIMIT_RUN = 3;
    public static final int PRINTER_RUN = 4;
    public static final double PRINTER_INTERVAL = 0.9;
    public static final double PRINTER_MAX_INTERVAL = 3.0;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.BLOCK_PLACE);
    }

    @Override
    public String key() {
        return "scaffold";
    }

    @Override
    public String name() {
        return "Scaffold";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.WORLD;
    }

    @Override
    public String description() {
        return "Detects the tower scaffold pattern of repeatedly placing blocks under the player "
                + "while airborne, placements the player is not facing, placements with nothing "
                + "in hand to place, and the metronome spacing of an automated printer.";
    }

    @Override
    public Object createState() {
        return new ScaffoldState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!(packet instanceof BlockPlacePacket place)) {
            return;
        }
        var state = (ScaffoldState) state(context.player());
        if (state == null) {
            return;
        }

        var player = context.player();
        var movement = player.movement();

        if (!player.equipment().holdingPlaceable()) {
            state.placesWithoutItem++;
            if (state.placesWithoutItem >= FACING_LIMIT_RUN) {
                reportWithoutItem(context, state, place);
            }
            return;
        }
        state.placesWithoutItem = 0;

        if (movement.onGround() || movement.sneaking() || !movement.sprinting()) {
            state.consecutive = 0;
            return;
        }
        if (movement.ticksSinceGround() > 30) {
            state.consecutive = 0;
            return;
        }

        BlockPos position = FastPlaceCheck.positionOf(place);
        double eyeY = player.position().y() + 1.62;
        if (position.y() >= eyeY + ABOVE_EYES) {
            state.consecutive = 0;
            return;
        }
        if (position.y() < player.position().y() - 4.0) {
            state.consecutive = 0;
            return;
        }
        if (player.position().distanceTo(position.toVec()) > MAX_REACH) {
            state.consecutive = 0;
            return;
        }

        if (accountPrinter(context, state, position)) {
            return;
        }

        double facing = facingAngle(context, position, place.faceId());
        if (facing > FACING_LIMIT) {
            state.blindPlacements++;
            if (state.blindPlacements >= FACING_LIMIT_RUN) {
                reportNotFacing(context, state, position, place.faceId(), facing);
            }
            return;
        }
        state.blindPlacements = 0;

        state.consecutive++;
        if (state.consecutive < REQUIRED_CONSECUTIVE) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("consecutive", state.consecutive);
        evidence.put("airTicks", movement.ticksSinceGround());
        evidence.put("blockY", position.y());
        evidence.put("playerY", round(player.position().y()));
        evidence.put("sprinting", movement.sprinting());
        evidence.put("onGround", false);
        context.flag("scaffold tower of " + state.consecutive + " blocks while airborne", evidence, 5.0);
        state.consecutive = 0;
    }

    private boolean accountPrinter(CheckContext context, ScaffoldState state, BlockPos position) {
        long now = System.currentTimeMillis();
        if (state.lastPlaceMillis == 0L) {
            state.lastPlaceMillis = now;
            state.lastPlaceY = position.y();
            return false;
        }
        long interval = now - state.lastPlaceMillis;
        int height = position.y() - state.lastPlaceY;
        state.lastPlaceMillis = now;
        state.lastPlaceY = position.y();

        if (height != 1 || interval < PRINTER_INTERVAL * 1000L
                || interval > PRINTER_MAX_INTERVAL * 1000L) {
            state.printerRun = 0;
            return false;
        }

        state.printerRun++;
        if (state.printerRun < PRINTER_RUN) {
            return false;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("mode", "printer");
        evidence.put("consecutive", state.printerRun);
        evidence.put("intervalMillis", interval);
        evidence.put("intervalFloorMillis", (long) (PRINTER_INTERVAL * 1000.0));
        evidence.put("block", position.toString());
        evidence.put("heightGain", height);
        evidence.put("sprinting", context.player().movement().sprinting());
        evidence.put("heldItemSlot", context.player().equipment().heldItemSlot());

        context.preventPlacement("placement spacing is mechanical");
        context.flag("placed a block every " + interval + "ms while rising, which is a printer",
                evidence, 6.0);
        state.printerRun = 0;
        return true;
    }

    public static double facingAngle(CheckContext context, BlockPos position, int faceId) {
        var eye = new dev.snuffac.api.Vec3d(
                context.player().position().x(),
                context.player().position().y() + 1.62,
                context.player().position().z());
        var face = dev.snuffac.core.util.BlockFace.byId(faceId);
        var target = new dev.snuffac.api.Vec3d(
                position.x() + 0.5 + face.stepX() * 0.5,
                position.y() + 0.5 + face.stepY() * 0.5,
                position.z() + 0.5 + face.stepZ() * 0.5);
        double dx = target.x() - eye.x();
        double dy = target.y() - eye.y();
        double dz = target.z() - eye.z();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 1.0E-6) {
            return Math.abs(dy) < 1.0E-6 ? 0.0 : 90.0;
        }
        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double targetPitch = Math.toDegrees(-Math.atan2(dy, horizontal));
        double yawError = angleBetween(targetYaw, context.player().movement().yaw());
        double pitchError = angleBetween(targetPitch, context.player().movement().pitch());
        return Math.max(yawError, pitchError);
    }

    private static double angleBetween(double a, double b) {
        double difference = Math.abs(a - b) % 360.0;
        return difference > 180.0 ? 360.0 - difference : difference;
    }

    private void reportNotFacing(
            CheckContext context, ScaffoldState state, BlockPos position, int faceId, double facing) {
        Map<String, Object> evidence = context.newEvidence();
        evidence.put("mode", "not facing");
        evidence.put("facingAngle", round(facing));
        evidence.put("limit", FACING_LIMIT);
        evidence.put("consecutive", state.blindPlacements);
        evidence.put("block", position.toString());
        evidence.put("face", dev.snuffac.core.util.BlockFace.byId(faceId).name());
        evidence.put("yaw", round(context.player().movement().yaw()));
        evidence.put("pitch", round(context.player().movement().pitch()));
        context.preventPlacement("placed " + position + " while facing " + round(facing) + " degrees away");
        context.flag("placed a block " + round(facing) + " degrees away from where the player is looking",
                evidence, 7.0);
        state.blindPlacements = 0;
    }

    private void reportWithoutItem(CheckContext context, ScaffoldState state, BlockPlacePacket place) {
        Map<String, Object> evidence = context.newEvidence();
        evidence.put("mode", "empty hand");
        evidence.put("consecutive", state.placesWithoutItem);
        evidence.put("hand", place.handOrdinal());
        evidence.put("block", FastPlaceCheck.positionOf(place).toString());
        evidence.put("heldItemSlot", context.player().equipment().heldItemSlot());
        context.preventPlacement("placed with nothing in hand");
        context.flag("placed a block with nothing in hand to place", evidence, 8.0);
        state.placesWithoutItem = 0;
    }

    @Override
    public void onTick(CheckContext context) {
        var state = (ScaffoldState) state(context.player());
        if (state != null && context.player().movement().onGround()) {
            state.consecutive = 0;
            state.blindPlacements = 0;
        }
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    static final class ScaffoldState {

        private int consecutive;
        private int blindPlacements;
        private int placesWithoutItem;
        private int printerRun;
        private long lastPlaceMillis;
        private int lastPlaceY;
    }
}
