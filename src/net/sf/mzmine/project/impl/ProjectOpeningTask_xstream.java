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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineClient;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.project.ProjectOpeningTask;
import net.sf.mzmine.project.ProjectType;
import net.sf.mzmine.project.converters.RawDataFileConverter;

import com.thoughtworks.xstream.XStream;

/**
 * project opening task with xstream library
 */
public class ProjectOpeningTask_xstream implements ProjectOpeningTask {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private File projectDir;
	private TaskStatus status;
	private String errorMessage;
	private ProjectType projectType;
	private HashMap option;

	private float FINISHED_STARTED = 0.1f;
	private float FINISHED_LOADED = 0.8f;
	private float FINISHED_COMPLETE = 1.0f;

	private float finished;
	MZmineProjectImpl project;

	public ProjectOpeningTask_xstream(File projectDir) {
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

	public void setOption(HashMap option) {
		this.option = option;
	}

	private MZmineProjectImpl loadProject(XStream xstream, float start,
			float end) throws IOException, ClassNotFoundException {
		File xmlFile;
		InputStreamReader reader;
		ObjectInputStream in;

		logger.info("Loading project");
		xmlFile = new File(projectDir, "project.xml");
		reader = new InputStreamReader(new FileInputStream(xmlFile), "UTF-8");
		in = xstream.createObjectInputStream(reader);
		in.close();
		finished = end;
		return (MZmineProjectImpl) in.readObject();
	}

	private HashMap<String, String> loadInfo(XStream xstream, float start,
			float end) throws IOException, FileNotFoundException,
			ClassNotFoundException {
		File xmlFile;
		InputStreamReader reader;
		ObjectInputStream in;
		xmlFile = new File(projectDir, "info.xml");
		logger.info("Loading Info");
		HashMap<String, String> info;
		if (xmlFile.exists()) {
			reader = new InputStreamReader(new FileInputStream(xmlFile),
					"UTF-8");
			in = xstream.createObjectInputStream(reader);
			info = (HashMap<String, String>) in.readObject();
			in.close();

		} else {
			// create dummy info
			info = new HashMap<String, String>();
			info.put("numDataFiles", "100");

		}
		finished = end;
		return info;
	}

	private void loadRawDataFiles(XStream xstream, MZmineProject project,
			float start, float end) throws IOException, ClassNotFoundException {

		final String DIR = "dataFiles";
		int numDataFiles;

		logger.info("Loading RawDataFiles");
		File inDir=new File(projectDir, DIR);
		DirectoryStorage storage = new DirectoryStorage(xstream, inDir);

		ArrayList<String> dataFileList = storage.readIndex();
		numDataFiles = dataFileList.size();

		RawDataFile rawDataFile;
		int i;
		for (i = 0; i < numDataFiles; i++) {
			rawDataFile = (RawDataFile) storage.get(i);
			logger.info("Loading RawDataFile:" + rawDataFile.getFileName());
			project.addFile(rawDataFile);
			finished = start + (end - start) / numDataFiles * (i + 1);
		}
		finished = end;
	}

	private void loadPeakListsListModel(XStream xstream, MZmineProject project,
			float start, float end) throws IOException, ClassNotFoundException {

		int numPeakLists;
		final String DIR = "peakLists";
		
		logger.info("Loading RawDataFiles");
		
		File inDir=new File(projectDir, DIR);
		DirectoryStorage storage = new DirectoryStorage(xstream, inDir);

		ArrayList<String> peakListsList = storage.readIndex();
		numPeakLists = peakListsList.size();

		PeakList peakList;
		int i;
		for (i = 0; i < numPeakLists; i++) {

			peakList = (PeakList) storage.get(i);
			logger.info("Loading peakList:" + peakList.toString());
			project.addPeakList(peakList);
			finished = start + (end - start) / numPeakLists * (i + 1);
		}
		finished = end;
	}

	private Hashtable<Parameter, Hashtable<String, Object>> loadParameter(
			XStream xstream, float start, float end) throws IOException,
			ClassNotFoundException {
		File xmlFile;
		InputStreamReader reader;
		ObjectInputStream in;
		Hashtable<Parameter, Hashtable<String, Object>> projectParameters;
		logger.info("Loading Parameter");
		xmlFile = new File(projectDir, "parameters.xml");
		reader = new InputStreamReader(new FileInputStream(xmlFile), "UTF-8");
		in = xstream.createObjectInputStream(reader);
		projectParameters = (Hashtable<Parameter, Hashtable<String, Object>>) in
				.readObject();
		in.close();
		finished = end;
		return projectParameters;
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

			Hashtable<Parameter, Hashtable<String, Object>> projectParameters;
			HashMap<String, String> info;
			int NUM_STEP = 7;
			float start;
			float end;

			// register converter for specific type
			XStream xstream = MZmineXStream.getXstream();
            
            // set references to ID, because XPath consumes too much memory
            xstream.setMode(XStream.ID_REFERENCES);

			// restore info first
			start = finished;
			end = FINISHED_STARTED + (FINISHED_LOADED - FINISHED_STARTED)
					/ NUM_STEP * 1;

			info = this.loadInfo(xstream, start, end);
			this.projectType = ProjectType.valueOf(info.get("projectType"));

			// restore project
			start = finished;
			end = FINISHED_STARTED + (FINISHED_LOADED - FINISHED_STARTED)
					/ NUM_STEP * 2;
			project = this.loadProject(xstream, start, end);

			// restore data filess
			start = finished;
			end = FINISHED_STARTED + (FINISHED_LOADED - FINISHED_STARTED)
					/ NUM_STEP * 3;

			this.loadRawDataFiles(xstream, project, start, end);

			RawDataFileConverter rawDataFileConverter = (RawDataFileConverter) xstream
					.getConverterLookup().lookupConverterForType(
							RawDataFile.class);
			rawDataFileConverter.setMode(RawDataFileConverter.Mode.SIMPLE);

			// restore peak lists
			start = finished;
			end = FINISHED_STARTED + (FINISHED_LOADED - FINISHED_STARTED)
					/ NUM_STEP * 4;

			this.loadPeakListsListModel(xstream, project, start, end);

			// restore parameters
			start = finished;
			end = FINISHED_STARTED + (FINISHED_LOADED - FINISHED_STARTED)
					/ NUM_STEP * 5;
			projectParameters = this.loadParameter(xstream, start, end);
			project.setProjectParameters(projectParameters);

			// load configuraton
			start = finished;
			end = FINISHED_STARTED + (FINISHED_LOADED - FINISHED_STARTED)
					/ NUM_STEP * 6;

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

}
