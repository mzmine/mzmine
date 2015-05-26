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

/*
 * Original author: Yann Richet - https://github.com/yannrichet/rsession
 */

package net.sf.mzmine.util.R.Rsession;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.rosuda.REngine.Rserve.RConnection;

/** helper class that consumes output of a process. In addition, it filter output of the REG command on Windows to look for InstallPath registry entry which specifies the location of R. */
class RegistryHog extends Thread {

    InputStream is;
    boolean capture;
    String installPath;

    RegistryHog(InputStream is, boolean capture) {
        this.is = is;
        this.capture = capture;
        start();
    }

    public String getInstallPath() {
        return installPath;
    }

    public void run() {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(is));
            String line = null;
            while ((line = br.readLine()) != null) {
                if (capture) { // we are supposed to capture the output from REG command

                    int i = line.indexOf("InstallPath");
                    if (i >= 0) {
                        String s = line.substring(i + 11).trim();
                        int j = s.indexOf("REG_SZ");
                        if (j >= 0) {
                            s = s.substring(j + 6).trim();
                        }
                        installPath = s;
                        //System.out.println("R InstallPath = " + s);
                    }
                } else {
                    //System.out.println("Rserve>" + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}

class StreamHog extends Thread {

    InputStream is;
    boolean capture;
    StringBuffer out = new StringBuffer();

    StreamHog(InputStream is, boolean capture) {
        this.is = is;
        this.capture = capture;
        start();
    }

    public String getOutput() {
        return out.toString();
    }

    public void run() {
        //System.err.println("start streamhog");
        BufferedReader br = null;
        InputStreamReader isr = null;
        try {
            isr = new InputStreamReader(is);
            br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null) {
                if (capture) {
                    out.append("\n").append(line);
                } else {
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            if (isr != null) {
                try {
                    isr.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        //System.err.println("finished streamhog");
    }
}

/** simple class that start Rserve locally if it's not running already - see mainly <code>checkLocalRserve</code> method. It spits out quite some debugging outout of the console, so feel free to modify it for your application if desired.<p>
<i>Important:</i> All applications should shutdown every Rserve that they started! Never leave Rserve running if you started it after your application quits since it may pose a security risk. Inform the user if you started an Rserve instance.
 */
public class StartRserve {

    /** R batch to check Rserve is installed
     * @param Rcmd command necessary to start R
     * @return Rserve is already installed
     */
    public static boolean isRserveInstalled(String Rcmd) {
        StringBuffer result = new StringBuffer();
        boolean done = doInR("i=installed.packages();is.element(set=i,el='Rserve')", Rcmd, "--vanilla -q", result, result);
        if (!done) {
            return false;
        }
        //System.err.println("output=\n===========\n" + result.toString() + "\n===========\n");
        if (result.toString().contains("TRUE")) {
            return true;
        } else {
            return false;
        }
    }

    /** R batch to install Rserve
     * @param Rcmd command necessary to start R
     * @param http_proxy http://login:password@proxy:port string to enable internet access to rforge server
     * @return success
     */
    public static boolean installRserve(String Rcmd, String http_proxy, String repository) {
        if (repository == null || repository.length() == 0) {
            repository = Rsession.DEFAULT_REPOS;
        }
        System.err.print("Install Rserve from " + repository + " ... (http_proxy=" + http_proxy + ") ");
        boolean ok = doInR((http_proxy != null ? "Sys.setenv(http_proxy=" + http_proxy + ");" : "") + "install.packages('Rserve',repos='" + repository + "')", Rcmd, "--vanilla", null, null);
        if (!ok) {
            System.err.println("failed");
            return false;
        }
        int n = 5;
        while (n > 0) {
            try {
                Thread.sleep(10000 / n);
                System.err.print(".");
            } catch (InterruptedException ex) {
            }
            if (isRserveInstalled(Rcmd)) {
                System.err.println("ok");
                return true;
            }
            n--;
        }
        System.err.println("failed");
        return false;
    }

    /** attempt to start Rserve. Note: parameters are <b>not</b> quoted, so avoid using any quotes in arguments
    @param todo command to execute in R
    @param Rcmd command necessary to start R
    @param rargs arguments are are to be passed to R (e.g. --vanilla -q)
    @return <code>true</code> if Rserve is running or was successfully started, <code>false</code> otherwise.
     */
    public static boolean doInR(String todo, String Rcmd, String rargs, StringBuffer out, StringBuffer err) {
        try {
            Process p;
            boolean isWindows = false;
            String osname = System.getProperty("os.name");
            String command = null;
            if (osname != null && osname.length() >= 7 && osname.substring(0, 7).equals("Windows")) {
                isWindows = true; /* Windows startup */
                command = "\"" + Rcmd + "\" -e \"" + todo + "\" " + rargs;
                //System.out.println("e=" + e);
                p = Runtime.getRuntime().exec(command);
            } else /* unix startup */ {
                command = "echo \"" + todo + "\"|" + Rcmd + " " + rargs;
                //System.out.println("e=" + e);
                p = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", command});
            }
            System.err.println("  executing " + command);
            // we need to fetch the output - some platforms will die if you don't ...
            StreamHog error = new StreamHog(p.getErrorStream(), (err != null));
            StreamHog output = new StreamHog(p.getInputStream(), (out != null));
            if (err != null) {
                error.join();
            }
            if (out != null) {
                output.join();
            }
            if (!isWindows) /* on Windows the process will never return, so we cannot wait */ {
                p.waitFor();
            }
            if (out != null) {
                out.append(output.getOutput());
            }
            if (err != null) {
                err.append(error.getOutput());
            }
        } catch (Exception x) {
            return false;
        }
        return true;
    }

    /** shortcut to <code>launchRserve(cmd, "--no-save --slave", "--no-save --slave", false)</code> */
    public static boolean launchRserve(String cmd) {
        StringBuffer RserveArgs = new StringBuffer("--no-save --slave ");      
        if (!System.getProperty("os.name").contains("Win"))
        	RserveArgs.append(" --RS-pidfile \\'" /*+ System.getProperty("user.dir")*/ + "/tmp" + "/rs_pid.pid\\'");
        return launchRserve(cmd, /*null,*/ "--no-save --slave", RserveArgs.toString(), false);
    }

    /** attempt to start Rserve. Note: parameters are <b>not</b> quoted, so avoid using any quotes in arguments
    @param cmd command necessary to start R
    @param rargs arguments are are to be passed to R
    @param rsrvargs arguments to be passed to Rserve
    @return <code>true</code> if Rserve is running or was successfully started, <code>false</code> otherwise.
     */
    public static boolean launchRserve(String cmd, /*String libloc,*/ String rargs, String rsrvargs, boolean debug) {
        System.err.println("Waiting for Rserve to start ...");
        //boolean startRserve = doInR("library(" + /*(libloc != null ? "lib.loc='" + libloc + "'," : "") +*/ "Rserve);Rserve(" + (debug ? "TRUE" : "FALSE") + ",args='" + rsrvargs + "')", cmd, rargs, null, null);
        boolean startRserve = doInR("library(Rserve);Rserve(" + (debug ? "TRUE" : "FALSE") + ",args='" + rsrvargs + "')", cmd, rargs, null, null);
        if (startRserve) {
            System.err.println("Rserve startup done, let us try to connect ...");
        } else {
            System.err.println("Failed to start Rserve process.");
            return false;
        }

        int attempts = 5; /* try up to 5 times before giving up. We can be conservative here, because at this point the process execution itself was successful and the start up is usually asynchronous */
        while (attempts > 0) {
            try {
                RConnection c = null;
                int port = -1;
                if (rsrvargs.contains("--RS-port")) {
                    String rsport = rsrvargs.split("--RS-port")[1].trim().split(" ")[0];
                    port = Integer.parseInt(rsport);
                    c = new RConnection("localhost", port);
                } else {
                    c = new RConnection("localhost");
                }
                System.err.println("Rserve is running.");
                c.close();
                return true;
            } catch (Exception e2) {
                System.err.println("Try failed with: " + e2.getMessage());
            }
            /* a safety sleep just in case the start up is delayed or asynchronous */
            try {
                Thread.sleep(500);
            } catch (InterruptedException ix) {
            }

            attempts--;
        }
        return false;
    }

    /** checks whether Rserve is running and if that's not the case it attempts to start it using the defaults for the platform where it is run on. This method is meant to be set-and-forget and cover most default setups. For special setups you may get more control over R with <<code>launchRserve</code> instead. */
    public static boolean checkLocalRserve() {
        if (isRserveRunning()) {
            return true;
        }
        String osname = System.getProperty("os.name");
        if (osname != null && osname.length() >= 7 && osname.substring(0, 7).equals("Windows")) {
            System.err.println("Windows: query registry to find where R is installed ...");
            String installPath = null;
            try {
                Process rp = Runtime.getRuntime().exec("reg query HKLM\\Software\\R-core\\R");
                RegistryHog regHog = new RegistryHog(rp.getInputStream(), true);
                rp.waitFor();
                regHog.join();
                installPath = regHog.getInstallPath();
            } catch (Exception rge) {
                System.err.println("ERROR: unable to run REG to find the location of R: " + rge);
                return false;
            }
            if (installPath == null) {
                System.err.println("ERROR: canot find path to R. Make sure reg is available and R was installed with registry settings.");
                return false;
            }
            return launchRserve(installPath + "\\bin\\R.exe");
        }
        return (launchRserve("R")
                || /* try some common unix locations of R */ ((new File("/Library/Frameworks/R.framework/Resources/bin/R")).exists() && launchRserve("/Library/Frameworks/R.framework/Resources/bin/R"))
                || ((new File("/usr/local/lib/R/bin/R")).exists() && launchRserve("/usr/local/lib/R/bin/R"))
                || ((new File("/usr/lib/R/bin/R")).exists() && launchRserve("/usr/lib/R/bin/R"))
                || ((new File("/usr/local/bin/R")).exists() && launchRserve("/usr/local/bin/R"))
                || ((new File("/sw/bin/R")).exists() && launchRserve("/sw/bin/R"))
                || ((new File("/usr/common/bin/R")).exists() && launchRserve("/usr/common/bin/R"))
                || ((new File("/opt/bin/R")).exists() && launchRserve("/opt/bin/R")));
    }

    /** check whether Rserve is currently running (on local machine and default port).
    @return <code>true</code> if local Rserve instance is running, <code>false</code> otherwise
     */
    public static boolean isRserveRunning() {
        try {
            RConnection c = new RConnection();
            System.err.println("Rserve is running.");
            c.close();
            return true;
        } catch (Exception e) {
            System.err.println("First connect try failed with: " + e.getMessage());
        }
        return false;
    }

    /** just a demo main method which starts Rserve and shuts it down again */
    public static void main(String[] args) {
        System.err.println("result=" + checkLocalRserve());
        try {
            RConnection c = new RConnection();
            c.shutdown();
        } catch (Exception x) {
        }
    }
}
