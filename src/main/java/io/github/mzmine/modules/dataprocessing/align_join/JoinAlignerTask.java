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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.align_join;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreCalculator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;

public class JoinAlignerTask extends AbstractTask {

  private final MZmineProject project;
  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private ModularFeatureList[] featureLists;
  private final ModularFeatureList baseList;
  private ModularFeatureList alignedFeatureList;

  // Processed rows counter
  private int processedRows, totalRows;


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

  public JoinAlignerTask(MZmineProject project, ParameterSet parameters,
      @Nullable MemoryMapStorage storage) {
    super(storage);

    this.project = project;
    this.parameters = parameters;

    featureLists = parameters.getParameter(JoinAlignerParameters.peakLists).getValue()
        .getMatchingFeatureLists();

    // split into base and lists to merge
    baseList = featureLists[0];
    featureLists = Arrays.copyOfRange(featureLists, 1, featureLists.length);

    featureListName = parameters.getParameter(JoinAlignerParameters.peakListName).getValue();

    mzTolerance = parameters.getParameter(JoinAlignerParameters.MZTolerance).getValue();
    rtTolerance = parameters.getParameter(JoinAlignerParameters.RTTolerance).getValue();

    compareMobility = parameters.getParameter(JoinAlignerParameters.mobilityTolerance).getValue();
    mobilityTolerance = parameters.getParameter(JoinAlignerParameters.mobilityTolerance)
        .getEmbeddedParameter()
        .getValue();

    mzWeight = parameters.getParameter(JoinAlignerParameters.MZWeight).getValue();
    rtWeight = parameters.getParameter(JoinAlignerParameters.RTWeight).getValue();
    mobilityWeight = parameters.getParameter(JoinAlignerParameters.mobilityWeight).getValue();

    sameChargeRequired =
        parameters.getParameter(JoinAlignerParameters.SameChargeRequired).getValue();

    sameIDRequired = parameters.getParameter(JoinAlignerParameters.SameIDRequired).getValue();

    compareIsotopePattern =
        parameters.getParameter(JoinAlignerParameters.compareIsotopePattern).getValue();

    isotopeParams = parameters.getParameter(JoinAlignerParameters.compareIsotopePattern)
        .getEmbeddedParameters();

    compareSpectraSimilarity =
        parameters.getParameter(JoinAlignerParameters.compareSpectraSimilarity).getValue();

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
    return "Join aligner, " + featureListName + " (" + (featureLists.length+1) + " feature lists)";
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0) {
      return 0f;
    }
    return (double) processedRows / (double) totalRows;
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
    logger.info("Running join aligner");

    // Remember how many rows we need to process. Each row will be processed
    // twice, first for score calculation, second for actual alignment.
    for (FeatureList list : featureLists) {
      totalRows += list.getNumberOfRows() * 2;
    }

    // Collect all data files
    List<RawDataFile> allDataFiles = new ArrayList<>(baseList.getRawDataFiles());
    for (FeatureList featureList : featureLists) {

      for (RawDataFile dataFile : featureList.getRawDataFiles()) {

        // Each data file can only have one column in aligned feature
        // list
        if (allDataFiles.contains(dataFile)) {
          setStatus(TaskStatus.ERROR);
          setErrorMessage("Cannot run alignment, because file " + dataFile
                          + " is present in multiple feature lists");
          return;
        }

        allDataFiles.add(dataFile);
      }
    }

    // Create a new aligned feature list based on the baseList and renumber IDs
    alignedFeatureList = baseList.createCopy(featureListName, getMemoryMapStorage(), allDataFiles, true);

    int newRowID = alignedFeatureList.getNumberOfRows() + 1;

