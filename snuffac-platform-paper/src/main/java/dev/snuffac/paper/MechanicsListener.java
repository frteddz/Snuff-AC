package dev.snuffac.paper;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public final class MechanicsListener implements Listener {

    private final SnuffPaperPlugin plugin;

    public MechanicsListener(SnuffPaperPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        var item = event.getItem();
        if (item == null) {
            return;
        }
        String type = item.getType().name();
        if (type.contains("WIND_CHARGE")) {
            var data = plugin.dataOf(player.getUniqueId());
            if (data != null) {
                data.movement().markWindCharge();
            }
            return;
        }
        if (type.contains("BREEZE_ROD") || type.contains("BREEZE_WIND_CHARGE")) {
            var data = plugin.dataOf(player.getUniqueId());
            if (data != null) {
                data.movement().markWindCharge();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player shooter)) {
            return;
        }
        if (!isWindChargeProjectile(event.getEntity())) {
            return;
        }
        var data = plugin.dataOf(shooter.getUniqueId());
        if (data != null) {
            data.movement().markWindCharge();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (!isWindChargeSource(event)) {
            return;
        }
        var data = plugin.dataOf(victim.getUniqueId());
        if (data != null) {
            data.movement().markWindChargeHit();
        }
    }

    private static boolean isWindChargeSource(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Projectile projectile) {
            return isWindChargeProjectile(projectile);
        }
        return false;
    }

    private static boolean isWindChargeProjectile(Projectile projectile) {
        var meta = projectile.getMetadata("minecraft:wind_charge");
        if (meta != null && !meta.isEmpty()) {
            return true;
        }
        String key = projectile.getType().getKey().toString();
        return key.contains("wind_charge");
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(org.bukkit.event.player.AsyncPlayerChatEvent event) {
        if (plugin.punishments().isMuted(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            String reason = "no reason recorded";
            for (var record : plugin.punishments().active(event.getPlayer().getUniqueId())) {
                if (record.kind() == dev.snuffac.core.punish.PunishmentService.Kind.MUTE
                        || record.kind() == dev.snuffac.core.punish.PunishmentService.Kind.TEMPMUTE) {
                    reason = record.reason();
                    break;
                }
            }
            event.getPlayer().sendMessage(net.kyori.adventure.text.Component.text(
                    "[Snuff] You are muted. Reason: " + reason));
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCommand(org.bukkit.event.player.PlayerCommandPreprocessEvent event) {
        if (plugin.punishments().isMuted(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(net.kyori.adventure.text.Component.text(
                    "[Snuff] You are muted and cannot run commands."));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        var data = plugin.dataOf(player.getUniqueId());
        if (data != null) {
            data.movement().markBroke();
        }
    }
}
