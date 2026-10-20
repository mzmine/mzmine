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

package io.github.mzmine.modules.tools.batch_resource_estimation;

import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.batchmode.LoadedBatchQueue;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.modules.io.projectsave.ProjectSaveAsModule;
import io.github.mzmine.modules.io.projectsave.ProjectSaveModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractSimpleToolTask;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Runs a batch on nested subsets of its files and a validation run and extrapolates heap, temp file
 * space and time to all files. The batch runs with the current memory and temp file settings, so
 * the estimate is valid for these settings. Export steps are removed as they do not change the
 * resources of the processing and would overwrite results.
 */
public class BatchResourceEstimationTask extends AbstractSimpleToolTask {

  private static final Logger logger = Logger.getLogger(
      BatchResourceEstimationTask.class.getName());

  private final @NotNull EstimationProjectProvider projectProvider;
  private volatile @Nullable SubsetRunner runner;
  private volatile @NotNull String description = "Batch resource estimation";

  public BatchResourceEstimationTask(@NotNull final Instant moduleCallDate,
      @NotNull final ParameterSet parameters,
      @NotNull final EstimationProjectProvider projectProvider) {
    super(moduleCallDate, parameters);
    this.projectProvider = projectProvider;
  }

  @Override
  protected void process() {
    final File batchFile = parameters.getValue(BatchResourceEstimationParameters.batchFile);
    final BatchQueue queue = loadQueue(batchFile);
    if (queue == null) {
      return;
    }
    final ParameterSet importParameters = queue.getFirst().getParameterSet();
    final File[] allFiles = importParameters.getValue(AllSpectralDataImportParameters.fileNames);
    final File metadataFile = importParameters.getEmbeddedParameterValueIfSelectedOrElse(
        AllSpectralDataImportParameters.metadataFile, null);
    if (allFiles == null || allFiles.length == 0) {
      error("The data import of the batch has no files: " + batchFile.getAbsolutePath());
      return;
    }
    final int smallestSubset = parameters.getValue(
        BatchResourceEstimationParameters.smallestSubset);
    final int levels = parameters.getValue(BatchResourceEstimationParameters.subsetSizes);
    final int requiredFiles = ResourceSubsetSelector.requiredFiles(smallestSubset, levels);
    if (allFiles.length < requiredFiles) {
      error(notEnoughFilesMessage(allFiles.length, requiredFiles));
      return;
    }

    final List<MZmineProcessingStep<MZmineProcessingModule>> removed = new ArrayList<>();
    queue.removeIf(step -> isExportStep(step.getModule()) && removed.add(step));
    if (!removed.isEmpty()) {
      logger.info("Resource estimation skips export steps: " + removed.stream()
          .map(step -> step.getModule().getName()).toList());
    }
    final List<StepScalingMode> modes = StepScalingMode.ofSteps(queue);

    description = "Batch resource estimation: reading file sizes";
    final List<EstimationInputFile> inputFiles = Arrays.stream(allFiles)
        .map(EstimationInputFile::of).toList();
    final List<EstimationInputFile> order = ResourceSubsetSelector.orderForSubsets(inputFiles);
    final double meanBytesPerFile = inputFiles.stream()
        .mapToLong(EstimationInputFile::sizeBytes).average().orElse(1);
    final SubsetRunner subsetRunner = new SubsetRunner(queue, order, metadataFile,
        meanBytesPerFile, projectProvider);
    runner = subsetRunner;

    final List<Integer> sizes = ResourceSubsetSelector.subsetSizes(allFiles.length,
        smallestSubset, levels);
    final int numThreads = ConfigService.getConfiguration().getNumOfThreads();
    // the batch has enough files for the validation run, checked above
    final int validationSize = ResourceSubsetSelector.validationSize(sizes.getLast());
    final int totalRuns = sizes.size() + 1;
    totalItems = totalRuns;

    final List<SubsetRun> runs = new ArrayList<>(sizes.size());
    for (final int size : sizes) {
      if (isCanceled()) {
        return;
      }
      description = "Batch resource estimation: processing subset of %d files (%d/%d)".formatted(
          size, runs.size() + 1, totalRuns);
      final SubsetRun run = subsetRunner.run(size);
      if (run == null) {
        return;
      }
      runs.add(run);
      incrementFinishedItems();
    }

    if (isCanceled()) {
      return;
    }
    description = "Batch resource estimation: validation run on %d files (%d/%d)".formatted(
        validationSize, totalRuns, totalRuns);
    final SubsetRun validationRun = subsetRunner.run(validationSize);
    if (validationRun == null) {
      return;
    }
    incrementFinishedItems();

    final EstimationSession session = new EstimationSession(subsetRunner, modes, numThreads,
        List.copyOf(runs), validationRun, List.of());
    final BatchResourceModel model = session.fitModel();
    BatchResourceReport.logPrediction(model);

    if (DesktopService.isGUI()) {
      FxThread.runLater(() -> BatchResourceEstimationResultTab.show(session, model, parameters));
    }
  }

