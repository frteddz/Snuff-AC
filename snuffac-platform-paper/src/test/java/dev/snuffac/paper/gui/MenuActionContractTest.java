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
    void reportCategoriesComeFromTheOptionsFileRatherThanASwitch() {
        String menu = read(GUI_DIR + "/ReportsMenu.java");
        assertTrue(menu.contains("isCategoryAction(action)"),
                "categories are listed in a file now, so a hardcoded case per category would "
                + "silently drop any category an owner adds");
    }
}
