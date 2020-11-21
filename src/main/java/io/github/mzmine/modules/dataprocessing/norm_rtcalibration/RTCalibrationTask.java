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

package io.github.mzmine.modules.dataprocessing.norm_rtcalibration;

import io.github.mzmine.datamodel.data.Feature;
import io.github.mzmine.datamodel.data.FeatureList;
import io.github.mzmine.datamodel.data.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.data.FeatureListRow;
import io.github.mzmine.datamodel.data.ModularFeature;
import io.github.mzmine.datamodel.data.ModularFeatureList;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.SimpleFeatureListAppliedMethod;
import io.github.mzmine.util.FeatureUtils;
import java.util.Vector;
import java.util.logging.Logger;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;

class RTCalibrationTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final MZmineProject project;
  private FeatureList originalFeatureLists[], normalizedFeatureLists[];

  // Processed rows counter
  private int processedRows, totalRows;

  private String suffix;
  private MZTolerance mzTolerance;
  private RTTolerance rtTolerance;
  private double minHeight;
  private boolean removeOriginal;
  private ParameterSet parameters;

  public RTCalibrationTask(MZmineProject project, ParameterSet parameters) {

    this.project = project;
    this.originalFeatureLists = parameters.getParameter(RTCalibrationParameters.featureLists).getValue()
        .getMatchingPeakLists();
    this.parameters = parameters;

    suffix = parameters.getParameter(RTCalibrationParameters.suffix).getValue();
    mzTolerance = parameters.getParameter(RTCalibrationParameters.MZTolerance).getValue();
    rtTolerance = parameters.getParameter(RTCalibrationParameters.RTTolerance).getValue();
    minHeight = parameters.getParameter(RTCalibrationParameters.minHeight).getValue();
    removeOriginal = parameters.getParameter(RTCalibrationParameters.autoRemove).getValue();

  }

  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0)
      return 0f;
    return (double) processedRows / (double) totalRows;
  }

  @Override
  public String getTaskDescription() {
    return "Retention time normalization of " + originalFeatureLists.length + " feature lists";
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);
    logger.info("Running retention time normalizer");

    // First we need to find standards by iterating through first feature
    // list
    totalRows = originalFeatureLists[0].getNumberOfRows();

    // Create new feature lists
    normalizedFeatureLists = new ModularFeatureList[originalFeatureLists.length];
    for (int i = 0; i < originalFeatureLists.length; i++) {
      normalizedFeatureLists[i] = new ModularFeatureList(originalFeatureLists[i] + " " + suffix,
          originalFeatureLists[i].getRawDataFiles());

      // Remember how many rows we need to normalize
      totalRows += originalFeatureLists[i].getNumberOfRows();

    }

    // goodStandards Vector contains identified standard rows, represented
    // by arrays. Each array has same length as originalFeatureLists array.
    // Array items represent particular standard feature in each feature list
    Vector<FeatureListRow[]> goodStandards = new Vector<FeatureListRow[]>();

    // Iterate the first feature list
    standardIteration: for (FeatureListRow candidate : originalFeatureLists[0].getRows()) {

      // Cancel?
      if (isCanceled()) {
        return;
      }

      processedRows++;

      // Check that all features of this row have proper height
      for (Feature p : candidate.getFeatures()) {
        if (p.getHeight() < minHeight)
          continue standardIteration;
      }

      FeatureListRow goodStandardCandidate[] = new FeatureListRow[originalFeatureLists.length];
      goodStandardCandidate[0] = candidate;

      double candidateMZ = candidate.getAverageMZ();
      float candidateRT = candidate.getAverageRT();

      // Find matching rows in remaining feature lists
      for (int i = 1; i < originalFeatureLists.length; i++) {
        Range<Float> rtRange = rtTolerance.getToleranceRange(candidateRT);
        Range<Double> mzRange = mzTolerance.getToleranceRange(candidateMZ);
        FeatureListRow matchingRows[] =
            originalFeatureLists[i].getRowsInsideScanAndMZRange(rtRange, mzRange)
                .toArray(new FeatureListRow[0]);

        // If we have not found exactly 1 matching feature, move to next
        // standard candidate
        if (matchingRows.length != 1)
          continue standardIteration;

        // Check that all features of this row have proper height
        for (Feature p : matchingRows[0].getFeatures()) {
          if (p.getHeight() < minHeight)
            continue standardIteration;
        }

        // Save reference to matching peak in this feature list
        goodStandardCandidate[i] = matchingRows[0];

      }

      // If we found a match of same peak in all peaklists, mark it as a
      // good standard
      goodStandards.add(goodStandardCandidate);
      logger.finest("Found a good standard for RT normalization: " + candidate);

    }

    // Check if we have any standards
    if (goodStandards.size() == 0) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("No good standard peak was found");
      return;
    }

    // Calculate average retention times of all standards
    double averagedRTs[] = new double[goodStandards.size()];
    for (int i = 0; i < goodStandards.size(); i++) {
      double rtAverage = 0;
      for (FeatureListRow row : goodStandards.get(i))
        rtAverage += row.getAverageRT();
      rtAverage /= originalFeatureLists.length;
      averagedRTs[i] = rtAverage;
    }

    // Normalize each feature list
    for (int featureListIndex = 0; featureListIndex < originalFeatureLists.length; featureListIndex++) {

      // Get standard rows for this feature list only
      FeatureListRow standards[] = new FeatureListRow[goodStandards.size()];
      for (int i = 0; i < goodStandards.size(); i++) {
        standards[i] = goodStandards.get(i)[featureListIndex];
      }

      normalizeFeatureList(originalFeatureLists[featureListIndex], normalizedFeatureLists[featureListIndex],
          standards, averagedRTs);

    }

    // Cancel?
    if (isCanceled()) {
      return;
    }

    // Add new feature lists to the project

    for (int i = 0; i < originalFeatureLists.length; i++) {

      project.addFeatureList(normalizedFeatureLists[i]);

      // Load previous applied methods
      for (FeatureListAppliedMethod proc : originalFeatureLists[i].getAppliedMethods()) {
        normalizedFeatureLists[i].addDescriptionOfAppliedTask(proc);
      }

      // Add task description to feature list
      normalizedFeatureLists[i].addDescriptionOfAppliedTask(
          new SimpleFeatureListAppliedMethod("Retention time normalization", parameters));

      // Remove the original feature lists if requested
      if (removeOriginal)
        project.removeFeatureList(originalFeatureLists[i]);

    }

    logger.info("Finished retention time normalizer");
    setStatus(TaskStatus.FINISHED);

  }

  /**
   * Normalize retention time of all rows in given feature list and save normalized rows into new
   * feature list.
   *
   * @param originalFeatureList Feature list to be normalized
   * @param normalizedFeatureList New feature list, where normalized rows are to be saved
   * @param standards Standard rows in same feature list
   * @param normalizedStdRTs Normalized retention times of standard rows
   */
  private void normalizeFeatureList(FeatureList originalFeatureList, FeatureList normalizedFeatureList,
      FeatureListRow standards[], double normalizedStdRTs[]) {

    FeatureListRow originalRows[] = originalFeatureList.getRows().toArray(FeatureListRow[]::new);

    // Iterate feature list rows
    for (FeatureListRow originalRow : originalRows) {

      // Cancel?
      if (isCanceled()) {
        return;
      }

      // Normalize one row
      FeatureListRow normalizedRow = normalizeRow(originalRow, standards, normalizedStdRTs);

      // Copy comment and identification
      normalizedRow.setComment(originalRow.getComment());
      for (FeatureIdentity ident : originalRow.getPeakIdentities())
        normalizedRow.addFeatureIdentity(ident, false);
      normalizedRow.setPreferredFeatureIdentity(originalRow.getPreferredFeatureIdentity());

      // Add the new row to normalized feature list
      normalizedFeatureList.addRow(normalizedRow);

      processedRows++;

    }

  }

  /**
   * Normalize retention time of given row using selected standards
   *
   * @param originalRow Feature list row to be normalized
   * @param standards Standard rows in same feature list
   * @param normalizedStdRTs Normalized retention times of standard rows
   * @return New feature list row with normalized retention time
   */
  private FeatureListRow normalizeRow(FeatureListRow originalRow, FeatureListRow standards[],
      double normalizedStdRTs[]) {

    FeatureListRow normalizedRow = new ModularFeatureListRow(
        (ModularFeatureList) originalRow.getFeatureList(), originalRow.getID());

    // Standard rows preceding and following this row
    int prevStdIndex = -1, nextStdIndex = -1;

    for (int stdIndex = 0; stdIndex < standards.length; stdIndex++) {

      // If this standard feature is actually originalRow
      if (standards[stdIndex] == originalRow) {
        prevStdIndex = stdIndex;
        nextStdIndex = stdIndex;
        break;
      }

      // If this standard feature is before our originalRow
      if (standards[stdIndex].getAverageRT() < originalRow.getAverageRT()) {
        if ((prevStdIndex == -1)
            || (standards[stdIndex].getAverageRT() > standards[prevStdIndex].getAverageRT()))
          prevStdIndex = stdIndex;
      }

      // If this standard feature is after our originalRow
      if (standards[stdIndex].getAverageRT() > originalRow.getAverageRT()) {
        if ((nextStdIndex == -1)
            || (standards[stdIndex].getAverageRT() < standards[nextStdIndex].getAverageRT()))
          nextStdIndex = stdIndex;
      }

    }

    // Calculate normalized retention time of this row
    double normalizedRT = -1;

    if ((prevStdIndex == -1) || (nextStdIndex == -1)) {
      normalizedRT = originalRow.getAverageRT();
    } else

    if (prevStdIndex == nextStdIndex) {
      normalizedRT = normalizedStdRTs[prevStdIndex];
    } else {
      double weight = (originalRow.getAverageRT() - standards[prevStdIndex].getAverageRT())
          / (standards[nextStdIndex].getAverageRT() - standards[prevStdIndex].getAverageRT());
      normalizedRT = normalizedStdRTs[prevStdIndex]
          + (weight * (normalizedStdRTs[nextStdIndex] - normalizedStdRTs[prevStdIndex]));
    }

    // Set normalized retention time to all features in this row
    for (RawDataFile file : originalRow.getRawDataFiles()) {
      Feature originalFeature = originalRow.getFeature(file);
      if (originalFeature != null) {
        ModularFeature normalizedFeature = new ModularFeature(originalFeature);
        FeatureUtils.copyFeatureProperties(originalFeature, normalizedFeature);
        normalizedFeature.setRT((float) normalizedRT);
        normalizedRow.addFeature(file, normalizedFeature);
      }
    }

    return normalizedRow;

  }

}
