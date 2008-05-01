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

package net.sf.mzmine.project.impl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Logger;

import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.impl.MainWindow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.project.ProjectListener;
import net.sf.mzmine.project.ProjectManager;
import net.sf.mzmine.project.ProjectOpeningTask;
import net.sf.mzmine.project.ProjectSavingTask;
import net.sf.mzmine.project.ProjectStatus;
import net.sf.mzmine.project.ProjectTask;
import net.sf.mzmine.project.ProjectType;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskListener;

/**
 * project manager implementation Using reflection to support different
 * implementation of actual tasks
 */
public class ProjectManagerImpl implements ProjectManager, TaskListener {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private TaskController taskController;
	private Desktop desktop;
	// status to check for app quitting or etc...
	private ProjectStatus status;
	private ProjectType projectType;

	public ProjectManagerImpl(ProjectType projectType) {
		this.status = ProjectStatus.Idle;
		this.projectType = projectType;
		this.initModule();
	}

	/**
	 * This method is non-blocking, it places a request to open these files and
	 * exits immediately.
	 */
	public synchronized void createTemporalProject() throws IOException {
		File projectDir = File.createTempFile("mzmine", null);
		projectDir.delete();
		this._createProject(projectDir, true);
	}

	public synchronized void createProject(File projectDir) throws IOException {
		this._createProject(projectDir, false);
	}

	public void _createProject(File projectDir, Boolean isTemporal) {
		status = ProjectStatus.Processing;
		Boolean ok;

		// Check to delete temporary project after creation of new project
		MZmineProject oldProject;
		File oldProjectDir = null;
		Boolean removeOld = false;
		if (isTemporal != true) {
			oldProject = MZmineCore.getCurrentProject();
			if (oldProject.getIsTemporal() == true) {
				removeOld = true;
				oldProjectDir = oldProject.getLocation();
			}
		}

		try {
			ok = projectDir.mkdir();
			if (ok == false) {
				throw new IOException();
			}

			MZmineProjectImpl project = new MZmineProjectImpl(projectDir);
			project.setLocation(projectDir);
			project.setIsTemporal(isTemporal);
			if (desktop != null) {
				project.addProjectListener((MainWindow) desktop);
			}
			MZmineCore.setProject(project);

			if (removeOld == true) {
				this.removeProjectDir(oldProjectDir);
			}

		} catch (Throwable e) {
			String msg = "Error in cerating project directory";
			logger.severe(msg);
			desktop.displayErrorMessage(msg);
			return;
		} finally {
			status = ProjectStatus.Idle;
		}

	}

	public synchronized void openProject(File projectDir) throws IOException {
		this.openProject(projectDir, null);
	}

	public synchronized void openProject(File projectDir, HashMap options)
			throws IOException {
		try {
			//Check Project format
			
			String version;
			String fileNames[]=projectDir.list();
			if (!Arrays.asList( fileNames).contains("dataFiles" )){
				//format is v1
				version="1";
			}else{
				version="";
			}
			
			String className="net.sf.mzmine.project.impl.ProjectOpeningTask_"
				+ this.projectType.toString();
			if (!version.equals("")){
				className=className+"_"+version;
			}
			Class projectClass = Class
					.forName(className);
			Constructor projectConst = projectClass.getConstructor(File.class);
			ProjectTask openTask = (ProjectTask) projectConst
					.newInstance(projectDir);
			openTask.setOption(options);
			taskController.addTask(openTask, this);
			status = ProjectStatus.Processing;

		} catch (Throwable e) {
			String msg = "Error in starting project opening: ";
			logger.severe(msg);
			desktop.displayErrorMessage(msg);
			return;
		}
	}

	public synchronized void saveProject(File projectDir) throws IOException {
		this.saveProject(projectDir, null);
	}

	public synchronized void saveProject(File projectDir, HashMap options)
			throws IOException {

		try {
			String className="net.sf.mzmine.project.impl.ProjectSavingTask_"
				+ this.projectType.toString();
			Class projectClass = Class
					.forName(className);
			Constructor projectConst = projectClass.getConstructor(File.class);
			ProjectTask saveTask = (ProjectTask) projectConst
					.newInstance(projectDir);
			saveTask.setOption(options);
			taskController.addTask(saveTask, this);
			status = ProjectStatus.Processing;

		} catch (Throwable e) {
			String msg = "Error in starting project saving: ";
			logger.severe(msg);
			desktop.displayErrorMessage(msg);
			return;
		}
	}

	public void removeProjectDir(File projectDir) {
		for (File file : projectDir.listFiles()) {
			file.delete();
		}
		projectDir.delete();
	}

	/**
	 * This method is called when the file opening task is finished.
	 * 
	 * @see net.sf.mzmine.taskcontrol.TaskListener#taskFinished(net.sf.mzmine.taskcontrol.Task)
	 */
	public void taskFinished(Task task) {
		if (task instanceof ProjectOpeningTask) {
			if (task.getStatus() == Task.TaskStatus.FINISHED) {
				MZmineProject project = ((ProjectTask) task).getResult();
				MZmineCore.setProject(project);

				// transfer listeners in the old project to new one
				project.addProjectListener((MainWindow) desktop);
			} else if (task.getStatus() == Task.TaskStatus.ERROR) {
				/* Task encountered an error */
				logger.severe("Error in project processing: "
						+ task.getErrorMessage());
				desktop.displayErrorMessage("Error: " + task.getErrorMessage());
			}

		} else if (task instanceof ProjectSavingTask) {
			if (task.getStatus() == Task.TaskStatus.FINISHED) {

				// transfer listeners in the old project to new one
				MZmineProject project = MZmineCore.getCurrentProject();
				for (ProjectListener listener : project.getProjectListeners()) {
					listener.projectModified(
							ProjectListener.ProjectEvent.PROJECT_CHANGED,
							project);
				}

			} else if (task.getStatus() == Task.TaskStatus.ERROR) {
				/* Task encountered an error */
				logger.severe("Error in project processing: "
						+ task.getErrorMessage());
				desktop.displayErrorMessage("Error: " + task.getErrorMessage());
			}

		}
		status = ProjectStatus.Idle;
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
	 * @see net.sf.mzmine.project.ProjectManager#getStatus()
	 */
	public ProjectStatus getStatus() {
		return status;

	}

}
