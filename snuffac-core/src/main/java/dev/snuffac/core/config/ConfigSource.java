package dev.snuffac.core.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public interface ConfigSource {

    Object raw(String path);

    boolean contains(String path);

    Set<String> keys(String path);

    void set(String path, Object value);

    void save();

    Map<String, Object> asMap();

    default String getString(String path, String fallback) {
        Object value = raw(path);
        return value == null ? fallback : String.valueOf(value);
    }

    default int getInt(String path, int fallback) {
        Object value = raw(path);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value).trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    default long getLong(String path, long fallback) {
        Object value = raw(path);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null) {
            try {
                return Long.parseLong(String.valueOf(value).trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    default double getDouble(String path, double fallback) {
        Object value = raw(path);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value != null) {
            try {
                return Double.parseDouble(String.valueOf(value).trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    default boolean getBoolean(String path, boolean fallback) {
        Object value = raw(path);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value != null) {
            String text = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
            if (text.equals("true") || text.equals("yes") || text.equals("on")) {
                return true;
            }
            if (text.equals("false") || text.equals("no") || text.equals("off")) {
                return false;
            }
        }
        return fallback;
    }

    default List<String> getStringList(String path, List<String> fallback) {
        Object value = raw(path);
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>(list.size());
            for (Object element : list) {
                result.add(String.valueOf(element));
            }
            return result;
        }
        return fallback == null ? Collections.emptyList() : fallback;
    }

    default List<Integer> getIntegerList(String path, List<Integer> fallback) {
        Object value = raw(path);
        if (value instanceof List<?> list) {
            List<Integer> result = new ArrayList<>(list.size());
            for (Object element : list) {
                if (element instanceof Number number) {
                    result.add(number.intValue());
                }
            }
            return result;
        }
        return fallback == null ? Collections.emptyList() : fallback;
    }

    static ConfigSource ofMap(Map<String, Object> map) {
        return new MapConfigSource(map);
    }

    final class MapConfigSource implements ConfigSource {

        private final Map<String, Object> root;

        public MapConfigSource(Map<String, Object> root) {
            this.root = new LinkedHashMap<>(root);
        }

        @Override
        public Object raw(String path) {
            if (root.containsKey(path)) {
                return root.get(path);
            }
            Map<String, Object> current = root;
            String[] segments = path.split("\\.");
            for (int i = 0; i < segments.length - 1; i++) {
                Object next = current.get(segments[i]);
                if (!(next instanceof Map)) {
                    return null;
                }
                current = castMap(next);
            }
            return current.get(segments[segments.length - 1]);
        }

        @Override
        public boolean contains(String path) {
            return raw(path) != null;
        }

        @Override
        public Set<String> keys(String path) {
            if (!path.isEmpty() && !contains(path)) {
                Set<String> direct = new java.util.LinkedHashSet<>();
                String prefix = path + ".";
                for (String key : root.keySet()) {
                    if (key.startsWith(prefix)) {
                        direct.add(key.substring(prefix.length()));
                    }
                }
                if (!direct.isEmpty()) {
                    return direct;
                }
            }
            Object value = path.isEmpty() ? root : raw(path);
            if (value instanceof Map<?, ?> map) {
                return map.keySet().stream().map(String::valueOf).collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
            }
            return Set.of();
        }

        @Override
        public void set(String path, Object value) {
            String[] segments = path.split("\\.");
            Map<String, Object> current = root;
            for (int i = 0; i < segments.length - 1; i++) {
                Object next = current.get(segments[i]);
                if (!(next instanceof Map)) {
                    next = new LinkedHashMap<String, Object>();
                    current.put(segments[i], next);
                }
                current = castMap(next);
            }
            current.put(segments[segments.length - 1], value);
        }

        @Override
        public void save() {
        }

        @Override
        public Map<String, Object> asMap() {
            return Collections.unmodifiableMap(root);
        }

        @SuppressWarnings("unchecked")
        private static Map<String, Object> castMap(Object value) {
            return (Map<String, Object>) value;
        }
    }
}
