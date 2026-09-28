package dev.snuffac.paper;

import dev.snuffac.core.packet.ServerTeleportPacket;
import dev.snuffac.core.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public final class SnuffPlayerListener implements Listener {

    private final SnuffPaperPlugin plugin;

    public SnuffPlayerListener(SnuffPaperPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> plugin.registerPlayer(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        plugin.unregisterPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRespawn(PlayerRespawnEvent event) {
        PlayerData data = plugin.dataOf(event.getPlayer().getUniqueId());
        if (data != null) {
            data.reset();
            data.movement().teleportedThisTick(true);
        }
        Location spawn = event.getRespawnLocation();
        if (spawn != null && data != null) {
            data.movement().position(new dev.snuffac.api.Vec3d(spawn.getX(), spawn.getY(), spawn.getZ()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getTo() == null) {
            return;
        }
        PlayerData data = plugin.dataOf(event.getPlayer().getUniqueId());
        if (data == null) {
            return;
        }
        ServerTeleportPacket.TeleportCause cause = switch (event.getCause()) {
            case PLUGIN -> ServerTeleportPacket.TeleportCause.PLUGIN;
            case COMMAND -> ServerTeleportPacket.TeleportCause.COMMAND;
            case NETHER_PORTAL, END_PORTAL, END_GATEWAY -> ServerTeleportPacket.TeleportCause.PORTAL;
            default -> ServerTeleportPacket.TeleportCause.UNKNOWN;
        };
        plugin.noteTeleport(data, cause, event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        PlayerData data = plugin.dataOf(event.getPlayer().getUniqueId());
        if (data != null) {
            data.reset();
            data.movement().teleportedThisTick(true);
        }
    }
}
