package dev.snuffac.paper;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Messages {

    private static final Map<String, String> TEMPLATES = new LinkedHashMap<>();

    static {
        TEMPLATES.put("usage", "<gray>Usage: <white>/snuff <info|version|reload|debug|alerts|checks|violations|toggle|stats|setback|profile></white>");
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
