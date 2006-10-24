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

import java.io.File;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import net.sf.mzmine.io.IOController;
import net.sf.mzmine.io.IOController.PreloadLevel;
import net.sf.mzmine.io.impl.IOControllerImpl;
import net.sf.mzmine.methods.alignment.filterbygaps.AlignmentResultFilterByGapsParameters;
import net.sf.mzmine.methods.alignment.join.JoinAlignerParameters;
import net.sf.mzmine.methods.deisotoping.incompletefilter.IncompleteIsotopePatternFilterParameters;
import net.sf.mzmine.methods.deisotoping.simplegrouper.SimpleIsotopicPeaksGrouperParameters;
import net.sf.mzmine.methods.filtering.chromatographicmedian.ChromatographicMedianFilterParameters;
import net.sf.mzmine.methods.filtering.crop.CropFilterParameters;
import net.sf.mzmine.methods.filtering.mean.MeanFilterParameters;
import net.sf.mzmine.methods.filtering.savitzkygolay.SavitzkyGolayFilterParameters;
import net.sf.mzmine.methods.filtering.zoomscan.ZoomScanFilterParameters;
import net.sf.mzmine.methods.gapfilling.simple.SimpleGapFillerParameters;
import net.sf.mzmine.methods.normalization.linear.LinearNormalizerParameters;
import net.sf.mzmine.methods.peakpicking.centroid.CentroidPickerParameters;
import net.sf.mzmine.methods.peakpicking.local.LocalPickerParameters;
import net.sf.mzmine.methods.peakpicking.recursivethreshold.RecursiveThresholdPickerParameters;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.impl.TaskControllerImpl;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;

public class MZmineClient implements Runnable, MZmineCore {

    private static final String CONFIG_PROPERTIES = "conf/config";

    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    private HashSet<MZmineModule> moduleSet;
    
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

        // configuration properties
        ResourceBundle configBundle = ResourceBundle.getBundle(CONFIG_PROPERTIES);

        // get the configured number of computation nodes
        int numberOfNodes;
        try {
            String numberOfNodesConfigEntry = configBundle.getString("NumberOfNodes");
            numberOfNodes = Integer.parseInt(numberOfNodesConfigEntry);
        } catch (Exception e) {
            numberOfNodes = Runtime.getRuntime().availableProcessors();
        }

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
        
        moduleSet = new HashSet<MZmineModule>();

        // get module classes and trim spaces from their names
        String[] modules = configBundle.getString("Modules").split(" *[,;:] *");

        for (String className : modules) {

            try {

                logger.finest("Loading module " + className);
                
                // load the module class
                Class moduleClass = Class.forName(className);

                // create instance
                MZmineModule moduleInstance = (MZmineModule) moduleClass.newInstance();

                // init module
                moduleInstance.initModule(this);
                
                // add to the module set
                moduleSet.add(moduleInstance);

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Could not load module "
                        + className, e);
            }

        }

        // show the GUI
        logger.finer("Showing main window");
        mainWindow.setVisible(true);
        
        // show the welcome message
        mainWindow.setStatusBarText("Welcome to MZmine!");
        
        
///////////////////////////////////////////////////////////        
        // DEBUG test one dialog
         /*
        ParameterSetupDialog psdialog 
        = new ParameterSetupDialog
        		(		MainWindow.getInstance(),
        				"Testing...",
        				new JoinAlignerParameters()
        		);
        psdialog.setVisible(true);
        MainWindow.getInstance().notifySelectionListeners();
        */
        
        
        // DEBUG open files
        /*
        File[] selectedFiles = new File[2];
        selectedFiles[0] = new File("C:/VTT/Data_Netcdf/NewCentroidMouse/cdf/ob_f1a.cdf");
        selectedFiles[1] = new File("C:/VTT/Data_Netcdf/NewCentroidMouse/cdf/ob_f1b.cdf");
        ioController.openFiles(selectedFiles, PreloadLevel.NO_PRELOAD);
         */

        
///////////////////////////////////////////////////////////        

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

}