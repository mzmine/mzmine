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

package io.github.mzmine.modules.dataprocessing.group_spectral_networking.modified_cosine;


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
import io.github.mzmine.datamodel.features.types.networking.NetworkStats;
import io.github.mzmine.datamodel.features.types.networking.NetworkStatsType;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.CosinePairContributions;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.MainSpectralNetworkingParameters;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.NetworkCluster;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.SignalAlignmentAnnotation;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.SpectralSignalFilter;
import io.github.mzmine.modules.visualization.networking.visual.FeatureNetworkGenerator;
import io.github.mzmine.modules.visualization.networking.visual.enums.NodeAtt;
import static io.github.mzmine.modules.visualization.networking.visual.enums.NodeAtt.CLUSTER_ID;
import static io.github.mzmine.modules.visualization.networking.visual.enums.NodeAtt.CLUSTER_SIZE;
import static io.github.mzmine.modules.visualization.networking.visual.enums.NodeAtt.COMMUNITY_ID;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.GraphStreamUtils;
import io.github.mzmine.util.collections.CollectionUtils;
import io.github.mzmine.util.collections.StreamUtils;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.maths.Combinatorics;
import io.github.mzmine.util.maths.similarity.Similarity;
import io.github.mzmine.util.scans.FragmentScanSelection;
import io.github.mzmine.util.scans.ScanAlignment;
import io.github.mzmine.util.scans.ScanMZDiffConverter;
import io.github.mzmine.util.scans.similarity.Weights;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import static java.util.Objects.requireNonNullElse;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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
  private final @Nullable ModularFeatureList featureList;
  // target
  private final SpectralSignalFilter signalFilter;
  private final double maxMzDelta;
  private final List<FeatureListRow> mzSortedRows;
  private long totalMaxPairs = 0;
  // this is always off for now. Could be reintroduced as separate similarity metric
  private final @NotNull FragmentScanSelection scanMergeSelect;

  public ModifiedCosineSpectralNetworkingTask(final ParameterSet mainParameters,
      @NotNull ModularFeatureList featureList, @NotNull Instant moduleCallDate,
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
    // make sure to copy rows and sort them by mz
    // copy to not change original sorting
    this.mzSortedRows = rows.stream().sorted(FeatureListRowSorter.MZ_ASCENDING)
        .collect(CollectionUtils.toArrayList());
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
    scanMergeSelect = subParams.getParameter(
            ModifiedCosineSpectralNetworkingParameters.spectraMergeSelect)
        .createFragmentScanSelection(getMemoryMapStorage());
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
    try {
      checkRows(mapCosineSim, mzSortedRows);
      logger.info(MessageFormat.format(
          "MS2 similarity check on rows done. MS2 modified cosine similarity edges={0}",
          mapCosineSim.size()));

      if (featureList != null) {
        R2RNetworkingMaps rowMaps = featureList.getRowMaps();
        rowMaps.addAllRowsRelationships(mapCosineSim, Type.MS2_COSINE_SIM);
        R2RNetworkingMaps onlyCosineMap = new R2RNetworkingMaps();
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

  public static void addNetworkStatisticsToRows(@Nullable FeatureList featureList,
      R2RNetworkingMaps r2RNetworkingMaps) {
    if (featureList == null) {
      return;
    }
    // set community and cluster_index
    FeatureNetworkGenerator generator = new FeatureNetworkGenerator();
    var graph = generator.createNewGraph(featureList.getRows(), false, true, r2RNetworkingMaps,
        false);
    final Map<Integer, Integer> communitySizes = GraphStreamUtils.detectCommunities(graph).stream()
        .collect(Collectors.toMap(NetworkCluster::id, NetworkCluster::size));

    // add cluster id
    GraphStreamUtils.detectClusters(graph, true);

    // for each node in cluster
    graph.nodes().forEach(node -> {
      if (node.getAttribute(NodeAtt.ROW.toString()) instanceof FeatureListRow row) {
        int communityId = requireNonNullElse((Integer) node.getAttribute(COMMUNITY_ID.toString()),
            -1);

        int communitySize = communitySizes.getOrDefault(communityId, 0);
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
   * @param mapSimilarity map for all MS2 cosine similarity edges
   * @param mzSortedRows  match rows
   */
  public void checkRows(R2RMap<RowsRelationship> mapSimilarity, List<FeatureListRow> mzSortedRows)
      throws MissingMassListException {
    List<Entry<FeatureListRow, List<FilteredRowData>>> sortedFilteredRows = new ArrayList<>(
        prepareRowSpectra(mzSortedRows).entrySet());
    final int numRows = sortedFilteredRows.size();
    totalMaxPairs = Combinatorics.uniquePairs(sortedFilteredRows.size());
    logger.log(Level.INFO, MessageFormat.format("Checking MS2 similarity on {0} rows", numRows));

    long comparedPairs = StreamUtils.processPairs(sortedFilteredRows, this::isCanceled, true, //
        // stop inner loop if mz distance is too far
        (first, later) -> maxMzDelta < later.getKey().getAverageMZ() - first.getKey()
            .getAverageMZ(), //
        pair -> {
          // the actual processing
          var row1 = pair.left().getKey();
          var scans1 = pair.left().getValue();
          var row2 = pair.right().getKey();
          var scans2 = pair.right().getValue();
          checkRowsPair(row1, scans1, row2, scans2, mapSimilarity);
          processedPairs.incrementAndGet();
        });

    logger.info("Spectral networking: Performed %d pairwise comparisons.".formatted(comparedPairs));
  }

  /**
   * Check and add {@link R2RSpectralSimilarity} to mapSimilarity.
   *
   * @param mapSimilarity results to be added
   * @return true if relationship was added
   */
  private boolean checkRowsPair(final FeatureListRow row1, final List<FilteredRowData> scans1,
      final FeatureListRow row2, final List<FilteredRowData> scans2,
      final R2RMap<RowsRelationship> mapSimilarity) {
    SpectralSimilarity best = null;

    // TODO think about ways to match the same energies against each other
    // Maybe in a +- 15 energy range
    // currently this just matches all scans against all scans and takes the best score
    for (final FilteredRowData a : scans1) {
      for (final FilteredRowData b : scans2) {
        // align and check spectra
        var result = calcSpectralSimilarity(a.row(), b.row(), a.data(), b.data());
        if (result != null && (best == null || result.cosine() > best.cosine())) {
          best = result;
        }
      }
    }
    if (best != null && best.cosine() >= minCosineSimilarity && best.overlap() >= minMatch) {
      var r2r = new R2RSpectralSimilarity(row1, row2, Type.MS2_COSINE_SIM, best);
      mapSimilarity.add(row1, row2, r2r);
      return true;
    }
    return false;
  }

  private Map<FeatureListRow, List<FilteredRowData>> prepareRowSpectra(
      final List<FeatureListRow> mzSortedRows) {
    // prefilter rows: has MS2 and in case only best MS2 is considered - check minDP
    // and prepare data points
    // retain order of mz in LinkedHashMap
    Map<FeatureListRow, List<FilteredRowData>> filteredRows = new LinkedHashMap<>();
    for (FeatureListRow row : mzSortedRows) {
      List<Scan> selectedScans = scanMergeSelect.getAllFragmentSpectra(row);
      if (selectedScans.isEmpty()) {
        continue;
      }
      List<FilteredRowData> prepared = new ArrayList<>();

      // prepare all scans
      for (final Scan scan : selectedScans) {
        FilteredRowData data = getDataAndFilter(row, scan, row.getAverageMZ(), minMatch);
        if (data != null) {
          prepared.add(data);
        }
      }
      // add row if prepared
      if (!prepared.isEmpty()) {
        filteredRows.put(row, prepared);
      }
    }
    return filteredRows;
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


  private @Nullable SpectralSimilarity calcSpectralSimilarity(final FeatureListRow a,
      final FeatureListRow b, final DataPoint[] sortedA, final DataPoint[] sortedB) {
    return createMS2SimModificationAware(mzTolerance, sortedA, sortedB, minMatch, SIZE_OVERLAP,
        a.getAverageMZ(), b.getAverageMZ());
  }

  @Override
  public double getFinishedPercentage() {
    return totalMaxPairs == 0 ? 0 : processedPairs.get() / (double) totalMaxPairs;
  }

  @Override
  public String getTaskDescription() {
    return "Check similarity of MSMS scans (mass lists)";
  }

}
