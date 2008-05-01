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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Logger;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.project.ProjectSavingTask;
import net.sf.mzmine.project.ProjectType;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.persistence.StreamStrategy;
import com.thoughtworks.xstream.persistence.XmlArrayList;

/**
 * project saving task with xstream library
 */

public class ProjectSavingTask_xstream implements ProjectSavingTask {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private File projectDir, oldProjectDir;
	private Boolean replace;
	private Boolean isTemporal;
	private Boolean ok;
	private TaskStatus status;
	private String errorMessage;
	private MZmineProjectImpl project;
	private float finished = (float) 0.0;
	private static final float FINISHED_STARTED = 0.1f;
	private static final float FINISHED_COPY_FILES = 0.7f;
	private static final float FINISHED_SAVE_DATA = 0.95f;
	private static final float FINISHED_COMPLETE = 1.0f;
	private ProjectType projectType;
	private HashMap option;

	public ProjectSavingTask_xstream(File projectDir) {
		this.projectDir = projectDir;
		status = TaskStatus.WAITING;
		option = new HashMap();
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		return "Saving project to " + projectDir;
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
	 * @see net.sf.mzmine.taskcontrol.Task#cancel()
	 */
	public void cancel() {
		logger.info("Cancelling saving of project" + projectDir);
		status = TaskStatus.CANCELED;
	}

	/**
	 * @see net.sf.mzmine.project.ProjectSavingTask#getResult()
	 */
	public MZmineProject getResult() {
		return project;
	}

	public void setOption(HashMap option) {
		this.option = option;
	}
	private boolean moveFile(File fromFile,File destFile,float start,float end){
		boolean ok;
		ok=this.copyFile(fromFile,destFile,start,end);
		if (ok==false){
			return false;
		}
		fromFile.delete();
		return true;
	}
	private boolean copyFile(File fromFile, File destFile, float start,
			float stop) {

		byte[] buffer = new byte[256 * 8 * 8 * 8];

		InputStream input_test;

		// calculate length of file
		int length_total = 0;
		int length_test = 0;
		try {
			input_test = new FileInputStream(fromFile);
			while (true) {
				length_test = input_test.read(buffer);
				if (length_test == -1) {
					break;
				}
				length_total += length_test;
			}
			input_test.close();
		} catch (Throwable e) {
			return false;
		}

		int length = 0;
		int length_read = 0;
		InputStream input;
		try {
			destFile.createNewFile();
			input = new FileInputStream(fromFile);
			OutputStream output = new FileOutputStream(destFile);
			while (true) {
				synchronized (buffer) {
					length = input.read(buffer);

					if (length != -1) {
						output.write(buffer, 0, length);
					} else {
						break;
					}
					length_read += length;
					finished = start + (stop - start) / length_total
							* length_read;
				}
			}
		} catch (IOException e) {
			// cancel process
			destFile.delete();
			return false;
		}
		return true;
	}

	private boolean copyDir(File fromDir, File destDir, float finished_start,
			float finished_stop) {
		boolean ok = false;

		if (!destDir.exists()) {
			ok = destDir.mkdirs();
			if (ok == false) {
				return false;
			}
		}

		File[] fromFiles = fromDir.listFiles();
		float sub_stop;
		float sub_start;
		for (int i = 0; i < fromFiles.length; i++) {
			File fromFile = fromFiles[i];
			File destFile = new File(destDir, fromFile.getName());
			sub_start = finished_start + (finished_stop - finished_start)
					/ fromFiles.length * i;
			sub_stop = finished_start + (finished_stop - finished_start)
					/ fromFiles.length * (i + 1);

			try {
				ok = this.copyFile(fromFile, destFile, sub_start, sub_stop);
			} catch (Throwable e) {
				return false;
			}

			finished = sub_stop;

		}
		return true;
	}

	private boolean moveDir(File fromDir, File destDir, float finished_start,
			float finished_stop) {
		boolean ok = false;
		ok = fromDir.renameTo(destDir);
		if (ok == false) {
			logger.info("Renaming failed : try copying... " + fromDir + "to "
					+ destDir);
			ok = moveDir_copy(fromDir, destDir, finished_start, finished_stop);
		}
		return true;
	}

	private boolean moveDir_copy(File fromDir, File destDir,
			float finished_start, float finished_stop) {
		boolean ok = false;

		if (!destDir.exists()) {
			ok = destDir.mkdirs();
			if (ok = false) {
				return false;
			}
		}

		ok = this.copyDir(fromDir, destDir, finished_start, finished_stop);
		if (ok == false) {
			return false;
		}
		this.removeDir(fromDir);
		return true;
	}

	private boolean removeDir(File dir) {
		boolean ok;
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				ok = removeDir(file);
				if (ok == false) {
					return false;
				}
			} else {
				ok = file.delete();
				if (ok == false) {
					return false;
				}
			}
		}
		ok = dir.delete();
		if (ok == false) {
			return false;
		}
		return true;
	}

	private Boolean preserveOldProject(float start, float end) {
		replace = true;
		logger
				.info("Moving original files (except scanfiles) to temporal space ");

		File tmpPlace;
		int index = 0;
		while (true) {
			tmpPlace = new File(oldProjectDir.toString() + ".tmp" + index);
			if (tmpPlace.exists()) {
				index++;
			} else {
				break;
			}
		}
		tmpPlace.mkdir();

		float subStart;
		float subEnd;
		String fileName;
		File fromFile;
		File destFile;
		File[] files = oldProjectDir.listFiles();

		for (int i = 0; i < files.length; i++) {
			fromFile = files[i];
			fileName = fromFile.getName();
			String fileTail = fileName.substring(fileName.length()
					- ".scan".length(), fileName.length());
			if (!fileTail.equals(".scan")) {
				destFile = new File(tmpPlace, fileName);
				subStart = start + (end - start) / files.length * i;
				subEnd = start + (end - start) / files.length * (i + 1);
				if (fromFile.isDirectory() == true) {
					ok = this.moveDir(fromFile, destFile, subStart, subEnd);
				} else {
					ok = this.moveFile(fromFile, destFile, subStart, subEnd);
				}
				if (ok != true) {
					this.removeDir(tmpPlace);
					errorMessage = "Could not save data into :" + projectDir;
					status = TaskStatus.ERROR;
					return false;
				}
			}
		}
		oldProjectDir = tmpPlace;
		return true;
	}

	private Boolean copyProjectDir(float start, float end) {
		replace = false;
		// Move files to new place
		ok = projectDir.mkdir();

		// check
		if (ok == false) {
			logger
					.fine("Failed to create project dir:"
							+ projectDir.toString());
			errorMessage = "Could not create project directory :" + projectDir;
			return false;
		}

		logger.info("Copying scanfiles in " + projectDir);
		ok = this.copyDir(oldProjectDir, projectDir, start, end);

		if (ok == false) {
			errorMessage = "Could not copy scan files to :" + projectDir;

			return false;
		}
		return true;
	}

	private void saveProject(XStream xstream, float start, float end)
			throws IOException {
		File xmlFile;
		OutputStreamWriter writer;
		ObjectOutputStream out;
		logger.info("Saving peakList in " + projectDir);

		xmlFile = new File(projectDir, "project.xml");

		writer = new OutputStreamWriter(new FileOutputStream(xmlFile), "UTF-8");
		out = xstream.createObjectOutputStream(writer);

		// start serializing objects
		out.writeObject(project);
		out.close();
		this.finished = end;
	}

	@SuppressWarnings("unchecked")
    private void savePeakLists(XStream xstream, float start, float end)
			throws IOException, FileNotFoundException {

		PeakList[] peakLists;
		StreamStrategy strategy;
		List xmlList;

		logger.info("Saving peakList in " + projectDir);
		File peakListDir = new File(projectDir, "peakLists");
		peakListDir.mkdir();

		strategy = new MZmineFileStreamStrategy(peakListDir, xstream, project,
				projectType);

		xmlList = new XmlArrayList(strategy);

		peakLists = project.getPeakLists();
		PeakList peakList;
		for (int i = 0; i < peakLists.length; i++) {
			peakList = peakLists[i];
			xmlList.add(peakList);
			finished = start + (end - start) * i / peakLists.length;
		}
		this.finished = end;
	}

	@SuppressWarnings("unchecked")
    private void saveRawDataFiles(XStream xstream, float start, float end)
			throws IOException {

		RawDataFile[] dataFiles;
		StreamStrategy strategy;
		dataFiles = project.getDataFiles();
		List xmlList;

		logger.info("Saving datafiles in " + projectDir);
		xstream.alias("rawDataFile", RawDataFile.class);

		File dataFileDir = new File(projectDir, "dataFiles");
		dataFileDir.mkdir();

		strategy = new MZmineFileStreamStrategy(dataFileDir, xstream, project,
				projectType);
		xmlList = new XmlArrayList(strategy);

		dataFiles = MZmineCore.getCurrentProject().getDataFiles();
		RawDataFile dataFile;

		for (int i = 0; i < dataFiles.length; i++) {
			dataFile = dataFiles[i];
			xmlList.add(dataFile);
			finished = start + (end - start) * i / dataFiles.length;
		}
		this.finished = end;
	}

	private void saveParameters(XStream xstream, float start, float end)
			throws IOException {
		File xmlFile;
		OutputStreamWriter writer;
		ObjectOutputStream out;
		Hashtable<Parameter, Hashtable<String, Object>> projectParameters;

		projectParameters = project.getProjectParameters();

		logger.info("Saving parameters in" + projectDir);
		xmlFile = new File(projectDir, "parameters.xml");
		writer = new OutputStreamWriter(new FileOutputStream(xmlFile), "UTF-8");
		out = xstream.createObjectOutputStream(writer);
		out.writeObject(projectParameters);
		out.close();
		out = null;
		this.finished = end;

	}

	private void saveInfo(XStream xstream, float start, float end)
			throws IOException {
		File xmlFile;
		OutputStreamWriter writer;
		ObjectOutputStream out;

		logger.info("Saving additional infos in" + projectDir);
		xmlFile = new File(projectDir, "info.xml");
		writer = new OutputStreamWriter(new FileOutputStream(xmlFile), "UTF-8");

		HashMap<String, String> info = new HashMap<String, String>();
		info.put("projectType", this.projectType.toString());
		info.put("Save format", "version.2");
		out = xstream.createObjectOutputStream(writer);
		out.writeObject(info);
		out.close();
		out = null;
		this.finished = end;
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		// Update task status
		try {
			logger.info("Started saving project" + projectDir);
			status = TaskStatus.PROCESSING;
			finished = FINISHED_STARTED;
			project = (MZmineProjectImpl) MZmineCore.getCurrentProject();
			float start;
			float end;

			// store current status
			oldProjectDir = project.getLocation();
			isTemporal = project.getIsTemporal();
			ok = false;

			// if save existing project, make temporal project and rename it

			start = FINISHED_STARTED;
			end = FINISHED_COPY_FILES;
			if (oldProjectDir == this.projectDir) {
				ok = this.preserveOldProject(start, end);
			} else {
				ok = this.copyProjectDir(start, end);
			}
			if (ok != true) {
				return;
			}

			// only store part of the project to xml

			logger.info("Prepareing save for " + projectDir);
			project.setLocation(projectDir);
			int NUM_STEP = 6;
			try {

				// setup xstream
				XStream xstream = new XStream();
				if (this.option != null && this.option.containsKey("zip")
						&& this.option.get("zip").equals(true)) {
					projectType = ProjectType.zippedXML;
				} else {
					projectType = ProjectType.xml;
				}

				start = finished;
				end = FINISHED_COPY_FILES
						+ (FINISHED_SAVE_DATA - FINISHED_COPY_FILES) / NUM_STEP
						* 1;
				this.saveProject(xstream, start, end);

				// save project files
				start = finished;
				end = FINISHED_COPY_FILES
						+ (FINISHED_SAVE_DATA - FINISHED_COPY_FILES) / NUM_STEP
						* 2;
				this.saveRawDataFiles(xstream, start, end);

				// save peak lists
				start = finished;
				end = FINISHED_COPY_FILES
						+ (FINISHED_SAVE_DATA - FINISHED_COPY_FILES) / NUM_STEP
						* 3;
				this.savePeakLists(xstream, start, end);

				// save parameters
				start = finished;
				end = FINISHED_COPY_FILES
						+ (FINISHED_SAVE_DATA - FINISHED_COPY_FILES) / NUM_STEP
						* 4;
				this.saveParameters(xstream, start, end);

				// store additional info about project
				start = finished;
				finished = FINISHED_COPY_FILES
						+ (FINISHED_SAVE_DATA - FINISHED_COPY_FILES) / NUM_STEP
						* 5;
				this.saveInfo(xstream, start, end);

				// store configFile
				File configFile = new File(projectDir, "config.xml");
				MZmineCore.saveConfiguration(configFile);

				finished = FINISHED_SAVE_DATA;
				logger.info("Complete converting" + projectDir);

			} catch (Throwable e) {
				errorMessage = "Could not save data into :" + projectDir;
				this.rollback();
				return;
			}

			if (isTemporal || replace == true) {
				// remove old project
				this.removeDir(oldProjectDir);
			}
			project.setIsTemporal(false);

			logger.info("Finished saving " + projectDir);
			status = TaskStatus.FINISHED;
			finished = FINISHED_COMPLETE;

		} catch (Throwable e) {
			status = TaskStatus.ERROR;
			//
		}
	}

	private void rollback() {
		try {
			this.removeDir(projectDir);
		} catch (Throwable e) {
			// do nothing
		}
		if (this.replace == true) {
			File destFile;
			for (File file : oldProjectDir.listFiles()) {
				destFile = new File(projectDir, file.getName());
				ok = this.copyFile(file, destFile, finished, finished);
				file.delete();
			}
			oldProjectDir.delete();
		}
		status = TaskStatus.ERROR;
	}

}
