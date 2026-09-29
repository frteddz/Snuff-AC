package dev.snuffac.paper.gui;

import dev.snuffac.core.player.PlayerData;
import java.util.List;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.Plugin;

public final class MainMenu extends SnuffMenu {

    public static final String ACTION_SUSPICIOUS = "open_sus";
    public static final String ACTION_ALERTS = "toggle_alerts";
    public static final String ACTION_SETTINGS = "open_settings";
    public static final String ACTION_WARNINGS = "open_warnings";
    public static final String ACTION_CLOSE = "close";

    private final GuiBridge bridge;
    private final boolean alertsOn;

    public MainMenu(Plugin plugin, GuiBridge bridge) {
        this(plugin, bridge, true);
    }

    public MainMenu(Plugin plugin, GuiBridge bridge, boolean alertsOn) {
        super(plugin, 27, "Snuff AC");
        this.bridge = bridge;
        this.alertsOn = alertsOn;
    }

    @Override
    protected void render() {
        set(10, Material.PLAYER_HEAD, "<white>Suspicious Players",
                List.of("<gray>Players with recorded flags", "<gray>Click to review"), ACTION_SUSPICIOUS);
        set(14, Material.BELL, "<white>Your Alerts",
                List.of("<gray>Toggle the alerts you receive",
                        "<gray>Current: <white>" + (alertsOn ? "on" : "off")), ACTION_ALERTS);
        set(12, Material.WRITABLE_BOOK, "<white>Warnings",
                List.of("<gray>Warned players"), ACTION_WARNINGS);
        set(16, Material.COMPARATOR, "<white>Settings",
                List.of("<gray>Retention, prevention, alerts"), ACTION_SETTINGS);
        set(22, Material.BARRIER, "<white>Close", List.of(), ACTION_CLOSE);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String action = actionOf(event.getInventory(), event.getRawSlot());
        if (action == null) {
            return;
        }
        PlayerData data = bridge.dataOf(player.getUniqueId());
        switch (action) {
            case ACTION_SUSPICIOUS -> bridge.openSuspicious(player, 0);
            case ACTION_ALERTS -> {
                if (data != null) {
                    data.alertsEnabled(!data.alertsEnabled());
                    bridge.alertsToggled(player, data.alertsEnabled());
                }
            }
            case ACTION_SETTINGS -> {
                if (player.hasPermission("snuffac.admin")) {
                    bridge.openSettings(player);
                }
            }
            case ACTION_WARNINGS -> bridge.openWarned(player);
            case ACTION_CLOSE -> close(player);
            default -> {
            }
        }
    }

    public interface GuiBridge {

        PlayerData dataOf(UUID id);

        void openSuspicious(Player player, int page);

        void openSettings(Player player);

        void openWarned(Player player);

        void alertsToggled(Player player, boolean enabled);
    }
}
