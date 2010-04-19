/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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

import javax.swing.SwingUtilities;

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

	public void addFile(final RawDataFile newFile) {

		// In case we are loading new project, we don't want to fire listeners
		if (MZmineCore.getCurrentProject() != this) {
			dataFiles.add(newFile);
			return;
		}

		// We will perform the actual addition (and listener firing) in the
		// swing thread, because it will cause repainting the project tree.
		Runnable swingThreadCode = new Runnable() {
			public void run() {
				int newFileIndex = dataFiles.size();
				dataFiles.add(newFile);

				ProjectEvent newEvent = new ProjectEvent(
						ProjectEventType.DATAFILE_ADDED, newFile, newFileIndex);
				ProjectManagerImpl.getInstance().fireProjectListeners(newEvent);
			}
		};
		SwingUtilities.invokeLater(swingThreadCode);

	}

	public void removeFile(final RawDataFile file) {

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

		Runnable swingThreadCode = new Runnable() {
			public void run() {
				int removedFileIndex = dataFiles.indexOf(file);
				dataFiles.remove(file);
				file.close();

				ProjectEvent newEvent = new ProjectEvent(
						ProjectEventType.DATAFILE_REMOVED, file,
						removedFileIndex);
				ProjectManagerImpl.getInstance().fireProjectListeners(newEvent);
			}
		};

		SwingUtilities.invokeLater(swingThreadCode);

	}

	public void moveDataFiles(final RawDataFile[] movedFiles,
			final int toPosition) {

		Runnable swingThreadCode = new Runnable() {
			public void run() {
				int currentPosition, movePosition = toPosition;

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

				ProjectEvent newEvent = new ProjectEvent(
						ProjectEventType.DATAFILES_REORDERED);
				ProjectManagerImpl.getInstance().fireProjectListeners(newEvent);
			};
		};

		SwingUtilities.invokeLater(swingThreadCode);

	}

	/**

	 */
	public RawDataFile[] getDataFiles() {
		return dataFiles.toArray(new RawDataFile[0]);

	}

	public PeakList[] getPeakLists() {
		return peakLists.toArray(new PeakList[0]);
	}

	public void addPeakList(final PeakList peakList) {

		// In case we are loading new project, we don't want to fire listeners
		if (MZmineCore.getCurrentProject() != this) {
			peakLists.add(peakList);
			return;
		}

		Runnable swingThreadCode = new Runnable() {

			public void run() {
				int peakListsSize = peakLists.size();
				peakLists.add(peakList);
				ProjectEvent newEvent = new ProjectEvent(
						ProjectEventType.PEAKLIST_ADDED, peakList,
						peakListsSize);
				ProjectManagerImpl.getInstance().fireProjectListeners(newEvent);

			}
		};

		SwingUtilities.invokeLater(swingThreadCode);

	}

	public void removePeakList(final PeakList peakList) {

		Runnable swingThreadCode = new Runnable() {

			public void run() {
				int peakListIndex = peakLists.indexOf(peakList);
				peakLists.remove(peakList);

				ProjectEvent newEvent = new ProjectEvent(
						ProjectEventType.PEAKLIST_REMOVED, peakList,
						peakListIndex);
				ProjectManagerImpl.getInstance().fireProjectListeners(newEvent);
			}
		};

		SwingUtilities.invokeLater(swingThreadCode);

	}

	public void movePeakLists(final PeakList[] movedPeakLists,
			final int toPosition) {

		Runnable swingThreadCode = new Runnable() {
			public void run() {
				int currentPosition, movePosition = toPosition;

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
		};

		SwingUtilities.invokeLater(swingThreadCode);

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