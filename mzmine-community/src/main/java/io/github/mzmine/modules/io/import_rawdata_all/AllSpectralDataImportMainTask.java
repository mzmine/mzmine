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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.impl.WrappedTask;
import io.github.mzmine.modules.visualization.projectmetadata.color.ColorByMetadataParameters;
import io.github.mzmine.modules.visualization.projectmetadata.color.ColorByMetadataTask;
import io.github.mzmine.modules.visualization.projectmetadata.extract.SampleMetadataExtractionParameters;
import io.github.mzmine.modules.visualization.projectmetadata.io.ProjectMetadataImportParameters;
import io.github.mzmine.modules.visualization.projectmetadata.io.ProjectMetadataImportTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.threadpools.ThreadPoolTask;
import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AllSpectralDataImportMainTask extends AbstractTask {

  private final ThreadPoolTask mainImportTask;
  private final List<? extends Task> importTasks;
  private volatile List<WrappedTask> fixedImportHandles = List.of();
  private final File metadataFile;
  private final ParameterSet parameters;
  // embedded extraction parameters if the option is selected, otherwise null
  private final ParameterSet extractMetadataParameters;
  private final boolean sortAndRecolor;
  private final @Nullable MZmineProject fixedProject;
  private final boolean cancelOnCurrentProjectChange;
  private final Set<RawDataFile> baselineRawDataFiles;
  private final List<? extends Task> rawDataImportTasks;
  private volatile @Nullable Task currentFollowupTask;

  public AllSpectralDataImportMainTask(final List<? extends Task> tasks,
      final @NotNull ParameterSet parameters) {
    this(tasks, List.of(), parameters, null, false);
  }

  /** Creates an import main task that never follows a changed current project. */
  static @NotNull AllSpectralDataImportMainTask forFixedProject(
      final @NotNull List<? extends Task> tasks, final @NotNull List<? extends Task> rawDataImportTasks,
      final @NotNull ParameterSet parameters, final @NotNull MZmineProject fixedProject) {
    return new AllSpectralDataImportMainTask(tasks, rawDataImportTasks, parameters, fixedProject,
        true);
  }

  /** Creates an import task bound to a private project without consulting the global project. */
  static @NotNull AllSpectralDataImportMainTask forIsolatedProject(
      final @NotNull List<? extends Task> tasks, final @NotNull List<? extends Task> rawDataImportTasks,
      final @NotNull ParameterSet parameters, final @NotNull MZmineProject project) {
    return new AllSpectralDataImportMainTask(tasks, rawDataImportTasks, parameters, project, false);
  }

  private AllSpectralDataImportMainTask(final @NotNull List<? extends Task> tasks,
      final @NotNull List<? extends Task> rawDataImportTasks, final @NotNull ParameterSet parameters,
      @Nullable final MZmineProject fixedProject, final boolean cancelOnCurrentProjectChange) {
    super(Instant.now(), "Main data import task");
    importTasks = List.copyOf(tasks);
    mainImportTask = ThreadPoolTask.createDefaultTaskManagerPool("Importing data", tasks);
    metadataFile = parameters.getEmbeddedParameterValueIfSelectedOrElse(
        AllSpectralDataImportParameters.metadataFile, null);
    extractMetadataParameters = parameters.getEmbeddedParametersIfSelectedOrElse(
        AllSpectralDataImportParameters.extractMetadata, null);
    sortAndRecolor = parameters.getValue(AllSpectralDataImportParameters.sortAndRecolor);
    this.parameters = parameters;
    this.fixedProject = fixedProject;
    this.cancelOnCurrentProjectChange = cancelOnCurrentProjectChange;
    baselineRawDataFiles = fixedProject == null ? Set.of()
        : Set.copyOf(fixedProject.getCurrentRawDataFiles());
    this.rawDataImportTasks = List.copyOf(rawDataImportTasks);
  }


  @Override
  public String getTaskDescription() {
    return mainImportTask.getTaskDescription();
  }

  @Override
  public double getFinishedPercentage() {
    return fixedProject == null ? mainImportTask.getFinishedPercentage()
        : importTasks.stream().mapToDouble(Task::getFinishedPercentage).average().orElse(0);
  }

  @Override
  public void run() {
    if (cancelIfFixedProjectChanged()) {
      return;
    }
    setStatus(TaskStatus.PROCESSING);

    // do data import and library import directly and import metadata after all is completed.
    // this ensures that data files are already loaded
    if (fixedProject != null) {
      if (!runFixedProjectImports()) {
        return;
      }
    } else {
      mainImportTask.run();
    }
    if (fixedProject == null && mainImportTask.isCanceled()) {
      setStatus(mainImportTask.getStatus());
      setErrorMessage(mainImportTask.getErrorMessage());
      return;
    }
    if (cancelIfFixedProjectChanged()) {
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
      if (cancelIfFixedProjectChanged()) {
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
      if (cancelIfFixedProjectChanged()) {
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
      if (cancelIfFixedProjectChanged()) {
        return;
      }
    }

    setStatus(TaskStatus.FINISHED);
  }

  /** Wait for actual worker exit, even when cancellation has already completed its Future. */
  private boolean runFixedProjectImports() {
    if (isCanceled() || cancelIfFixedProjectChanged()) {
      return false;
    }
    final WrappedTask[] submitted = MZmineCore.getTaskController().addTasks(
        importTasks.toArray(Task[]::new));
    if (submitted == null) {
      setErrorMessage("The local import tasks could not be submitted.");
      setStatus(TaskStatus.ERROR);
      return false;
    }
    fixedImportHandles = List.of(submitted);
    boolean interrupted = false;
    try {
      for (final WrappedTask task : fixedImportHandles) {
        if (task.getFuture() == null) {
          task.cancel();
          setErrorMessage("A local import task could not be submitted.");
          setStatus(TaskStatus.ERROR);
        }
      }
      while (fixedImportHandles.stream().anyMatch(task -> !task.isExecutionComplete())) {
        interrupted |= Thread.interrupted();
        if (interrupted || isCanceled() || cancelIfFixedProjectChanged()) {
          fixedImportHandles.forEach(WrappedTask::cancel);
        }
        java.util.concurrent.locks.LockSupport.parkNanos(10_000_000L);
      }
      if (importTasks.stream().anyMatch(task -> task.getStatus() == TaskStatus.ERROR)) {
        setErrorMessage("One or more local import tasks failed. See local task details.");
        setStatus(TaskStatus.ERROR);
        return false;
      }
      if (isCanceled() || importTasks.stream().anyMatch(Task::isCanceled)) {
        cancel();
        return false;
      }
      return true;
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private ColorByMetadataTask recolorBlanksAndQcs() {
    List<RawDataFile> loaded = AllSpectralDataImportParameters.getLoadedRawDataFiles(
        getProject(), parameters);

    ColorByMetadataTask task = fixedProject == null ? new ColorByMetadataTask(moduleCallDate,
        ColorByMetadataParameters.createDefault(loaded), AllSpectralDataImportModule.class)
        : new ColorByMetadataTask(moduleCallDate, ColorByMetadataParameters.createDefault(loaded),
            AllSpectralDataImportModule.class, loaded.toArray(RawDataFile[]::new), fixedProject);
    runFollowupTask(task);
    return task;
  }

  private Task loadMetadata() {
    var metaParams = ProjectMetadataImportParameters.create(metadataFile, false, false);
    var metadataTask = new ProjectMetadataImportTask(metaParams, moduleCallDate, fixedProject);
    runFollowupTask(metadataTask);
    return metadataTask;
  }

  private Task extractMetadata() {
    final List<RawDataFile> loaded = AllSpectralDataImportParameters.getLoadedRawDataFiles(
        getProject(), parameters);
    final RawDataFile[] files = loaded.toArray(RawDataFile[]::new);
    final Task extractTask = fixedProject == null
        ? SampleMetadataExtractionParameters.createTaskWithDataFiles(extractMetadataParameters,
            moduleCallDate, AllSpectralDataImportModule.class, files)
        : SampleMetadataExtractionParameters.createTaskWithDataFiles(extractMetadataParameters,
            moduleCallDate, AllSpectralDataImportModule.class, files, fixedProject);
    runFollowupTask(extractTask);
    return extractTask;
  }

  /** Cancels both the pool and whichever native post-import task is currently running. */
  @Override
  public void cancel() {
    super.cancel();
    mainImportTask.cancel();
    if (fixedProject != null) {
      importTasks.forEach(Task::cancel);
      fixedImportHandles.forEach(WrappedTask::cancel);
    }
    final Task followup = currentFollowupTask;
    if (followup != null) {
      followup.cancel();
    }
  }

  /** Actual raw files added by this fixed-project import; ordinary imports intentionally expose none. */
  public @NotNull List<RawDataFile> getImportedRawDataFiles() {
    if (fixedProject == null) {
      return List.of();
    }
    return AllSpectralDataImportParameters.getLoadedRawDataFiles(fixedProject, parameters).stream()
        .filter(raw -> !baselineRawDataFiles.contains(raw)).toList();
  }

  /** Bounded measured import counts for the fixed project. */
  public @NotNull ImportOutcome getImportOutcome() {
    final int selected = rawDataImportTasks.size();
    final int imported = getImportedRawDataFiles().size();
    final int failed = (int) rawDataImportTasks.stream()
        .filter(task -> task.getStatus() == TaskStatus.ERROR).count();
    return new ImportOutcome(selected, imported, failed);
  }

  private void runFollowupTask(final @NotNull Task task) {
    if (cancelIfFixedProjectChanged()) {
      task.cancel();
      return;
    }
    currentFollowupTask = task;
    try {
      task.run();
    } finally {
      currentFollowupTask = null;
    }
  }

  private @NotNull MZmineProject getProject() {
    return fixedProject == null ? ProjectService.getProject() : fixedProject;
  }

  private boolean cancelIfFixedProjectChanged() {
    if (cancelOnCurrentProjectChange && ProjectService.getProject() != fixedProject) {
      setErrorMessage("The reviewed project changed before import could continue.");
      cancel();
      return true;
    }
    return false;
  }

  public record ImportOutcome(int selectedRawFileCount, int importedRawFileCount,
                              int failedRawFileCount) {
  }

  @Override
  public TaskPriority getTaskPriority() {
    return TaskPriority.HIGH;
  }
}
