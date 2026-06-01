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
import com.google.common.collect.Lists;
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
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.SpectralNetworkingOptions;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.dreams.DreaMSNetworkingParameters;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.ms2deepscore.MS2DeepscoreNetworkingParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.scans.FragmentScanSelection;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import io.github.mzmine.util.scans.similarity.impl.DreaMS.DreaMSModel;
import io.github.mzmine.util.scans.similarity.impl.ms2deepscore.EmbeddingBasedSimilarity;
import io.github.mzmine.util.scans.similarity.impl.ms2deepscore.MS2DeepscoreModel;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Embedding-based analog-search task (MS2Deepscore / DREAMS). Owns ALL selected feature lists and
 * runs them sequentially inside a single task so the underlying ML model — which manages its own
 * CPU/GPU thread pool — is never multiplied across the task scheduler.
 * <p>
 * Library embeddings are computed once per run and cached on each {@link SpectralLibraryEntry} via
 * the model-specific {@link DBEntryField}; subsequent runs with the same model load the cached
 * {@code float[]} vectors instead of re-predicting. During a single task lifetime the batched
 * library NDArrays are retained in native memory so per-feature-list matching is a pure matrix
 * multiply with no float[]↔NDArray round-trip.
 */
public class AnalogSpectralLibrarySearchMlTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(
      AnalogSpectralLibrarySearchMlTask.class.getName());

  // MS2Deepscore embeddings are predicted in fixed-size batches so the progress bar advances
  // while spectra are being forwarded through the model (instead of stalling on one big call).
  private static final int MS2DEEPSCORE_BATCH_SIZE = 100;

  // m/z tolerance used to compute the cosine fallback shown in the UI for ML matches.
  private static final MZTolerance ML_FALLBACK_MZ_TOL = MZTolerance.FIFTEEN_PPM_OR_FIVE_MDA;

  private final MZmineProject project;
  private final ModularFeatureList[] featureLists;
  private final SpectralNetworkingOptions algorithm;
  private final MLModelId modelId;
  private final File modelFile;
  private final File modelSettingsFile;
  private final FragmentScanSelection scanSelect;
  private final double mlMinScore;
  private final int mlMinSignals;
  // DREAMS-only — number of spectra forwarded through the model in a single pass. Without batching
  // a large library OOMs because Fourier features balloon to (N * num_peaks * num_fourier).
  private final int batchSize;

  private String description;

  public AnalogSpectralLibrarySearchMlTask(@NotNull final MZmineProject project,
      @NotNull final ParameterSet mainParameters, @NotNull final ModularFeatureList[] featureLists,
      @NotNull final Instant moduleCallDate,
      @NotNull final Class<? extends MZmineModule> moduleClass) {
    super(featureLists.length > 0 ? featureLists[0].getMemoryMapStorage() : null, moduleCallDate,
        mainParameters, moduleClass);
    this.project = project;
    this.featureLists = featureLists;
    this.algorithm = mainParameters.getValue(AnalogSpectralLibrarySearchParameters.algorithm);
    this.description = "Analog spectral library search";

    final ParameterSet algoParams = mainParameters.getEmbeddedParameterValue(
        AnalogSpectralLibrarySearchParameters.algorithm);
    switch (algorithm) {
      case MS2_DEEPSCORE -> {
        modelId = MLModelId.MS2_DEEPSCORE_2_0;
        final SpectraMergeSelectParameter mergeParam = algoParams.getParameter(
            MS2DeepscoreNetworkingParameters.spectraMergeSelect);
        scanSelect = mergeParam.createFragmentScanSelection(getMemoryMapStorage());
        modelFile = algoParams.getValue(MS2DeepscoreNetworkingParameters.ms2deepscoreModelFile);
        modelSettingsFile = MS2DeepscoreNetworkingParameters.findModelSettingsFile(modelFile);
        mlMinScore = algoParams.getValue(MS2DeepscoreNetworkingParameters.minScore);
        mlMinSignals = algoParams.getValue(MS2DeepscoreNetworkingParameters.minSignals);
        batchSize = MS2DEEPSCORE_BATCH_SIZE;
      }
      /*case DREAMS -> {
        modelId = MLModelId.DREAMS_1_0;
        final SpectraMergeSelectParameter mergeParam = algoParams.getParameter(
            DreaMSNetworkingParameters.spectraMergeSelect);
        scanSelect = mergeParam.createFragmentScanSelection(getMemoryMapStorage());
        modelFile = algoParams.getValue(DreaMSNetworkingParameters.dreaMSModelFile);
        modelSettingsFile = DreaMSNetworkingParameters.findModelSettingsFile(modelFile);
        mlMinScore = algoParams.getValue(DreaMSNetworkingParameters.minScore);
        mlMinSignals = 0;
        batchSize = algoParams.getValue(DreaMSNetworkingParameters.batchSize);
      }*/
      default -> throw new AssertionError(
          "AnalogSpectralLibrarySearchMlTask called with non-ML algorithm: " + algorithm);
    }
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(featureLists);
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  protected void process() {
    description = "Loading spectral library entries";
    final List<SpectralLibraryEntry> sortedLibrary = loadAndSortLibraryEntries();
    if (sortedLibrary.isEmpty()) {
      logger.info("Analog ML search: no library entries available; nothing to do.");
      return;
    }
    final List<List<SpectralLibraryEntry>> libraryBatches = Lists.partition(sortedLibrary,
        batchSize);

    // Progress: 1 unit per library entry during phase 1 + 1 unit per query scan during phase 2.
    // queryScans are counted upfront from each feature list. ML fallback cosine work is cheap
    // compared to embeddings so we don't budget for it.
    final long queryScansTotal = Arrays.stream(featureLists)
        .mapToLong(fl -> fl.getNumberOfRows()) // upper bound — collectMlQueryScans may filter
        .sum();
    totalItems = (long) sortedLibrary.size() * 2 + queryScansTotal;
    finishedItems.set(0);

    for (final ModularFeatureList flist : featureLists) {
      flist.addRowType(DataTypes.get(AnalogSpectralLibraryMatchesType.class));
    }

    description = "Loading " + algorithm.toString() + " model";

    final NDArray[] libBatchEmbeddings = new NDArray[libraryBatches.size()];
    final BitSet freshlyPredicted = new BitSet(libraryBatches.size());

    try (var model = openMlModel()) {

      // Phase 1: build per-batch NDArrays. Predict whenever ANY entry lacks a cached vector;
      // otherwise rebuild from cache via the model's NDManager so the batch shares the model's
      // native-memory lifecycle (and gets cleaned up by model.close()).
      try {
        precomputeLibraryEmbeddings(model, libraryBatches, libBatchEmbeddings, freshlyPredicted,
            algorithm);
      } catch (TranslateException e) {
        error(
            "Failed to predict " + algorithm.toString() + " library embeddings: " + e.getMessage(),
            e);
        return;
      }
      if (isCanceled()) {
        return;
      }

      logPrecomputeSummary(libraryBatches, freshlyPredicted, libBatchEmbeddings);

      // Phase 2: per feature list, embed queries and dot-product against the kept lib NDArrays.
      for (final ModularFeatureList flist : featureLists) {
        if (isCanceled()) {
          return;
        }
        try {
          matchFeatureList(model, flist, libraryBatches, libBatchEmbeddings, algorithm);
        } catch (TranslateException e) {
          error("Failed to predict " + algorithm.toString() + " query embeddings for "
              + flist.getName() + ": " + e.getMessage(), e);
          return;
        }
        flist.addDescriptionOfAppliedTask(new SimpleFeatureListAppliedMethod(
            "Analog spectral library search (" + algorithm.getStableId() + ")",
            AnalogSpectralLibrarySearchModule.class, parameters, getModuleCallDate()));
      }

      // Phase 3: persist freshly-predicted batches to the entry cache for future runs.
      writeBackPredictedBatches(libraryBatches, libBatchEmbeddings, freshlyPredicted);

    } catch (ModelNotFoundException | MalformedModelException | IOException e) {
      error("Could not load " + algorithm.toString() + " model: " + e.getMessage(), e);
    }
    // closing the model also closes the NDManager which owns every NDArray in libBatchEmbeddings —
    // no per-array close call needed.
  }

  // ----- phase 1: library precompute -----

  private void precomputeLibraryEmbeddings(final EmbeddingBasedSimilarity model,
      final List<List<SpectralLibraryEntry>> libraryBatches, final NDArray[] libBatchEmbeddings,
      final BitSet freshlyPredicted, final SpectralNetworkingOptions label)
      throws TranslateException {
    final DBEntryField field = modelId.getEmbeddingField();
    final int total = libraryBatches.size();
    for (int b = 0; b < total; b++) {
      if (isCanceled()) {
        return;
      }
      final List<SpectralLibraryEntry> batch = libraryBatches.get(b);
      final boolean allCached = batch.stream().allMatch(e -> e.getOrElse(field, null) != null);
      if (allCached) {
        final float[][] rows = new float[batch.size()][];
        for (int i = 0; i < batch.size(); i++) {
          rows[i] = (float[]) batch.get(i).getOrElse(field, null);
        }
        libBatchEmbeddings[b] = model.getNDManager().create(rows);
      } else {
        libBatchEmbeddings[b] = model.predictEmbedding(batch);
        freshlyPredicted.set(b);
      }
      finishedItems.addAndGet(batch.size());
      description = label + " library embeddings: %d / %d batches".formatted(b + 1, total);
    }
  }

  private void logPrecomputeSummary(final List<List<SpectralLibraryEntry>> libraryBatches,
      final BitSet freshlyPredicted, final NDArray[] libBatchEmbeddings) {
    long predicted = 0;
    long cached = 0;
    long elements = 0;
    for (int b = 0; b < libraryBatches.size(); b++) {
      final int size = libraryBatches.get(b).size();
      if (freshlyPredicted.get(b)) {
        predicted += size;
      } else {
        cached += size;
      }
      if (libBatchEmbeddings[b] != null) {
        elements += libBatchEmbeddings[b].size();
      }
    }
    final long approxMB = (elements * Float.BYTES) / (1024L * 1024L);
    logger.finest(
        "Cached %d ML embeddings (~%d MB) for model %s (%d freshly predicted, %d reused from cache)".formatted(
            predicted + cached, approxMB, modelId.labelVersion(), predicted, cached));
  }

  // ----- phase 2: per-feature-list matching -----

  private void matchFeatureList(final EmbeddingBasedSimilarity model,
      final ModularFeatureList flist, final List<List<SpectralLibraryEntry>> libraryBatches,
      final NDArray[] libBatchEmbeddings, final SpectralNetworkingOptions algorithm)
      throws TranslateException {
    description = algorithm.toString() + " analog matching: " + flist.getName();
    final List<FeatureListRow> queryRows = new ArrayList<>();
    final List<Scan> queryScans = new ArrayList<>();
    collectMlQueryScans(flist, queryRows, queryScans);
    if (queryRows.isEmpty()) {
      // still advance progress so the bar reaches 100% even when a list has no eligible scans
      finishedItems.addAndGet(flist.getNumberOfRows());
      return;
    }

    final NDArray queryEmb = predictEmbeddingsBatched(model, queryScans, algorithm,
        flist.getName());
    try {
      final DataPoint[][] queryDpsCache = new DataPoint[queryScans.size()][];
      for (int b = 0; b < libraryBatches.size(); b++) {
        if (isCanceled()) {
          return;
        }
        final float[][] simBatch = EmbeddingBasedSimilarity.dotProduct(queryEmb,
            libBatchEmbeddings[b]);
        appendBatchMatchesFromSim(simBatch, queryRows, queryScans, libraryBatches.get(b),
            queryDpsCache);

        finishedItems.getAndAdd(libraryBatches.get(b).size());
      }
      sortRowAnalogMatchesByMlScore(queryRows);
    } finally {
      queryEmb.close();
    }
    // some rows in the feature list may have been skipped (no MS2 scan) — top off so progress
    // reaches the budget we allocated
    final long skipped = (long) flist.getNumberOfRows() - queryScans.size();
    if (skipped > 0) {
      finishedItems.addAndGet(skipped);
    }
  }

  /**
   * Forwards spectra through the model in fixed-size batches. Returns the concatenated embeddings
   * as a single NDArray of shape {@code [N, embDim]}. Each batch advances {@code finishedItems} so
   * the progress bar moves while query embeddings are being computed.
   */
  private NDArray predictEmbeddingsBatched(final EmbeddingBasedSimilarity model,
      final List<? extends MassSpectrum> spectra, final SpectralNetworkingOptions algorithm,
      final String flistName) throws TranslateException {
    final NDList batches = new NDList();
    for (int start = 0; start < spectra.size(); start += batchSize) {
      if (isCanceled()) {
        break;
      }
      final int end = Math.min(start + batchSize, spectra.size());
      batches.add(model.predictEmbedding(spectra.subList(start, end)));
      finishedItems.addAndGet(end - start);
      description = "Computing " + algorithm.toString() + " query embeddings for " + flistName
          + ": %d / %d".formatted(end, spectra.size());
    }
    return NDArrays.concat(batches);
  }

  private void collectMlQueryScans(final ModularFeatureList flist,
      final List<FeatureListRow> outRows, final List<Scan> outScans) {
    for (final FeatureListRow row : flist.getRows()) {
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
        final SpectralLibraryEntry entry = entryBatch.get(c);

        if (mlScore < mlMinScore || (entry.getPrecursorMZ() != null
            && ML_FALLBACK_MZ_TOL.checkWithinTolerance(entry.getPrecursorMZ(),
            row.getAverageMZ()))) {
          // skip direct matches
          continue;
        }
        // lazy — only build the sorted-copy once at least one candidate passes the threshold
        if (queryDpsCache[q] == null) {
          queryDpsCache[q] = AnalogSearchSimilarities.sortAndCopyScan(queryScans.get(q));
        }
        final SpectralSimilarity cosineForViz = AnalogSearchSimilarities.computeFallbackCosine(
            queryDpsCache[q], entry, ML_FALLBACK_MZ_TOL, rowPrecursor);
        if (cosineForViz == null) {
          continue;
        }
        final SpectralDBAnnotation annotation = new SpectralDBAnnotation(entry, cosineForViz,
            queryScans.get(q), null, rowPrecursor, row.getAverageRT(), null,
            AnalogSpectralLibraryMatchesType.class);
        annotation.set(MLScoreType.class, new MLScore(mlScore, modelId));
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
   * Sorts every query row's analog matches by the active ML score (descending) so the best matches
   * surface first downstream.
   */
  private void sortRowAnalogMatchesByMlScore(final List<FeatureListRow> queryRows) {
    final Comparator<SpectralDBAnnotation> byMlScoreDesc = Comparator.comparingDouble(
        (SpectralDBAnnotation a) -> {
          final MLScore s = a.get(MLScoreType.class);
          return s == null ? 0d : s.score();
        }).reversed();
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

  private @Nullable Scan pickSingleQueryScanForMl(final FeatureListRow row) {
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

  // ----- phase 3: persist cache -----

  private void writeBackPredictedBatches(final List<List<SpectralLibraryEntry>> libraryBatches,
      final NDArray[] libBatchEmbeddings, final BitSet freshlyPredicted) {
    final DBEntryField field = modelId.getEmbeddingField();
    for (int b = freshlyPredicted.nextSetBit(0); b >= 0; b = freshlyPredicted.nextSetBit(b + 1)) {
      final float[][] rows = EmbeddingBasedSimilarity.convertNDArrayToFloatMatrix(
          libBatchEmbeddings[b]);
      final List<SpectralLibraryEntry> batch = libraryBatches.get(b);
      for (int i = 0; i < batch.size(); i++) {
        batch.get(i).putIfNotNull(field, rows[i]);
      }
    }
  }

  // ----- helpers -----

  private @NotNull EmbeddingBasedSimilarity openMlModel()
      throws ModelNotFoundException, MalformedModelException, IOException {
    return switch (algorithm) {
      case MS2_DEEPSCORE -> new MS2DeepscoreModel(modelFile, modelSettingsFile);
//      case DREAMS -> new DreaMSModel(modelFile, modelSettingsFile);
      case MODIFIED_COSINE, COSINE_NO_PRECURSOR ->
          throw new AssertionError("openMlModel called for non-ML algorithm: " + algorithm);
    };
  }

  private @NotNull List<SpectralLibraryEntry> loadAndSortLibraryEntries() {
    final List<SpectralLibraryEntry> entries = parameters.getValue(
            AnalogSpectralLibrarySearchParameters.libraries)
        .getMatchingLibraryEntriesAndCheckAvailability();
    return entries.stream().filter(e -> e.getPrecursorMZ() != null)
        .sorted(Comparator.comparing(SpectralLibraryEntry::getPrecursorMZ)).toList();
  }

}
