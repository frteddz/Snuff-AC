package dev.snuffac.paper.gui;

import dev.snuffac.core.config.ConfigSource;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public final class GuiLayout {

    public static final String FOLDER = dev.snuffac.core.gui.GuiRules.FOLDER;

    public record Button(
            int slot,
            Material material,
            String name,
            List<String> lore,
            String action,
            boolean enabled,
            String permission,
            int amount,
            boolean glow) {
    }

    public record Layout(
            String id,
            String title,
            int rows,
            Material filler,
            List<Button> buttons,
            boolean fillerEnabled) {

        public int size() {
            return rows * 9;
        }

        public Button buttonAt(int slot) {
            for (Button button : buttons) {
                if (button.slot() == slot) {
                    return button;
                }
            }
            return null;
        }
    }

    public static final Layout EMPTY =
            new Layout("empty", "Snuff AC", 3, Material.BLACK_STAINED_GLASS_PANE, List.of(), true);

    private static final Map<String, Layout> CACHE = new LinkedHashMap<>();

    private GuiLayout() {
    }

    public static File folder(Plugin plugin) {
        return new File(plugin.getDataFolder(), FOLDER);
    }

    public static void reload(Plugin plugin) {
        CACHE.clear();
    }

    public static Layout load(Plugin plugin, String id) {
        Layout cached = CACHE.get(id);
        if (cached != null) {
            return cached;
        }
        File file = new File(folder(plugin), id + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        if (file.isFile()) {
            try {
                yaml.load(file);
            } catch (java.io.IOException
                    | org.bukkit.configuration.InvalidConfigurationException
                    | RuntimeException exception) {
                plugin.getLogger().warning(
                        "GUI file " + id + ".yml is malformed, using built-in defaults: " + exception);
                return EMPTY;
            }
        }
        ConfigSource source = new dev.snuffac.paper.YamlConfigSource(yaml);
        Layout layout = parse(id, source);
        CACHE.put(id, layout);
        return layout;
    }

    static Layout parse(String id, ConfigSource source) {
        int rows = clampRows(source.getInt("rows", 3));
        int size = rows * 9;
        String title = source.getString("title", "Snuff AC");
        Material filler = material(source.getString("filler", "BLACK_STAINED_GLASS_PANE"),
                Material.BLACK_STAINED_GLASS_PANE);
        boolean fillerEnabled = source.getBoolean("filler", true);

        List<Button> buttons = new ArrayList<>();
        for (int slot = 0; slot < size; slot++) {
            String base = "items." + slot;
            if (!source.contains(base + ".material") && !source.contains(base)) {
                continue;
            }
            Material material = material(source.getString(base + ".material", ""), null);
            if (material == null) {
                continue;
            }
            int configuredSlot = source.getInt(base + ".slot", slot);
            int safeSlot = clampSlot(configuredSlot, size);
            String action = sanitizeAction(source.getString(base + ".action", ""));
            String permission = source.getString(base + ".permission", "");
            int amount = clampAmount(source.getInt(base + ".amount", 1));
            boolean glow = source.getBoolean(base + ".glow", false);
            boolean enabled = source.getBoolean(base + ".enabled", true);

            List<String> lore = new ArrayList<>();
            for (String line : source.getStringList(base + ".lore", List.of())) {
                lore.add(line == null ? "" : line);
            }

            buttons.add(new Button(
                    safeSlot,
                    material,
                    source.getString(base + ".name", ""),
                    List.copyOf(lore),
                    action,
                    enabled,
                    permission,
                    amount,
                    glow));
        }
        return new Layout(id, title, rows, filler, List.copyOf(buttons), fillerEnabled);
    }

    public static int clampRows(int rows) {
        return dev.snuffac.core.gui.GuiRules.clampRows(rows);
    }

    static int clampSlot(int slot, int size) {
        return dev.snuffac.core.gui.GuiRules.clampSlot(slot, size);
    }

    static int clampAmount(int amount) {
        return dev.snuffac.core.gui.GuiRules.clampAmount(amount);
    }

    public static Material material(String name, Material fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        Material material = Material.matchMaterial(name.trim().toUpperCase(Locale.ROOT));
        if (material == null || !material.isItem()) {
            return fallback;
        }
        return material;
    }

    public static final Set<String> ALLOWED_ACTIONS = dev.snuffac.core.gui.GuiRules.ALLOWED_ACTIONS;

    static String sanitizeAction(String action) {
        return dev.snuffac.core.gui.GuiRules.sanitizeAction(action);
    }

    public static boolean isAllowedAction(String action) {
        return dev.snuffac.core.gui.GuiRules.isAllowedAction(action);
    }

    public static boolean write(Plugin plugin, String id, YamlConfiguration yaml) throws IOException {
        File directory = folder(plugin);
        if (!directory.isDirectory() && !directory.mkdirs()) {
            return false;
        }
        yaml.save(new File(directory, id + ".yml"));
        return true;
    }
}
