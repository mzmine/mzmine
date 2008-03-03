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
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.project.ProjectListener;

/**
 * This class represents a MZmine project. That includes raw data files,
 * processed raw data files, peak lists, alignment results....
 */
public class MZmineProjectImpl implements MZmineProject {

	private transient Hashtable<Parameter, Hashtable<String, Object>> projectParametersAndValues;
	private transient Vector<ProjectListener> listeners;
	private transient DefaultListModel rawDataList;
	private transient DefaultListModel peakListsList;

	private transient Logger logger;
	// filePath to save project
	private File projectDir;
	private boolean isTemporal = true;

	public MZmineProjectImpl(File projectDir) {
		this.projectDir = projectDir;
		this.initModule();
	}

	/**
	 * 
	 */
	public void initModule() {
		listeners = new Vector<ProjectListener>();
		this.rawDataList = new DefaultListModel();
		this.peakListsList = new DefaultListModel();
		projectParametersAndValues = new Hashtable<Parameter, Hashtable<String, Object>>();
		logger = Logger.getLogger(this.getClass().getName());
		isTemporal = true;
	}

	public boolean getIsTemporal() {
		return this.isTemporal;
	}

	public void setIsTemporal(boolean flag) {
		this.isTemporal = flag;
	}

	public void setLocation(File dirPath) {
		this.projectDir = dirPath;
	}

	public File getLocation() {
		return this.projectDir;
	}

	public void addParameter(Parameter parameter) {
		if (projectParametersAndValues.containsKey(parameter))
			return;

		Hashtable<String, Object> parameterValues = new Hashtable<String, Object>();
		projectParametersAndValues.put(parameter, parameterValues);

	}

	public void removeParameter(Parameter parameter) {
		projectParametersAndValues.remove(parameter);
	}

	public boolean hasParameter(Parameter parameter) {
		if (projectParametersAndValues.containsKey(parameter))
			return true;
		else
			return false;
	}

	public Parameter[] getParameters() {
		return projectParametersAndValues.keySet().toArray(new Parameter[0]);
	}

	public void setParameterValue(Parameter parameter, RawDataFile rawDataFile,
			Object value) {
		if (!(hasParameter(parameter)))
			addParameter(parameter);
		Hashtable<String, Object> parameterValues = projectParametersAndValues
				.get(parameter);
		parameterValues.put(rawDataFile.getFileName(), value);
	}

	public Object getParameterValue(Parameter parameter, RawDataFile rawDataFile) {
		if (!(hasParameter(parameter)))
			return null;
		Object value = projectParametersAndValues.get(parameter).get(
				rawDataFile.getFileName());
		if (value == null)
			return parameter.getDefaultValue();
		return value;
	}

	public void addFile(RawDataFile newFile) {
		this.rawDataList.addElement(newFile);
	}

	public void removeFile(RawDataFile file) {
		this.rawDataList.removeElement(file);
	}

	public RawDataFile[] getDataFiles() {

		Vector<RawDataFile> dataFiles = new Vector<RawDataFile>();
		for (int i = 0; i < this.rawDataList.size(); i++) {
			dataFiles.add((RawDataFile) this.rawDataList.getElementAt(i));
		}
		return dataFiles.toArray(new RawDataFile[0]);
	}

	public RawDataFile getDataFile(String fileName) {
		RawDataFile file;
		for (int i = 0; i < this.rawDataList.size(); i++) {
			file = (RawDataFile) this.rawDataList.getElementAt(i);
			if (file.getFileName().equals(fileName)) {
				return file;
			}
		}
		return null;
	}

	public void addPeakList(PeakList peakList) {
		this.peakListsList.addElement(peakList);
	}

	public void removePeakList(PeakList peakList) {
		this.peakListsList.removeElement(peakList);
	}

	public PeakList[] getPeakLists() {
		Vector<PeakList> peakLists = new Vector<PeakList>();
		for (int i = 0; i < this.peakListsList.size(); i++) {
			peakLists.add((PeakList) this.peakListsList.getElementAt(i));
		}
		return peakLists.toArray(new PeakList[0]);
	}

	public PeakList[] getPeakLists(RawDataFile file) {
		Vector<PeakList> result = new Vector<PeakList>();
		SimplePeakList peakList;
		for (int i = 0; i < this.peakListsList.size(); i++) {
			peakList = (SimplePeakList) this.peakListsList.elementAt(i);
			if (peakList.hasRawDataFile(file))
				result.add(peakList);
		}
		return result.toArray(new PeakList[0]);
	}

	public void addProjectListener(ProjectListener listener) {
		listeners.add(listener);
		listener.projectModified(ProjectListener.ProjectEvent.PROJECT_CHANGED,
				this);
	}

	public void removeProjectListener(ProjectListener listener) {
		listeners.remove(listener);
	}

	//
	public Vector<ProjectListener> getProjectListeners() {
		return listeners;
	}

	protected Hashtable<Parameter, Hashtable<String, Object>> getProjectParameters() {
		return (Hashtable<Parameter, Hashtable<String, Object>>) projectParametersAndValues;
	}

	protected void setProjectParameters(
			Hashtable<Parameter, Hashtable<String, Object>> projectParameters) {
		projectParametersAndValues = projectParameters;
	}

	public DefaultListModel getPeakListsListModel() {
		return this.peakListsList;
	};

	/**
	 * Returns DataFileListModel for GUI
	 */
	public DefaultListModel getRawDataListModel() {
		return this.rawDataList;
	};

	private Object readResolve() {
		listeners = new Vector<ProjectListener>();
		this.peakListsList = new DefaultListModel();
		this.rawDataList = new DefaultListModel();
		projectParametersAndValues = new Hashtable<Parameter, Hashtable<String, Object>>();
		logger = Logger.getLogger(this.getClass().getName());
		return this;
	}

	public void setPeakListsListModel(DefaultListModel listModel) {
		this.peakListsList = listModel;

	}

	public void setRawDataListModel(DefaultListModel listModel) {
		this.rawDataList = listModel;

	}
}