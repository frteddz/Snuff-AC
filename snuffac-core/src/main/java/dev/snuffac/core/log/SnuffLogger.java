package dev.snuffac.core.log;

public interface SnuffLogger {

    void info(String message);

    void warn(String message);

    void severe(String message);

    void debug(String message);

    void violation(String message);
}
