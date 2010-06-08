/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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

package net.sf.mzmine.main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.RawDataFileWriter;
import net.sf.mzmine.data.StorableParameterSet;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.helpsystem.HelpImpl;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.project.ProjectManager;
import net.sf.mzmine.project.impl.ProjectManagerImpl;
import net.sf.mzmine.project.impl.RawDataFileImpl;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.util.NumberFormatter;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

/**
 * This interface represents MZmine core modules - I/O, task controller and GUI.
 */
public abstract class MZmineCore {

	public static final String MZMINE_VERSION = "1.98";

	public static final File CONFIG_FILE = new File("conf/config.xml");
	public static final File DEFAULT_CONFIG_FILE = new File(
			"conf/config-default.xml");

	// configuration XML structure
	public static final String PARAMETER_ELEMENT_NAME = "parameter";
	public static final String PARAMETERS_ELEMENT_NAME = "parameters";
	public static final String MODULES_ELEMENT_NAME = "modules";
	public static final String MODULE_ELEMENT_NAME = "module";
	public static final String CLASS_ATTRIBUTE_NAME = "class";
	public static final String PREFERENCES_ELEMENT_NAME = "preferences";

	private static Logger logger = Logger.getLogger(MZmineCore.class.getName());

	protected static MZminePreferences preferences;

	protected static TaskController taskController;
	protected static Desktop desktop;
	protected static ProjectManagerImpl projectManager;
	protected static MZmineModule[] initializedModules;
	protected static HelpImpl help;

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
	public static ProjectManager getProjectManager() {
		return projectManager;
	}

	/**
     * 
     */
	public static MZmineProject getCurrentProject() {
		return projectManager.getCurrentProject();
	}

	/**
     * 
     */
	public static MZminePreferences getPreferences() {
		return preferences;
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
	 * 
	 * 
	 * @return
	 */
	public static HelpImpl getHelpImp() {
		return help;
	}

	/**
	 * Saves configuration and exits the application.
	 * 
	 */
	public static void exitMZmine() {

		// If we have GUI, ask if use really wants to quit
		if (desktop != null) {
			int selectedValue = JOptionPane.showInternalConfirmDialog(desktop
					.getMainFrame().getContentPane(),
					"Are you sure you want to exit?", "Exiting...",
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
			Document configuration = null;
			try {
				configuration = reader.read(CONFIG_FILE);
			} catch (DocumentException e) {
				configuration = reader.read(DEFAULT_CONFIG_FILE);
			}
			Element configRoot = configuration.getRootElement();

			// save desktop configuration

			Element preferencesConfigElement = configRoot
					.element(PREFERENCES_ELEMENT_NAME);
			if (preferencesConfigElement == null) {
				preferencesConfigElement = configRoot
						.addElement(PREFERENCES_ELEMENT_NAME);
			}
			preferencesConfigElement.clearContent();
			try {
				preferences.exportValuesToXML(preferencesConfigElement);
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Could not save preferences", e);
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
				Element moduleElement = (Element) configuration
						.selectSingleNode(xpathLocation);
				if (moduleElement != null) {

					Element parametersElement = moduleElement
							.element(PARAMETERS_ELEMENT_NAME);
					if (parametersElement == null)
						parametersElement = moduleElement
								.addElement(PARAMETERS_ELEMENT_NAME);
					else
						parametersElement.clearContent();

					try {
						((StorableParameterSet) currentParameters)
								.exportValuesToXML(parametersElement);
					} catch (Exception e) {
						logger.log(Level.SEVERE,
								"Could not save configuration of module "
										+ module, e);
					}
				}

			}

			// write the config file
			OutputFormat format = OutputFormat.createPrettyPrint();

			// It is important to use FileOutputStream, not FileWriter. If we
			// use FileWriter, the file will be written using incorrect encoding
			// (not UTF8).
			XMLWriter writer = new XMLWriter(new FileOutputStream(file), format);
			writer.write(configuration);
			writer.close();

			logger.finest("Saved configuration to file " + file);

		} catch (Exception e) {
			logger.log(Level.SEVERE, "Could not save configuration file "
					+ file, e);
		}

	}

	public static void loadConfiguration(File file) throws DocumentException {

		SAXReader reader = new SAXReader();
		Document configuration = reader.read(file);
		Element configRoot = configuration.getRootElement();

		logger.finest("Loading desktop configuration");

		Element preferencesConfigElement = configRoot
				.element(PREFERENCES_ELEMENT_NAME);
		if (preferencesConfigElement != null)
			preferences.importValuesFromXML(preferencesConfigElement);

		logger.finest("Loading modules configuration");

		for (MZmineModule module : getAllModules()) {
			String className = module.getClass().getName();
			String xpathLocation = "//configuration/modules/module[@class='"
					+ className + "']";

			Element moduleElement = (Element) configuration
					.selectSingleNode(xpathLocation);
			if (moduleElement == null)
				continue;

			Element parametersElement = moduleElement
					.element(PARAMETERS_ELEMENT_NAME);

			if (parametersElement != null) {
				ParameterSet moduleParameters = module.getParameterSet();
				if ((moduleParameters != null)
						&& (moduleParameters instanceof StorableParameterSet))
					((StorableParameterSet) moduleParameters)
							.importValuesFromXML(parametersElement);
			}

		}

		logger.finest("Loaded configuration from file " + file);

	}

	// Number formatting functions
	public static NumberFormatter getIntensityFormat() {
		return preferences.getIntensityFormat();
	}

	public static NumberFormatter getMZFormat() {
		return preferences.getMZFormat();
	}

	public static NumberFormatter getRTFormat() {
		return preferences.getRTFormat();
	}

	public static RawDataFileWriter createNewFile(String name)
			throws IOException {
		return new RawDataFileImpl(name);
	}

	public static String getMZmineVersion() {
		return MZMINE_VERSION;
	}

}
