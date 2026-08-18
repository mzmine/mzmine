/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

import io.github.mzmine.util.files.ExtensionFilters;
import io.github.mzmine.util.io.WriterOptions;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javafx.stage.FileChooser.ExtensionFilter;
import org.junit.jupiter.api.Assertions;

/**
 * Utility to replace a literal string in all files of one type inside a directory. Adjust the
 * constants below and run manually. Disabled by default as it modifies local user files.
 */
public class ReplaceStringInFilesMain {

  private static final Logger logger = Logger.getLogger(ReplaceStringInFilesMain.class.getName());
  private static final boolean CREATE_BACKUP = false;
  // decision: dry run by default so a first run only reports what would change
  private static final boolean DRY_RUN = false;

  // stream all files in directory and subdirectories
  private static final boolean INCLUDE_SUBDIRECTORIES = true;
  private static final String DIRECTORY = "D:\\Data\\batch";

  // all mzmine files
  private static final List<ExtensionFilter> EXTENSIONS = List.of(ExtensionFilters.MZ_WIZARD,
      ExtensionFilters.MZ_BATCH, ExtensionFilters.MZ_PRESETS);

  private static final String REPLACE_TARGET = "D:\\OneDrive - mzio GmbH\\mzio\\Example data";
  private static final String REPLACE_TO = "D:\\OneDrive - mzio GmbH\\Example data - Documents";

  // also replace paths with \\ in string like in xml within json in mzpresets
  private static final String REPLACE_TARGET2 = REPLACE_TARGET.replace("\\", "\\\\");
  private static final String REPLACE_TO2 = REPLACE_TO.replace("\\", "\\\\");

  void main() throws IOException {
    final Path directory = Path.of(DIRECTORY);
    Assertions.assertTrue(Files.isDirectory(directory), "Directory not found: " + directory);
    Assertions.assertFalse(REPLACE_TARGET.isEmpty(), "Replace target must not be empty");

    // clean names like mzmwizard, mzbatch, ... matched case-insensitively against the file suffix
    final List<String> extensions = ExtensionFilters.getAllCleanExtensionNames(EXTENSIONS)
        .map(ext -> "." + ext.toLowerCase()).distinct().toList();
    Assertions.assertFalse(extensions.isEmpty(), "No extensions defined");

    final int maxDepth = INCLUDE_SUBDIRECTORIES ? Integer.MAX_VALUE : 1;
    final List<Path> files;
    try (Stream<Path> stream = Files.walk(directory, maxDepth)) {
      files = stream.filter(Files::isRegularFile).filter(p -> {
        final String name = p.getFileName().toString().toLowerCase();
        return extensions.stream().anyMatch(name::endsWith);
      }).sorted().toList();
    }

    logger.info("""
        Scanning %d %s files in %s (subdirectories: %s)
          from: %s
            to: %s
          mode: %s""".formatted(files.size(), extensions, directory, INCLUDE_SUBDIRECTORIES,
        REPLACE_TARGET, REPLACE_TO, DRY_RUN ? "DRY RUN" : "APPLY"));

    int changed = 0;
    for (final Path file : files) {
      final String content = Files.readString(file, StandardCharsets.UTF_8);
      final String replaced = content.replace(REPLACE_TARGET, REPLACE_TO)
          .replace(REPLACE_TARGET2, REPLACE_TO2);
      if (replaced.equals(content)) {
        continue;
      }
      changed++;

      final int hits = countOccurrences(content);
      // relative path so files in subdirectories stay distinguishable
      logger.info("%s %s (%d hits)".formatted(DRY_RUN ? "would change" : "changed",
          directory.relativize(file), hits));

      if (DRY_RUN) {
        continue;
      }
      if (CREATE_BACKUP) {
        Files.copy(file, file.resolveSibling(file.getFileName() + ".bak"),
            StandardCopyOption.REPLACE_EXISTING);
      }
      Files.writeString(file, replaced, StandardCharsets.UTF_8,
          WriterOptions.REPLACE.toOpenOption());
    }

    logger.log(Level.INFO, "Done. %d of %d files affected.".formatted(changed, files.size()));
  }

  private static int countOccurrences(final String content) {
    int count = 0;

    final List<String> replaceTarget = List.of(REPLACE_TARGET, REPLACE_TARGET2);
    for (String search : replaceTarget) {
      int index = content.indexOf(search);
      while (index >= 0) {
        count++;
        index = content.indexOf(search, index + search.length());
      }
    }
    return count;
  }
}
