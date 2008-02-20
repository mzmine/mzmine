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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Hashtable;
import java.util.logging.Logger;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.project.ProjectSavingTask;

import com.thoughtworks.xstream.XStream;

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

	public ProjectSavingTask_xstream(File projectDir) {
		this.projectDir = projectDir;
		status = TaskStatus.WAITING;
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

	private boolean copyFile(File fromFile, File destFile, float start,
			float stop) {

		byte[] buffer = new byte[256 * 8 * 8];

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

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		// Update task status
		logger.info("Started saving project" + projectDir);
		status = TaskStatus.PROCESSING;
		finished = FINISHED_STARTED;
		project = (MZmineProjectImpl) MZmineCore.getCurrentProject();

		// store current status
		oldProjectDir = project.getLocation();
		isTemporal = project.getIsTemporal();
		ok = false;

		// if save existing project, make temporal project and rename it

		if (oldProjectDir == this.projectDir) {
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

			float start;
			float stop;
			String fileName;
			File fromFile;
			File destFile;
			File[] files = oldProjectDir.listFiles();

			for (int i = 0; i < files.length; i++) {
				fromFile = files[i];
				fileName = fromFile.getName();
				if (fileName.substring(fileName.length() - ".scan".length(),
						fileName.length()) != ".scan") {
					destFile = new File(tmpPlace, fileName);
					start = FINISHED_STARTED
							+ (FINISHED_COPY_FILES - FINISHED_STARTED)
							/ files.length * i;
					stop = FINISHED_STARTED
							+ (FINISHED_COPY_FILES - FINISHED_STARTED)
							/ files.length * (i + 1);
					ok = this.copyFile(fromFile, destFile, start, stop);

					if (ok != true) {
						this.removeDir(tmpPlace);
						errorMessage = "Could not save data into :"
								+ projectDir;
						status = TaskStatus.ERROR;
						return;
					}
				}
			}
			oldProjectDir = tmpPlace;

		} else {
			replace = false;
			// Move files to new place
			ok = projectDir.mkdir();

			// check
			if (ok == false) {
				logger.fine("Failed to create project dir:"
						+ projectDir.toString());
				errorMessage = "Could not create project directory :"
						+ projectDir;
				this.rollback();
				return;
			}

			logger.info("Copying scanfiles in " + projectDir);
			ok = this.copyDir(oldProjectDir, projectDir, FINISHED_STARTED,
					FINISHED_COPY_FILES);

			if (ok == false) {
				errorMessage = "Could not copy scan files to :" + projectDir;
				this.rollback();
				return;
			}
		}
		finished = FINISHED_COPY_FILES;

		// only store part of the project to xml

		logger.info("Prepareing save for " + projectDir);
		project.setLocation(projectDir);
		try {
			RawDataFile[] dataFiles;
			PeakList[] peakLists;
			Hashtable<Parameter, Hashtable<String, Object>> projectParameters;

			dataFiles = project.getDataFiles();
			peakLists = project.getPeakLists();
			projectParameters = project.getProjectParameters();

			File xmlFile = new File(projectDir, "project.xml");
			XStream xstream = new XStream();

			OutputStreamWriter writer = new OutputStreamWriter(
					new FileOutputStream(xmlFile), "UTF-8");
			ObjectOutputStream out = xstream.createObjectOutputStream(writer);

			logger.info("Saving datafiles in " + projectDir);
			out.writeObject(dataFiles);
			finished = FINISHED_COPY_FILES
					+ (FINISHED_SAVE_DATA - FINISHED_COPY_FILES) / 4 * 1;

			logger.info("Saving peakLists in " + projectDir);
			out.writeObject(peakLists);
			finished = FINISHED_COPY_FILES
					+ (FINISHED_SAVE_DATA - FINISHED_COPY_FILES) / 4 * 2;

			logger.info("Saving parameters in" + projectDir);
			out.writeObject(projectParameters);
			finished = FINISHED_COPY_FILES
					+ (FINISHED_SAVE_DATA - FINISHED_COPY_FILES) / 4 * 3;

			out.close();

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
}
