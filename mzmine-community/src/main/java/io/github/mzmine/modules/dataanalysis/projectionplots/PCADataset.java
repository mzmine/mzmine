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

package io.github.mzmine.modules.dataanalysis.projectionplots;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.AbstractTaskXYDataset;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.util.Vector;
import java.util.logging.Logger;
import jmprojection.PCA;
import jmprojection.Preprocess;
import jmprojection.ProjectionStatus;

public class PCADataset extends AbstractTaskXYDataset implements ProjectionPlotDataset {

  private static final long serialVersionUID = 1L;

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private double[] component1Coords;
  private double[] component2Coords;

  private final ParameterSet parameters;
  private final FeatureList featureList;

  private final ColoringType coloringType;

  private final RawDataFile[] selectedRawDataFiles;
  private final FeatureListRow[] selectedRows;

  private final int[] groupsForSelectedRawDataFiles;
  private Object[] parameterValuesForGroups;
  int numberOfGroups;

  private final String datasetTitle;
  private final int xAxisPC;
  private final int yAxisPC;

  private ProjectionStatus projectionStatus;

  public PCADataset(MZmineProject project, ParameterSet parameters) {

    this.featureList = parameters.getParameter(ProjectionPlotParameters.featureLists).getValue()
        .getMatchingFeatureLists()[0];
    this.parameters = parameters;

    this.xAxisPC = parameters.getParameter(ProjectionPlotParameters.xAxisComponent).getValue();
    this.yAxisPC = parameters.getParameter(ProjectionPlotParameters.yAxisComponent).getValue();

    coloringType = parameters.getParameter(ProjectionPlotParameters.coloringType).getValue();

    selectedRawDataFiles = parameters.getParameter(ProjectionPlotParameters.dataFiles).getValue()
        .getMatchingRawDataFiles();
    selectedRows = featureList.getRows().toArray(FeatureListRow[]::new);

    datasetTitle = "Principal component analysis";

    // Determine groups for selected raw data files
    groupsForSelectedRawDataFiles = new int[selectedRawDataFiles.length];

    if (coloringType.equals(ColoringType.NOCOLORING)) {
      // All files to a single group
      for (int ind = 0; ind < selectedRawDataFiles.length; ind++) {
        groupsForSelectedRawDataFiles[ind] = 0;
      }

      numberOfGroups = 1;
    }

    if (coloringType.equals(ColoringType.COLORBYFILE)) {
      // Each file to own group
      for (int ind = 0; ind < selectedRawDataFiles.length; ind++) {
        groupsForSelectedRawDataFiles[ind] = ind;
      }

      numberOfGroups = selectedRawDataFiles.length;
    }

    if (coloringType.isByParameter()) {
      // Group files with same parameter value to same group
      Vector<Object> availableParameterValues = new Vector<Object>();
      UserParameter<?, ?> selectedParameter = coloringType.getParameter();
      for (RawDataFile rawDataFile : selectedRawDataFiles) {
        Object paramValue = project.getParameterValue(selectedParameter, rawDataFile);
        if (!availableParameterValues.contains(paramValue)) {
          availableParameterValues.add(paramValue);
        }
      }

      for (int ind = 0; ind < selectedRawDataFiles.length; ind++) {
        Object paramValue = project.getParameterValue(selectedParameter, selectedRawDataFiles[ind]);
        groupsForSelectedRawDataFiles[ind] = availableParameterValues.indexOf(paramValue);
      }
      parameterValuesForGroups = availableParameterValues.toArray();

      numberOfGroups = parameterValuesForGroups.length;
    }

  }

  @Override
  public TaskPriority getTaskPriority() {
    return TaskPriority.NORMAL;
  }

  @Override
  public String toString() {
    return datasetTitle;
  }

  @Override
  public String getXLabel() {
    if (xAxisPC == 1) {
      return "1st PC";
    }
    if (xAxisPC == 2) {
      return "2nd PC";
    }
    if (xAxisPC == 3) {
      return "3rd PC";
    }
    return "" + xAxisPC + "th PC";
  }

