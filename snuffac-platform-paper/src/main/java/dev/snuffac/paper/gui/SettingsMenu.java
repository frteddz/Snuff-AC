package dev.snuffac.paper.gui;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.Plugin;

public final class SettingsMenu extends SnuffMenu {

    public static final String ACTION_RETENTION_UP = "retention_up";
    public static final String ACTION_RETENTION_DOWN = "retention_down";
    public static final String ACTION_HISTORY_UP = "history_up";
    public static final String ACTION_HISTORY_DOWN = "history_down";
    public static final String ACTION_PREVENTION = "prevention";
    public static final String ACTION_ESCALATION = "escalation";
    public static final String ACTION_ALERT_COOLDOWN_UP = "cooldown_up";
    public static final String ACTION_ALERT_COOLDOWN_DOWN = "cooldown_down";
    public static final String ACTION_RELOAD = "reload";
    public static final String ACTION_CLOSE = "close";

    private final dev.snuffac.paper.SnuffPaperPlugin plugin;

    public SettingsMenu(Plugin plugin) {
        super(plugin, 27, "Snuff Settings", GuiLayout.load(plugin, dev.snuffac.paper.GuiDefaults.SETTINGS));
        this.plugin = (dev.snuffac.paper.SnuffPaperPlugin) plugin;
    }

    @Override
    protected void render() {
        GuiLayout.Layout layout = layout();
        if (layout == null || layout.buttons().isEmpty()) {
            return;
        }
        for (GuiLayout.Button button : layout.buttons()) {
            if (!button.enabled()) {
                continue;
            }
            set(button.slot(), button.material(), button.name(), values(button.lore()), button.action());
        }
    }

    private List<String> values(List<String> lore) {
        var config = plugin.core().config();
        List<String> rendered = new ArrayList<>(lore.size());
        for (String line : lore) {
            rendered.add(line
                    .replace("{log-retention}", String.valueOf(config.logRetentionDays()))
                    .replace("{history-retention}", String.valueOf(config.historyRetentionDays()))
                    .replace("{prevention}", config.preventionEnabled() ? "on" : "off")
                    .replace("{cooldown}", String.valueOf(config.alertCooldownMillis()))
                    .replace("{max-warnings}", String.valueOf(escalation().maxWarnings()))
                    .replace("{ban-length}", dev.snuffac.core.punish.Durations
                            .describe(escalation().banMillis()))
                    .replace("{min-confidence}", String.valueOf(escalation().minConfidence()))
                    .replace("{escalation}", escalation().enabled() ? "on" : "off"));
        }
        return rendered;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!plugin.canUseMenu(player)) {
            close(player);
            return;
        }
        boolean increase = event.isRightClick();
        var config = plugin.core().config();
        String action = actionOf(event.getInventory(), event.getRawSlot());
        if (action == null) {
            return;
        }
        switch (action) {
            case "set_log_retention" -> {
                plugin.promptForRetention(player, RetentionKind.LOG);
                close(player);
                return;
            }
            case "set_history_retention" -> {
                plugin.promptForRetention(player, RetentionKind.HISTORY);
                close(player);
                return;
            }
            case "toggle_prevention" -> {
                config.preventionEnabled(!config.preventionEnabled());
                plugin.persistPrevention();
            }
            case "cooldown" -> {
                config.alertCooldownMillis(clamp(config.alertCooldownMillis() + (increase ? 500 : -500), 0, 60_000));
                plugin.persistRetention();
            }
            case "escalation" -> {
                if (increase) {
                    escalation().maxWarnings(clamp(escalation().maxWarnings() + 1, 1, 50));
                } else {
                    escalation().enabled(!escalation().enabled());
                }
                plugin.applyEscalationConfigFromSettings();
            }
            case ACTION_CLOSE -> {
                close(player);
                return;
            }
            case ACTION_RELOAD -> plugin.reloadEverything();
            default -> {
                return;
            }
        }
        build();
        player.updateInventory();
    }

    public enum RetentionKind {
        LOG,
        HISTORY
    }

    private dev.snuffac.core.punish.EscalationService escalation() {
        return plugin.escalation();
    }

    private static int clamp(int value, int low, int high) {
        return Math.max(low, Math.min(high, value));
    }
}
