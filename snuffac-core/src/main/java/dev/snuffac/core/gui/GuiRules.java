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

    public static boolean isAllowedAction(String action) {
        if (action == null) {
            return false;
        }
        return ALLOWED_ACTIONS.contains(action.trim().toLowerCase(Locale.ROOT));
    }

    public static String sanitizeAction(String action) {
        if (!isAllowedAction(action)) {
            return "";
        }
        return action.trim().toLowerCase(Locale.ROOT);
    }
}
