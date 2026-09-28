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
        return "Detects the tower scaffold pattern of repeatedly placing blocks under the player while airborne.";
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

        if (movement.onGround() || movement.sneaking() || !movement.sprinting()) {
            state.consecutive = 0;
            return;
        }
        if (!player.equipment().holdingPlaceable()) {
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

    @Override
    public void onTick(CheckContext context) {
        var state = (ScaffoldState) state(context.player());
        if (state != null && context.player().movement().onGround()) {
            state.consecutive = 0;
        }
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    static final class ScaffoldState {

        private int consecutive;
    }
}
