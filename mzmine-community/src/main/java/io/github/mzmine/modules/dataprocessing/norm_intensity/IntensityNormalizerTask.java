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
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedAreaType;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedHeightType;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.norm_intensity.StandardCompoundNormalizationTypeModule.StandardCompoundSelection;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.submodules.ValueWithParameters;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.StringUtils;
import io.github.mzmine.util.objects.ObjectUtils;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BooleanSupplier;
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
  private final @Nullable ReviewedNormalization reviewedNormalization;

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

    handleOriginal = normalizationTypeModule.requiresSourceCopy() ? OriginalFeatureListOption.KEEP
        : parameters.getParameter(IntensityNormalizerParameters.handleOriginal).getValue();
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
    reviewedNormalization = null;
  }

  /**
   * Creates the native task for functions frozen by a reviewed external result. The task owns the
   * copy, clearing, application, provenance, cancellation, and source-current checks.
   */
  static @NotNull IntensityNormalizerTask createReviewedTask(final @NotNull MZmineProject project,
      final @NotNull FeatureList featureList, final @NotNull String suffix,
      final @NotNull String sourceRevision, final @NotNull IntensityNormalizationSummary summary,
      final @NotNull ParameterSet provenanceParameters,
      final @NotNull Class<? extends MZmineModule> provenanceModule,
      final @NotNull MemoryMapStorage storage, final @NotNull Instant moduleCallDate,
      final @NotNull BooleanSupplier current) {
    return new IntensityNormalizerTask(project, featureList, suffix, sourceRevision, summary,
        provenanceParameters, provenanceModule, storage, moduleCallDate, current, null, false);
  }

  static @NotNull IntensityNormalizerTask createReplayTask(final @NotNull MZmineProject project,
      final @NotNull FeatureList featureList, final @NotNull String suffix,
      final @Nullable String sourceRevision, final @NotNull IntensityNormalizationSummary summary,
      final @Nullable String expectedFingerprint, final @NotNull ParameterSet provenanceParameters,
      final @NotNull Class<? extends MZmineModule> provenanceModule,
      final @NotNull MemoryMapStorage storage, final @NotNull Instant moduleCallDate) {
    return new IntensityNormalizerTask(project, featureList, suffix,
        sourceRevision == null ? "unknown" : sourceRevision, summary, provenanceParameters,
        provenanceModule, storage, moduleCallDate, () -> true, expectedFingerprint, true);
  }

  private IntensityNormalizerTask(final @NotNull MZmineProject project,
      final @NotNull FeatureList featureList, final @NotNull String suffix,
      final @NotNull String sourceRevision, final @NotNull IntensityNormalizationSummary summary,
      final @NotNull ParameterSet provenanceParameters,
      final @NotNull Class<? extends MZmineModule> provenanceModule,
      final @NotNull MemoryMapStorage storage, final @NotNull Instant moduleCallDate,
      final @NotNull BooleanSupplier current, final @Nullable String expectedFingerprint,
      final boolean replay) {
    super(storage, moduleCallDate);
    if (!(featureList instanceof ModularFeatureList modular)) {
      throw new IllegalArgumentException("Intensity normalization requires a modular feature list.");
    }
    this.project = project;
    originalFeatureList = modular;
    mainParameters = provenanceParameters.cloneParameterSet(true);
    this.suffix = suffix;
    handleOriginal = OriginalFeatureListOption.KEEP;
    totalFiles = modular.getNumberOfRawDataFiles();
    normalizationType = NormalizationType.NoNormalization;
    normalizationTypeModule = null;
    normalizationTypeModuleParameters = null;
    intraBatchCorrectionEnabled = false;
    interBatchCorrectionEnabled = false;
    byMetadataEnabled = false;
    byMetadataColumn = null;
    internalStandardNormalizer = NormalizationType.NoNormalization;
    internalStandardEnabled = false;
    internalStandardParams = null;
    batchIdColumn = null;
    batchIdEnabled = false;
    totalNormalizationSteps = 1;
    reviewedNormalization = new ReviewedNormalization(sourceRevision, summary.copy(), provenanceModule,
        current, expectedFingerprint, replay);
  }

  public double getFinishedPercentage() {
    return isFinished() ? 1d : totalFiles == 0 ? 0d
        : Math.min(1d, (double) processedFiles / totalFiles / totalNormalizationSteps);
  }

  public String getTaskDescription() {
    if (reviewedNormalization != null) return "Applying reviewed intensity normalization to "
        + originalFeatureList;
    return "Intensity normalization of " + originalFeatureList + " by " + normalizationType;
  }

  public void run() {
    runUnchecked();
  }

  private void runUnchecked() {

    if (reviewedNormalization != null) {
      try {
        runReviewedNormalization(reviewedNormalization);
      } catch (final java.util.concurrent.CancellationException canceled) {
        cancel();
      } catch (Throwable exception) {
        logger.log(Level.SEVERE, "Unhandled exception while applying reviewed normalization", exception);
        if (exception instanceof Exception checkedException) {
          error(exception.getMessage(), checkedException);
        } else {
          error(exception.getMessage());
        }
      }
      return;
    }

    try {
      runNativeNormalization();
    } catch (final java.util.concurrent.CancellationException canceled) {
      cancel();
    } catch (final RuntimeException exception) {
      error("Intensity normalization: " + exception.getMessage(), exception);
    }
  }

  private void runNativeNormalization() {
    if (isCanceled()) return;
    setStatus(TaskStatus.PROCESSING);
    final MetadataTable metadata = project.getProjectMetadata();
    final List<RawDataFile> sourceFiles = List.copyOf(originalFeatureList.getRawDataFiles());
    final String sourceRevision = NormalizationSourceRevision.capture(originalFeatureList, metadata,
        this::isCanceled);
    // decision: calculate from raw measurements and explicit preceding functions before clearing values.
    normalizedFeatureList = originalFeatureList;
    final IntensityNormalizationSearchableSummary summary = new IntensityNormalizationSearchableSummary(
        sourceFiles.size());
    final List<SamplesBatch> sampleBatches = splitSampleBatches(metadata);
    totalNormalizationSteps = 1 + ObjectUtils.countTrue(byMetadataEnabled, internalStandardEnabled,
        intraBatchCorrectionEnabled, intraBatchCorrectionEnabled && sampleBatches.size() > 1);
    if (byMetadataEnabled) {
      normalizeByMetadataColumn(summary, metadata);
      if (getStatus() != TaskStatus.PROCESSING) return;
    }
    if (internalStandardEnabled) {
      selectInternalStandards(summary);
      if (getStatus() != TaskStatus.PROCESSING) return;
      // All batches must finish preceding correction before a global QC target can be prepared.
      for (final SamplesBatch batch : sampleBatches) {
        applyInternalStandardization(batch, summary, metadata);
        processedFiles += batch.size();
        if (isCanceled()) return;
      }
    }
    if (intraBatchCorrectionEnabled) {
      normalizationTypeModule.createAllNormalizationFunctionsToSummary(summary, originalFeatureList,
          sampleBatches, metadata, mainParameters, normalizationTypeModuleParameters, this::isCanceled,
          count -> processedFiles += count);
    }
    if (isCanceled()) return;
    if (intraBatchCorrectionEnabled && sampleBatches.size() > 1) {
      normalizeSamplesInterBatches(sampleBatches, summary);
    }
    prepareNormalizedFeatureList();
    final List<PendingNormalizedAbundance> pending = handleOriginal.isProcessInPlace()
        ? calculatePendingNormalizedAbundances(summary.functions()) : null;
    if (!handleOriginal.isProcessInPlace()) {
      applyFunctionsToFeatures(normalizedFeatureList, summary.functions());
    }
    if (isCanceled() || !sourceIsCurrent(sourceFiles, sourceRevision, metadata)) return;
    for (final String message : summary.messages()) {
      logger.log(message.contains(IntensityNormalizationSummary.WARNING_MARKER) ? Level.WARNING
          : Level.INFO, message);
    }
    final ParameterSet appliedMethodParameters = mainParameters.cloneParameterSet(true);
    appliedMethodParameters.setParameter(IntensityNormalizerParameters.handleOriginal, handleOriginal);
    appliedMethodParameters.setParameter(IntensityNormalizerParameters.hiddenNormalizationSummary,
        summary.toSimpleSummary());
    appliedMethodParameters.setParameter(IntensityNormalizerParameters.hiddenNativeSourceFingerprint,
        sourceRevision);
    appliedMethodParameters.setParameter(IntensityNormalizerParameters.hiddenSourceRevision,
        sourceRevision);
    if (isCanceled()) return;
    if (pending != null) commitPendingNormalizedAbundances(pending);
    ensureNormalizationTypes();
    normalizedFeatureList.addDescriptionOfAppliedTask(new SimpleFeatureListAppliedMethod(
        "Intensity normalization by " + normalizationType + "; source revision " + sourceRevision
            + (batchIdEnabled ? " (batch-aware; column %s)".formatted(batchIdColumn) : "")
            + (byMetadataEnabled ? ", pre: metadata (%s)".formatted(byMetadataColumn) : "")
            + (internalStandardEnabled ? ", pre: IS" : ""), IntensityNormalizerModule.class,
        appliedMethodParameters, getModuleCallDate()));
    // Once an in-place commit starts, complete its matching provenance atomically.
    if (pending == null && isCanceled()) return;
    handleOriginal.reflectNewFeatureListToProject(suffix, project, normalizedFeatureList,
        originalFeatureList);
    setStatus(TaskStatus.FINISHED);
  }

  private boolean sourceIsCurrent(@NotNull final List<RawDataFile> files,
      @NotNull final String revision, @NotNull final MetadataTable metadata) {
    if (isCanceled()) return false;
    if (!files.equals(originalFeatureList.getRawDataFiles()) || !revision.equals(
        NormalizationSourceRevision.capture(originalFeatureList, metadata, this::isCanceled))) {
      error("Normalization source changed during calculation; the result was not published.");
      return false;
    }
    return true;
  }

  private void runReviewedNormalization(final @NotNull ReviewedNormalization reviewed) {
    if (isCanceled()) return;
    setStatus(TaskStatus.PROCESSING);
    if (!reviewed.current().getAsBoolean()) {
      error("The reviewed feature-table snapshot changed; normalization copy was not added.");
      return;
    }
    if (reviewed.replay() && !StringUtils.hasValue(reviewed.expectedFingerprint())) {
      error("Cannot replay reviewed normalization: its saved native source fingerprint is missing. "
          + "Recalculate normalization from the reviewed result.");
      return;
    }
    final MetadataTable metadata = project.getProjectMetadata();
    final String initialFingerprint = NormalizationSourceRevision.capture(originalFeatureList, metadata,
        this::isCanceled);
    final String expectedFingerprint = reviewed.replay() ? reviewed.expectedFingerprint()
        : initialFingerprint;
    if (!expectedFingerprint.equals(initialFingerprint)) {
      error("The normalization source no longer matches the saved reviewed result; no copy was added.");
      return;
    }
    prepareNormalizedFeatureList();
    final List<RawFileNormalizationFunction> reviewedFunctions = reviewed.summary().functions();
    if (reviewedFunctions.size() != originalFeatureList.getNumberOfRawDataFiles()) {
      throw new IllegalArgumentException("Exactly one reviewed normalization function is required for every source file.");
    }
    final Map<RawDataFile, RawFileNormalizationFunction> functions = new java.util.LinkedHashMap<>();
    for (final RawDataFile raw : originalFeatureList.getRawDataFiles()) {
      final List<RawFileNormalizationFunction> matching = reviewedFunctions.stream()
          .filter(function -> function.rawDataFilePlaceholder().matches(raw)).toList();
      if (matching.size() != 1) {
        throw new IllegalArgumentException(
            "Exactly one reviewed normalization function is required for every source file.");
      }
      functions.put(raw, matching.getFirst());
    }
    applyFunctionsToFeatures(normalizedFeatureList, functions);
    if (isCanceled()) return;
    if (!reviewed.current().getAsBoolean()) {
      error("The reviewed feature-table snapshot changed; normalization copy was not added.");
      return;
    }
    if (!expectedFingerprint.equals(NormalizationSourceRevision.capture(originalFeatureList, metadata,
        this::isCanceled))) {
      error("The normalization source changed during calculation; no copy was added.");
      return;
    }
    mainParameters.setParameter(IntensityNormalizerParameters.hiddenNativeSourceFingerprint,
        expectedFingerprint);
    mainParameters.setParameter(IntensityNormalizerParameters.hiddenSourceRevision,
        reviewed.sourceRevision());
    ensureNormalizationTypes();
    normalizedFeatureList.addDescriptionOfAppliedTask(new SimpleFeatureListAppliedMethod(
        "Reviewed frozen intensity normalization; source revision " + reviewed.sourceRevision(),
        reviewed.provenanceModule(), mainParameters, getModuleCallDate()));
    if (isCanceled()) return;
    project.addFeatureList(normalizedFeatureList);
    setStatus(TaskStatus.FINISHED);
  }

  private record ReviewedNormalization(@NotNull String sourceRevision,
                                       @NotNull IntensityNormalizationSummary summary,
                                       @NotNull Class<? extends MZmineModule> provenanceModule,
                                       @NotNull BooleanSupplier current,
                                       @Nullable String expectedFingerprint, boolean replay) {
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
      processedFiles += sampleBatches.stream().mapToInt(SamplesBatch::size).sum();
      return;
    }

    logger.fine("Normalizing samples inter batches n=" + sampleBatches.size());
    final Map<SamplesBatch, Double> referenceLevels = new java.util.LinkedHashMap<>();
    for (final SamplesBatch batch : sampleBatches) {
      referenceLevels.put(batch, batch.getMedianReferenceNormMetric());
    }
    final var scales = ScalarQcBatchNormalization.calculateInterBatchScales(referenceLevels, true);

    // apply those to all samples in the same batch
    for (SamplesBatch sampleBatch : sampleBatches) {
      final double normFactor = scales.scales().get(sampleBatch);
      // reuse same function for all files in this batch
      final FactorNormalizationFunction fileFunction = new FactorNormalizationFunction(normFactor);

      logger.fine("Normalizing %d samples in batch %s inter batches norm metric=%.4f".formatted(
          sampleBatch.size(), sampleBatch.getGroupMetadataValueStr(), normFactor));

      for (RawDataFile raw : sampleBatch.getRaws()) {
        if (isCanceled()) return;
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
    // Create new feature list and copy all rows up front.
    normalizedFeatureList = handleOriginal.isProcessInPlace() ? originalFeatureList
        : FeatureListUtils.createCopy(originalFeatureList, suffix, storage, true);

    final NormalizedAreaType normAreaType = DataTypes.get(NormalizedAreaType.class);
    final NormalizedHeightType normHeightType = DataTypes.get(NormalizedHeightType.class);

    if (handleOriginal.isProcessInPlace()) {
      return;
    }
    if (normalizedFeatureList.hasFeatureType(normAreaType) || normalizedFeatureList.hasFeatureType(normHeightType)) {
      // clear old normalization
      FeatureDataUtils.clearIntensityNormalization(normalizedFeatureList);
    } else {
      // add as feature types and row type will be added as binding
      normalizedFeatureList.addFeatureType(normHeightType);
      normalizedFeatureList.addFeatureType(normAreaType);
    }
  }

  private void ensureNormalizationTypes() {
    final NormalizedAreaType normAreaType = DataTypes.get(NormalizedAreaType.class);
    final NormalizedHeightType normHeightType = DataTypes.get(NormalizedHeightType.class);
    if (!normalizedFeatureList.hasFeatureType(normAreaType)) {
      normalizedFeatureList.addFeatureType(normAreaType);
    }
    if (!normalizedFeatureList.hasFeatureType(normHeightType)) {
      normalizedFeatureList.addFeatureType(normHeightType);
    }
  }

  private @NotNull List<PendingNormalizedAbundance> calculatePendingNormalizedAbundances(
      @NotNull final Map<RawDataFile, RawFileNormalizationFunction> fileToFunction) {
    final List<PendingNormalizedAbundance> pending = new java.util.ArrayList<>();
    for (final RawDataFile raw : originalFeatureList.getRawDataFiles()) {
      final RawFileNormalizationFunction rawFunction = fileToFunction.get(raw);
      final NormalizationFunction function = rawFunction == null ? null : rawFunction.function();
      for (final FeatureListRow row : originalFeatureList.getRows()) {
        if (isCanceled()) return pending;
        if (row.getFeature(raw) instanceof ModularFeature feature) {
          pending.add(new PendingNormalizedAbundance(feature,
              FeatureDataUtils.calculateNormalizedAbundances(feature, function, row)));
        }
      }
      processedFiles++;
    }
    return pending;
  }

  private void commitPendingNormalizedAbundances(
      @NotNull final List<PendingNormalizedAbundance> pending) {
    pending.forEach(value -> {
      value.feature().set(NormalizedAreaType.class, value.abundances().area());
      value.feature().set(NormalizedHeightType.class, value.abundances().height());
    });
  }

  private record PendingNormalizedAbundance(@NotNull ModularFeature feature,
      @NotNull FeatureDataUtils.NormalizedAbundances abundances) {
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
        if (isCanceled()) return;
        final Feature feature = row.getFeature(raw);
        if (!(feature instanceof ModularFeature mfeature)) {
          continue;
        }

        FeatureDataUtils.normalizeAbundances(mfeature, fn, row);
      }

      // each function is one raw data files finished
      processedFiles++;
    }
  }

}
