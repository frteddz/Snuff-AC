package dev.snuffac.core.gui;

import java.util.Locale;
import java.util.Set;

public final class GuiRules {

    public static final String FOLDER = "GUI";
    public static final int MIN_ROWS = 1;
    public static final int MAX_ROWS = 6;
    public static final int MIN_AMOUNT = 1;
    public static final int MAX_AMOUNT = 64;

    public static final Set<String> ALLOWED_ACTIONS = Set.of(
            "open_sus",
            "toggle_alerts",
            "open_settings",
            "open_warnings",
            "open_reports",
            "open_reports_admin",
            "back",
            "refresh",
            "page_prev",
            "page_next",
            "set_log_retention",
            "set_history_retention",
            "toggle_prevention",
            "cooldown",
            "escalation",
            "reload",
            "report_cheating",
            "report_exploiting",
            "report_language",
            "report_offensive",
            "report_griefing",
            "report_name",
            "report_impersonation",
            "report_back",
            "report_submit",
            "report_claim",
            "report_unclaim",
            "report_resolve",
            "close",
            "none");

    private GuiRules() {
    }

    public static int clampRows(int rows) {
        if (rows < MIN_ROWS) {
            return MIN_ROWS;
        }
        return Math.min(rows, MAX_ROWS);
    }

    public static int clampSlot(int slot, int size) {
        if (size <= 0) {
            return 0;
        }
        if (slot < 0) {
            return 0;
        }
        return Math.min(slot, size - 1);
    }

    public static int clampAmount(int amount) {
        if (amount < MIN_AMOUNT) {
            return MIN_AMOUNT;
        }
        return Math.min(amount, MAX_AMOUNT);
    }

    private static final java.util.regex.Pattern REPORT_CATEGORY =
            java.util.regex.Pattern.compile("report_[a-z0-9_]{1,24}");

    private static final java.util.Set<String> REPORT_RESERVED = Set.of(
            "report_back", "report_submit", "report_claim", "report_unclaim", "report_resolve");

    public static boolean isAllowedAction(String action) {
        if (action == null) {
            return false;
        }
        String candidate = action.trim().toLowerCase(Locale.ROOT);
        if (ALLOWED_ACTIONS.contains(candidate)) {
            return true;
        }
        // report categories are listed in a file the owner edits, so the set of
        // valid ones cannot be known here. The id is still constrained, and the
        // built in control actions are excluded so a category cannot shadow them.
        return !REPORT_RESERVED.contains(candidate) && REPORT_CATEGORY.matcher(candidate).matches();
    }

    public static String sanitizeAction(String action) {
        if (!isAllowedAction(action)) {
            return "";
        }
        return action.trim().toLowerCase(Locale.ROOT);
    }
}
