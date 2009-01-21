/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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

package net.sf.mzmine.main.mzmineclient;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.RawDataFileWriter;
import net.sf.mzmine.data.StorableParameterSet;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.helpsystem.HelpImp;
import net.sf.mzmine.desktop.impl.MainWindow;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.project.impl.ProjectManagerImpl;
import net.sf.mzmine.project.impl.RawDataFileImpl;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.util.NumberFormatter;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

/**
 * This interface represents MZmine core modules - I/O, task controller and GUI.
 */
public abstract class MZmineCore {

    public static final File CONFIG_FILE = new File("conf/config.xml");

    // configuration XML structure
    public static final String PARAMETER_ELEMENT_NAME = "parameter";
    public static final String PARAMETERS_ELEMENT_NAME = "parameters";
    public static final String MODULES_ELEMENT_NAME = "modules";
    public static final String MODULE_ELEMENT_NAME = "module";
    public static final String CLASS_ATTRIBUTE_NAME = "class";
    public static final String NODES_ELEMENT_NAME = "nodes";
    public static final String LOCAL_ATTRIBUTE_NAME = "local";
    public static final String DESKTOP_ELEMENT_NAME = "desktop";

    private static Logger logger = Logger.getLogger(MZmineCore.class.getName());

    protected static TaskController taskController;
    protected static Desktop desktop;
    protected static ProjectManagerImpl projectManager;
    protected static MZmineModule[] initializedModules;
    protected static HelpImp help;
    protected static boolean isLightViewer;

    /**
     * Returns a reference to local task controller.
     * 
     * @return TaskController reference
     */
    public static TaskController getTaskController() {
        return taskController;
    }

    /**
     * Returns a reference to Desktop. May return null on MZmine nodes with no
     * GUI.
     * 
     * @return Desktop reference or null
     */
    public static Desktop getDesktop() {
        return desktop;
    }

    /**
     * 
     */
    public static MZmineProject getCurrentProject() {
    	if (projectManager != null){
    		return projectManager.getCurrentProject();
    	}
    	else{
    		return null;
    	}
    }

    /**
     * Returns an array of all initialized MZmine modules
     * 
     * @return Array of all initialized MZmine modules
     */
    public static MZmineModule[] getAllModules() {
        return initializedModules;
    }

    /**
     * Returns an array of all initialized MZmine modules
     * 
     * @return Array of all initialized MZmine modules
     */
    public static HelpImp getHelpImp() {
        return help;
    }
    
    public static boolean isLightViewer(){
    	return isLightViewer;
    }

    /**
     * Saves configuration and exits the application.
     * 
     */
    public static void exitMZmine() {

        // If we have GUI, ask if use really wants to quit
        if (desktop != null) {
            int selectedValue = JOptionPane.showInternalConfirmDialog(
                    desktop.getMainFrame().getContentPane(),
                    "Are you sure you want to exit MZmine 2?", "Exiting...",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (selectedValue != JOptionPane.YES_OPTION)
                return;
            desktop.getMainFrame().dispose();
        }

        System.exit(0);

    }

    public static void saveConfiguration(File file) {

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
            for (MZmineModule module : getAllModules()) {

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
            XMLWriter writer = new XMLWriter(new FileWriter(file), format);
            writer.write(configuration);
            writer.close();

            logger.finest("Saved configuration to file " + file);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not update configuration file "
                    + file, e);
        }

    }

    public static void loadConfiguration(File file) {

        try {
            SAXReader reader = new SAXReader();
            Document configuration = reader.read(file);
            Element configRoot = configuration.getRootElement();

            logger.finest("Loading desktop configuration");

            StorableParameterSet desktopParameters = (StorableParameterSet) desktop.getParameterSet();
            Element desktopConfigElement = configRoot.element(DESKTOP_ELEMENT_NAME);
            if (desktopConfigElement != null)
                desktopParameters.importValuesFromXML(desktopConfigElement);

            logger.finest("Loading modules configuration");

            for (MZmineModule module : getAllModules()) {
                String className = module.getClass().getName();
                String xpathLocation = "//configuration/modules/module[@class='"
                        + className + "']";

                Element moduleElement = (Element) configuration.selectSingleNode(xpathLocation);
                if (moduleElement == null)
                    continue;

                Element parametersElement = moduleElement.element(PARAMETERS_ELEMENT_NAME);

                if (parametersElement != null) {
                    ParameterSet moduleParameters = module.getParameterSet();
                    if ((moduleParameters != null)
                            && (moduleParameters instanceof StorableParameterSet))
                        ((StorableParameterSet) moduleParameters).importValuesFromXML(parametersElement);
                }

            }

            logger.finest("Loaded configuration from file " + file);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not parse configuration file "
                    + file, e);
        }

    }

    // Number formatting functions
    public static NumberFormatter getIntensityFormat() {
        //return ((MainWindow) desktop).getIntensityFormat();
        return desktop.getIntensityFormat();
    }

    public static NumberFormatter getMZFormat() {
        //return ((MainWindow) desktop).getMZFormat();
        return desktop.getMZFormat();
    }

    public static NumberFormatter getRTFormat() {
        //return ((MainWindow) desktop).getRTFormat();
        return desktop.getRTFormat();
    }

    public static RawDataFileWriter createNewFile(String name)
            throws IOException {
        return new RawDataFileImpl(name);
    }

}
