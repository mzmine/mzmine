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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.LoggerStream;
import net.sf.mzmine.util.TextUtils;
import net.sf.mzmine.util.R.Rsession.RserverConf;
import net.sf.mzmine.util.R.Rsession.Rsession;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPNull;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

/**
 * @description TODO
 */
public class RSessionWrapper {

    // Logger.
    private static final Logger LOG = Logger.getLogger(RSessionWrapper.class
            .getName());

    private static boolean DEBUG = false;

    // Rsession semaphore - non-parallelizable operations must be synchronized
    // using this semaphore.
    public static final Object R_SESSION_SEMAPHORE = new Object();
    public static Rsession MASTER_SESSION = null;
    private static int MASTER_PORT = -1;
    public static final ArrayList<RSessionWrapper> R_SESSIONS_REG = new ArrayList<RSessionWrapper>();
    public final static String R_HOME_KEY = "R_HOME";
    public static String R_HOME = null;

    private final Object R_DUMMY_SEMAPHORE = new Object();

    private static final Level rsLogLvl = (DEBUG) ? Level.FINEST : Level.OFF;
    private static final Level logLvl = Level.FINEST;
    private static PrintStream logStream = new LoggerStream(LOG, rsLogLvl);

    // Enhanced remote security stuffs.
    private static final String RS_LOGIN = "MZmineUser";
    private static final String RS_DYN_PWD = String.valueOf(java.util.UUID
            .randomUUID());
    private static final String TMP_DIR = System.getProperty("java.io.tmpdir");

    // ----

    // TODO: This variable has become quite useless since using Rsession.eval()
    // => Remove it...
    private Object rEngine = null;

    private String callerFeatureName;
    private String[] reqPackages;
    private String[] reqPackagesVersions;

    private Rsession session;
    // Debug? (shows R eval errors feedback)
    final static private boolean TRY_MODE = false;

    private int rServePid = -1;

    private boolean userCanceled = false;

    // MISC UTILITIES

    // Check if OS is windows.
    public static boolean isWindows() {
        String osname = System.getProperty("os.name");
        return (osname != null && osname.length() >= 7 && osname
                .substring(0, 7).equals("Windows"));
    }

    // Mute output stream utility.
    public static class NullPrintStream extends PrintStream {

        public NullPrintStream() {
            super(new NullByteArrayOutputStream());
        }

        private static class NullByteArrayOutputStream extends
                ByteArrayOutputStream {

            @Override
            public void write(int b) {
                // do nothing
            }

            @Override
            public void write(byte[] b, int off, int len) {
                // do nothing
            }

            @Override
            public void writeTo(OutputStream out) throws IOException {
                // do nothing
            }

        }
    }

    // ** R path utilities
    /**
     * Helper class that consumes output of a process. In addition, it filters
     * output of the REG command on Windows to look for InstallPath registry
     * entry which specifies the location of R.
     */
    static class StreamHog extends Thread {
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

    /**
     * Utility class to consume and eventually redirect system call outputs.
     * 
     */
    static class StreamGobbler extends Thread {

        InputStream is;
        String type;
        OutputStream os;

        StreamGobbler(InputStream is, String type) {
            this(is, type, null);
        }

        StreamGobbler(InputStream is, String type, OutputStream redirect) {
            this.is = is;
            this.type = type;
            this.os = redirect;
        }

