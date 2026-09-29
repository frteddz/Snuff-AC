package dev.snuffac.core.combat;

import dev.snuffac.api.Vec3d;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class CombatEnvironment {

    private final long timestampMillis;
    private final Map<Integer, EntitySnapshot> entities;
    private final int loadedRadius;
    private final int sentChunkRadius;
    private final boolean creative;
    private final boolean spectator;

    private CombatEnvironment(
            long timestampMillis,
            Map<Integer, EntitySnapshot> entities,
            int loadedRadius,
            int sentChunkRadius,
            boolean creative,
            boolean spectator) {
        this.timestampMillis = timestampMillis;
        this.entities = entities;
        this.loadedRadius = loadedRadius;
        this.sentChunkRadius = sentChunkRadius;
        this.creative = creative;
        this.spectator = spectator;
    }

    public static CombatEnvironment empty() {
        return new CombatEnvironment(0L, Map.of(), 0, 0, false, false);
    }

    public static CombatEnvironment of(
            long timestampMillis,
            Map<Integer, EntitySnapshot> entities,
            int loadedRadius,
            int sentChunkRadius) {
        return new CombatEnvironment(
                timestampMillis, Collections.unmodifiableMap(entities), loadedRadius, sentChunkRadius, false, false);
    }

    public static CombatEnvironment of(
            long timestampMillis,
            Map<Integer, EntitySnapshot> entities,
            int loadedRadius,
            int sentChunkRadius,
            boolean creative,
            boolean spectator) {
        return new CombatEnvironment(
                timestampMillis,
                Collections.unmodifiableMap(entities),
                loadedRadius,
                sentChunkRadius,
                creative,
                spectator);
    }

    public boolean creative() {
        return creative;
    }

    public boolean spectator() {
        return spectator;
    }

    public long timestampMillis() {
        return timestampMillis;
    }

    public List<EntitySnapshot> entities() {
        return List.copyOf(entities.values());
    }

    public EntitySnapshot byId(int entityId) {
        return entities.get(entityId);
    }

    public boolean known(int entityId) {
        return entities.containsKey(entityId);
    }

    public int count() {
        return entities.size();
    }

    public int loadedRadius() {
        return loadedRadius;
    }

    public int sentChunkRadius() {
        return sentChunkRadius;
    }

    public double nearestDistance(Vec3d from) {
        double best = Double.MAX_VALUE;
        for (EntitySnapshot entity : entities.values()) {
            best = Math.min(best, entity.distanceFrom(from));
        }
        return best;
    }
}