  static @NotNull String notEnoughFilesMessage(final int availableFiles, final int requiredFiles) {
    return ("The batch has %d files, but the subsets and the validation run need %d files. Reduce "
        + "the smallest subset or the number of subset sizes.").formatted(availableFiles,
        requiredFiles);
  }

  /**
   * @return the number of files in the data import of the batch, null if the batch cannot be read
   * or does not start with the data import
   */
  static @Nullable Integer countImportFiles(@NotNull final File batchFile) {
    try {
      final BatchQueue queue = BatchQueue.loadFromFile(batchFile).newQueue();
      if (queue.isEmpty() || !(queue.getFirst()
          .getModule() instanceof AllSpectralDataImportModule)) {
        return null;
      }
      final File[] files = queue.getFirst().getParameterSet()
          .getValue(AllSpectralDataImportParameters.fileNames);
      return files == null ? 0 : files.length;
    } catch (Exception e) {
      logger.log(Level.FINE, "Cannot read batch file " + batchFile.getAbsolutePath(), e);
      return null;
    }
  }

  private @Nullable BatchQueue loadQueue(@NotNull final File batchFile) {
    final LoadedBatchQueue loaded;
    try {
      loaded = BatchQueue.loadFromFile(batchFile);
    } catch (Exception e) {
      error("Cannot load batch file " + batchFile.getAbsolutePath(), e);
      return null;
    }
    for (final String message : loaded.errorMessages()) {
      logger.warning("Batch file: " + message);
    }
    final BatchQueue queue = loaded.newQueue();
    if (queue.isEmpty() || !(queue.getFirst()
        .getModule() instanceof AllSpectralDataImportModule)) {
      error("The first step of the batch must be the data import with all files of the dataset.");
      return null;
    }
    return queue;
  }

  /**
   * decision: exports and project saves are removed, they write results of the subsets and their
   * resources are not the limiting part of a batch
   */
  private static boolean isExportStep(@NotNull final MZmineModule module) {
    if (module instanceof ProjectSaveModule || module instanceof ProjectSaveAsModule) {
      return true;
    }
    if (!(module instanceof MZmineProcessingModule processingModule)) {
      return false;
    }
    return switch (processingModule.getModuleCategory()) {
      case FEATURELISTEXPORT, RAWDATAEXPORT, SPECLIBEXPORT -> true;
      case PROJECTIO, PROJECT, PROJECTMETADATA, RAWDATAIMPORT, RAWDATA, RAWDATAFILTERING,
           EIC_DETECTION, GAPFILLING, ISOTOPES, FEATURELIST, FEATURE_RESOLVING, FEATURE_GROUPING,
           ION_IDENTITY_NETWORKS, SPECTRALDECONVOLUTION, FEATURELISTFILTERING, ALIGNMENT,
           NORMALIZATION, ANNOTATION, FEATURELISTIMPORT, SPECLIBIMPORT, SPECLIB_PROCESSING,
           VISUALIZATIONRAWDATA, VISUALIZATIONFEATURELIST, VISUALIZATION_RAW_AND_FEATURE,
           VISUALIZATION_OTHER_DATA, DATAANALYSIS, HELPSYSTEM, TOOLS, OTHER_DATA_PROCESSING ->
          false;
    };
  }

  @Override
  public void cancel() {
    super.cancel();
    final SubsetRunner subsetRunner = runner;
    if (subsetRunner != null) {
      // stops after the current batch step
      subsetRunner.cancel();
    }
  }

  @Override
  public String getTaskDescription() {
    return description;
  }
}
