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
package io.github.mzmine.modules.dataanalysis.clustering;

import java.text.DecimalFormat;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.SwingUtilities;
import org.jfree.data.xy.AbstractXYDataset;
import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.Desktop;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.dataanalysis.clustering.hierarchical.HierarClusterer;
import io.github.mzmine.modules.dataanalysis.projectionplots.ProjectionPlotDataset;
import io.github.mzmine.modules.dataanalysis.projectionplots.ProjectionPlotWindow;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.PeakMeasurementType;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import jmprojection.PCA;
import jmprojection.Preprocess;
import jmprojection.ProjectionStatus;
import jmprojection.Sammons;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.gui.hierarchyvisualizer.HierarchyVisualizer;

public class ClusteringTask extends AbstractXYDataset implements ProjectionPlotDataset {

  private static final long serialVersionUID = 1L;

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private double[] component1Coords;
  private double[] component2Coords;
  private ParameterSet parameters;
  private RawDataFile[] selectedRawDataFiles;
  private PeakListRow[] selectedRows;
  private int[] groupsForSelectedRawDataFiles, groupsForSelectedVariables;
  private Object[] parameterValuesForGroups;
  private int finalNumberOfGroups;
  private String datasetTitle;
  private int xAxisDimension = 1;
  private int yAxisDimension = 2;
  private TaskStatus status = TaskStatus.WAITING;
  private String errorMessage;
  private ProjectionStatus projectionStatus;
  private MZmineProcessingStep<ClusteringAlgorithm> clusteringStep;
  private ClusteringDataType typeOfData;
  private Instances dataset;
  private int progress;
  private PeakList peakList;

  public ClusteringTask(ParameterSet parameters) {

    this.parameters = parameters;

    this.peakList = parameters.getParameter(ClusteringParameters.peakLists).getValue()
        .getMatchingPeakLists()[0];
    this.selectedRawDataFiles = parameters.getParameter(ClusteringParameters.dataFiles).getValue()
        .getMatchingRawDataFiles();
    this.selectedRows =
        parameters.getParameter(ClusteringParameters.rows).getMatchingRows(peakList);
    clusteringStep = parameters.getParameter(ClusteringParameters.clusteringAlgorithm).getValue();
    typeOfData = parameters.getParameter(ClusteringParameters.typeOfData).getValue();

    datasetTitle = "Clustering";

    // Determine groups for selected raw data files
    groupsForSelectedRawDataFiles = new int[selectedRawDataFiles.length];
    groupsForSelectedVariables = new int[selectedRows.length];
  }

  @Override
  public String toString() {
    return datasetTitle;
  }

  @Override
  public String getXLabel() {
    return "1st projected dimension";
  }

  @Override
  public String getYLabel() {
    return "2nd projected dimension";
  }

  @Override
  public int getSeriesCount() {
    return 1;
  }

  @Override
  public Comparable<?> getSeriesKey(int series) {
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
    if (typeOfData == ClusteringDataType.VARIABLES) {
      String name = "ID: " + this.selectedRows[item].getID();
      name += " M/Z: " + this.selectedRows[item].getAverageMZ() + " RT:"
          + this.selectedRows[item].getAverageRT();
      if (selectedRows[item].getPeakIdentities() != null
          && selectedRows[item].getPeakIdentities().length > 0) {
        name += " CompoundName: " + selectedRows[item].getPeakIdentities()[0].getName();
      }
      return name;
    } else {
      return selectedRawDataFiles[item].getName();
    }
  }

  @Override
  public int getGroupNumber(int item) {
    if (typeOfData == ClusteringDataType.VARIABLES) {
      return groupsForSelectedVariables[item];
    } else {
      return groupsForSelectedRawDataFiles[item];
    }
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
    return finalNumberOfGroups;
  }

