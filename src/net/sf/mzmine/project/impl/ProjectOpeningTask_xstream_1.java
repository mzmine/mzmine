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

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineClient;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.project.ProjectOpeningTask;

import com.thoughtworks.xstream.XStream;

/**
 * project opening task with xstream library
 */
public class ProjectOpeningTask_xstream_1 implements ProjectOpeningTask {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private File projectDir;
	private TaskStatus status;
	private String errorMessage;

	private float FINISHED_STARTED = 0.1f;
	private float FINISHED_LOADED = 0.8f;
	private float FINISHED_COMPLETE = 1.0f;

	private float finished;
	MZmineProjectImpl project;

	public ProjectOpeningTask_xstream_1(File projectDir) {
		this.projectDir = projectDir;
		status = TaskStatus.WAITING;
		finished = FINISHED_STARTED;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		return "Opening project" + projectDir;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
	public float getFinishedPercentage() {
		return finished;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getStatus()
	 */
	public TaskStatus getStatus() {
		return status;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getErrorMessage()
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * @see net.sf.mzmine.project.ProjectOpeningTask#getResult()
	 */
	public MZmineProjectImpl getResult() {
		return project;
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		// Update task status
		logger.info("Started openning project" + projectDir);
		status = TaskStatus.PROCESSING;
		finished = FINISHED_STARTED;

		Boolean removeOld = false;
		File oldProjectDir = null;
		MZmineProject oldProject = MZmineCore.getCurrentProject();
		if (oldProject.getIsTemporal() == true) {
			removeOld = true;
			oldProjectDir = oldProject.getLocation();
		}

		try {

			DefaultListModel rawDataListModel;
			DefaultListModel peakListsListModel;

			Hashtable<Parameter, Hashtable<String, Object>> projectParameters;

			XStream xstream = new XStream();
			File xmlFile;
			InputStreamReader reader;
			ObjectInputStream in;
			int NUM_STEP = 7;
			int step = 1;

			float start;
			float end;
			int count;

			int numDataFiles;
			int numPeakLists;

			// restore info first
			xmlFile = new File(projectDir, "info.xml");

			if (xmlFile.exists()) {
				reader = new InputStreamReader(new FileInputStream(xmlFile),
						"UTF-8");
				in = xstream.createObjectInputStream(reader);
				HashMap<String, String> info = (HashMap<String, String>) in
						.readObject();
				in.close();
				numDataFiles = Integer.parseInt(info.get("numDataFiles"));
				numPeakLists = Integer.parseInt(info.get("numPeakLists"));
			} else {
				//
				numDataFiles = 100;
				numPeakLists = 100;
			}
			finished = FINISHED_STARTED + (FINISHED_LOADED - FINISHED_STARTED)
					/ NUM_STEP * 1;

			// restore project
			xmlFile = new File(projectDir, "project.xml");
			reader = new InputStreamReader(new FileInputStream(xmlFile),
					"UTF-8");
			in = xstream.createObjectInputStream(reader);
			project = (MZmineProjectImpl) in.readObject();
			in.close();
			finished = FINISHED_STARTED + (FINISHED_LOADED - FINISHED_STARTED)
					/ NUM_STEP * 2;

			// restore data files
			step = 3;
			start = finished;
			end = FINISHED_STARTED + (FINISHED_LOADED - FINISHED_STARTED)
					/ NUM_STEP * step;

			xmlFile = new File(projectDir, "dataFiles.xml");
			reader = new InputStreamReader(new FileInputStream(xmlFile),
					"UTF-8");

			in = xstream.createObjectInputStream(reader);
			rawDataListModel = new DefaultListModel();
			count = 0;
			while (true) {
				try {
					rawDataListModel.addElement(in.readObject());
					finished = start + (end - start) / numDataFiles
							* (count + 1);
					count++;
				} catch (EOFException e) {
					break;
				}
			}
			in.close();
			project.setRawDataListModel(rawDataListModel);
			finished = end;

			// restore peak lists
			step = 4;
			start = finished;
			end = finished = FINISHED_STARTED
					+ (FINISHED_LOADED - FINISHED_STARTED) / NUM_STEP * step;

			xmlFile = new File(projectDir, "peakLists.xml");
			reader = new InputStreamReader(new FileInputStream(xmlFile),
					"UTF-8");
			in = xstream.createObjectInputStream(reader);
			peakListsListModel = new DefaultListModel();
			count = 0;
			while (true) {
				try {
					peakListsListModel.addElement(in.readObject());
					finished = start + (end - start) / numPeakLists
							* (count + 1);
					count++;
				} catch (EOFException e) {
					break;
				}
			}
			in.close();
			project.setPeakListsListModel(peakListsListModel);
			finished = end;

			// restore parameters
			xmlFile = new File(projectDir, "parameters.xml");
			reader = new InputStreamReader(new FileInputStream(xmlFile),
					"UTF-8");
			in = xstream.createObjectInputStream(reader);
			projectParameters = (Hashtable<Parameter, Hashtable<String, Object>>) in
					.readObject();
			in.close();
			finished = FINISHED_STARTED + (FINISHED_LOADED - FINISHED_STARTED)
					/ NUM_STEP * 5;

			project.setPeakListsListModel(peakListsListModel);
			project.setRawDataListModel(rawDataListModel);
			project.setProjectParameters(projectParameters);
			finished = FINISHED_STARTED + (FINISHED_LOADED - FINISHED_STARTED)
					/ NUM_STEP * 6;

			// load configuraton
			File configFile = new File(projectDir, "config.xml");
			MZmineCore.loadConfiguration(configFile);

			finished = FINISHED_LOADED;

			// reset project state
			project.setLocation(projectDir);
			project.setIsTemporal(false);

			// update scanDataFile in rawDataFiles
			for (RawDataFile file : project.getDataFiles()) {
				if (!file.getScanDataFileName().equals(null)) {
					File filePath = new File(project.getLocation(), file
							.getScanDataFileName());
					file.updateScanDataFile(filePath);
				}
			}

			// remove old project if old one is temporal
			if (removeOld == true) {
				MZmineClient.getInstance().getProjectManager()
						.removeProjectDir(oldProjectDir);
			}
			finished = FINISHED_COMPLETE;

		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Could not open project "
					+ projectDir.getPath(), e);
			errorMessage = e.toString();
			status = TaskStatus.ERROR;
			return;
		}

		logger.info("Finished openning " + projectDir);
		status = TaskStatus.FINISHED;

	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#cancel()
	 */
	public void cancel() {
		logger.info("Cancelling opening of project" + projectDir);
		status = TaskStatus.CANCELED;
	}

	public void setOption(HashMap option) {
		//do nothing		
	}

}
