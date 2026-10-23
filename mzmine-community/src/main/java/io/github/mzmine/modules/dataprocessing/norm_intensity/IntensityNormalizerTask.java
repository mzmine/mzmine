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

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedAreaType;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedHeightType;
import io.github.mzmine.modules.dataprocessing.norm_intensity.StandardCompoundNormalizationTypeModule.StandardCompoundSelection;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.submodules.ValueWithParameters;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.StringUtils;
import io.github.mzmine.util.objects.ObjectUtils;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class IntensityNormalizerTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(IntensityNormalizerTask.class.getName());

  private final OriginalFeatureListOption handleOriginal;

  private final MZmineProject project;
  private final ModularFeatureList originalFeatureList;
  private ModularFeatureList normalizedFeatureList;

  private final long totalFiles;
  private long processedFiles;

  private final String suffix;

  private final boolean intraBatchCorrectionEnabled;
  private final boolean interBatchCorrectionEnabled;
  private final NormalizationType normalizationType;
  private final NormalizationTypeModule normalizationTypeModule;
  private final ParameterSet normalizationTypeModuleParameters;
  private final ParameterSet mainParameters;

  // Pre-normalization: metadata (dilution factor, sample weight, injection volume)
  private final boolean byMetadataEnabled;
  private final MetadataNormalizationConfig byMetadataColumn;

  // Pre-normalization: internal standards
  private final NormalizationType internalStandardNormalizer;
  private final boolean internalStandardEnabled;
  private final ParameterSet internalStandardParams;
  /**
   * Standard rows resolved once for the whole feature list, before the samples are split into
   * batches. Null if the internal standard normalizer does not support a precomputed selection.
   */
  private @Nullable StandardCompoundSelection internalStandardSelection;

  // Batch-aware main normalization
  private final @Nullable String batchIdColumn;
  private final boolean batchIdEnabled;

  private int totalNormalizationSteps;
  // can only apply once at end
  private boolean hasFinishedApplyFunctions = false;

  public IntensityNormalizerTask(MZmineProject project, FeatureList featureList,
      ParameterSet parameters, @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate); // no new data stored -> null

    this.project = project;
    this.originalFeatureList = (ModularFeatureList) featureList;
    this.mainParameters = parameters;

    suffix = parameters.getParameter(IntensityNormalizerParameters.suffix).getValue();

    final ValueWithParameters<NormalizationType> normalizationTypeWithParameters = parameters.getParameter(
        IntensityNormalizerParameters.normalizationType).getValueWithParameters();
    normalizationType = normalizationTypeWithParameters.value();
    intraBatchCorrectionEnabled = normalizationType.isActive();
    normalizationTypeModule = normalizationType.getModuleInstance();
    normalizationTypeModuleParameters = normalizationTypeWithParameters.parameters();

    handleOriginal = parameters.getParameter(IntensityNormalizerParameters.handleOriginal)
        .getValue();
    totalFiles = originalFeatureList.getNumberOfRawDataFiles();

    // Pre-normalization: metadata (optional)
    final var preMetaParam = parameters.getParameter(
        IntensityNormalizerParameters.metadataNormFactorCol);
    byMetadataEnabled = preMetaParam.getValue();
    byMetadataColumn = byMetadataEnabled ? preMetaParam.getEmbeddedParameter().getValue() : null;

    // Pre-normalization: internal standards (optional)
    final var internalParam = parameters.getParameter(
        IntensityNormalizerParameters.internalStandardization).getValueWithParameters();
    internalStandardNormalizer = internalParam.value();
    internalStandardEnabled = internalStandardNormalizer.isActive();
    internalStandardParams = internalStandardEnabled ? internalParam.parameters() : null;

    // Batch-aware main normalization
    batchIdColumn = parameters.getOptionalValue(IntensityNormalizerParameters.batchIdColumn)
        .map(String::strip).filter(StringUtils::hasValue).orElse(null);
    batchIdEnabled = batchIdColumn != null;

    this.interBatchCorrectionEnabled =
        intraBatchCorrectionEnabled && normalizationType != NormalizationType.FeatureQcLoess;

    // 1+ for applying the functions at the end
    totalNormalizationSteps = 1 + ObjectUtils.countTrue(byMetadataEnabled, internalStandardEnabled,
        intraBatchCorrectionEnabled, interBatchCorrectionEnabled);
  }

  public double getFinishedPercentage() {
    return isFinished() ? 1d : totalFiles == 0 ? 0d
        : Math.min(1d, (double) processedFiles / totalFiles / totalNormalizationSteps);
  }

  public String getTaskDescription() {
    return "Intensity normalization of " + originalFeatureList + " by " + normalizationType;
  }

  public void run() {
    if (isCanceled()) {
      return;
    }
    setStatus(TaskStatus.PROCESSING);
    try {
      normalize();
    } catch (final java.util.concurrent.CancellationException exception) {
      cancel();
    } catch (final RuntimeException exception) {
      error("Intensity normalization: " + exception.getMessage(), exception);
    }
  }

  private void normalize() {
    logger.fine("Running Intensity normalizer");
    prepareNormalizedFeatureList();
    final IntensityNormalizationSearchableSummary summary = new IntensityNormalizationSearchableSummary(
        normalizedFeatureList.getRawDataFiles().size());
    final MetadataTable metadata = ProjectService.getMetadata();
    final List<SamplesBatch> sampleBatches = splitSampleBatches(metadata);
    totalNormalizationSteps = 1 + ObjectUtils.countTrue(byMetadataEnabled, internalStandardEnabled,
        intraBatchCorrectionEnabled, interBatchCorrectionEnabled && sampleBatches.size() > 1);
    if (byMetadataEnabled) {
      // First compose sample-specific metadata factors into every later correction.
      normalizeByMetadataColumn(summary, metadata);
      if (isCanceled()) {
        return;
      }
    }
    if (internalStandardEnabled) {
      // Then add internal-standard corrections, resolved once before batches are processed.
      selectInternalStandards(summary);
      if (isCanceled()) {
        return;
      }
      for (final SamplesBatch batch : sampleBatches) {
        applyInternalStandardization(batch, summary, metadata);
        processedFiles += batch.size();
        if (isCanceled()) {
          return;
        }
      }
    }
    if (intraBatchCorrectionEnabled) {
      // The main correction builds functions only; values remain unchanged until the final commit.
      if (normalizationTypeModule instanceof FeatureQcNormalizationModule featureQc) {
        featureQc.createAllNormalizationFunctionsToSummary(summary, normalizedFeatureList,
            sampleBatches, metadata, mainParameters, normalizationTypeModuleParameters,
            this::isCanceled, count -> processedFiles += count);
      } else {
        for (final SamplesBatch batch : sampleBatches) {
          normalizationTypeModule.createAllNormalizationFunctionsToSummary(summary,
              normalizedFeatureList, batch, metadata, mainParameters,
              normalizationTypeModuleParameters);
          processedFiles += batch.size();
          if (isCanceled()) {
            return;
          }
        }
      }
    }
    if (isCanceled()) {
      return;
    }
    if (interBatchCorrectionEnabled && sampleBatches.size() > 1) {
      // Scalar methods may additionally align their already-corrected batch reference metrics.
      normalizeSamplesInterBatches(sampleBatches, summary);
    }
    // last cancel check: applying the functions is a single cheap pass that always runs to the end,
    // so values, settings and applied method always match
    if (isCanceled()) {
      return;
    }
    applyFunctionsToFeatures(normalizedFeatureList, summary);
    for (final String message : summary.messages()) {
      logger.log(message.contains(IntensityNormalizationSummary.WARNING_MARKER) ? Level.WARNING
          : Level.INFO, message);
    }

    // decision: the functions are stored once in the settings, the applied method keeps only the
    // diagnostics (messages and feature-QC provenance) for the applied methods pane
    final IntensityNormalizationSummary simpleSummary = summary.toSimpleSummary();
    normalizedFeatureList.setSettings(
        normalizedFeatureList.getSettings().withNormalization(simpleSummary.withFunctionsOnly()));
    final ParameterSet appliedMethodParameters = mainParameters.cloneParameterSet(true);
    appliedMethodParameters.setParameter(IntensityNormalizerParameters.hiddenNormalizationSummary,
        simpleSummary.withDiagnosticsOnly());
    normalizedFeatureList.addDescriptionOfAppliedTask(new SimpleFeatureListAppliedMethod(
        "Intensity normalization by " + normalizationType + (batchIdEnabled
            ? " (batch-aware; column %s)".formatted(batchIdColumn) : "") + (byMetadataEnabled
            ? ", pre: metadata (%s)".formatted(byMetadataColumn) : "") + (internalStandardEnabled
            ? ", pre: IS" : ""), IntensityNormalizerModule.class, appliedMethodParameters,
        getModuleCallDate()));

    // canceled while applying: an in-place list keeps the complete values and matching settings,
    // a copy is not published and the original stays in the project
    if (isCanceled()) {
      return;
    }
    handleOriginal.reflectNewFeatureListToProject(suffix, project, normalizedFeatureList,
        originalFeatureList);
    logger.fine("Finished intensity normalization");
    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Normalize by median intensity of reference samples inter batch
   *
   * @param sampleBatches
   * @param summary
   */
  private void normalizeSamplesInterBatches(@NotNull List<SamplesBatch> sampleBatches,
      IntensityNormalizationSearchableSummary summary) {

    // Intra-batch normalization has already attached a per-file metric. This phase only aligns
    // those finite batch metrics and merges its factor into each file's existing function.
    final double[] batchNormMetrics = sampleBatches.stream()
        .mapToDouble(SamplesBatch::getMedianReferenceNormMetric).filter(Double::isFinite).toArray();

    // values might be NaN if they are unset
    if (batchNormMetrics.length != sampleBatches.size()) {
      logger.fine(
          "Skipping inter batch correction as it seems to be not supported by this batch normalizer: "
              + normalizationTypeModule.getName());
      processedFiles += sampleBatches.stream().mapToInt(SamplesBatch::size).sum();
      return;
    }

    logger.fine("Normalizing samples inter batches n=" + sampleBatches.size());
    final double interBatchMedian = MathUtils.calcMedian(batchNormMetrics);

    // apply those to all samples in the same batch
    for (SamplesBatch sampleBatch : sampleBatches) {
      final double normFactor = interBatchMedian / sampleBatch.getMedianReferenceNormMetric();
      // reuse same function for all files in this batch
      final FactorNormalizationFunction fileFunction = new FactorNormalizationFunction(normFactor);

      logger.fine("Normalizing %d samples in batch %s inter batches norm metric=%.4f".formatted(
          sampleBatch.size(), sampleBatch.getGroupMetadataValueStr(), normFactor));

      for (RawDataFile raw : sampleBatch.getRaws()) {
        if (isCanceled()) {
          return;
        }
        // add and merge function into summary
        summary.addMergeFunction(raw, fileFunction);
        processedFiles++;
      }
    }
  }

  private void applyInternalStandardization(SamplesBatch samplesBatch,
      IntensityNormalizationSearchableSummary summary, MetadataTable metadata) {
    final NormalizationTypeModule isNormalizer = internalStandardNormalizer.getModuleInstance();
    switch (isNormalizer) {
      case StandardCompoundNormalizationTypeModule standardModule -> {
        if (internalStandardSelection == null) {
          throw new IllegalStateException(
              "Resolved internal standards were null. It seems this module does not handle internal standards.");
        }
        // reuse the standard rows that were resolved for the whole feature list
        standardModule.createAllNormalizationFunctionsToSummary(summary, normalizedFeatureList,
            samplesBatch, metadata, mainParameters, internalStandardParams,
            internalStandardSelection);
      }
      case FeatureIntensityNormalizationModule _, TotalRawSignalNormalizationTypeModule _,
           NoNormalizationTypeModule _, MetadataColumnNormalizationTypeModule _,
           FeatureQcNormalizationModule _ -> {
        // currently only no normalization (!internalStandardEnabled) and no internal standard
        // normalization are handled here. In case we add another normalizer we may have to handle it
        throw new IllegalStateException(
            "There is an internal standard normalizer module selected %s that is not handled in intensity normalizer task.".formatted(
                isNormalizer.getName()));
      }
    }
  }

  /**
   * Resolves the internal standards to feature list rows once for the whole feature list. Has to
   * run after the metadata pre-normalization, because the standard levels are computed on the
   * already normalized abundances, and before the samples are split into batches, so that every
   * batch uses the same rows and the same reference levels.
   */
  private void selectInternalStandards(IntensityNormalizationSearchableSummary summary) {
    if (!(internalStandardNormalizer.getModuleInstance() instanceof InternalStandardSelectingNormalizer standardModule)) {
      // other normalizers resolve their references per batch
      return;
    }

    try {
      final List<RawDataFile> referenceFiles = NormalizationFunctionUtils.getReferenceSamplesOrThrow(
          false, new SamplesBatch(normalizedFeatureList.getRawDataFiles()),
          internalStandardParams.getValue(StandardCompoundNormalizationTypeParameters.sampleTypes));

      internalStandardSelection = standardModule.selectStandards(summary, normalizedFeatureList,
          referenceFiles, mainParameters, internalStandardParams);
    } catch (IllegalStateException e) {
      error("Pre-normalization internal standards: " + e.getMessage());
    }
  }

  private void normalizeByMetadataColumn(IntensityNormalizationSearchableSummary summary,
      MetadataTable metadata) {
    final MetadataColumnNormalizationTypeModule metadataModule = new MetadataColumnNormalizationTypeModule();
    final MetadataColumnNormalizationTypeParameters metadataModuleParams = MetadataColumnNormalizationTypeParameters.create(
        byMetadataColumn);
    // MetadataColumn normalization covers all files (no interpolation needed).
    try {
      // use all raw data files here, batching not needed
      metadataModule.createAllNormalizationFunctionsToSummary(summary, normalizedFeatureList,
          new SamplesBatch(normalizedFeatureList.getRawDataFiles()), metadata, mainParameters,
          metadataModuleParams);

      processedFiles += normalizedFeatureList.getNumberOfRawDataFiles();
    } catch (IllegalStateException e) {
      error("Error during pre-normalization by metadata column (" + byMetadataColumn + "): "
          + e.getMessage());
    }
  }

  private @NotNull List<SamplesBatch> splitSampleBatches(MetadataTable metadata) {
    final List<SamplesBatch> sampleBatches;
    if (batchIdEnabled) {
      final MetadataColumn<?> column = metadata.getColumnByName(batchIdColumn);
      if (column == null) {
        throw new IllegalArgumentException("Batch ID column not found: " + batchIdColumn);
      }
      sampleBatches = metadata.groupFilesByColumnIncludeNull(
              normalizedFeatureList.getRawDataFiles(), column).stream()
          .map(g -> new SamplesBatch(g.files(), g.value())).toList();
    } else {
      // handle all samples as one batch
      sampleBatches = List.of(new SamplesBatch(normalizedFeatureList.getRawDataFiles(), null));
    }
    return sampleBatches;
  }

  private void prepareNormalizedFeatureList() {
    // existing normalized values are only replaced when the functions are applied, so cancelling
    // while the functions are built leaves an in-place list unchanged
    normalizedFeatureList = handleOriginal.isProcessInPlace() ? originalFeatureList
        : FeatureListUtils.createCopy(originalFeatureList, suffix, storage, true);
  }

  /**
   * Applies the normalization functions to all features in the feature list.
   */
  private void applyFunctionsToFeatures(@NotNull final ModularFeatureList featureList,
      final @NotNull IntensityNormalizationSearchableSummary summary) {

    if (hasFinishedApplyFunctions) {
      throw new IllegalStateException("Cannot apply functions twice. Should only normalize once.");
    }
    hasFinishedApplyFunctions = true;

    final NormalizedAreaType normAreaType = DataTypes.get(NormalizedAreaType.class);
    final NormalizedHeightType normHeightType = DataTypes.get(NormalizedHeightType.class);
    if (featureList.hasFeatureType(normAreaType) || featureList.hasFeatureType(normHeightType)) {
      // clear old normalization, e.g., when normalizing in place again
      FeatureDataUtils.clearIntensityNormalization(featureList);
    } else {
      // add as feature types and row type will be added as binding
      featureList.addFeatureType(normHeightType);
      featureList.addFeatureType(normAreaType);
    }

    // decision: no cancel checks in this loop, see normalize()
    FeatureDataUtils.applyIntensityNormalization(featureList, summary);
    processedFiles += summary.size();
  }

}
