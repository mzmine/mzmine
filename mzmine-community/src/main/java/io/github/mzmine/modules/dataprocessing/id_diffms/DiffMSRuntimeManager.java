/*
 * Copyright (c) 2004-2026 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.id_diffms;

import io.github.mzmine.util.ZipUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.function.BooleanSupplier;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages DiffMS Python runtimes built from conda-pack archives.
 */
public final class DiffMSRuntimeManager {

  private static final Logger logger = Logger.getLogger(DiffMSRuntimeManager.class.getName());
  private static final Object INSTALL_LOCK = new Object();

  private static final String USER_RUNTIME_DIR_REL = "external_resources/models/diffms/runtimes/";

  public enum Variant {
    CPU("cpu"), CUDA("cuda");

    private final String id;

    Variant(final String id) {
      this.id = id;
    }

    public String id() {
      return id;
    }
  }

  private DiffMSRuntimeManager() {
  }

  /**
   * Ensures the requested runtime exists (extract+conda-unpack if needed) and
   * returns a usable
   * Python executable.
   * <p>
   * If {@code requested == CUDA} but no suitable CUDA pack exists on this
   * platform, it falls back
   * to CPU.
   */
  public static @NotNull File ensureRuntimeAndGetPython(final @NotNull Variant requested,
      final @Nullable BooleanSupplier isCanceled) {
    Objects.requireNonNull(requested);

    synchronized (INSTALL_LOCK) {
      final File py = getUsablePython(requested);
      if (py != null) {
        return py;
      }

      final Platform platform = Platform.detect();
      final Variant effective = (requested == Variant.CUDA && platform.os == OS.MACOS) ? Variant.CPU : requested;
      final File runtimeDir = getUserRuntimeDir(effective, platform);

      if (findPythonExecutable(runtimeDir) != null) {
        // we found a python candidate but it is not executable/usable.
        logger.warning(() -> "DiffMS: found unusable python executable in existing runtime dir. "
            + "Will reinstall runtime pack.");
        deleteDirectoryRecursively(runtimeDir.toPath(), isCanceled);
      }

      // Install runtime from pack
      final File pack = findBestPackFile(effective, platform)
          .or(() -> effective == Variant.CUDA ? findBestPackFile(Variant.CPU, platform)
              : Optional.empty())
          .orElseThrow(() -> {
            final File userPacks = new File(new File(FileAndPathUtil.getMzmineDir(), "diffms"), "runtime-packs");
            return new IllegalStateException(
                "No DiffMS runtime pack found for " + effective.id + " on " + platform + ". Expected under "
                    + userPacks.getAbsolutePath() + ". Use the DiffMS Build Runtime module to create one.");
          });

      logger.info(() -> "DiffMS: extracting runtime pack: " + pack.getAbsolutePath());
      extractPack(pack, runtimeDir, isCanceled);

      // Run conda-unpack (once)
      runCondaUnpackIfNeeded(runtimeDir, isCanceled);

      final File py2 = findPythonExecutable(runtimeDir);
      if (!isUsablePythonExecutable(py2)) {
        throw new IllegalStateException("DiffMS runtime installed but python was not found in "
            + runtimeDir.getAbsolutePath());
      }
      return py2;
    }
  }

  /**
   * Returns a usable Python executable for the requested variant if already
   * installed and relocated.
   */
  public static @Nullable File getUsablePython(final @NotNull Variant requested) {
    final Platform platform = Platform.detect();
    final Variant effective = (requested == Variant.CUDA && platform.os == OS.MACOS) ? Variant.CPU : requested;
    final File runtimeDir = getUserRuntimeDir(effective, platform);
    final File py = findPythonExecutable(runtimeDir);
    return isUsablePythonExecutable(py) ? py : null;
  }

  private static boolean isUsablePythonExecutable(final @Nullable File py) {
    if (py == null || !py.isFile()) {
      return false;
    }
    // We rely on being able to execute the python entrypoint. Existence alone is
    // not enough
    // because broken tar extraction (missing symlinks) can create 0-byte
    // placeholder files.
    if (!py.canExecute()) {
      return false;
    }
    return py.length() > 0;
  }

