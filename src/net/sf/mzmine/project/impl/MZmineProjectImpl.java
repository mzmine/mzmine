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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.project.impl;

import java.io.File;
import java.util.Hashtable;
import java.util.Vector;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.project.ProjectEvent;
import net.sf.mzmine.project.ProjectEvent.ProjectEventType;

/**
 * This class represents a MZmine project. That includes raw data files, peak
 * lists and parameters.
 */
public class MZmineProjectImpl implements MZmineProject {

	private Hashtable<Parameter, Hashtable<RawDataFile, Object>> projectParametersAndValues;

	private Vector<RawDataFile> dataFiles;
	private Vector<PeakList> peakLists;

	/**
	 * These arrays are used to avoid locking the dataFiles and peakLists
	 * Vectors every time when project tree is repainted. That would cause dead
	 * locks when a new object is added to the project.
	 */
	private RawDataFile currentDataFiles[];
	private PeakList currentPeakLists[];

	private File projectFile;

	public MZmineProjectImpl() {

		this.dataFiles = new Vector<RawDataFile>();
		this.currentDataFiles = new RawDataFile[0];
		this.peakLists = new Vector<PeakList>();
		this.currentPeakLists = new PeakList[0];
		projectParametersAndValues = new Hashtable<Parameter, Hashtable<RawDataFile, Object>>();

	}

	public void addParameter(Parameter parameter) {
		if (projectParametersAndValues.containsKey(parameter))
			return;

		Hashtable<RawDataFile, Object> parameterValues = new Hashtable<RawDataFile, Object>();
		projectParametersAndValues.put(parameter, parameterValues);

	}

	public void removeParameter(Parameter parameter) {
		projectParametersAndValues.remove(parameter);
	}

	public boolean hasParameter(Parameter parameter) {
		return projectParametersAndValues.containsKey(parameter);
	}

	public Parameter[] getParameters() {
		return projectParametersAndValues.keySet().toArray(new Parameter[0]);
	}

	public void setParameterValue(Parameter parameter, RawDataFile rawDataFile,
			Object value) {
		if (!(hasParameter(parameter)))
			addParameter(parameter);
		Hashtable<RawDataFile, Object> parameterValues = projectParametersAndValues
				.get(parameter);
		parameterValues.put(rawDataFile, value);
	}

	public Object getParameterValue(Parameter parameter, RawDataFile rawDataFile) {
		if (!(hasParameter(parameter)))
			return null;
		Object value = projectParametersAndValues.get(parameter).get(
				rawDataFile);
		if (value == null)
			return parameter.getDefaultValue();
		return value;
	}

	public synchronized void addFile(RawDataFile newFile) {

		int newFileIndex = dataFiles.size();
		dataFiles.add(newFile);
		currentDataFiles = dataFiles.toArray(new RawDataFile[0]);

		if (MZmineCore.getCurrentProject() == this) {
			ProjectEvent newEvent = new ProjectEvent(
					ProjectEventType.DATAFILE_ADDED, newFile, newFileIndex);
			ProjectManagerImpl.getInstance().fireProjectListeners(newEvent);
		}

	}

	public synchronized void removeFile(RawDataFile file) {

		// If the data file is present in any peak list, we must not remove it
		PeakList currentPeakLists[] = getPeakLists();
		for (PeakList peakList : currentPeakLists) {
			if (peakList.hasRawDataFile(file)) {
				Desktop desktop = MZmineCore.getDesktop();
				desktop.displayErrorMessage("Cannot remove file \"" + file
						+ "\", because it is present in the peak list \""
						+ peakList + "\"");
				return;
			}
		}

		int removedFileIndex = dataFiles.indexOf(file);
		dataFiles.remove(file);
		file.close();

		currentDataFiles = dataFiles.toArray(new RawDataFile[0]);

		if (MZmineCore.getCurrentProject() == this) {
			ProjectEvent newEvent = new ProjectEvent(
					ProjectEventType.DATAFILE_REMOVED, file, removedFileIndex);
			ProjectManagerImpl.getInstance().fireProjectListeners(newEvent);
		}

	}

