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

package io.github.mzmine.modules.dataprocessing.norm_rtcalibration;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.Vector;
import java.util.logging.Logger;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class RTCalibrationTask extends AbstractTask {

  private final OriginalFeatureListOption handleOriginal;
  private final MZmineProject project;
  private Logger logger = Logger.getLogger(this.getClass().getName());
  private ModularFeatureList originalFeatureLists[], normalizedFeatureLists[];

  // Processed rows counter
  private int processedRows, totalRows;

  private String suffix;
  private MZTolerance mzTolerance;
  private RTTolerance rtTolerance;
  private double minHeight;
  private ParameterSet parameters;

  public RTCalibrationTask(MZmineProject project, ParameterSet parameters,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.originalFeatureLists = (ModularFeatureList[]) parameters.getParameter(
        RTCalibrationParameters.featureLists).getValue().getMatchingFeatureLists();
    this.parameters = parameters;

    suffix = parameters.getParameter(RTCalibrationParameters.suffix).getValue();
    mzTolerance = parameters.getParameter(RTCalibrationParameters.MZTolerance).getValue();
    rtTolerance = parameters.getParameter(RTCalibrationParameters.RTTolerance).getValue();
    minHeight = parameters.getParameter(RTCalibrationParameters.minHeight).getValue();
    handleOriginal = parameters.getParameter(RTCalibrationParameters.handleOriginal).getValue();

  }

  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0) {
      return 0f;
    }
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
          getMemoryMapStorage(), originalFeatureLists[i].getRawDataFiles());

      // Remember how many rows we need to normalize
      totalRows += originalFeatureLists[i].getNumberOfRows();

    }

    // goodStandards Vector contains identified standard rows, represented
    // by arrays. Each array has same length as originalFeatureLists array.
    // Array items represent particular standard feature in each feature list
    Vector<ModularFeatureListRow[]> goodStandards = new Vector<ModularFeatureListRow[]>();

    // Iterate the first feature list
    ObservableList<FeatureListRow> rows = originalFeatureLists[0].getRows();
    standardIteration:
    for (int j = 0, rowsSize = rows.size(); j < rowsSize; j++) {
      ModularFeatureListRow candidate = (ModularFeatureListRow) rows.get(j);

      // Cancel?
      if (isCanceled()) {
        return;
      }

      processedRows++;

      // Check that all features of this row have proper height
      for (Feature p : candidate.getFeatures()) {
        if (p.getHeight() < minHeight) {
          continue standardIteration;
        }
      }

      ModularFeatureListRow goodStandardCandidate[] = new ModularFeatureListRow[originalFeatureLists.length];
      goodStandardCandidate[0] = candidate;

      double candidateMZ = candidate.getAverageMZ();
      float candidateRT = candidate.getAverageRT();

      // Find matching rows in remaining feature lists
      for (int i = 1; i < originalFeatureLists.length; i++) {
        Range<Float> rtRange = rtTolerance.getToleranceRange(candidateRT);
        Range<Double> mzRange = mzTolerance.getToleranceRange(candidateMZ);
        ModularFeatureListRow matchingRows[] = originalFeatureLists[i].getRowsInsideScanAndMZRange(
            rtRange, mzRange).toArray(new ModularFeatureListRow[0]);

        // If we have not found exactly 1 matching feature, move to next
        // standard candidate
        if (matchingRows.length != 1) {
          continue standardIteration;
        }

        // Check that all features of this row have proper height
        for (Feature p : matchingRows[0].getFeatures()) {
          if (p.getHeight() < minHeight) {
            continue standardIteration;
          }
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
      for (FeatureListRow row : goodStandards.get(i)) {
        rtAverage += row.getAverageRT();
      }
      rtAverage /= originalFeatureLists.length;
      averagedRTs[i] = rtAverage;
    }

    // Normalize each feature list
    for (int featureListIndex = 0; featureListIndex < originalFeatureLists.length;
        featureListIndex++) {

      // Get standard rows for this feature list only
      ModularFeatureListRow standards[] = new ModularFeatureListRow[goodStandards.size()];
      for (int i = 0; i < goodStandards.size(); i++) {
        standards[i] = goodStandards.get(i)[featureListIndex];
      }

      normalizeFeatureList(originalFeatureLists[featureListIndex],
          normalizedFeatureLists[featureListIndex], standards, averagedRTs);

    }

    // Cancel?
    if (isCanceled()) {
      return;
    }

    // Add new feature lists to the project
    for (int i = 0; i < originalFeatureLists.length; i++) {
      // Load previous applied methods
      for (FeatureListAppliedMethod proc : originalFeatureLists[i].getAppliedMethods()) {
        normalizedFeatureLists[i].addDescriptionOfAppliedTask(proc);
      }

      // set selected scans
      for (RawDataFile f : originalFeatureLists[i].getRawDataFiles()) {
        normalizedFeatureLists[i].setSelectedScans(f, originalFeatureLists[i].getSeletedScans(f));
      }

      // Add task description to feature list
      normalizedFeatureLists[i].addDescriptionOfAppliedTask(
          new SimpleFeatureListAppliedMethod("Retention time normalization",
              RTCalibrationModule.class, parameters, getModuleCallDate()));

      // add or remove lists
      handleOriginal.reflectNewFeatureListToProject(suffix, project, normalizedFeatureLists[i],
          originalFeatureLists[i]);
    }

    logger.info("Finished retention time normalizer");
    setStatus(TaskStatus.FINISHED);

  }

  /**
   * Normalize retention time of all rows in given feature list and save normalized rows into new
   * feature list.
   *
   * @param originalFeatureList   Feature list to be normalized
   * @param normalizedFeatureList New feature list, where normalized rows are to be saved
   * @param standards             Standard rows in same feature list
   * @param normalizedStdRTs      Normalized retention times of standard rows
   */
  private void normalizeFeatureList(ModularFeatureList originalFeatureList,
      ModularFeatureList normalizedFeatureList, ModularFeatureListRow standards[],
      double normalizedStdRTs[]) {

    ModularFeatureListRow originalRows[] = originalFeatureList.getRows()
        .toArray(ModularFeatureListRow[]::new);

    // Iterate feature list rows
    for (ModularFeatureListRow originalRow : originalRows) {

      // Cancel?
      if (isCanceled()) {
        return;
      }

      // Normalize one row
      ModularFeatureListRow normalizedRow = normalizeRow(normalizedFeatureList, originalRow,
          standards, normalizedStdRTs);

      // Add the new row to normalized feature list
      normalizedFeatureList.addRow(normalizedRow);

      processedRows++;

    }

  }

  /**
   * Normalize retention time of given row using selected standards
   *
   * @param originalRow      Feature list row to be normalized
   * @param standards        Standard rows in same feature list
   * @param normalizedStdRTs Normalized retention times of standard rows
   * @return New feature list row with normalized retention time
   */
  private ModularFeatureListRow normalizeRow(ModularFeatureList targetFeatureList,
      ModularFeatureListRow originalRow, ModularFeatureListRow standards[],
      double normalizedStdRTs[]) {

    ModularFeatureListRow normalizedRow = new ModularFeatureListRow(targetFeatureList, originalRow,
        false);

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
        if ((prevStdIndex == -1) || (standards[stdIndex].getAverageRT()
                                     > standards[prevStdIndex].getAverageRT())) {
          prevStdIndex = stdIndex;
        }
      }

      // If this standard feature is after our originalRow
      if (standards[stdIndex].getAverageRT() > originalRow.getAverageRT()) {
        if ((nextStdIndex == -1) || (standards[stdIndex].getAverageRT()
                                     < standards[nextStdIndex].getAverageRT())) {
          nextStdIndex = stdIndex;
        }
      }

    }

    // Calculate normalized retention time of this row
    double normalizedRT = -1;

    if ((prevStdIndex == -1) || (nextStdIndex == -1)) {
      normalizedRT = originalRow.getAverageRT();
    } else if (prevStdIndex == nextStdIndex) {
      normalizedRT = normalizedStdRTs[prevStdIndex];
    } else {
      double weight = (originalRow.getAverageRT() - standards[prevStdIndex].getAverageRT()) / (
          standards[nextStdIndex].getAverageRT() - standards[prevStdIndex].getAverageRT());
      normalizedRT = normalizedStdRTs[prevStdIndex] + (weight * (normalizedStdRTs[nextStdIndex]
                                                                 - normalizedStdRTs[prevStdIndex]));
    }

    // Set normalized retention time to all features in this row
    for (RawDataFile file : originalRow.getRawDataFiles()) {
      ModularFeature originalFeature = originalRow.getFeature(file);
      if (originalFeature != null) {
        ModularFeature normalizedFeature = new ModularFeature(targetFeatureList, originalFeature);
        normalizedFeature.setRT((float) normalizedRT);
        normalizedRow.addFeature(file, normalizedFeature);
      }
    }

    return normalizedRow;

  }

}
