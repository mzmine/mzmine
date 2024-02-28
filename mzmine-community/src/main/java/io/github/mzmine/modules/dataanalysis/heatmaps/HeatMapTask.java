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

package io.github.mzmine.modules.dataanalysis.heatmaps;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.R.REngineType;
import io.github.mzmine.util.R.RSessionWrapper;
import io.github.mzmine.util.R.RSessionWrapperException;
import io.github.mzmine.util.R.Rsession.Rsession;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;
import org.apache.commons.math.MathException;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.inference.TTestImpl;
import org.jetbrains.annotations.NotNull;

public class HeatMapTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private RSessionWrapper rSession;
  private String errorMsg;

  private final MZmineProject project;
  private final REngineType rEngineType;
  private final String outputType;
  private final boolean log, rcontrol, scale, plegend, area, onlyIdentified;
  private final int height, width, columnMargin, rowMargin, starSize;
  private final File outputFile;
  private double[][] newFeatureList;
  private String[] rowNames, colNames;
  private String[][] pValueMatrix;
  private double finishedPercentage = 0.0f;
  private final UserParameter<?, ?> selectedParameter;
  private final Object referenceGroup;
  private final FeatureList featureList;

  public HeatMapTask(MZmineProject project, FeatureList featureList, ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null

    this.project = project;
    this.featureList = featureList;

    // Parameters
    rEngineType = parameters.getParameter(HeatMapParameters.RENGINE_TYPE).getValue();
    outputFile = parameters.getParameter(HeatMapParameters.fileName).getValue();
    outputType = parameters.getParameter(HeatMapParameters.fileTypeSelection).getValue();
    selectedParameter = parameters.getParameter(HeatMapParameters.selectionData).getValue();
    referenceGroup = parameters.getParameter(HeatMapParameters.referenceGroup).getValue();
    area = parameters.getParameter(HeatMapParameters.useFeatureArea).getValue();
    onlyIdentified = parameters.getParameter(HeatMapParameters.useIdenfiedRows).getValue();

    log = parameters.getParameter(HeatMapParameters.log).getValue();
    scale = parameters.getParameter(HeatMapParameters.scale).getValue();
    rcontrol = parameters.getParameter(HeatMapParameters.showControlSamples).getValue();
    plegend = parameters.getParameter(HeatMapParameters.plegend).getValue();

    height = parameters.getParameter(HeatMapParameters.height).getValue();
    width = parameters.getParameter(HeatMapParameters.width).getValue();
    columnMargin = parameters.getParameter(HeatMapParameters.columnMargin).getValue();
    rowMargin = parameters.getParameter(HeatMapParameters.rowMargin).getValue();
    starSize = parameters.getParameter(HeatMapParameters.star).getValue();

  }

  public String getTaskDescription() {
    return "Heat map... ";
  }

  public double getFinishedPercentage() {
    return finishedPercentage;
  }

  @Override
  public void cancel() {

    super.cancel();

    // Turn off R instance, if already existing.
    try {
      if (rSession != null)
        rSession.close(true);
    } catch (RSessionWrapperException e) {
      // Silent, always...
    }
  }

  public void run() {
    errorMsg = null;

    setStatus(TaskStatus.PROCESSING);

    logger.info("Heat map plot");

    if (plegend) {
      newFeatureList = groupingDataset(selectedParameter, referenceGroup.toString());
    } else {
      newFeatureList = modifySimpleDataset(selectedParameter, referenceGroup.toString());
    }

    if (newFeatureList.length == 0 || newFeatureList[0].length == 0) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("The data for heat map is empty.");
      return;
    }

    try {

      // Load gplots library
      String[] reqPackages = {"gplots"};
      rSession =
          new RSessionWrapper(this.rEngineType, "HeatMap analysis module", reqPackages, null);
      rSession.open();

      finishedPercentage = 0.3f;

      if (outputType.contains("png")) {
        if (height < 500 || width < 500) {

          setStatus(TaskStatus.ERROR);
          setErrorMessage(
              "Figure height or width is too small. " + "Minimun height and width is 500.");
          return;
        }
      }

      rSession.eval("dataset<- matrix(\"\",nrow =" + newFeatureList[0].length + ",ncol="
          + newFeatureList.length + ")");

      if (plegend) {
        rSession.eval("stars<- matrix(\"\",nrow =" + newFeatureList[0].length + ",ncol="
            + newFeatureList.length + ")");
      }

      // assing the values to the matrix
      for (int row = 0; row < newFeatureList[0].length; row++) {

        for (int column = 0; column < newFeatureList.length; column++) {

          int r = row + 1;
          int c = column + 1;

          double value = newFeatureList[column][row];

          if (plegend) {
            String pValue = pValueMatrix[column][row];
            rSession.eval("stars[" + r + "," + c + "] = \"" + pValue + "\"");
          }

          if (!Double.isInfinite(value) && !Double.isNaN(value)) {

            rSession.eval("dataset[" + r + "," + c + "] = " + value);
          } else {

            rSession.eval("dataset[" + r + "," + c + "] = NA");
          }
        }
      }
      finishedPercentage = 0.4f;

      rSession.eval("dataset <- apply(dataset, 2, as.numeric)");

      // Assign row names to the data set
      rSession.assign("rowNames", rowNames);
      rSession.eval("rownames(dataset)<-rowNames");

      // Assign column names to the data set
      rSession.assign("colNames", colNames);
      rSession.eval("colnames(dataset)<-colNames");

      finishedPercentage = 0.5f;

      // Remove the rows with too many NA's. The distances between
      // rows can't be calculated if the rows don't have
      // at least one sample in common.
      rSession.eval("d <- as.matrix(dist(dataset))");
      rSession.eval("d[upper.tri(d)] <- 0");
      rSession.eval("naindices <- na.action(na.omit(d))");
      rSession.eval("if (! is.null(naindices)) dataset <- dataset[-naindices,]");

      finishedPercentage = 0.8f;

      String marginParameter = "margins = c(" + columnMargin + "," + rowMargin + ")";
      rSession.eval("br<-c(seq(from=min(dataset,na.rm=T),to=0,length.out=256),"
          + "seq(from=0.00001,to=max(dataset,na.rm=T),length.out=256))", false);

      // Convert the path to R-compatible string
      final String escapedOutputFileName = Rsession.toRpath(outputFile);

      // Possible output file types
      if (outputType.contains("pdf")) {

        rSession.eval(
            "pdf(\"" + escapedOutputFileName + "\", height=" + height + ", width=" + width + ")");
      } else if (outputType.contains("fig")) {

        rSession.eval("xfig(\"" + escapedOutputFileName + "\", height=" + height + ", width="
            + width + ", horizontal = FALSE, pointsize = 12)");
      } else if (outputType.contains("svg")) {

        // Load RSvgDevice library
        rSession.loadPackage("RSvgDevice");

        rSession.eval("devSVG(\"" + escapedOutputFileName + "\", height=" + height + ", width="
            + width + ")");
      } else if (outputType.contains("png")) {

        rSession.eval(
            "png(\"" + escapedOutputFileName + "\", height=" + height + ", width=" + width + ")");
      }

      if (plegend) {

        rSession.eval(
            "heatmap.2(dataset," + marginParameter + ", trace=\"none\", col=bluered(length(br)-1),"
                + " breaks=br, cellnote=stars, notecol=\"black\"" + ", notecex=" + starSize
                + ", na.color=\"grey\")",
            false);
      } else {

        rSession.eval("heatmap.2(dataset," + marginParameter
            + ", trace=\"none\", col=bluered(length(br)-1)," + " breaks=br, na.color=\"grey\")",
            false);
      }

      rSession.eval("dev.off()", false);

      // Stands for a (void) collect!
      this.rSession.runOnlyOnline();
      // Done: Refresh R code stack
      this.rSession.clearCode();

      finishedPercentage = 1.0;

      // Turn off R instance, once task ended gracefully.
      if (!isCanceled())
        rSession.close(false);

    } catch (RSessionWrapperException e) {
      if (!isCanceled()) {
        errorMsg = "'R computing error' during heatmap generation. \n" + e.getMessage();
      }
    } catch (Exception e) {
      if (!isCanceled()) {
        errorMsg = "'Unknown error' during heatmap generation. \n" + e.getMessage();
      }
    }

    // Turn off R instance, once task ended UNgracefully.
    try {
      if (!isCanceled())
        if (rSession != null)
          rSession.close(isCanceled());
    } catch (RSessionWrapperException e) {
      if (!isCanceled()) {
        // Do not override potential previous error message.
        if (errorMsg == null) {
          errorMsg = e.getMessage();
        }
      } else {
        // User canceled: Silent.
      }
    }

    // Report error.
    if (errorMsg != null) {
      setErrorMessage(errorMsg);
      setStatus(TaskStatus.ERROR);
    } else {
      setStatus(TaskStatus.FINISHED);
    }
  }

  private double[][] modifySimpleDataset(UserParameter<?, ?> selectedParameter,
      String referenceGroup) {

    // Collect all data files
    Vector<RawDataFile> allDataFiles = new Vector<RawDataFile>();
    allDataFiles.addAll(featureList.getRawDataFiles());

    // Determine the reference group and non reference group (the rest of
    // the samples) for raw data files
    List<RawDataFile> referenceDataFiles = new ArrayList<RawDataFile>();
    List<RawDataFile> nonReferenceDataFiles = new ArrayList<RawDataFile>();

    for (RawDataFile rawDataFile : allDataFiles) {

      Object paramValue = project.getParameterValue(selectedParameter, rawDataFile);

      if (paramValue.equals(referenceGroup)) {

        referenceDataFiles.add(rawDataFile);
      } else {

        nonReferenceDataFiles.add(rawDataFile);
      }
    }

    int numRows = 0;
    for (int row = 0; row < featureList.getNumberOfRows(); row++) {

      if (!onlyIdentified
          || (onlyIdentified && featureList.getRow(row).getPeakIdentities().size() > 0)) {
        numRows++;
      }
    }

    // Create a new aligned feature list with all the samples if the
    // reference
    // group has to be shown or with only
    // the non reference group if not.
    double[][] dataMatrix;
    if (rcontrol) {
      dataMatrix = new double[allDataFiles.size()][numRows];
    } else {
      dataMatrix = new double[nonReferenceDataFiles.size()][numRows];
    }

    // Data files that should be in the heat map
    List<RawDataFile> shownDataFiles = null;
    if (rcontrol) {
      shownDataFiles = allDataFiles;
    } else {
      shownDataFiles = nonReferenceDataFiles;
    }

    for (int row = 0, rowIndex = 0; row < featureList.getNumberOfRows(); row++) {
      FeatureListRow rowFeature = featureList.getRow(row);
      if (!onlyIdentified || (onlyIdentified && rowFeature.getPeakIdentities().size() > 0)) {

        // Average area or height of the reference group
        double referenceAverage = 0;
        int referenceFeatureCount = 0;
        for (int column = 0; column < referenceDataFiles.size(); column++) {

          if (rowFeature.getFeature(referenceDataFiles.get(column)) != null) {

            if (area) {

              referenceAverage += rowFeature.getFeature(referenceDataFiles.get(column)).getArea();
            } else {

              referenceAverage += rowFeature.getFeature(referenceDataFiles.get(column)).getHeight();
            }
            referenceFeatureCount++;
          }
        }
        if (referenceFeatureCount > 0) {

          referenceAverage /= referenceFeatureCount;
        }

        // Divide the area or height of each feature by the average of the
        // area or height of the reference features in each row
        for (int column = 0; column < shownDataFiles.size(); column++) {
          double value = Double.NaN;
          if (rowFeature.getFeature(shownDataFiles.get(column)) != null) {

            Feature feature = rowFeature.getFeature(shownDataFiles.get(column));
            if (area) {

              value = feature.getArea() / referenceAverage;
            } else {

              value = feature.getHeight() / referenceAverage;
            }
            if (log) {

              value = Math.log(value);
            }
          }

          dataMatrix[column][rowIndex] = value;
        }
        rowIndex++;
      }
    }

    // Scale the data dividing the feature area/height by the standard
    // deviation of each column
    if (scale) {
      scale(dataMatrix);
    }

    // Create two arrays: row and column names
    rowNames = new String[dataMatrix[0].length];
    colNames = new String[shownDataFiles.size()];

    for (int column = 0; column < shownDataFiles.size(); column++) {

      colNames[column] = shownDataFiles.get(column).getName();
    }
    for (int row = 0, rowIndex = 0; row < featureList.getNumberOfRows(); row++) {
      if (!onlyIdentified
          || (onlyIdentified && featureList.getRow(row).getPeakIdentities().size() > 0)) {
        if (featureList.getRow(row).getPeakIdentities() != null
            && featureList.getRow(row).getPeakIdentities().size() > 0) {

          rowNames[rowIndex++] = featureList.getRow(row).getPreferredFeatureIdentity().getName();
        } else {

          rowNames[rowIndex++] = "Unknown";
        }
      }
    }

    return dataMatrix;
  }

  private void scale(double[][] featureList) {
    DescriptiveStatistics stdDevStats = new DescriptiveStatistics();

    for (int columns = 0; columns < featureList.length; columns++) {
      stdDevStats.clear();
      for (int row = 0; row < featureList[columns].length; row++) {
        if (!Double.isInfinite(featureList[columns][row]) && !Double.isNaN(featureList[columns][row])) {
          stdDevStats.addValue(featureList[columns][row]);
        }
      }

      double stdDev = stdDevStats.getStandardDeviation();

      for (int row = 0; row < featureList[columns].length; row++) {
        if (stdDev != 0) {
          featureList[columns][row] = featureList[columns][row] / stdDev;
        }
      }
    }
  }

  private double[][] groupingDataset(UserParameter<?, ?> selectedParameter, String referenceGroup) {
    // Collect all data files
    Vector<RawDataFile> allDataFiles = new Vector<RawDataFile>();
    DescriptiveStatistics meanControlStats = new DescriptiveStatistics();
    DescriptiveStatistics meanGroupStats = new DescriptiveStatistics();
    allDataFiles.addAll(featureList.getRawDataFiles());

    // Determine the reference group and non reference group (the rest of
    // the samples) for raw data files
    List<RawDataFile> referenceDataFiles = new ArrayList<RawDataFile>();
    List<RawDataFile> nonReferenceDataFiles = new ArrayList<RawDataFile>();

    List<String> groups = new ArrayList<String>();

    for (RawDataFile rawDataFile : allDataFiles) {

      Object paramValue = project.getParameterValue(selectedParameter, rawDataFile);
      if (!groups.contains(String.valueOf(paramValue))) {
        groups.add(String.valueOf(paramValue));
      }
      if (String.valueOf(paramValue).equals(referenceGroup)) {

        referenceDataFiles.add(rawDataFile);
      } else {

        nonReferenceDataFiles.add(rawDataFile);
      }
    }

    int numRows = 0;
    for (int row = 0; row < featureList.getNumberOfRows(); row++) {

      if (!onlyIdentified
          || (onlyIdentified && featureList.getRow(row).getPeakIdentities().size() > 0)) {
        numRows++;
      }
    }

    // Create a new aligned feature list with all the samples if the
    // reference
    // group has to be shown or with only
    // the non reference group if not.
    double[][] dataMatrix = new double[groups.size() - 1][numRows];
    pValueMatrix = new String[groups.size() - 1][numRows];

    // data files that should be in the heat map
    List<RawDataFile> shownDataFiles = nonReferenceDataFiles;

    for (int row = 0, rowIndex = 0; row < featureList.getNumberOfRows(); row++) {
      FeatureListRow rowFeature = featureList.getRow(row);
      if (!onlyIdentified || (onlyIdentified && rowFeature.getPeakIdentities().size() > 0)) {
        // Average area or height of the reference group
        meanControlStats.clear();
        for (int column = 0; column < referenceDataFiles.size(); column++) {

          if (rowFeature.getFeature(referenceDataFiles.get(column)) != null) {

            if (area) {

              meanControlStats.addValue(rowFeature.getFeature(referenceDataFiles.get(column)).getArea());
            } else {

              meanControlStats
                  .addValue(rowFeature.getFeature(referenceDataFiles.get(column)).getHeight());
            }

          }
        }

        // Divide the area or height of each feature by the average of the
        // area or height of the reference features in each row
        int columnIndex = 0;
        for (int column = 0; column < groups.size(); column++) {
          String group = groups.get(column);
          meanGroupStats.clear();
          if (!group.equals(referenceGroup)) {

            for (int dataColumn = 0; dataColumn < shownDataFiles.size(); dataColumn++) {

              Object paramValue =
                  project.getParameterValue(selectedParameter, shownDataFiles.get(dataColumn));
              if (rowFeature.getFeature(shownDataFiles.get(dataColumn)) != null
                  && String.valueOf(paramValue).equals(group)) {

                Feature feature = rowFeature.getFeature(shownDataFiles.get(dataColumn));

                if (!Double.isInfinite(feature.getArea()) && !Double.isNaN(feature.getArea())) {

                  if (area) {

                    meanGroupStats.addValue(feature.getArea());
                  } else {

                    meanGroupStats.addValue(feature.getHeight());
                  }
                }

              }
            }

            double value = meanGroupStats.getMean() / meanControlStats.getMean();
            if (meanGroupStats.getN() > 1 && meanControlStats.getN() > 1) {
              pValueMatrix[columnIndex][rowIndex] =
                  this.getPvalue(meanGroupStats, meanControlStats);
            } else {
              pValueMatrix[columnIndex][rowIndex] = "";
            }

            if (log) {

              value = Math.log(value);
            }
            dataMatrix[columnIndex++][rowIndex] = value;
          }
        }
        rowIndex++;
      }
    }

    // Scale the data dividing the feature area/height by the standard
    // deviation of each column
    if (scale) {
      scale(dataMatrix);
    }

    // Create two arrays: row and column names
    rowNames = new String[dataMatrix[0].length];
    colNames = new String[groups.size() - 1];

    int columnIndex = 0;
    for (String group : groups) {

      if (!group.equals(referenceGroup)) {

        colNames[columnIndex++] = group;
      }
    }
    for (int row = 0, rowIndex = 0; row < featureList.getNumberOfRows(); row++) {
      if (!onlyIdentified
          || (onlyIdentified && featureList.getRow(row).getPeakIdentities().size() > 0)) {
        if (featureList.getRow(row).getPeakIdentities() != null
            && featureList.getRow(row).getPeakIdentities().size() > 0) {

          rowNames[rowIndex++] = featureList.getRow(row).getPreferredFeatureIdentity().getName();
        } else {

          rowNames[rowIndex++] = "Unknown";
        }
      }
    }

    return dataMatrix;
  }

  private String getPvalue(DescriptiveStatistics group1, DescriptiveStatistics group2) {
    TTestImpl ttest = new TTestImpl();
    String sig = "";
    try {
      double pValue = ttest.tTest(group1, group2);
      if (pValue < 0.05) {
        sig = "*";
      }
      if (pValue < 0.01) {
        sig = "**";
      }
      if (pValue < 0.001) {
        sig = "***";
      }

    } catch (IllegalArgumentException ex) {
      sig = "-";

    } catch (MathException ex) {
      sig = "-";
    }
    return sig;
  }
}
