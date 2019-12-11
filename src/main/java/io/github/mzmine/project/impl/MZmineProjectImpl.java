/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.project.impl;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Vector;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MZmineProjectListener;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.MZmineGUI;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.UserParameter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * This class represents a MZmine project. That includes raw data files, feature
 * lists and parameters.
 */
public class MZmineProjectImpl implements MZmineProject {

    private Hashtable<UserParameter<?, ?>, Hashtable<RawDataFile, Object>> projectParametersAndValues;

    private final ObservableList<RawDataFile> rawDataFiles = FXCollections
            .synchronizedObservableList(FXCollections.observableArrayList());

    private final ObservableList<PeakList> featureLists = FXCollections
            .synchronizedObservableList(FXCollections.observableArrayList());

    private File projectFile;

    private Collection<MZmineProjectListener> listeners = Collections
            .synchronizedCollection(new LinkedList<MZmineProjectListener>());

    public MZmineProjectImpl() {

        projectParametersAndValues = new Hashtable<UserParameter<?, ?>, Hashtable<RawDataFile, Object>>();

    }

    public void activateProject() {

        // If running without GUI, just return
        if (MZmineCore.getDesktop() == null)
            return;

        MZmineGUI.activateProject(this);

    }

    public void addParameter(UserParameter<?, ?> parameter) {
        if (projectParametersAndValues.containsKey(parameter))
            return;

        Hashtable<RawDataFile, Object> parameterValues = new Hashtable<RawDataFile, Object>();
        projectParametersAndValues.put(parameter, parameterValues);

    }

    public void removeParameter(UserParameter<?, ?> parameter) {
        projectParametersAndValues.remove(parameter);
    }

    public boolean hasParameter(UserParameter<?, ?> parameter) {
        return projectParametersAndValues.containsKey(parameter);
    }

    public UserParameter<?, ?>[] getParameters() {
        return projectParametersAndValues.keySet()
                .toArray(new UserParameter[0]);
    }

    public void setParameterValue(UserParameter<?, ?> parameter,
            RawDataFile rawDataFile, Object value) {
        if (!(hasParameter(parameter)))
            addParameter(parameter);
        Hashtable<RawDataFile, Object> parameterValues = projectParametersAndValues
                .get(parameter);
        if (value == null)
            parameterValues.remove(rawDataFile);
        else
            parameterValues.put(rawDataFile, value);
    }

    public Object getParameterValue(UserParameter<?, ?> parameter,
            RawDataFile rawDataFile) {
        if (!(hasParameter(parameter)))
            return null;
        Object value = projectParametersAndValues.get(parameter)
                .get(rawDataFile);

        return value;
    }

    public void addFile(final RawDataFile newFile) {

        assert newFile != null;

        Platform.runLater(() -> {
            rawDataFiles.add(newFile);
        });

    }

    public void removeFile(final RawDataFile file) {

        assert file != null;

        Platform.runLater(() -> {
            rawDataFiles.remove(file);
        });

        // Close the data file, which also removed the temporary data
        file.close();

    }

    public RawDataFile[] getDataFiles() {
        return rawDataFiles.toArray(new RawDataFile[0]);
    }

    public PeakList[] getPeakLists() {
        return featureLists.toArray(new PeakList[0]);
    }

    public void addPeakList(final PeakList peakList) {

        assert peakList != null;
        Platform.runLater(() -> {
            featureLists.add(peakList);
        });

    }

    public void removePeakList(final PeakList peakList) {

        assert peakList != null;

        Platform.runLater(() -> {
            featureLists.remove(peakList);
        });
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
        // Notify the tree model to update the name of the project
        // treeModel.notifyObjectChanged(this, false);
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

    @Override
    public void notifyObjectChanged(Object object, boolean structureChanged) {
        // peakListTreeModel.notifyObjectChanged(object, structureChanged);
        // awDataTreeModel.notifyObjectChanged(object, structureChanged);
    }

    @Override
    public void addProjectListener(MZmineProjectListener newListener) {
        listeners.add(newListener);
    }

    @Override
    public void removeProjectListener(MZmineProjectListener newListener) {
        listeners.remove(newListener);
    }

    @Override
    public ObservableList<RawDataFile> rawDataFiles() {
        return rawDataFiles;
    }

    @Override
    public ObservableList<PeakList> featureLists() {
        return featureLists;
    }

}
