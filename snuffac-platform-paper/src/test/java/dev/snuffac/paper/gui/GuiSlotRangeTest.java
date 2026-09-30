package dev.snuffac.paper.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class GuiSlotRangeTest {

    private static final String DEFAULTS = "src/main/java/dev/snuffac/paper/GuiDefaults.java";

    private static String read() {
        try {
            return Files.readString(Path.of(DEFAULTS));
        } catch (Exception failure) {
            return "";
        }
    }

    private static Map<String, String> declaredSlotsByMenu() {
        Map<String, Integer> rows = new LinkedHashMap<>();
        Matcher menu = Pattern.compile(
                "private static YamlConfiguration (\\w+)\\(\\) \\{(.*?)\\n    \\}", Pattern.DOTALL)
                .matcher(read());
        List<String[]> out = new ArrayList<>();
        while (menu.find()) {
            String name = menu.group(1);
            String body = menu.group(2);
            Matcher rowMatcher = Pattern.compile("yaml\\.set\\(\"rows\", (\\d+)\\)").matcher(body);
            int rowCount = rowMatcher.find() ? Integer.parseInt(rowMatcher.group(1)) : 3;
            Matcher slotMatcher = Pattern.compile("item\\(yaml, (\\d+),").matcher(body);
            List<Integer> slots = new ArrayList<>();
            while (slotMatcher.find()) {
                slots.add(Integer.parseInt(slotMatcher.group(1)));
            }
            out.add(new String[] {name, String.valueOf(rowCount), slots.toString().trim()});
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (String[] entry : out) {
            result.put(entry[0] + "#rows", entry[1]);
            result.put(entry[0] + "#slots", entry[2]);
        }
        return result;
    }

    @Test
    void everyShippedMenuDeclaresARowCount() {
        Map<String, String> data = declaredSlotsByMenu();
        assertTrue(data.size() >= 12, "expected at least six menus, found " + data.size() / 2);
        assertTrue(data.containsKey("main#rows"), "the main menu must declare its rows");
        assertTrue(data.containsKey("reports#rows"), "the report picker must declare its rows");
        assertTrue(data.containsKey("reportsAdmin#rows"), "the admin report view must declare its rows");
    }

    @Test
    void everyDeclaredSlotIsInsideItsOwnInventory() {
        Map<String, String> data = declaredSlotsByMenu();
        List<String> bad = new ArrayList<>();
        for (String key : data.keySet()) {
            if (!key.endsWith("#slots")) {
                continue;
            }
            String menu = key.replace("#slots", "");
            int rows = Integer.parseInt(data.getOrDefault(menu + "#rows", "3"));
            int size = rows * 9;
            String slots = data.get(key);
            Matcher matcher = Pattern.compile("\\d+").matcher(slots);
            while (matcher.find()) {
                int slot = Integer.parseInt(matcher.group());
                if (slot < 0 || slot >= size) {
                    bad.add(menu + " declares slot " + slot + " but only has " + size
                            + " slots at " + rows + " rows, so the button would be moved elsewhere");
                }
            }
        }
        assertTrue(bad.isEmpty(), "out of range slots: " + bad);
    }

    @Test
    void theReportPickerIsBigEnoughForItsSubmitButton() {
        Map<String, String> data = declaredSlotsByMenu();
        int rows = Integer.parseInt(data.getOrDefault("reports#rows", "0"));
        assertTrue(rows * 9 > 49,
                "the report picker declares a submit button at slot 49, so it needs at least "
                + "six rows, it has " + rows);
    }

    @Test
    void theFillerKeyIsNotOverloaded() {
        String source = read();
        int material = source.split("filler-material", -1).length - 1;
        assertEquals(6, material, "every menu must write a filler-material key");
        assertTrue(!source.contains("yaml.set(\"filler\", \"BLACK_STAINED_GLASS_PANE\");"),
                "filler must only ever be a boolean, writing a material into it silently "
                + "disabled the material and the filler fell back to the default");
    }

}
