/*
 * Copyright 2006 The MZmine Development Team
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

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.io.RawDataFile;
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
    private Vector<PeakList> projectResults;
    //private Vector<Parameter> projectParameters;
    private Hashtable<Parameter, Hashtable<RawDataFile, Object>> projectParametersAndValues;
    private Hashtable<RawDataFile, PeakList> peakLists;

    public void addParameter(Parameter parameter) {
        if (projectParametersAndValues.containsKey(parameter)) return;
        
        Hashtable<RawDataFile, Object> parameterValues = new Hashtable<RawDataFile, Object>();
        projectParametersAndValues.put(parameter, parameterValues);
        
    }
    public void removeParameter(Parameter parameter) {
    	projectParametersAndValues.remove(parameter);
    }
    
    public boolean hasParameter(Parameter parameter) {
    	if (projectParametersAndValues.containsKey(parameter)) return true; else return false; 
    }
    
    public Parameter[] getParameters() {	
    	return projectParametersAndValues.keySet().toArray(new Parameter[0]);
    }
    
    public void setParameterValue(Parameter parameter, RawDataFile rawDataFile, Object value) {
    	if (!(hasParameter(parameter))) addParameter(parameter);
    	Hashtable<RawDataFile, Object> parameterValues = projectParametersAndValues.get(parameter);
    	parameterValues.put(rawDataFile, value);
    }
    
    public Object getParameterValue(Parameter parameter, RawDataFile rawDataFile) {
    	if (!(hasParameter(parameter))) return null;
    	return projectParametersAndValues.get(parameter).get(rawDataFile);
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

    public void updateFile(RawDataFile oldFile, RawDataFile newFile) {
        MainWindow mainWindow = (MainWindow) MZmineCore.getDesktop();
        ItemSelector itemSelector = mainWindow.getItemSelector();
        projectFiles.remove(oldFile);
        peakLists.remove(oldFile);
        projectFiles.add(newFile);
        itemSelector.replaceRawData(oldFile, newFile);
    }

    public RawDataFile[] getDataFiles() {
        return projectFiles.toArray(new RawDataFile[0]);
    }

    public void addAlignedPeakList(PeakList newResult) {
        MainWindow mainWindow = (MainWindow) MZmineCore.getDesktop();
        ItemSelector itemSelector = mainWindow.getItemSelector();
        projectResults.add(newResult);
        itemSelector.addAlignmentResult(newResult);
    }

    public void removeAlignedPeakList(PeakList result) {
        MainWindow mainWindow = (MainWindow) MZmineCore.getDesktop();
        ItemSelector itemSelector = mainWindow.getItemSelector();
        projectResults.remove(result);
        itemSelector.removeAlignedPeakList(result);
    }

    public PeakList[] getAlignedPeakLists() {
        return projectResults.toArray(new PeakList[0]);
    }

    public PeakList getFilePeakList(RawDataFile file) {
        return peakLists.get(file);
    }

    public void setFilePeakList(RawDataFile file, PeakList peakList) {
        peakLists.put(file, peakList);
    }

    /**
     * 
     */
    public void initModule() {

        projectFiles = new Vector<RawDataFile>();
        projectResults = new Vector<PeakList>();
        //projectParameters = new Vector<Parameter>();
        projectParametersAndValues = new Hashtable<Parameter, Hashtable<RawDataFile, Object>>();
        peakLists = new Hashtable<RawDataFile, PeakList>();
        

    }

}