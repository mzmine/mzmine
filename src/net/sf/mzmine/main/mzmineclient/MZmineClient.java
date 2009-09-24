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
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import net.sf.mzmine.desktop.helpsystem.HelpImp;
import net.sf.mzmine.desktop.impl.MainWindow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.main.MZminePreferences;
import net.sf.mzmine.project.impl.ProjectManagerImpl;
import net.sf.mzmine.taskcontrol.impl.TaskControllerImpl;

import org.dom4j.Document;
import org.dom4j.DocumentException;
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

		logger.finest("Checking for old temporary files...");

		try {

			// Get the temporary directory
			File tempDir = new File(System.getProperty("java.io.tmpdir"));

			// Find all files with the mask mzmine*.scans
			File remainingTmpFiles[] = tempDir.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.matches("mzmine.*\\.scans");
				}
			});

			for (File remainingTmpFile : remainingTmpFiles) {

				// Skip files created by someone else
				if (!remainingTmpFile.canWrite())
					continue;

				// Try to obtain a lock on the file
				RandomAccessFile rac = new RandomAccessFile(remainingTmpFile,
						"rw");

				FileLock lock = rac.getChannel().tryLock();
				rac.close();

				if (lock != null) {
					// We locked the file, which means nobody is using it
					// anymore and it can be removed
					logger.finest("Removing unused file " + remainingTmpFile);
					remainingTmpFile.delete();
				}

			}
		} catch (IOException e) {
			logger.log(Level.WARNING,
					"Error while checking for old temporary files", e);
		}

		SAXReader reader = new SAXReader();
		try {
			configuration = reader.read(CONFIG_FILE);
		} catch (DocumentException e1) {

			// If the file actually exists, show an error message
			if (CONFIG_FILE.exists()) {
				logger.log(Level.WARNING,
						"Error parsing the configuration file " + CONFIG_FILE
								+ ", loading default configuration", e1);
			}

			// Try to read the default config file
			try {
				configuration = reader.read(DEFAULT_CONFIG_FILE);
			} catch (DocumentException e2) {
				logger.log(Level.SEVERE,
						"Error parsing the default configuration file "
								+ DEFAULT_CONFIG_FILE, e2);
				System.exit(1);
			}
		}
		Element configRoot = configuration.getRootElement();

		logger.info("Starting MZmine 2");
		logger.info("Loading core classes..");

		// create instance of preferences
		MZmineCore.preferences = new MZminePreferences();

		// create instances of core modules
		TaskControllerImpl taskController = new TaskControllerImpl();
		projectManager = new ProjectManagerImpl();
		desktop = new MainWindow();
		help = new HelpImp();

		// save static references to MZmineCore
		MZmineCore.taskController = taskController;
		MZmineCore.desktop = desktop;

		logger.finer("Initializing core classes..");

		// First initialize project manager, because desktop needs to
		// register project listener
		projectManager.initModule();

		// Second, initialize desktop, because task controller needs to add
		// TaskProgressWindow to the desktop
		desktop.initModule();

		// Last, initialize task controller
		taskController.initModule();

		logger.finer("Loading modules");

		moduleSet = new Vector<MZmineModule>();

		Iterator<Element> modIter = configRoot.element(MODULES_ELEMENT_NAME)
				.elementIterator(MODULE_ELEMENT_NAME);

		while (modIter.hasNext()) {
			Element moduleElement = modIter.next();
			String className = moduleElement
					.attributeValue(CLASS_ATTRIBUTE_NAME);
			loadModule(className);
		}

		MZmineCore.initializedModules = moduleSet.toArray(new MZmineModule[0]);

		// load module configuration
		try {
			if (CONFIG_FILE.exists())
				loadConfiguration(CONFIG_FILE);
		} catch (DocumentException e) {
			logger.log(Level.WARNING,
					"Error while loading module configuration", e);
		}

		// register shutdown hook
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
			MZmineModule moduleInstance = (MZmineModule) moduleClass
					.newInstance();

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
}
