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

import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import java.util.Vector;
import java.util.logging.Logger;
import org.jfree.data.xy.AbstractXYDataset;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureMeasurementType;
import jmprojection.Preprocess;
import jmprojection.ProjectionStatus;
import jmprojection.Sammons;

public class SammonsDataset extends AbstractXYDataset implements ProjectionPlotDataset {

  private static final long serialVersionUID = 1L;

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private double[] component1Coords;
  private double[] component2Coords;

  private FeatureList featureList;
  private ParameterSet parameters;

  private ColoringType coloringType;

  private RawDataFile[] selectedRawDataFiles;
  private FeatureListRow[] selectedRows;

  private int[] groupsForSelectedRawDataFiles;
  private Object[] parameterValuesForGroups;
  int numberOfGroups;

  private String datasetTitle;
  private int xAxisDimension;
  private int yAxisDimension;

  private TaskStatus status = TaskStatus.WAITING;
  private String errorMessage;

  private ProjectionStatus projectionStatus;

  public SammonsDataset(MZmineProject project, ParameterSet parameters) {

    this.featureList = parameters.getParameter(ProjectionPlotParameters.featureLists).getValue()
        .getMatchingFeatureLists()[0];
    this.parameters = parameters;
    this.xAxisDimension =
        parameters.getParameter(ProjectionPlotParameters.xAxisComponent).getValue();
    this.yAxisDimension =
        parameters.getParameter(ProjectionPlotParameters.yAxisComponent).getValue();

    coloringType = parameters.getParameter(ProjectionPlotParameters.coloringType).getValue();
    selectedRawDataFiles = parameters.getParameter(ProjectionPlotParameters.dataFiles).getValue()
        .getMatchingRawDataFiles();
    selectedRows = featureList.getRows().toArray(FeatureListRow[]::new);

    datasetTitle = "Sammon's projection";

    // Determine groups for selected raw data files
    groupsForSelectedRawDataFiles = new int[selectedRawDataFiles.length];

    if (coloringType.equals(ColoringType.NOCOLORING)) {
      // All files to a single group
      for (int ind = 0; ind < selectedRawDataFiles.length; ind++)
        groupsForSelectedRawDataFiles[ind] = 0;

      numberOfGroups = 1;
    }

    if (coloringType.equals(ColoringType.COLORBYFILE)) {
      // Each file to own group
      for (int ind = 0; ind < selectedRawDataFiles.length; ind++)
        groupsForSelectedRawDataFiles[ind] = ind;

      numberOfGroups = selectedRawDataFiles.length;
    }

    if (coloringType.isByParameter()) {
      // Group files with same parameter value to same group
      Vector<Object> availableParameterValues = new Vector<Object>();
      UserParameter<?, ?> selectedParameter = coloringType.getParameter();
      for (RawDataFile rawDataFile : selectedRawDataFiles) {
        Object paramValue = project.getParameterValue(selectedParameter, rawDataFile);
        if (!availableParameterValues.contains(paramValue))
          availableParameterValues.add(paramValue);
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
  public String toString() {
    return datasetTitle;
  }

  @Override
  public String getXLabel() {
    if (xAxisDimension == 1)
      return "1st projected dimension";
    if (xAxisDimension == 2)
      return "2nd projected dimension";
    if (xAxisDimension == 3)
      return "3rd projected dimension";
    return "" + xAxisDimension + "th projected dimension";
  }

  @Override
  public String getYLabel() {
    if (yAxisDimension == 1)
      return "1st projected dimension";
    if (yAxisDimension == 2)
      return "2nd projected dimension";
    if (yAxisDimension == 3)
      return "3rd projected dimension";
    return "" + yAxisDimension + "th projected dimension";
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
    if (parameterValuesForGroups == null)
      return null;
    if ((parameterValuesForGroups.length - 1) < groupNumber)
      return null;
    return parameterValuesForGroups[groupNumber];
  }

  @Override
  public int getNumberOfGroups() {
    return numberOfGroups;
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    if (selectedRows.length == 0) {
      this.status = TaskStatus.ERROR;
      errorMessage = "No features selected for Sammons plot";
      return;
    }
    if (selectedRawDataFiles.length == 0) {
      this.status = TaskStatus.ERROR;
      errorMessage = "No raw data files selected for Sammons plot";
      return;
    }

    logger.info("Computing projection plot");

    // Generate matrix of raw data (input to Sammon's projection)
    boolean useArea = false;
    if (parameters.getParameter(ProjectionPlotParameters.featureMeasurementType)
        .getValue() == FeatureMeasurementType.AREA)
      useArea = true;

    double[][] rawData = new double[selectedRawDataFiles.length][selectedRows.length];
    for (int rowIndex = 0; rowIndex < selectedRows.length; rowIndex++) {
      FeatureListRow featureListRow = selectedRows[rowIndex];
      for (int fileIndex = 0; fileIndex < selectedRawDataFiles.length; fileIndex++) {
        RawDataFile rawDataFile = selectedRawDataFiles[fileIndex];
        Feature p = featureListRow.getFeature(rawDataFile);
        if (p != null) {
          if (useArea)
            rawData[fileIndex][rowIndex] = p.getArea();
          else
            rawData[fileIndex][rowIndex] = p.getHeight();
        }
      }
    }

    int numComponents = xAxisDimension;
    if (yAxisDimension > numComponents)
      numComponents = yAxisDimension;

    // Scale data and do Sammon's mapping
    Preprocess.scaleToUnityVariance(rawData);
    Sammons sammonsProj = new Sammons(rawData);

    projectionStatus = sammonsProj.getProjectionStatus();

    sammonsProj.iterate(100);

    if (status == TaskStatus.CANCELED)
      return;

    double[][] result = sammonsProj.getState();

    if (status == TaskStatus.CANCELED)
      return;

    component1Coords = result[xAxisDimension - 1];
    component2Coords = result[yAxisDimension - 1];

    ProjectionPlotWindow newFrame = new ProjectionPlotWindow(featureList, this, parameters);
    newFrame.show();

    setStatus(TaskStatus.FINISHED);
    logger.info("Finished computing projection plot.");

  }

  @Override
  public void cancel() {
    if (projectionStatus != null)
      projectionStatus.cancel();
    setStatus(TaskStatus.CANCELED);
  }

  @Override
  public String getErrorMessage() {
    return errorMessage;
  }

  @Override
  public TaskStatus getStatus() {
    return status;
  }

  @Override
  public String getTaskDescription() {
    return "Sammon's projection";
  }

  @Override
  public double getFinishedPercentage() {
    if (projectionStatus == null)
      return 0;
    return projectionStatus.getFinishedPercentage();
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#setStatus()
   */
  public void setStatus(TaskStatus newStatus) {
    this.status = newStatus;
  }

  @Override
  public TaskPriority getTaskPriority() {
    return TaskPriority.NORMAL;
  }
}
