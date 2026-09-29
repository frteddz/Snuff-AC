package dev.snuffac.paper;

import dev.snuffac.core.platform.SnuffMessenger;
import dev.snuffac.core.platform.SnuffPermissionChecker;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class BukkitPlatformAdapters {

    private BukkitPlatformAdapters() {
    }

    public static final class Messenger implements SnuffMessenger {

        private static final MiniMessage MINI = MiniMessage.miniMessage();

        private final Plugin plugin;

        public Messenger(Plugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public void sendMessage(Object handle, String message) {
            if (handle instanceof Player player && player.isOnline()) {
                Component component = mini(message);
                runOnMain(() -> {
                    if (player.isOnline()) {
                        player.sendMessage(component);
                    }
                });
            }
        }

        @Override
        public void broadcast(String message, String permission) {
            Component component = mini(LegacyColour.toMiniMessage(message));
            runOnMain(() -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    deliver(player, component, permission);
                }
            });
        }

        private void deliver(Player player, Component component, String permission) {
            try {
                if (player == null || !player.isOnline()) {
                    return;
                }
                if (permission != null && !permission.isEmpty() && !player.hasPermission(permission)) {
                    return;
                }
                player.sendMessage(component);
            } catch (RuntimeException exception) {
                SnuffPaperPlugin.reportMessengerFailure(exception);
            }
        }

        @Override
        public void sendConsole(String message) {
            Component component = mini(message);
            runOnMain(() -> {
                try {
                    Bukkit.getConsoleSender().sendMessage(component);
                } catch (RuntimeException exception) {
                    SnuffPaperPlugin.reportMessengerFailure(exception);
                }
            });
        }

        private void runOnMain(Runnable action) {
            try {
                if (Bukkit.isPrimaryThread()) {
                    action.run();
                    return;
                }
                if (plugin == null || !plugin.isEnabled()) {
                    return;
                }
                Bukkit.getScheduler().runTask(plugin, action);
            } catch (RuntimeException exception) {
                SnuffPaperPlugin.reportMessengerFailure(exception);
            }
        }

        private static Component mini(String message) {
            try {
                return MINI.deserialize(message);
            } catch (RuntimeException exception) {
                return Component.text(message);
            }
        }
    }

    public static final class Permissions implements SnuffPermissionChecker {

        @Override
        public boolean hasPermission(Object handle, String permission) {
            return handle instanceof Player player && player.hasPermission(permission);
        }

        @Override
        public List<String> playersWithPermission(String permission) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission(permission)) {
                    names.add(player.getName());
                }
            }
            return names;
        }
    }

    public static UUID uuidOf(Player player) {
        return player.getUniqueId();
    }
}