	public synchronized void moveDataFiles(RawDataFile[] movedFiles,
			int movePosition) {

		int currentPosition;

		for (RawDataFile movedFile : movedFiles) {
			currentPosition = dataFiles.indexOf(movedFile);
			if (currentPosition < 0)
				continue;
			dataFiles.remove(currentPosition);
			if (currentPosition < movePosition)
				movePosition--;
			dataFiles.add(movePosition, movedFile);
			movePosition++;
		}

		currentDataFiles = dataFiles.toArray(new RawDataFile[0]);

		if (MZmineCore.getCurrentProject() == this) {
			ProjectEvent newEvent = new ProjectEvent(
					ProjectEventType.DATAFILES_REORDERED);
			ProjectManagerImpl.getInstance().fireProjectListeners(newEvent);
		}

	}

	/**
	 * This method is not synchronized on purpose. Making it synchronized would
	 * cause a dead-lock whenever something is added to the project (thread A
	 * adds a data file, causes TreeEvent, then event dispatching thread wants
	 * to reload the tree, calls getDataFiles, causes deadlock).
	 */
	public RawDataFile[] getDataFiles() {
		return currentDataFiles;
	}

	/**
	 * This method is not synchronized on purpose. Making it synchronized would
	 * cause a dead-lock whenever something is added to the project (thread A
	 * adds a data file, causes TreeEvent, then event dispatching thread wants
	 * to reload the tree, calls getDataFiles, causes deadlock).
	 */
	public PeakList[] getPeakLists() {
		return currentPeakLists;
	}

	public synchronized void addPeakList(PeakList peakList) {
		int peakListsSize = peakLists.size();
		peakLists.add(peakList);
		currentPeakLists = peakLists.toArray(new PeakList[0]);
		if (MZmineCore.getCurrentProject() == this) {
			ProjectEvent newEvent = new ProjectEvent(
					ProjectEventType.PEAKLIST_ADDED, peakList, peakListsSize);
			ProjectManagerImpl.getInstance().fireProjectListeners(newEvent);
		}

	}

	public synchronized void removePeakList(PeakList peakList) {
		int peakListIndex = peakLists.indexOf(peakList);
		peakLists.remove(peakList);
		currentPeakLists = peakLists.toArray(new PeakList[0]);

		if (MZmineCore.getCurrentProject() == this) {
			ProjectEvent newEvent = new ProjectEvent(
					ProjectEventType.PEAKLIST_REMOVED, peakList, peakListIndex);
			ProjectManagerImpl.getInstance().fireProjectListeners(newEvent);
		}

	}

	public synchronized void movePeakLists(PeakList[] movedPeakLists,
			int movePosition) {

		int currentPosition;

		for (PeakList movedPeakList : movedPeakLists) {
			currentPosition = peakLists.indexOf(movedPeakList);
			if (currentPosition < 0)
				continue;
			peakLists.remove(currentPosition);
			if (currentPosition < movePosition)
				movePosition--;
			peakLists.add(movePosition, movedPeakList);
			movePosition++;
		}

		currentPeakLists = peakLists.toArray(new PeakList[0]);

		if (MZmineCore.getCurrentProject() == this) {
			ProjectEvent newEvent = new ProjectEvent(
					ProjectEventType.PEAKLISTS_REORDERED);
			ProjectManagerImpl.getInstance().fireProjectListeners(newEvent);
		}

	}

	public PeakList[] getPeakLists(RawDataFile file) {
		PeakList[] currentPeakLists = getPeakLists();
		Vector<PeakList> result = new Vector<PeakList>();
		for (PeakList peakList : currentPeakLists) {
			if (peakList.hasRawDataFile(file))
				result.add(peakList);
		}
		return result.toArray(new PeakList[0]);

	}

	public File getProjectFile() {
		return projectFile;
	}

	public void setProjectFile(File file) {
		projectFile = file;
		if (MZmineCore.getCurrentProject() == this) {
			ProjectManagerImpl.getInstance().fireProjectListeners(
					new ProjectEvent(ProjectEventType.PROJECT_NAME_CHANGED));
		}
	}

	public void removeProjectFile() {
		projectFile.delete();
	}

	public String toString() {
		if (projectFile == null)
			return "New project";
		String projectName = projectFile.getName();
		if (projectName.endsWith(".mzmine")) {
			projectName = projectName.substring(0, projectName.length() - 7);
		}
		return projectName;
	}

}