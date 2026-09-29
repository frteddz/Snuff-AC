package dev.snuffac.paper;

import dev.snuffac.core.config.SnuffConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class VisualConcealment {

    public static final int REVEAL_TICKS = 2;

    private final SnuffPaperPlugin plugin;
    private final SnuffConfig config;

    private final Map<UUID, Map<Integer, State>> tracked = new HashMap<>();

    public VisualConcealment(SnuffPaperPlugin plugin, SnuffConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    private enum State {

        VISIBLE,
        HIDDEN
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
        tracked.remove(playerId);
    }

    public void forgetAll() {
        tracked.clear();
    }

    public void pass(Player viewer) {
        if (!concealsEntities() || viewer == null || !viewer.isOnline()) {
            revealAll(viewer);
            return;
        }
        var data = plugin.dataOf(viewer.getUniqueId());
        if (data == null || data.exempt()) {
            revealAll(viewer);
            return;
        }

        Map<Integer, State> view = tracked.computeIfAbsent(
                viewer.getUniqueId(), ignored -> new HashMap<>());
        Map<Integer, Entity> live = index(viewer.getWorld());
        Set<Integer> relevant = new HashSet<>();

        for (Entity candidate : live.values()) {
            if (!(candidate instanceof LivingEntity living)) {
                continue;
            }
            if (living instanceof Player other && other.getUniqueId().equals(viewer.getUniqueId())) {
                continue;
            }
            relevant.add(candidate.getEntityId());
            if (conceal(viewer, living, live, view)) {
                continue;
            }
        }

        for (Integer id : new ArrayList<>(view.keySet())) {
            if (relevant.contains(id)) {
                continue;
            }
            State state = view.get(id);
            Entity gone = live.get(id);
            if (gone == null || !gone.isValid()) {
                view.remove(id);
                continue;
            }
            if (state == State.HIDDEN) {
                show(viewer, gone);
            }
            view.remove(id);
        }
    }

    private boolean conceal(
            Player viewer,
            LivingEntity target,
            Map<Integer, Entity> live,
            Map<Integer, State> view) {
        if (target.isDead() || !target.isValid()) {
            return false;
        }
        int id = target.getEntityId();
        double distance = LineOfSight.distance(viewer, target);
        double proximity = config.visualRevealRadius();
        double padding = config.visualRevealPadding();

        boolean legal;
        if (distance <= proximity) {
            legal = true;
        } else if (distance <= padding) {
            legal = LineOfSight.clear(viewer, target);
        } else {
            legal = LineOfSight.clear(viewer, target) && distance <= proximity + padding;
        }

        State state = view.get(id);
        if (legal) {
            if (state == State.HIDDEN) {
                show(viewer, target);
            }
            view.put(id, State.VISIBLE);
            return false;
        }
        if (state == State.VISIBLE) {
            hide(viewer, target);
            view.put(id, State.HIDDEN);
        }
        return true;
    }

    private void revealAll(Player viewer) {
        if (viewer == null) {
            return;
        }
        Map<Integer, State> view = tracked.get(viewer.getUniqueId());
        if (view == null) {
            return;
        }
        for (Entity entity : index(viewer.getWorld()).values()) {
            if (view.get(entity.getEntityId()) == State.HIDDEN) {
                show(viewer, entity);
            }
        }
        view.clear();
    }

    private static Map<Integer, Entity> index(org.bukkit.World world) {
        Map<Integer, Entity> map = new HashMap<>();
        for (Entity entity : world.getEntities()) {
            map.putIfAbsent(entity.getEntityId(), entity);
        }
        return map;
    }

    private void hide(Player viewer, Entity target) {
        try {
            viewer.hideEntity(plugin, target);
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    private void show(Player viewer, Entity target) {
        try {
            viewer.showEntity(plugin, target);
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    public List<String> debug(Player viewer) {
        List<String> lines = new ArrayList<>();
        Map<Integer, State> view = tracked.get(viewer.getUniqueId());
        if (view == null) {
            lines.add("no tracked entities");
            return lines;
        }
        int visible = 0;
        int hidden = 0;
        for (State state : view.values()) {
            if (state == State.HIDDEN) {
                hidden++;
            } else {
                visible++;
            }
        }
        lines.add("tracked " + view.size() + ", visible " + visible + ", hidden " + hidden);
        return lines;
    }
}
