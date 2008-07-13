/*
 * Copyright 2006-2008 The MZmine Development Team
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

import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import net.sf.mzmine.desktop.helpsystem.HelpImp;
import net.sf.mzmine.desktop.impl.MainWindow;
import net.sf.mzmine.project.impl.ProjectManagerImpl;
import net.sf.mzmine.taskcontrol.impl.TaskControllerImpl;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

/**
 * Main client class
 */
public class MZmineClient extends MZmineCore implements Runnable {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private Vector<MZmineModule> moduleSet;

    // make MZmineClient a singleton
    private static MZmineClient client = new MZmineClient();

    private MZmineClient() {
    }

    public static MZmineClient getInstance() {
        return client;
    }

    /**
     * Main method
     */
    public static void main(String args[]) {

        // create the GUI in the event-dispatching thread
        SwingUtilities.invokeLater(client);

    }

    /**
     * @see java.lang.Runnable#run()
     */
    @SuppressWarnings("unchecked")
    public void run() {

        // load configuration from XML
        Document configuration = null;
        MainWindow desktop = null;
        try {
            SAXReader reader = new SAXReader();
            configuration = reader.read(CONFIG_FILE);
            Element configRoot = configuration.getRootElement();

            // get the configured number of computation nodes
            int numberOfNodes;

            Element nodes = configRoot.element(NODES_ELEMENT_NAME);
            String numberOfNodesConfigEntry = nodes.attributeValue(LOCAL_ATTRIBUTE_NAME);
            if (numberOfNodesConfigEntry != null) {
                numberOfNodes = Integer.parseInt(numberOfNodesConfigEntry);
            } else
                numberOfNodes = Runtime.getRuntime().availableProcessors();

            logger.info("MZmine starting with " + numberOfNodes
                    + " computation nodes");

            logger.finer("Loading core classes");

            // create instances of core modules
            TaskControllerImpl taskController = new TaskControllerImpl(
                    numberOfNodes);
            projectManager = new ProjectManagerImpl();
            desktop = new MainWindow();
            help = new HelpImp();

            // save static references to MZmineCore
            MZmineCore.taskController = taskController;
            MZmineCore.desktop = desktop;

            logger.finer("Initializing core classes");

            taskController.initModule();
            projectManager.initModule();
            desktop.initModule();

            // menu items have to be added after desktop is initialized
            projectManager.createMenuItems();

            logger.finer("Loading modules");

            moduleSet = new Vector<MZmineModule>();

            Iterator<Element> modIter = configRoot.element(MODULES_ELEMENT_NAME).elementIterator(
                    MODULE_ELEMENT_NAME);

            while (modIter.hasNext()) {
                Element moduleElement = modIter.next();
                String className = moduleElement.attributeValue(CLASS_ATTRIBUTE_NAME);
                loadModule(className);
            }

            MZmineCore.initializedModules = moduleSet.toArray(new MZmineModule[0]);

            // load module configuration
            loadConfiguration(CONFIG_FILE);
            

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not parse configuration file "
                    + CONFIG_FILE, e);
            System.exit(1);
        }

        // register the shutdown hook
        ShutDownHook shutDownHook = new ShutDownHook();
        Runtime.getRuntime().addShutdownHook(shutDownHook);

        // show the GUI
        logger.finest("Showing main window");
        desktop.setVisible(true);

        // show the welcome message
        desktop.setStatusBarText("Welcome to MZmine 2!");

    }

    public MZmineModule loadModule(String moduleClassName) {

        try {

            logger.finest("Loading module " + moduleClassName);

            // load the module class
            Class moduleClass = Class.forName(moduleClassName);

            // create instance
            MZmineModule moduleInstance = (MZmineModule) moduleClass.newInstance();

            // init module
            moduleInstance.initModule();

            // add to the module set
            moduleSet.add(moduleInstance);

            return moduleInstance;

        } catch (Exception e) {
            logger.log(Level.SEVERE,
                    "Could not load module " + moduleClassName, e);
            return null;
        }

    }

    /**
     * Shutdown hook - invoked on JRE shutdown. This method saves current
     * configuration to XML.
     * 
     */
    private class ShutDownHook extends Thread {

        public void start() {
            saveConfiguration(CONFIG_FILE);
        }
    }
}
