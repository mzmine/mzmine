/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.io.import_rawdata_shimadzu;

import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Lifecycle wrapper around the {@code ShimadzuBridge.exe} child process. The
 * bridge is launched on construction, kept alive for the import session, and
 * shut down via {@link #close()}.
 */
final class ShimadzuBridgeProcess implements AutoCloseable {

  private static final Logger logger = Logger.getLogger(ShimadzuBridgeProcess.class.getName());

  private static final String EXE_NAME = "ShimadzuBridge.exe";
  private static final String EXE_SUBDIR = "shimadzu_wrapper";

  private final Process process;
  private final ShimadzuProtocol protocol;
  private final Thread stderrPump;
  private final Thread shutdownHook;

  ShimadzuBridgeProcess() throws IOException {
    final File exe = FileAndPathUtil.resolveInExternalToolsDir(EXE_SUBDIR + "/" + EXE_NAME);
    if (!exe.isFile()) {
      throw new IOException("ShimadzuBridge.exe not found at " + exe
          + ". Build the C# project from git/shimadzu_wrapper/ and copy"
          + " bin\\x64\\Release\\* into external_tools/" + EXE_SUBDIR + "/");
    }

    final ProcessBuilder pb = new ProcessBuilder(List.of(exe.getAbsolutePath()));
    pb.directory(exe.getParentFile());
    pb.redirectErrorStream(false);

    this.process = pb.start();
    final InputStream stdout = new BufferedInputStream(process.getInputStream(), 1 << 16);
    final OutputStream stdin = new BufferedOutputStream(process.getOutputStream(), 1 << 12);
    this.protocol = new ShimadzuProtocol(stdout, stdin);

    this.stderrPump = startStderrPump(process.getErrorStream());

    // Belt-and-suspenders: if the JVM exits without us closing cleanly, kill
    // the child so we don't orphan it.
    this.shutdownHook = new Thread(this::destroyIfAlive, "shimadzu-bridge-shutdown");
    Runtime.getRuntime().addShutdownHook(shutdownHook);
  }

  @NotNull ShimadzuProtocol protocol() {
    return protocol;
  }

  private Thread startStderrPump(InputStream err) {
    final Thread t = new Thread(() -> {
      try (var reader = new BufferedReader(new InputStreamReader(err, StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          if (line.startsWith("[ERROR]")) {
            logger.log(Level.WARNING, "ShimadzuBridge: " + line);
          } else {
            logger.log(Level.FINE, "ShimadzuBridge: " + line);
          }
        }
      } catch (IOException ignored) {
        // child went away
      }
    }, "shimadzu-bridge-stderr");
    t.setDaemon(true);
    t.start();
    return t;
  }

  private void destroyIfAlive() {
    if (process != null && process.isAlive()) {
      process.destroyForcibly();
    }
  }

  @Override
  public void close() {
    try {
      protocol.send(ShimadzuProtocol.shutdownRequest());
    } catch (IOException ignored) {
      // child may already be dead; fall through to wait/destroy
    }
    try {
      if (!process.waitFor(5, TimeUnit.SECONDS)) {
        logger.warning("ShimadzuBridge did not exit within 5s; forcing termination");
        process.destroyForcibly();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      process.destroyForcibly();
    }
    try {
      Runtime.getRuntime().removeShutdownHook(shutdownHook);
    } catch (IllegalStateException ignored) {
      // JVM is already shutting down
    }
  }
}
