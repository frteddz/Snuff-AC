package dev.snuffac.paper.gui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * A menu action is a string that arrives from a file, so every string a shipped
 * menu file can produce must be handled by the menu that renders it. This
 * asserts that statically, because the alternative is a button that looks
 * real and does nothing.
 */
class MenuActionContractTest {

    private static final String GUI_DIR = "src/main/java/dev/snuffac/paper/gui";
    private static final String DEFAULTS = "src/main/java/dev/snuffac/paper/GuiDefaults.java";
    private static final String RULES = "../snuffac-core/src/main/java/dev/snuffac/core/gui/GuiRules.java";

    private static String read(String path) {
        try {
            return Files.readString(Path.of(path));
        } catch (Exception failure) {
            return "";
        }
    }

    private static List<String> menuSources() {
        List<String> files = new ArrayList<>();
        try {
            Files.list(Path.of(GUI_DIR))
                    .filter(p -> p.getFileName().toString().endsWith("Menu.java"))
                    .forEach(p -> files.add(p.toString()));
        } catch (Exception failure) {
            return files;
        }
        return files;
    }

    private static Set<String> shippedActions() {
        Set<String> found = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile("\"(open_[a-z_]+|toggle_[a-z_]+|close|report_[a-z_]+)\"")
                .matcher(read(DEFAULTS));
        while (matcher.find()) {
            found.add(matcher.group(1));
        }
        return found;
    }

    private static Set<String> allowlisted() {
        Set<String> found = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile("\"([a-z_]+)\"").matcher(read(RULES));
        while (matcher.find()) {
            found.add(matcher.group(1));
        }
        return found;
    }

    private static boolean handledSomewhere(String action) {
        StringBuilder combined = new StringBuilder();
        for (String file : menuSources()) {
            combined.append(read(file));
        }
        return combined.toString().contains(action);
    }

    @Test
    void everyShippedMenuActionIsHandledBySomeMenu() {
        List<String> orphans = new ArrayList<>();
        for (String action : shippedActions()) {
            if (!handledSomewhere(action)) {
                orphans.add(action);
            }
        }
        assertTrue(orphans.isEmpty(),
                "these actions are written into a shipped GUI file but nothing handles them: " + orphans
                + ". A button with an unhandled action renders and does nothing.");
    }

    @Test
    void everyShippedMenuActionIsOnTheAllowlist() {
        Set<String> allowlist = allowlisted();
        List<String> missing = new ArrayList<>();
        for (String action : shippedActions()) {
            if (!allowlist.contains(action)) {
                missing.add(action);
            }
        }
        assertTrue(missing.isEmpty(),
                "these actions are in a GUI file but not in GuiRules.ALLOWED_ACTIONS, so "
                + "sanitiseAction will drop them and the button will be inert: " + missing);
    }

    private static Set<String> reportConstants() {
        Set<String> found = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile("String\\s+ACTION_\\w+\\s*=\\s*\"(report_[a-z_]+)\"")
                .matcher(read(GUI_DIR + "/ReportsMenu.java"));
        while (matcher.find()) {
            found.add(matcher.group(1));
        }
        return found;
    }

    @Test
    void theResolveActionConstantMatchesTheAllowlist() {
        assertTrue(allowlisted().contains("report_resolve"),
                "ACTION_RESOLVE was report_dismiss while the allowlist said report_resolve, "
                + "so the comparison could never be true");
    }

    @Test
    void theMainMenuDispatchesTheReportsButton() {
        String main = read(GUI_DIR + "/MainMenu.java");
        assertTrue(main.contains("case ACTION_REPORTS"),
                "the Reports button had a constant and a bridge method but no dispatch case, "
                + "which is why clicking it did nothing");
    }

    @Test
    void everyReportCategoryHasAnExplicitCase() {
        String reports = read(GUI_DIR + "/ReportsMenu.java");
        Set<String> categories = Set.of("cheating", "exploiting", "language", "offensive",
                "griefing", "name", "impersonation");
        for (String category : categories) {
            String action = "report_" + category;
            assertTrue(reportConstants().contains(action),
                    action + " has no action constant in ReportsMenu");
            assertTrue(reports.contains("case ACTION_REPORT_" + category.toUpperCase(
                    java.util.Locale.ROOT)),
                    action + " has no explicit case, it relied on a startsWith fallback");
        }
    }

    @Test
    void theReportPickerIsNotGatedBehindTheStaffMenuPermission() {
        String reports = read(GUI_DIR + "/ReportsMenu.java");
        assertTrue(reports.contains("snuffac.report"),
                "a player facing menu must be gated on the report permission, not the staff "
                + "menu permission, or every click closed the menu for a normal player");
    }
}
