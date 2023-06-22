/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.group_spectral_networking;


import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import io.github.mzmine.datamodel.features.correlation.R2RSpectralSimilarity;
import io.github.mzmine.datamodel.features.correlation.R2RSpectralSimilarityList;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import io.github.mzmine.datamodel.features.correlation.SpectralSimilarity;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.maths.similarity.Similarity;
import io.github.mzmine.util.scans.ScanAlignment;
import io.github.mzmine.util.scans.ScanMZDiffConverter;
import io.github.mzmine.util.scans.similarity.Weights;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpectralNetworkingTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(SpectralNetworkingTask.class.getName());

  public static final DataPointSorter dpSorter = new DataPointSorter(SortingProperty.Intensity,
      SortingDirection.Descending);
  public final static Function<List<DataPoint[]>, Integer> DIFF_OVERLAP = list -> ScanMZDiffConverter.getOverlapOfAlignedDiff(
      list, 0, 1);
  public final static Function<List<DataPoint[]>, Integer> SIZE_OVERLAP = SpectralNetworkingTask::calcOverlap;
  // Logger.
  private final AtomicDouble stageProgress;
  private final int minMatch;
  private final MZTolerance mzTolerance;
  private final double minCosineSimilarity;
  private final int maxDPForDiff;
  private final boolean onlyBestMS2Scan;
  private final ModularFeatureList featureList;
  // target
  private final boolean checkNeutralLoss;
  private final boolean useModAwareCosine; // TODO mod aware
  private List<FeatureListRow> rows;
  private final boolean isRemovePrecursor;
  private final double removePrecursorMz;
  private final boolean useMaxMzDelta;
  private final double maxMzDelta;

  // use a maximum
  private final int signalThresholdForTargetIntensityPercent = 50;
  private final double targetIntensityPercentage = 0.98;
  // hard cut off of noisy spectra
  private final int cropAfterSignalsCount = 500;

  public SpectralNetworkingTask(final ParameterSet params, @Nullable ModularFeatureList featureList,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.featureList = featureList;
    mzTolerance = params.getValue(SpectralNetworkingParameters.MZ_TOLERANCE);
    isRemovePrecursor = params.getValue(SpectralNetworkingParameters.REMOVE_PRECURSOR);
    removePrecursorMz = params.getEmbeddedParameterValueIfSelectedOrElse(
        SpectralNetworkingParameters.REMOVE_PRECURSOR, 0d);
    useMaxMzDelta = params.getValue(SpectralNetworkingParameters.MAX_MZ_DELTA);
    maxMzDelta = params.getEmbeddedParameterValueIfSelectedOrElse(
        SpectralNetworkingParameters.MAX_MZ_DELTA, Double.MAX_VALUE);

    minMatch = params.getValue(SpectralNetworkingParameters.MIN_MATCH);
    useModAwareCosine = params.getValue(SpectralNetworkingParameters.MODIFICATION_AWARE_COSINE);
    minCosineSimilarity = params.getValue(SpectralNetworkingParameters.MIN_COSINE_SIMILARITY);
    onlyBestMS2Scan = params.getValue(SpectralNetworkingParameters.ONLY_BEST_MS2_SCAN);
    stageProgress = new AtomicDouble(0);
    // check neutral loss similarity?
    checkNeutralLoss = params.getValue(SpectralNetworkingParameters.CHECK_NEUTRAL_LOSS_SIMILARITY);
    if (checkNeutralLoss) {
      final NeutralLossSimilarityParameters nlossParam = params.getParameter(
          SpectralNetworkingParameters.CHECK_NEUTRAL_LOSS_SIMILARITY).getEmbeddedParameters();
      maxDPForDiff = nlossParam.getValue(NeutralLossSimilarityParameters.MAX_DP_FOR_DIFF);
    } else {
      maxDPForDiff = 0;
    }
  }

  /**
   * Create the task on set of rows
   */
  public SpectralNetworkingTask(final ParameterSet parameters,
      @Nullable ModularFeatureList featureList, List<FeatureListRow> rows,
      @NotNull Instant moduleCallDate) {
    this(parameters, featureList, moduleCallDate);
    this.rows = rows;
  }

  /**
   * Make sure to use arrays sorted by intensity
   *
   * @param minMatch minimum overlapping signals in the two mass lists sortedA and sortedB
   * @param sortedA  sorted array of data points (by intensity)
   * @param sortedB  sorted array of data points (by intensity)
   * @param mzTol    the tolerance to match signals
   * @return the spectral similarity if number of overlapping signals >= minimum, else null
   */
  @Nullable
  public static SpectralSimilarity createMS2Sim(MZTolerance mzTol, DataPoint[] sortedA,
      DataPoint[] sortedB, double minMatch) {
    return createMS2SimModificationAware(mzTol, sortedA, sortedB, minMatch, SIZE_OVERLAP, -1d, -1d);
  }

  /**
   * Make sure to use arrays sorted by intensity
   *
   * @param minMatch        minimum overlapping signals in the two mass lists sortedA and sortedB
   * @param overlapFunction different functions to determin the size of overlap
   * @param sortedA         sorted array of data points (by intensity)
   * @param sortedB         sorted array of data points (by intensity)
   * @param mzTol           the tolerance to match signals
   * @return the spectral similarity if number of overlapping signals >= minimum, else null
   */
  @Nullable
  public static SpectralSimilarity createMS2Sim(MZTolerance mzTol, DataPoint[] sortedA,
      DataPoint[] sortedB, double minMatch, Function<List<DataPoint[]>, Integer> overlapFunction) {
    return createMS2SimModificationAware(mzTol, sortedA, sortedB, minMatch, overlapFunction, -1d,
        -1d);
  }

  /**
   * Make sure to use arrays sorted by intensity
   *
   * @param minMatch minimum overlapping signals in the two mass lists sortedA and sortedB
   * @param sortedA  sorted array of data points (by intensity)
   * @param sortedB  sorted array of data points (by intensity)
   * @param mzTol    the tolerance to match signals
   * @return the spectral similarity if number of overlapping signals >= minimum, else null
   */
  public static SpectralSimilarity createMS2Sim(MZTolerance mzTol, DataPoint[] sortedA,
      DataPoint[] sortedB, int minMatch, Weights weights) {
    return createMS2SimModificationAware(mzTol, weights, sortedA, sortedB, minMatch, SIZE_OVERLAP,
        -1d, -1d);
  }

  /**
   * Make sure to use arrays sorted by intensity
   *
   * @param minMatch        minimum overlapping signals in the two mass lists sortedA and sortedB
   * @param overlapFunction different functions to determin the size of overlap
   * @param sortedA         sorted array of data points (by intensity)
   * @param sortedB         sorted array of data points (by intensity)
   * @param precursorMzA    precursor mz of array sortedA, only used if useModAwareCosine is active
   * @param precursorMzB    precursor mz of array sortedB, only used if useModAwareCosine is active
   * @param mzTol           the tolerance to match signals
   * @return the spectral similarity if number of overlapping signals >= minimum, else null
   */
  @Nullable
  public static SpectralSimilarity createMS2SimModificationAware(MZTolerance mzTol,
      DataPoint[] sortedA, DataPoint[] sortedB, double minMatch,
      Function<List<DataPoint[]>, Integer> overlapFunction, double precursorMzA,
      double precursorMzB) {
    return createMS2SimModificationAware(mzTol, Weights.SQRT, sortedA, sortedB, minMatch,
        overlapFunction, precursorMzA, precursorMzB);
  }

  /**
   * Make sure to use arrays sorted by intensity
   *
   * @param minMatch        minimum overlapping signals in the two mass lists sortedA and sortedB
   * @param overlapFunction different functions to determin the size of overlap
   * @param sortedA         sorted array of data points (by intensity)
   * @param sortedB         sorted array of data points (by intensity)
   * @param precursorMzA    precursor mz of array sortedA, only used if useModAwareCosine is active
   * @param precursorMzB    precursor mz of array sortedB, only used if useModAwareCosine is active
   * @param mzTol           the tolerance to match signals
   * @return the spectral similarity if number of overlapping signals >= minimum, else null
   */
  @Nullable
  public static SpectralSimilarity createMS2SimModificationAware(MZTolerance mzTol, Weights weights,
      DataPoint[] sortedA, DataPoint[] sortedB, double minMatch,
      Function<List<DataPoint[]>, Integer> overlapFunction, double precursorMzA,
      double precursorMzB) {
    // align
    final List<DataPoint[]> aligned = alignDataPoints(precursorMzA, precursorMzB, mzTol, sortedB,
        sortedA);
    // keep unaligned
    // aligned = ScanAlignment.removeUnaligned(aligned);
    // overlapping mass diff
    int overlap = overlapFunction.apply(aligned);

    if (overlap >= minMatch) {
      // cosine
      double[][] diffArray = ScanAlignment.toIntensityMatrixWeighted(aligned,
          weights.getIntensity(), weights.getMz());
      double diffCosine = Similarity.COSINE.calc(diffArray);

      int sizeA = 0;
      int sizeB = 0;
      double totalIntensityA = 0;
      double totalIntensityB = 0;
      double explainedIntensityA = 0;
      double explainedIntensityB = 0;

      for (DataPoint[] alignedDP : aligned) {
        DataPoint a = alignedDP[0];
        DataPoint b = alignedDP[1];

        if (a != null && b != null) {
          explainedIntensityA += a.getIntensity();
          explainedIntensityB += b.getIntensity();
        }
        if (a != null) {
          totalIntensityA += a.getIntensity();
          sizeA++;
        }
        if (b != null) {
          totalIntensityB += b.getIntensity();
          sizeB++;
        }
      }

      return new SpectralSimilarity(diffCosine, overlap, sizeA, sizeB,
          explainedIntensityA / totalIntensityA, explainedIntensityB / totalIntensityB);
    }
    return null;
  }

  /**
   * Make sure to use arrays sorted by intensity
   *
   * @param mzTol        the tolerance to match signals
   * @param sortedA      sorted array of data points (by intensity)
   * @param sortedB      sorted array of data points (by intensity)
   * @param precursorMzA precursor mz of array sortedA, only used if useModAwareCosine is active
   * @param precursorMzB precursor mz of array sortedB, only used if useModAwareCosine is active
   * @return the spectral similarity if number of overlapping signals >= minimum, else null
   */
  public static CosinePairContributions calculateModifiedCosineSimilarityContributions(
      MZTolerance mzTol, DataPoint[] sortedA, DataPoint[] sortedB, double precursorMzA,
      double precursorMzB) {
    return calculateModifiedCosineSimilarityContributions(mzTol, Weights.SQRT, sortedA, sortedB,
        precursorMzA, precursorMzB);
  }

  /**
   * Make sure to use arrays sorted by intensity
   *
   * @param mzTol        the tolerance to match signals
   * @param sortedA      sorted array of data points (by intensity)
   * @param sortedB      sorted array of data points (by intensity)
   * @param precursorMzA precursor mz of array sortedA, only used if useModAwareCosine is active
   * @param precursorMzB precursor mz of array sortedB, only used if useModAwareCosine is active
   * @return the spectral similarity if number of overlapping signals >= minimum, else null
   */
  public static CosinePairContributions calculateModifiedCosineSimilarityContributions(
      MZTolerance mzTol, Weights weights, DataPoint[] sortedA, DataPoint[] sortedB,
      double precursorMzA, double precursorMzB) {
    // align
    final List<DataPoint[]> aligned = alignDataPoints(precursorMzA, precursorMzB, mzTol, sortedB,
        sortedA);
    if (aligned.size() >= 2) {
      // cosine
      double[][] diffArray = ScanAlignment.toIntensityMatrixWeighted(aligned,
          weights.getIntensity(), weights.getMz());

      final double cosineDivisor = Similarity.cosineDivisor(diffArray);
      double[] contributions = new double[diffArray.length];
      SignalAlignmentAnnotation[] annotations = new SignalAlignmentAnnotation[diffArray.length];
      for (int i = 0; i < diffArray.length; i++) {
        final double[] pair = diffArray[i];
        contributions[i] = Similarity.cosineSignalContribution(pair, cosineDivisor);

        final DataPoint[] dps = aligned.get(i);
        if (dps[0] != null && dps[1] != null) {
          if (mzTol.checkWithinTolerance(dps[0].getMZ(), dps[1].getMZ())) {
            annotations[i] = SignalAlignmentAnnotation.MATCH;
          } else {
            annotations[i] = SignalAlignmentAnnotation.MODIFIED;
          }
        } else {
          annotations[i] = SignalAlignmentAnnotation.NONE;
        }
      }

      return new CosinePairContributions(aligned, contributions, annotations);
    }
    return null;
  }

  @NotNull
  private static List<DataPoint[]> alignDataPoints(double precursorMzA, double precursorMzB,
      MZTolerance mzTol, DataPoint[] sortedB, DataPoint[] sortedA) {
    final List<DataPoint[]> aligned;
    if (precursorMzA > 0 && precursorMzB > 0) {
      aligned = ScanAlignment.alignOfSortedModAware(mzTol, sortedB, sortedA, precursorMzB,
          precursorMzA);
    } else {
      aligned = ScanAlignment.alignOfSorted(mzTol, sortedB, sortedA);
    }
    return aligned;
  }

  /**
   * Calculate overlap
   *
   * @param aligned
   * @return
   */
  public static int calcOverlap(List<DataPoint[]> aligned) {
    return (int) aligned.stream().filter(dp -> dp[0] != null && dp[1] != null).count();
  }


  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    final R2RMap<RowsRelationship> mapCosineSim = new R2RMap<>();
    final R2RMap<RowsRelationship> mapNeutralLoss = new R2RMap<>();
    try {
      if (onlyBestMS2Scan) {
        checkRowsBest(mapCosineSim, mapNeutralLoss, rows);
      } else {
        checkAllFeatures(mapCosineSim, mapNeutralLoss, rows);
      }
      logger.info(MessageFormat.format(
          "MS2 similarity check on rows done. MS2 cosine similarity={0}, MS2 neutral loss={1}",
          mapCosineSim.size(), mapNeutralLoss.size()));

      if (featureList != null) {
        featureList.addRowsRelationships(mapCosineSim, Type.MS2_COSINE_SIM);
        featureList.addRowsRelationships(mapNeutralLoss, Type.MS2_NEUTRAL_LOSS_SIM);
      }
      setStatus(TaskStatus.FINISHED);

    } catch (MissingMassListException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
      setErrorMessage(e.getMessage());
      setStatus(TaskStatus.ERROR);
      return;
    }
  }

  /**
   * Parallel check of all r2r similarities
   *
   * @param mapSimilarity  map for all MS2 cosine similarity edges
   * @param mapNeutralLoss map for all neutral loss MS2 edges
   * @param rows           match rows
   */
  public void checkRowsBest(R2RMap<RowsRelationship> mapSimilarity,
      R2RMap<RowsRelationship> mapNeutralLoss, List<FeatureListRow> rows)
      throws MissingMassListException {
    // prefilter rows: has MS2 and in case only best MS2 is considered - check minDP
    // and prepare data points
    List<FilteredRowData> filteredRows = new ArrayList<>();
    for (FeatureListRow row : rows) {
      FilteredRowData data = getDataAndFilter(row, row.getMostIntenseFragmentScan(),
          row.getAverageMZ(), minMatch);
      if (data != null) {
        filteredRows.add(data);
      }
    }
    int numRows = filteredRows.size();
    logger.log(Level.INFO,
        () -> MessageFormat.format("Checking MS2 similarity on {0} rows", numRows));
    // run in parallel
    IntStream.range(0, numRows - 1).parallel().forEach(i -> {
      if (!isCanceled()) {
        for (int j = i + 1; j < numRows; j++) {
          if (!isCanceled()) {
            FilteredRowData a = filteredRows.get(i);
            FilteredRowData b = filteredRows.get(j);
            checkR2RMs2Similarity(mapSimilarity, a.row(), b.row(), a.data(), b.data(),
                Type.MS2_COSINE_SIM);

            // check neutral loss similarity
            if (checkNeutralLoss) {
              // create mass diff array
              DataPoint[] massDiffA = ScanMZDiffConverter.getAllMZDiff(a.data(), mzTolerance, -1,
                  maxDPForDiff);
              DataPoint[] massDiffB = ScanMZDiffConverter.getAllMZDiff(b.data(), mzTolerance, -1,
                  maxDPForDiff);

              checkR2RMs2Similarity(mapNeutralLoss, a.row(), b.row(), massDiffA, massDiffB,
                  Type.MS2_NEUTRAL_LOSS_SIM);
            }
          }
        }
      }
      if (stageProgress != null) {
        stageProgress.getAndAdd(1d / numRows);
      }
    });
  }

  /**
   * Parallel check of all r2r similarities
   *
   * @param mapSimilarity  map for all MS2 cosine similarity edges
   * @param mapNeutralLoss map for all neutral loss MS2 edges
   * @param rows           match rows
   */
  public void checkAllFeatures(R2RMap<RowsRelationship> mapSimilarity,
      R2RMap<RowsRelationship> mapNeutralLoss, List<FeatureListRow> rows)
      throws MissingMassListException {
    // prefilter rows: has MS2 and in case only best MS2 is considered - check minDP
    // and prepare data points
    Map<Feature, FilteredRowData> mapFeatureData = new HashMap<>();
    List<FeatureListRow> filteredRows = new ArrayList<>();
    for (FeatureListRow row : rows) {
      if (prepareAllMS2(mapFeatureData, row)) {
        filteredRows.add(row);
      }
    }
    int numRows = filteredRows.size();
    logger.log(Level.INFO,
        () -> MessageFormat.format("Checking MS2 similarity on {0} rows", numRows));
    // run in parallel
    IntStream.range(0, numRows - 1).parallel().forEach(i -> {
      if (!isCanceled()) {
        for (int j = i + 1; j < numRows; j++) {
          if (!isCanceled()) {
            FeatureListRow a = filteredRows.get(i);
            FeatureListRow b = filteredRows.get(j);

            checkR2RAllFeaturesMs2Similarity(mapFeatureData, a, b, mapSimilarity, mapNeutralLoss);
          }
        }
      }
      if (stageProgress != null) {
        stageProgress.getAndAdd(1d / numRows);
      }
    });
  }

  private void checkR2RAllFeaturesMs2Similarity(Map<Feature, FilteredRowData> mapFeatureData,
      FeatureListRow a, FeatureListRow b, final R2RMap<RowsRelationship> mapSimilarity,
      final R2RMap<RowsRelationship> mapNeutralLoss) {

    R2RSpectralSimilarityList cosineSim = new R2RSpectralSimilarityList(a, b, Type.MS2_COSINE_SIM);
    R2RSpectralSimilarityList neutralLossSim =
        checkNeutralLoss ? new R2RSpectralSimilarityList(a, b, Type.MS2_NEUTRAL_LOSS_SIM) : null;

    DataPoint[] massDiffA = null;
    DataPoint[] massDiffB = null;

    for (Feature fa : a.getFeatures()) {
      DataPoint[] dpa = mapFeatureData.get(fa).data();
      if (dpa != null) {
        // create mass diff array
        if (checkNeutralLoss) {
          massDiffA = ScanMZDiffConverter.getAllMZDiff(dpa, mzTolerance, -1, maxDPForDiff);
          Arrays.sort(massDiffA, dpSorter);
        }
        for (Feature fb : b.getFeatures()) {
          DataPoint[] dpb = mapFeatureData.get(fb).data();
          if (dpb != null) {
            // align and check spectra
            SpectralSimilarity spectralSim = createMS2SimModificationAware(mzTolerance, dpa, dpb,
                minMatch, SIZE_OVERLAP, fa.getMZ(), fb.getMZ());
            if (spectralSim != null && spectralSim.cosine() >= minCosineSimilarity) {
              cosineSim.addSpectralSim(spectralSim);
            }

            // alignment and sim of neutral losses
            if (checkNeutralLoss) {
              massDiffB = ScanMZDiffConverter.getAllMZDiff(dpb, mzTolerance, maxDPForDiff);
              Arrays.sort(massDiffB, dpSorter);
              SpectralSimilarity massDiffSim = createMS2Sim(mzTolerance, massDiffA, massDiffB,
                  minMatch, DIFF_OVERLAP);

              if (massDiffSim != null && massDiffSim.cosine() >= minCosineSimilarity) {
                neutralLossSim.addSpectralSim(massDiffSim);
              }
            }
          }
        }
      }
    }

    if (checkNeutralLoss && neutralLossSim.size() > 0) {
      mapNeutralLoss.add(a, b, neutralLossSim);
    }
    if (cosineSim.size() > 0) {
      mapSimilarity.add(a, b, cosineSim);
    }
  }

  /**
   * Checks the minimum requirements for a row to be matched by MS2 similarity (minimum number of
   * data points and MS2 data availability)
   *
   * @param row         the test row
   * @param ms2         the scan with data
   * @param precursorMz the precursor mz is used to remove signals within range
   * @param minDP       minimum number of data points in mass list
   * @return the filtered data for a row or null if minimum criteria not met
   */
  @Nullable
  private FilteredRowData getDataAndFilter(@NotNull FeatureListRow row, @Nullable Scan ms2,
      double precursorMz, int minDP) throws MissingMassListException {
    if (ms2 == null) {
      return null;
    }
    MassList masses = ms2.getMassList();
    if (masses == null) {
      throw new MissingMassListException(ms2);
    }
    if (masses.getNumberOfDataPoints() < minDP) {
      return null;
    }
    DataPoint[] dps = masses.getDataPoints();
    // remove precursor signals
    if (isRemovePrecursor && removePrecursorMz > 0) {
      dps = DataPointUtils.removePrecursorMz(dps, precursorMz, removePrecursorMz);
      if (dps.length < minDP) {
        return null;
      }
    }

    // sort by intensity
    Arrays.sort(dps, dpSorter);

    // apply some filters to avoid noisy spectra with too many signals
    if (dps.length > signalThresholdForTargetIntensityPercent) {
      dps = DataPointUtils.filterDataByIntensityPercent(dps, targetIntensityPercentage,
          cropAfterSignalsCount);
    }

    if (dps.length < minDP) {
      return null;
    }
    return new FilteredRowData(row, dps);
  }


  /**
   * Checks the minimum requirements for the best MS2 for each feature in a row to be matched by MS2
   * similarity (minimum number of data points and MS2 data availability). Results are sorted by
   * intensity and stored in the mapRowData
   *
   * @param mapFeatureData the target map to store filtered and sorted data point arrays
   * @param row            the test row
   * @return true if the row matches all criteria, false otherwise
   */
  private boolean prepareAllMS2(@NotNull Map<Feature, FilteredRowData> mapFeatureData,
      @NotNull FeatureListRow row) throws MissingMassListException {
    if (!row.hasMs2Fragmentation()) {
      return false;
    }
    boolean result = false;
    for (Feature feature : row.getFeatures()) {

      Scan ms2 = feature.getMostIntenseFragmentScan();
      if (ms2 != null) {
        FilteredRowData data = getDataAndFilter(row, ms2, row.getAverageMZ(), minMatch);
        mapFeatureData.put(feature, data);
        result = true;
      }
    }
    return result;
  }

  /**
   * @param mapSimilarity map to add new MS2 cosine similarity edges to
   * @param a             row a
   * @param b             row b
   * @param simType       similarity type
   */
  public void checkR2RMs2Similarity(R2RMap<RowsRelationship> mapSimilarity, FeatureListRow a,
      FeatureListRow b, DataPoint[] sortedA, DataPoint[] sortedB, Type simType) {
    // align and check spectra
    SpectralSimilarity spectralSim =
        simType == Type.MS2_NEUTRAL_LOSS_SIM ? createMS2Sim(mzTolerance, sortedA, sortedB, minMatch,
            SIZE_OVERLAP)
            : createMS2SimModificationAware(mzTolerance, sortedA, sortedB, minMatch, SIZE_OVERLAP,
                a.getAverageMZ(), b.getAverageMZ());

    if (spectralSim != null && spectralSim.cosine() >= minCosineSimilarity) {
      var r2r = new R2RSpectralSimilarity(a, b, simType, spectralSim);
      mapSimilarity.add(a, b, r2r);
    }
  }

  @Override
  public double getFinishedPercentage() {
    return stageProgress.get();
  }

  @Override
  public String getTaskDescription() {
    return "Check similarity of MSMS scans (mass lists)";
  }

  /**
   * the filtered data of the best MS2 scan from row
   */
  private record FilteredRowData(FeatureListRow row, DataPoint[] data) {

  }
}
