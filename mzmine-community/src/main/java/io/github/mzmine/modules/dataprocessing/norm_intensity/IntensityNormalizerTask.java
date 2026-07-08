/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedAreaType;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedHeightType;
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
import java.util.Map;
import java.util.Map.Entry;
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

    // currently intra batch activates inter batch
    this.interBatchCorrectionEnabled = intraBatchCorrectionEnabled;

    // 1+ for applying the functions at the end
    totalNormalizationSteps = 1 + ObjectUtils.countTrue(byMetadataEnabled, internalStandardEnabled,
        intraBatchCorrectionEnabled, interBatchCorrectionEnabled);
  }

  public double getFinishedPercentage() {
    return (double) processedFiles / (double) totalFiles / (double) totalNormalizationSteps;
  }

  public String getTaskDescription() {
    return "Intensity normalization of " + originalFeatureList + " by " + normalizationType;
  }

  public void run() {

    setStatus(TaskStatus.PROCESSING);
    logger.fine("Running Intensity normalizer");

    // create copy or prepare in place featurelist
    prepareNormalizedFeatureList();

    // build up summary as we go
    final IntensityNormalizationSearchableSummary summary = new IntensityNormalizationSearchableSummary(
        normalizedFeatureList.getRawDataFiles().size());

    final MetadataTable metadata = ProjectService.getMetadata();


    // ── Pass 1: pre-normalization by metadata column (dilution factor, sample weight, …) ──
    // should happen before separating samples into batches because factor may be scaled to median
    // and this should happen for all samples together and not within batches
    if (byMetadataEnabled) {
      normalizeByMetadataColumn(summary, metadata);
      if (isCanceled()) {
        return;
      }
    }


    // split samples into batches so that the QC of 2nd batch does not influence the first batch
    final @NotNull List<SamplesBatch> sampleBatches = splitSampleBatches(metadata);
    totalNormalizationSteps += sampleBatches.size() <= 1 ? 0 : 1;

    for (SamplesBatch samplesBatch : sampleBatches) {
      normalizeSamplesBatch(samplesBatch, summary);
      if (isCanceled()) {
        return;
      }
    }

    // only if intra batch correction is enabled then also apply inter batch correction
    if (intraBatchCorrectionEnabled && sampleBatches.size() > 1) {
      totalNormalizationSteps++;
      // inter batch normalization by median intensities in reference samples
      normalizeSamplesInterBatches(sampleBatches, summary);
    }

    if (isCanceled()) {
      return;
    }

    // finally apply the normalization functions
    applyFunctionsToFeatures(normalizedFeatureList, summary.functions());

    final ParameterSet appliedMethodParameters = mainParameters.cloneParameterSet(true);
    appliedMethodParameters.setParameter(IntensityNormalizerParameters.hiddenNormalizationSummary,
        summary.toSimpleSummary());

    normalizedFeatureList.addDescriptionOfAppliedTask(new SimpleFeatureListAppliedMethod(
        "Intensity normalization by " + normalizationType + (batchIdEnabled
            ? " (batch-aware; column %s)".formatted(batchIdColumn) : "") + (byMetadataEnabled
            ? ", pre: metadata (%s)".formatted(byMetadataColumn) : "") + (internalStandardEnabled
            ? ", pre: IS" : ""), IntensityNormalizerModule.class, appliedMethodParameters,
        getModuleCallDate()));

    // Add normalized feature list to the project.
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

    // norm factors are already set by
    final double[] batchNormMetrics = sampleBatches.stream()
        .mapToDouble(SamplesBatch::getMedianReferenceNormMetric).filter(Double::isFinite).toArray();

    // values might be NaN if they are unset
    if (batchNormMetrics.length != sampleBatches.size()) {
      logger.fine(
          "Skipping inter batch correction as it seems to be not supported by this batch normalizer: "
              + normalizationTypeModule.getName());
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
        // add and merge function into summary
        summary.addMergeFunction(raw, fileFunction);
        processedFiles++;
      }
    }
  }

  /// Applies normalization to a single samples batch (intra batch) which may be selected by
  /// metadata. If no batching is applied, all samples may be handled as a single batch.
  ///
  /// Metadata factor normalization should be applied to all samples without batching as there is an
  /// option to normalize the metadata factor medians to keep intensity scale similar. Applying
  /// batching changes the median per batch and this may result in wrong normalization.
  ///
  /// Applies:
  /// - internal standards normalization sample-wise.
  /// - Reference samples (QC)-based intra batch correction
  ///
  /// @param samplesBatch the samples of this batch. Normalizers may set the medianNormMetric of
  /// this batch for later inter batch correction.
  /// @param summary      the summary to save normalization results to
  private void normalizeSamplesBatch(SamplesBatch samplesBatch,
      IntensityNormalizationSearchableSummary summary) {
    final MetadataTable metadata = ProjectService.getMetadata();

    // ── Pass 2: pre-normalization by internal standard compounds ──
    // usually applied by internal standards to each sample
    // but this may be applied to internal standards in QCs and then interpolated to samples
    if (internalStandardEnabled) {
      try {
        // Use normalized abundances as base for IS metric computation if pass 1 already ran.
        final NormalizationTypeModule isNormalizer = internalStandardNormalizer.getModuleInstance();
        isNormalizer.createAllNormalizationFunctionsToSummary(summary, normalizedFeatureList,
            samplesBatch, metadata, mainParameters, internalStandardParams);
        processedFiles += samplesBatch.size();
      } catch (IllegalStateException e) {
        error("Pre-normalization internal standards: " + e.getMessage());
        return;
      }
    }

    // ── Pass 3: main normalization (QC drift correction, optionally batch-aware) ──
    if (intraBatchCorrectionEnabled) {
      try {
        normalizationTypeModule.createAllNormalizationFunctionsToSummary(summary,
            normalizedFeatureList, samplesBatch, metadata, mainParameters,
            normalizationTypeModuleParameters);
        processedFiles += samplesBatch.size();
      } catch (IllegalStateException e) {
        error("Error during %s step by %s: ".formatted(
            IntensityNormalizerParameters.normalizationType.getName(),
            normalizationTypeModule.getName()) + e.getMessage());
        return;
      }
    }
  }

  private void normalizeByMetadataColumn(IntensityNormalizationSearchableSummary summary, MetadataTable metadata) {
    final MetadataColumnNormalizationTypeModule metadataModule = new MetadataColumnNormalizationTypeModule();
    final MetadataColumnNormalizationTypeParameters metadataModuleParams = MetadataColumnNormalizationTypeParameters.create(
        byMetadataColumn);
    // MetadataColumn normalization covers all files (no interpolation needed).
    try {
      // use all raw data files here, batching not needed
      metadataModule.createAllNormalizationFunctionsToSummary(summary, normalizedFeatureList,
          new SamplesBatch(normalizedFeatureList.getRawDataFiles()), metadata, mainParameters, metadataModuleParams);

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
    // Create new feature list and copy all rows up front.
    normalizedFeatureList = handleOriginal.isProcessInPlace() ? originalFeatureList
        : FeatureListUtils.createCopy(originalFeatureList, suffix, storage, true);

    final NormalizedAreaType normAreaType = DataTypes.get(NormalizedAreaType.class);
    final NormalizedHeightType normHeightType = DataTypes.get(NormalizedHeightType.class);

    if (normalizedFeatureList.hasFeatureType(normAreaType)) {
      // clear old normalization
      FeatureDataUtils.clearIntensityNormalization(normalizedFeatureList);
    } else {
      // add as feature types and row type will be added as binding
      normalizedFeatureList.addFeatureType(normHeightType);
      normalizedFeatureList.addFeatureType(normAreaType);
    }
  }


  /**
   * Applies the normalization functions to all features in the feature list.
   */
  private void applyFunctionsToFeatures(@NotNull final ModularFeatureList featureList,
      final @NotNull Map<RawDataFile, RawFileNormalizationFunction> fileToFunction) {

    if (hasFinishedApplyFunctions) {
      throw new IllegalStateException("Cannot apply functions twice. Should only normalize once.");
    }
    hasFinishedApplyFunctions = true;

    for (Entry<RawDataFile, RawFileNormalizationFunction> entry : fileToFunction.entrySet()) {
      final NormalizationFunction fn = entry.getValue().function();
      final RawDataFile raw = entry.getKey();
      if (isCanceled()) {
        return;
      }
      for (final FeatureListRow row : featureList.getRows()) {
        final Feature feature = row.getFeature(raw);
        if (!(feature instanceof ModularFeature mfeature)) {
          continue;
        }

        FeatureDataUtils.normalizeAbundances(mfeature, fn);
      }

      // each function is one raw data files finished
      processedFiles++;
    }
  }

}
