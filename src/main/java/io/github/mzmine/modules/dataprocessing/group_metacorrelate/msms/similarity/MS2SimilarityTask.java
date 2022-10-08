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

package io.github.mzmine.modules.dataprocessing.group_metacorrelate.msms.similarity;


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

public class MS2SimilarityTask extends AbstractTask {

  public static final DataPointSorter dpSorter = new DataPointSorter(SortingProperty.Intensity,
      SortingDirection.Descending);
  public final static Function<List<DataPoint[]>, Integer> DIFF_OVERLAP = list -> ScanMZDiffConverter.getOverlapOfAlignedDiff(
      list, 0, 1);
  public final static Function<List<DataPoint[]>, Integer> SIZE_OVERLAP = list -> calcOverlap(list);
  // Logger.
  private static final Logger LOG = Logger.getLogger(MS2SimilarityTask.class.getName());
  private final AtomicDouble stageProgress;
  private final int minMatch;
  private final int minDP;
  private final MZTolerance mzTolerance;
  private final double minHeight;
  private final double minCosineSimilarity;
  private final int maxDPForDiff;
  private final boolean onlyBestMS2Scan;
  private final ModularFeatureList featureList;
  // target
  private final R2RMap<RowsRelationship> mapCosineSim = new R2RMap<>();
  private final R2RMap<RowsRelationship> mapNeutralLoss = new R2RMap<>();
  private final boolean checkNeutralLoss;
  private final boolean useModAwareCosine;
  private List<FeatureListRow> rows;


  public MS2SimilarityTask(final ParameterSet parameterSet,
      @Nullable ModularFeatureList featureList, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.featureList = featureList;
    mzTolerance = parameterSet.getParameter(MS2SimilarityParameters.MZ_TOLERANCE).getValue();
    minHeight = parameterSet.getParameter(MS2SimilarityParameters.MIN_HEIGHT).getValue();
    minDP = parameterSet.getParameter(MS2SimilarityParameters.MIN_DP).getValue();
    minMatch = parameterSet.getParameter(MS2SimilarityParameters.MIN_MATCH).getValue();
    useModAwareCosine = parameterSet.getParameter(MS2SimilarityParameters.MODIFICATION_AWARE_COSINE)
        .getValue();
    minCosineSimilarity = parameterSet.getParameter(MS2SimilarityParameters.MIN_COSINE_SIMILARITY)
        .getValue();
    onlyBestMS2Scan = parameterSet.getParameter(MS2SimilarityParameters.ONLY_BEST_MS2_SCAN)
        .getValue();
    stageProgress = new AtomicDouble(0);
    // check neutral loss similarity?
    checkNeutralLoss = parameterSet.getParameter(
        MS2SimilarityParameters.CHECK_NEUTRAL_LOSS_SIMILARITY).getValue();
    if (checkNeutralLoss) {
      final NeutralLossSimilarityParameters nlossParam = parameterSet.getParameter(
          MS2SimilarityParameters.CHECK_NEUTRAL_LOSS_SIMILARITY).getEmbeddedParameters();
      maxDPForDiff = nlossParam.getParameter(NeutralLossSimilarityParameters.MAX_DP_FOR_DIFF)
          .getValue();
    } else {
      maxDPForDiff = 0;
    }
  }

  /**
   * Create the task on set of rows
   */
  public MS2SimilarityTask(final ParameterSet parameters, @Nullable ModularFeatureList featureList,
      List<FeatureListRow> rows, @NotNull Instant moduleCallDate) {
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

    try {
      if (onlyBestMS2Scan) {
        checkRowsBest(mapCosineSim, mapNeutralLoss, rows);
      } else {
        checkAllFeatures(mapCosineSim, mapNeutralLoss, rows);
      }
      LOG.info(MessageFormat.format(
          "MS2 similarity check on rows done. MS2 cosine similarity={0}, MS2 neutral loss={1}",
          mapCosineSim.size(), mapNeutralLoss.size()));

      if (featureList != null) {
        featureList.addRowsRelationships(mapCosineSim, Type.MS2_COSINE_SIM);
        featureList.addRowsRelationships(mapNeutralLoss, Type.MS2_NEUTRAL_LOSS_SIM);
      }
      setStatus(TaskStatus.FINISHED);

    } catch (MissingMassListException e) {
      LOG.log(Level.SEVERE, e.getMessage(), e);
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
      FilteredRowData data = getDataAndFilter(row, minDP, minHeight);
      if (data != null) {
        filteredRows.add(data);
      }
    }
    int numRows = filteredRows.size();
    LOG.log(Level.INFO, () -> MessageFormat.format("Checking MS2 similarity on {0} rows", numRows));
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
              DataPoint[] massDiffA = ScanMZDiffConverter.getAllMZDiff(a.data(), mzTolerance,
                  minHeight, maxDPForDiff);
              DataPoint[] massDiffB = ScanMZDiffConverter.getAllMZDiff(b.data(), mzTolerance,
                  minHeight, maxDPForDiff);

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
    Map<Feature, DataPoint[]> mapFeatureData = new HashMap<>();
    List<FeatureListRow> filteredRows = new ArrayList<>();
    for (FeatureListRow row : rows) {
      if (prepareAllMS2(mapFeatureData, row, minDP, minHeight)) {
        filteredRows.add(row);
      }
    }
    int numRows = filteredRows.size();
    LOG.log(Level.INFO, () -> MessageFormat.format("Checking MS2 similarity on {0} rows", numRows));
    // run in parallel
    IntStream.range(0, numRows - 1).parallel().forEach(i -> {
      if (!isCanceled()) {
        for (int j = i + 1; j < numRows; j++) {
          if (!isCanceled()) {
            FeatureListRow a = filteredRows.get(i);
            FeatureListRow b = filteredRows.get(j);

            checkR2RAllFeaturesMs2Similarity(mapFeatureData, a, b);
          }
        }
      }
      if (stageProgress != null) {
        stageProgress.getAndAdd(1d / numRows);
      }
    });
  }

