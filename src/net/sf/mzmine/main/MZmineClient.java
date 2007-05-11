/*
 * Copyright 2006-2007 The MZmine Development Team
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

import java.io.File;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.StorableParameterSet;
import net.sf.mzmine.io.IOController;
import net.sf.mzmine.io.impl.IOControllerImpl;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.impl.TaskControllerImpl;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

/**
 * Main client class 
 */
public class MZmineClient extends Thread implements Runnable, MZmineCore {

    private static final File CONFIG_FILE = new File("conf/config.xml");

    public static final String PARAMETER_ELEMENT_NAME = "parameter";
    public static final String PARAMETERS_ELEMENT_NAME = "parameters";
    public static final String MODULES_ELEMENT_NAME = "modules";
    public static final String MODULE_ELEMENT_NAME = "module";
    public static final String CLASS_ATTRIBUTE_NAME = "class";
    public static final String NODES_ELEMENT_NAME = "nodes";
    public static final String LOCAL_ATTRIBUTE_NAME = "local";

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private Vector<MZmineModule> moduleSet;

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

        // load configuration from XML
        Document configuration = null;
        try {
            SAXReader reader = new SAXReader();
            configuration = reader.read(CONFIG_FILE);

            // get the configured number of computation nodes
            int numberOfNodes;

            Element nodes = configuration.getRootElement().element(
                    NODES_ELEMENT_NAME);
            String numberOfNodesConfigEntry = nodes.attributeValue(LOCAL_ATTRIBUTE_NAME);
            if (numberOfNodesConfigEntry != null) {
                numberOfNodes = Integer.parseInt(numberOfNodesConfigEntry);
            } else
                numberOfNodes = Runtime.getRuntime().availableProcessors();

            logger.info("MZmine starting with " + numberOfNodes
                    + " computation nodes");

            logger.finer("Loading core classes");

            taskController = new TaskControllerImpl(numberOfNodes);
            ioController = new IOControllerImpl();
            mainWindow = new MainWindow();
            MZmineProject project = new MZmineProject();

            logger.finer("Initializing core classes");

            taskController.initModule(this);
            ioController.initModule(this);
            mainWindow.initModule(this);
            project.initModule(this);

            logger.finer("Loading modules");

            moduleSet = new Vector<MZmineModule>();

            Iterator modIter = configuration.getRootElement().element(
                    MODULES_ELEMENT_NAME).elementIterator(MODULE_ELEMENT_NAME);

            while (modIter.hasNext()) {
                Element moduleElement = (Element) modIter.next();
                String className = moduleElement.attributeValue(CLASS_ATTRIBUTE_NAME);

                MZmineModule newModule = loadModule(className);

                if (newModule != null) {
                    Element parametersElement = moduleElement.element(PARAMETERS_ELEMENT_NAME);
                    if (parametersElement != null) {
                        ParameterSet moduleParameters = newModule.getParameterSet();
                        if ((moduleParameters != null) && (moduleParameters instanceof StorableParameterSet))
                            ((StorableParameterSet) moduleParameters).importValuesFromXML(parametersElement);
                    }
                }

            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not parse configuration file "
                    + CONFIG_FILE, e);
            System.exit(1);
        }

        // register the shutdown hook
        Runtime.getRuntime().addShutdownHook(this);

        // show the GUI
        logger.finest("Showing main window");
        mainWindow.setVisible(true);

        // show the welcome message
        mainWindow.setStatusBarText("Welcome to MZmine!");

    }

    public MZmineModule loadModule(String moduleClassName) {

        try {

            logger.finest("Loading module " + moduleClassName);

            // load the module class
            Class moduleClass = Class.forName(moduleClassName);

            // create instance
            MZmineModule moduleInstance = (MZmineModule) moduleClass.newInstance();

            // init module
            moduleInstance.initModule(this);

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
     * @see net.sf.mzmine.main.MZmineCore#getIOController()
     */
    public IOController getIOController() {
        return ioController;
    }

    /**
     * @see net.sf.mzmine.main.MZmineCore#getTaskController()
     */
    public TaskController getTaskController() {
        return taskController;
    }

    /**
     * @see net.sf.mzmine.main.MZmineCore#getDesktop()
     */
    public Desktop getDesktop() {
        return mainWindow;
    }

    /**
     * @see net.sf.mzmine.main.MZmineCore#getAllModules()
     */
    public MZmineModule[] getAllModules() {
        return moduleSet.toArray(new MZmineModule[0]);
    }

    /**
     * Prepares everything for quit and then shutdowns the application
     */
    public void exitMZmine() {

        // Ask if use really wants to quit
        int selectedValue = JOptionPane.showInternalConfirmDialog(
                mainWindow.getDesktopPane(),
                "Are you sure you want to exit MZmine?", "Exiting...",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (selectedValue != JOptionPane.YES_OPTION)
            return;

        logger.info("Exiting MZmine");
        mainWindow.dispose();

        System.exit(0);

    }

    /**
     * Shutdown hook - invoked on JRE shutdown. This method saves current
     * configuration to XML.
     * 
     * @see java.lang.Thread#start()
     */
    public void start() {

        try {

            // load current configuration from XML
            SAXReader reader = new SAXReader();
            Document configuration = reader.read(CONFIG_FILE);

            // traverse modules
            Iterator<MZmineModule> iterator = moduleSet.iterator();
            while (iterator.hasNext()) {
                MZmineModule module = iterator.next();
                ParameterSet currentParameters = module.getParameterSet();
                if ((currentParameters == null) || (!(currentParameters instanceof StorableParameterSet)))
                    continue;

                String className = module.getClass().getName();
                String xpathLocation = "//configuration/modules/module[@class='"
                        + className + "']";
                Element moduleElement = (Element) configuration.selectSingleNode(xpathLocation);
                if (moduleElement != null) {

                    Element parametersElement = moduleElement.element(PARAMETERS_ELEMENT_NAME);
                    if (parametersElement == null)
                        parametersElement = moduleElement.addElement(PARAMETERS_ELEMENT_NAME);
                    else
                        parametersElement.clearContent();

                    ((StorableParameterSet) currentParameters).exportValuesToXML(parametersElement);
                }

            }

            // write the config file
            OutputFormat format = OutputFormat.createPrettyPrint();
            XMLWriter writer = new XMLWriter(new FileWriter(CONFIG_FILE),
                    format);
            writer.write(configuration);
            writer.close();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not update configuration file "
                    + CONFIG_FILE, e);
        }

    }

}