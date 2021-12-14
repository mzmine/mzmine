/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.project.impl;

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.projectload.CachedIMSRawDataFile;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import java.io.File;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class represents a MZmine project. That includes raw data files, feature lists and
 * parameters.
 */
public class MZmineProjectImpl implements MZmineProject {

  private static final Logger logger = Logger.getLogger(MZmineProjectImpl.class.getName());

  private final ObservableList<RawDataFile> rawDataFiles = //
      FXCollections.synchronizedObservableList(FXCollections.observableArrayList());

  private final ObservableList<FeatureList> featureLists = //
      FXCollections.synchronizedObservableList(FXCollections.observableArrayList());

  private final ObservableList<SpectralLibrary> spectralLibraries = //
      FXCollections.synchronizedObservableList(FXCollections.observableArrayList());

  private Hashtable<UserParameter<?, ?>, Hashtable<RawDataFile, Object>> projectParametersAndValues;
  private File projectFile;

  @Nullable
  private Boolean standalone;

  /*
   * private Collection<MZmineProjectListener> listeners = Collections.synchronizedCollection(new
   * LinkedList<MZmineProjectListener>());
   */
  public MZmineProjectImpl() {
    projectParametersAndValues = new Hashtable<>();
  }

  public static String getUniqueName(String proposedName, List<String> existingNames) {
    int i = 1;

    proposedName = proposedName.trim().replaceAll("\\([\\d]+\\)$", "").trim();

    String unique = proposedName + " (" + i + ")";
    while (existingNames.contains(unique)) {
      i++;
      unique = proposedName + " (" + i + ")";
    }
    return unique;
  }

  @Override
  public Hashtable<UserParameter<?, ?>, Hashtable<RawDataFile, Object>> getProjectParametersAndValues() {
    return projectParametersAndValues;
  }

  @Override
  public void setProjectParametersAndValues(
      Hashtable<UserParameter<?, ?>, Hashtable<RawDataFile, Object>> projectParametersAndValues) {
    this.projectParametersAndValues = projectParametersAndValues;
  }

  @Nullable
  @Override
  public FeatureList getFeatureList(String name) {
    List<FeatureList> lists = getCurrentFeatureLists();
    // find last with name
    for (int i = lists.size() - 1; i >= 0; i--) {
      if (lists.get(i).getName().equals(name)) {
        return lists.get(i);
      }
    }
    return null;
  }

  @Override
  public void addParameter(UserParameter<?, ?> parameter) {
    if (projectParametersAndValues.containsKey(parameter)) {
      return;
    }

    Hashtable<RawDataFile, Object> parameterValues = new Hashtable<>();
    projectParametersAndValues.put(parameter, parameterValues);

  }

  @Override
  public void removeParameter(UserParameter<?, ?> parameter) {
    projectParametersAndValues.remove(parameter);
  }

  @Override
  public UserParameter<?, ?> getParameterByName(String name) {
    for (UserParameter<?, ?> parameter : getParameters()) {
      if (parameter.getName().equals(name)) {
        return parameter;
      }
    }
    return null;
  }

  @Override
  public boolean hasParameter(UserParameter<?, ?> parameter) {
    // matching by name
    UserParameter<?, ?> param = getParameterByName(parameter.getName());
    return param != null;
  }

  @Override
  public UserParameter<?, ?>[] getParameters() {
    return projectParametersAndValues.keySet().toArray(new UserParameter[0]);
  }

  @Override
  public void setParameterValue(UserParameter<?, ?> parameter, RawDataFile rawDataFile,
      Object value) {
    if (!(hasParameter(parameter))) {
      addParameter(parameter);
    }
    Hashtable<RawDataFile, Object> parameterValues = projectParametersAndValues.get(parameter);
    if (value == null) {
      parameterValues.remove(rawDataFile);
    } else {
      parameterValues.put(rawDataFile, value);
    }
  }

  @Override
  public Object getParameterValue(UserParameter<?, ?> parameter, RawDataFile rawDataFile) {
    if (!(hasParameter(parameter))) {
      return null;
    }

    return projectParametersAndValues.get(parameter).get(rawDataFile);
  }

