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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.AnalogSpectralLibraryMatchesType;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.SpectraMergeSelectParameter;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.SignalFiltersParameters;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.SpectralNetworkingOptions;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.SpectralSignalFilter;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.cosine_no_precursor.NoPrecursorCosineSpectralNetworkingParameters;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.modified_cosine.ModifiedCosineSpectralNetworkingParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.scans.FragmentScanSelection;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Cosine analog-search task. Owns ALL selected feature lists and processes them one-by-one, because
 * {@link #processCosine} parallelises rows internally — spawning one task per feature list would
 * oversubscribe the CPU.
 */
public class AnalogSpectralLibrarySearchCosineTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(
      AnalogSpectralLibrarySearchCosineTask.class.getName());

  private static final String FN_MODIFIED_COSINE = "Modified cosine";
  private static final String FN_COSINE_NO_PRECURSOR = "Cosine (no precursor)";

  private final MZmineProject project;
  private final ModularFeatureList[] featureLists;
  private final SpectralNetworkingOptions algorithm;
  private final MZTolerance mzTolerance;
  private final int minMatch;
  private final double minCosine;
  private final double maxMzDelta;
  private final SpectralSignalFilter signalFilter;
  // MODIFIED_COSINE removes precursor signals; COSINE_NO_PRECURSOR keeps them
  private final boolean removePrecursor;
  @Nullable
  private final FragmentScanSelection scanSelect;

  private String description;
  private ModularFeatureList currentFeatureList;

  public AnalogSpectralLibrarySearchCosineTask(@NotNull final MZmineProject project,
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
      }
      case COSINE_NO_PRECURSOR -> {
        mzTolerance = algoParams.getValue(
            NoPrecursorCosineSpectralNetworkingParameters.MZ_TOLERANCE);
        minMatch = algoParams.getValue(NoPrecursorCosineSpectralNetworkingParameters.MIN_MATCH);
        minCosine = algoParams.getValue(
            NoPrecursorCosineSpectralNetworkingParameters.MIN_COSINE_SIMILARITY);
        signalFilter = algoParams.getValue(
            NoPrecursorCosineSpectralNetworkingParameters.signalFilters).createFilter();
        scanSelect = null; // uses most intense fragment scan per row
        removePrecursor = false;
        maxMzDelta = 1E4;
      }
      default -> throw new AssertionError(
          "AnalogSpectralLibrarySearchCosineTask called with non-cosine algorithm: " + algorithm);
    }

    totalItems = Arrays.stream(featureLists).mapToLong(FeatureList::getNumberOfRows).sum();
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
    final List<SpectralLibraryEntry> sortedEntries = loadAndSortLibraryEntries();
    if (sortedEntries.isEmpty()) {
      logger.info("Analog cosine search: no library entries available; nothing to do.");
      return;
    }

    for (final ModularFeatureList flist : featureLists) {
      if (isCanceled()) {
        return;
      }
      currentFeatureList = flist;
      flist.addRowType(DataTypes.get(AnalogSpectralLibraryMatchesType.class));
      description = "Analog cosine search on " + flist.getName();
      try {
        processCosine(flist, sortedEntries, algorithm);
      } catch (MissingMassListException e) {
        error("No mass list found in scan: " + e.getMessage(), e);
        return;
      }
      flist.addDescriptionOfAppliedTask(new SimpleFeatureListAppliedMethod(
          "Analog spectral library search (" + algorithm.getStableId() + ")",
          AnalogSpectralLibrarySearchModule.class, parameters, getModuleCallDate()));
    }
  }

  private @NotNull List<SpectralLibraryEntry> loadAndSortLibraryEntries() {
    final List<SpectralLibraryEntry> entries = parameters.getValue(
            AnalogSpectralLibrarySearchParameters.libraries)
        .getMatchingLibraryEntriesAndCheckAvailability();
    // todo: filter for precursor mz only necessary for modified cosine
    return entries.stream().filter(e -> e.getPrecursorMZ() != null)
        .sorted(Comparator.comparing(SpectralLibraryEntry::getPrecursorMZ)).toList();
  }

  private @NotNull List<SpectralLibraryEntry> candidatesFor(final double rowPrecursorMz,
      final List<SpectralLibraryEntry> sortedEntries) {
    return BinarySearch.indexRange(
        Range.closed(rowPrecursorMz - maxMzDelta, rowPrecursorMz + maxMzDelta), sortedEntries,
        SpectralLibraryEntry::getPrecursorMZ).sublist(sortedEntries);
  }

  private void processCosine(final ModularFeatureList flist,
      final List<SpectralLibraryEntry> sortedEntries, final SpectralNetworkingOptions algorithm) {
    flist.getRows().parallelStream().forEach(row -> {
      if (isCanceled()) {
        return;
      }
      try {
        processCosineRow(row, sortedEntries, algorithm);
      } finally {
        finishedItems.incrementAndGet();
      }
    });
  }

  private void processCosineRow(final FeatureListRow row,
      final List<SpectralLibraryEntry> sortedEntries, final SpectralNetworkingOptions algorithm) {
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

      if (entry.getPrecursorMZ() != null && mzTolerance.checkWithinTolerance(row.getAverageMZ(),
          entry.getPrecursorMZ())) {
        // if it is a direct match, skip this entry
        continue;
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
          sim = AnalogSearchSimilarities.computeModifiedCosine(queryDps, sortedEntryDps,
              mzTolerance, minMatch, rowPrecursor, entry.getPrecursorMZ(), algorithm.toString());
        } else {
          sim = AnalogSearchSimilarities.computeModifiedCosine(queryDps, sortedEntryDps,
              mzTolerance, minMatch, -1d, -1d, algorithm.toString());
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
}
