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

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.REXPList;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPNull;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.RList;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RFileInputStream;
import org.rosuda.REngine.Rserve.RFileOutputStream;
import org.rosuda.REngine.Rserve.RserveException;

public class Rsession implements Logger {

    public static final String HEAD_TRY = "";// -try- ";
    public boolean TRY_MODE_DEFAULT = true;
    public boolean TRY_MODE = false;
    public static final String CAST_ERROR = "Cannot cast ";
    private static final String _PACKAGE_ = "  package ";
    public RConnection connection;
    Logger console;
    boolean tryLocalRServe;
    public static final String PACKAGEINSTALLED = "Package installed.";
    public static final String PACKAGELOADED = "Package loaded.";
    public boolean connected = false;
    static String separator = ",";
    public final static int MinRserveVersion = 103;
    Rdaemon localRserve;
    public RserverConf rServeConf;
    public final static String STATUS_NOT_SET = "Unknown status",
            STATUS_READY = "Ready", STATUS_ERROR = "Error",
            STATUS_ENDED = "End", STATUS_NOT_CONNECTED = "Not connected",
            STATUS_CONNECTING = "Connecting...";
    public String status = STATUS_NOT_SET;
    // <editor-fold defaultstate="collapsed" desc="Add/remove interfaces">
    List<Logger> loggers;
    public boolean debug;

    // GLG HACK:
    private static String tmpDir = null;
    public static final Object R_SESSION_SEMAPHORE = new Object();
    public static ArrayList<Integer> PORTS_REG = new ArrayList<Integer>();
    //** GLG HACK: Logging fix **//
    boolean SINK_OUTPUT = true;
    // GLG HACK: fixed sink file in case of multiple instances
    // (Appending the port number of the instance to file name)
    String SINK_FILE_BASE = ".Rout";
    String SINK_FILE = null;
    String lastOuput = "";


