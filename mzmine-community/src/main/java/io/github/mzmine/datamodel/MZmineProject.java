/*
 * Copyright (c) 2004-2025 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.datamodel;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.io.projectload.CachedIMSRawDataFile;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.project.impl.ProjectChangeEvent.Type;
import io.github.mzmine.project.impl.ProjectChangeListener;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import java.io.File;
import java.nio.file.Path;
import java.util.Hashtable;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * MZmineProject collects all item's user has opened or created during an MZmine session. This
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
 */
public interface MZmineProject {

  /**
   * Return the filename of the project file
   */
  File getProjectFile();

  /**
   * Adds a new experimental parameter to the project
   */
  void addParameter(UserParameter<?, ?> parameter);

  /**
   * Removes an experimental parameter from the project
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
   */
  void setParameterValue(UserParameter<?, ?> parameter, RawDataFile rawDataFile, Object value);

  /**
   * Returns experimental parameter's value corresponding to a RawDataFile.
   */
  Object getParameterValue(UserParameter<?, ?> parameter, RawDataFile rawDataFile);

  /**
   * Adds a new RawDataFile to the project.
   */
  void addFile(@NotNull RawDataFile newFile);

  /**
   * Removes a RawDataFile from the project.
   */
  void removeFile(@NotNull RawDataFile... file);

  /**
   * Returns all RawDataFiles of the project.
   */
  RawDataFile[] getDataFiles();

  /**
   * Adds a feature list to the project
   */
  void addFeatureList(FeatureList featureList);

  /**
   * Removes a feature list from the project
   */
  void removeFeatureList(@NotNull FeatureList... featureList);

  /**
   * Unmodifiable copy of current list
   *
   * @return copy of the current feature lists
   */
  @NotNull
  List<FeatureList> getCurrentFeatureLists();

  /**
   * Unmodifiable copy of current list
   *
   * @return copy of current raw data files
   */
  @NotNull
  List<RawDataFile> getCurrentRawDataFiles();

  void addProjectListener(ProjectChangeListener newListener);

  void removeProjectListener(ProjectChangeListener newListener);

  void removeFeatureLists(@NotNull List<FeatureList> featureLists);

  /**
   * Returns all feature lists which contain given data file
   */
  FeatureList[] getFeatureLists(RawDataFile file);

  Hashtable<UserParameter<?, ?>, Hashtable<RawDataFile, Object>> getProjectParametersAndValues();

  void setProjectParametersAndValues(
      Hashtable<UserParameter<?, ?>, Hashtable<RawDataFile, Object>> projectParametersAndValues);

  /**
   * Feature list for name
   *
   * @param name the exact name of the feature list
   * @return the last feature list with that name or null
   */
  @Nullable
  FeatureList getFeatureList(String name);

  @Nullable
  Boolean isStandalone();

  void setStandalone(Boolean standalone);

  /**
   * Enables/disables usage of {@link CachedIMSRawDataFile}s for {@link IMSRawDataFile}s in the
   * project. Cached files are used during feature list import to avoid multiple copies of
   * {@link io.github.mzmine.datamodel.MobilityScan}s, since the main implementation
   * ({@link io.github.mzmine.datamodel.impl.StoredMobilityScan}) is created on demand and passed
   * through data types.
   * <p></p>
   * After the project import, the files have to be replaced to lower ram consumption and allow
   * further processing.
   */
  void setProjectLoadImsImportCaching(boolean enabled);

  /**
   * Add a spectral library that can be reused later
   *
   * @param library new library
   */
  void addSpectralLibrary(final SpectralLibrary... library);

  /**
   * The observable list of spectral preloaded libraries
   *
   * @return current list of preloaded libraries
   */
  @NotNull
  List<SpectralLibrary> getCurrentSpectralLibraries();

  /**
   * Remove preloaded spectral library
   *
   * @param library library to be removed
   */
  void removeSpectralLibrary(SpectralLibrary... library);

  /**
   * Remove all preloaded spectral libraries
   */
  void clearSpectralLibrary();

  int getNumberOfFeatureLists();

  int getNumberOfLibraries();

  /**
   * Finds and sets a unique name for a feature list
   *
   * @param featureList the target feature list thats renamed
   * @param name        the new name candidate
   * @return the unique name that was set
   */
  String setUniqueFeatureListName(FeatureList featureList, String name);

  void fireLibrariesChangeEvent(List<SpectralLibrary> libraries, Type type);

  void fireFeatureListsChangeEvent(List<FeatureList> featureLists, Type type);

  void fireDataFilesChangeEvent(List<RawDataFile> dataFiles, Type type);

  int getNumberOfDataFiles();

  @NotNull
  MetadataTable getProjectMetadata();

  /**
   * find data file by name. Acquires read lock on files for synchronization.
   *
   * @param name name of the file, compared with ignore case
   * @return the RawDataFile or null if the name was null or no such file exists
   */
  @Nullable
  RawDataFile getDataFileByName(@Nullable String name);

  /**
   * @return The relative path of the file/folder with regard to the project file. Null if the path
   * or the project file are null.
   */
  @Nullable
  Path getRelativePath(@Nullable Path path);

  /**
   * @return A
   */
  @Nullable
  File resolveRelativePathToFile(@Nullable String path);
}
