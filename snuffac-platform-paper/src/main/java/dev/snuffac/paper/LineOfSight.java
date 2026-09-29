package dev.snuffac.paper;

import dev.snuffac.core.combat.EntitySnapshot;
import dev.snuffac.core.combat.HitboxVerifier;
import dev.snuffac.core.combat.ReachResolver;
import dev.snuffac.core.util.AxisAlignedBox;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class LineOfSight {

    private static final double MAX_RAY = 64.0;

    private LineOfSight() {
    }

    public static Vector eye(Player viewer) {
        return viewer.getEyeLocation().toVector();
    }

    public static Vector bodyCenter(Entity target) {
        BoundingBox box = target.getBoundingBox();
        return new Vector(box.getCenterX(), box.getCenterY(), box.getCenterZ());
    }

    public static double distance(Player viewer, Entity target) {
        return eye(viewer).distance(bodyCenter(target));
    }

    public static AxisAlignedBox box(Entity target) {
        BoundingBox box = target.getBoundingBox();
        return new AxisAlignedBox(box.getMinX(), box.getMinY(), box.getMinZ(),
                box.getMaxX(), box.getMaxY(), box.getMaxZ());
    }

    public static boolean clearAt(Player viewer, Location point) {
        if (!viewer.getWorld().isChunkLoaded(point.getBlockX() >> 4, point.getBlockZ() >> 4)) {
            return true;
        }
        Location origin = viewer.getEyeLocation();
        Vector aim = point.toVector().subtract(origin.toVector());
        double length = aim.length();
        if (length < 1.0E-6) {
            return true;
        }
        RayTraceResult hit = viewer.getWorld().rayTraceBlocks(
                origin,
                aim.multiply(1.0 / length),
                Math.min(length, MAX_RAY),
                FluidCollisionMode.NEVER,
                true);
        return hit == null;
    }

    public static boolean clear(Player viewer, Entity target) {
        if (viewer.getWorld() != target.getWorld()) {
            return false;
        }
        Location origin = viewer.getEyeLocation();
        Vector aim = bodyCenter(target).subtract(origin.toVector());
        double length = aim.length();
        if (length < 1.0E-6) {
            return true;
        }
        RayTraceResult hit = viewer.getWorld().rayTraceBlocks(
                origin,
                aim.multiply(1.0 / length),
                Math.min(length + ReachResolver.verticalPadding(), MAX_RAY),
                FluidCollisionMode.NEVER,
                true);
        return hit == null;
    }

    public static boolean rayTouches(Player viewer, Entity target) {
        Vector origin = eye(viewer);
        Vector look = viewer.getEyeLocation().getDirection();
        double reach = Math.min(distance(viewer, target) + ReachResolver.verticalPadding(), MAX_RAY);
        AxisAlignedBox ray = new AxisAlignedBox(
                Math.min(origin.getX(), origin.getX() + look.getX() * reach),
                Math.min(origin.getY(), origin.getY() + look.getY() * reach),
                Math.min(origin.getZ(), origin.getZ() + look.getZ() * reach),
                Math.max(origin.getX(), origin.getX() + look.getX() * reach),
                Math.max(origin.getY(), origin.getY() + look.getY() * reach),
                Math.max(origin.getZ(), origin.getZ() + look.getZ() * reach));
        return box(target).expanded(ReachResolver.verticalPadding()).intersects(ray);
    }

    public static double rayAngleDegrees(Player viewer, Entity target) {
        Vector look = viewer.getEyeLocation().getDirection();
        Vector to = bodyCenter(target).subtract(eye(viewer));
        double length = to.length();
        if (length < 1.0E-6) {
            return 0.0;
        }
        double dot = Math.max(-1.0, Math.min(1.0, look.dot(to.multiply(1.0 / length))));
        return Math.toDegrees(Math.acos(dot));
    }

    public static java.util.Map<Integer, Entity> index(org.bukkit.World world) {
        java.util.Map<Integer, Entity> map = new java.util.HashMap<>();
        for (Entity entity : world.getEntities()) {
            map.putIfAbsent(entity.getEntityId(), entity);
        }
        return map;
    }

    public static Entity live(EntitySnapshot snapshot, java.util.Map<Integer, Entity> index) {
        if (snapshot == null) {
            return null;
        }
        return index.get(snapshot.entityId());
    }

    public static boolean visuallyReachable(
            Player viewer,
            EntitySnapshot snapshot,
            java.util.Map<Integer, Entity> index,
            double proximity,
            double padding) {
        Entity target = live(snapshot, index);
        if (target == null || target.getWorld() != viewer.getWorld()) {
            return true;
        }
        double distance = distance(viewer, target);
        if (distance <= proximity) {
            return true;
        }
        return distance <= padding && clear(viewer, target);
    }

    public static double eyeHeight(Entity target) {
        return target instanceof LivingEntity living ? living.getEyeHeight() : 0.0;
    }
}
