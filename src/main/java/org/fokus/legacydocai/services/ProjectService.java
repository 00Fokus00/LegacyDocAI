package org.fokus.legacydocai.services;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

@Service
public class ProjectService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public long countFiles(String directoryPath) {
        if (directoryPath == null || directoryPath.isBlank()) return 0;
        try (Stream<Path> walk = Files.walk(Paths.get(directoryPath))) {
            return walk.filter(Files::isRegularFile).count();
        } catch (IOException | InvalidPathException e) {
            return 0;
        }
    }

    public long countJavaClasses(String directoryPath) {
        if (directoryPath == null || directoryPath.isBlank()) return 0;
        try (Stream<Path> walk = Files.walk(Paths.get(directoryPath))) {
            return walk.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .count();
        } catch (IOException | InvalidPathException e) {
            return 0;
        }
    }

    public String getCurrentTimestamp() {
        return LocalDateTime.now().format(FORMATTER);
    }

    public boolean isValidDirectory(String path) {
        if (path == null || path.isBlank()) return false;
        Path dir = Paths.get(path);
        return Files.exists(dir) && Files.isDirectory(dir) && Files.isReadable(dir);
    }
}
