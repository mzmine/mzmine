/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.project.impl;

import java.io.File;
import java.util.Hashtable;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MobilogramList;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.parameters.UserParameter;
import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * This class represents a MZmine project. That includes raw data files, feature lists and
 * parameters.
 */
public class MZmineProjectImpl implements MZmineProject {

  private Hashtable<UserParameter<?, ?>, Hashtable<RawDataFile, Object>> projectParametersAndValues;

  private final SimpleListProperty<RawDataFile> rawDataFilesProperty = //
      new SimpleListProperty<>(//
          FXCollections.synchronizedObservableList(//
              FXCollections.observableArrayList()//
          ));


  private final SimpleListProperty<PeakList> featureListsProperty = //
      new SimpleListProperty<>(//
          FXCollections.synchronizedObservableList(//
              FXCollections.observableArrayList()//
          ));

  @Override
  public Hashtable<UserParameter<?, ?>, Hashtable<RawDataFile, Object>> getProjectParametersAndValues() {
    return projectParametersAndValues;
  }

  @Override
  public void setProjectParametersAndValues(Hashtable<UserParameter<?, ?>, Hashtable<RawDataFile, Object>> projectParametersAndValues) {
    this.projectParametersAndValues = projectParametersAndValues;
  }

  @Override
  public MobilogramList[] getMobilogramLists() {
    return new MobilogramList[0];
  }

  @Override
  public MobilogramList[] getMobilogramLists(RawDataFile file) {
    return new MobilogramList[0];
  }

  private File projectFile;

  /*
   * private Collection<MZmineProjectListener> listeners = Collections.synchronizedCollection(new
   * LinkedList<MZmineProjectListener>());
   */
  public MZmineProjectImpl() {

    projectParametersAndValues =
        new Hashtable<UserParameter<?, ?>, Hashtable<RawDataFile, Object>>();

  }

  @Override
  public void addParameter(UserParameter<?, ?> parameter) {
    if (projectParametersAndValues.containsKey(parameter))
      return;

    Hashtable<RawDataFile, Object> parameterValues = new Hashtable<RawDataFile, Object>();
    projectParametersAndValues.put(parameter, parameterValues);

  }

  @Override
  public void removeParameter(UserParameter<?, ?> parameter) {
    projectParametersAndValues.remove(parameter);
  }

  @Override
  public UserParameter<?,?> getParameterByName(String name){
    for(UserParameter<?,?> parameter:getParameters()){
      if(parameter.getName().equals(name)){
        return parameter;
      }
    }
    return null;
  }

  @Override
  public boolean hasParameter(UserParameter<?, ?> parameter) {
    //matching by name
    UserParameter<?,?> param =  getParameterByName(parameter.getName());
    if(param==null){
      return false;
    }
    return true;
  }

  @Override
  public UserParameter<?, ?>[] getParameters() {
    return projectParametersAndValues.keySet().toArray(new UserParameter[0]);
  }

  @Override
  public void setParameterValue(UserParameter<?, ?> parameter, RawDataFile rawDataFile,
      Object value) {
    if (!(hasParameter(parameter)))
      addParameter(parameter);
    Hashtable<RawDataFile, Object> parameterValues = projectParametersAndValues.get(parameter);
    if (value == null)
      parameterValues.remove(rawDataFile);
    else
      parameterValues.put(rawDataFile, value);
  }

  @Override
  public Object getParameterValue(UserParameter<?, ?> parameter, RawDataFile rawDataFile) {
    if (!(hasParameter(parameter)))
      return null;
    Object value = projectParametersAndValues.get(parameter).get(rawDataFile);

    return value;
  }

  @Override
  public void addFile(final RawDataFile newFile) {

    assert newFile != null;

    Platform.runLater(() -> {
      rawDataFilesProperty.get().add(newFile);
    });

  }

  @Override
  public void removeFile(final RawDataFile file) {

    assert file != null;

    Platform.runLater(() -> {
      rawDataFilesProperty.get().remove(file);
    });

    // Close the data file, which also removed the temporary data
    file.close();

  }

  @Override
  public RawDataFile[] getDataFiles() {
    return rawDataFilesProperty.get().toArray(new RawDataFile[0]);
  }

  @Override
  public PeakList[] getPeakLists() {
    return featureListsProperty.get().toArray(new PeakList[0]);
  }

  @Override
  public void addPeakList(final PeakList peakList) {

    assert peakList != null;
    Platform.runLater(() -> {
      featureListsProperty.get().add(peakList);
    });

  }

  @Override
  public void removePeakList(final PeakList peakList) {

    assert peakList != null;

    Platform.runLater(() -> {
      featureListsProperty.get().remove(peakList);
    });
  }

  @Override
  public PeakList[] getPeakLists(RawDataFile file) {
    PeakList[] currentPeakLists = getPeakLists();
    Vector<PeakList> result = new Vector<PeakList>();
    for (PeakList peakList : currentPeakLists) {
      if (peakList.hasRawDataFile(file))
        result.add(peakList);
    }
    return result.toArray(new PeakList[0]);

  }

  @Override
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

  @Override
  public String toString() {
    if (projectFile == null)
      return "New project";
    String projectName = projectFile.getName();
    if (projectName.endsWith(".mzmine")) {
      projectName = projectName.substring(0, projectName.length() - 7);
    }
    return projectName;
  }

  /*
   * @Override public void addProjectListener(MZmineProjectListener newListener) {
   * listeners.add(newListener); }
   * 
   * @Override public void removeProjectListener(MZmineProjectListener newListener) {
   * listeners.remove(newListener); }
   */

  @Override
  public ObservableList<RawDataFile> getRawDataFiles() {
    return rawDataFilesProperty.get();
  }

  @Override
  public ListProperty<RawDataFile> rawDataFilesProperty() {
    return rawDataFilesProperty;
  }

  @Override
  public ObservableList<PeakList> getFeatureLists() {
    return featureListsProperty.get();
  }

  @Override
  public ListProperty<PeakList> featureListsProperty() {
    return featureListsProperty;
  }

}
