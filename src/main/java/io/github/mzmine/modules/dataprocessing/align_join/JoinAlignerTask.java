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

package io.github.mzmine.modules.dataprocessing.align_join;

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
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.DataType;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JoinAlignerTask extends AbstractTask {

  private final MZmineProject project;
  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private final AtomicInteger alignedRows = new AtomicInteger(0);
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
  private final boolean compareMobility;
  private final boolean removeOriginalFeatureLists;
  /**
   * All feature lists except the base list
   */
  private List<ModularFeatureList> featureLists;
  private ModularFeatureList alignedFeatureList;
  // Processed rows counter
  private int totalRows;
  // ID counter for the new peaklist
  private int iteration = 0;
  // fields for spectra similarity
  private MZmineProcessingStep<SpectralSimilarityFunction> simFunction;
  private int msLevel;

  public JoinAlignerTask(MZmineProject project, ParameterSet parameters,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.parameters = parameters;

    featureLists = Arrays.stream(parameters.getParameter(JoinAlignerParameters.peakLists).getValue()
        .getMatchingFeatureLists()).toList();

    featureListName = parameters.getParameter(JoinAlignerParameters.peakListName).getValue();

    mzTolerance = parameters.getParameter(JoinAlignerParameters.MZTolerance).getValue();
    rtTolerance = parameters.getParameter(JoinAlignerParameters.RTTolerance).getValue();

    removeOriginalFeatureLists = parameters
        .getValue(JoinAlignerParameters.removeOriginalFeatureLists);

    compareMobility = parameters.getParameter(JoinAlignerParameters.mobilityTolerance).getValue();
    mobilityTolerance = parameters.getParameter(JoinAlignerParameters.mobilityTolerance)
        .getEmbeddedParameter().getValue();

    mzWeight = parameters.getParameter(JoinAlignerParameters.MZWeight).getValue();
    rtWeight = parameters.getParameter(JoinAlignerParameters.RTWeight).getValue();
    mobilityWeight = parameters.getParameter(JoinAlignerParameters.mobilityWeight).getValue();

    sameChargeRequired = parameters.getParameter(JoinAlignerParameters.SameChargeRequired)
        .getValue();

    sameIDRequired = parameters.getParameter(JoinAlignerParameters.SameIDRequired).getValue();
    compareIsotopePattern = parameters.getParameter(JoinAlignerParameters.compareIsotopePattern)
        .getValue();
    isotopeParams = parameters.getParameter(JoinAlignerParameters.compareIsotopePattern)
        .getEmbeddedParameters();
    compareSpectraSimilarity = parameters.getParameter(
        JoinAlignerParameters.compareSpectraSimilarity).getValue();

    if (compareSpectraSimilarity) {
      simFunction = parameters.getParameter(JoinAlignerParameters.compareSpectraSimilarity)
          .getEmbeddedParameters()
          .getParameter(JoinAlignerSpectraSimilarityScoreParameters.similarityFunction).getValue();
      msLevel = parameters.getParameter(JoinAlignerParameters.compareSpectraSimilarity)
          .getEmbeddedParameters().getParameter(JoinAlignerSpectraSimilarityScoreParameters.msLevel)
          .getValue();
    }
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Join aligner, " + featureListName + " (" + featureLists.size() + " feature lists)";
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0) {
      return 0f;
    }
    return alignedFeatureList != null ? (alignedFeatureList.getNumberOfRows() + alignedRows.get())
        / (double) totalRows : 0d;
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
    logger.info(
        () -> "Running parallel join aligner on " + featureLists.size() + " feature lists.");

    // Remember how many rows we need to process. Each row will be processed
    // twice, first for score calculation, second for actual alignment.
    for (FeatureList list : featureLists) {
      totalRows += list.getNumberOfRows();
    }

    // Collect all data files
    final List<RawDataFile> allDataFiles = getAllDataFiles();
    if (allDataFiles == null) {
      return;
    }

    // Create a new aligned feature list based on the baseList and renumber IDs
    alignedFeatureList = new ModularFeatureList(featureListName, getMemoryMapStorage(),
        allDataFiles);
    transferRowTypes(alignedFeatureList, featureLists);
    transferSelectedScans(featureLists, alignedFeatureList);
    final AtomicInteger newRowID = new AtomicInteger(1);

    // get all rows of all feature lists.
    final List<FeatureListRow> unalignedRows = new ArrayList<>(
        featureLists.stream().flatMap(flist -> flist.stream()).toList());

    // contains all files that potentially have unaligned features.
    final List<ModularFeatureList> leftoverFlists = new ArrayList<>(featureLists);
    boolean alignmentDone = false;

    // Contains all rows without a match in the aligned feature list. All rows for now, since there
    // has been no alignment yet
    final List<FeatureListRow> leftoverRows = Collections.synchronizedList(
        new ArrayList<>(unalignedRows));

    while (leftoverFlists.size() > 0) {
      // select the next base feature list, and get all rows from that feature list from our list
      // of rows. We use the flist with the most rows first.
      Map<FeatureList, Long> remainingFlists = unalignedRows.stream()
          .collect(Collectors.groupingBy(FeatureListRow::getFeatureList, Collectors.counting()));
      var nextEntry = remainingFlists.entrySet().stream()
          .max(Comparator.comparingLong(Entry::getValue)).orElse(null);
      if (nextEntry == null) {
        logger.finest(() -> "No more leftover rows. Some feature lists were empty.");
        break;
      }
      final FeatureList nextBaseList = nextEntry.getKey();
      leftoverFlists.remove(nextBaseList);

      // we add a new set of unaligned rows to the feature list that we can align on.
      List<FeatureListRow> nextBaseRows = new ArrayList<>(
          leftoverRows.stream().filter(row -> row.getFeatureList().equals(nextBaseList)).map(
              row -> (FeatureListRow) new ModularFeatureListRow(alignedFeatureList,
                  newRowID.getAndIncrement(), (ModularFeatureListRow) row, true)).toList());
      nextBaseRows.sort(new FeatureListRowSorter(SortingProperty.MZ, SortingDirection.Ascending));
      nextBaseRows.forEach(row -> alignedFeatureList.addRow(row));

      // remove new base rows from the leftover rows.
      leftoverRows.removeIf(row -> row.getFeatureList().equals(nextBaseList));

      // unaligned rows are our rows that shall be matched to the rows of the feature list. The
      // leftover rows is supposed to contain all rows that have not been aligned during the method
      // call.
      unalignedRows.clear();
      unalignedRows.addAll(leftoverRows);
      leftoverRows.clear();

      // use the whole feature list to align on. the average row m/zs and rts change during alignment due to
      List<FeatureListRow> baseRows = new ArrayList<>(alignedFeatureList.getRows());
      baseRows.sort(new FeatureListRowSorter(SortingProperty.MZ, SortingDirection.Ascending));
      alignRowsOnBaseRows(unalignedRows, baseRows, leftoverRows);

      iteration++;
    }

    alignedFeatureList.getAppliedMethods().addAll(featureLists.get(0).getAppliedMethods());
    // Add task description to peakList
    alignedFeatureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("Join aligner", JoinAlignerModule.class, parameters,
            getModuleCallDate()));
    // Add new aligned feature list to the project {
    project.addFeatureList(alignedFeatureList);

    logger.info("Finished join aligner");

    if (removeOriginalFeatureLists) {
      project.removeFeatureLists(featureLists);
      logger.info("Original lists removed after join aligner");
    }
    setStatus(TaskStatus.FINISHED);

  }

  private void alignRowsOnBaseRows(List<FeatureListRow> unalignedRows,
      List<FeatureListRow> baseRowsByMz, List<FeatureListRow> leftoverRows) {

    final Map<FeatureListRow, Boolean> assignedRows = new HashMap<>();
    unalignedRows.forEach(row -> assignedRows.put(row, false));

    // key = a row to be aligned, value = all possible matches in the aligned fl and it's scores
//    final Map<FeatureListRow, List<RowVsRowScore>> rowToScoreMap = new ConcurrentHashMap<>();
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
          compareMobility && row.getAverageMobility() != null ? mobilityTolerance.getToleranceRange(
              row.getAverageMobility()) : Range.singleton(0f);

      // find all rows in the aligned rows that might match
      final List<FeatureListRow> candidatesInAligned = FeatureListUtils.getRows(baseRowsByMz,
          rtRange, mzRange, true);

      if (candidatesInAligned.isEmpty()) {
        return;
      }

//      final List<RowVsRowScore> rowVsRowScores = rowToScoreMap
//          .computeIfAbsent(row, r -> new ArrayList<>());

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
//          rowVsRowScores.add(score);
          scoresList.add(score);
        }
      }
    });

    // after an iteration, rows of all other featureLists have been given a mapping
    // now we have to find the best match
    scoresList.sort(RowVsRowScore::compareTo);
