package dev.snuffac.paper.report;

import dev.snuffac.core.report.ReportCategory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * Report categories live in their own file so a server owner can add one without
 * touching the plugin. The built in list is the fallback, not the source.
 */
public final class ReportOptions {

    public static final String FILE = "report-options.yml";
    private static final Pattern ID = Pattern.compile("[a-z0-9_]{1,24}");

    private ReportOptions() {
    }

    public record Option(String id, String label, String description, Material icon) {
    }

    private static volatile List<Option> cache;
    private static volatile long cacheStamp;

    public static File file(Plugin plugin) {
        return new File(new File(plugin.getDataFolder(), dev.snuffac.core.gui.GuiRules.FOLDER), FILE);
    }

    public static void writeDefaults(Plugin plugin) {
        File target = file(plugin);
        if (target.isFile()) {
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("options.cheating.label", "Cheating");
        yaml.set("options.cheating.description", "Use of a client that gives an unfair advantage");
        yaml.set("options.cheating.icon", "DIAMOND_SWORD");
        yaml.set("options.cheating.enabled", true);
        yaml.set("options.exploiting.label", "Exploiting");
        yaml.set("options.exploiting.description", "Duplication, crashes, or abusing a flaw");
        yaml.set("options.exploiting.icon", "TNT");
        yaml.set("options.exploiting.enabled", true);
        yaml.set("options.language.label", "Explicit Language");
        yaml.set("options.language.description", "Sexual or graphic language");
        yaml.set("options.language.icon", "PAPER");
        yaml.set("options.language.enabled", true);
        yaml.set("options.offensive.label", "Offensive Behaviour");
        yaml.set("options.offensive.description", "Harassment, toxicity, or slurs");
        yaml.set("options.offensive.icon", "REDSTONE");
        yaml.set("options.offensive.enabled", true);
        yaml.set("options.griefing.label", "Griefing");
        yaml.set("options.griefing.description", "Destroying or stealing other people's work");
        yaml.set("options.griefing.icon", "IRON_PICKAXE");
        yaml.set("options.griefing.enabled", true);
        yaml.set("options.name.label", "Inappropriate Name");
        yaml.set("options.name.description", "A username that breaks the rules");
        yaml.set("options.name.icon", "NAME_TAG");
        yaml.set("options.name.enabled", true);
        yaml.set("options.impersonation.label", "Staff Impersonation");
        yaml.set("options.impersonation.description", "Pretending to be an administrator");
        yaml.set("options.impersonation.icon", "PLAYER_HEAD");
        yaml.set("options.impersonation.enabled", true);
        try {
            File parent = target.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            yaml.save(target);
        } catch (IOException failure) {
            plugin.getLogger().warning("Could not write " + FILE + ": " + failure);
        }
    }

    public static List<Option> load(Plugin plugin) {
        File source = file(plugin);
        long stamp = source.isFile() ? source.lastModified() : 0L;
        List<Option> cached = cache;
        if (cached != null && cacheStamp == stamp) {
            return cached;
        }
        List<Option> loaded = read(plugin);
        cache = loaded;
        cacheStamp = stamp;
        return loaded;
    }

    public static void invalidate() {
        cache = null;
        cacheStamp = 0L;
    }

    private static List<Option> read(Plugin plugin) {
        writeDefaults(plugin);
        YamlConfiguration yaml = new YamlConfiguration();
        File source = file(plugin);
        if (source.isFile()) {
            try {
                yaml.load(source);
            } catch (IOException | org.bukkit.configuration.InvalidConfigurationException
                    | RuntimeException failure) {
                plugin.getLogger().warning(FILE + " is malformed, using the built in categories: "
                        + failure.getMessage());
                return builtIn();
            }
        }
        Map<String, Option> options = new LinkedHashMap<>();
        org.bukkit.configuration.ConfigurationSection section = yaml.getConfigurationSection("options");
        if (section == null) {
            return builtIn();
        }
        for (String key : section.getKeys(false)) {
            String id = key.toLowerCase(Locale.ROOT);
            if (!ID.matcher(id).matches() || !yaml.getBoolean("options." + key + ".enabled", true)) {
                continue;
            }
            String label = yaml.getString("options." + key + ".label", key);
            String description = yaml.getString("options." + key + ".description", "");
            options.put(id, new Option(id, label, description,
                    icon(yaml.getString("options." + key + ".icon", "PAPER"))));
        }
        return options.isEmpty() ? builtIn() : new ArrayList<>(options.values());
    }

    private static List<Option> builtIn() {
        List<Option> options = new ArrayList<>();
        for (ReportCategory.Definition definition : ReportCategory.all()) {
            options.add(new Option(definition.id(), definition.label(), definition.description(),
                    icon(iconNameFor(definition.id()))));
        }
        return options;
    }

    private static String iconNameFor(String id) {
        return switch (id) {
            case ReportCategory.CHEATING -> "DIAMOND_SWORD";
            case ReportCategory.EXPLOITING -> "TNT";
            case ReportCategory.LANGUAGE -> "PAPER";
            case ReportCategory.OFFENSIVE -> "REDSTONE";
            case ReportCategory.GRIEFING -> "IRON_PICKAXE";
            case ReportCategory.NAME -> "NAME_TAG";
            case ReportCategory.IMPERSONATION -> "PLAYER_HEAD";
            default -> "PAPER";
        };
    }

    private static Material icon(String name) {
        Material material = Material.matchMaterial(name == null ? "" : name.trim().toUpperCase(Locale.ROOT));
        return material == null ? Material.PAPER : material;
    }
}
