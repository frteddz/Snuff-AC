package dev.snuffac.paper;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Messages {

    private static final Map<String, String> TEMPLATES = new LinkedHashMap<>();

    static {
        TEMPLATES.put("usage", "<gray>Usage: <white>/snuff</white> opens the menu. Try <white>version</white>, <white>violations [player]</white>, <white>settings</white>, <white>punishments [player]</white>");
        TEMPLATES.put("punish-usage", "<gray>Usage <white><use></white>");
        TEMPLATES.put("bad-duration", "<red>That duration is not valid. <white><input></white> <dark_gray><why>");
        TEMPLATES.put("punish-no-reason", "<red>A reason is required. <gray>Example: <white><use>");
        TEMPLATES.put("punish-unknown", "<red>No cached player called <white><player></white><gray>. They must have joined before.");
        TEMPLATES.put("punish-protected", "<red><player> is marked unpunishable.");
        TEMPLATES.put("punish-too-long", "<red>Your permission cap does not allow <white><duration></white>.");
        TEMPLATES.put("punish-applied", "<green><type> <white><player></white> <gray>for <white><duration></white>. <gray>Reason: <white><reason></white> <dark_gray>(<id>)");
        TEMPLATES.put("reverse-applied", "<green>Removed <white><type></white> from <white><player></white> <gray>(<count> record(s))");
        TEMPLATES.put("reverse-none", "<gray><player> has no active punishment of that type.");
        TEMPLATES.put("punish-none", "<gray>Nothing is active against <white><player></white>.");
        TEMPLATES.put("punish-none-online", "<gray>No online player has an active punishment.");
        TEMPLATES.put("punish-online-header", "<gray>Online players with active punishments: <white><count></white>");
        TEMPLATES.put("punish-list-header", "<gray>Active punishments for <white><player></white>: <white><count></white>");
        TEMPLATES.put("warn-none", "<gray><white><player></white> has no warnings.");
        TEMPLATES.put("warn-header", "<gray>Warnings for <white><player></white>: <white><count></white>");
        TEMPLATES.put("muted", "<red>You are muted. Reason: <white><reason></white>");
        TEMPLATES.put("sounds-toggled", "<gray>Your Snuff sounds are now <white><state></white>.");
        TEMPLATES.put("no-permission", "<red>You do not have permission to use this command.</red>");
        TEMPLATES.put("unknown", "<red>Unknown subcommand <white><sub></white>.</red>");
        TEMPLATES.put("unknown-check", "<red>Unknown check <white><check></white>.</red>");
        TEMPLATES.put("no-player", "<red>That player is not being tracked.</red>");
        TEMPLATES.put("version", "<white>Snuff AC <version></white> on <white><platform></white> with <white><checks></white> checks.");
        TEMPLATES.put("info", "<white>Snuff AC <version></white>\n<gray>Platform:</gray> <white><platform></white>\n<gray>Checks:</gray> <white><checks></white>\n<gray>Enabled:</gray> <white><enabled></white>\n<gray>TPS:</gray> <white><tps></white>\n<gray>Tracked players:</gray> <white><players></white>");
        TEMPLATES.put("stats", "<gray>Tracked:</gray> <white><players></white> <gray>Total VL:</gray> <white><violations></white>\n<gray>TPS:</gray> <white><tps></white> <gray>Worst tick TPS:</gray> <white><minTps></white>");
        TEMPLATES.put("reloaded", "<green>Configuration reloaded.</green>");
        TEMPLATES.put("toggled", "<gray><check></gray> is now <state>.");
        TEMPLATES.put("debug-toggled", "<gray>Debug for</gray> <white><player></white> <gray>is now <state></gray>.");
        TEMPLATES.put("alerts-verbose-toggled", "<gray>Verbose alerts are now <white><state></white>.");
        TEMPLATES.put("gui-needs-player", "<red>That needs a player. Open it in game instead.");
        TEMPLATES.put("alerts-toggled", "<gray>Alerts for</gray> <white><player></white> <gray>are now <state></gray>.");
        TEMPLATES.put("no-violations", "<gray><player></gray> has no recorded violations.");
        TEMPLATES.put("setback", "<gray>Setback applied to</gray> <white><player></white>.");
    }

    private Messages() {
    }

    public static String render(String key, Map<String, String> placeholders) {
        String template = TEMPLATES.get(key);
        if (template == null) {
            return "<red>missing message: " + key + "</red>";
        }
        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("<" + entry.getKey() + ">", entry.getValue());
        }
        return result;
    }

    public static String render(String key) {
        return render(key, Map.of());
    }
}
