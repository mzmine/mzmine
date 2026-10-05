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

package io.github.mzmine.modules.tools.tools_autoparam.estimation;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.preferences.VendorImportParameters;
import io.github.mzmine.javafx.dialogs.NotificationService;
import io.github.mzmine.javafx.dialogs.NotificationService.NotificationType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.modules.tools.tools_autoparam.AutoParamModule;
import io.github.mzmine.modules.tools.tools_autoparam.AutoParamParameters;
import io.github.mzmine.modules.tools.tools_autoparam.AutoParamTask;
import io.github.mzmine.modules.tools.tools_autoparam.DataFileStatistics;
import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.AllTasksFinishedListener;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskService;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.collections.CollectionUtils;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Imports raw files and computes the per-file measurements used for parameter preparation.
 */
public final class RawDataPreparation {

  private static final Logger logger = Logger.getLogger(RawDataPreparation.class.getName());

  private RawDataPreparation() {
  }

  /**
   * Derives the per file statistics the estimator and the variable ranges are both built from. This
   * is the cheap half of an optimization - one pass over each file, no batch runs - so a caller
   * that only wants to inspect or re-derive estimates can stop here.
   *
   * @param benchmarkFeatures additional target features, or null
   * @param polarity          only MS1 scans of this polarity are used, {@link PolarityType#ANY} for
   *                          all
   * @return the statistics of all files that could be analyzed. Failed files are skipped with a
   * warning.
   * @throws IllegalStateException if no file could be analyzed. The message names the files and
   *                               reasons.
   */
  public static @NotNull List<DataFileStatistics> computeFileStatistics(
      @NotNull List<RawDataFile> importedFiles, @Nullable List<FeatureRecord> benchmarkFeatures,
      @Nullable MemoryMapStorage storage, @NotNull PolarityType polarity) {
    final List<AutoParamTask> tasks = importedFiles.stream()
        .map(file -> new AutoParamTask(storage, Instant.now(),
            AutoParamParameters.of(importedFiles, polarity), AutoParamModule.class, file,
            benchmarkFeatures, false)).toList();
    tasks.parallelStream().forEach(AutoParamTask::run);

    final List<DataFileStatistics> statistics = tasks.stream().map(AutoParamTask::get)
        .filter(Objects::nonNull).toList();
    final List<AutoParamTask> failed = tasks.stream().filter(task -> task.get() == null).toList();
    if (failed.isEmpty()) {
      return statistics;
    }

    final String message = describeFailedStatistics(failed, tasks.size());
    if (statistics.isEmpty()) {
      throw new IllegalStateException(message);
    }
    logger.warning(message);
    NotificationService.show(NotificationType.WARNING, "Skipped raw data files", message);
    return statistics;
  }

  private static @NotNull String describeFailedStatistics(@NotNull List<AutoParamTask> failed,
      int total) {
    return "Could not compute statistics for %d of %d file(s):\n".formatted(failed.size(), total)
        + failed.stream().map(task -> "%s: %s".formatted(task.getDataFile().getName(),
            Objects.requireNonNullElse(task.getErrorMessage(), task.getStatus().toString())))
        .collect(Collectors.joining("\n"));
  }

  /**
   * The m/z tolerance and the peak shape estimates are derived from isotope envelopes. Without any
   * envelope, an estimate would be arbitrary.
   *
   * @throws IllegalStateException if none of the files contains a potential isotope envelope
   */
  public static void requireIsotopeSignals(@NotNull List<DataFileStatistics> statistics) {
    if (statistics.stream().allMatch(stat -> stat.featureStatistics().isEmpty())) {
      throw new IllegalStateException(
          "Unable to detect potential isotopes. Parameter estimation not possible.");
    }
  }

  /**
   * Imports the files into the current project and waits for the import to finish. Files that are
   * already loaded are not imported again.
   *
   * @throws IllegalStateException if the import could not be started, failed, or was canceled. The
   *                               message contains the reason.
   */
  public static @NotNull List<RawDataFile> importFilesBlocking(File @NotNull [] filesToImport,
      @Nullable File metadata) {
    final ParameterSet importParam = AllSpectralDataImportParameters.create(
        VendorImportParameters.createDefault(), filesToImport, metadata, null);
    final List<Task> tasks = new ArrayList<>();
    final MZmineProject project = ProjectService.getProject();
    final ExitCode exitCode = MZmineCore.getModuleInstance(AllSpectralDataImportModule.class)
        .runModule(project, importParam, tasks, Instant.now());
    if (exitCode != ExitCode.OK || tasks.isEmpty()) {
      // the import module already showed or logged the reason
      throw new IllegalStateException(
          "Could not start the raw data import. Check the selected files and the metadata file.");
    }

    // completes with null on success, otherwise with the error message
    final CompletableFuture<@Nullable String> importError = new CompletableFuture<>();
    // register before adding the tasks, so a fast import cannot finish unnoticed.
    // stopOnError, otherwise an error is reported as finished
    AllTasksFinishedListener.registerCallbacks(tasks, true, _ -> importError.complete(null),
        failed -> importError.complete(describeImportErrors(failed)),
        _ -> importError.complete("The raw data import was canceled."));
    TaskService.getController().addTasks(tasks.toArray(new Task[0]));

    final String error = importError.join();
    if (error != null) {
      throw new IllegalStateException(error);
    }

    final Set<String> files = new HashSet<>(
        List.of(filesToImport).stream().map(File::getName).toList());
    return project.getCurrentRawDataFiles().stream()
        .filter(raw -> files.contains(raw.getFileName())).toList();
  }

  private static @NotNull String describeImportErrors(@NotNull List<? extends Task> tasks) {
    final String messages = tasks.stream().filter(task -> task.getStatus() == TaskStatus.ERROR)
        .map(Task::getErrorMessage).filter(Objects::nonNull).distinct()
        .collect(Collectors.joining("\n"));
    return messages.isBlank() ? "The raw data import failed."
        : "The raw data import failed:\n" + messages;
  }

  /**
   * Uses the same small, representative input set for raw-data estimation and optimization.
   */
  public static File @NotNull [] selectOptimizerInputFiles(File @NotNull [] allFiles) {
    final List<File> qcFiles = CollectionUtils.selectRandomElements(Arrays.stream(allFiles)
        .filter(file -> SampleType.guessFromName(file.getName()) == SampleType.QC).limit(10)
        .toList(), 10);
    if (qcFiles.size() >= 3) {
      return qcFiles.toArray(File[]::new);
    }

    final File[] nonBlankFiles = Arrays.stream(allFiles)
        .filter(file -> SampleType.guessFromName(file.getName()) != SampleType.BLANK).limit(10)
        .toArray(File[]::new);
    final File[] candidates = nonBlankFiles.length > 3 ? nonBlankFiles : allFiles;
    return CollectionUtils.selectRandomElements(List.of(candidates), 10).toArray(File[]::new);
  }
}
