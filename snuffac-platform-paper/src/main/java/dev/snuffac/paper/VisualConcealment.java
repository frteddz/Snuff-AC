package dev.snuffac.paper;

import dev.snuffac.core.combat.EntitySnapshot;
import dev.snuffac.core.config.SnuffConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class VisualConcealment {

    private final SnuffPaperPlugin plugin;
    private final SnuffConfig config;

    private final Map<UUID, Set<Integer>> visible = new HashMap<>();
    private final Map<UUID, Set<Integer>> seeded = new HashMap<>();

    public VisualConcealment(SnuffPaperPlugin plugin, SnuffConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public boolean enabled() {
        return config.antiXrayMode() != null
                && config.antiXrayMode() != dev.snuffac.core.world.ObfuscationPolicy.OFF;
    }

    public boolean concealsEntities() {
        return enabled() && config.visualEntityHiding();
    }

    public boolean concealsSounds() {
        return enabled() && config.visualSoundFuzzing();
    }

    public void forget(UUID playerId) {
        visible.remove(playerId);
        seeded.remove(playerId);
    }

    public Set<Integer> currentlyVisible(UUID playerId) {
        return visible.computeIfAbsent(playerId, ignored -> new HashSet<>());
    }

    public Set<Integer> seeded(UUID playerId) {
        return seeded.computeIfAbsent(playerId, ignored -> new HashSet<>());
    }

    public List<LivingEntity> concealPass(Player viewer) {
        List<LivingEntity> hidden = new ArrayList<>();
        if (!concealsEntities() || viewer == null || !viewer.isOnline()) {
            return hidden;
        }
        var data = plugin.dataOf(viewer.getUniqueId());
        if (data == null || data.exempt()) {
            return hidden;
        }

        Set<Integer> seen = currentlyVisible(viewer.getUniqueId());
        Set<Integer> known = seeded(viewer.getUniqueId());
        Map<Integer, Entity> index = LineOfSight.index(viewer.getWorld());

        for (EntitySnapshot snapshot : data.combatEnvironment().entities()) {
            Entity target = LineOfSight.live(snapshot, index);
            if (target == null || !(target instanceof LivingEntity living)) {
                continue;
            }
            if (living instanceof Player other && other.getUniqueId().equals(viewer.getUniqueId())) {
                continue;
            }
            if (living.isDead() || !living.isValid()) {
                seen.remove(snapshot.entityId());
                known.remove(snapshot.entityId());
                continue;
            }

            double proximity = config.visualRevealRadius();
            double padding = config.visualRevealPadding();
            boolean legal = LineOfSight.visuallyReachable(viewer, snapshot, index, proximity, padding);

            if (legal) {
                if (known.add(snapshot.entityId())) {
                    seen.add(snapshot.entityId());
                }
                continue;
            }

            if (seen.remove(snapshot.entityId())) {
                known.remove(snapshot.entityId());
                hide(viewer, living);
            }
        }

        for (Integer stale : new ArrayList<>(seen)) {
            Entity target = index.get(stale);
            if (target == null || !target.isValid()) {
                seen.remove(stale);
                known.remove(stale);
            }
        }
        return hidden;
    }

    private void hide(Player viewer, LivingEntity target) {
        try {
            viewer.hideEntity(plugin, target);
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    public boolean isVisible(UUID playerId, int entityId) {
        return currentlyVisible(playerId).contains(entityId);
    }

    public void forgetAll() {
        visible.clear();
        seeded.clear();
    }
}
