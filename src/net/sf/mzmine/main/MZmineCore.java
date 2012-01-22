/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import net.sf.mzmine.data.RawDataFileWriter;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.impl.HeadLessDesktop;
import net.sf.mzmine.desktop.impl.MainWindow;
import net.sf.mzmine.desktop.preferences.MZminePreferences;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.modules.batchmode.BatchModeModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.project.ProjectManager;
import net.sf.mzmine.project.impl.MZmineProjectImpl;
import net.sf.mzmine.project.impl.ProjectManagerImpl;
import net.sf.mzmine.project.impl.RawDataFileImpl;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.impl.TaskControllerImpl;
import net.sf.mzmine.util.dialogs.ExitCode;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.schlichtherle.io.FileOutputStream;

/**
 * MZmine main class
 */
public abstract class MZmineCore {

	private static Logger logger = Logger.getLogger(MZmineCore.class.getName());

	protected static final File CONFIG_FILE = new File("conf/config.xml");

	protected static MZminePreferences preferences;
	protected static TaskControllerImpl taskController;
	protected static Desktop desktop;
	protected static ProjectManagerImpl projectManager;
	protected static MZmineModule[] initializedModules;

	/**
	 * Main method
	 */
	public static void main(String args[]) {

		// In the beginning, set the default locale to English, to avoid
		// problems with conversion of numbers etc. (e.g. decimal separator may
		// be . or , depending on the locale)
		Locale.setDefault(new Locale("en", "US"));

		logger.info("Starting MZmine " + getMZmineVersion());

		// Remove old temporary files, if we find any
		removeOldTemporaryFiles();

		logger.fine("Loading core classes..");

		// create instance of preferences
		preferences = new MZminePreferences();

		// create instances of core modules
		projectManager = new ProjectManagerImpl();
		taskController = new TaskControllerImpl();

		logger.fine("Initializing core classes..");

		projectManager.initModule();
		taskController.initModule();

		logger.fine("Loading modules");

		Vector<MZmineModule> moduleSet = new Vector<MZmineModule>();

		for (Class<?> moduleClass : MZmineModulesList.MODULES) {

			try {

				logger.finest("Loading module " + moduleClass.getName());

				// create instance and init module
				MZmineModule moduleInstance = (MZmineModule) moduleClass
						.newInstance();

				// add to the module set
				moduleSet.add(moduleInstance);

			} catch (Throwable e) {
				logger.log(Level.SEVERE,
						"Could not load module " + moduleClass, e);
				e.printStackTrace();
				continue;
			}

		}

		initializedModules = moduleSet.toArray(new MZmineModule[0]);

		// If we have no arguments, run in GUI mode, otherwise run in batch mode
		if (args.length == 0) {

			// Create the Swing GUI in the event-dispatching thread, as is
			// generally recommended
			Runnable desktopInit = new Runnable() {
				public void run() {

					logger.fine("Initializing GUI");
					MainWindow mainWindow = new MainWindow();
					desktop = mainWindow;
					mainWindow.initModule();

					// Activate project - bind it to the desktop's project tree
					MZmineProjectImpl currentProject = (MZmineProjectImpl) projectManager
							.getCurrentProject();
					currentProject.activateProject();

					// add desktop menu icon
					for (MZmineModule module : initializedModules) {
						if (module instanceof MZmineProcessingModule) {
							mainWindow.getMainMenu().addMenuItemForModule(
									(MZmineProcessingModule) module);
						}
					}
				};

			};

			try {
				SwingUtilities.invokeAndWait(desktopInit);
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Could not initialize GUI", e);
				System.exit(1);
			}

		} else {
			desktop = new HeadLessDesktop();
		}

		// load configuration
		if (CONFIG_FILE.exists() && CONFIG_FILE.canRead()) {
			try {
				loadConfiguration(CONFIG_FILE);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// if we have GUI, show it now
		if (desktop.getMainFrame() != null) {
			// show the GUI
			logger.info("Showing main window");
			desktop.getMainFrame().setVisible(true);

			// show the welcome message
			desktop.setStatusBarText("Welcome to MZmine 2!");

			// register shutdown hook only if we have GUI - we don't want to
			// save configuration on exit if we only run a batch
			ShutDownHook shutDownHook = new ShutDownHook();
			Runtime.getRuntime().addShutdownHook(shutDownHook);
		}

		// if arguments were specified (= running without GUI), run the batch
		// mode for each argument
		for (String fileName : args) {
			File batchFile = new File(fileName);
			if ((!batchFile.exists()) || (!batchFile.canRead())) {
				logger.severe("Cannot read batch file " + batchFile);
				continue;
			}
			BatchModeModule.runBatch(batchFile);
			System.exit(0);
		}

	}

	public static void saveConfiguration(File file)
			throws ParserConfigurationException, TransformerException,
			FileNotFoundException {

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

		Document configuration = dBuilder.newDocument();
		Element configRoot = configuration.createElement("configuration");
		configuration.appendChild(configRoot);

		Element prefElement = configuration.createElement("preferences");
		configRoot.appendChild(prefElement);
		preferences.saveValuesToXML(prefElement);

		Element modulesElement = configuration.createElement("modules");
		configRoot.appendChild(modulesElement);

		// traverse modules
		for (MZmineModule module : getAllModules()) {

			String className = module.getClass().getName();

			Element moduleElement = configuration.createElement("module");
			moduleElement.setAttribute("class", className);
			modulesElement.appendChild(moduleElement);

			Element paramElement = configuration.createElement("parameters");
			moduleElement.appendChild(paramElement);

			ParameterSet moduleParameters = module.getParameterSet();
			if (moduleParameters != null) {
				moduleParameters.saveValuesToXML(paramElement);
			}

		}

		TransformerFactory transfac = TransformerFactory.newInstance();
		Transformer transformer = transfac.newTransformer();
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(
				"{http://xml.apache.org/xslt}indent-amount", "4");

		StreamResult result = new StreamResult(new FileOutputStream(file));
		DOMSource source = new DOMSource(configuration);
		transformer.transform(source, result);

		logger.info("Saved configuration to file " + file);

	}

	public static void loadConfiguration(File file)
			throws ParserConfigurationException, SAXException, IOException,
			XPathExpressionException {

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document configuration = dBuilder.parse(file);

		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();

		logger.finest("Loading desktop configuration");

		XPathExpression expr = xpath.compile("//configuration/preferences");
		NodeList nodes = (NodeList) expr.evaluate(configuration,
				XPathConstants.NODESET);
		if (nodes.getLength() == 1) {
			Element preferencesElement = (Element) nodes.item(0);
			preferences.loadValuesFromXML(preferencesElement);
		}

		logger.finest("Loading modules configuration");

		for (MZmineModule module : getAllModules()) {

			String className = module.getClass().getName();
			expr = xpath.compile("//configuration/modules/module[@class='"
					+ className + "']/parameters");
			nodes = (NodeList) expr.evaluate(configuration,
					XPathConstants.NODESET);
			if (nodes.getLength() != 1)
				continue;

			Element moduleElement = (Element) nodes.item(0);

			ParameterSet moduleParameters = module.getParameterSet();
			if (moduleParameters != null) {
				moduleParameters.loadValuesFromXML(moduleElement);
			}
		}

		logger.info("Loaded configuration from file " + file);
	}

	/**
	 * Returns a reference to local task controller.
	 * 
	 * @return TaskController reference
	 */
	public static TaskController getTaskController() {
		return taskController;
	}

	/**
	 * Returns a reference to Desktop.
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
	 * Returns an array of all initialized MZmine modules
	 * 
	 * @return Array of all initialized MZmine modules
	 */
	public static MZmineModule[] getAllModules() {
		return initializedModules;
	}

	/**
	 * Saves configuration and exits the application.
	 * 
	 */
	public static ExitCode exitMZmine() {

		// If we have GUI, ask if use really wants to quit
		int selectedValue = JOptionPane.showInternalConfirmDialog(desktop
				.getMainFrame().getContentPane(),
				"Are you sure you want to exit?", "Exiting...",
				JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

		if (selectedValue != JOptionPane.YES_OPTION)
			return ExitCode.CANCEL;

		desktop.getMainFrame().dispose();

		logger.info("Exiting MZmine");

		System.exit(0);

		return ExitCode.OK;

	}

	// Number formatting functions
	public static NumberFormat getIntensityFormat() {
		return preferences.getParameter(MZminePreferences.intensityFormat)
				.getValue();
	}

	public static NumberFormat getMZFormat() {
		return preferences.getParameter(MZminePreferences.mzFormat).getValue();
	}

	public static NumberFormat getRTFormat() {
		return preferences.getParameter(MZminePreferences.rtFormat).getValue();
	}

	public static RawDataFileWriter createNewFile(String name)
			throws IOException {
		return new RawDataFileImpl(name);
	}

	public static String getMZmineVersion() {
		return MZmineVersion.MZMINE_VERSION;
	}

	public static MZminePreferences getPreferences() {
		return preferences;
	}

	private static void removeOldTemporaryFiles() {

		logger.fine("Checking for old temporary files...");
		try {

			// Find all temporary files with the mask mzmine*.scans
			File tempDir = new File(System.getProperty("java.io.tmpdir"));
			File remainingTmpFiles[] = tempDir.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.matches("mzmine.*\\.scans");
				}
			});

			if (remainingTmpFiles != null)
				for (File remainingTmpFile : remainingTmpFiles) {

					// Skip files created by someone else
					if (!remainingTmpFile.canWrite())
						continue;

					// Try to obtain a lock on the file
					RandomAccessFile rac = new RandomAccessFile(
							remainingTmpFile, "rw");

					FileLock lock = rac.getChannel().tryLock();
					rac.close();

					if (lock != null) {
						// We locked the file, which means nobody is using it
						// anymore and it can be removed
						logger.finest("Removing unused file "
								+ remainingTmpFile);
						remainingTmpFile.delete();
					}

				}
		} catch (IOException e) {
			logger.log(Level.WARNING,
					"Error while checking for old temporary files", e);
		}

	}
}
