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

import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Logger;
import java.io.File;
import java.io.IOException;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.PreloadLevel;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.userinterface.mainwindow.ItemSelector;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;

/**
 * This class represents a MZmine project. That includes raw data files,
 * processed raw data files, peak lists, alignment results....
 */
public class MZmineProjectImpl implements MZmineProject {

    private Vector<RawDataFile> projectFiles;
    private Vector<PeakList> projectPeakLists;
    private Hashtable<Parameter, Hashtable<RawDataFile, Object>> projectParametersAndValues;
    private Hashtable<RawDataFile, PeakList> peakLists;
    private Logger logger = Logger.getLogger(this.getClass().getName());
    //filePath to save project
    private File dirPath;
    
    public MZmineProjectImpl(){
    	String dirPath="";
    	try {
    		//create default project dir 
			File tmpFile=File.createTempFile("mzmine", "");
			dirPath=tmpFile.toString();
			tmpFile.delete();
			new File(dirPath).mkdir();
		} catch (IOException e) {
			logger.fine("Could not make temporary file");
		}
		this.setLocation(new File(dirPath));
		peakLists = new Hashtable<RawDataFile, PeakList>();
    }
    
    public void setLocation(File dirPath){
    	this.dirPath=dirPath;
    }   
    
    public File getLocation(){
    	return this.dirPath;
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
        Hashtable<RawDataFile, Object> parameterValues = projectParametersAndValues.get(parameter);
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
        MainWindow mainWindow = (MainWindow) MZmineCore.getDesktop();
        ItemSelector itemSelector = mainWindow.getItemSelector();
        projectFiles.add(newFile);
        itemSelector.addRawData(newFile);
    }

    public void removeFile(RawDataFile file) {
        MainWindow mainWindow = (MainWindow) MZmineCore.getDesktop();
        ItemSelector itemSelector = mainWindow.getItemSelector();
        projectFiles.remove(file);
        itemSelector.removeRawData(file);
    }

    public RawDataFile[] getDataFiles() {
        return projectFiles.toArray(new RawDataFile[0]);
    }

    public void addPeakList(PeakList peakList) {
        MainWindow mainWindow = (MainWindow) MZmineCore.getDesktop();
        ItemSelector itemSelector = mainWindow.getItemSelector();
        projectPeakLists.add(peakList);
        itemSelector.addPeakList(peakList);
    }

    public void removePeakList(PeakList peakList) {
        MainWindow mainWindow = (MainWindow) MZmineCore.getDesktop();
        ItemSelector itemSelector = mainWindow.getItemSelector();
        projectPeakLists.remove(peakList);
        itemSelector.removePeakList(peakList);
    }

    public PeakList[] getPeakLists() {
        return projectPeakLists.toArray(new PeakList[0]);
    }

    public PeakList[] getPeakLists(RawDataFile file) {
        Vector<PeakList> result = new Vector<PeakList>();
        for (PeakList p : projectPeakLists) {
            if (p.hasRawDataFile(file))
                result.add(p);
        }
        return result.toArray(new PeakList[0]);
    }

    /**
     * 
     */
    public void initModule() {

        projectFiles = new Vector<RawDataFile>();
        projectPeakLists = new Vector<PeakList>();
        projectParametersAndValues = new Hashtable<Parameter, Hashtable<RawDataFile, Object>>();

    }
    protected void finalize(){
    	//remove all temporary files
    	//but first close all temporary files.
    	for (RawDataFile file:this.getDataFiles()){
    		if (file.getPreloadLevel().equals(PreloadLevel.PRELOAD_ALL_SCANS)){
    			break;
    		}
    		try {
				file.getScanDataFile().close();
    		} catch (IOException e) {
				logger.warning("Error while closing temporary files.");
			}
    	}
    	for (File file:this.getLocation().listFiles()){
    		file.delete();
    	}
    	this.getLocation().delete();
    }

}