  @Override
  public void run() {

    status = TaskStatus.PROCESSING;

    logger.info("Clustering");

    double[][] rawData;

    // Creating weka dataset using samples or metabolites (variables)

    if (typeOfData == ClusteringDataType.VARIABLES) {
      rawData = createMatrix(false);
      dataset = createVariableWekaDataset(rawData);
    } else {
      rawData = createMatrix(true);
      dataset = createSampleWekaDataset(rawData);
    }

    // Run the clustering algorithm
    ClusteringAlgorithm clusteringAlgorithm = clusteringStep.getModule();
    ParameterSet clusteringParameters = clusteringStep.getParameterSet();
    ClusteringResult result = clusteringAlgorithm.performClustering(dataset, clusteringParameters);

    String cluster = "";
    if (clusteringAlgorithm.getName().toString().equals("Hierarchical clusterer")) {
      progress = 0;
      // Getting the result of the clustering in Newick format
      cluster = result.getHiearchicalCluster();

      // Getting the number of clusters counting the number of times the
      // word "cluster" is in the result
      Pattern p = Pattern.compile("Cluster", Pattern.LITERAL | Pattern.CASE_INSENSITIVE);
      int numberOfClusters = p.split(cluster, -1).length - 1;
      if (numberOfClusters == 0) {
        numberOfClusters = 1;
      }

      // Visualization window for each cluster
      for (int i = 0; i < numberOfClusters; i++) {
        String c = null;
        String clusterNumber = "Cluster " + i;
        if (cluster.indexOf(clusterNumber) > 0) {
          int nextNumber = i + 1;
          String clusterNumber2 = "Cluster " + nextNumber;

          if (cluster.indexOf(clusterNumber2) < 0) {
            c = cluster.substring(cluster.indexOf(clusterNumber) + clusterNumber.length(),
                cluster.length());
          } else {
            c = cluster.substring(cluster.indexOf(clusterNumber) + clusterNumber.length(),
                cluster.indexOf(clusterNumber2));
          }
        } else {
          c = cluster;
        }

        HierarchyVisualizer visualizer = new HierarchyVisualizer(c);
        SwingNode sn = new SwingNode();
        visualizer.fitToScreen();
        SwingUtilities.invokeLater(() -> {
          sn.setContent(visualizer);
        });

        BorderPane visualizationPane = new BorderPane();
        visualizationPane.setCenter(sn);
        Scene visualizationWindowScene = new Scene(visualizationPane);

        // Text field with the clustering result in Newick format
        TextField data = new TextField(c);
        visualizationPane.setBottom(data);

        Platform.runLater(() -> {
          Stage visualizationWindow = new Stage();
          visualizationWindow.setTitle(clusterNumber);
          visualizationWindow.setScene(visualizationWindowScene);
          visualizationWindow.setMinWidth(600.0);
          visualizationWindow.setMinHeight(500.0);
          visualizationWindow.show();
        });

      }
      progress = 100;
    } else {

      List<Integer> clusteringResult = result.getClusters();

      // Report window
      Desktop desktop = MZmineCore.getDesktop();
      if (typeOfData == ClusteringDataType.SAMPLES) {
        String[] sampleNames = new String[selectedRawDataFiles.length];
        for (int i = 0; i < selectedRawDataFiles.length; i++) {
          sampleNames[i] = selectedRawDataFiles[i].getName();
        }

        ClusteringReportWindow reportWindow = new ClusteringReportWindow(sampleNames,
            clusteringResult.toArray(new Integer[0]), "Clustering Report");
        reportWindow.show();
      } else {
        String[] variableNames = new String[selectedRows.length];
        for (int i = 0; i < selectedRows.length; i++) {
          variableNames[i] = selectedRows[i].getID() + " - " + selectedRows[i].getAverageMZ()
              + " - " + selectedRows[i].getAverageRT();
          if (selectedRows[i].getPeakIdentities() != null
              && selectedRows[i].getPeakIdentities().length > 0) {
            variableNames[i] += " - " + selectedRows[i].getPeakIdentities()[0].getName();
          }
        }

        ClusteringReportWindow reportWindow = new ClusteringReportWindow(variableNames,
            clusteringResult.toArray(new Integer[0]), "Clustering Report");
        reportWindow.show();

      }

      // Visualization
      if (typeOfData == ClusteringDataType.VARIABLES) {
        for (int ind = 0; ind < selectedRows.length; ind++) {
          groupsForSelectedVariables[ind] = clusteringResult.get(ind);
        }

      } else {
        for (int ind = 0; ind < selectedRawDataFiles.length; ind++) {
          groupsForSelectedRawDataFiles[ind] = clusteringResult.get(ind);
        }
      }

      this.finalNumberOfGroups = result.getNumberOfGroups();
      parameterValuesForGroups = new Object[finalNumberOfGroups];
      for (int i = 0; i < finalNumberOfGroups; i++) {
        parameterValuesForGroups[i] = "Group " + i;
      }

      int numComponents = xAxisDimension;
      if (yAxisDimension > numComponents) {
        numComponents = yAxisDimension;
      }

      if (result.getVisualizationType() == VisualizationType.PCA) {
        // Scale data and do PCA
        Preprocess.scaleToUnityVariance(rawData);
        PCA pcaProj = new PCA(rawData, numComponents);
        projectionStatus = pcaProj.getProjectionStatus();

        double[][] pcaResult = pcaProj.getState();

        if (status == TaskStatus.CANCELED) {
          return;
        }

        component1Coords = pcaResult[xAxisDimension - 1];
        component2Coords = pcaResult[yAxisDimension - 1];
      } else if (result.getVisualizationType() == VisualizationType.SAMMONS) {
        // Scale data and do Sammon's mapping
        Preprocess.scaleToUnityVariance(rawData);
        Sammons sammonsProj = new Sammons(rawData);
        projectionStatus = sammonsProj.getProjectionStatus();

        sammonsProj.iterate(100);

        double[][] sammonsResult = sammonsProj.getState();

        if (status == TaskStatus.CANCELED) {
          return;
        }

        component1Coords = sammonsResult[xAxisDimension - 1];
        component2Coords = sammonsResult[yAxisDimension - 1];
      }

      Platform.runLater(() -> {
        ProjectionPlotWindow newFrame =
            new ProjectionPlotWindow(desktop.getSelectedPeakLists()[0], this, parameters);
        newFrame.show();
      });
    }
    status = TaskStatus.FINISHED;
    logger.info("Finished computing Clustering visualization.");
  }

