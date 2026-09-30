package dev.snuffac.paper.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snuffac.core.config.ConfigSource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Every menu is meant to be editable from a file. This parses layout sources
 * directly so the rules hold without a server, including the rule that
 * mattered most: a button whose slot is outside the inventory is a bug, not
 * something to silently relocate.
 */
class GuiLayoutContractTest {

    private static ConfigSource source(Map<String, Object> values) {
        return ConfigSource.ofMap(values);
    }

    private static Map<String, Object> base(int rows) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("title", "Test");
        values.put("rows", rows);
        return values;
    }

    @Test
    void rowsDriveTheInventorySize() {
        GuiLayout.Layout layout = GuiLayout.parse("t", source(base(4)));
        assertEquals(36, layout.size());
        assertEquals(4, layout.rows());
    }

    @Test
    void aButtonIsReadWithItsActionAndPermission() {
        Map<String, Object> values = base(3);
        values.put("items.10.material", "STONE");
        values.put("items.10.name", "A button");
        values.put("items.10.action", "open_reports");
        values.put("items.10.permission", "snuffac.report");
        values.put("items.10.glow", true);

        GuiLayout.Layout layout = GuiLayout.parse("t", source(values));
        GuiLayout.Button button = layout.buttonAt(10);
        assertNotNull(button, "the button was not placed at the slot it declared");
        assertEquals("open_reports", button.action());
        assertEquals("snuffac.report", button.permission());
        assertTrue(button.glow());
    }

    @Test
    void fillerMaterialAndToggleAreSeparateKeys() {
        Map<String, Object> values = base(3);
        values.put("filler-material", "RED_STAINED_GLASS_PANE");
        values.put("filler", false);

        GuiLayout.Layout layout = GuiLayout.parse("t", source(values));
        assertEquals("RED_STAINED_GLASS_PANE", layout.filler().name(),
                "the configured filler material was lost");
        assertFalse(layout.fillerEnabled());
    }

    @Test
    void anUnknownMaterialDropsTheButtonInsteadOfCrashing() {
        Map<String, Object> values = base(3);
        values.put("items.10.material", "NOT_A_REAL_BLOCK");
        values.put("items.10.action", "close");
        GuiLayout.Layout layout = GuiLayout.parse("t", source(values));
        assertNull(layout.buttonAt(10),
                "a bad material should be dropped, not placed as something unrecognisable");
    }

    @Test
    void aDisabledButtonIsStillParsedButMarked() {
        Map<String, Object> values = base(3);
        values.put("items.10.material", "STONE");
        values.put("items.10.action", "close");
        values.put("items.10.enabled", false);

        GuiLayout.Layout layout = GuiLayout.parse("t", source(values));
        GuiLayout.Button button = layout.buttonAt(10);
        assertNotNull(button);
        assertFalse(button.enabled(), "a disabled button should never be clickable");
    }

    @Test
    void aSubmitButtonAtSlot49NeedsSixRows() {
        Map<String, Object> tooSmall = base(3);
        tooSmall.put("items.49.material", "PAPER");
        tooSmall.put("items.49.action", "report_submit");
        assertEquals(27, GuiLayout.parse("t", source(tooSmall)).size());

        Map<String, Object> bigEnough = base(6);
        bigEnough.put("items.49.material", "PAPER");
        bigEnough.put("items.49.action", "report_submit");
        GuiLayout.Layout layout = GuiLayout.parse("t", source(bigEnough));
        assertNotNull(layout.buttonAt(49), "a six row menu can hold slot 49, so it must land there");
        assertEquals(54, layout.size());
    }

    @Test
    void loreIsReadAsAList() {
        Map<String, Object> values = base(3);
        values.put("items.10.material", "STONE");
        values.put("items.10.action", "close");
        values.put("items.10.lore", List.of("first line", "second line"));

        GuiLayout.Layout layout = GuiLayout.parse("t", source(values));
        assertEquals(List.of("first line", "second line"), layout.buttonAt(10).lore());
    }

    @Test
    void aMissingFileFallsBackToAnEmptyLayout() {
        GuiLayout.Layout layout = GuiLayout.parse("nothing-here", source(base(3)));
        assertTrue(layout.buttons().isEmpty());
        assertEquals(27, layout.size());
    }
}
