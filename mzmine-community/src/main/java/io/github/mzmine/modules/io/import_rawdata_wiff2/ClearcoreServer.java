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

package io.github.mzmine.modules.io.import_rawdata_wiff2;

import com.sun.jna.Platform;
import io.github.mzmine.util.ShellUtils;
import io.github.mzmine.util.concurrent.CloseableReentrantReadWriteLock;
import io.github.mzmine.util.concurrent.CloseableResourceLock;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.io.JsonUtils;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout.OfByte;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClearcoreServer {

  private static final Logger logger = Logger.getLogger(ClearcoreServer.class.getName());
  private static final CloseableReentrantReadWriteLock startLock = new CloseableReentrantReadWriteLock();

  /**
   * Version of the bundled SCIEX Clearcore server. The server is copied from the (potentially
   * read-only) installation directory into the writable user directory so that the launcher can
   * write the license and appsettings next to the executable. Bump this whenever the bundled server
   * changes to copy it into a fresh, version-named directory on the next launch.
   */
  private static final String SERVER_VERSION = "v1.6.1.766";
  /**
   * Directory of the bundled server in the (read-only) installation / external tools dir.
   */
  private static final String SCIEX_SOURCE_DIR = "sciex_wiff2";
  /**
   * Directory of the server copy in the writable user resources dir
   * ({@code .mzmine/external_resources/sciex_wiff2_<version>/}). The version is encoded in the folder
   * name, so the presence of the directory itself signals that this version was already copied.
   */
  private static final String SCIEX_USER_DIR = SCIEX_SOURCE_DIR + "_" + SERVER_VERSION;
  // 461808 is the minimum release key for .NET Framework 4.7.2. Later versions use larger keys.
  private static final int DOT_NET_FRAMEWORK_472_RELEASE_KEY = 461808;
  private static final Pattern WINDOWS_DOT_NET_RELEASE_PATTERN = Pattern.compile(
      "(?m)^\\s*Release\\s+REG_DWORD\\s+(0x[0-9a-fA-F]+|\\d+)\\s*$");
  private static final String WINDOWS_DOT_NET_RELEASE_REGISTRY_PATH = "HKLM\\SOFTWARE\\Microsoft\\NET Framework Setup\\NDP\\v4\\Full";
  private static final Pattern LINUX_DOT_NET_RUNTIME_PATTERN = Pattern.compile(
      "(?m)^Microsoft\\.NETCore\\.App\\s+([\\d]+)\\.\\d\\.\\d\\b.*$");

  /**
   * current instance. may change if the server crashes or so.
   */
  private static ClearcoreServer server;
  private final ProcessHandle processHandle;

  private final int port;
  private final String address;

  private ClearcoreServer() throws IOException {
    // The installation directory may be read-only (e.g. C:\Program Files), but the launcher needs
    // to write the license and appsettings next to the server executable. Copy the bundled server
    // into the writable user directory once per version, and run it from there.
    ensureServerCopied();

    final File dataAccessExe = FileAndPathUtil.resolveInDownloadResourcesDir(getDataAccessPath());
    if (!dataAccessExe.exists()) {
      throw new RuntimeException(
          "SCIEX data API executable does not exist after copying to the user directory: "
              + dataAccessExe.getAbsolutePath());
    }
    if (!Platform.isWindows() && !dataAccessExe.setExecutable(true, false)) {
      // make sure file is set as executable on linux
      throw new RuntimeException(
          "Failed to make SCIEX data API executable: " + dataAccessExe.getAbsolutePath());
    }

    // Read port/address from the appsettings.json that was copied into the user directory.
    final File appsettings = FileAndPathUtil.resolveInDownloadResourcesDir(getAppSettingsPath());
    Map<String, String> o = JsonUtils.readValueOrThrow(appsettings);
    port = Integer.parseInt(Objects.requireNonNullElse(o.get("port"), o.get("Port")));
    address = Objects.requireNonNullElse(o.get("Host"), o.get("host"));

    if (address == null) {
      throw new RuntimeException("Cannot determine address of SCIEX clearcore service.");
    }

//    ProcessBuilder b = new ProcessBuilder(dataAccessExe.getAbsolutePath(), "--console").inheritIO();
//    b.directory(dataAccessExe.getParentFile());
//    processHandle = b.start().toHandle();

    final Arena arena = Arena.ofAuto();
    final ByteArrayList bytes = new ByteArrayList(
        dataAccessExe.getAbsolutePath().getBytes(StandardCharsets.UTF_8));
    bytes.add((byte) 0);
    final MemorySegment memorySegment = arena.allocateFrom(OfByte.JAVA_BYTE, bytes.toByteArray());

    // Decrypt + write the license file, spawn the server, delete the license.
    final long handle = Wiff2LauncherLib_h.wiff2_launch_server(memorySegment);

    processHandle = ProcessHandle.of(handle)
        .orElseThrow(() -> new RuntimeException("Cannot launch wiff2 data access server."));
  }

  @NotNull
  public static ClearcoreServer getOrStart() {
    startServer();
    return server;
  }

  @Nullable
  public static ClearcoreServer getInstance() {
    return server;
  }

  public static void terminateSeverIfRunning() {
    if (getInstance() != null) {
      getInstance().terminateClearcoreInstance();
    }
  }

  private static @NotNull String getDataAccessPath() {
    if (Platform.isWindows()) {
      return getPathForOs("Clearcore2.SampleData.DataAccessApi.exe");
    }
    if (Platform.isLinux()) {
      return getPathForOs("Clearcore2.SampleData.DataAccessApi");
    }
    throw new RuntimeException(
        "Native SCIEX support is not available for your operating system. Please convert to mzML or switch to Windows/Linux.");
  }

  private static @NotNull String getAppSettingsPath() {
    return getPathForOs("appsettings.json");
  }

  private static @NotNull String getPathForOs(@NotNull final String filename) {
    return getUserServerDir() + "/" + filename;
  }

  /**
   * OS-specific server sub-directory name (e.g. {@code Server-win10-x64}).
   */
  private static @NotNull String getServerSubDirForOs() {
    if (Platform.isWindows()) {
      return "Server-win10-x64";
    }
    if (Platform.isLinux()) {
      return "Server-linux-x64";
    }
    throw new RuntimeException(
        "Native SCIEX support is not available for your operating system. Please convert to mzML or switch to Windows/Linux.");
  }

  /**
   * Relative path of the bundled server in the installation / external tools dir (unversioned).
   */
  private static @NotNull String getSourceServerDir() {
    return SCIEX_SOURCE_DIR + "/" + getServerSubDirForOs();
  }

  /**
   * Relative path of the server copy in the user resources dir (version encoded in the folder name).
   */
  private static @NotNull String getUserServerDir() {
    return SCIEX_USER_DIR + "/" + getServerSubDirForOs();
  }

  /**
   * Copies the bundled server from the installation directory into the writable user directory
   * ({@code .mzmine/external_resources/sciex_wiff2_<version>/Server-<os>/}) unless it has already
   * been copied for the current {@link #SERVER_VERSION} (signalled by the version-named directory
   * existing). This is required because the launcher writes the license and appsettings next to the
   * executable, which is not possible inside a read-only installation directory (e.g. C:\Program
   * Files).
   */
  private static void ensureServerCopied() throws IOException {
    final File destServerDir = FileAndPathUtil.resolveInDownloadResourcesDir(getUserServerDir());
    if (destServerDir.isDirectory()) {
      logger.fine(() -> "SCIEX server version %s already present in %s".formatted(SERVER_VERSION,
          destServerDir.getAbsolutePath()));
      return;
    }

    // Synchronize the copy so concurrent threads accessing the server do not copy at the same time.
    // The lock is reentrant, so it is safe to acquire here even while held by startServer().
    try (var _ = startLock.lockWrite()) {
      // double-checked: another thread may have finished the copy while we waited for the lock.
      if (destServerDir.isDirectory()) {
        return;
      }

      final File sourceServerDir = FileAndPathUtil.resolveInExternalToolsDir(getSourceServerDir());
      if (!sourceServerDir.isDirectory()) {
        throw new IOException(
            "SCIEX server source directory does not exist. Run gradlew build first to download: "
                + sourceServerDir.getAbsolutePath());
      }

      logger.info(() -> "Copying SCIEX server version %s from %s to %s".formatted(SERVER_VERSION,
          sourceServerDir.getAbsolutePath(), destServerDir.getAbsolutePath()));

      // Copy into a temporary sibling directory first, then move it into place. This way an
      // interrupted copy never leaves a partial directory under the version-named folder (which
      // would otherwise be mistaken for a complete copy on the next launch).
      final File parentDir = destServerDir.getParentFile();
      if (parentDir != null && !FileAndPathUtil.createDirectory(parentDir)) {
        throw new IOException(
            "Could not create user directory for SCIEX server: " + parentDir.getAbsolutePath());
      }
      final Path destPath = destServerDir.toPath();
      final Path tmpPath = destPath.resolveSibling(
          destPath.getFileName() + ".tmp-" + ProcessHandle.current().pid());
      deleteRecursively(tmpPath.toFile());
      copyDirectoryRecursively(sourceServerDir.toPath(), tmpPath);
      try {
        Files.move(tmpPath, destPath, StandardCopyOption.ATOMIC_MOVE);
      } catch (IOException atomicMoveUnsupported) {
        Files.move(tmpPath, destPath);
      }
    }
  }

  private static void copyDirectoryRecursively(@NotNull final Path source, @NotNull final Path target)
      throws IOException {
    try (var stream = Files.walk(source)) {
      for (Path src : (Iterable<Path>) stream::iterator) {
        final Path dest = target.resolve(source.relativize(src).toString());
        if (Files.isDirectory(src)) {
          Files.createDirectories(dest);
        } else {
          Files.createDirectories(dest.getParent());
          Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING,
              StandardCopyOption.COPY_ATTRIBUTES);
        }
      }
    }
  }

  private static void deleteRecursively(@NotNull final File dir) throws IOException {
    final Path root = dir.toPath();
    if (!Files.exists(root)) {
      return;
    }
    try (var stream = Files.walk(root)) {
      stream.sorted(Comparator.reverseOrder()).forEach(p -> {
        try {
          Files.deleteIfExists(p);
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      });
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }

  private static void startServer() {
    try (CloseableResourceLock _ = startLock.lockRead()) {
      if (server != null && server.isAlive()) {
        return;
      }
    }

    try (var _ = startLock.lockWrite()) {
      if (server != null && server.isAlive()) {
        return;
      }
      if (server != null && !server.isAlive()) {
        server.terminateClearcoreInstance();
      }

      if (Platform.isWindows() && !checkDependenciesWindows()) {
        throw new RuntimeException(
            "SCIEX Clearcore on Windows requires .NET Framework 4.7.2 or later and Visual Studio C++ Redistributable.");
      }
      if (Platform.isLinux() && !checkDependenciesLinux()) {
        throw new RuntimeException("SCIEX Clearcore on Linux requires .NET 6.0.");
      }
      server = new ClearcoreServer();
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Error while starting SCIEX Clearcore server.", e);
      throw new RuntimeException("Cannot start SCIEX Clearcore server.");
    }

    if (server == null || !server.isAlive()) {
      throw new RuntimeException("Unable to start SCIEX clearcore server.");
    }
  }

  public static boolean isInstanceAlive() {
    return server != null && server.isAlive();
  }

  private void terminateClearcoreInstance() {
    if (processHandle != null) {
      logger.info("Terminating SCIEX clearcore service.");
      if (!processHandle.destroy()) {
        processHandle.destroyForcibly();
      }
    }
  }

  private boolean isAlive() {
    return processHandle != null && processHandle.isAlive();
  }

  public int port() {
    return port;
  }

  public String address() {
    return address;
  }

  private static boolean checkDependenciesWindows() {
    // decision: query the 64-bit registry view because the bundled Clearcore server is x64.
    final String output = ShellUtils.runGetOutput("reg", "query",
        WINDOWS_DOT_NET_RELEASE_REGISTRY_PATH, "/v", "Release", "/reg:64");
    if (output == null || output.isBlank()) {
      return false;
    }

    final var matcher = WINDOWS_DOT_NET_RELEASE_PATTERN.matcher(output);
    if (!matcher.find()) {
      return false;
    }

    try {
      final int releaseKey = Integer.decode(matcher.group(1));
      return releaseKey >= DOT_NET_FRAMEWORK_472_RELEASE_KEY;
    } catch (NumberFormatException e) {
      logger.fine("Cannot parse .NET Framework release key from registry output: " + output);
      return false;
    }
  }

  private static boolean checkDependenciesLinux() {
    final String output = ShellUtils.runGetOutput("dotnet", "--list-runtimes");
    if (output == null || output.isBlank()) {
      return false;
    }

    Matcher matcher = LINUX_DOT_NET_RUNTIME_PATTERN.matcher(output);
    if (matcher.find()) {
      String versionStr = matcher.group(1);
      if (Integer.parseInt(versionStr) >= 6) {
        return true;
      }
    }
    return false;
  }
}
