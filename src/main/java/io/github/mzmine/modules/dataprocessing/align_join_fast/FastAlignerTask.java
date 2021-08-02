/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.dataprocessing.align_join_fast;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreCalculator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;

public class FastAlignerTask extends AbstractTask {

  private final MZmineProject project;
  private final Logger logger = Logger.getLogger(this.getClass().getName());
  /**
   * All feature lists except the base list
   */
  private List<ModularFeatureList> featureLists;
  private ModularFeatureList alignedFeatureList;

  // Processed rows counter
  private int totalRows;
  private final AtomicInteger processedRows = new AtomicInteger(0);

  private final ParameterSet isotopeParams;
  private final String featureListName;
  private final MZTolerance mzTolerance;
  private final RTTolerance rtTolerance;
  private final MobilityTolerance mobilityTolerance;
  private final double mzWeight;
  private final double rtWeight;
  private final double mobilityWeight;
  private final boolean sameIDRequired;
  private final boolean sameChargeRequired;
  private final boolean compareIsotopePattern;
  private final boolean compareSpectraSimilarity;
  private final ParameterSet parameters;

  // ID counter for the new peaklist
  private int newRowID = 1;


  // fields for spectra similarity
  private MZmineProcessingStep<SpectralSimilarityFunction> simFunction;
  private int msLevel;
  private final boolean compareMobility;

  public FastAlignerTask(MZmineProject project, ParameterSet parameters,
      @Nullable MemoryMapStorage storage) {
    super(storage);

    this.project = project;
    this.parameters = parameters;

    featureLists = Arrays.stream(parameters.getParameter(FastAlignerParameters.peakLists).getValue()
        .getMatchingFeatureLists()).toList();

    featureListName = parameters.getParameter(FastAlignerParameters.peakListName).getValue();

    mzTolerance = parameters.getParameter(FastAlignerParameters.MZTolerance).getValue();
    rtTolerance = parameters.getParameter(FastAlignerParameters.RTTolerance).getValue();

    compareMobility = parameters.getParameter(FastAlignerParameters.mobilityTolerance).getValue();
    mobilityTolerance = parameters.getParameter(FastAlignerParameters.mobilityTolerance)
        .getEmbeddedParameter().getValue();

    mzWeight = parameters.getParameter(FastAlignerParameters.MZWeight).getValue();
    rtWeight = parameters.getParameter(FastAlignerParameters.RTWeight).getValue();
    mobilityWeight = parameters.getParameter(FastAlignerParameters.mobilityWeight).getValue();

    sameChargeRequired = parameters.getParameter(FastAlignerParameters.SameChargeRequired)
        .getValue();

    sameIDRequired = parameters.getParameter(FastAlignerParameters.SameIDRequired).getValue();
    compareIsotopePattern = parameters.getParameter(FastAlignerParameters.compareIsotopePattern)
        .getValue();
    isotopeParams = parameters.getParameter(FastAlignerParameters.compareIsotopePattern)
        .getEmbeddedParameters();
    compareSpectraSimilarity = parameters
        .getParameter(FastAlignerParameters.compareSpectraSimilarity).getValue();

    if (compareSpectraSimilarity) {
      simFunction = parameters.getParameter(FastAlignerParameters.compareSpectraSimilarity)
          .getEmbeddedParameters()
          .getParameter(FastAlignerSpectraSimilarityScoreParameters.similarityFunction).getValue();
      msLevel = parameters.getParameter(FastAlignerParameters.compareSpectraSimilarity)
          .getEmbeddedParameters().getParameter(FastAlignerSpectraSimilarityScoreParameters.msLevel)
          .getValue();
    }
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Join aligner, " + featureListName + " (" + (featureLists.size() + 1)
        + " feature lists)";
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0) {
      return 0f;
    }
    return (double) processedRows.get() / (double) totalRows;
  }

  /**
   * @see Runnable#run()
   */
  @Override
  public void run() {

    if ((mzWeight == 0) && (rtWeight == 0)) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Cannot run alignment, all the weight parameters are zero");
      return;
    }

    setStatus(TaskStatus.PROCESSING);
    logger.info(() -> "Running join aligner on " + (featureLists.size() + 1) + " feature lists.");

    // Remember how many rows we need to process. Each row will be processed
    // twice, first for score calculation, second for actual alignment.
    for (FeatureList list : featureLists) {
      totalRows += list.getNumberOfRows() * 2;
    }

    // Collect all data files
    final List<RawDataFile> allDataFiles = getAllDataFiles();
    if (allDataFiles == null) {
      return;
    }

    // Create a new aligned feature list based on the baseList and renumber IDs
    alignedFeatureList = new ModularFeatureList(featureListName, getMemoryMapStorage(),
        allDataFiles);
    final AtomicInteger newRowID = new AtomicInteger(1);

    // get all rows of all feature lists.
    final List<FeatureListRow> unalignedRows = featureLists.stream()
        .flatMap(flist -> flist.stream()).toList();

    // contains all files that potentially have unaligned features.
    final List<ModularFeatureList> leftoverFlists = new ArrayList<>(featureLists);
    boolean alignmentDone = false;

