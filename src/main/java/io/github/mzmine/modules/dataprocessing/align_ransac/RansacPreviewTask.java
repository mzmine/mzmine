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
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class RansacPreviewTask extends AbstractTask {

  private String message;
  AlignmentRansacPlot plot;

  private FeatureList featureListX;
  private FeatureList featureListY;

  private ParameterSet parameters;

  public RansacPreviewTask(AlignmentRansacPlot plot,
      FeatureList featureListComboX, FeatureList featureListComboY, ParameterSet parameterSet) {
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

  //TODO Write a working percentage calculation
  @Override
  public double getFinishedPercentage() {
    return 0;
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

    this.plot.removeSeries();

    // Select the rawDataFile which has more peaks in each peakList
    int numPeaks = 0;
    RawDataFile file = null;
    RawDataFile file2 = null;

    for (RawDataFile rfile : featureListX.getRawDataFiles()) {
      if (featureListX.getFeatures(rfile).size() > numPeaks) {
        numPeaks = featureListX.getFeatures(rfile).size();
        file = rfile;
      }
    }

    numPeaks = 0;
    for (RawDataFile rfile : featureListY.getRawDataFiles()) {
      if (featureListY.getFeatures(rfile).size() > numPeaks) {
        numPeaks = featureListY.getFeatures(rfile).size();
        file2 = rfile;
      }
    }

    // Ransac Alignment
    Vector<AlignStructMol> list = this.getVectorAlignment(featureListX, featureListY, file, file2);
    RANSAC ransac = new RANSAC(parameters);
    ransac.alignment(list);

    // Plot the result
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

    Vector<AlignStructMol> alignMol = new Vector<AlignStructMol>();

    for (FeatureListRow row : peakListX.getRows()) {

      // Calculate limits for a row with which the row can be aligned
      MZTolerance mzTolerance = parameters.getParameter(RansacAlignerParameters.MZTolerance)
          .getValue();
      RTTolerance rtTolerance = parameters.getParameter(
          RansacAlignerParameters.RTToleranceBefore).getValue();
      Range<Double> mzRange = mzTolerance.getToleranceRange(row.getAverageMZ());
      Range<Float> rtRange = rtTolerance.getToleranceRange(row.getAverageRT());

      // Get all rows of the aligned peaklist within parameter limits
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
    }
    return alignMol;
  }
}
