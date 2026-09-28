package dev.snuffac.core.log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;

public final class FileViolationLogger implements AutoCloseable {

    private static final int MAX_LINES_IN_MEMORY = 512;

    private final Path directory;
    private final Deque<String> recent = new ArrayDeque<>();
    private final int retentionDays;

    private BufferedWriter writer;
    private LocalDate openDate;

    public FileViolationLogger(Path directory, int retentionDays) {
        this.directory = directory;
        this.retentionDays = retentionDays;
    }

    public synchronized void log(String line) {
        recent.addLast(line);
        while (recent.size() > MAX_LINES_IN_MEMORY) {
            recent.removeFirst();
        }
        try {
            ensureWriter();
            if (writer != null) {
                writer.write(line);
                writer.newLine();
                writer.flush();
            }
        } catch (IOException | UncheckedIOException exception) {
            writer = null;
        }
    }

    public synchronized Deque<String> recent() {
        return new ArrayDeque<>(recent);
    }

    private void ensureWriter() throws IOException {
        LocalDate today = LocalDate.now();
        if (writer != null && today.equals(openDate)) {
            return;
        }
        closeQuietly();
        Files.createDirectories(directory);
        purgeOldFiles();
        Path file = directory.resolve("violations-" + today + ".log");
        writer = Files.newBufferedWriter(
                file,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND);
        openDate = today;
    }

    private void purgeOldFiles() throws IOException {
        if (retentionDays <= 0 || !Files.isDirectory(directory)) {
            return;
        }
        LocalDate cutoff = LocalDate.now().minusDays(retentionDays);
        try (var stream = Files.list(directory)) {
            stream.filter(path -> path.getFileName().toString().startsWith("violations-"))
                    .filter(path -> path.getFileName().toString().endsWith(".log"))
                    .forEach(path -> {
                        String name = path.getFileName().toString();
                        String datePart = name.substring("violations-".length(), name.length() - ".log".length());
                        try {
                            if (LocalDate.parse(datePart).isBefore(cutoff)) {
                                Files.deleteIfExists(path);
                            }
                        } catch (Exception ignored) {
                        }
                    });
        }
    }

    private void closeQuietly() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException ignored) {
            }
            writer = null;
        }
    }

    @Override
    public synchronized void close() {
        closeQuietly();
    }
}
