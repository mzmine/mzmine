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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.project.impl;

import java.io.File;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.DefaultListModel;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.project.ProjectListener;

/**
 * This class represents a MZmine project. That includes raw data files,
 * processed raw data files, peak lists, alignment results....
 */
public class MZmineProjectImpl implements MZmineProject {

	private Hashtable<Parameter, Hashtable<String, Object>> projectParametersAndValues;
	private transient Vector<ProjectListener> listeners;
	
    private DefaultListModel rawDataList;
	private DefaultListModel peakListsList;

	private File projectFile;

	public MZmineProjectImpl() {
		
        listeners = new Vector<ProjectListener>();
        this.rawDataList = new DefaultListModel();
        this.peakListsList = new DefaultListModel();
        projectParametersAndValues = new Hashtable<Parameter, Hashtable<String, Object>>();
        
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
		return projectParametersAndValues.containsKey(parameter);
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
		file.close();
	}

	public RawDataFile[] getDataFiles() {

		Vector<RawDataFile> dataFiles = new Vector<RawDataFile>();
		for (int i = 0; i < this.rawDataList.size(); i++) {
			dataFiles.add((RawDataFile) this.rawDataList.getElementAt(i));
		}
		return dataFiles.toArray(new RawDataFile[0]);
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

	public DefaultListModel getPeakListsListModel() {
		return this.peakListsList;
	}

	/**
	 * Returns DataFileListModel for GUI
	 */
	public DefaultListModel getRawDataListModel() {
		return this.rawDataList;
	}

    public File getProjectFile() {
        return projectFile;
    }
    
   void setProjectFile(File file) {
        this.projectFile = file;
    }
    
    private Object readResolve() {
        listeners = new Vector<ProjectListener>();
        return this;
    }
}