    // Contains all rows without a match in the aligned feature list. All rows for now, since there
    // has been no alignment yet
    final List<FeatureListRow> leftoverRows = Collections
        .synchronizedList(new ArrayList<>(unalignedRows));

    while (leftoverFlists.size() > 1) {
      // select the next base feature list, and get all rows from that feature list from our list
      // of rows. We use the flist with the most rows first.
      final ModularFeatureList nextBaseList = leftoverFlists.stream()
          .sorted((l1, l2) -> Integer.compare(l1.getNumberOfRows(), l2.getNumberOfRows()) * -1)
          .toList().get(0);
      leftoverFlists.remove(nextBaseList);

      // the base rows are the rows we align the features on to.
      List<FeatureListRow> nextBaseRows = leftoverRows.stream()
          .filter(row -> row.getFeatureList().equals(nextBaseList)).toList();
      nextBaseRows.sort(new FeatureListRowSorter(SortingProperty.MZ, SortingDirection.Ascending));

      // remove base rows from the leftover rows.
      leftoverRows.removeIf(
          row -> row.getFeatureList().equals(nextBaseList)); // removeIf faster than removeAll?

      // unaligned rows are our rows that shall be matched to the nextBaseRows. The leftover rows
      // is supposed to contain all rows that have not been aligned during the method call.
      unalignedRows.clear();
      unalignedRows.addAll(leftoverRows);
      leftoverRows.clear();

      alignRowsOnBaseRows(unalignedRows, nextBaseRows, leftoverRows);

      // todo base rows have to be copied and added to the new feature list.
    }

    // Add new aligned feature list to the project {
    project.addFeatureList(alignedFeatureList);

