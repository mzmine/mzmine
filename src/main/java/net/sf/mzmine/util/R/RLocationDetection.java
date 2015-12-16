/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.util.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.google.common.base.Strings;

import net.sf.mzmine.main.MZmineCore;

/**
 * A utility class for obtaining R executable location
 */
public class RLocationDetection {

    private static final Logger LOG = Logger
            .getLogger(RLocationDetection.class.getName());

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
                LOG.warning("R executable location set to '" + rExecPath
                        + "' in MZmine preferences, but this file is not executable");
            LOG.finest("R path set to '" + rExecPath
                    + "' based on MZmine preferences");
            return rExecPath;
        }

        // Second attempt - check the environment variable
        String rHome = System.getenv(R_HOME_ENV_KEY);
        if (!Strings.isNullOrEmpty(rHome)) {
            rExecPath = rHome + File.separator + "bin" + File.separator + "R"
                    + (System.getProperty("os.name").contains("Win") ? ".exe"
                            : "");

            LOG.finest("R executable location value set to '" + rExecPath
                    + "' based on the '" + R_HOME_ENV_KEY
                    + "' environment variable");
            return rExecPath;
        }

        // Third attempt - autodetection
        rExecPath = autodetectRExecutable();
        if (!Strings.isNullOrEmpty(rExecPath)) {
            LOG.finest(
                    "R executable location auto-detected as '" + rExecPath + "'");
            return rExecPath;
        }

        LOG.warning("No R installation found");
        return null;

    }

    /**
     * Auto-detects the location of R executable
     */
    private static @Nullable String autodetectRExecutable() {

        // Win: Get R path from registry.
        if (isWindows()) {
            LOG.log(Level.FINEST,
                    "Windows: Query registry to find where R is installed ...");
            String installPath = null;
            try {
                Process rp = Runtime.getRuntime()
                        .exec("reg query HKLM\\Software\\R-core\\R");
                StreamHog regHog = new StreamHog(rp.getInputStream(), true);
                rp.waitFor();
                regHog.join();
                installPath = regHog.getInstallPath();
            } catch (Exception rge) {
                LOG.log(Level.SEVERE,
                        "ERROR: Unable to run REG to find the location of R: "
                                + rge);
                return null;
            }
            if (installPath == null) {
                LOG.log(Level.SEVERE,
                        "ERROR: Cannot find path to R. Make sure reg is available"
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
     * Helper class that consumes output of a process. In addition, it filters
     * output of the REG command on Windows to look for InstallPath registry
     * entry which specifies the location of R.
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
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(is));
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
                            LOG.log(Level.FINEST, "R InstallPath = " + s);
                        }
                    } else
                        LOG.log(Level.FINEST, "Rserve > " + line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
