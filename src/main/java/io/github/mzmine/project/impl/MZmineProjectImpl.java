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
import io.github.mzmine.util.javafx.FxThreadUtil;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import java.io.File;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.logging.Logger;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.Nullable;

/**
 * This class represents a MZmine project. That includes raw data files, feature lists and
 * parameters.
 */
public class MZmineProjectImpl implements MZmineProject {

  private static final Logger logger = Logger.getLogger(MZmineProjectImpl.class.getName());

  private final SimpleListProperty<RawDataFile> rawDataFilesProperty = //
      new SimpleListProperty<>(//
          FXCollections.synchronizedObservableList(//
              FXCollections.observableArrayList()//
          ));

  private final SimpleListProperty<FeatureList> featureListsProperty = //
      new SimpleListProperty<>(//
          FXCollections.synchronizedObservableList(//
              FXCollections.observableArrayList()//
          ));

  private final SimpleListProperty<SpectralLibrary> spectralLibrariesProperty = //
      new SimpleListProperty<>(FXCollections.synchronizedObservableList(
          FXCollections.observableArrayList()
      ));

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
    ObservableList<FeatureList> lists = getFeatureLists();
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
    if (param == null) {
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
    Object value = projectParametersAndValues.get(parameter).get(rawDataFile);

    return value;
  }

  @Override
  public synchronized void addFile(final RawDataFile newFile) {

    assert newFile != null;

    // avoid duplicate file names and check the actual names of the files of the raw data files
    // since that will be the problem during project save (duplicate zip entries)
    final List<String> names = rawDataFilesProperty.get().stream().map(RawDataFile::getAbsolutePath)
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

    FxThreadUtil.runOnFxThreadAndWait(() -> {
      rawDataFilesProperty.get().add(newFile);
    });

  }

  @Override
  public void removeFile(final RawDataFile file) {

    assert file != null;

    FxThreadUtil.runOnFxThreadAndWait(() -> {
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
  public void addFeatureList(final FeatureList featureList) {
    if (featureList == null) {
      return;
    }

    synchronized (featureListsProperty.get()) {
      // avoid duplicate file names
      final List<String> names = featureListsProperty.get().stream().map(f -> f.getName()).toList();
      if (names.contains(featureList.getName())) {
        featureList.setName(getUniqueName(featureList.getName(), names));
      }
    }

    FxThreadUtil.runOnFxThreadAndWait(() -> {
      featureListsProperty.get().add(featureList);
    });
  }

  @Override
  public void removeFeatureList(final FeatureList... featureList) {

    assert featureList != null;

    synchronized (featureListsProperty) {
      FxThreadUtil.runOnFxThreadAndWait(() -> {
        featureListsProperty.get().removeAll(featureList);
      });
    }
  }

  @Override
  public void removeFeatureLists(List<ModularFeatureList> featureLists) {
    assert featureLists != null;

    synchronized (featureListsProperty) {
      FxThreadUtil.runOnFxThreadAndWait(() -> {
        featureListsProperty.get().removeAll(featureLists);
      });
    }
  }

  @Override
  public ModularFeatureList[] getFeatureLists(RawDataFile file) {
    FeatureList[] currentFeatureLists = getFeatureLists().toArray(FeatureList[]::new);
    Vector<ModularFeatureList> result = new Vector<ModularFeatureList>();
    for (FeatureList featureList : currentFeatureLists) {
      if (featureList.hasRawDataFile(file)) {
        result.add((ModularFeatureList) featureList);
      }
    }
    return result.toArray(new ModularFeatureList[0]);

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
  public ObservableList<RawDataFile> getRawDataFiles() {
    return FXCollections.unmodifiableObservableList(rawDataFilesProperty.get());
  }

  @Override
  public ListProperty<RawDataFile> rawDataFilesProperty() {
    return rawDataFilesProperty;
  }

  @Override
  public ObservableList<FeatureList> getFeatureLists() {
    return FXCollections.unmodifiableObservableList(featureListsProperty.get());
  }

  @Override
  public ListProperty<FeatureList> featureListsProperty() {
    return featureListsProperty;
  }

  @Override
  public ListProperty<SpectralLibrary> spectralLibrariesProperty() {
    return spectralLibrariesProperty;
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
      for (int i = 0; i < getRawDataFiles().size(); i++) {
        RawDataFile file = rawDataFilesProperty.get(i);
        if (file instanceof IMSRawDataFile imsfile) {
          if (enabled) {
            if (!(file instanceof CachedIMSRawDataFile)) {
              rawDataFilesProperty.set(i, new CachedIMSRawDataFile(imsfile));
            }
          } else {
            if (file instanceof CachedIMSRawDataFile cached) {
              rawDataFilesProperty.set(i, cached.getOriginalFile());
            }
          }
        }
      }
    });
  }

  @Override
  public void addSpectralLibrary(final SpectralLibrary... library) {
    synchronized (spectralLibrariesProperty) {
      FxThreadUtil.runOnFxThreadAndWait(() -> {
        // remove all with same path
        spectralLibrariesProperty.removeIf(lib -> Arrays.stream(library)
            .anyMatch(newlib -> lib.getPath().equals(newlib.getPath())));
        spectralLibrariesProperty.addAll(library);
      });
    }
  }

  @Override
  public ObservableList<SpectralLibrary> getSpectralLibraries() {
    return FXCollections.unmodifiableObservableList(spectralLibrariesProperty.get());
  }

  @Override
  public void removeSpectralLibrary(SpectralLibrary... library) {
    synchronized (spectralLibrariesProperty) {
      FxThreadUtil.runOnFxThreadAndWait(() -> {
        spectralLibrariesProperty.removeAll(library);
      });
    }
  }


}