    // Add task description to peakList
    alignedFeatureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("Join aligner", FastAlignerModule.class, parameters));

    logger.info("Finished join aligner");

    setStatus(TaskStatus.FINISHED);

  }

  private void alignRowsOnBaseRows(List<FeatureListRow> unalignedRows,
      List<FeatureListRow> baseRowsByMz, List<FeatureListRow> leftoverRows) {

    final Map<FeatureListRow, Boolean> assignedRows = new ConcurrentHashMap<>();
    unalignedRows.forEach(row -> assignedRows.put(row, false));

    // key = a row to be aligned, value = all possible matches in the aligned fl and it's scores
    final Map<FeatureListRow, List<RowVsRowScore>> rowToScoreMap = new ConcurrentHashMap<>();
    final List<RowVsRowScore> scoresList = Collections.synchronizedList(new ArrayList<>());

    unalignedRows.parallelStream().forEach(row -> {
      if (isCanceled()) {
        return;
      }

      final Range<Double> mzRange =
          mzWeight > 0 ? mzTolerance.getToleranceRange(row.getAverageMZ()) : Range.all();
      final Range<Float> rtRange =
          rtWeight > 0 ? rtTolerance.getToleranceRange(row.getAverageRT()) : Range.all();
      final Range<Float> mobilityRange =
          compareMobility && row.getAverageMobility() != null ? mobilityTolerance
              .getToleranceRange(row.getAverageMobility()) : Range.singleton(0f);

      // find all rows in the aligned rows that might match
      final List<FeatureListRow> candidatesInAligned = FeatureListUtils
          .getRows(baseRowsByMz, rtRange, mzRange, true);

      if (candidatesInAligned.isEmpty()) {
        return;
      }

      final List<RowVsRowScore> rowVsRowScores = rowToScoreMap
          .computeIfAbsent(row, r -> new ArrayList<>());

      for (FeatureListRow candidateInAligned : candidatesInAligned) {
        if (checkMZ(candidateInAligned, mzRange) && checkRT(candidateInAligned, rtRange)
            && checkMobility(candidateInAligned, mobilityRange) && (!sameChargeRequired
            || FeatureUtils.compareChargeState(row, candidateInAligned)) && (!sameIDRequired
            || FeatureUtils.compareIdentities(row, candidateInAligned))
            && checkIsotopePattern(/*isotopePatternMap,*/ row, candidateInAligned)
            && checkSpectralSimilarity(row, candidateInAligned)) {

          final RowVsRowScore score;
          if (!compareMobility) {
            score = new RowVsRowScore(row, candidateInAligned,
                RangeUtils.rangeLength(mzRange) / 2.0, mzWeight,
                RangeUtils.rangeLength(rtRange) / 2.0, rtWeight);
          } else {
            score = new RowVsRowScore(row, candidateInAligned,
                RangeUtils.rangeLength(mzRange) / 2.0, mzWeight,
                RangeUtils.rangeLength(rtRange) / 2.0, rtWeight,
                RangeUtils.rangeLength(mobilityRange), mobilityWeight);
          }
          rowVsRowScores.add(score);
          scoresList.add(score);
        }
      }
      processedRows.getAndIncrement();
    });

    // after an iteration, rows of all other featureLists have been given a mapping
    // now we have to find the best match
    scoresList.sort(RowVsRowScore::compareTo);
    Map<FeatureListRow, FeatureListRow> assignedAlignedRows = new HashMap<>();
    for (RowVsRowScore score : scoresList) {
      final FeatureListRow alignedRow = score.getAlignedRow();
      final FeatureListRow row = score.getPeakListRow();

      if (!assignedRows.containsKey(row)) {
        // put all features of the row into the aligned row
        for (Feature feature : row.getFeatures()) {
          if (!alignedRow.hasFeature(feature.getRawDataFile())) {
            alignedRow.addFeature(feature.getRawDataFile(),
                new ModularFeature(alignedFeatureList, feature));
            assignedRows.put(row, true);
          } else {
            logger.finest(() -> "Warning: already a feature for raw file in row.");
          }
        }
      }
    }

    // keep track of unaligned rows for the next interation.
    assignedRows.entrySet().stream().filter(e -> e.getValue() == false)
        .forEach(e -> leftoverRows.add(e.getKey()));
    logger.finest(
        () -> "Assigned " + (unalignedRows.size() - leftoverRows.size()) + "/" + unalignedRows
            + ". " + leftoverRows.size() + " remaining.");
  }

  @Nullable
  private List<RawDataFile> getAllDataFiles() {
    List<RawDataFile> allDataFiles = new ArrayList<>();
    for (FeatureList featureList : featureLists) {
      for (RawDataFile dataFile : featureList.getRawDataFiles()) {
        // Each data file can only have one column in aligned feature
        // list
        if (allDataFiles.contains(dataFile)) {
          setStatus(TaskStatus.ERROR);
          setErrorMessage("Cannot run alignment, because file " + dataFile
              + " is present in multiple feature lists");
          return null;
        }
        allDataFiles.add(dataFile);
      }
    }
    return allDataFiles;
  }

  private boolean checkSpectralSimilarity(FeatureListRow row, FeatureListRow candidate) {
    // compare the similarity of spectra mass lists on MS1 or
    // MS2 level
    if (compareSpectraSimilarity) {
      DataPoint[] rowDPs = null;
      DataPoint[] candidateDPs = null;
      SpectralSimilarity sim = null;

      // get data points of mass list of the representative
      // scans
      if (msLevel == 1) {
        rowDPs = row.getBestFeature().getRepresentativeScan().getMassList().getDataPoints();
        candidateDPs = candidate.getBestFeature().getRepresentativeScan().getMassList()
            .getDataPoints();
      }

      // get data points of mass list of the best
      // fragmentation scans
      if (msLevel == 2) {
        if (row.getMostIntenseFragmentScan() != null && candidate.getMostIntenseFragmentScan() != null) {
          rowDPs = row.getMostIntenseFragmentScan().getMassList().getDataPoints();
          candidateDPs = candidate.getMostIntenseFragmentScan().getMassList().getDataPoints();
        } else {
          return false;
        }
      }

      // compare mass list data points of selected scans
      if (rowDPs != null && candidateDPs != null) {

        // calculate similarity using SimilarityFunction
        sim = createSimilarity(rowDPs, candidateDPs);

        // check if similarity is null. Similarity is not
        // null if similarity score is >= the
        // user set threshold
        if (sim == null) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean checkIsotopePattern(/*Map<FeatureListRow, IsotopePattern> isotopePatternMap,*/
      FeatureListRow row, FeatureListRow candidate) {
    if (compareIsotopePattern) {
      // get isotope pattern or put the best
//      IsotopePattern ip1 = isotopePatternMap
//          .computeIfAbsent(row, FeatureListRow::getBestIsotopePattern);
//      IsotopePattern ip2 = isotopePatternMap
//          .computeIfAbsent(candidate, FeatureListRow::getBestIsotopePattern);
      IsotopePattern ip1 = row.getBestIsotopePattern();
      IsotopePattern ip2 = candidate.getBestIsotopePattern();

      return (ip1 == null) || (ip2 == null) || IsotopePatternScoreCalculator
          .checkMatch(ip1, ip2, isotopeParams);
    }
    return true;
  }

  private boolean checkMZ(FeatureListRow candidate, Range<Double> mzRange) {
    return mzWeight <= 0 || mzRange.contains(candidate.getAverageMZ());
  }

  private boolean checkRT(FeatureListRow candidate, Range<Float> rtRange) {
    return rtWeight <= 0 || candidate.getAverageRT() < 0 || rtRange
        .contains(candidate.getAverageRT());
  }

  private boolean checkMobility(FeatureListRow candidate, Range<Float> mobilityRange) {
    return !compareMobility || mobilityWeight <= 0 || candidate.getAverageMobility() == null
        || mobilityRange.contains(candidate.getAverageMobility());
  }

  /**
   * Uses the similarity function and filter to create similarity.
   *
   * @return positive match with similarity or null if criteria was not met
   */
  private SpectralSimilarity createSimilarity(DataPoint[] library, DataPoint[] query) {
    return simFunction.getModule()
        .getSimilarity(simFunction.getParameterSet(), mzTolerance, 0, library, query);
  }

}
