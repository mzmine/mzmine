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

package io.github.mzmine.util.files;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.ToLongFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

/**
 * Serializes directory configuration, selection, and the complete creation operation, including
 * memory mapping. Getters read an immutable snapshot without taking the creation lock. Existing
 * files keep using their original drive after a directory switch.
 */
final class TempDirectoryManager {

  private static final Logger logger = Logger.getLogger(TempDirectoryManager.class.getName());
  /**
   * Usable space a directory needs beyond the requested size. Sparse memory mappings only allocate
   * disk space on write, so a drive is left before the open mappings fill it up.
   */
  static final long MIN_USABLE_SPACE = 10_000_000_000L;

  private final ToLongFunction<File> usableSpace;
  private volatile DirectoryState state;

  TempDirectoryManager() {
    this(File::getUsableSpace);
  }

  TempDirectoryManager(ToLongFunction<File> usableSpace) {
    this.usableSpace = usableSpace;
    final File defaultDirectory = defaultDirectory();
    state = new DirectoryState(List.of(defaultDirectory), defaultDirectory);
  }

  private static File defaultDirectory() {
    return new File(System.getProperty("java.io.tmpdir")).getAbsoluteFile();
  }

  /**
   * Makes sure the .mzmine folder is never used
   */
  private static boolean isAllowed(File directory) {
    if (FileAndPathUtil.isInMzmineDir(directory)) {
      logger.warning(
          "The mzmine settings folder cannot be used as temporary directory: " + directory);
      return false;
    }
    return true;
  }

  File getDirectory() {
    return state.currentDirectory();
  }

  File[] getDirectories() {
    return state.directories().toArray(File[]::new);
  }

  /**
   * Keeps inaccessible directories in the list, so they are retried on failover, e.g., after a
   * drive was reconnected. The previous configuration is kept if no directory is accessible. The
   * .mzmine dir and its subdirectories are removed, because temp file cleanup deletes files there.
   * @return the actually set directories
   */
  synchronized File @NotNull [] setDirectories(File... paths) {
    List<File> directories = Arrays.stream(paths).filter(Objects::nonNull)
        .map(File::getAbsoluteFile).distinct().filter(TempDirectoryManager::isAllowed).toList();
    if (directories.isEmpty()) {
      directories = Stream.of(defaultDirectory()).filter(TempDirectoryManager::isAllowed).toList();
    }
    for (File directory : directories) {
      try {
        Files.createDirectories(directory.toPath());
      } catch (IOException e) {
        logger.log(Level.WARNING, "Cannot create temporary directory " + directory, e);
      }
    }
    final File first = directories.stream().filter(File::isDirectory).findFirst().orElse(null);
    if (first == null) {
      logger.warning(
          "No configured temporary directory is accessible. Keeping " + state.directories());
      return state.directories.toArray(File[]::new);
    }
    if (!state.directories().equals(directories)) {
      state = new DirectoryState(directories, first);
    }
    return state.directories.toArray(File[]::new);
  }

  /**
   * Tries the current directory first and then all directories in priority order, so reclaimed
   * space is reused. A directory needs {@link #MIN_USABLE_SPACE} beyond the required space. If no
   * directory has that, the one with the most usable space is used if it fits the required space.
   * The operation must close any channels it opened before throwing.
   */
  synchronized <T> T create(long requiredSpace, TempFileOperation<T> operation) throws IOException {
    final DirectoryState snapshot = state;
    final IOException failure = new IOException(
        "Cannot create a temporary file in any configured directory: %s. Directories need %d GB of usable space beyond the requested size.".formatted(
            snapshot.directories(), MIN_USABLE_SPACE / 1_000_000_000L));

    // keeps priority order so ties choose the earlier directory
    final Map<File, Long> lowSpace = new LinkedHashMap<>();
    final List<File> candidates = Stream.concat(Stream.of(snapshot.currentDirectory()),
        snapshot.directories().stream()).distinct().toList();
    for (File directory : candidates) {
      try {
        // usually exists, check first to avoid the exception thrown internally by createDirectories
        final Path path = directory.toPath();
        if (!Files.isDirectory(path)) {
          Files.createDirectories(path);
        }
        // usable space is 0 for missing directories, so check after creation
        final long space = usableSpace.applyAsLong(directory);
        if (space < requiredSpace + MIN_USABLE_SPACE) {
          lowSpace.put(directory, space);
          throw new IOException("Only %d bytes usable in %s".formatted(space, directory));
        }
        return createIn(snapshot, directory, operation);
      } catch (IOException e) {
        failure.addSuppressed(e);
      }
    }

    // decision: low space on all drives should not stop processing as long as the file fits
    final File fallback = lowSpace.entrySet().stream()
        .filter(entry -> entry.getValue() >= requiredSpace).max(Entry.comparingByValue())
        .map(Entry::getKey).orElse(null);
    if (fallback != null) {
      logger.warning("Low disk space in all temporary directories. Using " + fallback);
      try {
        return createIn(snapshot, fallback, operation);
      } catch (IOException e) {
        failure.addSuppressed(e);
      }
    }
    throw failure;
  }

  private <T> T createIn(DirectoryState snapshot, File directory, TempFileOperation<T> operation)
      throws IOException {
    final T result = operation.create(directory.toPath());
    if (!directory.equals(snapshot.currentDirectory())) {
      state = new DirectoryState(snapshot.directories(), directory);
      logger.info("Using temporary directory " + directory);
    }
    return result;
  }

  private record DirectoryState(List<File> directories, File currentDirectory) {

  }

  @FunctionalInterface
  interface TempFileOperation<T> {

    T create(Path directory) throws IOException;
  }
}
