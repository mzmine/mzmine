/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.modules.dataprocessing.group_metacorrelate.msms.similarity;


import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.correlation.R2RMS2CosineSimilarity;
import io.github.mzmine.datamodel.features.correlation.R2RMS2NeutralLossSimilarity;
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import io.github.mzmine.datamodel.features.correlation.SpectralSimilarity;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.maths.similarity.Similarity;
import io.github.mzmine.util.scans.ScanAlignment;
import io.github.mzmine.util.scans.ScanMZDiffConverter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import javax.annotation.Nullable;

public class MS2SimilarityTask extends AbstractTask {

  // Logger.
  private static final Logger LOG = Logger.getLogger(MS2SimilarityTask.class.getName());

  public static Function<List<DataPoint[]>, Integer> DIFF_OVERLAP =
      list -> ScanMZDiffConverter.getOverlapOfAlignedDiff(list, 0, 1);
  public static Function<List<DataPoint[]>, Integer> SIZE_OVERLAP = List::size;

  private final AtomicDouble stageProgress;

  private final int minMatch;
  private final int minDP;
  private final MZTolerance mzTolerance;
  private final double minHeight;
  private final ModularFeatureList featureList;
  // target
  private final R2RMap<R2RMS2CosineSimilarity> mapCosineSim = new R2RMap<>();
  private final R2RMap<R2RMS2NeutralLossSimilarity> mapNeutralLoss = new R2RMap<>();
  private int maxDPForDiff = 25;
  private boolean onlyBestMS2Scan = true;
  private List<FeatureListRow> rows;
  private IonNetwork[] nets;


  public MS2SimilarityTask(final ParameterSet parameterSet,
      @Nullable ModularFeatureList featureList) {
    super(null);
    this.featureList = featureList;
    mzTolerance = parameterSet.getParameter(MS2SimilarityParameters.MZ_TOLERANCE).getValue();
    minHeight = parameterSet.getParameter(MS2SimilarityParameters.MIN_HEIGHT).getValue();
    minDP = parameterSet.getParameter(MS2SimilarityParameters.MIN_DP).getValue();
    minMatch = parameterSet.getParameter(MS2SimilarityParameters.MIN_MATCH).getValue();
    maxDPForDiff = parameterSet.getParameter(MS2SimilarityParameters.MAX_DP_FOR_DIFF).getValue();
    onlyBestMS2Scan =
        parameterSet.getParameter(MS2SimilarityParameters.ONLY_BEST_MS2_SCAN).getValue();
    stageProgress = new AtomicDouble(0);
  }

  /**
   * Create the task on set of rows
   */
  public MS2SimilarityTask(final ParameterSet parameters, @Nullable ModularFeatureList featureList,
      List<FeatureListRow> rows) {
    this(parameters, featureList);
    this.rows = rows;
  }

  /**
   * Create the task only on rows in {@link IonNetwork}s
   */
  public MS2SimilarityTask(final ParameterSet parameters, @Nullable ModularFeatureList featureList,
      IonNetwork[] nets) {
    this(parameters, featureList);
    this.nets = nets;
  }

  public static R2RMap<R2RMS2CosineSimilarity> checkNetworks(AbstractTask task,
      AtomicDouble stageProgress,
      R2RMap<R2RMS2CosineSimilarity> map,
      R2RMap<R2RMS2NeutralLossSimilarity> mapNeutralLoss,
      FeatureList featureList, IonNetwork[] nets,
      MZTolerance mzTolerance, double minHeight, int minDP, int minMatch, int maxDPForDiff,
      boolean onlyBestMS2Scan) {
    // get all rows of all networks
    Map<Integer, FeatureListRow> rows = new HashMap<>();
    Arrays.stream(nets).flatMap(n -> n.keySet().stream()).forEach(r -> rows.put(r.getID(), r));
    List<FeatureListRow> allRows = new ArrayList<>(rows.values());

    // add all to this map
    checkRows(task, stageProgress, map, mapNeutralLoss,
        allRows, mzTolerance,
        minHeight, minMatch, minDP, maxDPForDiff, onlyBestMS2Scan);

    featureList.addRowsRelationships(map, Type.MS2_COSINE_SIM);
    return map;
  }