  private void checkR2RAllFeaturesMs2Similarity(Map<Feature, DataPoint[]> mapFeatureData,
      FeatureListRow a, FeatureListRow b) {

    R2RSpectralSimilarityList cosineSim = new R2RSpectralSimilarityList(a, b, Type.MS2_COSINE_SIM);
    R2RSpectralSimilarityList neutralLossSim =
        checkNeutralLoss ? new R2RSpectralSimilarityList(a, b, Type.MS2_NEUTRAL_LOSS_SIM) : null;

    DataPoint[] massDiffA = null;
    DataPoint[] massDiffB = null;

    for (Feature fa : a.getFeatures()) {
      DataPoint[] dpa = mapFeatureData.get(fa);
      if (dpa != null) {
        // create mass diff array
        if (checkNeutralLoss) {
          massDiffA = ScanMZDiffConverter.getAllMZDiff(dpa, mzTolerance, minHeight, maxDPForDiff);
          Arrays.sort(massDiffA, dpSorter);
        }
        for (Feature fb : b.getFeatures()) {
          DataPoint[] dpb = mapFeatureData.get(fb);
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
      mapCosineSim.add(a, b, cosineSim);
    }
  }

  /**
   * Checks the minimum requirements for a row to be matched by MS2 similarity (minimum number of
   * data points and MS2 data availability)
   *
   * @param row       the test row
   * @param minDP     minimum number of data points in mass list
   * @param minHeight minimum height of signals
   * @return the filtered data for a row or null if minimum criteria not met
   */
  @Nullable
  private FilteredRowData getDataAndFilter(@NotNull FeatureListRow row, int minDP, double minHeight)
      throws MissingMassListException {
    if (!row.hasMs2Fragmentation()) {
      return null;
    }
    MassList masses = row.getMostIntenseFragmentScan().getMassList();
    if (masses == null) {
      throw new MissingMassListException(row.getMostIntenseFragmentScan());
    }
    DataPoint[] filteredData = masses.getDataPoints();
    if (minHeight > 0) {
      filteredData = Arrays.stream(filteredData).filter(dp -> dp.getIntensity() >= minHeight)
          .toArray(DataPoint[]::new);
    }
    // filtered data or return null if minimum criteria not met
    if (filteredData.length >= minDP) {
      // sort by intensity
      Arrays.sort(filteredData, dpSorter);
      return new FilteredRowData(row, filteredData);
    } else {
      return null;
    }
  }

  /**
   * Checks the minimum requirements for the best MS2 for each feature in a row to be matched by MS2
   * similarity (minimum number of data points and MS2 data availability). Results are sorted by
   * intensity and stored in the mapRowData
   *
   * @param mapFeatureData the target map to store filtered and sorted data point arrays
   * @param row            the test row
   * @param minDP          minimum number of data points in mass list
   * @param minHeight      minimum height of signals
   * @return true if the row matches all criteria, false otherwise
   */
  private boolean prepareAllMS2(@NotNull Map<Feature, DataPoint[]> mapFeatureData,
      @NotNull FeatureListRow row, int minDP, double minHeight) throws MissingMassListException {
    if (!row.hasMs2Fragmentation()) {
      return false;
    }
    boolean result = false;
    for (Feature feature : row.getFeatures()) {
      Scan ms2 = feature.getMostIntenseFragmentScan();
      if (ms2 != null) {
        MassList masses = ms2.getMassList();
        if (masses == null) {
          throw new MissingMassListException(ms2);
        } else {
          DataPoint[] filteredData = masses.getDataPoints();
          if (minHeight > 0) {
            filteredData = Arrays.stream(filteredData).filter(dp -> dp.getIntensity() >= minHeight)
                .toArray(DataPoint[]::new);
          }
          // put data or null into map
          if (filteredData.length >= minDP) {
            // sort by intensity for later
            Arrays.sort(filteredData, dpSorter);
            mapFeatureData.put(feature, filteredData);
            result = true;
          }
        }
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
   * Resulting map of row-2-row MS2 spectral cosine similarities
   *
   * @return cosine similarity map
   */
  public R2RMap<RowsRelationship> getMapCosineSim() {
    return mapCosineSim;
  }

  /**
   * Resulting map of row-2-row MS2 neutral loss similarities
   *
   * @return neutral loss similarity map
   */
  public R2RMap<RowsRelationship> getMapNeutralLoss() {
    return mapNeutralLoss;
  }

  /**
   * the filtered data of the best MS2 scan from row
   */
  private record FilteredRowData(FeatureListRow row, DataPoint[] data) {

  }
}
