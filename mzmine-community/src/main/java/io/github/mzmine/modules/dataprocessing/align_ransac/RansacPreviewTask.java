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
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListRowSorter;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

public class RansacPreviewTask extends AbstractTask {

  private String message;
  AlignmentRansacPlot plot;

  private FeatureList featureListX;
  private FeatureList featureListY;

  private Vector<AlignStructMol> list;
  private int alignedRows;
  private int totalRows;

  private ParameterSet parameters;

  public RansacPreviewTask(AlignmentRansacPlot plot, FeatureList featureListComboX,
      FeatureList featureListComboY, ParameterSet parameterSet) {
    super(null, Instant.now());

    this.plot = plot;
    this.featureListX = featureListComboX;
    this.featureListY = featureListComboY;

    this.parameters = parameterSet;

  }

  @Override
  public String getTaskDescription() {
    return message;
  }

  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0) {
      return 0d;
    }
    return alignedRows / totalRows;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    message = "Refreshing RANSAC preview";

    updateRansacPlot();

    setStatus(TaskStatus.FINISHED);
  }

  //TODO This method at the moment is quite slow. Optimize it (or underlying methods) if possible
  private void updateRansacPlot() {

    // Select the rawDataFile which has more peaks in each feature list
    RawDataFile file = featureListX.getRawDataFiles().stream()
        .max(Comparator.comparingInt(raw -> featureListX.getFeatures(raw).size())).get();
    RawDataFile file2 = featureListY.getRawDataFiles().stream()
        .max(Comparator.comparingInt(raw -> featureListY.getFeatures(raw).size())).get();

    // Ransac Alignment
    list = this.getVectorAlignment(featureListX, featureListY, file, file2);
    RANSAC ransac = new RANSAC(parameters);
    ransac.alignment(list);

    // Plot the result
    this.plot.removeSeries();
    this.plot.addSeries(list, featureListX.getName() + " vs " + featureListY.getName(),
        parameters.getParameter(RansacAlignerParameters.Linear).getValue());
    this.plot.printAlignmentChart(featureListX.getName() + " RT", featureListY.getName() + " RT");
  }

  /**
   * Create the vector which contains all the possible aligned peaks.
   *
   * @return vector which contains all the possible aligned peaks.
   */
  private Vector<AlignStructMol> getVectorAlignment(FeatureList peakListX, FeatureList peakListY,
      RawDataFile file, RawDataFile file2) {

    Vector<AlignStructMol> alignMol = new Vector<>();
    totalRows = peakListX.getNumberOfRows();

    peakListX.getRows().sorted(FeatureListRowSorter.DEFAULT_RT);
    for (FeatureListRow row : peakListX.getRows()) {

      // Calculate limits for a row with which the row can be aligned
      MZTolerance mzTolerance = parameters.getParameter(RansacAlignerParameters.MZTolerance)
          .getValue();
      RTTolerance rtTolerance = parameters.getParameter(RansacAlignerParameters.RTToleranceBefore)
          .getValue();
      Range<Double> mzRange = mzTolerance.getToleranceRange(row.getAverageMZ());
      Range<Float> rtRange = rtTolerance.getToleranceRange(row.getAverageRT());

      // Get all rows of the aligned feature list within parameter limits
      List<FeatureListRow> candidateRows = peakListY.getRowsInsideScanAndMZRange(rtRange, mzRange);

      for (FeatureListRow candidateRow : candidateRows) {
        if (file == null || file2 == null) {
          alignMol.addElement(new AlignStructMol(row, candidateRow));
        } else {
          if (candidateRow.getFeature(file2) != null) {
            alignMol.addElement(new AlignStructMol(row, candidateRow, file, file2));
          }
        }
      }
      alignedRows++;
    }
    return alignMol;
  }
}