    // Iterate source feature lists
    for (ModularFeatureList featureList : featureLists) {
      // set initial chromatogram builder scans
      featureList.getRawDataFiles().forEach(
          file -> alignedFeatureList.setSelectedScans(file, featureList.getSeletedScans(file)));

      // Create a sorted set of scores matching
      TreeSet<RowVsRowScore> scoreSet = new TreeSet<>();

      FeatureListRow[] allRows = featureList.getRows().toArray(FeatureListRow[]::new);

      Map<FeatureListRow, IsotopePattern> isotopePatternMap = new HashMap<>();

      // Calculate scores for all possible alignments of this row
      for (FeatureListRow row : allRows) {

        if (isCanceled()) {
          return;
        }

        // Calculate limits for a row with which the row can be aligned
        Range<Double> mzRange = mzTolerance.getToleranceRange(row.getAverageMZ());
        Range<Float> rtRange = rtTolerance.getToleranceRange(row.getAverageRT());

        Range<Float> mobilityRange = compareMobility && !Float.isNaN(row.getAverageMobility()) ?
            mobilityTolerance.getToleranceRange(row.getAverageMobility()) : Range.singleton(0f);

        // Calculate scores and store them
        for (FeatureListRow candidate : alignedFeatureList.getRows()) {
          if (checkMZ(candidate, mzRange) && checkRT(candidate, rtRange) &&
              checkMobility(candidate, mobilityRange) &&
              (!sameChargeRequired || FeatureUtils.compareChargeState(row, candidate)) &&
              (!sameIDRequired || FeatureUtils.compareIdentities(row, candidate)) &&
              checkIsotopePattern(isotopePatternMap, row, candidate) &&
              checkSpectralSimilarity(row, candidate)) {

            final RowVsRowScore score;
            if (!compareMobility) {
              score = new RowVsRowScore(row, candidate, RangeUtils.rangeLength(mzRange) / 2.0,
                  mzWeight,
                  RangeUtils.rangeLength(rtRange) / 2.0, rtWeight);
            } else {
              score = new RowVsRowScore(row, candidate, RangeUtils.rangeLength(mzRange) / 2.0,
                  mzWeight,
                  RangeUtils.rangeLength(rtRange) / 2.0, rtWeight,
                  RangeUtils.rangeLength(mobilityRange), mobilityWeight);
            }
            scoreSet.add(score);
          }
        }
        processedRows++;
      }

      // Create a table of mappings for best scores
      Hashtable<FeatureListRow, FeatureListRow> alignmentMapping =
          new Hashtable<>();

      // Iterate scores by descending order
      for (RowVsRowScore score : scoreSet) {

        // Check if the row is already mapped
        if (alignmentMapping.containsKey(score.getPeakListRow())) {
          continue;
        }

        // Check if the aligned row is already filled
        if (alignmentMapping.containsValue(score.getAlignedRow())) {
          continue;
        }

        alignmentMapping.put(score.getPeakListRow(), score.getAlignedRow());

      }

      // Align all rows using mapping
      for (FeatureListRow row : allRows) {

        FeatureListRow targetRow = alignmentMapping.get(row);

        // If we have no mapping for this row, add a new one
        if (targetRow == null) {
          targetRow = new ModularFeatureListRow(alignedFeatureList, newRowID);
          newRowID++;
          alignedFeatureList.addRow(targetRow);
        }

        // Add all peaks from the original row to the aligned row
        // TODO: test aligned feature list correctness, seems like rows are not aligned correctly
        //  while aligning previously aligned feature lists
        for (RawDataFile file : row.getRawDataFiles()) {
          targetRow.addFeature(file, new ModularFeature(alignedFeatureList, row.getFeature(file)));
        }

        processedRows++;
      }
    } // Next feature list

    // Add new aligned feature list to the project
    project.addFeatureList(alignedFeatureList);

    // Add task description to peakList
    alignedFeatureList
        .addDescriptionOfAppliedTask(
            new SimpleFeatureListAppliedMethod("Join aligner", JoinAlignerModule.class,
                parameters));

    logger.info("Finished join aligner");

    setStatus(TaskStatus.FINISHED);

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
        rowDPs =
            row.getBestFeature().getRepresentativeScan().getMassList()
                .getDataPoints();
        candidateDPs = candidate.getBestFeature().getRepresentativeScan()
            .getMassList()
            .getDataPoints();
      }

      // get data points of mass list of the best
      // fragmentation scans
      if (msLevel == 2) {
        if (row.getBestFragmentation() != null && candidate.getBestFragmentation() != null) {
          rowDPs = row.getBestFragmentation().getMassList().getDataPoints();
          candidateDPs =
              candidate.getBestFragmentation().getMassList().getDataPoints();
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

  private boolean checkIsotopePattern(Map<FeatureListRow, IsotopePattern> isotopePatternMap,
      FeatureListRow row, FeatureListRow candidate) {
    if (compareIsotopePattern) {
      // get isotope pattern or put the best
      IsotopePattern ip1 = isotopePatternMap.computeIfAbsent(row,
          FeatureListRow::getBestIsotopePattern);
      IsotopePattern ip2 = isotopePatternMap
          .computeIfAbsent(candidate, FeatureListRow::getBestIsotopePattern);

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
    return !compareMobility || mobilityWeight <= 0 || Float.isNaN(candidate.getAverageMobility())
           || mobilityRange.contains(candidate.getAverageMobility());
  }


  /**
   * Uses the similarity function and filter to create similarity.
   *
   * @return positive match with similarity or null if criteria was not met
   */
  private SpectralSimilarity createSimilarity(DataPoint[] library, DataPoint[] query) {
    return simFunction.getModule().getSimilarity(simFunction.getParameterSet(), mzTolerance, 0,
        library, query);
  }

}
