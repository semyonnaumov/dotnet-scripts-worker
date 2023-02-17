package com.naumov.dotnetscriptsworker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class TestFileUtil {

    public static void clearDirectoryIfExists(Path path) throws IOException {
        if (!Files.exists(path) || !Files.isDirectory(path)) return;
        try (Stream<Path> pathStream = Files.walk(path)) {
            List<Path> pathsToDelete = pathStream.sorted(Comparator.reverseOrder()).toList();
            for (Path pathToDelete : pathsToDelete) {
                if (!path.equals(pathToDelete)) {
                    Files.deleteIfExists(pathToDelete);
                }
            }
        }
    }

    public static boolean isDirectoryEmpty(Path path) throws IOException {
        if (!Files.exists(path) || !Files.isDirectory(path)) return false;
        try (Stream<Path> entries = Files.list(path)) {
            return entries.findFirst().isEmpty();
        }
    }

    public static boolean isDirectoryExists(Path path) {
        return Files.exists(path) & Files.isDirectory(path);
    }
}
