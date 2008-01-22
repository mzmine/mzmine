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

package net.sf.mzmine.io.impl;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.io.IOController;
import net.sf.mzmine.io.PreloadLevel;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.RawDataFileWriter;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.impl.MZmineProjectImpl;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.mainwindow.ItemSelector;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;

/**
 * IO controller
 */
public class IOControllerImpl implements IOController, TaskListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private TaskController taskController;
    private Desktop desktop;

    /**
     * This method is non-blocking, it places a request to open these files and
     * exits immediately.
     */
    public void openFiles(File[] files, PreloadLevel preloadLevel) {

        Task openTask;

        for (File file : files) {

            openTask = new FileOpeningTask(file, preloadLevel);
            taskController.addTask(openTask, this);
        }

    }
    
    public void openProject(File projectDir) throws IOException {
        Task openTask = new ProjectOpeningTask(projectDir);
        taskController.addTask(openTask, this);
    }
    public void saveProject(File projectDir) throws IOException {
        Task saveTask = new ProjectSavingTask(projectDir);
        taskController.addTask(saveTask, this);	
    }
    /**
     * This method is called when the file opening task is finished.
     * 
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskFinished(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskFinished(Task task) {
    	if (task instanceof FileOpeningTask) {
			if (task.getStatus() == Task.TaskStatus.FINISHED) {
				RawDataFile newFile = ((FileOpeningTask) task).getResult();
				MZmineCore.getCurrentProject().addFile(newFile);
			} else if (task.getStatus() == Task.TaskStatus.ERROR) {
				/* Task encountered an error */
				logger
						.severe("Error opening a file: "
								+ task.getErrorMessage());
				desktop.displayErrorMessage("Error opening a file: "
						+ task.getErrorMessage());

			}
		}
    	else if(task instanceof ProjectOpeningTask){
    		if (task.getStatus() == Task.TaskStatus.FINISHED) {
    			//do nothing
				MZmineProjectImpl project=(MZmineProjectImpl)((ProjectOpeningTask) task).getResult();
				MZmineCore.setProject(project);
				//restore user interface
    			//add files to fileList
    			MainWindow window=(MainWindow)MZmineCore.getDesktop();
    			ItemSelector itemSelector=window.getItemSelector();
    			itemSelector.removeAll();
    			for (RawDataFile file:project.getDataFiles()){
    				itemSelector.addRawData(file);
    			}
    			
    			//add resultList to peak list pane
    			for(PeakList peakList:project.getPeakLists()){
    				itemSelector.addPeakList(peakList);
    			}
				
				
			} else if (task.getStatus() == Task.TaskStatus.ERROR) {
				/* Task encountered an error */
				logger.severe("Error opening a project: "
						+ task.getErrorMessage());
				desktop.displayErrorMessage("Error opening a project: "
						+ task.getErrorMessage());
			}
		}
    	else if(task instanceof ProjectSavingTask){
    		if (task.getStatus() == Task.TaskStatus.FINISHED) {
    			//do nothing
			} else if (task.getStatus() == Task.TaskStatus.ERROR) {
				/* Task encountered an error */
				logger.severe("Error saving a project: "
						+ task.getErrorMessage());
				desktop.displayErrorMessage("Error saving a project: "
						+ task.getErrorMessage());
			}
		}
    }

    /**
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskStarted(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskStarted(Task task) {
        // do nothing
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule() {
        this.taskController = MZmineCore.getTaskController();
        this.desktop = MZmineCore.getDesktop();
    }

    
    /**
     * @see net.sf.mzmine.io.IOController#createNewFile(java.lang.String,
     *      net.sf.mzmine.io.PreloadLevel)
     */
    public RawDataFileWriter createNewFile(String fileName,String suffix,
            PreloadLevel preloadLevel) throws IOException {
        return new RawDataFileImpl(fileName,suffix, preloadLevel);
    }
    
    /**
     * @see net.sf.mzmine.io.IOController#createNewFile(java.lang.String,
     *      net.sf.mzmine.io.PreloadLevel)
     */
    public RawDataFileWriter createNewFile(File file,
            PreloadLevel preloadLevel) throws IOException {
        return new RawDataFileImpl(file.getName(),"scan", preloadLevel);
    }
    

}
