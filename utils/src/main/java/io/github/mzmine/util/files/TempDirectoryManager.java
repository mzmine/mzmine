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

import io.github.mzmine.util.collections.CollectionUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Serializes directory selection and the complete creation operation, including memory mapping.
 * Getters read an immutable snapshot without locking, {@link #getDirectory()} additionally checks
 * write access. Existing files keep using their original drive after a directory switch.
 * <p>
 * The default java temp directory is a runtime fallback while no configured directory works. It is
 * never added to the configured directories, so it is not saved to the preferences and not used for
 * rotation while a configured directory can hold a file.
 */
final class TempDirectoryManager {

  private static final Logger logger = Logger.getLogger(TempDirectoryManager.class.getName());
  /**
   * Usable space a directory needs beyond the requested size. Sparse memory mappings only allocate
   * disk space on write, so a drive is left before the open mappings fill it up.
   */
  static final long MIN_USABLE_SPACE = 10_000_000_000L;

  private final @NotNull File defaultDirectory;
  private final @NotNull ToLongFunction<File> usableSpace;
  private final @NotNull Predicate<File> writableDirectory;
  /**
   * Serializes configuration separately from the creation lock, so applying the preferences on the
   * FX thread never waits for a running creation.
   */
  private final Object configurationLock = new Object();
  private final @NotNull AtomicReference<DirectoryState> state;

  TempDirectoryManager() {
    this(File::getUsableSpace);
  }

  TempDirectoryManager(@NotNull ToLongFunction<File> usableSpace) {
    this(new File(System.getProperty("java.io.tmpdir")), usableSpace,
        TempDirectoryManager::isWritableDirectory);
  }

  /**
   * @param defaultDirectory  fallback if no configured directory works
   * @param usableSpace       usable space of a directory in bytes
   * @param writableDirectory true if the directory exists or was created and is writable
   */
  TempDirectoryManager(@NotNull File defaultDirectory, @NotNull ToLongFunction<File> usableSpace,
      @NotNull Predicate<File> writableDirectory) {
    this.defaultDirectory = defaultDirectory.getAbsoluteFile();
    this.usableSpace = usableSpace;
    this.writableDirectory = writableDirectory;
    state = new AtomicReference<>(
        new DirectoryState(List.of(this.defaultDirectory), this.defaultDirectory));
  }

  /**
   * Makes sure the .mzmine folder is never used
   */
  private static boolean isAllowed(@Nullable File directory) {
    if (directory == null) {
      return false;
    }
    if (directory.exists() && directory.isFile()) {
      logger.warning("A file cannot be used as temporary directory: " + directory);
      return false;
    }
    if (FileAndPathUtil.isInMzmineDir(directory)) {
      logger.warning(
          "The mzmine settings folder cannot be used as temporary directory: " + directory);
      return false;
    }
    return true;
  }

  /**
   * Creates the directory if needed and writes a probe file, because an existing directory may
   * still be read-only, e.g., on a write-protected drive or network share.
   */
  private static boolean isWritableDirectory(@NotNull File directory) {
    try {
      final Path path = directory.toPath();
      Files.createDirectories(path);
      // the name matches the temp file cleanup pattern in case the deletion fails
      final Path probe = Files.createTempFile(path, "mzmine_write_test", ".tmp");
      Files.deleteIfExists(probe);
      return true;
    } catch (IOException | InvalidPathException | SecurityException e) {
      logger.log(Level.WARNING,
          "Cannot write to temporary directory %s: %s".formatted(directory, e));
      return false;
    }
  }

  /**
   * Checks write access on every call, because callers use the directory directly without failover,
   * e.g., as the working directory of external tools. Free space is not checked.
   *
   * @return the first writable configured directory, otherwise the default directory
   */
  @NotNull File getDirectory() {
    return firstWritableDirectory(state.get().directories());
  }

  /**
   * @return the directory that {@link #create(long, TempFileOperation)} tries first
   */
  @NotNull File getCurrentDirectory() {
    return state.get().currentDirectory();
  }

  /**
   * @return the configured directories, without the runtime fallback
   */
  File @NotNull [] getDirectories() {
    return state.get().directories().toArray(File[]::new);
  }

  /**
   * Keeps inaccessible directories in the list, so they are retried on failover, e.g., after a
   * drive was reconnected. The current directory is the first writable directory. If none is
   * writable, the default java temp directory is used until a configured directory works again,
   * without adding it to the list. The .mzmine dir and its subdirectories are removed, because temp
   * file cleanup deletes files there. An empty list is replaced by the default directory.
   *
   * @return the configured directories, without the runtime fallback
   */
  File @NotNull [] setDirectories(@NotNull File... paths) {
    final List<File> requested = Arrays.stream(paths).map(File::getAbsoluteFile).distinct()
        .filter(TempDirectoryManager::isAllowed).toList();
    synchronized (configurationLock) {
      final DirectoryState previous = state.get();
      // decision: an unchanged list returns without disk access, because the preferences are
      // applied on the FX thread after any change, e.g., when toggling dark mode
      if (requested.equals(previous.directories())) {
        return previous.directories().toArray(File[]::new);
      }

      final List<File> directories = requested.stream().collect(CollectionUtils.toArrayList());
      if (directories.isEmpty()) {
        directories.add(defaultDirectory);
      }
      if (!directories.equals(previous.directories())) {
        // select first actually accessible directory
        state.set(new DirectoryState(List.copyOf(directories), firstWritableDirectory(directories)));
      }
      return directories.toArray(File[]::new);
    }
  }

  /**
   * Callers of {@link #getDirectory()} use the directory directly without failover, e.g., as the
   * working directory of external tools, so it needs to be writable.
   *
   * @return the first writable directory, otherwise the default directory
   */
  private @NotNull File firstWritableDirectory(@NotNull List<File> directories) {
    // stops at the first writable directory, as unreachable network drives may block for long
    for (File directory : directories) {
      if (writableDirectory.test(directory)) {
        return directory;
      }
    }
    if (writableDirectory.test(defaultDirectory)) {
      logger.warning("""
          No configured temporary directory is writable: %s. Using the default temporary \
          directory %s until a configured directory becomes available.""".formatted(directories,
          defaultDirectory));
      return defaultDirectory;
    }
    logger.warning("No temporary directory is writable: " + directories);
    return directories.getFirst();
  }

  /**
   * Tries the current directory first and then all configured directories in priority order, so
   * reclaimed space is reused. A directory needs {@link #MIN_USABLE_SPACE} beyond the required
   * space. If no directory has that, the one with the most usable space is used if it fits the
   * required space. If no configured directory can hold the file, the default directory is used.
   * The operation must close any channels it opened before throwing.
   */
  synchronized <T> T create(long requiredSpace, @NotNull TempFileOperation<T> operation)
      throws IOException {
    final DirectoryState snapshot = state.get();
    final List<File> configured = snapshot.directories();

    final List<Exception> failures = new ArrayList<>();
    // keeps priority order so ties choose the earlier directory
    final Map<File, Long> lowSpace = new LinkedHashMap<>();

    final List<File> candidates = Stream.concat(Stream.of(snapshot.currentDirectory()),
        snapshot.directories().stream()).distinct().toList();
    for (File directory : candidates) {
      try {
        final long space = createAndGetUsableSpace(directory);
        if (space < requiredSpace + MIN_USABLE_SPACE) {
          lowSpace.put(directory, space);
          continue;
        }
        return createIn(snapshot, directory, operation);
      } catch (IOException e) {
        failures.add(e);
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
        failures.add(e);
      }
    }

    // decision: the default directory is only a last resort, so the system drive is not filled
    // while a configured directory can still hold the file
      try {
        final long space = createAndGetUsableSpace(defaultDirectory);
        if (space >= requiredSpace) {
          if (!defaultDirectory.equals(snapshot.currentDirectory())) {
            logger.warning("No configured temporary directory can hold the file. "
                + "Using the default temporary directory " + defaultDirectory);
          }
          return createIn(snapshot, defaultDirectory, operation);
        }
        lowSpace.put(defaultDirectory, space);
      } catch (IOException e) {
        failures.add(e);
      }

    final String lowSpaceWarning = lowSpace.entrySet().stream()
        .map(e -> "Only %d bytes usable in %s".formatted(e.getValue(), e.getKey()))
        .collect(Collectors.joining("\n"));

    final IOException failure = new IOException("""
        Cannot create a temporary file in any configured directory: %s. Directories need %d GB of usable space beyond the requested size.
        %s""".formatted(configured, MIN_USABLE_SPACE / 1_000_000_000L, lowSpaceWarning));
    failures.forEach(failure::addSuppressed);
    throw failure;
  }

  /**
   * @return usable space after creating the directory if needed
   */
  private long createAndGetUsableSpace(@NotNull File directory) throws IOException {
    // usually exists, check first to avoid the exception thrown internally by createDirectories
    final Path path = directory.toPath();
    if (!Files.isDirectory(path)) {
      Files.createDirectories(path);
    }
    // usable space is 0 for missing directories, so check after creation
    return usableSpace.applyAsLong(directory);
  }

  private <T> T createIn(@NotNull DirectoryState snapshot, @NotNull File directory,
      @NotNull TempFileOperation<T> operation) throws IOException {
    final T result = operation.create(directory.toPath());
    if (!directory.equals(snapshot.currentDirectory()) && state.compareAndSet(snapshot,
        new DirectoryState(snapshot.directories(), directory))) {
      logger.info("Using temporary directory " + directory);
    }
    return result;
  }

  /**
   * @param directories      configured directories in priority order
   * @param currentDirectory one of the directories or the default directory as runtime fallback
   */
  private record DirectoryState(@NotNull List<File> directories, @NotNull File currentDirectory) {

  }

  @FunctionalInterface
  interface TempFileOperation<T> {

    T create(Path directory) throws IOException;
  }
}
