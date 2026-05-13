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

package io.github.mzmine.modules.dataprocessing.id_spectral_library_analog_search;

import ai.djl.MalformedModelException;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.translate.TranslateException;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.AnalogSpectralLibraryMatchesType;
import io.github.mzmine.datamodel.features.types.numbers.scores.MLModelId;
import io.github.mzmine.datamodel.features.types.numbers.scores.MLScore;
import io.github.mzmine.datamodel.features.types.numbers.scores.MLScoreType;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.SpectraMergeSelectParameter;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.SignalFiltersParameters;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.SpectralNetworkingOptions;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.SpectralSignalFilter;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.cosine_no_precursor.NoPrecursorCosineSpectralNetworkingParameters;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.dreams.DreaMSNetworkingParameters;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.modified_cosine.ModifiedCosineSpectralNetworkingParameters;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.ms2deepscore.MS2DeepscoreNetworkingParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.maths.similarity.Similarity;
import io.github.mzmine.util.scans.FragmentScanSelection;
import io.github.mzmine.util.scans.ScanAlignment;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import io.github.mzmine.util.scans.similarity.Weights;
import io.github.mzmine.util.scans.similarity.impl.DreaMS.DreaMSModel;
import io.github.mzmine.util.scans.similarity.impl.ms2deepscore.EmbeddingBasedSimilarity;
import io.github.mzmine.util.scans.similarity.impl.ms2deepscore.MS2DeepscoreModel;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Searches every row of one feature list against a spectral library using the selected
 * networking-style similarity algorithm with a wide precursor m/z window. Results are stored as
 * {@link SpectralDBAnnotation}s under {@link AnalogSpectralLibraryMatchesType}. For ML algorithms
 * (MS2Deepscore, DREAMS) the embedding-based score gates the match; a fallback modified-cosine
 * {@link SpectralSimilarity} is computed for visualization only when the ML score passes the
 * threshold.
 */
public class AnalogSpectralLibrarySearchTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(
      AnalogSpectralLibrarySearchTask.class.getName());

  // similarity-function names embedded in SpectralSimilarity for downstream display
  private static final String FN_MODIFIED_COSINE = "Modified cosine";
  private static final String FN_COSINE_NO_PRECURSOR = "Cosine (no precursor)";
  private static final String FN_FALLBACK_COSINE = "Modified cosine (fallback for ML match)";

  // MS2Deepscore embeddings are predicted in fixed-size batches so the progress bar advances
  // while spectra are being forwarded through the model (instead of stalling on one big call).
  private static final int MS2DEEPSCORE_BATCH_SIZE = 100;

  // m/z tolerance used to compute the cosine fallback shown in the UI for ML matches.
  private static final MZTolerance ML_FALLBACK_MZ_TOL = MZTolerance.FIFTEEN_PPM_OR_FIVE_MDA;

  private final MZmineProject project;
  private final ModularFeatureList featureList;
  private final SpectralNetworkingOptions algorithm;
  private final ParameterSet algoParams;
  private final double maxMzDelta;

  // cosine variants
  @Nullable
  private final MZTolerance mzTolerance;
  private final int minMatch;
  private final double minCosine;
  @Nullable
  private final SpectralSignalFilter signalFilter;
  @Nullable
  private final FragmentScanSelection scanSelect;
  // MODIFIED_COSINE removes precursor signals; COSINE_NO_PRECURSOR keeps them
  private final boolean removePrecursor;

  // ML variants
  @Nullable
  private final File modelFile;
  @Nullable
  private final File modelSettingsFile;
  private final double mlMinScore;
  private final int mlMinSignals;
  // DREAMS-only — number of spectra forwarded through the model in a single pass. Without batching
  // a large library OOMs because Fourier features balloon to (N * num_peaks * num_fourier).
  private final int dreamsBatchSize;

  private String description;

  public AnalogSpectralLibrarySearchTask(@NotNull final MZmineProject project,
      @NotNull final ParameterSet mainParameters, @NotNull final ModularFeatureList featureList,
      @NotNull final Instant moduleCallDate,
      @NotNull final Class<? extends MZmineModule> moduleClass) {
    super(featureList.getMemoryMapStorage(), moduleCallDate, mainParameters, moduleClass);
    this.project = project;
    this.featureList = featureList;
    this.algorithm = mainParameters.getValue(AnalogSpectralLibrarySearchParameters.algorithm);
    this.algoParams = mainParameters.getEmbeddedParameterValue(
        AnalogSpectralLibrarySearchParameters.algorithm);
    this.description = "Analog spectral library search";

    switch (algorithm) {
      case MODIFIED_COSINE -> {
        mzTolerance = algoParams.getValue(ModifiedCosineSpectralNetworkingParameters.MZ_TOLERANCE);
        minMatch = algoParams.getValue(ModifiedCosineSpectralNetworkingParameters.MIN_MATCH);
        maxMzDelta = algoParams.getEmbeddedParameterValueIfSelectedOrElse(
            ModifiedCosineSpectralNetworkingParameters.MAX_MZ_DELTA, 1E4);
        minCosine = algoParams.getValue(
            ModifiedCosineSpectralNetworkingParameters.MIN_COSINE_SIMILARITY);
        final SignalFiltersParameters sf = algoParams.getValue(
            ModifiedCosineSpectralNetworkingParameters.signalFilters);
        signalFilter = sf.createFilter();
        final SpectraMergeSelectParameter mergeParam = algoParams.getParameter(
            ModifiedCosineSpectralNetworkingParameters.spectraMergeSelect);
        scanSelect = mergeParam.createFragmentScanSelection(getMemoryMapStorage());
        removePrecursor = true;
        modelFile = null;
        modelSettingsFile = null;
        mlMinScore = 0d;
        mlMinSignals = 0;
        dreamsBatchSize = 0;
      }
      case COSINE_NO_PRECURSOR -> {
        mzTolerance = algoParams.getValue(
            NoPrecursorCosineSpectralNetworkingParameters.MZ_TOLERANCE);
        minMatch = algoParams.getValue(NoPrecursorCosineSpectralNetworkingParameters.MIN_MATCH);
        minCosine = algoParams.getValue(
            NoPrecursorCosineSpectralNetworkingParameters.MIN_COSINE_SIMILARITY);
        signalFilter = algoParams.getValue(
            NoPrecursorCosineSpectralNetworkingParameters.signalFilters).createFilter();
        // no scan-selection sub-parameter — uses most intense fragment scan per row
        scanSelect = null;
        removePrecursor = false;
        modelFile = null;
        modelSettingsFile = null;
        mlMinScore = 0d;
        mlMinSignals = 0;
        dreamsBatchSize = 0;
        maxMzDelta = 1E4;
      }
      case MS2_DEEPSCORE -> {
        mzTolerance = null;
        minMatch = 0;
        minCosine = 0d;
        signalFilter = null;
        final SpectraMergeSelectParameter mergeParam = algoParams.getParameter(
            MS2DeepscoreNetworkingParameters.spectraMergeSelect);
        scanSelect = mergeParam.createFragmentScanSelection(getMemoryMapStorage());
        removePrecursor = false;
        modelFile = algoParams.getValue(MS2DeepscoreNetworkingParameters.ms2deepscoreModelFile);
        modelSettingsFile = MS2DeepscoreNetworkingParameters.findModelSettingsFile(modelFile);
        mlMinScore = algoParams.getValue(MS2DeepscoreNetworkingParameters.minScore);
        mlMinSignals = algoParams.getValue(MS2DeepscoreNetworkingParameters.minSignals);
        dreamsBatchSize = 0;
        maxMzDelta = 1E4;
      }
      case DREAMS -> {
        mzTolerance = null;
        minMatch = 0;
        minCosine = 0d;
        signalFilter = null;
        final SpectraMergeSelectParameter mergeParam = algoParams.getParameter(
            DreaMSNetworkingParameters.spectraMergeSelect);
        scanSelect = mergeParam.createFragmentScanSelection(getMemoryMapStorage());
        removePrecursor = false;
        modelFile = algoParams.getValue(DreaMSNetworkingParameters.dreaMSModelFile);
        modelSettingsFile = DreaMSNetworkingParameters.findModelSettingsFile(modelFile);
        mlMinScore = algoParams.getValue(DreaMSNetworkingParameters.minScore);
        mlMinSignals = 0;
        dreamsBatchSize = algoParams.getValue(DreaMSNetworkingParameters.batchSize);
        maxMzDelta = 1E4;
      }
      default -> throw new AssertionError("Unhandled analog search algorithm: " + algorithm);
    }

    totalItems = featureList.getNumberOfRows();
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(featureList);
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  protected void process() {
    description = "Loading spectral library entries";
    final List<SpectralLibraryEntry> sortedEntries = loadAndSortLibraryEntries();
    if (sortedEntries.isEmpty()) {
      logger.info("Analog search: no library entries available; nothing to do.");
      return;
    }

    featureList.addRowType(DataTypes.get(AnalogSpectralLibraryMatchesType.class));

    description = "Analog search (" + algorithm.getStableId() + ")";
    try {
      switch (algorithm) {
        case MODIFIED_COSINE, COSINE_NO_PRECURSOR -> processCosine(sortedEntries);
        case MS2_DEEPSCORE, DREAMS -> processMlAnalog(sortedEntries);
      }
    } catch (MissingMassListException e) {
      error("No mass list found in scan: " + e.getMessage(), e);
      return;
    }

    featureList.addDescriptionOfAppliedTask(new SimpleFeatureListAppliedMethod(
        "Analog spectral library search (" + algorithm.getStableId() + ")",
        AnalogSpectralLibrarySearchModule.class, parameters, getModuleCallDate()));
  }

  // ----- library loading -----

  private @NotNull List<SpectralLibraryEntry> loadAndSortLibraryEntries() {
    final List<SpectralLibraryEntry> entries = parameters.getValue(
            AnalogSpectralLibrarySearchParameters.libraries)
        .getMatchingLibraryEntriesAndCheckAvailability();
    return entries.stream().filter(e -> e.getPrecursorMZ() != null)
        .sorted(Comparator.comparing(SpectralLibraryEntry::getPrecursorMZ)).toList();
  }

  private @NotNull List<SpectralLibraryEntry> candidatesFor(final double rowPrecursorMz,
      final List<SpectralLibraryEntry> sortedEntries) {
    return BinarySearch.indexRange(
        Range.closed(rowPrecursorMz - maxMzDelta, rowPrecursorMz + maxMzDelta), sortedEntries,
        SpectralLibraryEntry::getPrecursorMZ).sublist(sortedEntries);
  }

  // ----- cosine path -----

  private void processCosine(final List<SpectralLibraryEntry> sortedEntries)
      throws MissingMassListException {
    final String functionName =
        algorithm == SpectralNetworkingOptions.MODIFIED_COSINE ? FN_MODIFIED_COSINE
            : FN_COSINE_NO_PRECURSOR;

    int sum = featureList.getRows().parallelStream().mapToInt(row -> {
      if (isCanceled()) {
        return 0;
      }
      try {
        processCosineRow(row, sortedEntries, functionName);
      } finally {
        finishedItems.incrementAndGet();
      }
      return 1;
    }).sum();
  }

  private void processCosineRow(final FeatureListRow row,
      final List<SpectralLibraryEntry> sortedEntries, final String functionName)
      throws MissingMassListException {
    final Double rowPrecursor = row.getAverageMZ();
    if (rowPrecursor == null) {
      return;
    }
    final List<Scan> queryScans = pickQueryScans(row);
    if (queryScans.isEmpty()) {
      return;
    }
    final List<SpectralLibraryEntry> candidates = candidatesFor(rowPrecursor, sortedEntries);
    if (candidates.isEmpty()) {
      return;
    }

    // sort & filter once per query scan
    final List<DataPoint[]> queryDpsPerScan = new ArrayList<>(queryScans.size());
    for (final Scan scan : queryScans) {
      final DataPoint[] dps = signalFilter.applyFilterAndSortByIntensity(scan,
          removePrecursor ? rowPrecursor : null, minMatch);
      queryDpsPerScan.add(dps);
    }

    final List<SpectralDBAnnotation> matches = new ArrayList<>();

    for (final SpectralLibraryEntry entry : candidates) {
      if (isCanceled()) {
        return;
      }
      final DataPoint[] entryDps = entry.getDataPoints();
      if (entryDps == null || entryDps.length < minMatch) {
        continue;
      }
      final DataPoint[] sortedEntryDps = signalFilter.applyFilterAndSortByIntensity(entryDps,
          removePrecursor ? entry.getPrecursorMZ() : null, minMatch);
      if (sortedEntryDps == null) {
        continue;
      }

      SpectralSimilarity best = null;
      Scan bestScan = null;
      for (int i = 0; i < queryScans.size(); i++) {
        final DataPoint[] queryDps = queryDpsPerScan.get(i);
        if (queryDps == null) {
          continue;
        }
        final SpectralSimilarity sim;
        if (algorithm == SpectralNetworkingOptions.MODIFIED_COSINE) {
          sim = computeModifiedCosine(queryDps, sortedEntryDps, mzTolerance, minMatch, rowPrecursor,
              entry.getPrecursorMZ(), functionName);
        } else {
          sim = computeModifiedCosine(queryDps, sortedEntryDps, mzTolerance, minMatch, -1d, -1d,
              functionName);
        }
        if (sim != null && (best == null || sim.getScore() > best.getScore())) {
          best = sim;
          bestScan = queryScans.get(i);
        }
      }

      if (best != null && best.getScore() >= minCosine && best.getOverlap() >= minMatch) {
        matches.add(
            new SpectralDBAnnotation(entry, best, bestScan, null, rowPrecursor, row.getAverageRT(),
                null, AnalogSpectralLibraryMatchesType.class));
      }
    }

    if (!matches.isEmpty()) {
      matches.sort(
          Comparator.comparingDouble((SpectralDBAnnotation a) -> a.getSimilarity().getScore())
              .reversed());
      ((ModularFeatureListRow) row).addAnalogSpectralLibraryMatches(matches);
    }
  }

  private @NotNull List<Scan> pickQueryScans(final FeatureListRow row) {
    if (scanSelect != null) {
      return scanSelect.getAllFragmentSpectra(row);
    }
    final Scan best = row.getMostIntenseFragmentScan();
    return best == null ? List.of() : List.of(best);
  }

  // ----- ML path (shared by MS2Deepscore and DREAMS) -----

  /**
   * Runs the ML analog search: embeds query scans up front, streams the library in batches,
   * dot-products each batch against the queries, appends per-row matches, then drops the batch's
   * embeddings. RAM stays bounded by batch size.
   * <p>
   * ML embedding similarity is precursor-mass-agnostic — every library entry can match; the analog
   * precursor delta is a cosine-only parameter and is not applied here.
   */
  private void processMlAnalog(final List<SpectralLibraryEntry> sortedEntries) {
    final List<FeatureListRow> queryRows = new ArrayList<>();
    final List<Scan> queryScans = new ArrayList<>();
    collectMlQueryScans(queryRows, queryScans);
    if (queryRows.isEmpty()) {
      return;
    }

    final int batchSize = mlBatchSize();
    final String label = mlAlgorithmLabel();

    // progress is dominated by spectra forwarded through the model.
    // totalItems = (queries + library); finishedItems advances per embedding batch.
    totalItems = (long) queryScans.size() + (long) sortedEntries.size();
    finishedItems.set(0);

    description = "Loading " + label + " model";
    try (var model = openMlModel()) {
      // 1) embed all query scans once (typically << library size). Held in RAM.
      description = "Predicting " + label + " query embeddings";
      final NDArray queryEmb;
      try {
        queryEmb = predictEmbeddingsBatched(model, queryScans, batchSize, label);
      } catch (TranslateException e) {
        error("Failed to predict " + label + " query embeddings: " + e.getMessage(), e);
        return;
      }

      // 2) iterate library in batches; for each batch, predict embeddings, dot-product with
      //    queries, and immediately accumulate per-row analog matches. Library embeddings for
      //    the batch are dropped after each iteration so RAM stays bounded.
      description = label + " analog matching";
      try {
        matchEmbeddingLibraryStreaming(model, queryRows, queryScans, sortedEntries, queryEmb,
            batchSize, label);
      } catch (TranslateException e) {
        error("Failed to predict " + label + " library embeddings: " + e.getMessage(), e);
        return;
      } finally {
        queryEmb.close();
      }

      description = "Sorting " + label + " analog matches";
      sortRowAnalogMatchesByMlScore(queryRows);
    } catch (ModelNotFoundException | MalformedModelException | IOException e) {
      error("Could not load " + label + " model: " + e.getMessage(), e);
    }
  }

  private @NotNull EmbeddingBasedSimilarity openMlModel()
      throws ModelNotFoundException, MalformedModelException, IOException {
    return switch (algorithm) {
      case MS2_DEEPSCORE -> new MS2DeepscoreModel(modelFile, modelSettingsFile);
      case DREAMS -> new DreaMSModel(modelFile, modelSettingsFile);
      case MODIFIED_COSINE, COSINE_NO_PRECURSOR ->
          throw new AssertionError("openMlModel called for non-ML algorithm: " + algorithm);
    };
  }

  private int mlBatchSize() {
    return switch (algorithm) {
      case MS2_DEEPSCORE -> MS2DEEPSCORE_BATCH_SIZE;
      case DREAMS -> dreamsBatchSize;
      case MODIFIED_COSINE, COSINE_NO_PRECURSOR ->
          throw new AssertionError("mlBatchSize called for non-ML algorithm: " + algorithm);
    };
  }

  private @NotNull String mlAlgorithmLabel() {
    return switch (algorithm) {
      case MS2_DEEPSCORE -> "MS2Deepscore";
      case DREAMS -> "DREAMS";
      case MODIFIED_COSINE, COSINE_NO_PRECURSOR ->
          throw new AssertionError("mlAlgorithmLabel called for non-ML algorithm: " + algorithm);
    };
  }

  /**
   * Streams library batches through the model: predict → dot-product with query embeddings → score
   * &amp; write matches for each query row directly to the row → drop batch embeddings. No
   * cross-batch buffering.
   */
  private void matchEmbeddingLibraryStreaming(final EmbeddingBasedSimilarity model,
      final List<FeatureListRow> queryRows, final List<Scan> queryScans,
      final List<SpectralLibraryEntry> sortedEntries, final NDArray queryEmb, final int batchSize,
      final String label) throws TranslateException {
    final int total = sortedEntries.size();

    // cache of intensity-sorted query data points (only built once we know the row has at least
    // one ML hit, so we skip the sort cost for rows with no analogs)
    final DataPoint[][] queryDpsCache = new DataPoint[queryScans.size()][];

    for (int batchStart = 0; batchStart < total; batchStart += batchSize) {
      if (isCanceled()) {
        return;
      }
      final int batchEnd = Math.min(batchStart + batchSize, total);
      final List<SpectralLibraryEntry> batch = sortedEntries.subList(batchStart, batchEnd);

      // forward this batch through the model
      final NDArray libBatchEmb = model.predictEmbedding(batch);
      final float[][] simBatch;
      try {
        // simBatch shape: [queryRows.size(), batch.size()]
        simBatch = EmbeddingBasedSimilarity.dotProduct(queryEmb, libBatchEmb);
      } finally {
        libBatchEmb.close();
      }
      // batch forward pass complete → advance progress
      finishedItems.addAndGet(batch.size());

      description = label + " analog matching: %d / %d".formatted(batchStart, total);

      // build matches for THIS BATCH ONLY and append directly to each row — no cross-batch buffer
      appendBatchMatchesFromSim(simBatch, queryRows, queryScans, batch, queryDpsCache);
    }
  }

  /**
   * Forwards spectra through the model in fixed-size batches. Returns the concatenated embeddings
   * as a single NDArray of shape {@code [N, embDim]}. Used for the (typically small) query set; do
   * NOT use for the library. Each batch advances {@code finishedItems} so the progress bar moves
   * while query embeddings are being computed.
   */
  private NDArray predictEmbeddingsBatched(final EmbeddingBasedSimilarity model,
      final List<? extends MassSpectrum> spectra, final int batchSize, final String label)
      throws TranslateException {
    final NDList batches = new NDList();
    for (int start = 0; start < spectra.size(); start += batchSize) {
      if (isCanceled()) {
        break;
      }
      final int end = Math.min(start + batchSize, spectra.size());
      batches.add(model.predictEmbedding(spectra.subList(start, end)));
      finishedItems.addAndGet(end - start);
      description =
          "Computing " + label + " query embeddings: %d / %d".formatted(end, spectra.size());
    }
    return NDArrays.concat(batches);
  }

  private void collectMlQueryScans(final List<FeatureListRow> outRows, final List<Scan> outScans) {
    for (final FeatureListRow row : featureList.getRows()) {
      final Scan scan = pickSingleQueryScanForMl(row);
      if (scan != null) {
        outRows.add(row);
        outScans.add(scan);
      }
    }
  }

  /**
   * Builds analog-search annotations from one slice (a batch) of an ML similarity matrix and
   * appends them directly to each query row — no cross-batch buffering.
   * <p>
   * {@code simBatch} shape: {@code [queryRows.size(), entryBatch.size()]}. {@code queryDpsCache} is
   * shared across calls so each query row's intensity-sorted data points are computed at most
   * once.
   * <p>
   * No precursor-mass filter: ML embedding similarity is precursor-mass-agnostic, every library
   * entry is a valid candidate. The analog precursor delta only applies to cosine.
   */
  private void appendBatchMatchesFromSim(final float[][] simBatch,
      final List<FeatureListRow> queryRows, final List<Scan> queryScans,
      final List<SpectralLibraryEntry> entryBatch, final DataPoint[][] queryDpsCache) {
    for (int q = 0; q < queryRows.size(); q++) {
      if (isCanceled()) {
        return;
      }
      final FeatureListRow row = queryRows.get(q);
      final Double rowPrecursor = row.getAverageMZ();
      if (rowPrecursor == null) {
        continue;
      }
      List<SpectralDBAnnotation> rowBatchMatches = null;
      for (int c = 0; c < entryBatch.size(); c++) {
        final float mlScore = simBatch[q][c];
        if (mlScore < mlMinScore) {
          continue;
        }
        // lazy — only build the sorted-copy when at least one candidate passes the threshold
        if (queryDpsCache[q] == null) {
          queryDpsCache[q] = sortAndCopyScan(queryScans.get(q));
        }
        final SpectralLibraryEntry entry = entryBatch.get(c);
        final SpectralSimilarity cosineForViz = computeFallbackCosine(queryDpsCache[q], entry,
            ML_FALLBACK_MZ_TOL, rowPrecursor);
        if (cosineForViz == null) {
          continue;
        }
        final SpectralDBAnnotation annotation = new SpectralDBAnnotation(entry, cosineForViz,
            queryScans.get(q), null, rowPrecursor, row.getAverageRT(), null,
            AnalogSpectralLibraryMatchesType.class);
        applyMlScore(annotation, mlScore);
        if (rowBatchMatches == null) {
          rowBatchMatches = new ArrayList<>();
        }
        rowBatchMatches.add(annotation);
      }
      if (rowBatchMatches != null) {
        ((ModularFeatureListRow) row).addAnalogSpectralLibraryMatches(rowBatchMatches);
      }
    }
  }

  /**
   * Stores the ML similarity score on the annotation as a single {@link MLScore} record carrying
   * both the score and the model identifier. Called only from the ML paths.
   */
  private void applyMlScore(final SpectralDBAnnotation annotation, final float score) {
    final MLModelId model = switch (algorithm) {
      case MS2_DEEPSCORE -> MLModelId.MS2_DEEPSCORE_2_0;
      case DREAMS -> MLModelId.DREAMS;
      case MODIFIED_COSINE, COSINE_NO_PRECURSOR ->
          throw new AssertionError("applyMlScore called for non-ML algorithm: " + algorithm);
    };
    annotation.set(MLScoreType.class, new MLScore(score, model));
  }

  /**
   * Sorts every query row's analog matches by the active ML score (descending) so the best matches
   * surface first downstream. Per-row work — no cross-row buffer.
   */
  private void sortRowAnalogMatchesByMlScore(final List<FeatureListRow> queryRows) {
    final Comparator<SpectralDBAnnotation> byMlScoreDesc = mlScoreComparatorDesc();
    for (final FeatureListRow row : queryRows) {
      if (isCanceled()) {
        return;
      }
      final ModularFeatureListRow mrow = (ModularFeatureListRow) row;
      final List<SpectralDBAnnotation> existing = mrow.getAnalogSpectralLibraryMatches();
      if (existing.size() > 1) {
        final List<SpectralDBAnnotation> sorted = new ArrayList<>(existing);
        sorted.sort(byMlScoreDesc);
        mrow.setAnalogSpectralLibraryMatch(sorted);
      }
    }
  }

  private Comparator<SpectralDBAnnotation> mlScoreComparatorDesc() {
    // single comparator regardless of model — MLScoreType holds both fields
    return Comparator.comparingDouble((SpectralDBAnnotation a) -> {
      final MLScore s = a.get(MLScoreType.class);
      return s == null ? 0d : s.score();
    }).reversed();
  }

  private @Nullable Scan pickSingleQueryScanForMl(final FeatureListRow row) {
    if (scanSelect == null) {
      return null;
    }
    final List<Scan> all = scanSelect.getAllFragmentSpectra(row);
    if (all.isEmpty()) {
      return null;
    }
    final Scan scan = all.getFirst();
    if (scan.getPrecursorMz() == null) {
      return null;
    }
    if (scan.getMassList() == null) {
      throw new MissingMassListException(scan);
    }
    if (mlMinSignals > 0 && scan.getMassList().getNumberOfDataPoints() < mlMinSignals) {
      return null;
    }
    return scan;
  }

  // ----- modified-cosine helpers (build util.scans.similarity.SpectralSimilarity directly) -----

  /**
   * Aligns query vs library data points (modification-aware when both precursors are provided),
   * computes the weighted cosine, and packages the result with aligned data so the UI mirror plot
   * can render. Returns {@code null} if {@code overlap < minOverlap}.
   */
  private static @Nullable SpectralSimilarity computeModifiedCosine(final DataPoint[] querySorted,
      final DataPoint[] librarySorted, final MZTolerance mzTol, final int minOverlap,
      final double rowPrecursorMz, final Double libraryPrecursorMz, final String functionName) {
    final List<DataPoint[]> aligned;
    if (rowPrecursorMz > 0 && libraryPrecursorMz != null && libraryPrecursorMz > 0) {
      aligned = ScanAlignment.alignOfSortedModAware(mzTol, librarySorted, querySorted,
          libraryPrecursorMz, rowPrecursorMz);
    } else {
      aligned = ScanAlignment.alignOfSorted(mzTol, librarySorted, querySorted);
    }

    int overlap = 0;
    for (final DataPoint[] pair : aligned) {
      if (pair[0] != null && pair[1] != null) {
        overlap++;
      }
    }
    if (overlap < minOverlap) {
      return null;
    }

    final double[][] matrix = ScanAlignment.toIntensityMatrixWeighted(aligned,
        Weights.SQRT.getIntensity(), Weights.SQRT.getMz());
    final double cosine = Similarity.COSINE.calc(matrix);

    // need to clone the data points as the similarity calculation sorts by mz
    return new SpectralSimilarity(functionName, cosine, overlap, librarySorted.clone(),
        querySorted.clone(), aligned);
  }

  /**
   * Cosine fallback for ML matches: same as {@link #computeModifiedCosine} but with
   * {@code minOverlap = 1} so a {@link SpectralSimilarity} is produced whenever at least one peak
   * aligns within {@code mzTol}. Returns {@code null} when the library entry has no data points or
   * when zero peaks align (the caller skips the row in that case).
   */
  private static @Nullable SpectralSimilarity computeFallbackCosine(
      final DataPoint[] queryDpsSorted, final SpectralLibraryEntry entry, final MZTolerance mzTol,
      final double rowPrecursor) {
    final DataPoint[] entryDps = entry.getDataPoints();
    if (entryDps == null || entryDps.length == 0) {
      return null;
    }
    final DataPoint[] entryDpsSorted = entryDps.clone();
    Arrays.sort(entryDpsSorted, DataPointSorter.DEFAULT_INTENSITY);
    final Double libPrecursor = entry.getPrecursorMZ();
    return computeModifiedCosine(queryDpsSorted.clone(), entryDpsSorted, mzTol, 1, rowPrecursor,
        libPrecursor, FN_FALLBACK_COSINE);
  }

  private static DataPoint[] sortAndCopyScan(final Scan scan) {
    if (scan.getMassList() == null) {
      throw new MissingMassListException(scan);
    }
    final DataPoint[] dps = scan.getMassList().getDataPoints();
    final DataPoint[] copy = dps.clone();
    Arrays.sort(copy, DataPointSorter.DEFAULT_INTENSITY);
    return copy;
  }

}
