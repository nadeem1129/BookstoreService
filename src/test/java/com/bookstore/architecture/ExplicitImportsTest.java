package com.bookstore.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplicitImportsTest {

    private static final Pattern NON_STATIC_WILDCARD_IMPORT =
            Pattern.compile("^\\s*import\\s+(?!static\\b)[\\w.]+\\.\\*\\s*;");

    @Test
    void productionSourcesUseExplicitImports() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java");
        try (Stream<Path> sourceFiles = Files.walk(sourceRoot)) {
            List<String> wildcardImports = sourceFiles
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> {
                        try {
                            return Files.readAllLines(path).stream()
                                    .filter(line -> NON_STATIC_WILDCARD_IMPORT.matcher(line).matches())
                                    .map(line -> sourceRoot.relativize(path) + ": " + line.trim());
                        } catch (IOException ex) {
                            throw new java.io.UncheckedIOException(ex);
                        }
                    })
                    .toList();

            assertTrue(wildcardImports.isEmpty(),
                    () -> "Replace wildcard imports with explicit imports:\n" + String.join("\n", wildcardImports));
        }
    }
}
