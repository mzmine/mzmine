/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_all;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.visualization.projectmetadata.color.ColorByMetadataParameters;
import io.github.mzmine.modules.visualization.projectmetadata.color.ColorByMetadataTask;
import io.github.mzmine.modules.visualization.projectmetadata.extract.SampleMetadataExtractionParameters;
import io.github.mzmine.modules.visualization.projectmetadata.io.ProjectMetadataImportParameters;
import io.github.mzmine.modules.visualization.projectmetadata.io.ProjectMetadataImportTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.threadpools.ThreadPoolTask;
import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AllSpectralDataImportMainTask extends AbstractTask {

  private final ThreadPoolTask mainImportTask;
  private final MZmineProject project;
  private final List<? extends Task> importTasks;
  private final File metadataFile;
  private final ParameterSet parameters;
  // embedded extraction parameters if the option is selected, otherwise null
  private final ParameterSet extractMetadataParameters;
  private final boolean sortAndRecolor;
  private final Set<RawDataFile> baselineRawDataFiles;
  private final Set<RawDataFile> reusedRawDataFiles;
  private final int requestedRawDataFileCount;
  private final List<? extends Task> rawDataImportTasks;
  private volatile @Nullable Task currentFollowupTask;

  public AllSpectralDataImportMainTask(final @NotNull MZmineProject project,
      final @NotNull List<? extends Task> tasks,
      final @NotNull List<? extends Task> rawDataImportTasks,
      final @NotNull ParameterSet parameters) {
    super(Instant.now(), "Main data import task");
    this.project = project;
    importTasks = List.copyOf(tasks);
    mainImportTask = ThreadPoolTask.createDefaultTaskManagerPool("Importing data", tasks);
    metadataFile = parameters.getEmbeddedParameterValueIfSelectedOrElse(
        AllSpectralDataImportParameters.metadataFile, null);
    extractMetadataParameters = parameters.getEmbeddedParametersIfSelectedOrElse(
        AllSpectralDataImportParameters.extractMetadata, null);
    sortAndRecolor = parameters.getValue(AllSpectralDataImportParameters.sortAndRecolor);
    this.parameters = parameters;
    baselineRawDataFiles = Set.copyOf(project.getCurrentRawDataFiles());
    reusedRawDataFiles = Set.copyOf(AllSpectralDataImportParameters.getLoadedRawDataFiles(project,
        parameters));
    requestedRawDataFileCount = (int) Arrays.stream(
            parameters.getValue(AllSpectralDataImportParameters.fileNames))
        .filter(Objects::nonNull).count();
    this.rawDataImportTasks = List.copyOf(rawDataImportTasks);
  }


  @Override
  public String getTaskDescription() {
    return mainImportTask.getTaskDescription();
  }

  @Override
  public double getFinishedPercentage() {
    return mainImportTask.getFinishedPercentage();
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // do data import and library import directly and import metadata after all is completed.
    // this ensures that data files are already loaded
    mainImportTask.run();
    if (mainImportTask.isCanceled()) {
      setStatus(mainImportTask.getStatus());
      setErrorMessage(mainImportTask.getErrorMessage());
      return;
    }

    if (metadataFile != null) {
      // load metadata after data files
      Task metaTask = loadMetadata();
      if (metaTask.isCanceled()) {
        setStatus(metaTask.getStatus());
        setErrorMessage(metaTask.getErrorMessage());
        return;
      }
    }

    // extract metadata from file names after the metadata file was imported
    if (extractMetadataParameters != null) {
      Task extractTask = extractMetadata();
      if (extractTask.isCanceled()) {
        setStatus(extractTask.getStatus());
        setErrorMessage(extractTask.getErrorMessage());
        return;
      }
    }

    // recolor by default without any metadata
    if (sortAndRecolor) {
      ColorByMetadataTask colorTask = recolorBlanksAndQcs();
      if (colorTask.isCanceled()) {
        setStatus(colorTask.getStatus());
        setErrorMessage(colorTask.getErrorMessage());
        return;
      }
    }

    setStatus(TaskStatus.FINISHED);
  }

  private ColorByMetadataTask recolorBlanksAndQcs() {
    List<RawDataFile> loaded = AllSpectralDataImportParameters.getLoadedRawDataFiles(
        project, parameters);

    ColorByMetadataTask task = new ColorByMetadataTask(moduleCallDate,
        ColorByMetadataParameters.createDefault(loaded), AllSpectralDataImportModule.class);
    runFollowupTask(task);
    return task;
  }

  private Task loadMetadata() {
    var metaParams = ProjectMetadataImportParameters.create(metadataFile, false, false);
    var metadataTask = new ProjectMetadataImportTask(metaParams, moduleCallDate);
    runFollowupTask(metadataTask);
    return metadataTask;
  }

  private Task extractMetadata() {
    final List<RawDataFile> loaded = AllSpectralDataImportParameters.getLoadedRawDataFiles(
        project, parameters);
    final RawDataFile[] files = loaded.toArray(RawDataFile[]::new);
    final Task extractTask = SampleMetadataExtractionParameters.createTaskWithDataFiles(
        extractMetadataParameters, moduleCallDate, AllSpectralDataImportModule.class, files);
    runFollowupTask(extractTask);
    return extractTask;
  }

  /** Cancels both the pool and whichever native post-import task is currently running. */
  @Override
  public void cancel() {
    super.cancel();
    mainImportTask.cancel();
    importTasks.forEach(Task::cancel);
    final Task followup = currentFollowupTask;
    if (followup != null) {
      followup.cancel();
    }
  }

  /** Actual raw files added while this native import task was running. */
  public @NotNull List<RawDataFile> getNewlyImportedRawDataFiles() {
    return AllSpectralDataImportParameters.getLoadedRawDataFiles(project, parameters).stream()
        .filter(raw -> !baselineRawDataFiles.contains(raw)).toList();
  }

  /** Bounded measured import counts for the prepared native import. */
  public @NotNull ImportOutcome getImportOutcome() {
    final int selected = requestedRawDataFileCount;
    final int reused = reusedRawDataFiles.size();
    final int imported = getNewlyImportedRawDataFiles().size();
    final int failed = (int) rawDataImportTasks.stream()
        .filter(task -> task.getStatus() == TaskStatus.ERROR).count();
    return new ImportOutcome(selected, reused, imported, failed);
  }

  private void runFollowupTask(final @NotNull Task task) {
    currentFollowupTask = task;
    try {
      task.run();
    } finally {
      currentFollowupTask = null;
    }
  }

  public record ImportOutcome(int selectedRawFileCount, int reusedRawFileCount,
                              int importedRawFileCount,
                              int failedRawFileCount) {
  }

  @Override
  public TaskPriority getTaskPriority() {
    return TaskPriority.HIGH;
  }
}
