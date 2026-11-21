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

package io.github.mzmine.modules.io.import_rawdata_agilent_d;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.sun.jna.Platform;
import io.github.mzmine.util.DotNetUtils;
import io.github.mzmine.util.ShellUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Thin subprocess transport for the C# AgilentReader. Speaks the bridge wire protocol: each request
 * is one line of JSON written to the bridge's stdin; each response is one line of JSON (the
 * "header") on stdout, optionally followed by little-endian {@code float64} binary blobs.
 * <p>
 * This is the Agilent equivalent of {@code MassLynxLib} (which binds an in-process native library);
 * here the vendor code lives in a separate Windows process and we talk to it over stdio.
 */
final class AgilentReaderClient implements AutoCloseable {

  private static final Logger logger = Logger.getLogger(AgilentReaderClient.class.getName());

  /**
   * Path relative to the {@code external_tools} directory.
   */
  private static final String RELATIVE_EXE = "agilent_bridge/AgilentReader.exe";

  /**
   * Native runtime DLLs the Agilent MassHunter SDK pulls in: the mixed-mode {@code BaseTof.dll}
   * (loaded by the MHDAC {@code MassSpecDataReader}) links against the Visual C++ 2013 x64
   * runtime.
   */
  private static final String[] VCPP_2013_RUNTIME_DLLS = {"MSVCR120.dll", "MSVCP120.dll"};

  /**
   * Matches a non-zero {@code Installed} DWORD in {@code reg query} output (hex or decimal).
   */
  private static final Pattern VCPP_INSTALLED_PATTERN = Pattern.compile(
      "(?m)^\\s*Installed\\s+REG_DWORD\\s+(0x0*[1-9a-fA-F][0-9a-fA-F]*|[1-9]\\d*)\\s*$");

  /**
   * True once the .NET and Visual C++ runtime checks passed in this session.
   */
  private static volatile boolean runtimeVerified = false;