  @Override
  public synchronized void addFile(final RawDataFile newFile) {

    assert newFile != null;

    // avoid duplicate file names and check the actual names of the files of the raw data files
    // since that will be the problem during project save (duplicate zip entries)
    final List<String> names = rawDataFiles.stream().map(RawDataFile::getAbsolutePath)
        .filter(Objects::nonNull).map(File::new).map(File::getName).toList();
    // if there is no path, it is an artificially created file (e.g. by a module) so it does not matter
    final String name =
        newFile.getAbsolutePath() != null ? new File(newFile.getAbsolutePath()).getName() : null;
    if (names.contains(name)) {
      if (!MZmineCore.isHeadLessMode()) {
        MZmineCore.getDesktop().displayErrorMessage("Cannot add raw data file " + name
                                                    + " because a file with the same name already exists in the project. Please copy "
                                                    + "the file and rename it, if you want to import it twice.");
      }
      logger.warning(
          "Cannot add file with an original name that already exists in project. (filename="
          + newFile.getName() + ")");
      return;
    }

    logger.finest("Adding a new file to the project: " + newFile.getName());

    rawDataFiles.add(newFile);

  }

  @Override
  public void removeFile(final RawDataFile file) {

    assert file != null;

    rawDataFiles.remove(file);

    // Close the data file, which also removed the temporary data
    file.close();

  }

  @Override
  public RawDataFile[] getDataFiles() {
    return rawDataFiles.toArray(RawDataFile[]::new);
  }

  @Override
  public void addFeatureList(final FeatureList featureList) {
    if (featureList == null) {
      return;
    }

    synchronized (featureLists) {
      // avoid duplicate file names
      final List<String> names = featureLists.stream().map(FeatureList::getName).toList();
      if (names.contains(featureList.getName())) {
        featureList.setName(getUniqueName(featureList.getName(), names));
      }
      featureLists.add(featureList);
    }

  }

  @Override
  public void removeFeatureList(final FeatureList featureList) {

    assert featureList != null;

    synchronized (featureLists) {
      featureLists.removeAll(featureList);
    }
  }


  @Override
  public void removeFeatureLists(@NotNull List<ModularFeatureList> featureLists) {

    synchronized (this.featureLists) {
      this.featureLists.removeAll(featureLists);
    }
  }

  @Override
  public ModularFeatureList[] getFeatureLists(RawDataFile file) {
    return getCurrentFeatureLists().stream()
        .filter(flist -> flist.hasRawDataFile(file) && flist instanceof ModularFeatureList)
        .toArray(ModularFeatureList[]::new);
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

  /*
   * @Override public void addProjectListener(MZmineProjectListener newListener) {
   * listeners.add(newListener); }
   *
   * @Override public void removeProjectListener(MZmineProjectListener newListener) {
   * listeners.remove(newListener); }
   */

  @Override
  public String toString() {
    if (projectFile == null) {
      return "New project";
    }
    String projectName = projectFile.getName();
    if (projectName.endsWith(".mzmine")) {
      projectName = projectName.substring(0, projectName.length() - 7);
    }
    return projectName;
  }

  @Override
  public @NotNull
  ObservableList<RawDataFile> getObservableRawDataFiles() {
    return rawDataFiles;
  }


  @Override
  public @NotNull
  ObservableList<FeatureList> getObservableFeatureLists() {
    return featureLists;
  }


  @Override
  public @Nullable Boolean isStandalone() {
    return standalone;
  }

  @Override
  public void setStandalone(Boolean standalone) {
    this.standalone = standalone;
  }

  @Override
  public void setProjectLoadImsImportCaching(boolean enabled) {
    MZmineCore.runLater(() -> {
      for (int i = 0; i < getCurrentRawDataFiles().size(); i++) {
        RawDataFile file = rawDataFiles.get(i);
        if (file instanceof IMSRawDataFile imsfile) {
          if (enabled) {
            if (!(file instanceof CachedIMSRawDataFile)) {
              rawDataFiles.set(i, new CachedIMSRawDataFile(imsfile));
            }
          } else {
            if (file instanceof CachedIMSRawDataFile cached) {
              rawDataFiles.set(i, cached.getOriginalFile());
            }
          }
        }
      }
    });
  }

  @Override
  public void addSpectralLibrary(final SpectralLibrary... library) {
    synchronized (spectralLibraries) {
      // remove all with same path
      spectralLibraries.removeIf(
          lib -> Arrays.stream(library).anyMatch(newlib -> lib.getPath().equals(newlib.getPath())));
      spectralLibraries.addAll(library);
    }
  }

  @Override
  public @NotNull
  ObservableList<SpectralLibrary> getObservableSpectralLibraries() {
    return spectralLibraries;
  }

  @Override
  public void removeSpectralLibrary(SpectralLibrary... library) {
    synchronized (spectralLibraries) {
      spectralLibraries.removeAll(library);
    }
  }

}