  private static @NotNull File getUserRuntimeDir(final @NotNull Variant variant,
      final @NotNull Platform platform) {
    final File base = FileAndPathUtil.resolveInMzmineDir(USER_RUNTIME_DIR_REL);
    if (base == null) {
      // should never happen, but keep error messages readable
      return new File(USER_RUNTIME_DIR_REL);
    }
    // keep per-platform to avoid collisions if users copy runtimes across machines
    return new File(base, "runtime-" + variant.id + "-" + platform.os.id + "-" + platform.arch.id);
  }

  private static @Nullable File findPythonExecutable(final @NotNull File runtimeDir) {
    if (!runtimeDir.isDirectory()) {
      return null;
    }
    final List<File> candidates = List.of( //
        new File(runtimeDir, "bin/python"),
        new File(runtimeDir, "bin/python3"),
        new File(runtimeDir, "python.exe"),
        new File(runtimeDir, "Scripts/python.exe"));
    for (final File f : candidates) {
      if (f.isFile()) {
        return f;
      }
    }

    // Fallback: conda environments often contain a versioned interpreter (e.g.,
    // bin/python3.9).
    final File bin = new File(runtimeDir, "bin");
    final File[] binFiles = bin.isDirectory() ? bin.listFiles() : null;
    if (binFiles != null) {
      for (final File f : binFiles) {
        final String n = f.getName();
        if (f.isFile() && (n.equals("python3") || n.matches("python3\\.\\d+"))) {
          return f;
        }
      }
    }
    return null;
  }

  private static void runCondaUnpackIfNeeded(final @NotNull File runtimeDir,
      final @Nullable BooleanSupplier isCanceled) {
    final File marker = new File(runtimeDir, ".mzmine_conda_unpacked");
    if (marker.isFile()) {
      return;
    }

    final File condaUnpack = findCondaUnpack(runtimeDir);
    if (condaUnpack == null || !condaUnpack.isFile()) {
      // Some conda-pack workflows use --dest-prefix and do not include conda-unpack.
      // In that case, we accept the runtime as-is.
      logger.info(() -> "DiffMS: conda-unpack not found; skipping prefix rewrite for "
          + runtimeDir.getAbsolutePath());
      try {
        Files.writeString(marker.toPath(), "skipped\n");
      } catch (Exception ignored) {
      }
      return;
    }

    // Find Python executable to run conda-unpack with.
    // We can't execute conda-unpack directly because its shebang points to a placeholder path
    // that hasn't been rewritten yet (chicken-and-egg problem).
    final File python = findPythonExecutable(runtimeDir);
    if (python == null || !python.isFile()) {
      logger.warning(() -> "DiffMS: Python not found in extracted runtime; skipping conda-unpack for "
          + runtimeDir.getAbsolutePath());
      try {
        Files.writeString(marker.toPath(), "skipped-no-python\n");
      } catch (Exception ignored) {
      }
      return;
    }

    // Ensure Python is executable (safety check for platforms where tar extraction may not preserve permissions)
    if (!python.canExecute()) {
      logger.info(() -> "DiffMS: Setting execute permission on Python: " + python.getAbsolutePath());
      python.setExecutable(true, false);
    }

    logger.info(() -> "DiffMS: running conda-unpack with Python: " + python.getAbsolutePath() 
        + " " + condaUnpack.getAbsolutePath());
    final ProcessBuilder pb = new ProcessBuilder(python.getAbsolutePath(), condaUnpack.getAbsolutePath());
    pb.directory(runtimeDir);
    pb.redirectErrorStream(true);
    try {
      final Process p = pb.start();
      
      // Capture output for error reporting
      final StringBuilder output = new StringBuilder();
      try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          if (isCanceled != null && isCanceled.getAsBoolean()) {
            p.destroyForcibly();
            throw new IllegalStateException("Canceled while running conda-unpack.");
          }
          // Keep output limited to avoid memory issues
          if (output.length() < 10000) {
            output.append(line).append('\n');
          }
        }
      }
      