  @Override
  public String getYLabel() {
    if (yAxisPC == 1) {
      return "1st PC";
    }
    if (yAxisPC == 2) {
      return "2nd PC";
    }
    if (yAxisPC == 3) {
      return "3rd PC";
    }
    return "" + yAxisPC + "th PC";
  }

  @Override
  public int getSeriesCount() {
    return 1;
  }

  @Override
  public Comparable<Integer> getSeriesKey(int series) {
    return 1;
  }

  @Override
  public int getItemCount(int series) {
    return component1Coords.length;
  }

  @Override
  public Number getX(int series, int item) {
    return component1Coords[item];
  }

  @Override
  public Number getY(int series, int item) {
    return component2Coords[item];
  }

  @Override
  public String getRawDataFile(int item) {
    return selectedRawDataFiles[item].getName();
  }

  @Override
  public int getGroupNumber(int item) {
    return groupsForSelectedRawDataFiles[item];
  }

  @Override
  public Object getGroupParameterValue(int groupNumber) {
    if (parameterValuesForGroups == null) {
      return null;
    }
    if ((parameterValuesForGroups.length - 1) < groupNumber) {
      return null;
    }
    return parameterValuesForGroups[groupNumber];
  }

  @Override
  public int getNumberOfGroups() {
    return numberOfGroups;
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    logger.info("Computing PCA projection plot");

    // Generate matrix of raw data (input to PCA)
    final boolean useArea = (
        parameters.getParameter(ProjectionPlotParameters.featureMeasurementType).getValue()
            == AbundanceMeasure.Area);

    if (selectedRows.length == 0) {
      setStatus(TaskStatus.ERROR);
      errorMessage = "No features selected for PCA plot";
      return;
    }
    if (selectedRawDataFiles.length == 0) {
      setStatus(TaskStatus.ERROR);
      errorMessage = "No raw data files selected for PCA plot";
      return;
    }

    double[][] rawData = new double[selectedRawDataFiles.length][selectedRows.length];
    for (int rowIndex = 0; rowIndex < selectedRows.length; rowIndex++) {
      FeatureListRow featureListRow = selectedRows[rowIndex];
      for (int fileIndex = 0; fileIndex < selectedRawDataFiles.length; fileIndex++) {
        RawDataFile rawDataFile = selectedRawDataFiles[fileIndex];
        Feature p = featureListRow.getFeature(rawDataFile);
        if (p != null) {
          if (useArea) {
            rawData[fileIndex][rowIndex] = p.getArea();
          } else {
            rawData[fileIndex][rowIndex] = p.getHeight();
          }
        }
      }
    }

    int numComponents = xAxisPC;
    if (yAxisPC > numComponents) {
      numComponents = yAxisPC;
    }

    // Scale data and do PCA
    Preprocess.scaleToUnityVariance(rawData);

    // Replace NaN values with 0.0
    for (int i = 0; i < rawData.length; i++) {
      for (int j = 0; j < rawData[i].length; j++) {
        if (Double.isNaN(rawData[i][j])) {
          rawData[i][j] = 0.0;
        }
      }
    }

    PCA pcaProj = new PCA(rawData, numComponents);

    projectionStatus = pcaProj.getProjectionStatus();

    double[][] result = pcaProj.getState();

    if (isCanceled()) {
      return;
    }

    component1Coords = result[xAxisPC - 1];
    component2Coords = result[yAxisPC - 1];

    ProjectionPlotWindow newFrame = new ProjectionPlotWindow(featureList, this, parameters);
    newFrame.show();

    setStatus(TaskStatus.FINISHED);
    logger.info("Finished computing projection plot.");

  }

  @Override
  public void cancel() {
    if (projectionStatus != null) {
      projectionStatus.cancel();
    }
    super.cancel();
  }

  @Override
  public String getTaskDescription() {
    return "PCA projection";
  }

  @Override
  public double getFinishedPercentage() {
    if (projectionStatus == null) {
      return 0;
    }
    return projectionStatus.getFinishedPercentage();
  }

}