  /**
   * Parallel check of all r2r similarities
   *
   * @param mapSimilarity  map for all MS2 cosine similarity edges
   * @param mapNeutralLoss map for all neutral loss MS2 edges
   * @param rows           match rows
   * @param mzTolerance    tolerance to match signals
   * @param minHeight      minimum height to generate the list of neutral losses
   * @param minDP          minimum number of data points
   * @param minMatch       minimum number of matched data points
   * @param maxDPForDiff   maximum number of data points to generate the neutral loss list
   */
  public static void checkRows(AbstractTask task,
      AtomicDouble stageProgress,
      R2RMap<R2RMS2CosineSimilarity> mapSimilarity,
      R2RMap<R2RMS2NeutralLossSimilarity> mapNeutralLoss,
      List<FeatureListRow> rows, MZTolerance mzTolerance, double minHeight, int minDP,
      int minMatch, int maxDPForDiff, boolean onlyBestMS2Scan) {
    // prefilter rows: has MS2 and in case only best MS2 is considered - check minDP
    FeatureListRow[] filteredRows = rows.stream()
        .filter(row -> filterRow(row, onlyBestMS2Scan, minDP)).toArray(FeatureListRow[]::new);
    int numRows = filteredRows.length;

    LOG.log(Level.INFO, "Checking MS2 similarity on {0} rows", numRows);

    IntStream.range(0, numRows - 1).parallel().forEach(i -> {
      if (task == null || !task.isCanceled()) {
        for (int j = i + 1; j < numRows; j++) {
          if (task == null || !task.isCanceled()) {
            checkR2RMs2Similarity(mapSimilarity, mapNeutralLoss,
                filteredRows[i], filteredRows[j], mzTolerance, minHeight,
                minDP, minMatch, maxDPForDiff, onlyBestMS2Scan);
          }
        }
      }
      if (stageProgress != null) {
        stageProgress.getAndAdd(1d / numRows);
      }
    });
    LOG.info(
        MessageFormat.format(
            "MS2 similarity check on rows done. MS2 cosine similarity={0}, MS2 neutral loss={1}",
            mapSimilarity.size(), mapNeutralLoss.size()));
  }

  /**
   * Checks the minimum requirements for a row to be matched by MS2 similarity (minimum number of
   * data points and MS2 data availability)
   *
   * @param row             the test row
   * @param onlyBestMS2Scan use only the best MS2 scan
   * @param minDP           minimum number of data points in mass list
   * @return true if the row matches all criteria
   */
  private static boolean filterRow(FeatureListRow row, boolean onlyBestMS2Scan, int minDP) {
    if (!row.hasMs2Fragmentation()) {
      return false;
    } else if (onlyBestMS2Scan) {
      return row.getBestFragmentation().getMassList().getNumberOfDataPoints() < minDP;
    } else {
      for (Feature feature : row.getFeatures()) {
        Scan ms2 = feature.getMostIntenseFragmentScan();
        if (ms2 != null && ms2.getMassList().getNumberOfDataPoints() >= minDP) {
          return true;
        }
      }
      // no feature had an MS2 with >= minDP
      return false;
    }
  }

  /**
   * @param mapSimilarity   map to add new MS2 cosine similarity edges to
   * @param mapNeutralLoss  map to add new MS2 neutral loss similarity edges to
   * @param a               row a
   * @param b               row b
   * @param mzTolerance     mz tolerance to align signals
   * @param minHeight       minimum height is only used to limit the number of signals to generate
   *                        the neutral loss map
   * @param minDP           minimum number of datapoints
   * @param minMatch        minimum number of matched data points
   * @param maxDPForDiff    maximum number of data points to generate the neutral loss list
   * @param onlyBestMS2Scan use only the best MS2 scan for each row - otherwise (false) use the best
   *                        MS2 for each feature in this row
   * @return
   */
  public static void checkR2RMs2Similarity(
      R2RMap<R2RMS2CosineSimilarity> mapSimilarity,
      R2RMap<R2RMS2NeutralLossSimilarity> mapNeutralLoss,
      FeatureListRow a, FeatureListRow b,
      MZTolerance mzTolerance, double minHeight, int minDP, int minMatch, int maxDPForDiff,
      boolean onlyBestMS2Scan) {
    R2RMS2CosineSimilarity cosineSim = new R2RMS2CosineSimilarity(a, b);
    R2RMS2NeutralLossSimilarity neutralLossSim = new R2RMS2NeutralLossSimilarity(a, b);
    // only check best fragmentation scans
    if (onlyBestMS2Scan) {
      Scan scana = a.getBestFragmentation();
      Scan scanb = b.getBestFragmentation();
      if (scana != null && scanb != null) {
        MassList massa = scana.getMassList();
        MassList massb = scanb.getMassList();
        if (massa != null && massb != null) {
          DataPoint[] dpa = massa.getDataPoints();
          DataPoint[] dpb = massb.getDataPoints();
          if (dpa != null && dpa.length >= minDP && dpb != null && dpb.length >= minDP) {
            // create mass diff array
            DataPoint[] massDiffA =
                ScanMZDiffConverter.getAllMZDiff(dpa, mzTolerance, minHeight, maxDPForDiff);
            DataPoint[] massDiffB =
                ScanMZDiffConverter.getAllMZDiff(dpb, mzTolerance, maxDPForDiff);
            // alignment and sim of neutral losses
            SpectralSimilarity massDiffSim =
                createMS2Sim(mzTolerance, massDiffA, massDiffB, minMatch, DIFF_OVERLAP);

            // align and check spectra
            SpectralSimilarity spectralSim = createMS2Sim(mzTolerance, dpa, dpb, minMatch,
                SIZE_OVERLAP);

            if (massDiffSim != null) {
              neutralLossSim.addSpectralSim(massDiffSim);
            }
            if (spectralSim != null) {
              cosineSim.addSpectralSim(spectralSim);
            }
          }
        }
      }
    } else {
      for (Feature fa : a.getFeatures()) {
        DataPoint[] dpa = getMassList(fa);
        if (dpa != null && dpa.length >= minDP) {
          // create mass diff array
          DataPoint[] massDiffA =
              ScanMZDiffConverter.getAllMZDiff(dpa, mzTolerance, minHeight, maxDPForDiff);
          for (Feature fb : b.getFeatures()) {
            DataPoint[] dpb = getMassList(fb);
            if (dpb != null && dpb.length >= minDP) {
              // align and check spectra
              SpectralSimilarity spectralSim =
                  createMS2Sim(mzTolerance, dpa, dpb, minMatch, SIZE_OVERLAP);

              // alignment and sim of neutral losses
              DataPoint[] massDiffB =
                  ScanMZDiffConverter.getAllMZDiff(dpb, mzTolerance, maxDPForDiff);
              SpectralSimilarity massDiffSim =
                  createMS2Sim(mzTolerance, massDiffA, massDiffB, minMatch, DIFF_OVERLAP);

              if (massDiffSim != null) {
                neutralLossSim.addSpectralSim(massDiffSim);
              }
              if (spectralSim != null) {
                cosineSim.addSpectralSim(spectralSim);
              }
            }
          }
        }
      }
    }
    if (cosineSim.size() > 0) {
      mapSimilarity.add(a, b, cosineSim);
    }
    if (neutralLossSim.size() > 0) {
      mapNeutralLoss.add(a, b, neutralLossSim);
    }
  }

