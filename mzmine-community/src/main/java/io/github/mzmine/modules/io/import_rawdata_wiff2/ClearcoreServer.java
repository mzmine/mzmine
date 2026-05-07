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
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout.OfByte;
import java.nio.charset.StandardCharsets;
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
    final File dataAccessExe = FileAndPathUtil.resolveInExternalToolsDir(getDataAccessPath());

    // Write appsettings.json (non-secret) before the native call so the server
    // finds the license path on startup.

    // Read port/address from the file we just wrote.
    final File appsettings = FileAndPathUtil.resolveInExternalToolsDir(getAppSettingsPath());
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
    if (Platform.isWindows()) {
      return "sciex_wiff2/Server-win10-x64/%s".formatted(filename);
    }
    if (Platform.isLinux()) {
      return "sciex_wiff2/Server-linux-x64/%s".formatted(filename);
    }
    throw new RuntimeException(
        "Native SCIEX support is not available for your operating system. Please convert to mzML or switch to Windows/Linux.");
  }

  private static void startServer() {
    try (CloseableResourceLock _ = startLock.lockRead()) {
      if (server != null && server.isAlive()) {
        return;
      }
    }

    try (var _ = startLock.lockWrite()) {
      if (server != null) {
        server.terminateClearcoreInstance();
      }
      if (Platform.isWindows() && !checkDependenciesWindows()) {
        throw new RuntimeException(
            "SCIEX Clearcore on Windows requires .NET Framework 4.7.2 or later.");
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
      if(!processHandle.destroy()) {
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
    if(matcher.find()) {
      String versionStr = matcher.group(1);
      if(Integer.parseInt(versionStr) >= 6) {
        return true;
      }
    }
    return false;
  }
}
