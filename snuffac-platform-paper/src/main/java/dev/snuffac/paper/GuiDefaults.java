package dev.snuffac.paper;

import dev.snuffac.paper.gui.GuiLayout;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public final class GuiDefaults {

    public static final String MAIN = "main-gui";
    public static final String SETTINGS = "settings-gui";
    public static final String FLAGS = "flags-gui";
    public static final String WARNED = "warned-gui";
    public static final String REPORTS = "reports-gui";
    public static final String REPORTS_ADMIN = "reports-admin-gui";

    private GuiDefaults() {
    }

    public static void write(Plugin plugin) {
        write(plugin, MAIN, main());
        write(plugin, SETTINGS, settings());
        write(plugin, FLAGS, flags());
        write(plugin, WARNED, warned());
        write(plugin, REPORTS, reports());
        write(plugin, REPORTS_ADMIN, reportsAdmin());
    }

    private static void write(Plugin plugin, String id, YamlConfiguration yaml) {
        var file = new java.io.File(GuiLayout.folder(plugin), id + ".yml");
        if (file.isFile()) {
            return;
        }
        try {
            GuiLayout.write(plugin, id, yaml);
        } catch (java.io.IOException failure) {
            plugin.getLogger().warning("Could not write GUI file " + id + ".yml: " + failure);
        }
    }

    private static YamlConfiguration main() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("title", "Snuff AC");
        yaml.set("rows", 3);
        yaml.set("filler-material", "BLACK_STAINED_GLASS_PANE");
        yaml.set("filler", true);
        item(yaml, 10, "PLAYER_HEAD", "Suspicious Players",
                List.of("Players with recorded flags", "Click to review"), "open_sus", "snuffac.admin", 1, false);
        item(yaml, 12, "WRITABLE_BOOK", "Warnings", List.of("Warned players"), "open_warnings",
                "snuffac.admin", 1, false);
        item(yaml, 14, "BELL", "Your Alerts", List.of("Toggle the alerts you receive"), "toggle_alerts",
                "snuffac.alerts", 1, false);
        item(yaml, 15, "BOOK", "Reports", List.of("Browse player reports"), "open_reports_admin",
                "snuffac.admin", 1, false);
        item(yaml, 16, "COMPARATOR", "Settings", List.of("Retention, prevention, alerts"), "open_settings",
                "snuffac.admin", 1, false);
        item(yaml, 22, "BARRIER", "Close", List.of(), "close", "", 1, false);
        return yaml;
    }

    private static YamlConfiguration settings() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("title", "Snuff AC Settings");
        yaml.set("rows", 3);
        yaml.set("filler-material", "GRAY_STAINED_GLASS_PANE");
        yaml.set("filler", true);
        item(yaml, 10, "BOOK", "Log Retention", List.of("Days before old log files are deleted",
                "<gray>Current: <aqua>{log-retention} days", "", "Click, then type the number in chat",
                "Type cancel to keep it"), "set_log_retention", "snuffac.admin", 1, false);
        item(yaml, 11, "PAPER", "History Retention", List.of("Days before old flag history is purged",
                "<gray>Current: <aqua>{history-retention} days", "", "Click, then type the number in chat",
                "Type cancel to keep it"), "set_history_retention", "snuffac.admin", 1, false);
        item(yaml, 12, "COMPARATOR", "Prevention",
                List.of("Turn setbacks and cancellation on or off",
                        "<gray>State: <white>{prevention}", "", "Left click to toggle"),
                "toggle_prevention", "snuffac.escalation.manage", 1, false);
        item(yaml, 13, "CLOCK", "Alert Cooldown", List.of("Per player alert rate limit",
                "<gray>Current: <aqua>{cooldown}ms", "", "Left click to decrease",
                "Right click to increase"), "cooldown", "snuffac.alerts.manage", 1, false);
        item(yaml, 14, "LADDER", "Warn Ladder", List.of("Automatic warns then a timed ban",
                "<gray>Max warnings: <white>{max-warnings}",
                "<gray>Ban length: <white>{ban-length}",
                "<gray>Minimum confidence: <white>{min-confidence}",
                "<gray>State: <white>{escalation}", "", "Left click to toggle on or off",
                "Right click to raise or lower the warning limit"), "escalation",
                "snuffac.escalation.manage", 1, false);
        item(yaml, 15, "BARRIER", "Close", List.of(), "close", "snuffac.use", 1, false);
        item(yaml, 22, "REDSTONE", "Reload Config",
                List.of("Re-read config and checks from disk"), "reload", "snuffac.admin", 1, false);
        return yaml;
    }

    private static YamlConfiguration flags() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("title", "Snuff AC Cases");
        yaml.set("rows", 6);
        yaml.set("filler-material", "LIGHT_BLUE_STAINED_GLASS_PANE");
        yaml.set("filler", true);
        item(yaml, 45, "ARROW", "Back", List.of(), "report_back", "snuffac.admin", 1, false);
        return yaml;
    }

    private static YamlConfiguration warned() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("title", "Snuff AC Warnings");
        yaml.set("rows", 6);
        yaml.set("filler-material", "YELLOW_STAINED_GLASS_PANE");
        yaml.set("filler", true);
        item(yaml, 45, "ARROW", "Back", List.of(), "report_back", "snuffac.admin", 1, false);
        return yaml;
    }

    private static YamlConfiguration reports() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("title", "Report a player");
        yaml.set("rows", 6);
        yaml.set("filler-material", "RED_STAINED_GLASS_PANE");
        yaml.set("filler", true);
        item(yaml, 10, "DIAMOND_SWORD", "Cheating", List.of("Use of a client that gives an unfair advantage"),
                "report_cheating", "snuffac.report", 1, false);
        item(yaml, 11, "TNT", "Exploiting", List.of("Duplication, crashes, or abusing a flaw"),
                "report_exploiting", "snuffac.report", 1, false);
        item(yaml, 12, "PAPER", "Explicit Language", List.of("Sexual or graphic language"),
                "report_language", "snuffac.report", 1, false);
        item(yaml, 13, "REDSTONE", "Offensive Behaviour", List.of("Harassment, toxicity, or slurs"),
                "report_offensive", "snuffac.report", 1, false);
        item(yaml, 14, "IRON_PICKAXE", "Griefing", List.of("Destroying or stealing other people's work"),
                "report_griefing", "snuffac.report", 1, false);
        item(yaml, 15, "NAME_TAG", "Inappropriate Name", List.of("A username that breaks the rules"),
                "report_name", "snuffac.report", 1, false);
        item(yaml, 16, "PLAYER_HEAD", "Staff Impersonation", List.of("Pretending to be an administrator"),
                "report_impersonation", "snuffac.report", 1, false);
        item(yaml, 49, "PAPER", "Submit report", List.of("Confirm the selected category"),
                "report_submit", "snuffac.report", 1, false);
        item(yaml, 45, "ARROW", "Back", List.of(), "report_back", "snuffac.report", 1, false);
        return yaml;
    }

    private static YamlConfiguration reportsAdmin() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("title", "Snuff AC Reports");
        yaml.set("rows", 6);
        yaml.set("filler-material", "PURPLE_STAINED_GLASS_PANE");
        yaml.set("filler", true);
        item(yaml, 45, "ARROW", "Back", List.of(), "report_back", "snuffac.admin", 1, false);
        item(yaml, 48, "LIME_CONCRETE", "Claim", List.of("Take ownership of the top report"),
                "report_claim", "snuffac.admin", 1, false);
        item(yaml, 50, "RED_CONCRETE", "Resolve", List.of("Close the top report as handled"),
                "report_resolve", "snuffac.admin", 1, false);
        return yaml;
    }

    private static void item(YamlConfiguration yaml, int slot, String material, String name,
            List<String> lore, String action, String permission, int amount, boolean glow) {
        String base = "items." + slot;
        yaml.set(base + ".slot", slot);
        yaml.set(base + ".material", material);
        yaml.set(base + ".name", "<white>" + name);
        yaml.set(base + ".lore", lore.stream().map(line -> "<gray>" + line).toList());
        yaml.set(base + ".action", action);
        yaml.set(base + ".permission", permission);
        yaml.set(base + ".amount", amount);
        yaml.set(base + ".glow", glow);
        yaml.set(base + ".enabled", true);
    }
}