        public void run() {
            try {
                PrintWriter pw = null;
                if (os != null) {
                    pw = new PrintWriter(os);
                }

                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line = null;
                while ((line = br.readLine()) != null) {
                    if (pw != null) {
                        pw.println(line);
                    }
                    System.out.println(type + " > " + line);
                }
                if (pw != null) {
                    pw.flush();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    public static String getRexecutablePath() {

        // Get it manually if set in "Project > Preferences".
        String rPath = MZmineCore.getConfiguration().getRexecPath();
        if (rPath != null && rPath != "") {
            File f = new File(rPath);
            if (f.exists())
                return f.getPath();
        }

        // Otherwise: automated attempt...

        // Win: Get R path from registry.
        if (isWindows()) {
            LOG.log(Level.FINEST,
                    "Windows: Query registry to find where R is installed ...");
            String installPath = null;
            try {
                Process rp = Runtime.getRuntime().exec(
                        "reg query HKLM\\Software\\R-core\\R");
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

    public static String getRhomePath() {
        String rPath = getRexecutablePath();
        if (rPath != null) {
            if (RSessionWrapper.isWindows()) {
                return rPath.substring(0, rPath.length() - 10);
            } else {
                return rPath.substring(0, rPath.length() - 5);
            }
        }
        return null;
    }

    // LET'S GET STARTED

    /**
     * Constructor.
     */
    public RSessionWrapper(String callerFeatureName, String[] reqPackages,
            String[] reqPackagesVersions) {

        this.callerFeatureName = callerFeatureName;
        this.reqPackages = reqPackages;
        this.reqPackagesVersions = reqPackagesVersions;
    }

    private void getRengineInstance() throws RSessionWrapperException {

        try {

            String globalFailureMsg = "Could not start Rserve ( R> install.packages(c('Rserve')) ). "
                    + "Please check if R and Rserve are installed and, in "
                    + "case the path to the R installation directory could not be "
                    + "detected automatically, if the 'R executable path' is properly "
                    + "set in the project's preferences. (Note: alternatively, the '"
                    + R_HOME_KEY + "' environment variable can also be used).";

            String r_homeFailureMsg = "Correct path to the R installation directory could not be "
                    + "detected automatically. Please try setting manually "
                    + "the 'R executable path' in the project's preferences. "
                    + "(Note: alternatively, the '"
                    + R_HOME_KEY
                    + "' environment variable can also be used).";

            if (this.rEngine == null) {

                boolean isWindows = RSessionWrapper.isWindows();

                try {

                    synchronized (RSessionWrapper.R_SESSION_SEMAPHORE) {

                        if (R_HOME == null) {
                            R_HOME = System.getenv(R_HOME_KEY);
                        }

                        // If retrieving 'R_HOME' from environment failed, try
                        // to find out automatically. (Since
                        // 'Rsession.newInstanceTry()', checks the environment
                        // first).
                        // @See getRhomePath().
                        if (R_HOME == null || !(new File(R_HOME).exists())) {
                            // Set "R_HOME" system property.
                            R_HOME = RSessionWrapper.getRhomePath();
                            if (R_HOME != null && new File(R_HOME).exists()) {
                                System.setProperty(R_HOME_KEY, R_HOME);
                                LOG.log(logLvl, "'" + R_HOME_KEY + "' set to '"
                                        + System.getProperty(R_HOME_KEY) + "'");
                            }
                        }

                        if (R_HOME == null)
                            throw new RSessionWrapperException(r_homeFailureMsg);

                        // // Security...
                        // Properties props = new Properties();
                        // props.setProperty("remote", "enable");
                        // props.setProperty("auth", "required");

                        // Under *NUX, create the very first Rserve instance
                        // (kind of proxy), designed only to spawn other
                        // (computing) instances (Released at app. exit - see
                        // note below).
                        if (!isWindows
                                && (RSessionWrapper.MASTER_SESSION == null || !checkMasterConnectivity())) {

                            // We absolutely need real new instance on a new
                            // port here (in case other Rserve, not spawned by
                            // MZmine, are running already).
                            // Note: this also fixes potential issues when
                            // running several instances of MZmine concurrently.
                            int port = RserverConf.getNewAvailablePort();
                            RserverConf conf = new RserverConf("localhost",
                                    port, RS_LOGIN, RS_DYN_PWD, null); // props);
                            RSessionWrapper.MASTER_PORT = port;
                            RSessionWrapper.MASTER_SESSION = Rsession
                                    .newInstanceTry(logStream, conf, TMP_DIR);
                            int masterPID = RSessionWrapper.MASTER_SESSION.connection
                                    .eval("Sys.getpid()").asInteger();

                            LOG.log(logLvl,
                                    ">> MASTER Rserve instance created (pid: '"
                                            + masterPID + "' | port '"
                                            + RSessionWrapper.MASTER_PORT
                                            + "').");

                            // Note: no need to 'register()' that particular
                            // instance. It is attached to the Rdaemon which
                            // will die/stop with the app. anyway.
                        }
                    }

                    // Need a new session to be completely instantiated before
                    // asking for another one.
                    // Otherwise, under Windows, the "multi-instance emulation"
                    // system will try several session startup on same port
                    // (aka: each new session port has to be in use/unavailable
                    // before trying to get another one).
                    // Win: Synch with any previous session, if applicable.
                    // *NUX: Synch with nothing that matters.
                    Object rSemaphore = (isWindows) ? RSessionWrapper.R_SESSION_SEMAPHORE
                            : this.R_DUMMY_SEMAPHORE;
                    synchronized (rSemaphore) {

                        RserverConf conf;
                        if (isWindows) {
                            // Win: Need to get a new port every time.
                            int port = RserverConf.getNewAvailablePort();
                            conf = new RserverConf("localhost", port, RS_LOGIN,
                                    RS_DYN_PWD, null); // props);
                        } else {
                            // *NUX: Just fit/target the MASTER instance.
                            conf = RSessionWrapper.MASTER_SESSION.rServeConf;
                        }

                        // Then, spawn a new computing instance.
                        if (isWindows) {
                            // Win: Figure out a new standalone instance every
                            // time.
                            this.session = Rsession.newInstanceTry(logStream,
                                    conf, TMP_DIR);
                        } else {
                            // *NUX: Just spawn a new connection on MASTER
                            // instance.
                            // Need to target the same port, in case another
                            // Rserve (not started by this MZmine instance) is
                            // running.
                            // We need to keep constantly a hand on what is
                            // spawned by the app. to remain able to shutdown
                            // everything related to it and it only when exiting
                            // (gracefully or not).
                            // Rsession.newLocalInstance(logStream, null);
                            this.session = Rsession.newRemoteInstance(
                                    logStream, conf, TMP_DIR);
                        }

                        if (this.session == null)
                            throw new IllegalArgumentException(globalFailureMsg);

                        this.register();

                    }

                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    // Redirect undeclared exceptions thrown by "Rsession"
                    // library to regular one.
                    throw new RSessionWrapperException(globalFailureMsg);
                }

                // As "Rsession.newInstanceTry()" runs an Rdaemon Thread. It is
                // scheduled already, meaning the session will be opened even
                // for "WAITING" tasks, in any case, and even if it's been
                // meanwhile canceled.
                // Consequently, we need to kill it after the instance has been
                // created, since trying to abort the instance (close the
                // session) before it exists would result in no termination at
                // all.
                if (this.session != null) {

                    if (this.session.connection != null) {
                        // Keep an opened instance and store the related PID.
                        this.rServePid = this.session.connection.eval(
                                "Sys.getpid()").asInteger();
                        this.rEngine = this.session.connection;
                        LOG.log(logLvl, "Rserve: started instance (pid: '"
                                + this.rServePid + "' | port: '"
                                + this.session.rServeConf.port + "').");

                        // Quick test
                        LOG.log(logLvl,
                                ((RConnection) this.rEngine).eval(
                                        "R.version.string").asString());
                        LOG.log(logLvl,
                                ((RConnection) this.rEngine).getServerVersion()
                                        + "");
                        LOG.log(logLvl, RConnection.transferCharset);
                    }
                    if (this.userCanceled) {
                        this.close(true);
                        return;
                    }

                }

            }
        } catch (Throwable t) {
            throw new RSessionWrapperException(TextUtils.wrapText(
                    t.getMessage(), 80));
        }
    }

    public void loadPackage(String packageName) throws RSessionWrapperException {

        String loadCode = "library(" + packageName + ", logical.return = TRUE)";

        if (TRY_MODE)
            loadCode = "try(" + loadCode + ", silent=TRUE)";

        String errorMsg = "The \""
                + this.callerFeatureName
                + "\" requires "
                + "the \""
                + packageName
                + "\" R package, which couldn't be loaded - is it installed in R?";

        if (this.session != null && !this.userCanceled) {
            LOG.log(logLvl, "Loading package '" + packageName + "'...");
            int loaded = 0;
            try {

                REXP r = ((RConnection) this.rEngine).eval(loadCode);
                if (r.inherits("try-error")) {
                    LOG.severe("R Error [0]: " + r.asString());
                    LOG.severe("R eval attempt [0]: " + loadCode);
                }
                loaded = r.asInteger();
                LOG.log(logLvl, "Load status: '" + (loaded != 0) + "'.");

            } catch (RserveException | REXPMismatchException e) {
                LOG.log(logLvl, "Loaded package KO: '" + e.getMessage() + "'.");
                // Remain silent if eval KO ("server down").
                loaded = Integer.MIN_VALUE;
            }

            // Throw loading failure only if eval OK, but return FALSE
            // (package not loaded).
            // ("server down" case will be handled soon enough).
            if (loaded == 0)
                if (!this.userCanceled)
                    throw new RSessionWrapperException(errorMsg);

            LOG.log(logLvl, "Loaded package: '" + packageName + "'.");
        }
    }

    public void checkPackageVersion(String packageName, String version)
            throws RSessionWrapperException {

        String checkVersionCode = "packageVersion('" + packageName + "') >= '"
                + version + "\'";

        if (TRY_MODE)
            checkVersionCode = "try(" + checkVersionCode + ", silent=TRUE)";

        String errorMsg = "The \""
                + this.callerFeatureName
                + "\" requires "
                + "the \""
                + packageName
                + "\" R package, which was found, but is too old? - please update '"
                + packageName + "' to version " + version + " or later.";

        if (this.session != null && !this.userCanceled) {
            LOG.log(logLvl, "Checking package version: '" + packageName
                    + "' for version '" + version + "'...");
            int version_ok = 0;
            try {
                version_ok = ((RConnection) this.rEngine)
                        .eval(checkVersionCode).asInteger();
            } catch (RserveException | REXPMismatchException e) {
                // Remain silent if eval KO ("server down").
                version_ok = Integer.MIN_VALUE;
            }

            // Throw version failure only if eval OK (package too old).
            // ("server down" case will be handled soon enough).
            if (version_ok == 0)
                if (!this.userCanceled)
                    throw new RSessionWrapperException(errorMsg);

            LOG.log(logLvl, "Checked package version: '" + packageName
                    + "' for version '" + version + "'.");
        }
    }

    public void loadAndCheckRequiredPackages() throws RSessionWrapperException {

        // Prerequisites...

        if (this.reqPackages == null)
            return;

        if (this.reqPackagesVersions != null && this.reqPackages.length > 0
                && this.reqPackages.length != this.reqPackagesVersions.length) {
            if (!this.userCanceled)
                throw new IllegalStateException(
                        "'reqPackages' and 'reqPackagesVersions' arrays must be the same length!");
        }

        //
        for (int i = 0; i < this.reqPackages.length; ++i) {

            // Load.
            this.loadPackage(this.reqPackages[i]);

            // Check version.
            // - Pass null as a version array to skip all version checks.
            // - Pass null as a version to skip version check for given 'i'
            // package.
            if (this.reqPackagesVersions == null
                    || this.reqPackagesVersions.length == 0
                    || this.reqPackagesVersions[i] == null) {
                continue;
            }
            this.checkPackageVersion(this.reqPackages[i],
                    this.reqPackagesVersions[i]);

        }
    }

    public static class InputREXPFactory {

        public static <T> REXP getREXP(T object) {

            REXP x = null;

            // // First check if we have primitive (single or array) or Object
            // boolean isPrimitiveOrWrapped =
            // ClassUtils.isPrimitiveOrWrapper(object.getClass());

            if (object instanceof Integer) {
                x = new REXPInteger((Integer) object);
            } else if (object instanceof int[]) {
                x = new REXPInteger((int[]) object);
            } else if (object instanceof Double) {
                x = new REXPDouble((Double) object);
                // Double matrix case.
            } else if (object instanceof double[][]) {
                x = REXP.createDoubleMatrix((double[][]) object);
                // Double vector cases.
            } else if (object instanceof double[]) {
                x = new REXPDouble((double[]) object);
            } else if (object instanceof String) {
                x = new REXPString((String) object);
            } else if (object instanceof String[]) {
                x = new REXPString((String[]) object);
            } else if (object == null) {
                x = new REXPNull();
            }

            return x;
        }
    }

    public static class OutputObjectFactory {

        public static <T> Object getObject(REXP rexp)
                throws REXPMismatchException {

            Object o = null;

            if (rexp instanceof REXPInteger) {
                int[] obj = rexp.asIntegers();
                if (obj == null)
                    return null;

                if (obj.length == 0)
                    o = null;
                else if (obj.length == 1)
                    o = obj[0];
                else
                    o = obj;
            } else if (rexp instanceof REXPDouble) {
                // Double matrix case (nrow > 1).
                // dim = [nrow, ncol]
                //if (rexp.hasAttribute("dim")) {
                if (rexp.dim() != null && rexp.dim()[0] > 1) {
                    o = rexp.asDoubleMatrix();
                }
                // Double vector case.
                else {
                    double[] obj = rexp.asDoubles();
                    if (obj == null)
                        return null;

                    if (obj.length == 0)
                        o = null;
                    else if (obj.length == 1)
                        o = obj[0];
                    else
                        o = obj;
                }
            } else if (rexp instanceof REXPString) {
                // o = rexp.asString();
                String[] obj = rexp.asStrings();
                if (obj == null)
                    return null;

                if (obj.length == 0)
                    o = null;
                else if (obj.length == 1)
                    o = obj[0];
                else
                    o = obj;
            } else if (rexp instanceof REXPNull) {
                o = null;
            }

            return o;
        }
    }

    public <T> void assign(String objName, T object)
            throws RSessionWrapperException {

        if (this.session != null && !this.userCanceled) {
            String msg = "Rserve error: couldn't assign R object '" + objName
                    + "' (instance '" + this.getPID() + "').";
            try {
                ((RConnection) this.rEngine).assign(objName,
                        InputREXPFactory.getREXP(object));
            } catch (REngineException e) {
                throw new RSessionWrapperException(msg);
            } catch (Exception e) {
                throw new RSessionWrapperException(e.getMessage());
            }
        }
    }

    // Check connectivity in case outside event broke it.
    // Required since we're using "Rsession"'s eval() which is damn silent.
    // TODO: [May be ??] better modify the way Rsession works:
    // (@See Rsession.silentlyEval() and @See Rsession.silentlyVoidEval())
    // This actual checkConnectivity() function does an additional call to
    // Rserve which could probably be avoided.
    private void checkConnectivity() throws RSessionWrapperException {

        checkConnectivity(((RConnection) this.rEngine));
    }

    private boolean checkMasterConnectivity() throws RSessionWrapperException {

        try {
            checkConnectivity(RSessionWrapper.MASTER_SESSION.connection);
            return true;
        } catch (RSessionWrapperException e) {
            return false;
        }
    }

    private void checkConnectivity(RConnection con)
            throws RSessionWrapperException {

        if (!this.userCanceled) {

            String msg = "Rserve connectivity failure.";
            try {
                con.assign("dummy", new REXPNull());// voidEval("0");
            } catch (RserveException e) {
                throw new RSessionWrapperException(msg);
            } catch (Exception e) {
                throw new RSessionWrapperException(msg);
            }
        }
    }

    /**
     * 
     * @param rCode
     * @throws RSessionWrapperException
     */
    public boolean eval(String rCode) throws RSessionWrapperException {
        return eval(rCode, true);
    }

    // Some weird behavior ("SIGPIPE signal error") was observed while using
    // basic "Rconnection.eval()", so, switch to use "Rsession"'s one...
    public boolean eval(String rCode, boolean stopOnError)
            throws RSessionWrapperException {

        // if (TRY_MODE) rCode = "try(" + rCode + ",silent=TRUE)";

        boolean ok = false;
        if (this.session != null && !this.userCanceled) {
            String msg = "Rserve error: couldn't eval R code '" + rCode
                    + "' (instance '" + this.getPID() + "').";
            // try {
            // ////((RConnection) this.rEngine).eval(rCode);
            // REXP r = ((RConnection) this.rEngine).eval(rCode);
            // if (r2.inherits("try-error")) {
            // LOG.severe("R Error [1]: " + r.asString());
            // LOG.severe("R eval attempt [1]: " + rCode);
            // LOG.severe("Debug string" + r.toDebugString());
            // }
            // //else { /* success ... */ }
            // LOG.severe("R error [3]: " + ((RConnection)
            // this.rEngine).getLastError());
            // }
            // catch (RserveException e) {
            // LOG.severe("R error [2]: " + getErrMessage());
            // throw new RSessionWrapperException(msg);
            // } catch (Exception e) {
            // throw new RSessionWrapperException(e.getMessage());
            // }

            ok = this.session.voidEval(rCode, true); // TRY_MODE);
            if (!ok) {
                if (stopOnError)
                    throw new RSessionWrapperException(msg);
                else
                    checkConnectivity();
            }
        }
        return ok;
    }

    public String getErrMessage() throws RSessionWrapperException {
        try {
            return ((RConnection) this.rEngine).eval("geterrmessage()")
                    .asString();
        } catch (RserveException e) {
            throw new RSessionWrapperException(
                    "Rserve error: couldn't get R error messages.");
        } catch (Exception e) {
            throw new RSessionWrapperException(e.getMessage());
        }
    }

    public Object collect(String obj) throws RSessionWrapperException {
        return collect(obj, true);
    }

    /**
     * Casting the result to the correct type is left to the user.
     * 
     * @param obj
     *            String expression or object name.
     * @return
     * @throws RSessionWrapperException
     */
    public Object collect(String obj, boolean stopOnError)
            throws RSessionWrapperException {

        Object object = null;

        if (this.session != null && !this.userCanceled) {

            String msg = "Rserve error: couldn't collect result for R expression '"
                    + obj + "' (instance '" + this.getPID() + "').";
            try {

                REXP r = this.session.eval(obj, true);
                if (r == null) {
                    if (stopOnError)
                        throw new RSessionWrapperException(msg);
                    else
                        checkConnectivity();
                }

                object = OutputObjectFactory.getObject(r);
            } catch (/* RserveException | */REXPMismatchException e) {
                throw new RSessionWrapperException(msg);
            } catch (Exception e) {
                throw new RSessionWrapperException(e.getMessage());
            }
        }

        return object;
    }

    public void open() throws RSessionWrapperException {

        // Redirect 'Rsession' gossiping on standard outputs to logger.
        System.setOut(logStream);
        System.setErr(logStream);

        // Do nothing if session was canceled.
        if (!this.userCanceled) {

            // Load R engine.
            getRengineInstance();

            // Load & check required R packages.
            loadAndCheckRequiredPackages();
        }
    }

    /**
     * This can be necessary to call 'close()' from a different thread than the
     * one which called 'open()', sometimes, with Rserve (if the related
     * instance is busy).
     * 
     * @param userCanceled
     *            Tell the application the closure came from a user action
     *            rather than from an unknown source error.
     * @throws RSessionWrapperException
     */
    public void close(boolean userCanceled) throws RSessionWrapperException {

        this.userCanceled = userCanceled;

        if (this.session != null) {

            try {

                LOG.log(logLvl, "Rserve: try terminate "
                        + ((this.rServePid == -1) ? "pending" : "")
                        + " session"
                        + ((this.rServePid == -1) ? "..." : " (pid: '"
                                + this.rServePid + "' | port: '"
                                + this.session.rServeConf.port + "')..."));

                // Avoid 'Rsession' to 'printStackTrace' while catching
                // 'SocketException'
                // (since we are about to brute force kill the Rserve instance,
                // such that
                // the session won't end properly).
                RSessionWrapper.muteStdOutErr();
                {
                    RSessionWrapper.killRserveInstance(this);
                    this.session.end();
                }
                RSessionWrapper.unMuteStdOutErr();

                LOG.log(logLvl, "Rserve: terminated "
                        + ((this.rServePid == -1) ? "pending" : "")
                        + " session"
                        + ((this.rServePid == -1) ? "..." : " (pid: '"
                                + this.rServePid + "' | port: '"
                                + this.session.rServeConf.port + "')..."));

                // Release session (prevents from calling close again on a
                // closed instance).
                this.session = null;

            } catch (Throwable t) {
                // Adapt/refactor message accordingly to the way the termination
                // was provoked:
                // User requested or unexpectedly...
                String msg;
                if (userCanceled) {
                    msg = "Rserve error: couldn't terminate instance with pid '"
                            + this.rServePid + "'. Details:\n";
                } else {
                    msg = "Rserve error: something when wrong with instance of pid '"
                            + this.rServePid + "'. Details:\n";
                }
                throw new RSessionWrapperException(msg
                        + TextUtils.wrapText(t.getMessage(), 80));

            } finally {
                // Make sure to restore standard outputs.
                System.setOut(System.out);
                System.setErr(System.err);
            }
        }

        this.unRegister();
    }

    private void register() {
        RSessionWrapper.R_SESSIONS_REG.add(this);
    }

    private void unRegister() {
        RSessionWrapper.R_SESSIONS_REG.remove(this);
    }

    /**
     * Keep logging clean. Turn off standard outputs, since 'Rsession' library
     * is way far too talkative on them.
     */
    private static void muteStdOutErr() {
        System.setOut(new NullPrintStream());
        System.setErr(new NullPrintStream());
        logStream = new NullPrintStream();
    }

    /**
     * Restore standard outputs.
     */
    private static void unMuteStdOutErr() {
        System.setOut(System.out);
        System.setErr(System.err);
        logStream = new LoggerStream(LOG, rsLogLvl);
    }

    public static void killRserveInstance(RSessionWrapper rSession)
            throws RSessionWrapperException {

        if (rSession != null && rSession.getPID() != -1) {
            // Win: faster to brute force kill the process (avoids
            // "Rsession.newInstanceTry()"
            // to attempt to recover the connection).
            if (RSessionWrapper.isWindows()) {
                try {
                    // Working but damn slow.
                    // // BEGIN OK
                    // Rsession s = Rsession.newInstanceTry(logStream, null);
                    // s.eval("tools::pskill("+ rSession.getPID() + ")");
                    // LOG.info("Eval: " + "tools::pskill("+ rSession.getPID() +
                    // ")");
                    // s.end();
                    // // END OK

                    // ... Using 'TASKKILL' instead ...
                    // FileOutputStream fos_out = new
                    // FileOutputStream("output.txt");
                    // FileOutputStream fos_err = new
                    // FileOutputStream("error.txt");
                    OutputStream os_err = new OutputStream() {
                        private StringBuilder string = new StringBuilder();

                        @Override
                        public void write(int b) throws IOException {
                            this.string.append((char) b);
                        }

                        @Override
                        public String toString() {
                            return this.string.toString();
                        }
                    };

                    Process proc = new ProcessBuilder("TASKKILL", "/PID", ""
                            + rSession.getPID(), "/F").start();
                    StreamGobbler errorGobbler = new StreamGobbler(
                            proc.getErrorStream(), "Error", os_err); // ,
                    // fos_err);
                    StreamGobbler outputGobbler = new StreamGobbler(
                            proc.getInputStream(), "Output", System.out); // ,
                    // fos_out);

                    // Consume outputs.
                    errorGobbler.start();
                    outputGobbler.start();

                    // Any error while processing 'TASKKILL'? (expected '0').
                    int exitVal = proc.waitFor();
                    // System.out.println("ExitValue: " + exitVal);
                    if (exitVal != 0)
                        throw new RSessionWrapperException(
                                "Killing Rserve instance of PID '"
                                        + rSession.getPID() + "'"
                                        + " failed. \n" + os_err.toString());
                    // fos_out.flush(); fos_out.close();
                    // fos_err.flush(); fos_err.close();
                } catch (Exception e) { // IOException | InterruptedException
                    // Silent.
                }
            }
            // *NUX: For portability reasons, we prefer asking R to terminate
            // the targeted instance
            // rather than using a call such as 'kill -9 pid' (even if this
            // would work in most cases).
            else {
                try {
                    final RConnection c2 = new RConnection(); // session.connection;
                    // SIGTERM might not be understood everywhere: so using
                    // explicitly SIGKILL signal, as well.
                    if (c2 != null && c2.isConnected()) {
                        c2.eval("tools::pskill(" + rSession.getPID() + ")"); // win
                        c2.eval("tools::pskill(" + rSession.getPID()
                                + ", tools::SIGKILL)"); // *nux
                        c2.close();
                    }
                } catch (RserveException e) {
                    throw new RSessionWrapperException(e.getMessage());
                }
            }
        }
    }

    public int getPID() {
        return this.rServePid;
    }

    public Rsession getSession() {
        return this.session;
    }

    public boolean isSessionRunning() {
        return (this.session != null && !this.userCanceled);
    }

    public static void CleanAll() {

        // Cleanup Rserve instances.
        for (int i = RSessionWrapper.R_SESSIONS_REG.size() - 1; i >= 0; --i) {
            try {
                if (RSessionWrapper.R_SESSIONS_REG.get(i) != null) {
                    LOG.info("CleanAll / instance: "
                            + RSessionWrapper.R_SESSIONS_REG.get(i).getPID());
                    RSessionWrapper.R_SESSIONS_REG.get(i).close(true);
                }
            } catch (RSessionWrapperException e) {
                // Silent.
            }
        }

    }

}