      final int code = p.waitFor();
      if (code != 0) {
        final String errorMsg = output.length() > 0 
            ? "conda-unpack failed with exit code " + code + ". Output:\n" + output
            : "conda-unpack failed with exit code " + code + " (no output captured)";
        throw new IllegalStateException(errorMsg);
      }
      Files.writeString(marker.toPath(), "ok\n");
    } catch (Exception e) {
      throw new IllegalStateException("Failed to run conda-unpack in " + runtimeDir.getAbsolutePath() 
          + ". Python: " + python.getAbsolutePath() + ", conda-unpack: " + condaUnpack.getAbsolutePath(),
          e);
    }
  }

  private static @Nullable File findCondaUnpack(final @NotNull File runtimeDir) {
    final List<File> candidates = List.of( //
        new File(runtimeDir, "bin/conda-unpack"),
        new File(runtimeDir, "Scripts/conda-unpack.exe"),
        new File(runtimeDir, "Scripts/conda-unpack"),
        new File(runtimeDir, "conda-unpack"));
    for (final File f : candidates) {
      if (f.isFile()) {
        return f;
      }
    }
    return null;
  }

  private static void extractPack(final @NotNull File pack, final @NotNull File runtimeDir,
      final @Nullable BooleanSupplier isCanceled) {
    try {
      Files.createDirectories(runtimeDir.toPath());
    } catch (IOException e) {
      throw new IllegalStateException("Cannot create DiffMS runtime directory: "
          + runtimeDir.getAbsolutePath(), e);
    }

    final String name = pack.getName().toLowerCase(Locale.ROOT);
    if (name.endsWith(".zip")) {
      try {
        ZipUtils.unzipFile(pack, runtimeDir);
        return;
      } catch (IOException e) {
        throw new IllegalStateException("Failed to unzip DiffMS runtime pack: " + pack.getAbsolutePath(),
            e);
      }
    }
    if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) {
      extractTarGz(pack, runtimeDir, isCanceled);
      return;
    }
    throw new IllegalStateException("Unsupported DiffMS runtime pack format: " + pack.getName());
  }

  private static void extractTarGz(final @NotNull File tarGz, final @NotNull File outDir,
      final @Nullable BooleanSupplier isCanceled) {
    final Path outPath = outDir.toPath().toAbsolutePath().normalize();
    try (var fis = new FileInputStream(tarGz);
        var bis = new BufferedInputStream(fis);
        var gzis = new GzipCompressorInputStream(bis);
        var tar = new TarArchiveInputStream(gzis)) {

      TarArchiveEntry entry;
      while ((entry = tar.getNextTarEntry()) != null) {
        if (isCanceled != null && isCanceled.getAsBoolean()) {
          throw new IllegalStateException("Canceled while extracting runtime pack.");
        }
        final String entryName = entry.getName();
        if (entryName == null || entryName.isBlank()) {
          continue;
        }
        if (entryName.contains("..")) {
          throw new IOException("Malicious tar entry: " + entryName);
        }
        final Path dest = outPath.resolve(entryName).normalize();
        if (!dest.startsWith(outPath)) {
          throw new IOException("Bad tar entry (path traversal): " + entryName);
        }

        if (entry.isDirectory()) {
          Files.createDirectories(dest);
          continue;
        }

        final Path parent = dest.getParent();
        if (parent == null) {
          throw new IOException("Bad tar entry (no parent): " + entryName);
        }
        Files.createDirectories(parent);

        // Preserve symlinks/hardlinks. conda-pack environments rely on them (e.g.,
        // bin/python
        // often is a symlink to python3).
        if (entry.isSymbolicLink()) {
          final String linkName = entry.getLinkName();
          if (linkName == null || linkName.isBlank()) {
            throw new IOException("Bad symlink entry (missing link target): " + entryName);
          }
          if (linkName.startsWith("/")) {
            throw new IOException(
                "Refusing absolute symlink target '" + linkName + "' for entry: " + entryName);
          }
          // Validate that the resolved target stays within the extracted runtime
          // directory,
          // while still preserving the original relative linkName.
          final Path resolvedTarget = parent.resolve(linkName).normalize();
          if (!resolvedTarget.startsWith(outPath)) {
            throw new IOException("Refusing symlink that escapes runtime dir. entry=" + entryName
                + ", target=" + linkName);
          }
          Files.deleteIfExists(dest);
          Files.createSymbolicLink(dest, Path.of(linkName));
          continue;
        }

        if (entry.isLink()) { // hard link
          final String linkName = entry.getLinkName();
          if (linkName == null || linkName.isBlank()) {
            throw new IOException("Bad hardlink entry (missing link target): " + entryName);
          }
          final Path target = outPath.resolve(linkName).normalize();
          if (!target.startsWith(outPath)) {
            throw new IOException("Bad hardlink target (path traversal): " + linkName);
          }
          Files.deleteIfExists(dest);
          try {
            Files.createLink(dest, target);
          } catch (UnsupportedOperationException | IOException e) {
            // Fallback for filesystems not supporting hardlinks.
            Files.copy(target, dest, StandardCopyOption.REPLACE_EXISTING);
          }
          continue;
        }

        // Regular file: write to temp then move (avoid partial files on cancel)
        final Path tmp = Files.createTempFile(parent, dest.getFileName().toString(), ".part");
        Files.copy(tar, tmp, StandardCopyOption.REPLACE_EXISTING);
        Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING);

        // Preserve execute bits (best-effort). Only set executable if tar entry
        // indicates it.
        final int mode = entry.getMode();
        final boolean anyExec = (mode & 0111) != 0;
        if (anyExec) {
          dest.toFile().setExecutable(true, false);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to extract DiffMS runtime tar.gz: " + tarGz.getAbsolutePath(),
          e);
    }
  }

  private static void deleteDirectoryRecursively(final @NotNull Path dir,
      final @Nullable BooleanSupplier isCanceled) {
    if (!Files.exists(dir)) {
      return;
    }
    final Path norm = dir.toAbsolutePath().normalize();
    final FileVisitor<Path> visitor = new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
          throws IOException {
        if (isCanceled != null && isCanceled.getAsBoolean()) {
          throw new IOException("Canceled while deleting runtime directory.");
        }
        Files.deleteIfExists(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(final Path d, final IOException exc)
          throws IOException {
        if (exc != null) {
          throw exc;
        }
        if (isCanceled != null && isCanceled.getAsBoolean()) {
          throw new IOException("Canceled while deleting runtime directory.");
        }
        Files.deleteIfExists(d);
        return FileVisitResult.CONTINUE;
      }
    };
    try {
      Files.walkFileTree(norm, visitor);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to delete broken DiffMS runtime directory: " + norm, e);
    }
  }

  public static boolean anyPackExists() {
    final Platform platform = Platform.detect();
    return findBestPackFile(Variant.CPU, platform).isPresent()
        || findBestPackFile(Variant.CUDA, platform).isPresent();
  }

  public static Optional<File> findBestPackFile(final @NotNull Variant variant,
      final @NotNull Platform platform) {
    File userDiffMs = new File(FileAndPathUtil.getMzmineDir(), "diffms");
    File userPacks = new File(userDiffMs, "runtime-packs");
    if (!userPacks.isDirectory()) {
      return Optional.empty();
    }

    final String v = variant.id;
    final String os = platform.os.id;
    final String arch = platform.arch.id;

    final List<File> candidates = new ArrayList<>();
    
    final File[] files = userPacks.listFiles();
    if (files != null) {
      for (final File f : files) {
        if (!f.isFile()) {
          continue;
        }
        final String n = f.getName().toLowerCase(Locale.ROOT);
        if (!n.contains("diffms-runtime-")) {
          continue;
        }
        if (!n.contains("-" + v + "-")) {
          continue;
        }
        if (!n.contains("-" + os + "-")) {
          continue;
        }
        if (!n.contains("-" + arch)) {
          continue;
        }
        if (!(n.endsWith(".zip") || n.endsWith(".tar.gz") || n.endsWith(".tgz"))) {
          continue;
        }
        candidates.add(f);
      }
    }

    return candidates.stream().max(Comparator.comparingLong(File::lastModified));
  }

  private enum OS {
    WINDOWS("windows"), LINUX("linux"), MACOS("macos");

    final String id;

    OS(final String id) {
      this.id = id;
    }
  }

  private enum Arch {
    X86_64("x86_64"), ARM64("arm64");

    final String id;

    Arch(final String id) {
      this.id = id;
    }
  }

  private record Platform(OS os, Arch arch) {

    static Platform detect() {
      final String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
      final OS os;
      if (osName.contains("win")) {
        os = OS.WINDOWS;
      } else if (osName.contains("mac")) {
        os = OS.MACOS;
      } else {
        os = OS.LINUX;
      }

      final String archName = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
      final Arch arch = (archName.contains("aarch64") || archName.contains("arm64")) ? Arch.ARM64
          : Arch.X86_64;

      return new Platform(os, arch);
    }

    @Override
    public String toString() {
      return os.id + "-" + arch.id;
    }
  }
}
