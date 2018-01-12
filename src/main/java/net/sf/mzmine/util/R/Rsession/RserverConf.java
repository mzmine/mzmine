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

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.util.Properties;

import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

public class RserverConf {

    public static final String DEFAULT_RSERVE_HOST = "localhost";
    RConnection connection;
    public String host;
    public int port;
    public String login;
    public String password;
    // public String RLibPath;
    public Properties properties;

    // public String http_proxy;

    public RserverConf(String RserverHostName, int RserverPort, String login,
            String password, Properties props) {
        this.host = RserverHostName;
        this.port = RserverPort;
        this.login = login;
        this.password = password;
        properties = props;
    }

    public static long CONNECT_TIMEOUT = 1000;

    public abstract class TimeOut {

        /**
         * @return the result
         */
        public Object getResult() {
            return result;
        }

        public class TimeOutException extends Exception {

            public TimeOutException(String why) {
                super(why);
            }
        }

        private class TimeoutThread implements Runnable {

            public void run() {
                Object res = command();
                synchronized (TimeOut.this) {
                    if (timedOut && res != null) {
                    } else {
                        result = res;
                        TimeOut.this.notify();
                    }
                }
            }
        }

        private boolean timedOut = false;
        private Object result = null;

        protected TimeOut() {
        }

        public synchronized void execute(long timeout) throws TimeOutException {
            new Thread(new TimeoutThread()).start();

            try {
                this.wait(timeout);
            } catch (InterruptedException e) {
                if (getResult() == null) {
                    timedOut = true;
                    result = defaultResult();
                    throw new TimeOutException("timed out");
                } else {
                    return;
                }
            }

            if (getResult() != null) {
                return;
            } else {
                timedOut = true;
                result = defaultResult();
                throw new TimeOutException("timed out");
            }
        }

        /**
         * @param result
         */
        protected abstract Object defaultResult();

        /**
         * @return
         */
        protected abstract Object command();
    }

    /*
     * private class ConnectionThread implements Runnable {
     * 
     * public void run() { try { if (host == null) { if (port > 0) { connection
     * = new RConnection(DEFAULT_RSERVE_HOST, port); } else { connection = new
     * RConnection(); } if (connection.needLogin()) { connection.login(login,
     * password); } } else { if (port > 0) { connection = new RConnection(host,
     * port); } else { connection = new RConnection(host); } if
     * (connection.needLogin()) { connection.login(login, password); } } } catch
     * (RserveException ex) { //ex.printStackTrace(); //return null; }
     * 
     * synchronized (this) { this.notify(); } } }
     */
    public synchronized RConnection connect() {
        // System.err.print("Connecting " + toString()+" ... ");

        TimeOut t = new TimeOut() {

            protected Object defaultResult() {
                return -2;
            }

            protected Object command() {
                try {
                    if (host == null) {
                        if (port > 0) {
                            connection = new RConnection(DEFAULT_RSERVE_HOST,
                                    port);
                        } else {
                            connection = new RConnection();
                        }
                        if (connection.needLogin()) {
                            connection.login(login, password);
                        }
                    } else {
                        if (port > 0) {
                            connection = new RConnection(host, port);
                        } else {
                            connection = new RConnection(host);
                        }
                        if (connection.needLogin()) {
                            connection.login(login, password);
                        }
                    }
                    return 0;
                } catch (RserveException ex) {
                    System.err.println("Failed to connect: " + ex.getMessage());
                    return -1;
                    // ex.printStackTrace();
                    // return null;
                }
            }
        };

        try {
            t.execute(CONNECT_TIMEOUT);
        } catch (Exception e) {
            System.err.println("  failed: " + e.getMessage());
        }

        /*
         * new Thread(new ConnectionThread()).start();
         * 
         * try { this.wait(CONNECT_TIMEOUT);
         * 
         * } catch (InterruptedException ie) { }
         */

        if (connection != null && connection.isConnected()) {
            if (properties != null) {
                for (String p : properties.stringPropertyNames()) {
                    try {
                        connection.eval("Sys.setenv(" + p + "="
                                + properties.getProperty(p) + ")");
                    } catch (RserveException ex) {
                        ex.printStackTrace();
                    }
                }
            }

            /*
             * Special libPath no more used. try { //if (RLibPath == null) {
             * boolean isWindows =
             * connection.eval("as.logical(Sys.info()[1]=='Windows')"
             * ).asInteger() == 1; RLibPath =
             * "paste(Sys.getenv(\"HOME\"),\"Rserve\",sep=\"" + (isWindows ?
             * "\\\\" : "/") + "\")"; //} if (RLibPath != null) {
             * connection.eval("if(!file.exists(" + RLibPath + ")) dir.create("
             * + RLibPath + ")"); connection.eval(".libPaths(new=" + RLibPath +
             * ")"); } } catch (REXPMismatchException r) { r.printStackTrace();
             * } catch (RserveException r) { r.printStackTrace(); }
             */

            // System.err.println("Connection " + toString()+" succeded.");
            return connection;
        } else {
            System.err.println("Connection " + toString() + " failed.");
            return null;
        }

    }

