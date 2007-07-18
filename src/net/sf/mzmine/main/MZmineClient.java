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

import javax.swing.SwingUtilities;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.StorableParameterSet;
import net.sf.mzmine.io.impl.IOControllerImpl;
import net.sf.mzmine.project.impl.MZmineProjectImpl;
import net.sf.mzmine.taskcontrol.impl.TaskControllerImpl;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.util.logging.JCommonLogHandler;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.jfree.report.JFreeReportBoot;

/**
 * Main client class
 */
public class MZmineClient extends MZmineCore implements Runnable {

    private static final File CONFIG_FILE = new File("conf/config.xml");

    public static final String PARAMETER_ELEMENT_NAME = "parameter";
    public static final String PARAMETERS_ELEMENT_NAME = "parameters";
    public static final String MODULES_ELEMENT_NAME = "modules";
    public static final String MODULE_ELEMENT_NAME = "module";
    public static final String CLASS_ATTRIBUTE_NAME = "class";
    public static final String NODES_ELEMENT_NAME = "nodes";
    public static final String LOCAL_ATTRIBUTE_NAME = "local";
    public static final String DESKTOP_ELEMENT_NAME = "desktop";

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private Vector<MZmineModule> moduleSet;

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
            IOControllerImpl ioController = new IOControllerImpl();
            MainWindow desktop = new MainWindow();
            MZmineProjectImpl project = new MZmineProjectImpl();

            // save static references to MZmineCore
            MZmineCore.taskController = taskController;
            MZmineCore.ioController = ioController;
            MZmineCore.desktop = desktop;
            MZmineCore.currentProject = project;

            logger.finer("Initializing core classes");

            taskController.initModule();
            ioController.initModule();
            desktop.initModule();
            project.initModule();

            logger.finer("Loading desktop configuration");

            StorableParameterSet desktopParameters = desktop.getParameterSet();
            Element desktopConfigElement = configRoot.element(DESKTOP_ELEMENT_NAME);
            if (desktopConfigElement != null)
                desktopParameters.importValuesFromXML(desktopConfigElement);

            logger.finer("Loading modules");

            moduleSet = new Vector<MZmineModule>();

            Iterator modIter = configRoot.element(MODULES_ELEMENT_NAME).elementIterator(
                    MODULE_ELEMENT_NAME);

            while (modIter.hasNext()) {
                Element moduleElement = (Element) modIter.next();
                String className = moduleElement.attributeValue(CLASS_ATTRIBUTE_NAME);

                MZmineModule newModule = loadModule(className);

                if (newModule != null) {
                    Element parametersElement = moduleElement.element(PARAMETERS_ELEMENT_NAME);
                    if (parametersElement != null) {
                        ParameterSet moduleParameters = newModule.getParameterSet();
                        if ((moduleParameters != null)
                                && (moduleParameters instanceof StorableParameterSet))
                            ((StorableParameterSet) moduleParameters).importValuesFromXML(parametersElement);
                    }
                }

            }

            MZmineCore.initializedModules = moduleSet.toArray(new MZmineModule[0]);

            // register the shutdown hook
            ShutDownHook shutDownHook = new ShutDownHook();
            Runtime.getRuntime().addShutdownHook(shutDownHook);

            // show the GUI
            logger.finest("Showing main window");
            desktop.setVisible(true);

            // show the welcome message
            desktop.setStatusBarText("Welcome to MZmine!");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not parse configuration file "
                    + CONFIG_FILE, e);
            System.exit(1);
        }

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

            try {

                // load current configuration from XML
                SAXReader reader = new SAXReader();
                Document configuration = reader.read(CONFIG_FILE);
                Element configRoot = configuration.getRootElement();

                // save desktop configuration
                StorableParameterSet desktopParameters = ((MainWindow) desktop).getParameterSet();
                Element desktopConfigElement = configRoot.element(DESKTOP_ELEMENT_NAME);
                if (desktopConfigElement == null) {
                    desktopConfigElement = configRoot.addElement(DESKTOP_ELEMENT_NAME);
                }
                desktopConfigElement.clearContent();
                try {
                    desktopParameters.exportValuesToXML(desktopConfigElement);
                } catch (Exception e) {
                    logger.log(Level.SEVERE,
                            "Could not save desktop configuration", e);
                }

                // traverse modules
                Iterator<MZmineModule> iterator = moduleSet.iterator();
                while (iterator.hasNext()) {
                    MZmineModule module = iterator.next();
                    ParameterSet currentParameters = module.getParameterSet();
                    if ((currentParameters == null)
                            || (!(currentParameters instanceof StorableParameterSet)))
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

                        try {
                            ((StorableParameterSet) currentParameters).exportValuesToXML(parametersElement);
                        } catch (Exception e) {
                            logger.log(Level.SEVERE,
                                    "Could not save configuration of module "
                                            + module, e);
                        }
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

}