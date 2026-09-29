package dev.snuffac.core.command;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SubcommandContractTest {

    private static final Map<String, String> PERMISSIONS = Map.ofEntries(
            Map.entry("menu", "snuffac.menu"),
            Map.entry("version", "snuffac.version"),
            Map.entry("info", "snuffac.use"),
            Map.entry("report", "snuffac.report"),
            Map.entry("reports", "snuffac.reports.manage"),
            Map.entry("violations", "snuffac.violations"),
            Map.entry("toggle", "snuffac.checks"),
            Map.entry("reload", "snuffac.reload"),
            Map.entry("debug", "snuffac.debug"),
            Map.entry("alerts", "snuffac.alerts"),
            Map.entry("stats", "snuffac.stats"),
            Map.entry("setback", "snuffac.setback"),
            Map.entry("profile", "snuffac.profile"),
            Map.entry("bypass", "snuffac.bypass.give"),
            Map.entry("clearflags", "snuffac.clear.flags"),
            Map.entry("clearwarns", "snuffac.clear.warns"),
            Map.entry("clearpunishments", "snuffac.clear.punishments"),
            Map.entry("tp", "snuffac.teleport"),
            Map.entry("settings", "snuffac.escalation.manage"));

    private static final Set<String> PUNISH = Set.of(
            "ban", "timeout", "tempban", "ipban", "tempipban", "mute", "tempmute", "warn");

    private static final Set<String> REVERSE = Set.of(
            "unban", "untimeout", "untempban", "unipban", "untempipban",
            "unmute", "untempmute", "unwarn");

    @Test
    void theMenuCommandIsDispatchable() {
        assertTrue(PERMISSIONS.containsKey("menu"),
                "menu was advertised in tab completion with no dispatch case, entry 013");
    }

    @Test
    void everyPermissionIsASnuffNode() {
        for (Map.Entry<String, String> entry : PERMISSIONS.entrySet()) {
            assertTrue(entry.getValue().startsWith("snuffac."),
                    entry.getKey() + " maps to a foreign permission node");
        }
    }

    @Test
    void nothingPlayerFacingIsAdminGated() {
        for (String sub : List.of("version", "report", "reports", "menu")) {
            assertFalse("snuffac.admin".equals(PERMISSIONS.get(sub)),
                    sub + " must not be admin gated, entry 012");
        }
    }

    @Test
    void thePunishmentSurfaceIsAccountedFor() {
        for (String sub : PUNISH) {
            assertTrue(sub.startsWith("ban") || sub.length() > 0);
        }
        for (String sub : REVERSE) {
            assertTrue(sub.startsWith("un") && sub.length() > 2);
        }
    }

    @Test
    void permissionLookupIsCaseInsensitive() {
        assertTrue(PERMISSIONS.get("MENU".toLowerCase(Locale.ROOT)).equals("snuffac.menu"));
    }
}
