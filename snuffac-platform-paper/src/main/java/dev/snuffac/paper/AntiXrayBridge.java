package dev.snuffac.paper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.World;

public final class AntiXrayBridge {

    private static final List<String> ATTEMPTS = new ArrayList<>();

    private AntiXrayBridge() {
    }

    public static String describeAttempts() {
        return ATTEMPTS.isEmpty() ? "no strategy was attempted" : String.join(" | ", ATTEMPTS);
    }

    public static Object configurationFor(World world) {
        ATTEMPTS.clear();
        for (Strategy strategy : STRATEGIES) {
            try {
                Object result = strategy.resolve(world);
                if (result != null) {
                    return result;
                }
                ATTEMPTS.add(strategy.name() + " returned null");
            } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
                ATTEMPTS.add(strategy.name() + " -> " + failure.getClass().getSimpleName()
                        + (failure.getMessage() == null ? "" : ": " + failure.getMessage()));
            }
        }
        return null;
    }

    public static Class<?> engineModeClass(Object configuration) {
        for (Class<?> candidate : configuration.getClass().getInterfaces()) {
            if (candidate.getSimpleName().contains("AntiXrayConfiguration")) {
                return candidate;
            }
        }
        return configuration.getClass();
    }

    public static Object engineModeValue(Class<?> engineMode, ObfuscationPolicy policy) {
        List<String> preferred = policy == ObfuscationPolicy.OFF
                ? List.of("NONE", "OFF")
                : List.of("OBFUSCATE", "ALL_ORES", "DIMINISHING");
        for (String name : preferred) {
            try {
                return engineMode.getMethod("valueOf", String.class).invoke(null, name);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        throw new IllegalStateException("no usable engine mode constant on " + engineMode.getName()
                + ", tried " + preferred);
    }

    public enum ObfuscationPolicy {
        OFF,
        HIDDEN_ORES,
        HIDDEN_ORES_AND_DEEPSLATE
    }

    private interface Strategy {

        String name();

        Object resolve(World world) throws ReflectiveOperationException;
    }

    private static final List<Strategy> STRATEGIES = List.of(
            new Strategy() {
                @Override
                public String name() {
                    return "world.getUnsafe().getWorldConfiguration().getAntiXrayConfiguration()";
                }

                @Override
                public Object resolve(World world) throws ReflectiveOperationException {
                    Method unsafe = world.getClass().getMethod("getUnsafe");
                    Object unsafeValue = unsafe.invoke(world);
                    Method worldConfig = findMethod(unsafeValue.getClass(),
                            "getWorldConfiguration", "getWorldConfig", "getConfig");
                    Object configValue = worldConfig.invoke(unsafeValue);
                    return invokeAntiXray(configValue);
                }
            },
            new Strategy() {
                @Override
                public String name() {
                    return "Bukkit.getServer().getWorldConfiguration().getAntiXrayConfiguration()";
                }

                @Override
                public Object resolve(World world) throws ReflectiveOperationException {
                    Method worldConfig = findMethod(Bukkit.getServer().getClass(),
                            "getWorldConfiguration", "getWorldConfig");
                    Object configValue = worldConfig.invoke(Bukkit.getServer());
                    return invokeAntiXray(configValue);
                }
            },
            new Strategy() {
                @Override
                public String name() {
                    return "world.getAntiXrayConfiguration()";
                }

                @Override
                public Object resolve(World world) throws ReflectiveOperationException {
                    return invokeAntiXray(world);
                }
            },
            new Strategy() {
                @Override
                public String name() {
                    return "CraftServer.getPaperConfig().getAntiXray()";
                }

                @Override
                public Object resolve(World world) throws ReflectiveOperationException {
                    Object paperConfig = findMethod(Bukkit.getServer().getClass(),
                            "getPaperConfig", "getConfig").invoke(Bukkit.getServer());
                    return invokeAntiXray(paperConfig);
                }
            });

    private static Object invokeAntiXray(Object holder) throws ReflectiveOperationException {
        if (holder == null) {
            return null;
        }
        for (Method method : holder.getClass().getMethods()) {
            if (method.getParameterCount() == 0
                    && method.getName().toLowerCase(java.util.Locale.ROOT).contains("antixray")) {
                Object value = method.invoke(holder);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    private static Method findMethod(Class<?> type, String... names)
            throws NoSuchMethodException {
        List<Class<?>> hierarchy = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class;
                current = current.getSuperclass()) {
            hierarchy.add(current);
        }
        for (String name : names) {
            for (Class<?> current : hierarchy) {
                try {
                    Method method = current.getDeclaredMethod(name);
                    method.setAccessible(true);
                    return method;
                } catch (NoSuchMethodException ignored) {
                }
            }
            for (Class<?> current : hierarchy) {
                for (Method method : current.getDeclaredMethods()) {
                    if (method.getName().equals(name) && method.getParameterCount() == 0) {
                        method.setAccessible(true);
                        return method;
                    }
                }
            }
        }
        throw new NoSuchMethodException(
                "none of " + Arrays.toString(names) + " exists on " + type.getName());
    }
}
