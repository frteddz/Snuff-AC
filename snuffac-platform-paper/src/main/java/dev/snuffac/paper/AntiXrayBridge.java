package dev.snuffac.paper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.World;

public final class AntiXrayBridge {

    public static final String ANTIXRAY_FIELD = "paperConfig";
    public static final String ANTICHEAT_FIELD = "anticheat";
    public static final String ANTIXRAY_SUBFIELD = "antiXray";
    public static final String ENGINE_MODE_FIELD = "engineMode";
    public static final String MAX_BLOCK_HEIGHT_FIELD = "maxBlockHeight";
    public static final String HIDDEN_BLOCKS_FIELD = "hiddenBlocks";
    public static final String REPLACEMENT_BLOCKS_FIELD = "replacementBlocks";
    public static final String ENABLED_FIELD = "enabled";

    private static final List<String> ATTEMPTS = new ArrayList<>();

    private AntiXrayBridge() {
    }

    public static void resetAttempts() {
        ATTEMPTS.clear();
    }

    public static String describeAttempts() {
        return ATTEMPTS.isEmpty() ? "no attempt was recorded" : String.join(" | ", ATTEMPTS);
    }

    public static Object levelHandle(World world) {
        try {
            return world.getClass().getMethod("getHandle").invoke(world);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            ATTEMPTS.add("CraftWorld.getHandle -> " + describe(failure));
            return null;
        }
    }

    public static Object worldConfiguration(World world) {
        Object level = levelHandle(world);
        if (level == null) {
            return null;
        }
        Object config = readField(level, ANTIXRAY_FIELD);
        if (config == null) {
            ATTEMPTS.add("Level." + ANTIXRAY_FIELD + " absent on " + level.getClass().getName());
        }
        return config;
    }

    public static Object antiXrayConfiguration(World world) {
        Object worldConfig = worldConfiguration(world);
        if (worldConfig == null) {
            return null;
        }
        Object anticheat = readField(worldConfig, ANTICHEAT_FIELD);
        if (anticheat == null) {
            return null;
        }
        Object antiXray = readField(anticheat, ANTIXRAY_SUBFIELD);
        if (antiXray == null) {
            ATTEMPTS.add("Anticheat." + ANTIXRAY_SUBFIELD + " was null");
        }
        return antiXray;
    }

    public static Object readField(Object holder, String name) {
        if (holder == null) {
            return null;
        }
        for (Class<?> type = holder.getClass(); type != null && type != Object.class;
                type = type.getSuperclass()) {
            try {
                java.lang.reflect.Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(holder);
            } catch (NoSuchFieldException ignored) {
            } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
                ATTEMPTS.add("read " + type.getSimpleName() + "." + name + " -> " + describe(failure));
                return null;
            }
        }
        ATTEMPTS.add("no field " + name + " on " + holder.getClass().getName());
        return null;
    }

    public static boolean writeField(Object holder, String name, Object value) {
        if (holder == null) {
            return false;
        }
        for (Class<?> type = holder.getClass(); type != null && type != Object.class;
                type = type.getSuperclass()) {
            try {
                java.lang.reflect.Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(holder, value);
                return true;
            } catch (NoSuchFieldException ignored) {
            } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
                ATTEMPTS.add("write " + type.getSimpleName() + "." + name + " -> " + describe(failure));
                return false;
            }
        }
        ATTEMPTS.add("no writable field " + name + " on " + holder.getClass().getName());
        return false;
    }

    public static Object engineModeValue(Object antiXray, boolean obfuscate) {
        Object current = readField(antiXray, ENGINE_MODE_FIELD);
        if (current == null) {
            return null;
        }
        Class<?> type = current.getClass();
        String[] preferred = obfuscate
                ? new String[] {"OBFUSCATE", "OBFUSCATE_LAYER", "HIDE"}
                : new String[] {"HIDE", "OBFUSCATE", "OBFUSCATE_LAYER"};
        for (String name : preferred) {
            try {
                return type.getMethod("valueOf", String.class).invoke(null, name);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        ATTEMPTS.add("no usable EngineMode on " + type.getName() + ", keeping " + current);
        return current;
    }

    public static List<Object> minecraftBlocks(List<String> materialNames) {
        List<Object> blocks = new ArrayList<>(materialNames.size());
        List<String> unresolved = new ArrayList<>();
        for (String name : materialNames) {
            Object block = minecraftBlock(name);
            if (block == null) {
                unresolved.add(name);
            } else {
                blocks.add(block);
            }
        }
        UNRESOLVED = List.copyOf(unresolved);
        if (!unresolved.isEmpty()) {
            ATTEMPTS.add("unresolved block names: " + unresolved);
        }
        return blocks;
    }

    private static List<String> UNRESOLVED = List.of();

    public static List<String> unresolvedBlocks() {
        return UNRESOLVED;
    }

    public static String registryPath(String bukkitMaterialName) {
        org.bukkit.Material material = org.bukkit.Material.matchMaterial(bukkitMaterialName);
        if (material == null || !material.isBlock()) {
            return null;
        }
        try {
            return material.getKey().getKey();
        } catch (RuntimeException | LinkageError failure) {
            return null;
        }
    }

    public static Object minecraftBlock(String bukkitMaterialName) {
        try {
            String path = registryPath(bukkitMaterialName);
            if (path == null) {
                ATTEMPTS.add("no Bukkit material named " + bukkitMaterialName);
                return null;
            }
            Class<?> keyClass = identifierClass();
            Method parse = keyClass.getMethod("parse", String.class);
            Object key = parse.invoke(null, "minecraft:" + path);

            Class<?> registries = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
            Object blockRegistry = registries.getField("BLOCK").get(null);

            Method getOptional = null;
            Class<?> optionalClass = null;
            for (Method method : blockRegistry.getClass().getMethods()) {
                if (method.getName().equals("getOptional") && method.getParameterCount() == 1
                        && method.getParameterTypes()[0] == keyClass) {
                    getOptional = method;
                    break;
                }
            }
            if (getOptional == null) {
                Class<?> registryClass = Class.forName("net.minecraft.core.Registry");
                for (Method method : registryClass.getMethods()) {
                    if (method.getName().equals("getOptional") && method.getParameterCount() == 1
                            && method.getParameterTypes()[0] == keyClass) {
                        getOptional = method;
                        break;
                    }
                }
            }
            if (getOptional == null) {
                ATTEMPTS.add("no getOptional(identifier) on " + blockRegistry.getClass().getName());
                return null;
            }
            Object optional = getOptional.invoke(blockRegistry, key);
            if (optional == null) {
                return null;
            }
            optionalClass = Class.forName("java.util.Optional");
            Object value = optionalClass.getMethod("orElse", Object.class).invoke(optional, (Object) null);
            return value;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            ATTEMPTS.add("block lookup for " + bukkitMaterialName + " -> " + describe(failure));
            return null;
        }
    }

    private static Class<?> identifierClass() throws ClassNotFoundException {
        try {
            return Class.forName("net.minecraft.resources.ResourceLocation");
        } catch (ClassNotFoundException missing) {
            return Class.forName("net.minecraft.resources.Identifier");
        }
    }

    static String describe(Throwable throwable) {
        String text = throwable.getClass().getName();
        if (throwable.getMessage() != null && !throwable.getMessage().isBlank()) {
            text = text + ": " + throwable.getMessage();
        }
        return text;
    }
}