//    Map<FeatureListRow, FeatureListRow> assignedAlignedRows = new HashMap<>();
    for (RowVsRowScore score : scoresList) {
      final FeatureListRow alignedRow = score.getAlignedRow();
      final FeatureListRow row = score.getPeakListRow();
      if (assignedRows.get(row) == false) {
        // put all features of the row into the aligned row
        for (Feature feature : row.getFeatures()) {
          if (!alignedRow.hasFeature(feature.getRawDataFile())) {
            alignedRow.addFeature(feature.getRawDataFile(),
                new ModularFeature(alignedFeatureList, feature));
            assignedRows.put(row, true);
            alignedRows.getAndIncrement();
          } /*else {
            // if we align all unaligned features on the full aligned feature list, this might actually happen.
            logger.finest(() -> "Warning: already a feature for raw file in row.");
          }*/
        }
      }
    }

    // keep track of unaligned rows for the next interation.
    assignedRows.entrySet().stream().filter(e -> e.getValue() == false)
        .forEach(e -> leftoverRows.add(e.getKey()));
    logger.finest(() -> "Assigned " + (unalignedRows.size() - leftoverRows.size()) + "/"
        + unalignedRows.size() + ". " + leftoverRows.size() + " remaining. Iteration " + iteration
        + "/" + featureLists.size());
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
        if (row.getMostIntenseFragmentScan() != null
            && candidate.getMostIntenseFragmentScan() != null) {
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

      return (ip1 == null) || (ip2 == null) || IsotopePatternScoreCalculator.checkMatch(ip1, ip2,
          isotopeParams);
    }
    return true;
  }

  private boolean checkMZ(FeatureListRow candidate, Range<Double> mzRange) {
    return mzWeight <= 0 || mzRange.contains(candidate.getAverageMZ());
  }

  private boolean checkRT(FeatureListRow candidate, Range<Float> rtRange) {
    return rtWeight <= 0 || candidate.getAverageRT() < 0 || rtRange.contains(
        candidate.getAverageRT());
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

  private void transferRowTypes(ModularFeatureList targetFlist,
      Collection<ModularFeatureList> sourceFlists) {
    for (ModularFeatureList sourceFlist : sourceFlists) {
      for (Class<? extends DataType> value : sourceFlist.getRowTypes().keySet()) {
        if (!targetFlist.hasRowType(value)) {
          targetFlist.addRowType(sourceFlist.getRowTypes().get(value));
        }
      }
    }
  }

  private void transferSelectedScans(Collection<ModularFeatureList> flists,
      ModularFeatureList target) {
    for (ModularFeatureList flist : flists) {
      for (RawDataFile rawDataFile : flist.getRawDataFiles()) {
        if (target.getSeletedScans(rawDataFile) != null) {
          throw new IllegalStateException(
              "Error, selected scans for file " + rawDataFile + " already set.");
        }
        target.setSelectedScans(rawDataFile, flist.getSeletedScans(rawDataFile));
      }
    }
  }
}
