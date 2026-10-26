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

package io.github.mzmine.modules.tools.tools_autoparam.statistics;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.preferences.VendorImportParameters;
import io.github.mzmine.javafx.dialogs.NotificationService;
import io.github.mzmine.javafx.dialogs.NotificationService.NotificationType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.DataImportWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
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
          "No suitable isotope-supported signals were found for the selected wizard workflow. "
              + "Parameter estimation requires MS1 isotope evidence. Check the selected files, "
              + "polarity, and wizard interface.");
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
    final List<RawDataFile> imported = project.getCurrentRawDataFiles().stream()
        .filter(raw -> files.contains(raw.getFileName())).toList();
    warnIfPartiallyImported(files, imported);
    return imported;
  }

  /**
   * A partial import continues with the imported files. No files fail the callers.
   */
  private static void warnIfPartiallyImported(@NotNull Set<String> requested,
      @NotNull List<RawDataFile> imported) {
    if (imported.isEmpty() || imported.size() >= requested.size()) {
      return;
    }
    final Set<String> missing = new HashSet<>(requested);
    imported.forEach(raw -> missing.remove(raw.getFileName()));
    final String message = "Only %d of %d raw data files were imported, continuing without: %s".formatted(
        imported.size(), requested.size(), String.join(", ", missing));
    logger.warning(message);
    NotificationService.show(NotificationType.WARNING, "Raw data files missing", message);
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
        .filter(file -> SampleType.guessFromName(file.getName()) == SampleType.QC).toList(), 10);
    if (qcFiles.size() >= 3) {
      return qcFiles.toArray(File[]::new);
    }

    final File[] nonBlankFiles = Arrays.stream(allFiles)
        .filter(file -> SampleType.guessFromName(file.getName()) != SampleType.BLANK).limit(10)
        .toArray(File[]::new);
    final File[] candidates = nonBlankFiles.length > 3 ? nonBlankFiles : allFiles;
    return CollectionUtils.selectRandomElements(List.of(candidates), 10).toArray(File[]::new);
  }

  /**
   * Selects raw-data input for native analysis from a wizard Data Import step without changing the
   * wizard's full data selection.
   *
   * @param sequence             wizard sequence containing the Data Import step
   * @param minimumAnalysisFiles minimum number of files required by the caller
   * @param selectionMode        whether to use the representative set or every selected file
   * @return immutable snapshots of the Data Import files, analysis files, and metadata file
   * @throws IllegalArgumentException if {@code minimumAnalysisFiles} is less than one
   * @throws IllegalStateException    if the Data Import step or required input is unavailable
   */
  public static @NotNull WizardAnalysisInput selectWizardAnalysisInput(
      @NotNull WizardSequence sequence, final int minimumAnalysisFiles,
      @NotNull AnalysisInputSelectionMode selectionMode) {
    if (minimumAnalysisFiles < 1) {
      throw new IllegalArgumentException("Minimum analysis files must be at least one.");
    }
    final WizardStepParameters importStep = sequence.get(WizardPart.DATA_IMPORT).orElseThrow(
        () -> new IllegalStateException("The wizard has no data import step."));
    final File[] dataFiles = importStep.getValue(DataImportWizardParameters.fileNames);
    if (dataFiles == null || dataFiles.length == 0) {
      throw new IllegalStateException("Select at least one raw data file in the Data Import step first.");
    }
    if (Arrays.stream(dataFiles).anyMatch(Objects::isNull)) {
      throw new IllegalStateException("The Data Import step contains an invalid raw data file.");
    }
    final File[] analysisFiles = switch (selectionMode) {
      case REPRESENTATIVE -> selectOptimizerInputFiles(dataFiles);
      case ALL -> dataFiles;
    };
    if (analysisFiles.length < minimumAnalysisFiles) {
      throw new IllegalStateException("""
          This requires at least %d raw data files. Those data files need to be comparable (same \
          instrument, same method, similar or same sample).""".formatted(minimumAnalysisFiles));
    }
    final File metadataFile = importStep.getOptionalValue(DataImportWizardParameters.metadataFile)
        .orElse(null);
    return new WizardAnalysisInput(dataFiles, analysisFiles, metadataFile);
  }

  /** Selection strategy for wizard raw-data analysis. */
  public enum AnalysisInputSelectionMode {
    REPRESENTATIVE, ALL
  }

  /** Immutable snapshots of the complete Data Import selection and its analysis subset. */
  public record WizardAnalysisInput(File @NotNull [] dataFiles, File @NotNull [] analysisFiles,
                                    @Nullable File metadataFile) {

    public WizardAnalysisInput {
      dataFiles = dataFiles.clone();
      analysisFiles = analysisFiles.clone();
    }

    @Override
    public File @NotNull [] dataFiles() {
      return dataFiles.clone();
    }

    @Override
    public File @NotNull [] analysisFiles() {
      return analysisFiles.clone();
    }
  }
}
