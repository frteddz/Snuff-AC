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

public final class BukkitPlatformAdapters {

    private BukkitPlatformAdapters() {
    }

    public static final class Messenger implements SnuffMessenger {

        private static final MiniMessage MINI = MiniMessage.miniMessage();

        @Override
        public void sendMessage(Object handle, String message) {
            if (handle instanceof Player player && player.isOnline()) {
                player.sendMessage(mini(message));
            }
        }

        @Override
        public void broadcast(String message, String permission) {
            Component component = mini(message);
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (permission == null || permission.isEmpty() || player.hasPermission(permission)) {
                    player.sendMessage(component);
                }
            }
        }

        @Override
        public void sendConsole(String message) {
            Bukkit.getConsoleSender().sendMessage(mini(message));
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
