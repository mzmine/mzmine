/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.group_spectral_networking.cosine_no_precursor;


import static io.github.mzmine.modules.dataprocessing.group_spectral_networking.modified_cosine.ModifiedCosineSpectralNetworkingTask.SIZE_OVERLAP;
import static io.github.mzmine.modules.dataprocessing.group_spectral_networking.modified_cosine.ModifiedCosineSpectralNetworkingTask.addNetworkStatisticsToRows;
import static io.github.mzmine.modules.dataprocessing.group_spectral_networking.modified_cosine.ModifiedCosineSpectralNetworkingTask.createMS2Sim;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import io.github.mzmine.datamodel.features.correlation.R2RNetworkingMaps;
import io.github.mzmine.datamodel.features.correlation.R2RSpectralSimilarity;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import io.github.mzmine.datamodel.features.correlation.SpectralSimilarity;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.MainSpectralNetworkingParameters;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.SpectralSignalFilter;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.modified_cosine.FilteredRowData;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.collections.CollectionUtils;
import io.github.mzmine.util.collections.StreamUtils;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.maths.Combinatorics;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NoPrecursorCosineSpectralNetworkingTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(
      NoPrecursorCosineSpectralNetworkingTask.class.getName());
  // Logger.
  private final AtomicLong processedPairs = new AtomicLong(0);
  private final int minMatch;
  private final MZTolerance mzTolerance;
  private final double minCosineSimilarity;
  private final @Nullable ModularFeatureList featureList;
  // target
  private final SpectralSignalFilter signalFilter;
  private final List<FeatureListRow> rows;
  private long totalMaxPairs = 0;

  public NoPrecursorCosineSpectralNetworkingTask(final ParameterSet mainParameters,
      @NotNull ModularFeatureList featureList, @NotNull Instant moduleCallDate,
      final Class<? extends MZmineModule> moduleClass) {
    this(mainParameters, featureList, featureList.getRows(), moduleCallDate, moduleClass);
  }

  /**
   * Create the task on set of rows
   */
  public NoPrecursorCosineSpectralNetworkingTask(final ParameterSet mainParameters,
      @Nullable ModularFeatureList featureList, List<FeatureListRow> rows,
      @NotNull Instant moduleCallDate, final Class<? extends MZmineModule> moduleClass) {
    super(null, moduleCallDate, mainParameters, moduleClass);
    this.rows = rows;
    // get sub parameters of this algorithm
    var subParams = mainParameters.getEmbeddedParameterValue(
        MainSpectralNetworkingParameters.algorithms);
    this.featureList = featureList;
    mzTolerance = subParams.getValue(NoPrecursorCosineSpectralNetworkingParameters.MZ_TOLERANCE);

    minMatch = subParams.getValue(NoPrecursorCosineSpectralNetworkingParameters.MIN_MATCH);
    minCosineSimilarity = subParams.getValue(
        NoPrecursorCosineSpectralNetworkingParameters.MIN_COSINE_SIMILARITY);
    // embedded signal filters
    signalFilter = subParams.getValue(NoPrecursorCosineSpectralNetworkingParameters.signalFilters)
        .createFilter();
  }

  @Override
  public void process() {
    final R2RMap<RowsRelationship> mapCosineSim = new R2RMap<>();
    try {
      checkRowsBestMs2(mapCosineSim, rows);

      logger.info(MessageFormat.format(
          "GC-EI-MS cosine similarity check on rows done. MS cosine similarity edges={0}",
          mapCosineSim.size()));

      if (featureList != null) {
        R2RNetworkingMaps rowMaps = featureList.getRowMaps();
        rowMaps.addAllRowsRelationships(mapCosineSim, Type.MS2_COSINE_SIM);
        R2RNetworkingMaps onlyCosineMap = new R2RNetworkingMaps();
        //noinspection OptionalGetWithoutIsPresent
        onlyCosineMap.addAllRowsRelationships(featureList.getMs2SimilarityMap().get(),
            Type.MS2_COSINE_SIM);
        addNetworkStatisticsToRows(featureList, onlyCosineMap);
      }

      logger.info("Added %d edges for %s".formatted(mapCosineSim.size(), Type.MS2_COSINE_SIM));
    } catch (MissingMassListException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
      setErrorMessage(e.getMessage());
      setStatus(TaskStatus.ERROR);
    }
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    if (featureList == null) {
      return List.of();
    }
    return List.of(featureList);
  }

  /**
   * Parallel check of all r2r similarities
   *
   * @param mapSimilarity map for all MS cosine similarity edges
   * @param rows          match rows
   */
  public void checkRowsBestMs2(R2RMap<RowsRelationship> mapSimilarity, List<FeatureListRow> rows)
      throws MissingMassListException {
    List<FilteredRowData> filteredRows = prepareRowBestSpectrum(rows);
    final int numRows = filteredRows.size();
    totalMaxPairs = Combinatorics.uniquePairs(filteredRows);
    logger.log(Level.INFO, MessageFormat.format("Checking MS2 similarity on {0} rows", numRows));

    long comparedPairs = StreamUtils.processPairs(filteredRows, this::isCanceled, true, //
        pair -> {
          // the actual processing
          checkSpectralPair(pair.left(), pair.right(), mapSimilarity);
          processedPairs.incrementAndGet();
        });

    logger.info("Spectral networking: Performed %d pairwise comparisons.".formatted(comparedPairs));
  }

  @NotNull
  private List<FilteredRowData> prepareRowBestSpectrum(final List<FeatureListRow> rows) {
    // sorting required but use stream to leave original row order
    // prefilter rows: has MS2 and in case only best MS2 is considered - check minDP
    // and prepare data points
    List<FilteredRowData> filteredRows = rows.stream().sorted(FeatureListRowSorter.MZ_ASCENDING)
        .map(row -> getDataAndFilter(row, row.getMostIntenseFragmentScan(), minMatch))
        .filter(Objects::nonNull).collect(CollectionUtils.toArrayList());
    return filteredRows;
  }

  private boolean checkSpectralPair(final FilteredRowData a, final FilteredRowData b,
      final R2RMap<RowsRelationship> mapSimilarity) {
    return checkR2RMs2Similarity(mapSimilarity, a.row(), b.row(), a.data(), b.data(),
        Type.MS2_COSINE_SIM);
  }

  /**
   * @param mapSimilarity map to add new MS cosine similarity edges to
   * @param a             row a
   * @param b             row b
   * @param simType       similarity type
   */
  public boolean checkR2RMs2Similarity(R2RMap<RowsRelationship> mapSimilarity, FeatureListRow a,
      FeatureListRow b, DataPoint[] sortedA, DataPoint[] sortedB, Type simType) {
    // align and check spectra
    SpectralSimilarity spectralSim = createMS2Sim(mzTolerance, sortedA, sortedB, minMatch,
        SIZE_OVERLAP);

    if (spectralSim != null && spectralSim.cosine() >= minCosineSimilarity) {
      var r2r = new R2RSpectralSimilarity(a, b, simType, spectralSim);
      mapSimilarity.add(a, b, r2r);
      return true;
    }
    return false;
  }

  /**
   * Checks the minimum requirements for a row to be matched by MS cosine similarity (minimum number
   * of data points and MS data availability)
   *
   * @param row   the test row
   * @param ms2   the scan with data
   * @param minDP minimum number of data points in mass list
   * @return the filtered data for a row or null if minimum criteria not met
   */
  @Nullable
  private FilteredRowData getDataAndFilter(@NotNull FeatureListRow row, @Nullable Scan ms2,
      int minDP) throws MissingMassListException {
    if (ms2 == null) {
      return null;
    }
    // remove precursor signals
    DataPoint[] dps = signalFilter.applyFilterAndSortByIntensity(ms2, null, minDP);
    return dps != null ? new FilteredRowData(row, dps) : null;
  }


  @Override
  public double getFinishedPercentage() {
    return totalMaxPairs == 0 ? 0 : processedPairs.get() / (double) totalMaxPairs;
  }

  @Override
  public String getTaskDescription() {
    return "Check cosine similarity of pseudo MS2 scans (mass lists)";
  }

}