  private final Process process;
  private final OutputStream stdin;
  private final BufferedInputStream stdout;
  // the bridge's serializer emits NaN/Infinity as bare tokens (non-standard JSON), e.g. an
  // unavailable frame TIC -> "tic":NaN; accept them rather than failing the whole response.
  private final ObjectMapper mapper = JsonMapper.builder()
      .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS).build();

  AgilentReaderClient() {
    // the runtime checks spawn reg query processes, only run them until they succeed once
    if (!runtimeVerified) {
      ensureRuntimeAvailable();
      ensureVcppRuntimeAvailable(locateExecutable().getParentFile());
      runtimeVerified = true;
    }
    final File exe = locateExecutable();
    try {
      final ProcessBuilder builder = new ProcessBuilder(exe.getAbsolutePath());
      builder.directory(exe.getParentFile()); // so the vendor DLLs next to the exe are found
      process = builder.start();
    } catch (IOException e) {
      throw new RuntimeException("Could not launch AgilentReader at " + exe.getAbsolutePath(), e);
    }
    stdin = process.getOutputStream();
    stdout = new BufferedInputStream(process.getInputStream());
    pumpStderr(process.getErrorStream());
  }

  /**
   * Send a request and return the parsed JSON response header. Any binary blobs that follow must be
   * read by the caller via {@link #readBlob(int)} using counts from the header.
   */
  @NotNull JsonNode send(@NotNull Map<String, Object> request) {
    try {
      final byte[] line = (mapper.writeValueAsString(request) + "\n").getBytes(
          StandardCharsets.UTF_8);
      stdin.write(line);
      stdin.flush();
    } catch (IOException e) {
      throw new RuntimeException("Failed writing request to AgilentReader: " + request, e);
    }

    final String header = readHeaderLine();
    if (header == null) {
      throw new RuntimeException(
          "AgilentReader closed the stream while awaiting a response to " + request.get("op"));
    }
    try {
      final JsonNode node = mapper.readTree(header);
      final JsonNode error = node.get("error");
      if (error != null && !error.isNull()) {
        throw new RuntimeException("AgilentReader error: " + error.asText());
      }
      return node;
    } catch (IOException e) {
      throw new RuntimeException("Failed parsing AgilentReader response: " + header, e);
    }
  }

  /**
   * Read {@code numPoints} little-endian float64 values from the response stream.
   */
  double[] readBlob(int numPoints) {
    if (numPoints <= 0) {
      return new double[0];
    }
    final byte[] bytes = readFully(numPoints * Double.BYTES);
    final double[] out = new double[numPoints];
    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asDoubleBuffer().get(out);
    return out;
  }

  private @Nullable String readHeaderLine() {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream(256);
    try {
      int b;
      while ((b = stdout.read()) != -1) {
        if (b == '\n') {
          return buffer.toString(StandardCharsets.UTF_8);
        }
        if (b != '\r') {
          buffer.write(b);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed reading AgilentReader response header", e);
    }
    return buffer.size() > 0 ? buffer.toString(StandardCharsets.UTF_8) : null;
  }

  private byte[] readFully(int n) {
    final byte[] buf = new byte[n];
    int read = 0;
    try {
      while (read < n) {
        final int r = stdout.read(buf, read, n - read);
        if (r == -1) {
          throw new RuntimeException(
              "AgilentReader stream ended after %d/%d blob bytes".formatted(read, n));
        }
        read += r;
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed reading AgilentReader blob", e);
    }
    return buf;
  }

  private static void pumpStderr(InputStream err) {
    final Thread t = new Thread(() -> {
      try (var reader = new java.io.BufferedReader(
          new java.io.InputStreamReader(err, StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          logger.finest("AgilentReader: " + line);
        }
      } catch (IOException ignored) {
        // stream closed on shutdown
      }
    }, "AgilentReader-stderr");
    t.setDaemon(true);
    t.start();
  }

  /**
   * Verifies the host can run the bridge before launching it: the Agilent MassHunter SDK is
   * Windows-only, and {@code AgilentReader.exe} targets .NET Framework 4.8. Throws a user-facing
   * {@link RuntimeException} with remediation guidance when a prerequisite is missing. Reuses the
   * shared {@link DotNetUtils} check (also used by the SCIEX wiff2/Clearcore import).
   */
  private static void ensureRuntimeAvailable() {
    if (!Platform.isWindows()) {
      throw new RuntimeException(
          "Agilent .d import is only supported on Windows: it relies on the Windows-only Agilent MassHunter SDK.");
    }
    if (!DotNetUtils.isWindowsFrameworkInstalled(DotNetUtils.NET_FRAMEWORK_48_RELEASE_KEY)) {
      throw new RuntimeException(
          "Agilent .d import requires Microsoft .NET Framework 4.8 or later. Please install it from "
              + "https://dotnet.microsoft.com/download/dotnet-framework and restart mzmine.");
    }
  }

  /**
   * Verifies the Visual C++ 2013 x64 runtime ({@code MSVCR120.dll} / {@code MSVCP120.dll}) is
   * available before launching the bridge: the MHDAC {@code MassSpecDataReader} loads the
   * mixed-mode {@code BaseTof.dll}, whose native part links against it, so the bridge fails to open
   * any file without it. These DLLs ship bundled next to the bridge exe (the Windows loader
   * resolves the exe's own directory before {@code System32}); a system-wide install of the
   * redistributable also satisfies the check.
   */
  private static void ensureVcppRuntimeAvailable(@NotNull File exeDir) {
    if (vcppRuntimeDllsPresent(exeDir) || isVcpp2013X64Registered()) {
      return;
    }
    throw new RuntimeException(
        "Agilent .d import requires the Visual C++ 2013 x64 runtime (MSVCR120.dll / MSVCP120.dll), "
            + "used by the Agilent MassHunter SDK. These normally ship alongside the bridge in "
            + "external_tools/agilent_bridge; their absence indicates an incomplete mzmine "
            + "installation. Please reinstall mzmine, or place both DLLs next to AgilentReader.exe.");
  }

  /**
   * Whether the Visual C++ 2013 (Visual Studio 12.0) x64 redistributable is registered as
   * installed. The x64 redist lands in the WOW6432 registry view on 64-bit Windows, so both views
   * are queried.
   */
  private static boolean isVcpp2013X64Registered() {
    final String key = "HKLM\\SOFTWARE\\Microsoft\\VisualStudio\\12.0\\VC\\Runtimes\\x64";
    for (final String view : new String[]{"/reg:64", "/reg:32"}) {
      final String output = ShellUtils.runGetOutput("reg", "query", key, "/v", "Installed", view);
      if (output != null && VCPP_INSTALLED_PATTERN.matcher(output).find()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Whether every required runtime DLL is resolvable by the Windows loader without a system
   * install, i.e. present next to the bridge exe or in {@code System32}.
   */
  private static boolean vcppRuntimeDllsPresent(@NotNull File exeDir) {
    final String windir = System.getenv("WINDIR");
    final File system32 = windir != null ? new File(windir, "System32") : null;
    for (final String dll : VCPP_2013_RUNTIME_DLLS) {
      final boolean nextToExe = new File(exeDir, dll).isFile();
      final boolean inSystem32 = system32 != null && new File(system32, dll).isFile();
      if (!nextToExe && !inSystem32) {
        return false;
      }
    }
    return true;
  }

  private static @NotNull File locateExecutable() {
    final File exe = FileAndPathUtil.resolveInExternalToolsDir(RELATIVE_EXE);
    if (exe != null && exe.isFile()) {
      return exe;
    }
    throw new RuntimeException("Could not find external_tools/" + RELATIVE_EXE);
  }

  @Override
  public void close() {
    try {
      stdin.close();
    } catch (IOException ignored) {
    }
    process.destroy();
  }
}