  /**
   * @param minMatch        minimum overlapping signals in the two mass lists a and b
   * @param overlapFunction different functions to determin the size of overlap
   * @return the spectral similarity if number of overlapping signals >= minimum, else null
   */
  @Nullable
  private static SpectralSimilarity createMS2Sim(MZTolerance mzTol, DataPoint[] a, DataPoint[] b,
      double minMatch, Function<List<DataPoint[]>, Integer> overlapFunction) {
    // align
    List<DataPoint[]> aligned = ScanAlignment.align(mzTol, b, a);
    aligned = ScanAlignment.removeUnaligned(aligned);
    // overlapping mass diff
    int overlap = overlapFunction.apply(aligned);

    if (overlap >= minMatch) {
      // cosine
      double[][] diffArray = ScanAlignment.toIntensityArray(aligned);
      double diffCosine = Similarity.COSINE.calc(diffArray);
      return new SpectralSimilarity(diffCosine, overlap);
    }
    return null;
  }

  private static DataPoint[] getMassList(Feature fa) {
    Scan msmsScan = fa.getMostIntenseFragmentScan();
    if (msmsScan == null) {
      return null;
    }
    return msmsScan.getMassList()
        .getDataPoints();
  }

  @Override
  public double getFinishedPercentage() {
    return stageProgress.get();
  }

  @Override
  public String getTaskDescription() {
    return "Check similarity of MSMS scans (mass lists)";
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    if (nets != null) {
      checkNetworks(this, stageProgress, mapCosineSim, mapNeutralLoss, featureList, nets,
          mzTolerance,
          minHeight, minDP, minMatch, maxDPForDiff, onlyBestMS2Scan);
    } else if (rows != null) {
      checkRows(this, stageProgress, mapCosineSim, mapNeutralLoss, rows, mzTolerance, minHeight,
          minDP,
          minMatch, maxDPForDiff, onlyBestMS2Scan);
    }

    if (featureList != null) {
      featureList.addRowsRelationships(mapCosineSim, Type.MS2_COSINE_SIM);
      featureList.addRowsRelationships(mapNeutralLoss, Type.MS2_NEUTRAL_LOSS_SIM);
    }

    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Resulting map of row-2-row MS2 spectral cosine similarities
   *
   * @return cosine similarity map
   */
  public R2RMap<R2RMS2CosineSimilarity> getMapCosineSim() {
    return mapCosineSim;
  }

  /**
   * Resulting map of row-2-row MS2 neutral loss similarities
   *
   * @return neutral loss similarity map
   */
  public R2RMap<R2RMS2NeutralLossSimilarity> getMapNeutralLoss() {
    return mapNeutralLoss;
  }

}
