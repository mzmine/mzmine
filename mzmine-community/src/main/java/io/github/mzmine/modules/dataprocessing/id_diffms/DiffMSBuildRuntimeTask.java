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

import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;

public class DiffMSBuildRuntimeTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(DiffMSBuildRuntimeTask.class.getName());
  private final @Nullable Runnable onFinished;
  private String description = "Initializing DiffMS runtime build...";

  public DiffMSBuildRuntimeTask() {
    this(null);
  }

  public DiffMSBuildRuntimeTask(@Nullable Runnable onFinished) {
    super(null, Instant.now());
    this.onFinished = onFinished;
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  public double getFinishedPercentage() {
    return finishedPercentage;
  }

  private double finishedPercentage = 0;

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    try {
      final File runtimeDir = FileAndPathUtil.resolveInExternalToolsDir("python-runtimes/diffms");
      if (runtimeDir == null || !runtimeDir.isDirectory()) {
        throw new IOException("DiffMS runtime build scripts not found in " 
            + (runtimeDir != null ? runtimeDir.getAbsolutePath() : "python-runtimes/diffms")
            + ". Please check MZmine installation.");
      }

      final File userDiffMsDir = new File(FileAndPathUtil.getMzmineDir(), "diffms");
      final File userPacksDir = new File(userDiffMsDir, "runtime-packs");
      if (!userPacksDir.exists() && !userPacksDir.mkdirs()) {
        throw new IOException("Could not create output directory: " + userPacksDir.getAbsolutePath());
      }

      // 1. Check for Micromamba
      File micromambaExe = findOrDownloadMicromamba(userDiffMsDir);

      // 2. Run the build script
      description = "Building DiffMS runtime environment (this may take a few minutes)...";
      runBuildScript(runtimeDir, userPacksDir, micromambaExe);

      // 3. Extract and initialize runtimes
      description = "Extracting and initializing runtimes...";
      DiffMSRuntimeManager.ensureRuntimeAndGetPython(DiffMSRuntimeManager.Variant.CPU, this::isCanceled);
      
      // On non-macOS platforms, we also built CUDA, so extract it as well
      final String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
      if (!os.contains("mac")) {
        try {
          DiffMSRuntimeManager.ensureRuntimeAndGetPython(DiffMSRuntimeManager.Variant.CUDA, this::isCanceled);
        } catch (Exception e) {
          logger.warning("Could not extract CUDA runtime after build: " + e.getMessage());
        }
      }

      description = "DiffMS runtime build and extraction completed.";
      setStatus(TaskStatus.FINISHED);

      if (onFinished != null) {
        onFinished.run();
      }
    } catch (Exception e) {
      error("DiffMS runtime build failed: " + e.getMessage(), e);
    }
  }

  private File findOrDownloadMicromamba(File userDiffMsDir) throws IOException, InterruptedException {
    // Check if on PATH
    if (canExecute("micromamba")) {
      return new File("micromamba"); // rely on PATH
    }

    // Check local user dir
    File binDir = new File(userDiffMsDir, "bin");
    File localExe = new File(binDir, isWindows() ? "micromamba.exe" : "micromamba");
    if (localExe.isFile() && localExe.canExecute()) {
      finishedPercentage = 0.05;
      return localExe;
    }

    // Download
    description = "Downloading micromamba...";
    finishedPercentage = 0.01;
    if (!binDir.exists() && !binDir.mkdirs()) {
      throw new IOException("Could not create bin directory: " + binDir.getAbsolutePath());
    }

    String url = getMicromambaDownloadUrl();
    logger.info("Downloading micromamba from " + url);
    
    // Using a simple download. For production, consider Hash verification.
    try (InputStream in = URI.create(url).toURL().openStream()) {
      Files.copy(in, localExe.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    if (!isWindows()) {
      localExe.setExecutable(true);
    }

    finishedPercentage = 0.05;
    return localExe;
  }

  private String getMicromambaDownloadUrl() {
    String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
    String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
    boolean isArm = arch.contains("arm") || arch.contains("aarch64");

    if (os.contains("win")) {
      return "https://github.com/mamba-org/micromamba-releases/releases/latest/download/micromamba-win-64";
    } else if (os.contains("mac")) {
      return isArm 
          ? "https://github.com/mamba-org/micromamba-releases/releases/latest/download/micromamba-osx-arm64"
          : "https://github.com/mamba-org/micromamba-releases/releases/latest/download/micromamba-osx-64";
    } else {
      // Linux
      return isArm 
          ? "https://github.com/mamba-org/micromamba-releases/releases/latest/download/micromamba-linux-aarch64"
          : "https://github.com/mamba-org/micromamba-releases/releases/latest/download/micromamba-linux-64";
    }
  }

  private void runBuildScript(File runtimeDir, File outputDir, File micromambaExe) throws IOException, InterruptedException {
    boolean isWin = isWindows();
    String scriptName = isWin ? "build_runtime_packs.ps1" : "build_runtime_packs.sh";
    File script = new File(runtimeDir, scriptName);
    
    if (!script.isFile()) {
      throw new IOException("Build script not found: " + script.getAbsolutePath());
    }
    if (!isWin) {
      script.setExecutable(true);
    }

    ProcessBuilder pb;
    if (isWin) {
      pb = new ProcessBuilder("powershell.exe", "-ExecutionPolicy", "Bypass", "-File", script.getAbsolutePath(), "-OutputDirectory", outputDir.getAbsolutePath());
    } else {
      pb = new ProcessBuilder("bash", script.getAbsolutePath(), outputDir.getAbsolutePath());
    }

    pb.directory(runtimeDir);
    // Pass micromamba path env var
    if (micromambaExe.isAbsolute()) {
        pb.environment().put("MICROMAMBA_EXE", micromambaExe.getAbsolutePath());
    } else {
        // If it was "micromamba" from PATH, we don't strictly need to set it if it's on PATH,
        // but setting it is safer if the script relies on the env var override we added.
        pb.environment().put("MICROMAMBA_EXE", "micromamba");
    }

    pb.redirectErrorStream(true);

    Process p = pb.start();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (isCanceled()) {
          p.destroy();
          break;
        }
        if (line.startsWith("PROGRESS: ")) {
          try {
            finishedPercentage = Double.parseDouble(line.substring(10).trim()) / 100.0;
          } catch (NumberFormatException e) {
            // ignore
          }
          continue;
        }
        logger.info("[DiffMS Build] " + line);
        // We could parse progress here if the script outputs it
      }
    }

    int exitCode = p.waitFor();
    if (exitCode != 0) {
      throw new IOException("Build script failed with exit code " + exitCode);
    }
  }

  private boolean canExecute(String cmd) {
    try {
      ProcessBuilder pb = new ProcessBuilder(isWindows() ? List.of("where", cmd) : List.of("which", cmd));
      Process p = pb.start();
      return p.waitFor() == 0;
    } catch (Exception e) {
      return false;
    }
  }

  private boolean isWindows() {
    return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
  }
}
