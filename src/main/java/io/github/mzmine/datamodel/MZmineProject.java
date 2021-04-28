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

package io.github.mzmine.datamodel;

import java.io.File;
import java.util.Hashtable;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.parameters.UserParameter;
import javafx.beans.property.ListProperty;
import javafx.collections.ObservableList;
import javax.annotation.Nullable;

/**
 *
 * MZmineProject collects all items user has opened or created during an MZmine session. This
 * includes
 * <ul>
 * <li>Experimental parameters and their values for each RawDataFile. Experimental parameters are
 * available for defining any properties of the sample, for instance concentration or a class label.
 * <li>Opened RawDataFiles
 * <li>FeatureLists of each RawDataFile. A feature list represents results of feature detection on a
 * single RawDataFile or a processed version of a preceding FeatureList.
 * <li>FeatureLists of multiple aligned FeatureLists. An aligned feature list represent results of
 * aligning multiple FeatureLists of individual runs or a processed version of a preceding aligned
 * FeatureList.
 * </ul>
 *
 * @see UserParameter
 * @see RawDataFile
 * @see FeatureList
 *
 */
public interface MZmineProject {

  /**
   * Return the filename of the project file
   */
  File getProjectFile();

  /**
   * Adds a new experimental parameter to the project
   *
   * @param parameter
   */
  void addParameter(UserParameter<?, ?> parameter);

  /**
   * Removes an experimental parameter from the project
   *
   * @param parameter
   */
  void removeParameter(UserParameter<?, ?> parameter);

  /**
   * Returns true if project contains the experimental parameter
   */
  boolean hasParameter(UserParameter<?, ?> parameter);

  /**
   * Returns all experimental parameter of the project
   */
  UserParameter<?, ?>[] getParameters();

  UserParameter<?, ?> getParameterByName(String name);

  /**
   * Sets experimental parameter's value corresponding to a RawDataFile.
   * <p>
   * If the parameter does not exists in the project, it is added to the project. If parameter
   * already has a value corresponding the given file, previous value is replaced.
   *
   */
  void setParameterValue(UserParameter<?, ?> parameter, RawDataFile rawDataFile, Object value);

  /**
   * Returns experimental parameter's value corresponding to a RawDataFile.
   *
   */
  Object getParameterValue(UserParameter<?, ?> parameter, RawDataFile rawDataFile);

  /**
   * Adds a new RawDataFile to the project.
   */
  void addFile(RawDataFile newFile);

  /**
   * Removes a RawDataFile from the project.
   */
  void removeFile(RawDataFile file);

  /**
   * Returns all RawDataFiles of the project.
   *
   */
  RawDataFile[] getDataFiles();

  /**
   * Adds a feature list to the project
   */
  void addFeatureList(FeatureList featureList);

  /**
   * Removes a feature list from the project
   */
  void removeFeatureList(FeatureList featureList);

  /**
   * Returns all feature lists of the project
   */
  ObservableList<FeatureList> getFeatureLists();

  ObservableList<RawDataFile> getRawDataFiles();

  ListProperty<RawDataFile> rawDataFilesProperty();

  ListProperty<FeatureList> featureListsProperty();

  /**
   * Returns all feature lists which contain given data file
   */
  FeatureList[] getFeatureLists(RawDataFile file);

  Hashtable<UserParameter<?, ?>, Hashtable<RawDataFile, Object>> getProjectParametersAndValues();

  void setProjectParametersAndValues(
      Hashtable<UserParameter<?, ?>, Hashtable<RawDataFile, Object>> projectParametersAndValues);

  /**
   * Feature list for name
   * @param name the exact name of the feature list
   * @return the last feature list with that name or null
   */
  @Nullable
  FeatureList getFeatureList(String name);

  // void notifyObjectChanged(Object object, boolean structureChanged);

  // void addProjectListener(MZmineProjectListener newListener);

  // void removeProjectListener(MZmineProjectListener listener);

}
