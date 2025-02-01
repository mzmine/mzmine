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

package io.github.mzmine.project.impl;

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.projectload.CachedIMSRawDataFile;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.project.impl.ProjectChangeEvent.Type;
import io.github.mzmine.util.StringUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
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
  private final ObservableList<RawDataFile> rawDataFiles = FXCollections.observableArrayList();
  private final ObservableList<FeatureList> featureLists = FXCollections.observableArrayList();
  private final ObservableList<SpectralLibrary> spectralLibraries = FXCollections.observableArrayList();
  private final List<ProjectChangeListener> listeners = Collections.synchronizedList(
      new ArrayList<>());

  // use read lock to allow unlimited reads while no write is happening
  private final ReadWriteLock rawLock = new ReentrantReadWriteLock();
  private final ReadWriteLock featureLock = new ReentrantReadWriteLock();

  private Hashtable<UserParameter<?, ?>, Hashtable<RawDataFile, Object>> projectParametersAndValues;
  private final MetadataTable projectMetadata;
  private File projectFile;

  @Nullable
  private Boolean standalone;

  public MZmineProjectImpl() {
    projectParametersAndValues = new Hashtable<>();
    projectMetadata = new MetadataTable();
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
  public @NotNull MetadataTable getProjectMetadata() {
    return projectMetadata;
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
  public void addFile(@NotNull final RawDataFile newFile) {
    try {
      rawLock.writeLock().lock();
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
      projectMetadata.addFile(newFile);

      fireDataFilesChangeEvent(List.of(newFile), Type.ADDED);
    } finally {
      rawLock.writeLock().unlock();
    }
  }

  @Override
  public void removeFile(@NotNull final RawDataFile... file) {
    try {
      rawLock.writeLock().lock();

      rawDataFiles.removeAll(file);
      fireDataFilesChangeEvent(List.of(file), Type.REMOVED);

      for (RawDataFile f : file) {
        // Remove the file from the metadata table
        projectMetadata.removeFile(f);

        // Close the data file, which also removed the temporary data
        f.close();
      }
    } finally {
      rawLock.writeLock().unlock();
    }
  }

  @Override
  public RawDataFile[] getDataFiles() {
    try {
      rawLock.readLock().lock();

      return rawDataFiles.toArray(RawDataFile[]::new);
    } finally {
      rawLock.readLock().unlock();
    }

  }

  @Override
  public void addFeatureList(final FeatureList featureList) {
    if (featureList == null) {
      return;
    }

    try {
      featureLock.writeLock().lock();

      // avoid duplicate file names
      final List<String> names = featureLists.stream().map(FeatureList::getName).toList();
      if (names.contains(featureList.getName())) {
        featureList.setName(getUniqueName(featureList.getName(), names));
      }
      featureLists.add(featureList);
      logger.finer(
          "Added feature list with %d rows named: %s".formatted(featureList.getNumberOfRows(),
              featureList.getName()));
      fireFeatureListsChangeEvent(List.of(featureList), Type.ADDED);

    } finally {
      featureLock.writeLock().unlock();
    }
  }

  @Override
  public void removeFeatureList(@NotNull final FeatureList... featureList) {
    try {
      featureLock.writeLock().lock();

      featureLists.removeAll(featureList);
      fireFeatureListsChangeEvent(List.of(featureList), Type.REMOVED);
    } finally {
      featureLock.writeLock().unlock();
    }
  }

  @Override
  public @NotNull List<FeatureList> getCurrentFeatureLists() {
    try {
      featureLock.readLock().lock();
      return List.copyOf(featureLists);
    } finally {
      featureLock.readLock().unlock();
    }
  }

  @Override
  public @NotNull List<RawDataFile> getCurrentRawDataFiles() {
    try {
      rawLock.readLock().lock();
      return List.copyOf(rawDataFiles);
    } finally {
      rawLock.readLock().unlock();
    }
  }

  @Override
  public @Nullable RawDataFile getDataFileByName(@Nullable String name) {
    if (StringUtils.isBlank(name)) {
      return null;
    }
    try {
      rawLock.readLock().lock();
      name = name.trim();
      for (final RawDataFile raw : rawDataFiles) {
        if (name.equalsIgnoreCase(raw.getName()) || name.equalsIgnoreCase(
            FileAndPathUtil.eraseFormat(raw.getName()))) {
          return raw;
        }
      }
      return null;
    } finally {
      rawLock.readLock().unlock();
    }

  }


  @Override
  public void removeFeatureLists(@NotNull List<FeatureList> featureLists) {
    try {
      featureLock.writeLock().lock();

      this.featureLists.removeAll(featureLists);
      fireFeatureListsChangeEvent(List.copyOf(featureLists), Type.REMOVED);
    } finally {
      featureLock.writeLock().unlock();
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

  @Override
  public void addProjectListener(ProjectChangeListener newListener) {
    synchronized (listeners) {
      listeners.add(newListener);
    }
  }

  @Override
  public void removeProjectListener(ProjectChangeListener newListener) {
    synchronized (listeners) {
      listeners.remove(newListener);
    }
  }


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
  public @Nullable Boolean isStandalone() {
    return standalone;
  }

  @Override
  public void setStandalone(Boolean standalone) {
    this.standalone = standalone;
  }

  @Override
  public void setProjectLoadImsImportCaching(boolean enabled) {
    FxThread.runLater(() -> {
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
      fireLibrariesChangeEvent(List.of(library), Type.UPDATED);
    }
  }

  @Override
  public @NotNull List<SpectralLibrary> getCurrentSpectralLibraries() {
    return List.copyOf(spectralLibraries);
  }

  @Override
  public void removeSpectralLibrary(SpectralLibrary... library) {
    synchronized (spectralLibraries) {
      spectralLibraries.removeAll(library);
      fireLibrariesChangeEvent(List.of(library), Type.REMOVED);
    }
  }

  @Override
  public void clearSpectralLibrary() {
    synchronized (spectralLibraries) {
      final List<SpectralLibrary> removed = List.copyOf(spectralLibraries);
      spectralLibraries.clear();
      fireLibrariesChangeEvent(removed, Type.REMOVED);
    }
  }

  @Override
  public void fireLibrariesChangeEvent(List<SpectralLibrary> libraries, Type type) {
    final var event = new ProjectChangeEvent<>(this, libraries, type);
    listeners.forEach(l -> l.librariesChanged(event));
  }

  @Override
  public void fireFeatureListsChangeEvent(List<FeatureList> featureLists, Type type) {
    final var event = new ProjectChangeEvent<>(this, featureLists, type);
    listeners.forEach(l -> l.featureListsChanged(event));
  }

  @Override
  public void fireDataFilesChangeEvent(List<RawDataFile> dataFiles, Type type) {
    final var event = new ProjectChangeEvent<>(this, dataFiles, type);
    listeners.forEach(l -> l.dataFilesChanged(event));
  }

  @Override
  public int getNumberOfDataFiles() {
    try {
      rawLock.readLock().lock();

      return rawDataFiles.size();
    } finally {
      rawLock.readLock().unlock();
    }
  }

  @Override
  public int getNumberOfFeatureLists() {
    try {
      featureLock.readLock().lock();

      return featureLists.size();
    } finally {
      featureLock.readLock().unlock();
    }
  }

  @Override
  public int getNumberOfLibraries() {
    synchronized (spectralLibraries) {
      return spectralLibraries.size();
    }
  }

  @Override
  public String setUniqueFeatureListName(FeatureList featureList, String name) {
    try {
      featureLock.writeLock().lock();

      // need to lock before getting all names
      final List<String> names = featureLists.stream().map(FeatureList::getName).toList();
      // make path safe
      name = FileAndPathUtil.safePathEncode(name);
      // handle duplicates
      name = names.contains(name) ? MZmineProjectImpl.getUniqueName(name, names) : name;

      // set the new name and notify listeners
      featureList.setNameNoChecks(name);
      return name;
    } finally {
      featureLock.writeLock().unlock();
    }
  }

  public @Nullable Path getRelativePath(@Nullable Path path) {
    if (path == null) {
      return null;
    }
    final File projectFile = getProjectFile();

    if (projectFile == null) {
      return null;
    }

    try {
      return projectFile.toPath().relativize(path).normalize();
    } catch (IllegalArgumentException e) {
      logger.warning(
          () -> "Cannot relativize path %s to project file %s. Files may be located on a different drive.".formatted(
              path.toFile().getAbsolutePath(), projectFile.getAbsolutePath()));
      return null;
    }
  }

  @Override
  public @Nullable File resolveRelativePathToFile(@Nullable String path) {
    if (path == null || path.isBlank() || getProjectFile() == null) {
      return null;
    }

    try {
      return projectFile.toPath().resolve(path).normalize().toFile();
    } catch (InvalidPathException e) {
      logger.log(Level.SEVERE, "Cannot resolve file path relative to project.", e);
      return null;
    }
  }
}
