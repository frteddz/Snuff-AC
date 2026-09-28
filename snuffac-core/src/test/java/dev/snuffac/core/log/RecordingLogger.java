package dev.snuffac.core.log;

import java.util.ArrayList;
import java.util.List;

public final class RecordingLogger implements SnuffLogger {

    private final List<String> info = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<String> severe = new ArrayList<>();
    private final List<String> debug = new ArrayList<>();
    private final List<String> violations = new ArrayList<>();

    @Override
    public void info(String message) {
        info.add(message);
    }

    @Override
    public void warn(String message) {
        warnings.add(message);
    }

    @Override
    public void severe(String message) {
        severe.add(message);
    }

    @Override
    public void debug(String message) {
        debug.add(message);
    }

    @Override
    public void violation(String message) {
        violations.add(message);
    }

    public List<String> infoMessages() {
        return List.copyOf(info);
    }

    public List<String> warnings() {
        return List.copyOf(warnings);
    }

    public List<String> severeMessages() {
        return List.copyOf(severe);
    }

    public List<String> debugMessages() {
        return List.copyOf(debug);
    }

    public List<String> violationMessages() {
        return List.copyOf(violations);
    }

    public void clear() {
        info.clear();
        warnings.clear();
        severe.clear();
        debug.clear();
        violations.clear();
    }
}
