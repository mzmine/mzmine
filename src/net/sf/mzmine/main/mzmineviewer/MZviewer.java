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

package net.sf.mzmine.main.mzmineviewer;

import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.project.impl.ProjectManagerImpl;
import net.sf.mzmine.taskcontrol.impl.TaskControllerImpl;

public class MZviewer extends MZmineCore implements Runnable {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    // make MZviewer a singleton
    private static MZviewer client = new MZviewer();
    
    private Vector<MZmineModule> moduleSet;
    
    // arguments
    private String args[];

    private MZviewer() {
    }

    public static MZviewer getInstance() {
        return client;
    }

    /**
     * Main method
     */
    public static void main(String args[]) {
    	
    	client.args = args;

        // create the GUI in the event-dispatching thread
        SwingUtilities.invokeLater(client);

    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        MZviewerWindow desktop = null;
        try {

            logger.info("MZviewer starting");

            // create instances of core modules
			TaskControllerImpl taskController = new TaskControllerImpl();
            desktop = new MZviewerWindow();
            ProjectManagerImpl projectManager = new ProjectManagerImpl();

            // save static references to MZmineCore
            MZmineCore.taskController = taskController;
            MZmineCore.desktop = desktop;
            MZmineCore.projectManager = projectManager;

            logger.finer("Initializing core classes");

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
            
            for (MZviewerModuleName module: MZviewerModuleName.values()){
            	loadModule(module.getClassName());
            }

            MZmineCore.initializedModules = moduleSet.toArray(new MZmineModule[0]);
            
            MZmineCore.isLightViewer = true;
           

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not initialize MZviewer ", e);
            System.exit(1);
        }


        // show the GUI
        logger.finest("Showing main window");
        desktop.setVisible(true);

        // show the welcome message
        desktop.setStatusBarText("Welcome to MZviewer!");
        
        // Retrieving peak list arguments
        if (args.length > 0){
            logger.finer("Loading peak lists from arguments");
        	desktop.addPeakLists(args);
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
    

}
