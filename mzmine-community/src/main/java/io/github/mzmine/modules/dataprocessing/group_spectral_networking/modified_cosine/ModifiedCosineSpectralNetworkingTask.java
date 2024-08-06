/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.group_spectral_networking.modified_cosine;


import static io.github.mzmine.modules.visualization.networking.visual.enums.NodeAtt.CLUSTER_ID;
import static io.github.mzmine.modules.visualization.networking.visual.enums.NodeAtt.CLUSTER_SIZE;
import static io.github.mzmine.modules.visualization.networking.visual.enums.NodeAtt.COMMUNITY_ID;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import io.github.mzmine.datamodel.features.correlation.R2RNetworkingMaps;
import io.github.mzmine.datamodel.features.correlation.R2RSpectralSimilarity;
import io.github.mzmine.datamodel.features.correlation.R2RSpectralSimilarityList;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import io.github.mzmine.datamodel.features.correlation.SpectralSimilarity;
import io.github.mzmine.datamodel.features.types.networking.NetworkStats;
import io.github.mzmine.datamodel.features.types.networking.NetworkStatsType;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.CosinePairContributions;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.MainSpectralNetworkingParameters;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.SignalAlignmentAnnotation;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.SpectralSignalFilter;
import io.github.mzmine.modules.visualization.networking.visual.FeatureNetworkGenerator;
import io.github.mzmine.modules.visualization.networking.visual.enums.NodeAtt;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.GraphStreamUtils;
import io.github.mzmine.util.collections.StreamUtils;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.maths.Combinatorics;
import io.github.mzmine.util.maths.similarity.Similarity;
import io.github.mzmine.util.scans.ScanAlignment;
import io.github.mzmine.util.scans.ScanMZDiffConverter;
import io.github.mzmine.util.scans.similarity.Weights;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import org.graphstream.algorithm.community.Community;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ModifiedCosineSpectralNetworkingTask extends AbstractFeatureListTask {

  public final static Function<List<DataPoint[]>, Integer> DIFF_OVERLAP = list -> ScanMZDiffConverter.getOverlapOfAlignedDiff(
      list, 0, 1);
  public final static Function<List<DataPoint[]>, Integer> SIZE_OVERLAP = ModifiedCosineSpectralNetworkingTask::calcOverlap;
  private static final Logger logger = Logger.getLogger(
      ModifiedCosineSpectralNetworkingTask.class.getName());
  // Logger.
  private final AtomicLong processedPairs = new AtomicLong(0);
  private final int minMatch;
  private final MZTolerance mzTolerance;
  private final double minCosineSimilarity;
  private final int maxDPForDiff;
  private final boolean onlyBestMS2Scan;
  private final @Nullable ModularFeatureList featureList;
  // target
  private final SpectralSignalFilter signalFilter;
  private final double maxMzDelta;
  private final List<FeatureListRow> rows;
  private long totalMaxPairs = 0;
  // this is always off for now. Could be reintroduced as separate similarity metric
  private final boolean checkNeutralLoss;

  public ModifiedCosineSpectralNetworkingTask(final ParameterSet mainParameters,
      @Nullable ModularFeatureList featureList, @NotNull Instant moduleCallDate,
      final Class<? extends MZmineModule> moduleClass) {
    this(mainParameters, featureList, featureList.getRows(), moduleCallDate, moduleClass);
  }

  /**
   * Create the task on set of rows
   */
  public ModifiedCosineSpectralNetworkingTask(final ParameterSet mainParameters,
      @Nullable ModularFeatureList featureList, List<FeatureListRow> rows,
      @NotNull Instant moduleCallDate, final Class<? extends MZmineModule> moduleClass) {
    super(null, moduleCallDate, mainParameters, moduleClass);
    this.rows = rows;
    // get sub parameters of this algorithm
    var subParams = mainParameters.getEmbeddedParameterValue(
        MainSpectralNetworkingParameters.algorithms);
    this.featureList = featureList;
    mzTolerance = subParams.getValue(ModifiedCosineSpectralNetworkingParameters.MZ_TOLERANCE);
    maxMzDelta = subParams.getEmbeddedParameterValueIfSelectedOrElse(
        ModifiedCosineSpectralNetworkingParameters.MAX_MZ_DELTA, Double.MAX_VALUE);

    minMatch = subParams.getValue(ModifiedCosineSpectralNetworkingParameters.MIN_MATCH);
    minCosineSimilarity = subParams.getValue(
        ModifiedCosineSpectralNetworkingParameters.MIN_COSINE_SIMILARITY);
    onlyBestMS2Scan = subParams.getValue(
        ModifiedCosineSpectralNetworkingParameters.ONLY_BEST_MS2_SCAN);
    // check neutral loss similarity was removed for now. Could be turned into other score
    checkNeutralLoss = false;
    maxDPForDiff = 0;
    // embedded signal filters
    signalFilter = subParams.getValue(ModifiedCosineSpectralNetworkingParameters.signalFilters)
        .createFilter();
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
  @Nullable
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
      @Nullable Double precursorMzA, @Nullable Double precursorMzB) {
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
  private static List<DataPoint[]> alignDataPoints(@Nullable Double precursorMzA,
      @Nullable Double precursorMzB, MZTolerance mzTol, DataPoint[] sortedB, DataPoint[] sortedA) {
    final List<DataPoint[]> aligned;
    if (precursorMzA != null && precursorMzA > 0 && precursorMzB != 0 && precursorMzB > 0) {
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
  public void process() {
    final R2RMap<RowsRelationship> mapCosineSim = new R2RMap<>();
    final R2RMap<RowsRelationship> mapNeutralLoss = new R2RMap<>();
    try {
      if (onlyBestMS2Scan) {
        checkRowsBestMs2(mapCosineSim, mapNeutralLoss, rows);
      } else {
        checkAllFeatures(mapCosineSim, mapNeutralLoss, rows);
      }
      logger.info(MessageFormat.format(
          "MS2 similarity check on rows done. MS2 modified cosine similarity edges={0}, MS2 neutral loss edges={1}",
          mapCosineSim.size(), mapNeutralLoss.size()));

      if (featureList != null) {
        R2RNetworkingMaps rowMaps = featureList.getRowMaps();
        rowMaps.addAllRowsRelationships(mapCosineSim, Type.MS2_COSINE_SIM);
        rowMaps.addAllRowsRelationships(mapNeutralLoss, Type.MS2_NEUTRAL_LOSS_SIM);
        R2RNetworkingMaps onlyCosineMap = new R2RNetworkingMaps();
        onlyCosineMap.addAllRowsRelationships(featureList.getMs2SimilarityMap().get(),
            Type.MS2_COSINE_SIM);
        addNetworkStatisticsToRows(featureList, onlyCosineMap);
      }

      logger.info("Added %d edges for %s".formatted(mapCosineSim.size(), Type.MS2_COSINE_SIM));
      if (checkNeutralLoss) {
        logger.info(
            "Added %d edges for %s".formatted(mapNeutralLoss.size(), Type.MS2_NEUTRAL_LOSS_SIM));
      }
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

  public static void addNetworkStatisticsToRows(@Nullable FeatureList featureList,
      R2RNetworkingMaps r2RNetworkingMaps) {
    // set community and cluster_index
    FeatureNetworkGenerator generator = new FeatureNetworkGenerator();
    var graph = generator.createNewGraph(featureList.getRows(), false, true, r2RNetworkingMaps,
        false);
    GraphStreamUtils.detectCommunities(graph);

    Object2IntMap<Object> communitySizes = GraphStreamUtils.getCommunitySizes(graph);
    // add cluster id
    GraphStreamUtils.detectClusters(graph, true);

    // for each node in cluster
    graph.nodes().forEach(node -> {
      if (node.getAttribute(NodeAtt.ROW.toString()) instanceof FeatureListRow row) {
        Object communityKey = node.getAttribute(COMMUNITY_ID.toString());

        int communityId = communityKey instanceof Community com ? com.id() : -1;
        int communitySize = communitySizes.getOrDefault(communityKey, 0);
        int clusterId = (int) node.getAttribute(CLUSTER_ID.toString());
        int clusterSize = (int) node.getAttribute(CLUSTER_SIZE.toString());

        var stats = new NetworkStats(clusterId, communityId, (int) node.edges().count(),
            clusterSize, communitySize);
        row.set(NetworkStatsType.class, stats);
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
  public void checkRowsBestMs2(R2RMap<RowsRelationship> mapSimilarity,
      R2RMap<RowsRelationship> mapNeutralLoss, List<FeatureListRow> rows)
      throws MissingMassListException {
    List<FilteredRowData> filteredRows = prepareRowBestSpectrum(rows);
    final int numRows = filteredRows.size();
    totalMaxPairs = Combinatorics.uniquePairs(filteredRows);
    logger.log(Level.INFO, MessageFormat.format("Checking MS2 similarity on {0} rows", numRows));

    long comparedPairs = StreamUtils.processPairs(filteredRows, this::isCanceled, true, //
        (first, later) -> maxMzDelta < later.row.getAverageMZ() - first.row.getAverageMZ(), //
        pair -> {
          // the actual processing
          checkSpectralPair(pair.left(), pair.right(), mapSimilarity, mapNeutralLoss);
          processedPairs.incrementAndGet();
        });

    // try map multi for all pairs
//    long comparedPairs = IntStream.range(0, numRows - 1).boxed()
//        .<Pair<FilteredRowData, FilteredRowData>>mapMulti((i, consumer) -> {
//          if (isCanceled()) {
//            return;
//          }
//          FilteredRowData a = filteredRows.get(i);
//          var mzA = a.row().getAverageMZ();
//          for (int j = i + 1; j < numRows; j++) {
//            FilteredRowData b = filteredRows.get(j);
//            double deltaMz = b.row().getAverageMZ() - mzA;
//            if (deltaMz > maxMzDelta) {
//              return; // out of range so stop searching
//            }
//            // within range so add to queue
//            consumer.accept(Pair.of(a, b));
//          }
//        }).parallel().mapToLong(pair -> {
//          // need to map to ensure thread is waiting for completion
//          checkSpectralPair(pair.left(), pair.right(), mapSimilarity, mapNeutralLoss);
//          // count comparisons
//          processedPairs.incrementAndGet();
//          return 1;
//        }).sum();

    logger.info("Spectral networking: Performed %d pairwise comparisons.".formatted(comparedPairs));
  }

  private boolean checkSpectralPair(final FilteredRowData a, final FilteredRowData b,
      final R2RMap<RowsRelationship> mapSimilarity, final R2RMap<RowsRelationship> mapNeutralLoss) {
    boolean result = checkR2RMs2Similarity(mapSimilarity, a.row(), b.row(), a.data(), b.data(),
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
    return result;
  }

  @NotNull
  private List<FilteredRowData> prepareRowBestSpectrum(final List<FeatureListRow> rows) {
    // required
    rows.sort(FeatureListRowSorter.MZ_ASCENDING);
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
    return filteredRows;
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
    totalMaxPairs = Combinatorics.uniquePairs(filteredRows);
    logger.log(Level.INFO,
        () -> MessageFormat.format("Checking MS2 similarity on {0} rows", numRows));

    // try map multi for all pairs
    long comparedPairs = IntStream.range(0, numRows - 1).boxed()
        .<Pair<FeatureListRow, FeatureListRow>>mapMulti((i, consumer) -> {
          if (isCanceled()) {
            return;
          }
          FeatureListRow a = filteredRows.get(i);
          var mzA = a.getAverageMZ();

          for (int j = i + 1; j < numRows; j++) {
            FeatureListRow b = filteredRows.get(j);
            double deltaMz = b.getAverageMZ() - mzA;
            if (deltaMz > maxMzDelta) {
              return; // out of range so stop searching
            }
            // within range so add to queue
            consumer.accept(Pair.of(a, b));
          }
        }).parallel().mapToLong(pair -> {
          // need to map to ensure thread is waiting for completion
          checkR2RAllFeaturesMs2Similarity(mapFeatureData, pair.left(), pair.right(), mapSimilarity,
              mapNeutralLoss);
          // count comparisons
          processedPairs.incrementAndGet();
          return 1;
        }).sum();

    logger.info(
        "Spectral networking: Performed %d pairwise comparisons of rows.".formatted(comparedPairs));
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
          Arrays.sort(massDiffA, DataPointSorter.DEFAULT_INTENSITY);
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
              Arrays.sort(massDiffB, DataPointSorter.DEFAULT_INTENSITY);
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
    // remove precursor signals
    DataPoint[] dps = signalFilter.applyFilterAndSortByIntensity(ms2, precursorMz, minDP);
    return dps != null ? new FilteredRowData(row, dps) : null;
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
  public boolean checkR2RMs2Similarity(R2RMap<RowsRelationship> mapSimilarity, FeatureListRow a,
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
      return true;
    }
    return false;
  }

  @Override
  public double getFinishedPercentage() {
    return totalMaxPairs == 0 ? 0 : processedPairs.get() / (double) totalMaxPairs;
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
