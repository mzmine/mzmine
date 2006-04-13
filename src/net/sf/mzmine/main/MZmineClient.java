/*
 * Copyright 2005 VTT Biotechnology
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.main;

import net.sf.mzmine.io.IOController;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.userinterface.MainWindow;
import net.sf.mzmine.util.Logger;

public class MZmineClient {

    private static MainWindow mainWindow;

    private static TaskController taskController;

    /**
     * Main method
     */
    public static void main(String argz[]) {

        Logger.disableOutput();

        int numberOfNodes = 2;

        for (String arg : argz) {
            if ((arg.compareToIgnoreCase(new String("verbose"))) == 0) {
                Logger.setOutputOnScreen();
            } else if (arg.toLowerCase().startsWith("numberOfNodes=")) {
                numberOfNodes = Integer.parseInt(arg.substring(arg
                        .toLowerCase().indexOf("numberOfNodes=") + 14));
            } else {
                Logger.putFatal("Could not interpret command-line argument: "
                        + arg);
                System.exit(-1);
            }

        }

        Logger.putFatal("MZmine client is starting up with " + numberOfNodes
                + " local computing threads..");

        // TODO: check if we have any remote nodes. if we do, startup RMI

        // Create RMI registry
        /*
         * Logger.put("STARTUP THREAD: Starting RMI registry."); RMI removed
         * from Single mode try {
         * java.rmi.registry.LocateRegistry.createRegistry(java.rmi.registry.Registry.REGISTRY_PORT); }
         * catch (Exception e) { Logger.putFatal("STARTUP THREAD: FATAL ERROR -
         * Couldn't create RMI registry."); Logger.putFatal(e.toString());
         * return; }
         * 
         * 
         * try { Thread.sleep(2000); } catch (Exception e) { Logger.put("STARTUP
         * THREAD: ERROR - Can't get no sleep"); }
         */

        Logger.put("STARTUP THREAD: Starting node(s)");

        new IOController();
        taskController = new TaskController(numberOfNodes);

        Logger.put("STARTUP THREAD: Starting GUI.");

        /*
         * Create the GUI in the event-dispatching thread, as recommended by Swing tutorial
         */
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                mainWindow = new MainWindow("MZmine", null);
                mainWindow.setVisible(true);
            }
        });

    }
    

}