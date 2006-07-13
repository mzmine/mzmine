/*
 * Copyright 2006 The MZmine Development Team
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.FileInputStream;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.JMenu;
import javax.swing.SwingUtilities;

import net.sf.mzmine.io.impl.IOControllerImpl;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.impl.TaskControllerImpl;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.util.GUIUtils;

public class MZmineClient implements Runnable {

    private Logger mainLogger;
    private TaskControllerImpl taskController;
    private IOControllerImpl ioController;
    private MainWindow mainWindow;

    /**
     * Main method
     */
    public static void main(String args[]) {

        // create the GUI in the event-dispatching thread
        MZmineClient client = new MZmineClient();
        SwingUtilities.invokeLater(client);

    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        // configure logging
        try {
            LogManager logManager = LogManager.getLogManager();
            FileInputStream loggingConfig = new FileInputStream(
                    "dist/logging.properties");
            logManager.readConfiguration(loggingConfig);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // get main class logger
        mainLogger = Logger.getLogger(this.getClass().getName());

        // configuration properties
        ResourceBundle configBundle = ResourceBundle.getBundle("config");

        // get the configured number of computation nodes
        int numberOfNodes = 2;
        String numberOfNodesConfigEntry = configBundle.getString("NumberOfNodes");
        if (numberOfNodesConfigEntry != null) {
            numberOfNodes = Integer.parseInt(numberOfNodesConfigEntry);
        }

        mainLogger.finer("Loading core classes");

        mainLogger.finest("Creating task controller with " + numberOfNodes
                + " nodes");
        taskController = new TaskControllerImpl(numberOfNodes);

        mainLogger.finest("Creating IO controller");
        ioController = new IOControllerImpl();

        mainLogger.finest("Creating main window");
        mainWindow = new MainWindow();

        mainLogger.finest("Creating MZmine project");
        MZmineProject project = new MZmineProject();

        Logger moduleLogger;
        mainLogger.finer("Initializing core classes");

        mainLogger.finest("Initializing task controller");
        moduleLogger = Logger.getLogger(taskController.getClass().getName());
        taskController.initModule(ioController, taskController, mainWindow,
                moduleLogger);

        mainLogger.finest("Initializing IO controller");
        moduleLogger = Logger.getLogger(ioController.getClass().getName());
        ioController.initModule(ioController, taskController, mainWindow,
                moduleLogger);

        mainLogger.finest("Initializing main window");
        moduleLogger = Logger.getLogger(mainWindow.getClass().getName());
        mainWindow.initModule(ioController, taskController, mainWindow,
                moduleLogger);

        mainLogger.finest("Initializing MZmine project");
        moduleLogger = Logger.getLogger(project.getClass().getName());
        project.initModule(ioController, taskController, mainWindow,
                moduleLogger);

        mainLogger.finer("Loading modules");

        // get module classes and trim spaces from their names
        String[] modules = configBundle.getString("Modules").split(" *[,;:] *");

        for (String className : modules) {

            try {

                mainLogger.finest("Loading class " + className);
                Class moduleClass = Class.forName(className);

                mainLogger.finest("Creating instance of module " + className);
                MZmineModule moduleInstance = (MZmineModule) moduleClass.newInstance();

                mainLogger.finest("Initializing module " + className);
                moduleLogger = Logger.getLogger(className);
                moduleInstance.initModule(ioController, taskController,
                        mainWindow, moduleLogger);

                mainLogger.finest("Module " + className + " initialized");

            } catch (Exception e) {
                mainLogger.log(Level.SEVERE, "Could not load module "
                        + className, e);
            }

        }

        // show the GUI
        mainLogger.finer("Showing main window");
        mainWindow.setVisible(true);

    }

}