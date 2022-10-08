/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.util.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jetbrains.annotations.Nullable;

import com.google.common.base.Strings;

import io.github.mzmine.main.MZmineCore;

/**
 * A utility class for obtaining R executable location
 */
public class RLocationDetection {

  private static final Logger logger = Logger.getLogger(RLocationDetection.class.getName());

  public final static String R_HOME_ENV_KEY = "R_HOME";

  /**
   * Returns the R executable path, by testing options (in this order):
   * 
   * 1. User setting in MZMine preferences
   * 
   * 2. Environment variable R_HOME + /bin/R or \bin\R.exe
   * 
   * 3. Auto-detection based on platform
   * 
   * Returns null if no R installation was found.
   * 
   */
  public static @Nullable String getRExecutablePath() {

    String rExecPath;

    // First attempt - check the R location setting in "Project >
    // Preferences".
    rExecPath = MZmineCore.getConfiguration().getRexecPath();
    if (!Strings.isNullOrEmpty(rExecPath)) {
      final File rExecFile = new File(rExecPath);
      if (!rExecFile.canExecute())
        logger.warning("R executable location set to '" + rExecPath
            + "' in MZmine preferences, but this file is not executable");
      logger.finest("R path set to '" + rExecPath + "' based on MZmine preferences");
      return rExecPath;
    }

    // Second attempt - check the environment variable
    String rHome = System.getenv(R_HOME_ENV_KEY);
    if (!Strings.isNullOrEmpty(rHome)) {
      rExecPath = rHome + File.separator + "bin" + File.separator + "R"
          + (System.getProperty("os.name").contains("Win") ? ".exe" : "");

      logger.finest("R executable location value set to '" + rExecPath + "' based on the '"
          + R_HOME_ENV_KEY + "' environment variable");
      return rExecPath;
    }

    // Third attempt - autodetection
    rExecPath = autodetectRExecutable();
    if (!Strings.isNullOrEmpty(rExecPath)) {
      logger.finest("R executable location auto-detected as '" + rExecPath + "'");
      return rExecPath;
    }

    logger.warning("No R installation found");
    return null;

  }

  public static @Nullable String getRScriptExecutablePath() {

    String rExecPath = getRExecutablePath();
    return ((rExecPath != null)
        ? rExecPath.replaceAll("bin" + File.separator + "R", "bin" + File.separator + "Rscript")
        : null);

  }

  /**
   * Auto-detects the location of R executable
   */
  private static @Nullable String autodetectRExecutable() {

    // Win: Get R path from registry.
    if (isWindows()) {
      logger.log(Level.FINEST, "Windows: Query registry to find where R is installed ...");
      String installPath = null;
      try {
        Process rp = Runtime.getRuntime().exec("reg query HKLM\\Software\\R-core\\R");
        StreamHog regHog = new StreamHog(rp.getInputStream(), true);
        rp.waitFor();
        regHog.join();
        installPath = regHog.getInstallPath();
      } catch (Exception rge) {
        logger.log(Level.SEVERE, "ERROR: Unable to run REG to find the location of R: " + rge);
        return null;
      }
      if (installPath == null) {
        logger.log(Level.SEVERE, "ERROR: Cannot find path to R. Make sure reg is available"
            + " and R was installed with registry settings.");
        return null;
      }
      File f = new File(installPath);
      return ((f.exists()) ? installPath + "\\bin\\R.exe" : null);
    }

    // Mac OSX.
    File f = new File("/Library/Frameworks/R.framework/Resources/bin/R");
    if (f.exists())
      return f.getPath();

    // *NUX.
    f = new File("/usr/local/lib/R/bin/R");
    if (f.exists())
      return f.getPath();
    f = new File("/usr/lib/R/bin/R");
    if (f.exists())
      return f.getPath();
    f = new File("/sw/bin/R");
    if (f.exists())
      return f.getPath();
    f = new File("/usr/common/bin/R");
    if (f.exists())
      return f.getPath();
    f = new File("/opt/bin/R");
    if (f.exists())
      return f.getPath();

    return null;

  }

  /**
   * Returns true if running on MS Windows.
   */
  private static boolean isWindows() {
    final String osname = System.getProperty("os.name");
    return ((osname != null) && (osname.length() >= 7)
        && (osname.substring(0, 7).toLowerCase().equals("windows")));
  }

  /**
   * Helper class that consumes output of a process. In addition, it filters output of the REG
   * command on Windows to look for InstallPath registry entry which specifies the location of R.
   */
  private static class StreamHog extends Thread {
    InputStream is;
    boolean capture;
    String installPath;

    StreamHog(InputStream is, boolean capture) {
      this.is = is;
      this.capture = capture;
      start();
    }

    public String getInstallPath() {
      return installPath;
    }

    public void run() {
      try {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line = null;
        while ((line = br.readLine()) != null) {
          if (capture) { // we are supposed to capture the output from
            // REG command
            int i = line.indexOf("InstallPath");
            if (i >= 0) {
              String s = line.substring(i + 11).trim();
              int j = s.indexOf("REG_SZ");
              if (j >= 0)
                s = s.substring(j + 6).trim();
              installPath = s;
              logger.log(Level.FINEST, "R InstallPath = " + s);
            }
          } else
            logger.log(Level.FINEST, "Rserve > " + line);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

}