  /**
   * Creates a matrix of heights of areas
   *
   * @param isForSamples
   * @return
   */
  private double[][] createMatrix(boolean isForSamples) {
    // Generate matrix of raw data (input to CDA)
    boolean useArea = true;
    if (parameters.getParameter(ClusteringParameters.peakMeasurementType)
        .getValue() == PeakMeasurementType.AREA) {
      useArea = true;
    }
    if (parameters.getParameter(ClusteringParameters.peakMeasurementType)
        .getValue() == PeakMeasurementType.HEIGHT) {
      useArea = false;
    }
    double[][] rawData;
    if (isForSamples) {
      rawData = new double[selectedRawDataFiles.length][selectedRows.length];
      for (int rowIndex = 0; rowIndex < selectedRows.length; rowIndex++) {
        PeakListRow peakListRow = selectedRows[rowIndex];
        for (int fileIndex = 0; fileIndex < selectedRawDataFiles.length; fileIndex++) {
          RawDataFile rawDataFile = selectedRawDataFiles[fileIndex];
          Feature p = peakListRow.getPeak(rawDataFile);
          if (p != null) {
            if (useArea) {
              rawData[fileIndex][rowIndex] = p.getArea();
            } else {
              rawData[fileIndex][rowIndex] = p.getHeight();
            }
          }
        }
      }
    } else {
      rawData = new double[selectedRows.length][selectedRawDataFiles.length];
      for (int rowIndex = 0; rowIndex < selectedRows.length; rowIndex++) {
        PeakListRow peakListRow = selectedRows[rowIndex];
        for (int fileIndex = 0; fileIndex < selectedRawDataFiles.length; fileIndex++) {
          RawDataFile rawDataFile = selectedRawDataFiles[fileIndex];
          Feature p = peakListRow.getPeak(rawDataFile);
          if (p != null) {
            if (useArea) {
              rawData[rowIndex][fileIndex] = p.getArea();
            } else {
              rawData[rowIndex][fileIndex] = p.getHeight();
            }
          }
        }
      }
    }

    return rawData;
  }

