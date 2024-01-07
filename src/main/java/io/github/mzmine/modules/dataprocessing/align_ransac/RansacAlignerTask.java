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

package io.github.mzmine.modules.dataprocessing.align_ransac;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RangeUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.optimization.fitting.PolynomialFitter;
import org.apache.commons.math.optimization.general.GaussNewtonOptimizer;
import org.apache.commons.math.stat.regression.SimpleRegression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class RansacAlignerTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final MZmineProject project;
  private ModularFeatureList[] featureLists;
  private ModularFeatureList alignedFeatureList;
  // Processed rows counter
  private int processedRows, totalRows;
  // Parameters
  private String featureListName;
  private MZTolerance mzTolerance;
  private RTTolerance rtToleranceBefore, rtToleranceAfter;
  private ParameterSet parameters;
  private boolean sameChargeRequired;
  // ID counter for the new peaklist
  private int newRowID = 1;

  public RansacAlignerTask(MZmineProject project, FeatureList[] featureLists,
      ParameterSet parameters, @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.featureLists = (ModularFeatureList[]) featureLists;
    this.parameters = parameters;

    // Get parameter values for easier use
    featureListName = parameters.getParameter(RansacAlignerParameters.peakListName).getValue();

    mzTolerance = parameters.getParameter(RansacAlignerParameters.MZTolerance).getValue();

    rtToleranceBefore = parameters.getParameter(RansacAlignerParameters.RTToleranceBefore)
        .getValue();

    rtToleranceAfter = parameters.getParameter(RansacAlignerParameters.RTToleranceAfter).getValue();

    sameChargeRequired = parameters.getParameter(RansacAlignerParameters.SameChargeRequired)
        .getValue();

  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Ransac aligner, " + featureListName + " (" + featureLists.length + " feature lists)";
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

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);
    logger.info("Running Ransac aligner");

    // Remember how many rows we need to process. Each row will be processed
    // twice, first for score calculation, second for actual alignment.
    for (int i = 0; i < featureLists.length; i++) {
      totalRows += featureLists[i].getNumberOfRows() * 2;
    }

    // Collect all data files
    List<RawDataFile> allDataFiles = new ArrayList<RawDataFile>();

    // Create a new aligned feature list, add all distinct files
    alignedFeatureList = new ModularFeatureList(featureListName, getMemoryMapStorage(),
        Arrays.stream(featureLists).flatMap(flist -> flist.getRawDataFiles().stream()).distinct()
            .toList());

    for (ModularFeatureList featureList : featureLists) {

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

        featureList.getRawDataFiles().forEach(
            file -> alignedFeatureList.setSelectedScans(file, featureList.getSeletedScans(file)));
      }
    }

    // Iterate source feature lists
    for (FeatureList featureList : featureLists) {

      HashMap<FeatureListRow, FeatureListRow> alignmentMapping = this.getAlignmentMap(featureList);

      List<FeatureListRow> allRows = featureList.getRows();

      // Align all rows using mapping
      for (FeatureListRow row : allRows) {
        FeatureListRow targetRow = alignmentMapping.get(row);

        // If we have no mapping for this row, add a new one
        if (targetRow == null) {
          targetRow = new ModularFeatureListRow(alignedFeatureList, newRowID);
          //(@NotNull ModularFeatureList flist, int id, RawDataFile raw,
          //    ModularFeature p)
          newRowID++;
          alignedFeatureList.addRow(targetRow);
        }

        // Add all peaks from the original row to the aligned row
        for (RawDataFile file : row.getRawDataFiles()) {
          targetRow.addFeature(file, new ModularFeature(alignedFeatureList, row.getFeature(file)));
        }

        processedRows++;
      }

    } // Next feature list

    // Add new aligned feature list to the project
    project.addFeatureList(alignedFeatureList);

    // Edit by Aleksandr Smirnov
    FeatureListRow row = alignedFeatureList.getRow(1);
    double alignedRetTime = row.getAverageRT();

    for (Feature feature : row.getFeatures()) {
      double retTimeDelta = alignedRetTime - feature.getRT();
      RawDataFile dataFile = feature.getRawDataFile();

      SortedMap<Double, Double> chromatogram = new TreeMap<>();

      for (int i = 0; i < feature.getNumberOfDataPoints(); i++) {
        DataPoint dataPoint = feature.getDataPointAtIndex(i);
        double retTime = feature.getRetentionTimeAtIndex(i) + retTimeDelta;
        if (dataPoint != null) {
          chromatogram.put(retTime, dataPoint.getIntensity());
        }
      }
    }

    // End of Edit

    // Add task description to peakList
    alignedFeatureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("Ransac aligner", RansacAlignerModule.class, parameters,
            getModuleCallDate()));

    logger.info("Finished RANSAC aligner");
    setStatus(TaskStatus.FINISHED);

  }

  /**
   * @param peakList
   * @return
   */
  private HashMap<FeatureListRow, FeatureListRow> getAlignmentMap(FeatureList peakList) {

    // Create a table of mappings for best scores
    HashMap<FeatureListRow, FeatureListRow> alignmentMapping = new HashMap<>();

    if (alignedFeatureList.getNumberOfRows() < 1) {
      return alignmentMapping;
    }

    // Create a sorted set of scores matching
    TreeSet<RowVsRowScore> scoreSet = new TreeSet<RowVsRowScore>();

    // RANSAC algorithm
    List<AlignStructMol> list = ransacPeakLists(alignedFeatureList, peakList);
    PolynomialFunction function = this.getPolynomialFunction(list);

    List<FeatureListRow> allRows = peakList.getRows();

    for (FeatureListRow row : allRows) {
      // Calculate limits for a row with which the row can be aligned
      Range<Double> mzRange = mzTolerance.getToleranceRange(row.getAverageMZ());

      float rt;
      try {
        rt = (float) function.value(row.getAverageRT());
      } catch (NullPointerException e) {
        rt = row.getAverageRT();
      }
      if (Double.isNaN(rt) || rt == -1) {
        rt = row.getAverageRT();
      }

      Range<Float> rtRange = rtToleranceAfter.getToleranceRange(rt);

      // Get all rows of the aligned peaklist within parameter limits
      List<FeatureListRow> candidateRows = alignedFeatureList.getRowsInsideScanAndMZRange(rtRange,
          mzRange);

      for (FeatureListRow candidate : candidateRows) {
        RowVsRowScore score;
        if (sameChargeRequired && (!FeatureUtils.compareChargeState(row, candidate))) {
          continue;
        }

        try {
          score = new RowVsRowScore(row, candidate, RangeUtils.rangeLength(mzRange) / 2.0,
              RangeUtils.rangeLength(rtRange) / 2.0, rt);

          scoreSet.add(score);
          setErrorMessage(score.getErrorMessage());

        } catch (Exception e) {
          e.printStackTrace();
          setStatus(TaskStatus.ERROR);
          return null;
        }
      }
      processedRows++;
    }

    // Iterate scores by descending order
    Iterator<RowVsRowScore> scoreIterator = scoreSet.iterator();
    while (scoreIterator.hasNext()) {

      RowVsRowScore score = scoreIterator.next();

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

    return alignmentMapping;
  }

  /**
   * RANSAC
   *
   * @param alignedPeakList
   * @param peakList
   * @return
   */
  private List<AlignStructMol> ransacPeakLists(FeatureList alignedPeakList, FeatureList peakList) {
    List<AlignStructMol> list = this.getVectorAlignment(alignedPeakList, peakList);
    RANSAC ransac = new RANSAC(parameters);
    ransac.alignment(list);
    return list;
  }

  /**
   * Return the corrected RT of the row
   *
   * @param list
   * @return
   */
  private PolynomialFunction getPolynomialFunction(List<AlignStructMol> list) {
    List<RTs> data = new ArrayList<RTs>();
    for (AlignStructMol m : list) {
      if (m.Aligned) {
        data.add(new RTs(m.RT2, m.RT));
      }
    }

    data = this.smooth(data);
    Collections.sort(data, new RTs());

    double[] xval = new double[data.size()];
    double[] yval = new double[data.size()];
    int i = 0;

    for (RTs rt : data) {
      xval[i] = rt.RT;
      yval[i++] = rt.RT2;
    }

    PolynomialFitter fitter = new PolynomialFitter(3, new GaussNewtonOptimizer(true));
    for (RTs rt : data) {
      fitter.addObservedPoint(1, rt.RT, rt.RT2);
    }
    try {
      return fitter.fit();

    } catch (Exception ex) {
      return null;
    }
  }

  private List<RTs> smooth(List<RTs> list) {
    // Add points to the model in between of the real points to smooth the
    // regression model
    Collections.sort(list, new RTs());

    for (int i = 0; i < list.size() - 1; i++) {
      RTs point1 = list.get(i);
      RTs point2 = list.get(i + 1);
      if (point1.RT < point2.RT - 2) {
        SimpleRegression regression = new SimpleRegression();
        regression.addData(point1.RT, point1.RT2);
        regression.addData(point2.RT, point2.RT2);
        double rt = point1.RT + 1;
        while (rt < point2.RT) {
          RTs newPoint = new RTs(rt, regression.predict(rt));
          list.add(newPoint);
          rt++;
        }

      }
    }

    return list;
  }

  /**
   * Create the vector which contains all the possible aligned peaks.
   *
   * @param peakListX
   * @param peakListY
   * @return vector which contains all the possible aligned peaks.
   */
  private List<AlignStructMol> getVectorAlignment(FeatureList peakListX, FeatureList peakListY) {

    List<AlignStructMol> alignMol = new ArrayList<AlignStructMol>();
    for (FeatureListRow row : peakListX.getRows()) {

      if (isCanceled()) {
        return null;
      }
      // Calculate limits for a row with which the row can be aligned
      Range<Double> mzRange = mzTolerance.getToleranceRange(row.getAverageMZ());
      Range<Float> rtRange = rtToleranceBefore.getToleranceRange(row.getAverageRT());

      // Get all rows of the aligned peaklist within parameter limits
      List<FeatureListRow> candidateRows = peakListY.getRowsInsideScanAndMZRange(rtRange, mzRange);

      for (FeatureListRow candidateRow : candidateRows) {
        alignMol.add(new AlignStructMol(row, candidateRow));
      }
    }

    return alignMol;
  }
}
