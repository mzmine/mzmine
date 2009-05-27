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
 * This class represents a MZmine project. That includes raw data files,
 * processed raw data files, peak lists, alignment results....
 */
public class MZmineProjectImpl implements MZmineProject {

	private Hashtable<Parameter, Hashtable<RawDataFile, Object>> projectParametersAndValues;

	private Vector<RawDataFile> dataFiles;
	private Vector<PeakList> peakLists;

	private File projectFile;

	public MZmineProjectImpl() {

		this.dataFiles = new Vector<RawDataFile>();
		this.peakLists = new Vector<PeakList>();
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

	public void addFile(RawDataFile newFile) {

		ProjectEvent newEvent;

		synchronized (dataFiles) {
			int newFileIndex = dataFiles.size();
			dataFiles.add(newFile);
			newEvent = new ProjectEvent(ProjectEventType.DATAFILE_ADDED,
					newFile, newFileIndex);
		}

		// We fire the listeners outside of the synchronized block. The reason
		// is that firing the listeners may cause some GUI update and Swing
		// thread may become deadlocked with another thread.
		ProjectManagerImpl.getInstance().fireProjectListeners(newEvent);
	}

	public void removeFile(RawDataFile file) {

		// If the data file is present in any peak list, we must not remove it
		synchronized (peakLists) {
			for (PeakList peakList : peakLists) {
				if (peakList.hasRawDataFile(file)) {
					Desktop desktop = MZmineCore.getDesktop();
					desktop.displayErrorMessage("Cannot remove file \"" + file
							+ "\", because it is present in the peak list \""
							+ peakList + "\"");
					return;
				}
			}
		}

		ProjectEvent newEvent;

		synchronized (dataFiles) {
			int removedFileIndex = dataFiles.indexOf(file);
			dataFiles.remove(file);
			file.close();
			newEvent = new ProjectEvent(ProjectEventType.DATAFILE_REMOVED,
					file, removedFileIndex);

		}

		// We fire the listeners outside of the synchronized block. The reason
		// is that firing the listeners may cause some GUI update and Swing
		// thread may become deadlocked with another thread.
		ProjectManagerImpl.getInstance().fireProjectListeners(newEvent);
	}

	public void moveDataFiles(RawDataFile[] movedFiles, int movePosition) {

		synchronized (dataFiles) {
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
		}

		// We fire the listeners outside of the synchronized block. The reason
		// is that firing the listeners may cause some GUI update and Swing
		// thread may become deadlocked with another thread.
		ProjectEvent newEvent = new ProjectEvent(
				ProjectEventType.DATAFILES_REORDERED);
		ProjectManagerImpl.getInstance().fireProjectListeners(newEvent);

	}

	public RawDataFile[] getDataFiles() {
		return dataFiles.toArray(new RawDataFile[0]);
	}

	public void addPeakList(PeakList peakList) {
		ProjectEvent newEvent;
		synchronized (peakLists) {
			newEvent = new ProjectEvent(ProjectEventType.PEAKLIST_ADDED,
					peakList, peakLists.size());
			peakLists.add(peakList);
		}

		// We fire the listeners outside of the synchronized block. The reason
		// is that firing the listeners may cause some GUI update and Swing
		// thread may become deadlocked with another thread.
		ProjectManagerImpl.getInstance().fireProjectListeners(newEvent);

	}

	public void removePeakList(PeakList peakList) {
		ProjectEvent newEvent;
		synchronized (peakLists) {
			newEvent = new ProjectEvent(ProjectEventType.PEAKLIST_REMOVED,
					peakList, peakLists.indexOf(peakList));
			peakLists.remove(peakList);
		}

		// We fire the listeners outside of the synchronized block. The reason
		// is that firing the listeners may cause some GUI update and Swing
		// thread may become deadlocked with another thread.
		ProjectManagerImpl.getInstance().fireProjectListeners(newEvent);
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

		ProjectEvent newEvent = new ProjectEvent(
				ProjectEventType.PEAKLISTS_REORDERED);
		ProjectManagerImpl.getInstance().fireProjectListeners(newEvent);
	}

	public PeakList[] getPeakLists() {
		synchronized (peakLists) {
			return peakLists.toArray(new PeakList[0]);
		}
	}

	public PeakList[] getPeakLists(RawDataFile file) {
		synchronized (peakLists) {
			Vector<PeakList> result = new Vector<PeakList>();
			for (PeakList peakList : peakLists) {
				if (peakList.hasRawDataFile(file))
					result.add(peakList);
			}
			return result.toArray(new PeakList[0]);
		}
	}

	public File getProjectFile() {
		return projectFile;
	}

	public void setProjectFile(File file) {
		projectFile = file;
		ProjectManagerImpl.getInstance().fireProjectListeners(
				new ProjectEvent(ProjectEventType.PROJECT_NAME_CHANGED));
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