    void cleanupListeners() {
        if (loggers != null) {
            while (!loggers.isEmpty()) {
                removeLogger(loggers.get(0));
            }
        }
        if (busy != null) {
            while (!busy.isEmpty()) {
                removeBusyListener(busy.get(0));
            }
        }
        if (updateObjects != null) {
            while (!updateObjects.isEmpty()) {
                removeUpdateObjectsListener(updateObjects.get(0));
            }
        }
        if (eval != null) {
            while (!eval.isEmpty()) {
                removeEvalListener(eval.get(0));
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    public void addLogger(Logger l) {
        if (!loggers.contains(l)) {
            // System.out.println("+ logger " + l.getClass().getSimpleName());
            loggers.add(l);
        }
    }

    public void removeLogger(Logger l) {
        if (loggers.contains(l)) {
            l.close();
            loggers.remove(l);
        }
    }

    public void close() {
        for (Logger l : loggers) {
            l.close();
        }
    }

    public void println(String message, Logger.Level level) {
        if (level == Level.ERROR) {
            try {
                message = message + "\n R> " + getLastLogEntry() + "\n R! "
                        + getLastError();
            } catch (Exception e) {
                message = message + "\n ! " + e.getMessage();
            }
        }
        // System.out.println("println " + message+
        // " in "+loggers.size()+" loggers.");
        for (Logger l : loggers) {
            // System.out.println("  log in " + l.getClass().getSimpleName());
            l.println(message, level);
        }
    }

    List<BusyListener> busy = new LinkedList<BusyListener>();

    public void addBusyListener(BusyListener b) {
        if (!busy.contains(b)) {
            busy.add(b);
        }
    }

    public void removeBusyListener(BusyListener b) {
        if (busy.contains(b)) {
            busy.remove(b);
        }
    }

    public void setBusy(boolean bb) {
        for (BusyListener b : busy) {
            b.setBusy(bb);
        }

    }

    List<UpdateObjectsListener> updateObjects = new LinkedList<UpdateObjectsListener>();

    public void addUpdateObjectsListener(UpdateObjectsListener b) {
        if (!updateObjects.contains(b)) {
            updateObjects.add(b);
        }
    }

    public void removeUpdateObjectsListener(UpdateObjectsListener b) {
        if (updateObjects.contains(b)) {
            b.setTarget(null);
            updateObjects.remove(b);
        }
    }

    List<EvalListener> eval = new LinkedList<EvalListener>();

    public void addEvalListener(EvalListener b) {
        if (!eval.contains(b)) {
            eval.add(b);
        }
    }

    public void removeEvalListener(EvalListener b) {
        if (eval.contains(b)) {
            eval.remove(b);
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed"
    // desc="Conveniency static String methods">
    public static String toString(Object o) {
        if (o == null) {
            return "NULL";
        } else if (o instanceof double[]) {
            return cat((double[]) o);
        } else if (o instanceof double[][]) {
            return cat((double[][]) o);
        } else if (o instanceof int[]) {
            return cat((int[]) o);
        } else if (o instanceof int[][]) {
            return cat((int[][]) o);
        } else if (o instanceof Object[]) {
            return cat((Object[]) o);
        } else if (o instanceof Object[][]) {
            return cat((Object[][]) o);
        } else if (o instanceof RList) {
            return cat((RList) o);
        } else {
            return o.toString();
        }
    }

    public static String cat(RList list) {
        try {
            StringBuffer sb = new StringBuffer("\t");
            double[][] data = new double[list.names.size()][];
            for (int i = 0; i < list.size(); i++) {
                String n = list.keyAt(i);
                sb.append(n + "\t");
                data[i] = list.at(n).asDoubles();
            }
            sb.append("\n");
            for (int i = 0; i < data[0].length; i++) {
                sb.append((i + 1) + "\t");
                for (int j = 0; j < data.length; j++) {
                    sb.append(data[j][i] + "\t");
                }
                sb.append("\n");
            }
            return sb.toString();
        } catch (REXPMismatchException r) {
            return "(Not a numeric dataframe)\n"
                    + new REXPList(list).toDebugString();
        }
    }

    public static String cat(double[] array) {
        if (array == null || array.length == 0) {
            return "NA";
        }

        String o = array[0] + "";
        if (array.length > 1) {
            for (int i = 1; i < array.length; i++) {
                o += (separator + (array[i] + ""));
            }
        }
        return o;
    }

    public static String cat(int[] array) {
        if (array == null || array.length == 0) {
            return "NA";
        }

        String o = array[0] + "";
        if (array.length > 1) {
            for (int i = 1; i < array.length; i++) {
                o += (separator + (array[i] + ""));
            }
        }
        return o;
    }

    public static String cat(double[][] array) {
        if (array == null || array.length == 0 || array[0].length == 0) {
            return "NA";
        }

        String o = cat(array[0]);
        if (array.length > 1) {
            for (int i = 1; i < array.length; i++) {
                o += "\n" + cat(array[i]);
            }
        }
        return o;
    }

    public static String cat(int[][] array) {
        if (array == null || array.length == 0 || array[0].length == 0) {
            return "NA";
        }

        String o = cat(array[0]);
        if (array.length > 1) {
            for (int i = 1; i < array.length; i++) {
                o += "\n" + cat(array[i]);
            }
        }
        return o;
    }

    public static String cat(Object[] array) {
        if (array == null || array.length == 0 || array[0] == null) {
            return "";
        }

        String o = array[0].toString();
        if (array.length > 1) {
            for (int i = 1; i < array.length; i++) {
                o += (separator + (array[i] == null ? "" : array[i].toString()));
            }
        }

        return o;
    }

    public static String cat(String sep, String[] array) {
        if (array == null || array.length == 0 || array[0] == null) {
            return "";
        }

        String o = array[0].toString();
        if (array.length > 1) {
            for (int i = 1; i < array.length; i++) {
                o += (sep + (array[i] == null ? "" : array[i].toString()));
            }
        }

        return o;
    }

    public static String cat(Object[][] array) {
        if (array == null || array.length == 0 || array[0].length == 0) {
            return "NA";
        }

        String o = cat(array[0]);
        if (array.length > 1) {
            for (int i = 1; i < array.length; i++) {
                o += "\n" + cat(array[i]);
            }
        }
        return o;
    }

    // </editor-fold>

    /**
     * Map java File object to R path (as string)
     * 
     * @param path
     *            java File object
     */
    public static String toRpath(File path) {
        return toRpath(path.getAbsolutePath());
    }

    /**
     * Map java path to R path (as string)
     * 
     * @param path
     *            java string path
     */
    public static String toRpath(String path) {
        return path.replaceAll("\\\\", "/");
    }

    /**
     * Build a new local Rsession
     * 
     * @param console
     *            PrintStream for R output
     * @param localRProperties
     *            properties to pass to R (eg http_proxy or R libpath)
     */
    public static Rsession newLocalInstance(final Logger console,
            Properties localRProperties, String tmpDirectory) {
        return new Rsession(console,
                RserverConf.newLocalInstance(localRProperties), false,
                tmpDirectory);
    }

    /**
     * Build a new remote Rsession
     * 
     * @param console
     *            PrintStream for R output
     * @param serverconf
     *            RserverConf server configuration object, giving IP, port,
     *            login, password, properties to pass to R (eg http_proxy or R
     *            libpath)
     */
    public static Rsession newRemoteInstance(final Logger console,
            RserverConf serverconf, String tmpDirectory) {
        return new Rsession(console, serverconf, false, tmpDirectory);
    }

    /**
     * Build a new Rsession. Fork to local spawned Rsession if given remote one
     * failed to initialized.
     * 
     * @param console
     *            PrintStream for R output
     * @param serverconf
     *            RserverConf server configuration object, giving IP, port,
     *            login, password, properties to pass to R (eg http_proxy)
     */
    public static Rsession newInstanceTry(final Logger console,
            RserverConf serverconf, String tmpDirectory) {
        return new Rsession(console, serverconf, true, tmpDirectory);
    }

    /**
     * Build a new local Rsession
     * 
     * @param console
     *            PrintStream for R output
     * @param localRProperties
     *            properties to pass to R (eg http_proxy or R libpath)
     */
    public static Rsession newLocalInstance(PrintStream pconsole,
            Properties localRProperties, String tmpDirectory) {
        return new Rsession(pconsole,
                RserverConf.newLocalInstance(localRProperties), false,
                tmpDirectory);
    }

    /**
     * Build a new remote Rsession
     * 
     * @param console
     *            PrintStream for R output
     * @param serverconf
     *            RserverConf server configuration object, giving IP, port,
     *            login, password, properties to pass to R (eg http_proxy or R
     *            libpath)
     */
    public static Rsession newRemoteInstance(PrintStream pconsole,
            RserverConf serverconf, String tmpDirectory) {
        return new Rsession(pconsole, serverconf, false, tmpDirectory);
    }

    /**
     * Build a new Rsession. Fork to local spawned Rsession if given remote one
     * failed to initialized.
     * 
     * @param console
     *            PrintStream for R output
     * @param serverconf
     *            RserverConf server configuration object, giving IP, port,
     *            login, password, properties to pass to R (eg http_proxy)
     */
    public static Rsession newInstanceTry(PrintStream pconsole,
            RserverConf serverconf, String tmpDirectory) {
        return new Rsession(pconsole, serverconf, true, tmpDirectory);
    }

    /**
     * create a new Rsession.
     * 
     * @param console
     *            PrintStream for R output
     * @param serverconf
     *            RserverConf server configuration object, giving IP, port,
     *            login, password, properties to pass to R (eg http_proxy or R
     *            libpath)
     * @param tryLocalRServe
     *            local spawned Rsession if given remote one failed to
     *            initialized
     */
    public Rsession(final Logger console, RserverConf serverconf,
            boolean tryLocalRServe, String tmpDirectory) {

        tmpDir = tmpDirectory;

        this.console = console;
        rServeConf = serverconf;
        this.tryLocalRServe = tryLocalRServe;

        loggers = new LinkedList<Logger>();
        loggers.add(console);
        
        // Make sink file specific to current Rserve instance
        SINK_FILE = SINK_FILE_BASE + "-" + serverconf.port;

        startup();
    }

    /**
     * create rsession using System as a logger
     */
    public Rsession(final PrintStream p, RserverConf serverconf,
            boolean tryLocalRServe, String tmpDirectory) {
        this(new Logger() {

            public void println(String string, Level level) {
                if (level == Level.WARNING) {
                    p.print("! ");
                } else if (level == Level.ERROR) {
                    p.print("!! ");
                }
                p.println(string);
            }

            public void close() {
                p.close();
            }
        }, serverconf, tryLocalRServe, tmpDirectory);
    }

    /**
     * create rsession using System as a logger
     */
    public Rsession(RserverConf serverconf, boolean tryLocalRServe,
            String tmpDirectory) {
        this(new Logger() {

            public void println(String string, Level level) {
                if (level == Level.INFO) {
                    System.out.println(string);
                } else {
                    System.err.println(string);
                }
            }

            public void close() {
            }
        }, serverconf, tryLocalRServe, tmpDirectory);
    }

    void startup() {
        if (rServeConf == null) {
            if (tryLocalRServe) {
                rServeConf = RserverConf.newLocalInstance(null);
                println("No Rserve conf given. Trying to use "
                        + rServeConf.toString(), Level.WARNING);
                begin(true);
            } else {
                println("No Rserve conf given. Failed to start session.",
                        Level.ERROR);
                status = STATUS_ERROR;
            }
        } else {
            begin(tryLocalRServe);
        }
    }

    /**
     * @return status of Rsession
     */
    public String getStatus() {
        return status;
    }

    void begin(boolean tryLocal) {

        // GLG HACK:
        synchronized (Rsession.R_SESSION_SEMAPHORE) {
            if (!Rsession.PORTS_REG.contains(Integer.valueOf(rServeConf.port)))
                PORTS_REG.add(Integer.valueOf(rServeConf.port));
        }

        status = STATUS_NOT_CONNECTED;

        /*
         * if (RserveConf == null) { RserveConf =
         * RserverConf.newLocalInstance(null);
         * println("No Rserve conf given. Trying to use " +
         * RserveConf.toString()); }
         */
        status = STATUS_CONNECTING;

        connection = rServeConf.connect();
        connected = (connection != null);

        if (!connected) {
            status = STATUS_ERROR;
            String message = "Rserve " + rServeConf + " is not accessible.";
            println(message, Level.ERROR);
        } else if (connection.getServerVersion() < MinRserveVersion) {
            status = STATUS_ERROR;
            String message = "Rserve " + rServeConf + " version is too old.";
            println(message, Level.ERROR);
        } else {
            status = STATUS_READY;
            return;
        }

        if (tryLocal) {// try a local start of Rserve

            status = STATUS_CONNECTING;

            // GLG HACK: Why not allowing a local instance to behave like a
            // remote one?
            // (using port, login and password ...)
            // *****RserveConf =
            // RserverConf.newLocalInstance(RserveConf.properties);
            int port_tmp = rServeConf.port;
            int port = (rServeConf.port > 0 && RserverConf
                    .isPortAvailable(rServeConf.port)) ? rServeConf.port
                    : RserverConf.getNewAvailablePort();
            // // Allow to get a new port only if it was unspecified in the
            // first place.
            // int port = (rServeConf.port != -1) ? rServeConf.port :
            // RserverConf.getNewAvailablePort();
            if (port_tmp != port) {
                // Keep sink file specific to current Rserve instance
                SINK_FILE = SINK_FILE_BASE + "-" + port;
                println("WARNING: Changed the original requested port from "
                        + port_tmp + " to " + port + "!", Level.WARNING);
            }
            rServeConf = new RserverConf(RserverConf.DEFAULT_RSERVE_HOST, port,
                    rServeConf.login, rServeConf.password, null);

            println("Trying to spawn " + rServeConf.toString(), Level.INFO);

            localRserve = new Rdaemon(rServeConf, this);
            String http_proxy = null;
            if (rServeConf != null && rServeConf.properties != null
                    && rServeConf.properties.containsKey("http_proxy")) {
                http_proxy = rServeConf.properties.getProperty("http_proxy");
            }
            localRserve.start(http_proxy, tmpDir);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }

            connection = rServeConf.connect();
            connected = (connection != null);

            if (!connected) {// failed !

                String message2 = "Failed to launch local Rserve. Unable to initialize Rsession.";
                println(message2, Level.ERROR);
                System.err.println(message2);
                throw new IllegalArgumentException(message2);
            } else {
                println("Local Rserve started. (Version "
                        + connection.getServerVersion() + ")", Level.INFO);
            }

        }
        // if (r.getServerVersion() < MinRserveVersion) {
        // throw new IllegalArgumentException("RServe version too low: " +
        // r.getServerVersion() + "\n  Rserve >= 0.6 needed.");
        // }

    }

    // RSession previous;

    /**
     * correctly (depending on execution platform) shutdown Rsession.
     */
    public void end() {
        if (connection == null) {
            log("Void session terminated.", Level.INFO);
            cleanupListeners();
            return;
        }
        if ((!System.getProperty("os.name").contains("Win"))
                && localRserve != null) {// if ((!UNIX_OPTIMIZE ||
                                         // System.getProperty("os.name").contains("Win"))
                                         // && localRserve != null) {
            log("Ending local session...", Level.INFO);
            localRserve.stop();
            connection.close();
        } else {
            log("Ending remote session...", Level.INFO);
            connection.close();
        }

        log("Session teminated.", Level.INFO);

        connection = null;
        cleanupListeners();

        // GLG HACK:
        synchronized (Rsession.R_SESSION_SEMAPHORE) {
            if (Rsession.PORTS_REG.contains(Integer.valueOf(rServeConf.port)))
                PORTS_REG.remove(Integer.valueOf(rServeConf.port));
        }

    }

    public static final boolean UNIX_OPTIMIZE = true;
    String lastmessage = "";
    int repeated = 0;

    public String getLastLogEntry() {
        return lastmessage;
    }

    public String getLastError() {
        return connection.getLastError();
    }

    public void log(String message, Level level) {
        if (message != null && message.trim().length() > 0
                && !message.trim().equals("\n") && level == Level.OUTPUT) {
            println(message, level);
        } else {
            if (message == null) {
                return;
            } else {
                message = message.trim();
            }
            if (message.equals(lastmessage) && repeated < 100) {
                repeated++;
            } else {
                if (repeated > 0) {
                    println("    Repeated " + repeated + " times.", level);
                    repeated = 0;
                    lastmessage = message;
                    println(message, level);
                } else {
                    lastmessage = message;
                    println(message, level);
                }
            }
        }
    }

    /**
     * @return available R commands
     */
    public String[] listCommands() {
        silentlyEval(".keyWords <- function() {n <- length(search());result <- c();for (i in 1:n) {result <- c(result,ls(pos=i,all.names=TRUE))}; result}");
        REXP rexp = silentlyEval(".keyWords()");
        String as[] = null;
        try {
            if (rexp != null && (as = rexp.asStrings()) != null) {
                return as;
            } else {
                return null;
            }
        } catch (REXPMismatchException ex) {
            log(HEAD_ERROR + ex.getMessage() + "\n  listCommands()",
                    Level.ERROR);
            return null;
        }
    }

    // <editor-fold defaultstate="collapsed" desc="Packages management">
    public static String DEFAULT_REPOS = "http://cran.irsn.fr/";
    public String repos = DEFAULT_REPOS;

    /**
     * @param url
     *            CRAN repository to use for packages installation (eg
     *            http://cran.r-project.org)
     */
    public void setCRANRepository(String url) {
        repos = url;
    }

    /**
     * @return CRAN repository used for packages installation
     */
    public String getCRANRepository() {
        return repos;
    }

    private static String loadedpacks = "loadedpacks";

    /**
     * Check for package loaded in R environment.
     * 
     * @param pack
     *            R package name
     * @return package loading status
     */
    public boolean isPackageLoaded(String pack) {
        silentlyVoidEval(loadedpacks + "<-.packages()", false);
        boolean isloaded = false;
        try {
            REXP i = silentlyEval("is.element(set=" + loadedpacks + ",el='"
                    + pack + "')");
            if (i != null) {
                isloaded = i.asInteger() == 1;
            }
        } catch (REXPMismatchException ex) {
            log(HEAD_ERROR + ex.getMessage()
                    + "\n  isPackageLoaded(String pack=" + pack + ")",
                    Level.ERROR);
        }
        if (isloaded) {
            log(_PACKAGE_ + pack + " is loaded.", Level.INFO);
        } else {
            log(_PACKAGE_ + pack + " is not loaded.", Level.INFO);
        }

        // silentlyEval("rm(" + loadedpacks + ")");
        return isloaded;
    }

    private static String packs = "packs";

    /**
     * Check for package installed in R environment.
     * 
     * @param pack
     *            R package name
     * @param version
     *            R package version
     * @return package loading status
     */
    public boolean isPackageInstalled(String pack, String version) {
        silentlyVoidEval(packs + "<-installed.packages(noCache=TRUE)", false);
        boolean isinstalled = false;
        REXP r = silentlyEval("is.element(set=" + packs + ",el='" + pack + "')");
        try {
            if (r != null) {
                isinstalled = (r.asInteger() == 1);
            } else {
                log(HEAD_ERROR + "Could not list installed packages"
                        + "\n  isPackageInstalled(String pack=" + pack
                        + ", String version=" + version + ")", Level.ERROR);
            }
        } catch (REXPMismatchException ex) {
            log(HEAD_ERROR + ex.getMessage()
                    + "\n  isPackageInstalled(String pack=" + pack
                    + ", String version=" + version + ")", Level.ERROR);
        }
        if (isinstalled) {
            log(_PACKAGE_ + pack + " is installed.", Level.INFO);
        } else {
            log(_PACKAGE_ + pack + " is not installed.", Level.INFO);
        }

        if (isinstalled && version != null && version.length() > 0) {
            try {
                isinstalled = silentlyEval(
                        packs + "['" + pack + "','Version'] == \"" + version
                                + "\"").asInteger() == 1;
            } catch (REXPMismatchException ex) {
                log(HEAD_ERROR + ex.getMessage()
                        + "\n  isPackageInstalled(String pack=" + pack
                        + ", String version=" + version + ")", Level.ERROR);
            }
            try {
                log("    version of package "
                        + pack
                        + " is "
                        + silentlyEval(packs + "['" + pack + "','Version']")
                                .asString(), Level.INFO);
            } catch (REXPMismatchException ex) {
                log(HEAD_ERROR + ex.getMessage()
                        + "\n  isPackageInstalled(String pack=" + pack
                        + ", String version=" + version + ")", Level.ERROR);
            }
            if (isinstalled) {
                log(_PACKAGE_ + pack + " (" + version + ") " + " is installed.",
                        Level.INFO);
            } else {
                log(_PACKAGE_ + pack + " (" + version + ") "
                        + " is not installed.", Level.INFO);
            }

        }
        // silentlyEval("rm(" + packs + ")");
        return isinstalled;
    }

    /**
     * Start installation procedure of R packages
     * 
     * @param pack
     *            packages to install
     * @param load
     *            automatically load packages after successfull installation
     * @return installation status
     */
    public String installPackages(String[] pack, boolean load) {
        String resall = "";
        for (String pv : pack) {
            String res = installPackage(pv, load);
            if (load) {
                if (!res.equals(PACKAGELOADED)) {
                    resall += "\n" + res;
                }
            } else {
                if (!res.equals(PACKAGEINSTALLED)) {
                    resall += "\n" + res;
                }
            }
        }
        if (resall.length() > 0) {
            return resall;
        } else {
            return load ? PACKAGELOADED : PACKAGEINSTALLED;
        }
    }

    /**
     * Start installation procedure of local R package
     * 
     * @param pack
     *            package file to install
     * @param load
     *            automatically load package after successfull installation
     * @return installation status
     */
    public String installPackage(File pack, boolean load) {
        sendFile(pack);
        eval("install.packages('" + pack.getName() + "',repos=NULL,"
                + /*
                   * (RserveConf.RLibPath == null ? "" : "lib=" +
                   * RserveConf.RLibPath + ",") +
                   */"dependencies=TRUE)");
        log("  request package " + pack + " install...", Level.INFO);

        String name = pack.getName();
        if (name.contains("_")) {
            name = name.substring(0, name.indexOf("_"));
        }
        if (name.contains(".")) {
            name = name.substring(0, name.indexOf("."));
        }

        if (isPackageInstalled(name, null)) {
            log(_PACKAGE_ + pack + " installation sucessfull.", Level.INFO);
            if (load) {
                return loadPackage(name);
            } else {
                return PACKAGEINSTALLED;
            }
        } else {
            log(_PACKAGE_ + pack + " installation failed.", Level.ERROR);
            if (load) {
                return loadPackage(name);
            } else {
                return "Impossible to install package " + pack + " !";
            }
        }
    }

    /**
     * Start installation procedure of local R package
     * 
     * @param pack
     *            package to install
     * @param dir
     *            directory where package file (.zip, .tar.gz or .tgz) is
     *            located.
     * @param load
     *            automatically load package after successfull installation
     * @return installation status
     */
    public String installPackage(final String pack, File dir, boolean load) {
        log("  trying to load package " + pack, Level.INFO);

        if (isPackageInstalled(pack, null)) {
            log(_PACKAGE_ + pack + " already installed.", Level.INFO);
            if (load) {
                return loadPackage(pack);
            } else {
                return PACKAGEINSTALLED;
            }
        } else {
            log(_PACKAGE_ + pack + " not yet installed.", Level.INFO);
        }

        File[] pack_files = (dir == null ? null : dir
                .listFiles(new FileFilter() {

                    public boolean accept(File pathname) {

                        if (!pathname.getName().contains(pack)) {
                            return false;
                        }
                        if (RServeOSisWindows()) {
                            return pathname.getName().endsWith(".zip");
                        }
                        if (RServeOSisLinux()) {
                            return pathname.getName().endsWith(".tar.gz");
                        }
                        if (RServeOSisMacOSX()) {
                            return pathname.getName().endsWith(".tgz");
                        }
                        return false;
                    }
                }));
        if (pack_files == null || pack_files.length == 0) {
            log("  impossible to find package " + pack + " in directory "
                    + dir.getAbsolutePath() + " !", Level.WARNING);
            return "Impossible to find package " + pack + " in directory "
                    + dir.getAbsolutePath() + " !";
        } else {
            log("  found package " + pack + " : "
                    + pack_files[0].getAbsolutePath(), Level.INFO);
        }

        sendFile(pack_files[0]);
        eval("install.packages('" + pack_files[0].getName() + "',repos=NULL,"
                + /*
                   * (RserveConf.RLibPath == null ? "" : "lib=" +
                   * RserveConf.RLibPath + ",") +
                   */"dependencies=TRUE)", TRY_MODE);
        log("  request package " + pack + " install...", Level.INFO);

        if (isPackageInstalled(pack, null)) {
            log(_PACKAGE_ + pack + " installation sucessfull.", Level.INFO);
            if (load) {
                return loadPackage(pack);
            } else {
                return PACKAGEINSTALLED;
            }
        } else {
            log(_PACKAGE_ + pack + " installation failed.", Level.ERROR);
            if (load) {
                return loadPackage(pack);
            } else {
                return "Impossible to install package " + pack + " !";
            }
        }
    }

    /**
     * Start installation procedure of CRAN R package
     * 
     * @param pack
     *            package to install
     * @param load
     *            automatically load package after successfull installation
     * @return installation status
     */
    public String installPackage(String pack, boolean load) {
        log("  trying to load package " + pack, Level.INFO);

        if (isPackageInstalled(pack, null)) {
            log(_PACKAGE_ + pack + " already installed.", Level.INFO);
            if (load) {
                return loadPackage(pack);
            } else {
                return PACKAGEINSTALLED;
            }
        } else {
            log(_PACKAGE_ + pack + " not yet installed.", Level.INFO);
        }

        /*
         * if (!Configuration.isWWWConnected()) { log("  package " + pack +
         * " not accessible on " + repos + ": CRAN unreachable."); return
         * "Impossible to get package " + pack + " from " + repos; }
         */
        eval("install.packages('" + pack + "',repos='" + repos + "',"
                + /*
                   * (RserveConf.RLibPath == null ? "" : "lib=" +
                   * RserveConf.RLibPath + ",") +
                   */"dependencies=TRUE)", TRY_MODE);
        log("  request package " + pack + " install...", Level.INFO);

        if (isPackageInstalled(pack, null)) {
            log(_PACKAGE_ + pack + " installation sucessfull.", Level.INFO);
            if (load) {
                return loadPackage(pack);
            } else {
                return PACKAGEINSTALLED;
            }
        } else {
            log(_PACKAGE_ + pack + " installation failed.", Level.ERROR);
            if (load) {
                return loadPackage(pack);
            } else {
                return "Impossible to install package " + pack + " !";
            }
        }
    }

    /**
     * load R backage using library() command
     * 
     * @param pack
     *            R package name
     * @return loading status
     */
    public String loadPackage(String pack) {
        log("  request package " + pack + " loading...", Level.INFO);
        try {
            boolean ok = eval(
                    "library(" + pack
                            + ",logical.return=T,quietly=T,verbose=F)",
                    TRY_MODE).asBytes()[0] == 1;
            if (ok) {
                log(_PACKAGE_ + pack + " loading sucessfull.", Level.INFO);
                return PACKAGELOADED;
            } else {
                log(_PACKAGE_ + pack + " loading failed.", Level.ERROR);
                return "Impossible to load package " + pack + ": "
                        + getLastLogEntry() + "," + getLastError();
            }
        } catch (Exception ex) {
            log(_PACKAGE_ + pack + " loading failed.", Level.ERROR);
            return "Impossible to load package " + pack + ": "
                    + ex.getLocalizedMessage();
        }

        /*
         * eval("library(" + pack + ",logical.return=T)", TRY_MODE);
         * log("  request package " + pack + " loading...", Level.INFO); if
         * (isPackageLoaded(pack)) { log(_PACKAGE_ + pack +
         * " loading sucessfull.", Level.INFO); return PACKAGELOADED; } else {
         * log(_PACKAGE_ + pack + " loading failed.", Level.ERROR); return
         * "Impossible to load package " + pack + ": " + getLastError(); }
         */
    }

    // </editor-fold>
    final static String HEAD_EVAL = "[eval] ";
    final static String HEAD_EXCEPTION = "[exception] ";
    final static String HEAD_ERROR = "[error] ";
    final static String HEAD_CACHE = "[cache] ";

    /**
     * Silently (ie no log) launch R command without return value. Encapsulate
     * command in try() to cacth errors
     * 
     * @param expression
     *            R expresison to evaluate
     */
    public boolean silentlyVoidEval(String expression) {
        return silentlyVoidEval(expression, TRY_MODE_DEFAULT);
    }


    public String getLastOutput() {
        if (!SINK_OUTPUT) {
            return null;
        } else {
            return lastOuput;
            /*
             * try { return connection.parseAndEval("paste(collapse='\n','" +
             * SINK_FILE + "')").asString(); } catch (Exception ex) { return
             * ex.getMessage(); }
             */
        }
    }

    /**
     * Silently (ie no log) launch R command without return value.
     * 
     * @param expression
     *            R expresison to evaluate
     * @param tryEval
     *            encapsulate command in try() to cacth errors
     */
    public boolean silentlyVoidEval(String expression, boolean tryEval) {
        // assert connected : "R environment not initialized.";
        if (!connected) {
            log(HEAD_EXCEPTION + "R environment not initialized.", Level.ERROR);
            return false;
        }
        if (expression == null) {
            return false;
        }
        if (expression.trim().length() == 0) {
            return true;
        }
        for (EvalListener b : eval) {
            b.eval(expression);
        }
        REXP e = null;
        try {
            synchronized (connection) {
                if (SINK_OUTPUT) {
                    connection.parseAndEval("sink('" + SINK_FILE
                            + "',type='output')");
                }
                if (tryEval) {
                    e = connection.parseAndEval("try(eval(parse(text='"
                            + expression.replace("'", "\\'")
                            + "')),silent=FALSE)");
                } else {
                    e = connection.parseAndEval(expression);
                }
                if (SINK_OUTPUT) {
                    connection.parseAndEval("sink(type='output')");
                    try {
                        lastOuput = connection.parseAndEval(
                                "paste(collapse='\n',readLines('" + SINK_FILE
                                        + "'))").asString();
                        log(lastOuput, Level.OUTPUT);
                    } catch (Exception ex) {
                        lastOuput = ex.getMessage();
                        log(lastOuput, Level.WARNING);
                    }
                    connection.parseAndEval("unlink('" + SINK_FILE + "')");
                }
            }
        } catch (Exception ex) {
            log(HEAD_EXCEPTION + ex.getMessage() + "\n  " + expression,
                    Level.ERROR);
        }

        if (tryEval && e != null) {
            try {
                if (e.inherits("try-error")/*
                                            * e.isString() &&
                                            * e.asStrings().length > 0 &&
                                            * e.asString
                                            * ().toLowerCase().startsWith
                                            * ("error")
                                            */) {
                    log(HEAD_EXCEPTION + e.asString() + "\n  " + expression,
                            Level.WARNING);
                    return false;
                }
            } catch (REXPMismatchException ex) {
                log(HEAD_ERROR + ex.getMessage() + "\n  " + expression,
                        Level.ERROR);
                return false;
            }
        }
        return true;
    }

    /**
     * Launch R command without return value.
     * 
     * @param expression
     *            R expresison to evaluate
     * @param tryEval
     *            encapsulate command in try() to cacth errors
     */
    public boolean voidEval(String expression, boolean tryEval) {
        // GLG HACK: Less verbosity when no error.
        // log(HEAD_EVAL + (tryEval ? HEAD_TRY : " ") + expression, Level.INFO);

        boolean done = silentlyVoidEval(expression, tryEval);

        if (done) {
            for (UpdateObjectsListener b : updateObjects) {
                b.update();
            }
        }

        return done;
    }

    /**
     * Launch R command without return value. Encapsulate command in try() to
     * cacth errors.
     * 
     * @param expression
     *            R expresison to evaluate
     */
    public boolean voidEval(String expression) {
        return voidEval(expression, TRY_MODE_DEFAULT);
    }

    /**
     * Silently (ie no log) launch R command and return value. Encapsulate
     * command in try() to cacth errors.
     * 
     * @param expression
     *            R expresison to evaluate
     * @return REXP R expression
     */
    public REXP silentlyEval(String expression) {
        return silentlyEval(expression, TRY_MODE_DEFAULT);
    }

    /**
     * Silently (ie no log) launch R command and return value.
     * 
     * @param expression
     *            R expression to evaluate
     * @param tryEval
     *            encapsulate command in try() to cacth errors
     * @return REXP R expression
     */
    public REXP silentlyEval(String expression, boolean tryEval) {
        assert connected : "R environment not initialized.";
        if (expression == null) {
            return null;
        }
        if (expression.trim().length() == 0) {
            return null;
        }
        for (EvalListener b : eval) {
            b.eval(expression);
        }
        REXP e = null;
        try {
            synchronized (connection) {
                if (SINK_OUTPUT) {
                    connection.parseAndEval("sink('" + SINK_FILE
                            + "',type='output')");
                }
                if (tryEval) {
                    e = connection.parseAndEval("try(eval(parse(text='"
                            + expression.replace("'", "\\'")
                            + "')),silent=FALSE)");
                } else {
                    e = connection.parseAndEval(expression);
                }
                if (SINK_OUTPUT) {
                    connection.parseAndEval("sink(type='output')");
                    try {
                        lastOuput = connection.parseAndEval(
                                "paste(collapse='\n',readLines('" + SINK_FILE
                                        + "'))").asString();
                        log(lastOuput, Level.OUTPUT);
                    } catch (Exception ex) {
                        lastOuput = ex.getMessage();
                        log(lastOuput, Level.WARNING);
                    }
                    connection.parseAndEval("unlink('" + SINK_FILE + "')");
                }
            }
        } catch (Exception ex) {
            log(HEAD_EXCEPTION + ex.getMessage() + "\n  " + expression,
                    Level.ERROR);
        }

        if (tryEval && e != null) {
            try {
                if (e.inherits("try-error")/*
                                            * e.isString() &&
                                            * e.asStrings().length > 0 &&
                                            * e.asString
                                            * ().toLowerCase().startsWith
                                            * ("error")
                                            */) {
                    log(HEAD_EXCEPTION + e.asString() + "\n  " + expression,
                            Level.WARNING);
                    e = null;
                }
            } catch (REXPMismatchException ex) {
                log(HEAD_ERROR + ex.getMessage() + "\n  " + expression,
                        Level.ERROR);
                return null;
            }
        }
        return e;
    }

    /**
     * Launch R command and return value.
     * 
     * @param expression
     *            R expresison to evaluate
     * @param tryEval
     *            encapsulate command in try() to cacth errors
     * @return REXP R expression
     */
    public REXP eval(String expression, boolean tryEval) {
        // GLG HACK: Less verbosity when no error.
        // log(HEAD_EVAL + (tryEval ? HEAD_TRY : "") + expression, Level.INFO);

        REXP e = silentlyEval(expression, tryEval);

        for (UpdateObjectsListener b : updateObjects) {
            b.update();
        }

        // GLG HACK: Less verbosity when no error.
        // if (e != null) {
        // log(__ + e.toDebugString(), Level.INFO);
        // }

        return e;
    }

    /**
     * Launch R command and return value. Encapsulate command in try() to cacth
     * errors.
     * 
     * @param expression
     *            R expresison to evaluate
     * @return REXP R expression
     */
    public REXP eval(String expression) {
        return eval(expression, TRY_MODE_DEFAULT);
    }

    public String getRServeOS() {
        try {
            return eval("Sys.info()['sysname']", TRY_MODE).asString();
        } catch (REXPMismatchException re) {
            return "unknown";
        }
    }

    public boolean RServeOSisWindows() {
        return getRServeOS().startsWith("Windows");
    }

    public boolean RServeOSisLinux() {
        return getRServeOS().startsWith("Linux");
    }

    public boolean RServeOSisMacOSX() {
        return getRServeOS().startsWith("Darwin");
    }

    public boolean RServeOSisUnknown() {
        return !RServeOSisWindows() && !RServeOSisLinux()
                && !RServeOSisMacOSX();
    }

    /**
     * delete all variables in R environment
     */
    public boolean rmAll() {
        return voidEval("rm(list=ls(all=TRUE))", TRY_MODE);
    }

    /**
     * create a R list with given R objects
     * 
     * @param vars
     *            R object names
     * @return list expression
     */
    public static String buildList(String... vars) {
        if (vars.length > 1) {
            StringBuffer b = new StringBuffer("c(");
            for (String v : vars) {
                b.append(v + ",");
            }

            return b.substring(0, b.length() - 1) + ")";
        } else {
            return vars[0];
        }
    }

    /**
     * create a R list with given R strings
     * 
     * @param vars
     *            R strings
     * @return String list expression
     */
    public static String buildListString(String... vars) {
        if (vars.length > 1) {
            StringBuffer b = new StringBuffer("c(");
            for (String v : vars) {
                b.append("'" + v + "',");
            }

            return b.substring(0, b.length() - 1) + ")";
        } else {
            return "'" + vars[0] + "'";
        }
    }

    /**
     * create a R list with given R string patterns
     * 
     * @param vars
     *            R string patterns
     * @return ls pattern expression
     */
    public static String buildListPattern(String... vars) {
        if (vars.length > 1) {
            StringBuffer b = new StringBuffer("c(");
            for (String v : vars) {
                b.append("ls(pattern='" + v + "'),");
            }

            return b.substring(0, b.length() - 1) + ")";
        } else {
            return "ls(pattern='" + vars[0] + "')";
        }
    }

    /**
     * loads R source file (eg ".R" file)
     * 
     * @param f
     *            ".R" file to source
     */
    public void source(File f) {
        sendFile(f);
        voidEval("source('" + f.getName() + "')", TRY_MODE);
    }

    /**
     * loads R data file (eg ".Rdata" file)
     * 
     * @param f
     *            ".Rdata" file to load
     */
    public void load(File f) {
        sendFile(f);
        try {
            assert eval("file.exists('" + f.getName() + "')", TRY_MODE)
                    .asInteger() == 1;
        } catch (REXPMismatchException r) {
            r.printStackTrace();
        }
        voidEval("load('" + f.getName() + "')", TRY_MODE);
    }

    /**
     * list R variables in R env.
     * 
     * @return list of R objects names
     */
    public String[] ls() {
        try {
            return eval("ls()", TRY_MODE).asStrings();
        } catch (REXPMismatchException re) {
            return new String[0];
        } catch (Exception re) {
            return new String[] { "?" };
        }
    }

    /**
     * list R variables in R env. matching patterns
     * 
     * @param vars
     *            R object name patterns
     * @return list of R objects names
     */
    public String[] ls(String... vars) {
        if (vars == null || vars.length == 0) {
            try {
                return eval("ls()", TRY_MODE).asStrings();
            } catch (REXPMismatchException re) {
                return new String[0];
            } catch (Exception re) {
                return new String[] { "?" };
            }
        } else if (vars.length == 1) {
            try {
                return eval(buildListPattern(vars[0]), TRY_MODE).asStrings();
            } catch (REXPMismatchException re) {
                return new String[0];
            } catch (Exception re) {
                return new String[] { "?" };
            }
        } else {
            try {
                return eval(buildListPattern(vars), TRY_MODE).asStrings();
            } catch (REXPMismatchException re) {
                return new String[0];
            } catch (Exception re) {
                return new String[] { "?" };
            }
        }
    }

    /**
     * delete R variables in R env.
     * 
     * @param vars
     *            R objects names
     */
    public boolean rm(String... vars) {
        if (vars.length == 1) {
            return voidEval("rm(" + vars[0] + ")", TRY_MODE);
        } else {
            return voidEval("rm(list=" + buildListString(vars) + ")", TRY_MODE);
        }
    }

    /**
     * delete R variables in R env. matching patterns
     * 
     * @param vars
     *            R object name patterns
     */
    public boolean rmls(String... vars) {
        if (vars.length == 1) {
            return voidEval("rm(list=" + buildListPattern(vars[0]) + ")",
                    TRY_MODE);
        } else {
            return voidEval("rm(list=" + buildListPattern(vars) + ")", TRY_MODE);
        }
    }

    public boolean SAVE_ASCII = false;

    /**
     * Save R variables in data file
     * 
     * @param f
     *            file to store data (eg ".Rdata")
     * @param vars
     *            R variables to save
     */
    public void save(File f, String... vars) {
        if (vars.length == 1) {
            voidEval("save(file='" + f.getName() + "'," + vars[0] + ",ascii="
                    + (SAVE_ASCII ? "TRUE" : "FALSE") + ")", TRY_MODE);
        } else {
            voidEval("save(file='" + f.getName() + "',list="
                    + buildListString(vars) + ",ascii="
                    + (SAVE_ASCII ? "TRUE" : "FALSE") + ")", TRY_MODE);
        }
        receiveFile(f);
        removeFile(f.getName());
    }

    /**
     * Save R variables in data file
     * 
     * @param f
     *            file to store data (eg ".Rdata")
     * @param vars
     *            R variables names patterns to save
     */
    public void savels(File f, String... vars) {
        if (vars.length == 1) {
            voidEval("save(file='" + f.getName() + "',list="
                    + buildListPattern(vars[0]) + ",ascii="
                    + (SAVE_ASCII ? "TRUE" : "FALSE") + ")", TRY_MODE);
        } else {
            voidEval("save(file='" + f.getName() + "',list="
                    + buildListPattern(vars) + ",ascii="
                    + (SAVE_ASCII ? "TRUE" : "FALSE") + ")", TRY_MODE);
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
        }
        receiveFile(f);
        removeFile(f.getName());
    }

    final static String[] types = { "data.frame", "null", "function", "array",
            "integer", "character", "double" };

    /**
     * 
     * @param robject
     *            R object name
     * @return R type of object
     */
    public String typeOf(String robject) {
        if (robject == null) {
            return "NULL";
        }
        for (String t : types) {
            REXP is = silentlyEval("is." + t + "(" + robject + ")");
            try {
                if (is != null && is.asInteger() == 1) {
                    return t;
                }
            } catch (REXPMismatchException ex) {
                log(HEAD_ERROR + "[typeOf] " + robject + " type unknown.",
                        Level.ERROR);
                return null;
            }
        }
        return "unknown";
    }

    /**
     * Build R liost in R env.
     * 
     * @param data
     *            numeric data (eg matrix)
     * @param names
     *            names of columns
     * @return RList object
     */
    public static RList buildRList(double[][] data, String... names) {
        assert data[0].length == names.length : "Cannot build R list from "
                + Arrays.deepToString(data) + " & " + Arrays.toString(names);
        REXP[] vals = new REXP[names.length];

        for (int i = 0; i < names.length; i++) {
            // System.out.println("i=" + i);
            double[] coli = new double[data.length];
            for (int j = 0; j < coli.length; j++) {
                // System.out.println("  j=" + j);
                if (data[j].length > i) {
                    coli[j] = data[j][i];
                } else {
                    coli[j] = Double.NaN;
                }
            }
            vals[i] = new REXPDouble(coli);
        }
        return new RList(vals, names);
    }

    /**
     * Build R liost in R env.
     * 
     * @param coldata
     *            numeric data as an array of numeric vectors
     * @param names
     *            names of columns
     * @return RList object
     */
    public static RList buildRList(List<double[]> coldata, String... names) {
        assert coldata.size() == names.length;
        RList list = new RList(coldata.size(), true);
        for (int i = 0; i < names.length; i++) {
            list.put(names[i], new REXPDouble(coldata.get(i)));
        }
        return list;
    }

    /**
     * delete R object in R env.
     * 
     * @param varname
     *            R objects to delete
     */
    public boolean unset(String... varname) {
        return rm(varname);
    }

    /**
     * delete R object in R env.
     * 
     * @param varname
     *            R objects to delete
     */
    public boolean unset(Collection<?> varname) {
        boolean done = true;
        for (Object v : varname) {
            done = done & rm(v.toString());
        }
        return done;
    }

    /**
     * Set R object in R env.
     * 
     * @param _vars
     *            R objects to set as key/values
     */
    public boolean set(Map<String, Object> _vars) {
        boolean done = true;
        for (String varname : _vars.keySet()) {
            done = done & set(varname, _vars.get(varname));
        }
        return done;
    }

    /**
     * Set R list in R env.
     * 
     * @param varname
     *            R list name
     * @param data
     *            numeric data in list
     * @param names
     *            names of columns
     */
    public boolean set(String varname, double[][] data, String... names) {
        RList list = buildRList(data, names);
        log(HEAD_SET + varname + " <- " + toString(list), Level.INFO);
        try {
            synchronized (connection) {
                connection.assign(varname, REXP.createDataFrame(list));
            }
        } catch (REXPMismatchException re) {
            log(HEAD_ERROR + " RList " + list.toString()
                    + " not convertible as dataframe.", Level.ERROR);
            return false;
        } catch (RserveException ex) {
            log(HEAD_EXCEPTION + ex.getMessage() + "\n  set(String varname="
                    + varname + ",double[][] data, String... names)",
                    Level.ERROR);
            return false;
        }
        return true;
    }

    public final static String HEAD_SET = "[set] ";

    /**
     * Set R object in R env.
     * 
     * @param varname
     *            R object name
     * @param var
     *            R object value
     */
    public boolean set(String varname, Object var) {
        // assert connected :
        // "R environment not initialized. Please make sure that R.init() method was called first.";
        if (!connected) {
            log(HEAD_EXCEPTION
                    + "R environment not initialized. Please make sure that R.init() method was called first.",
                    Level.ERROR);
            return false;
        }

        log(HEAD_SET + varname + " <- " + var, Level.INFO);
        /*
         * if (var instanceof DataFrame) { DataFrame df = (DataFrame) var;
         * set("names_" + varname, df.keySet().toArray(new String[]{}));
         * set("data_" + varname, df.dataSet()); eval(varname +
         * "=data.frame(x=data_" + varname + ")"); silentlyEval("names(" +
         * varname + ") <- names_" + varname); silentlyEval("rm(names_" +
         * varname + ",data_" + varname + ")"); }
         */
        if (var == null) {
            rm(varname);
            return true;
        } else if (var instanceof RList) {
            RList l = (RList) var;
            try {
                synchronized (connection) {
                    connection.assign(varname, new REXPList(l));
                }
            } catch (RserveException ex) {
                log(HEAD_EXCEPTION + ex.getMessage()
                        + "\n  set(String varname=" + varname
                        + ",Object (RList) var)", Level.ERROR);
                return false;
            }
        } else if (var instanceof File) {
            sendFile((File) var);
            return silentlyVoidEval(varname + "<-'" + ((File) var).getName()
                    + "'");
        } else if (var instanceof Integer) {
            return silentlyVoidEval(varname + "<-" + (Integer) var);
        } else if (var instanceof Double) {
            return silentlyVoidEval(varname + "<-" + (Double) var);
        } else if (var instanceof double[]) {
            try {
                synchronized (connection) {
                    connection.assign(varname, (double[]) var);
                }
            } catch (REngineException ex) {
                log(HEAD_ERROR + ex.getMessage() + "\n  set(String varname="
                        + varname + ",Object (double[]) var)", Level.ERROR);
                return false;
            }
            return silentlyVoidEval(varname/* , cat((double[]) var) */);
        } else if (var instanceof double[][]) {
            double[][] array = (double[][]) var;
            int rows = array.length;
            int col = array[0].length;
            try {
                synchronized (connection) {
                    connection.assign("row_" + varname, reshapeAsRow(array));
                }
            } catch (REngineException ex) {
                log(HEAD_ERROR + ex.getMessage() + "\n  set(String varname="
                        + varname + ",Object (double[][]) var)", Level.ERROR);
                return false;
            }
            // eval("print(row_" + varname + ")");
            boolean done = silentlyVoidEval(varname + "<-array(row_" + varname
                    + ",c(" + rows + "," + col + "))");
            return done && silentlyVoidEval("rm(row_" + varname + ")");
        } else if (var instanceof String) {
            try {
                synchronized (connection) {
                    connection.assign(varname, (String) var);
                }
            } catch (RserveException ex) {
                log(HEAD_EXCEPTION + ex.getMessage()
                        + "\n  set(String varname=" + varname
                        + ",Object (String) var)", Level.ERROR);
                return false;
            }
            return silentlyVoidEval(varname/* , (String) var */);
        } else if (var instanceof String[]) {
            try {
                synchronized (connection) {
                    connection.assign(varname, (String[]) var);
                }
            } catch (REngineException ex) {
                log(HEAD_ERROR + ex.getMessage() + "\n  set(String varname="
                        + varname + ",Object (String[]) var)", Level.ERROR);
                return false;
            }
            return silentlyVoidEval(varname/* , cat((String[]) var) */);
        } else if (var instanceof Map) {
            try {
                synchronized (connection) {
                    connection.assign(varname, asRList((Map<?,?>) var));
                }
            } catch (Exception ex) {
                log(HEAD_ERROR + ex.getMessage() + "\n  set(String varname="
                        + varname + ",Object (Map) var)", Level.ERROR);
                return false;
            }
            return silentlyVoidEval(varname);
        } else {
            throw new IllegalArgumentException(
                    "Variable "
                            + varname
                            + " is not double, double[],  double[][], String or String[]. R engine can not handle.");
        }
        return true;
    }

    public static REXPList asRList(Map<?,?> m) {
        RList l = new RList();
        for (Object o : m.keySet()) {
            Object v = m.get(o);
            if (v instanceof Double) {
                l.put(o.toString(), new REXPDouble((Double) v));
            } else if (v instanceof double[]) {
                l.put(o.toString(), new REXPDouble((double[]) v));
            } else if (v instanceof Integer) {
                l.put(o.toString(), new REXPInteger((Integer) v));
            } else if (v instanceof int[]) {
                l.put(o.toString(), new REXPInteger((int[]) v));
            } else if (v instanceof String) {
                l.put(o.toString(), new REXPString((String) v));
            } else if (v instanceof String[]) {
                l.put(o.toString(), new REXPString((String[]) v));
            } else if (v instanceof Boolean) {
                l.put(o.toString(), new REXPLogical((Boolean) v));
            } else if (v instanceof boolean[]) {
                l.put(o.toString(), new REXPLogical((boolean[]) v));
            } else if (v instanceof Map) {
                l.put(o.toString(), asRList((Map<?,?>) v));
            } else if (v instanceof RList) {
                l.put(o.toString(), (RList) v);
            } else if (v == null) {
                l.put(o.toString(), new REXPNull());
            } else {
                System.err.println("Could not cast object " + o + " : " + v);
            }
        }
        return new REXPList(l);
    }

    private static double[] reshapeAsRow(double[][] a) {
        double[] reshaped = new double[a.length * a[0].length];
        int ir = 0;
        for (int j = 0; j < a[0].length; j++) {
            for (int i = 0; i < a.length; i++) {
                reshaped[ir] = a[i][j];
                ir++;
            }
        }
        return reshaped;
    }

    /**
     * cast R object in java object
     * 
     * @param eval
     *            REXP R object
     * @return java object
     * @throws org.rosuda.REngine.REXPMismatchException
     */
    public static Object cast(REXP eval) throws REXPMismatchException {
        if (eval == null) {
            return null;
        }

        /*
         * int[] dim = eval.dim(); String dims = "["; if (dim == null) { dims =
         * "NULL"; } else { for (int i : dim) { dims += (i + " "); } dims +=
         * "]"; }
         * 
         * System.out.println(eval.toString() + "\n  isComplex=     " +
         * (eval.isComplex() ? "TRUE" : "    false") + "\n  isEnvironment= " +
         * (eval.isEnvironment() ? "TRUE" : "    false") + "\n  isExpression=  "
         * + (eval.isExpression() ? "TRUE" : "    false") +
         * "\n  isFactor=      " + (eval.isFactor() ? "TRUE" : "    false") +
         * "\n  isFactor=      " + (eval.isFactor() ? "TRUE" : "    false") +
         * "\n  isInteger=     " + (eval.isInteger() ? "TRUE" : "    false") +
         * "\n  isLanguage=    " + (eval.isLanguage() ? "TRUE" : "    false") +
         * "\n  isList=        " + (eval.isList() ? "TRUE" : "    false") +
         * "\n  isLogical=     " + (eval.isLogical() ? "TRUE" : "    false") +
         * "\n  isNull=        " + (eval.isNull() ? "TRUE" : "    false") +
         * "\n  isNumeric=     " + (eval.isNumeric() ? "TRUE" : "    false") +
         * "\n  isRaw=         " + (eval.isRaw() ? "TRUE" : "    false") +
         * "\n  isRecursive=   " + (eval.isRecursive() ? "TRUE" : "    false") +
         * "\n  isString=      " + (eval.isString() ? "TRUE" : "    false") +
         * "\n  isSymbol=      " + (eval.isSymbol() ? "TRUE" : "    false") +
         * "\n  isVector=      " + (eval.isVector() ? "TRUE" : "    false") +
         * "\n  length=  " + (eval.length()) + "\n  dim=  " + dims);
         */
        if (eval.isNumeric()) {
            if (eval.dim() == null || eval.dim().length == 1) {
                double[] array = eval.asDoubles();
                if (array.length == 0) {
                    return null;
                }
                if (array.length == 1) {
                    return array[0];
                }
                return array;
            } else {
                // System.err.println("eval.dim()="+eval.dim()+"="+cat(eval.dim()));
                double[][] mat = eval.asDoubleMatrix();
                if (mat.length == 0) {
                    return null;
                } else if (mat.length == 1) {
                    if (mat[0].length == 0) {
                        return null;
                    } else if (mat[0].length == 1) {
                        return mat[0][0];
                    } else {
                        return mat[0];
                    }
                } else {
                    if (mat[0].length == 0) {
                        return null;
                    } else if (mat[0].length == 1) {
                        double[] dmat = new double[mat.length];
                        for (int i = 0; i < dmat.length; i++) {
                            dmat[i] = mat[i][0];
                        }
                        return dmat;
                    } else {
                        return mat;
                    }
                }
            }
        }

        if (eval.isString()) {
            String[] s = eval.asStrings();
            if (s.length == 1) {
                return s[0];
            } else {
                return s;
            }
        }

        if (eval.isLogical()) {
            return eval.asInteger() == 1;
        }

        if (eval.isList()) {
            return eval.asList();
        }

        if (eval.isNull()) {
            return null;
        } else {
            System.err.println("Unsupported type: " + eval.toDebugString());
        }
        return eval.toString();
    }

    /**
     * cast to java String representation of object
     * 
     * @param eval
     *            REXP R object
     * @return String representation
     */
    public static String castToString(REXP eval) {
        if (eval == null) {
            return "";
        }
        return eval.toString();
    }

    /**
     * Create a JPEG file for R graphical command output
     * 
     * @param f
     *            File to store data (eg .jpg file)
     * @param width
     *            width of image
     * @param height
     *            height of image
     * @param fileformat
     *            format of image: png,tiff,jpeg,bmp
     * @param command
     *            R command to create image (eg plot())
     */
    public void toGraphic(File f, int width, int height, String fileformat,
            String... commands) {
        int h = Math.abs(f.hashCode());
        set("plotfile_" + h, f.getName());
        silentlyEval(fileformat + "(plotfile_" + h + ", width=" + width
                + ", height=" + height + ")");
        for (String command : commands) {
            silentlyVoidEval(command);
        }
        silentlyEval("dev.off()");
        receiveFile(f);
        rm("plotfile_" + h);
        removeFile(f.getName());
    }

    public final static String GRAPHIC_PNG = "png";
    public final static String GRAPHIC_JPEG = "jpeg";
    public final static String GRAPHIC_BMP = "bmp";
    public final static String GRAPHIC_TIFF = "tiff";

    public void toGraphic(File f, int width, int height, String... commands) {
        if (f.getName().endsWith(GRAPHIC_BMP)) {
            toBMP(f, width, height, commands);
        } else if (f.getName().endsWith(GRAPHIC_JPEG)) {
            toJPEG(f, width, height, commands);
        } else if (f.getName().endsWith(GRAPHIC_PNG)) {
            toPNG(f, width, height, commands);
        } else if (f.getName().endsWith(GRAPHIC_TIFF)) {
            toTIFF(f, width, height, commands);
        } else {
            toPNG(f, width, height, commands);
        }
    }

    public void toJPEG(File f, int width, int height, String... commands) {
        toGraphic(f, width, height, GRAPHIC_JPEG, commands);
    }

    public void toPNG(File f, int width, int height, String... commands) {
        if (RServeOSisMacOSX()) {
            toGraphic(f, width, height, GRAPHIC_JPEG, commands);
        } else {
            toGraphic(f, width, height, GRAPHIC_PNG, commands);
        }
    }

    public void toBMP(File f, int width, int height, String... commands) {
        toGraphic(f, width, height, GRAPHIC_BMP, commands);
    }

    public void toTIFF(File f, int width, int height, String... commands) {
        toGraphic(f, width, height, GRAPHIC_TIFF, commands);
    }

    /**
     * Get R command text output in HTML format
     * 
     * @param command
     *            R command returning text
     * @return HTML string
     */
    public String asR2HTML(String command) {
        String ret = installPackage("R2HTML", true);
        if (!ret.equals(PACKAGEINSTALLED)) {
            return ret;
        }
        int h = Math.abs(command.hashCode());
        silentlyEval("HTML(file=\"htmlfile_" + h + "\", " + command + ")");
        String[] lines = null;
        try {
            lines = silentlyEval("readLines(\"htmlfile_" + h + "\")")
                    .asStrings();
        } catch (REXPMismatchException e) {
            return e.getMessage();
        }
        removeFile("htmlfile_" + h);
        if (lines == null) {
            return "";
        }

        StringBuffer sb = new StringBuffer();
        for (String l : lines) {
            sb.append(l);
            sb.append("\n");
        }
        String str = sb.toString();
        str = str.replace("align= center ", "align='center'");
        str = str.replace("cellspacing=0", "cellspacing='0'");
        str = str.replace("border=1", "border='1'");
        str = str.replace("align=bottom", "align='bottom'");
        str = str.replace("class=dataframe", "class='dataframe'");
        str = str.replace("class= firstline ", "class='firstline'");
        str = str.replace("class=firstcolumn", "class='firstcolumn'");
        str = str.replace("class=cellinside", "class='cellinside'");
        str = str.replace("border=0", "border='0'");
        str = str.replace("class=captiondataframe", "class='captiondataframe'");
        str = str.replace("</td></table>", "</td></tr></table>");
        return str;
    }

    /**
     * Get R command text output in HTML format
     * 
     * @param command
     *            R command returning text
     * @return HTML string
     */
    public String asHTML(String command) {
        return toHTML(asString(command));
    }

    public static String toHTML(String src) {
        if (src == null) {
            return src;
        }
        src = src.replace("&", "&amp;");
        src = src.replace("\"", "&quot;");
        src = src.replace("'", "&apos;");
        src = src.replace("<", "&lt;");
        src = src.replace(">", "&gt;");
        return "<html>" + src.replace("\n", "<br/>") + "</html>";
    }

    /**
     * Get R command text output
     * 
     * @param command
     *            R command returning text
     * @return String
     */
    public String asString(String command) {
        try {
            String s = silentlyEval(
                    "paste(capture.output(print(" + command
                            + ")),collapse='\\n')").asString();
            return s;
        } catch (REXPMismatchException ex) {
            return ex.getMessage();
        }
        /*
         * String[] lines = null; try { lines = silentlyEval("capture.output( "
         * + command + ")").asStrings(); } catch (REXPMismatchException e) {
         * return e.getMessage(); } if (lines == null) { return ""; }
         * StringBuffer sb = new StringBuffer(); for (String l : lines) {
         * sb.append(l); sb.append("\n"); } return sb.toString();
         */
    }

    final static String IO_HEAD = "[IO] ";

    /**
     * Get file from R environment to user filesystem
     * 
     * @param localfile
     *            file to get (same name in R env. and user filesystem)
     */
    public void receiveFile(File localfile) {
        receiveFile(localfile, localfile.getName());
    }

    /**
     * Get file from R environment to user filesystem
     * 
     * @param localfile
     *            local filesystem file
     * @param remoteFile
     *            R environment file name
     */
    public void receiveFile(File localfile, String remoteFile) {
        try {
            /*
             * int i = 10; while (i > 0 && silentlyEval("file.exists('" +
             * remoteFile + "')", TRY_MODE).asInteger() != 1) {
             * Thread.sleep(1000); i--; }
             */
            if (silentlyEval("file.exists('" + remoteFile + "')", TRY_MODE)
                    .asInteger() != 1) {
                log(HEAD_ERROR + IO_HEAD + "file " + remoteFile + " not found.",
                        Level.ERROR);
            }
        } catch (Exception ex) {
            log(HEAD_ERROR + ex.getMessage() + "\n  getFile(File localfile="
                    + localfile.getAbsolutePath() + ", String remoteFile="
                    + remoteFile + ")", Level.ERROR);
            return;
        }
        if (localfile.exists()) {
            localfile.delete();
            if (!localfile.exists()) {
                log(IO_HEAD + "Local file " + localfile.getAbsolutePath()
                        + " deleted.", Level.INFO);
            } else {
                log(HEAD_ERROR + IO_HEAD + "file " + localfile
                        + " still exists !", Level.ERROR);
                return;
            }
        }
        int send_buffer_size = -1;
        try {
            send_buffer_size = (int) Math.pow(
                    2.0,
                    Math.ceil(Math.log(silentlyEval(
                            "file.info('" + remoteFile + "')$size", TRY_MODE)
                            .asInteger()) / Math.log(2))) / 2;
            // send_buffer_size = (int) Math.max(Math.pow(2.0, 15),
            // send_buffer_size);//min=32kB
            // UGLY turn around to avoid "broken pipe".
            send_buffer_size = (int) Math.max(Math.pow(2.0, 24),
                    4 * send_buffer_size);// min=16MB
            // System.err.println(IO_HEAD + "using buffer of size " +
            // send_buffer_size);
            log(IO_HEAD + "using buffer of size " + send_buffer_size,
                    Level.WARNING);
        } catch (REXPMismatchException ex) {
            ex.printStackTrace();
            log(HEAD_ERROR + IO_HEAD + "file " + remoteFile
                    + " size not found.", Level.ERROR);
        }

        RFileInputStream is = null;
        FileOutputStream os = null;
        int n = 0;
        synchronized (connection) {
            try {
                is = connection.openFile(remoteFile);
                os = new FileOutputStream(localfile);
                byte[] buf = new byte[send_buffer_size];
                // FIXME bug when received file exceeds 65kb
                connection.setSendBufferSize(buf.length);
                while ((n = is.read(buf)) > 0) {
                    os.write(buf, 0, n);
                }
            } catch (RserveException ex) {
                ex.printStackTrace();
                log(HEAD_EXCEPTION + ex.getMessage() + ":"
                        + ex.getRequestErrorDescription()
                        + "\n  getFile(File localfile="
                        + localfile.getAbsolutePath() + ", String remoteFile="
                        + remoteFile + ")", Level.ERROR);
            } catch (IOException e) {
                e.printStackTrace();
                log(HEAD_ERROR + IO_HEAD + connection.getLastError()
                        + ": file " + remoteFile + " not transmitted at " + n
                        + ".\n" + e.getMessage(), Level.ERROR);
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException ee) {
                    ee.printStackTrace();
                }
                try {
                    if (os != null) {
                        os.close();
                    }
                } catch (IOException ee) {
                    ee.printStackTrace();
                }
            }
        }
        log(IO_HEAD + "File " + remoteFile + " received.", Level.INFO);
    }

    /**
     * delete R environment file
     * 
     * @param remoteFile
     *            filename to delete
     */
    public void removeFile(String remoteFile) {
        try {
            synchronized (connection) {
                connection.removeFile(remoteFile);
            }
        } catch (RserveException ex) {
            log(HEAD_EXCEPTION + ex.getMessage()
                    + "\n  removeFile(String remoteFile=" + remoteFile + ")",
                    Level.ERROR);
        }
    }

    /**
     * Send user filesystem file in r environement (like data)
     * 
     * @param localfile
     *            File to send
     */
    public void sendFile(File localfile) {
        sendFile(localfile, localfile.getName());
    }

    /**
     * Send user filesystem file in r environement (like data)
     * 
     * @param localfile
     *            File to send
     * @param remoteFile
     *            filename in R env.
     */
    public void sendFile(File localfile, String remoteFile) {
        if (!localfile.exists()) {
            synchronized (connection) {
                log(HEAD_ERROR + IO_HEAD + connection.getLastError()
                        + "\n  file " + localfile.getAbsolutePath()
                        + " does not exists.", Level.ERROR);
            }
        }
        try {
            if (silentlyEval("file.exists('" + remoteFile + "')", TRY_MODE)
                    .asInteger() == 1) {
                silentlyVoidEval("file.remove('" + remoteFile + "')", TRY_MODE);
                // connection.removeFile(remoteFile);
                log(IO_HEAD + "Remote file " + remoteFile + " deleted.",
                        Level.INFO);
            }
            /*
             * } catch (RserveException ex) { log(HEAD_EXCEPTION +
             * ex.getMessage() + "\n  putFile(File localfile=" +
             * localfile.getAbsolutePath() + ", String remoteFile=" + remoteFile
             * + ")");
             */
        } catch (REXPMismatchException ex) {
            log(HEAD_ERROR + ex.getMessage() + "\n  putFile(File localfile="
                    + localfile.getAbsolutePath() + ", String remoteFile="
                    + remoteFile + ")", Level.ERROR);
            return;
        }
        int send_buffer_size = (int) localfile.length();
        FileInputStream is = null;
        RFileOutputStream os = null;
        synchronized (connection) {
            try {
                os = connection.createFile(remoteFile);
                is = new FileInputStream(localfile);
                byte[] buf = new byte[send_buffer_size];
                try {
                    connection.setSendBufferSize(buf.length);
                } catch (RserveException ex) {
                    ex.printStackTrace();
                    log(HEAD_EXCEPTION + ex.getMessage()
                            + "\n  putFile(File localfile="
                            + localfile.getAbsolutePath()
                            + ", String remoteFile=" + remoteFile + ")",
                            Level.ERROR);
                }
                int n = 0;
                while ((n = is.read(buf)) > 0) {
                    os.write(buf, 0, n);
                }
            } catch (IOException e) {
                log(HEAD_ERROR + IO_HEAD + connection.getLastError()
                        + ": file " + remoteFile + " not writable.\n"
                        + e.getMessage(), Level.ERROR);
                return;
            } finally {
                try {
                    if (os != null) {
                        os.close();
                    }
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException ee) {
                    ee.printStackTrace();
                }
            }
        }
        log(IO_HEAD + "File " + remoteFile + " sent.", Level.INFO);
    }

    final static String testExpression = "1+pi";
    final static double testResult = 1 + Math.PI;
    Map<String, Object> noVarsEvals = new HashMap<String, Object>();

    /**
     * Method to eval expression. Holds many optimizations (@see noVarsEvals)
     * and turn around for reliable usage (like engine auto restart). 1D Numeric
     * "vars" are replaced using Java replace engine instead of R one. Intended
     * to not interfer with current R env vars. Yes, it's hard-code :)
     * 
     * @param expression
     *            String to evaluate
     * @param vars
     *            HashMap<String, Object> vars inside expression. Passively
     *            overload current R env variables.
     * @return java cast Object
     * @warning UNSTABLE and high CPU cost.
     */
    public synchronized Object proxyEval(String expression,
            Map<String, Object> vars) throws Exception {
        // System.out.println("eval(" + expression + "," + vars + ")");
        if (expression.length() == 0) {
            return null;
        }

        try {
            log(HEAD_CACHE + "No evaluation needed for " + expression,
                    Level.INFO);
            return Double.parseDouble(expression);
        } catch (NumberFormatException ne) {

            if (!uses(expression, vars) && noVarsEvals.containsKey(expression)) {
                // System.out.println("noVarsEvals < " + expression + " -> " +
                // noVarsEvals.get(expression));
                log(HEAD_CACHE + "Cached evaluation of " + expression + " in "
                        + noVarsEvals, Level.INFO);
                return noVarsEvals.get(expression);
            }

            if (vars != null && vars.containsKey(expression)) {
                log(HEAD_CACHE + "Get evaluation of " + expression + " in "
                        + vars, Level.INFO);
                return vars.get(expression);
            }

            Map<String, Object> clean_vars = new HashMap<String, Object>();
            String clean_expression = expression;
            if (vars != null) {
                for (String v : vars.keySet()) {
                    if (vars.get(v) instanceof Number) {
                        while (containsVar(clean_expression, v)) {
                            clean_expression = replaceVar(clean_expression, v,
                                    "(" + vars.get(v) + ")");
                        }
                        log(HEAD_CACHE + "Replacing " + v + " in "
                                + clean_expression, Level.INFO);
                    } else {
                        if (containsVar(clean_expression, v)/*
                                                             * clean_expression.
                                                             * contains(v)
                                                             */) {
                            String newvarname = v;
                            while (ls(newvarname).length > 0) {
                                newvarname = "_" + newvarname;
                            }
                            log(HEAD_CACHE + "Renaming " + v + " by "
                                    + newvarname + " in " + clean_expression,
                                    Level.INFO);
                            while (containsVar(clean_expression, v)) {
                                clean_expression = replaceVar(clean_expression,
                                        v, newvarname);
                            }
                            clean_vars.put(newvarname, vars.get(v));
                        }
                    }
                }
            }

            if (!uses(clean_expression, clean_vars)
                    && noVarsEvals.containsKey(clean_expression)) {
                // System.out.println("noVarsEvals < " + expression + " -> " +
                // noVarsEvals.get(expression));
                log(HEAD_CACHE + "Cached evaluation of " + expression + " in "
                        + noVarsEvals, Level.INFO);
                return noVarsEvals.get(clean_expression);
            }

            // System.out.println("clean_expression=" + clean_expression);
            // System.out.println("clean_vars=" + clean_vars);
            Object out = null;
            try {
                if (uses(clean_expression, clean_vars)) {
                    set(clean_vars);
                }
                log(HEAD_CACHE + "True evaluation of " + clean_expression
                        + " with " + clean_vars, Level.INFO);
                // System.out.println("clean_expression=" + clean_expression);
                REXP exp = eval(clean_expression);
                // System.out.println("eval=" + eval.toDebugString());
                out = cast(exp);

                if (clean_vars.isEmpty() && out != null) {
                    log(HEAD_CACHE + "Saving result of " + clean_expression,
                            Level.INFO);
                    noVarsEvals.put(clean_expression, out);
                }

                if (!uses(expression, vars) && out != null) {
                    log(HEAD_CACHE + "Saving result of " + expression,
                            Level.INFO);
                    noVarsEvals.put(expression, out);
                    // System.out.println("noVarsEvals > " + expression + " -> "
                    // + out);
                }

            } catch (Exception e) {
                // out = CAST_ERROR + expression + ": " + e.getMessage();
                log(HEAD_CACHE + "Failed cast of " + expression, Level.INFO);
                throw new Exception(CAST_ERROR + expression + ": "
                        + e.getMessage());
            } finally {
                if (uses(clean_expression, clean_vars)) {
                    unset(clean_vars.keySet());
                }

            }

            if (out == null) {
                boolean restartR = false;
                try {
                    REXP testEval = eval(testExpression);
                    double testOut = (Double) Rsession.cast(testEval);
                    if (testOut == Double.NaN
                            || Math.abs(testOut - testResult) > 0.1) {
                        restartR = true;
                    }
                } catch (Exception e) {
                    restartR = true;
                }
                if (restartR) {
                    System.err.println("Problem occured, R engine restarted.");
                    log(HEAD_CACHE + "Problem occured, R engine restarted.",
                            Level.INFO);
                    end();
                    startup();

                    return proxyEval(expression, vars);
                }
            }
            return out;
        }
    }

    final static String AW = "((\\A)|(\\W))(";
    final static String Az = ")((\\W)|(\\z))";

    static String replaceVar(final String expr, final String var,
            final String val) {
        String regexp = AW + var + Az;
        Matcher m = Pattern.compile(regexp).matcher(expr);
        if (m.find()) {
            return expr.replace(m.group(), m.group().replace(var, val));
        } else {
            return expr;
        }
    }

    static boolean containsVar(final String expr, final String var) {
        String regexp = AW + var + Az;
        Matcher m = Pattern.compile(regexp).matcher(expr);
        return m.find();
    }

    static boolean areUsed(String expression, Set<String> vars) {
        for (String v : vars) {
            if (containsVar(expression, v)) {
                return true;
            }
        }
        return false;
    }

    static boolean uses(String expression, Map<String, Object> vars) {
        return vars != null && !vars.isEmpty()
                && areUsed(expression, vars.keySet());
    }

    public static void main(String[] args) throws Exception {
        // args = new
        // String[]{"install.packages('lhs',repos='\"http://cran.irsn.fr/\"',lib='.')",
        // "1+1"};
        if (args == null || args.length == 0) {
            args = new String[10];
            for (int i = 0; i < args.length; i++) {
                args[i] = Math.random() + "+pi";
            }
        }
        Rsession R = null;
        int i = 0;
        Logger l = new Logger() {

            public void println(String message, Level l) {
                if (l == Level.INFO) {
                    System.out.println(message);
                } else {
                    System.err.println(message);
                }
            }

            public void close() {
            }
        };
        // RLogPanel l = new RLogPanel();
        // JFrame f = new JFrame();
        // f.setContentPane(l);
        // f.pack();
        // f.setSize(600, 600);
        // f.setVisible(true);

        if (args[0].startsWith(RserverConf.RURL_START)) {
            i++;
            R = Rsession.newInstanceTry(l, RserverConf.parse(args[0]), null);
        } else {
            R = Rsession.newInstanceTry(l, null, null);
        }

        // RObjectsPanel o = new RObjectsPanel(R);
        // o.setAutoUpdate(true);
        // System.err.println(R.loadPackage("DiceView"));
        // RList list = new RList(new REXP[]{new REXPDouble(10)},new
        // String[]{"a"});
        // System.err.println(list.names);
        // System.err.println(list.at("a").asDouble());
        // R.connection.assign("l", new REXPList(list));
        // System.err.println(R.eval("l"));
        // Map m = new HashMap();
        // m.put("a", 10);
        // m.put("b", "dfsdfs");
        // m.put("c", new double[]{1, 2, 3});
        // Map m2 = new HashMap();
        // m2.put("d", 11);
        // m.put("e", m2);
        // R.set("l", m);
        // R.eval("l");
        for (int j = i; j < args.length; j++) {
            System.err.print(args[j] + ": ");
            System.err.println(castToString(R.eval(args[j])));
        }

        R.end();
    }
}