    public final static int RserverDefaultPort = 6311;
    private static int RserverPort = RserverDefaultPort; // used for windows
                                                         // multi-session
                                                         // emulation.
                                                         // Incremented at each
                                                         // new Rscript
                                                         // instance.

    public static boolean isPortAvailable(int p) {
        try {
            ServerSocket test = new ServerSocket(p);
            test.close();
        } catch (BindException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    // GLG TODO: Better not use this. See comment bellow.
    // The point being getting a new port (Win), see new function below.
    public static RserverConf newLocalInstance(Properties p) {
        RserverConf server = null;
        if (System.getProperty("os.name").contains("Win")
                || !Rsession.UNIX_OPTIMIZE) {
            // RserverPort = RserverDefaultPort;
            // GLG TODO: use ArrayList<Integer>() to reuse freed ports
            // (Rsession.end())
            // Otherwise, we'll run out of possible port number in case of
            // intensive use !!!
            while (!isPortAvailable(RserverPort)) {
                RserverPort++;
                // System.out.println("RserverPort++ = " + RserverPort);
            }
            server = new RserverConf(null, RserverPort, null, null, p);
        } else { // Unix supports multi-sessions natively, so no need to open a
                 // different Rserve on a new port

            server = new RserverConf(null, -1, null, null, p);
        }
        return server;
    }

    // Taking advantage, by the way, to restart the search from beginning.
    // (allows the reuse of freed ports, eventually).
    public static int getNewAvailablePort() {
        synchronized (Rsession.R_SESSION_SEMAPHORE) {
            int port = RserverConf.RserverDefaultPort;
            while (Rsession.PORTS_REG.contains(Integer.valueOf(port))
                    || !RserverConf.isPortAvailable(port)) {
                port++;
            }
            if (!Rsession.PORTS_REG.contains(Integer.valueOf(port)))
                Rsession.PORTS_REG.add(Integer.valueOf(port));
            return port;
        }
    }

    public boolean isLocal() {
        return host == null || host.equals(DEFAULT_RSERVE_HOST)
                || host.equals("127.0.0.1");
    }

    @Override
    public String toString() {
        return RURL_START
                + (login != null ? (login + ":" + password + "@") : "")
                + (host == null ? DEFAULT_RSERVE_HOST : host)
                + (port > 0 ? ":" + port : "") /*
                                                * + " http_proxy=" + http_proxy
                                                * + " RLibPath=" + RLibPath
                                                */;
    }

    public final static String RURL_START = "R://";

    public static RserverConf parse(String RURL) {
        String login = null;
        String passwd = null;
        String host = null;
        int port = -1;
        try {
            String hostport = null;
            if (RURL.contains("@")) {
                String loginpasswd = RURL.split("@")[0].substring((RURL_START)
                        .length());
                login = loginpasswd.split(":")[0];
                if (login.equals("user.name")) {
                    login = System.getProperty("user.name");
                }
                passwd = loginpasswd.split(":")[1];
                hostport = RURL.split("@")[1];
            } else {
                hostport = RURL.substring((RURL_START).length());
            }

            if (hostport.contains(":")) {
                host = hostport.split(":")[0];
                port = Integer.parseInt(hostport.split(":")[1]);
            } else {
                host = hostport;
            }

            return new RserverConf(host, port, login, passwd, null);
        } catch (Exception e) {
            throw new IllegalArgumentException("Impossible to parse " + RURL
                    + ":\n  host=" + host + "\n  port=" + port + "\n  login="
                    + login + "\n  password=" + passwd);
        }

    }
}
