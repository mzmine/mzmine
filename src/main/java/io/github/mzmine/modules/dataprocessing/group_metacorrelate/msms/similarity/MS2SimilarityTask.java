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
import io.github.mzmine.datamodel.features.RowGroupList;
import io.github.mzmine.datamodel.features.correlation.MS2Similarity;
import io.github.mzmine.datamodel.features.correlation.MS2SimilarityProviderGroup;
import io.github.mzmine.datamodel.features.correlation.R2RMS2Similarity;
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.maths.similarity.Similarity;
import io.github.mzmine.util.scans.ScanAlignment;
import io.github.mzmine.util.scans.ScanMZDiffConverter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class MS2SimilarityTask extends AbstractTask {

  // Logger.
  private static final Logger LOG = Logger.getLogger(MS2SimilarityTask.class.getName());

  public static Function<List<DataPoint[]>, Integer> DIFF_OVERLAP =
      list -> ScanMZDiffConverter.getOverlapOfAlignedDiff(list, 0, 1);
  public static Function<List<DataPoint[]>, Integer> SIZE_OVERLAP = List::size;

  private AtomicDouble stageProgress;

  private int minMatch;
  private int minDP;
  private int maxDPForDiff = 25;
  private boolean onlyBestMS2Scan = true;
  private MZTolerance mzTolerance;
  private double minHeight;

  private List<FeatureListRow> rows;
  private RowGroupList groups;
  private MS2SimilarityProviderGroup group;
  private IonNetwork[] nets;

  // target
  private R2RMap<R2RMS2Similarity> map;

  private ModularFeatureList featureList;


  /**
   * @param parameterSet
   */
  public MS2SimilarityTask(final ParameterSet parameterSet, ModularFeatureList featureList) {
    super(featureList.getMemoryMapStorage());
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
   * Create the task. to run on list of groups
   *
   * @param parameterSet the parameters.
   */
  public MS2SimilarityTask(final ParameterSet parameterSet, ModularFeatureList featureList,
      RowGroupList groups) {
    this(parameterSet, featureList);
    // performed on groups
    this.groups = groups;
  }

  /**
   * Create the task on set of rows
   *
   * @param parameterSet the parameters.
   */
  public MS2SimilarityTask(final ParameterSet parameterSet, ModularFeatureList featureList,
      List<FeatureListRow> rows) {
    this(parameterSet, featureList);
    this.rows = rows;
  }

  /**
   * Create the task on single group (the result is automatically set to the group
   *
   * @param parameterSet the parameters.
   */
  public MS2SimilarityTask(final ParameterSet parameterSet, ModularFeatureList featureList,
      MS2SimilarityProviderGroup group) {
    this(parameterSet, featureList);
    this.group = group;
  }

  public MS2SimilarityTask(ParameterSet parameters, ModularFeatureList featureList,
      IonNetwork[] nets) {
    this(parameters, featureList);
    this.nets = nets;
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
    doCheck();
    setStatus(TaskStatus.FINISHED);
  }

  public void doCheck() {
    if (nets != null) {
      map = checkNetworks(this, stageProgress, featureList, nets, mzTolerance, minHeight,
          minDP, minMatch, maxDPForDiff, onlyBestMS2Scan);
    } else if (group != null) {
      map = checkGroup(this, stageProgress, group, mzTolerance, minHeight, minDP,
          minMatch, maxDPForDiff, onlyBestMS2Scan);
    } else if (rows != null) {
      map = checkRows(this, stageProgress, rows, mzTolerance, minHeight, minDP, minMatch,
          maxDPForDiff, onlyBestMS2Scan);
    } else if (groups != null) {
      checkGroupList(this, stageProgress, groups, mzTolerance, minHeight, minDP, minMatch,
          maxDPForDiff, onlyBestMS2Scan);
    }

    if (featureList != null && map != null) {
      featureList.addR2RSimilarity(map);
    }
  }


  /**
   * Resulting map
   *
   * @return
   */
  public R2RMap<R2RMS2Similarity> getMap() {
    return map;
  }


  public void checkGroupList(AbstractTask task, AtomicDouble stageProgress, RowGroupList groups) {
    checkGroupList(task, stageProgress, groups, mzTolerance, minHeight, minDP, minMatch,
        maxDPForDiff, onlyBestMS2Scan);
  }

  public static void checkGroupList(AbstractTask task, AtomicDouble stageProgress,
      RowGroupList groups, MZTolerance mzTolerance, double minHeight, int minDP,
      int minMatch, int maxDPForDiff, boolean onlyBestMS2Scan) {
    LOG.info("Calc MS/MS similarity of groups");
    final int size = groups.size();
    groups.parallelStream().forEach(g -> {
      if (!task.isCanceled()) {
        if (g instanceof MS2SimilarityProviderGroup) {
          checkGroup(task, stageProgress, (MS2SimilarityProviderGroup) g, mzTolerance,
              minHeight, minDP, minMatch, maxDPForDiff, onlyBestMS2Scan);
        }
        stageProgress.addAndGet(1d / size);
      }
    });
  }

  /**
   * Checks for MS2 similarity of all rows in a group. the resulting map is set to the groups3
   */
  public R2RMap<R2RMS2Similarity> checkGroup(MS2SimilarityProviderGroup g) {
    return checkGroup(null, null, g, mzTolerance, minHeight, minDP, minMatch,
        maxDPForDiff, onlyBestMS2Scan);
  }


  public static R2RMap<R2RMS2Similarity> checkNetworks(AbstractTask task,
      AtomicDouble stageProgress, FeatureList featureList, IonNetwork[] nets,
      MZTolerance mzTolerance, double minHeight, int minDP, int minMatch, int maxDPForDiff,
      boolean onlyBestMS2Scan) {
    // get all rows of all networks
    Map<Integer, FeatureListRow> rows = new HashMap<>();
    Arrays.stream(nets).flatMap(n -> n.keySet().stream()).forEach(r -> rows.put(r.getID(), r));
    List<FeatureListRow> allRows = new ArrayList <>(rows.values());

    // add all to this map
    R2RMap<R2RMS2Similarity> map = checkRows(task, stageProgress, allRows, mzTolerance,
        minHeight, minMatch, minDP, maxDPForDiff, onlyBestMS2Scan);

    featureList.addR2RSimilarity(map);
    return map;
  }

  /**
   * Checks for MS2 similarity of all rows in a group. the resulting map is set to the groups3
   *
   * @param g
   * @param mzTolerance
   * @param minMatch
   * @param minDP
   * @param maxDPForDiff
   * @return
   */
  public static R2RMap<R2RMS2Similarity> checkGroup(AbstractTask task, AtomicDouble stageProgress,
      MS2SimilarityProviderGroup g, MZTolerance mzTolerance, double minHeight,
      int minDP, int minMatch, int maxDPForDiff, boolean onlyBestMS2Scan) {
    R2RMap<R2RMS2Similarity> map =
        checkRows(task, stageProgress, g, mzTolerance,
            minHeight, minMatch, minDP, maxDPForDiff, onlyBestMS2Scan);

    g.setMS2SimilarityMap(map);
    return map;
  }

  /**
   * Parallel check of all r2r similarities
   *
   * @param rows
   * @param mzTolerance
   * @param minHeight
   * @param minDP
   * @param minMatch
   * @param maxDPForDiff
   * @return
   */
  public static R2RMap<R2RMS2Similarity> checkRows(AbstractTask task, AtomicDouble stageProgress,
      List<FeatureListRow> rows, MZTolerance mzTolerance, double minHeight, int minDP,
      int minMatch, int maxDPForDiff, boolean onlyBestMS2Scan) {
    LOG.info("Checking MS2 similarity on " + rows.size() + " rows");
    R2RMap<R2RMS2Similarity> map = new R2RMap<>();

    IntStream.range(0, rows.size() - 1).parallel().forEach(i -> {
      if (task == null || !task.isCanceled()) {
        R2RMap<R2RMS2Similarity> submap = null;
        for (int j = i + 1; j < rows.size(); j++) {
          if (task == null || !task.isCanceled()) {
            R2RMS2Similarity r2r = checkR2R(rows.get(i), rows.get(j), mzTolerance, minHeight,
                minDP, minMatch, maxDPForDiff, onlyBestMS2Scan);
            if (r2r != null) {
              if (submap == null) {
                submap = new R2RMap<>();
              }
              submap.add(rows.get(i), rows.get(j), r2r);
            }
          }
        }
        // bundle to lower synchronizing
        if (submap != null) {
          map.putAll(submap);
        }
      }
      if (stageProgress != null) {
        stageProgress.getAndAdd(1d / rows.size());
      }
    });
    LOG.info("MS2 similarity check on rows done. R2R similarity=" + map.size());
    return map;
  }

  public static R2RMS2Similarity checkR2R(FeatureListRow a, FeatureListRow b,
      MZTolerance mzTolerance, double minHeight, int minDP, int minMatch, int maxDPForDiff,
      boolean onlyBestMS2Scan) {
    R2RMS2Similarity r2r = new R2RMS2Similarity(a, b);
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
            MS2Similarity massDiffSim =
                createMS2Sim(mzTolerance, massDiffA, massDiffB, minMatch, DIFF_OVERLAP);

            // align and check spectra
            MS2Similarity spectralSim = createMS2Sim(mzTolerance, dpa, dpb, minMatch, SIZE_OVERLAP);

            if (massDiffSim != null) {
              r2r.addMassDiffSim(massDiffSim);
            }
            if (spectralSim != null) {
              r2r.addSpectralSim(spectralSim);
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
              MS2Similarity spectralSim =
                  createMS2Sim(mzTolerance, dpa, dpb, minMatch, SIZE_OVERLAP);

              // alignment and sim of neutral losses
              DataPoint[] massDiffB =
                  ScanMZDiffConverter.getAllMZDiff(dpb, mzTolerance, maxDPForDiff);
              MS2Similarity massDiffSim =
                  createMS2Sim(mzTolerance, massDiffA, massDiffB, minMatch, DIFF_OVERLAP);

              if (massDiffSim != null) {
                r2r.addMassDiffSim(massDiffSim);
              }
              if (spectralSim != null) {
                r2r.addSpectralSim(spectralSim);
              }
            }
          }
        }
      }
    }
    return r2r.size() > 0 ? r2r : null;
  }

  /**
   * @param mzTol
   * @param a
   * @param b
   * @param minMatch        minimum overlapping signals in the two mass lists a and b
   * @param overlapFunction different functions to determin the size of overlap
   * @return
   */
  private static MS2Similarity createMS2Sim(MZTolerance mzTol, DataPoint[] a, DataPoint[] b,
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
      return new MS2Similarity(diffCosine, overlap);
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

}
