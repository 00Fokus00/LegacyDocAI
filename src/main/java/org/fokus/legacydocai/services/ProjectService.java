package org.fokus.legacydocai.services;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class ProjectService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final Map<String, String> extensionToLanguageMap;

    public ProjectService(Map<String, String> extensionToLanguageMap) {
        this.extensionToLanguageMap = extensionToLanguageMap;
    }

    public long countFiles(String directoryPath) {
        if (directoryPath == null || directoryPath.isBlank()) return 0;
        try (Stream<Path> walk = Files.walk(Paths.get(directoryPath))) {
            return walk.filter(Files::isRegularFile).count();
        } catch (IOException | InvalidPathException e) {
            return 0;
        }
    }

    public long countCodeClasses(String directoryPath) {
        if (directoryPath == null || directoryPath.isBlank()) return 0;
        try (Stream<Path> walk = Files.walk(Paths.get(directoryPath))) {
            return walk.filter(Files::isRegularFile)
                    .filter(p -> {
                        String filename = p.toString().toLowerCase();
                        String extension = StringUtils.getFilenameExtension(filename);
                        return extension != null && extensionToLanguageMap.containsKey(extension);
                        })
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
