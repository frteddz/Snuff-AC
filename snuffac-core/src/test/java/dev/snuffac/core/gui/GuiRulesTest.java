package dev.snuffac.core.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GuiRulesTest {

    @Test
    void rowsAreClampedToARealInventory() {
        assertEquals(1, GuiRules.clampRows(0));
        assertEquals(1, GuiRules.clampRows(-5));
        assertEquals(3, GuiRules.clampRows(3));
        assertEquals(6, GuiRules.clampRows(6));
        assertEquals(6, GuiRules.clampRows(99));
    }

    @Test
    void aMalformedGuiFileCannotIndexOutsideTheInventory() {
        assertTrue(GuiRules.clampRows(99) * 9 <= 54);
        assertEquals(0, GuiRules.clampSlot(-1, 27));
        assertEquals(26, GuiRules.clampSlot(9999, 27));
        assertEquals(0, GuiRules.clampSlot(5, 0));
    }

    @Test
    void aStackAmountCannotBeAbsurd() {
        assertEquals(1, GuiRules.clampAmount(0));
        assertEquals(1, GuiRules.clampAmount(-3));
        assertEquals(64, GuiRules.clampAmount(64));
        assertEquals(64, GuiRules.clampAmount(5000));
    }

    @Test
    void anActionOutsideTheAllowlistIsDropped() {
        assertFalse(GuiRules.isAllowedAction("op attacker"));
        assertFalse(GuiRules.isAllowedAction("say hello"));
        assertTrue(GuiRules.isAllowedAction("open_settings"));
        assertTrue(GuiRules.isAllowedAction("OPEN_SETTINGS"));
    }

    @Test
    void aCommandStringCanNeverBecomeAnAction() {
        assertEquals("", GuiRules.sanitizeAction("ban %player% sneaky"));
        assertEquals("", GuiRules.sanitizeAction("minecraft:op me"));
        assertEquals("", GuiRules.sanitizeAction("execute as @a run say owned"));
        assertEquals("close", GuiRules.sanitizeAction("close"));
    }

    @Test
    void everyReportCategoryHasAnAction() {
        for (String category : dev.snuffac.core.report.ReportCategory.ALL) {
            assertTrue(GuiRules.isAllowedAction("report_" + category),
                    "missing GUI action for report category " + category);
        }
    }
}