  /**
   * Creates the weka data set for clustering of samples
   *
   * @param rawData Data extracted from selected Raw data files and rows.
   * @return Weka library data set
   */
  private Instances createSampleWekaDataset(double[][] rawData) {
    FastVector attributes = new FastVector();

    for (int i = 0; i < rawData[0].length; i++) {
      String varName = "Var" + i;
      Attribute var = new Attribute(varName);
      attributes.addElement(var);
    }

    if (clusteringStep.getModule().getClass().equals(HierarClusterer.class)) {
      Attribute name = new Attribute("name", (FastVector) null);
      attributes.addElement(name);
    }
    Instances data = new Instances("Dataset", attributes, 0);

    for (int i = 0; i < rawData.length; i++) {
      double[] values = new double[data.numAttributes()];
      System.arraycopy(rawData[i], 0, values, 0, rawData[0].length);
      if (clusteringStep.getModule().getClass().equals(HierarClusterer.class)) {
        values[data.numAttributes() - 1] =
            data.attribute("name").addStringValue(this.selectedRawDataFiles[i].getName());
      }
      Instance inst = new SparseInstance(1.0, values);
      data.add(inst);
    }
    return data;
  }

  /**
   * Creates the weka data set for clustering of variables (metabolites)
   *
   * @param rawData Data extracted from selected Raw data files and rows.
   * @return Weka library data set
   */
  private Instances createVariableWekaDataset(double[][] rawData) {
    FastVector attributes = new FastVector();

    for (int i = 0; i < this.selectedRawDataFiles.length; i++) {
      String varName = "Var" + i;
      Attribute var = new Attribute(varName);
      attributes.addElement(var);
    }

    if (clusteringStep.getModule().getClass().equals(HierarClusterer.class)) {
      Attribute name = new Attribute("name", (FastVector) null);
      attributes.addElement(name);
    }
    Instances data = new Instances("Dataset", attributes, 0);

    for (int i = 0; i < selectedRows.length; i++) {
      double[] values = new double[data.numAttributes()];
      System.arraycopy(rawData[i], 0, values, 0, rawData[0].length);

      if (clusteringStep.getModule().getClass().equals(HierarClusterer.class)) {
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        double MZ = Double.valueOf(twoDForm.format(selectedRows[i].getAverageMZ()));
        double RT = Double.valueOf(twoDForm.format(selectedRows[i].getAverageRT()));
        String rowName = "MZ->" + MZ + "/RT->" + RT;
        values[data.numAttributes() - 1] = data.attribute("name").addStringValue(rowName);
      }
      Instance inst = new SparseInstance(1.0, values);
      data.add(inst);
    }
    return data;
  }

  @Override
  public void cancel() {
    if (projectionStatus != null) {
      projectionStatus.cancel();
    }

    status = TaskStatus.CANCELED;
  }

  @Override
  public String getErrorMessage() {
    return errorMessage;
  }

  @Override
  public TaskStatus getStatus() {
    return status;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#setStatus()
   */
  public void setStatus(TaskStatus newStatus) {
    this.status = newStatus;
  }

  public boolean isCanceled() {
    return status == TaskStatus.CANCELED;
  }

  public boolean isFinished() {
    return status == TaskStatus.FINISHED;
  }

  @Override
  public String getTaskDescription() {
    return "Clustering visualization " + datasetTitle;
  }

  @Override
  public double getFinishedPercentage() {
    if (this.projectionStatus != null) {
      if (projectionStatus.getFinishedPercentage() > 1.0) {
        return 1.0;
      }
      return projectionStatus.getFinishedPercentage();
    } else {
      if (progress > 100) {
        return 1.0;
      }
      progress++;
      return ((double) progress / 100);
    }
  }

  @Override
  public TaskPriority getTaskPriority() {
    return TaskPriority.NORMAL;
  }
}
