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

package io.github.mzmine.modules.dataprocessing.masscalibration;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_shoulderpeaksfilter.ShoulderPeaksFilter;
import io.github.mzmine.modules.dataprocessing.featdet_shoulderpeaksfilter.ShoulderPeaksFilterParameters;
import io.github.mzmine.modules.dataprocessing.masscalibration.errormodeling.DistributionRange;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerWindow;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.DataPointsDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.ScanDataSet;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.CollectionUtils;

import java.awt.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * This class extends ParameterSetupDialog class to include mass calibration plots. This is used to preview
 * how the chosen mass calibration setup will match peaks, estimate bias and calibrate the mass spectra.
 */
public class MassCalibrationSetupDialog extends ParameterSetupDialog {

  private RawDataFile[] dataFiles;
  private RawDataFile previewDataFile;

  // Dialog components
  private final BorderPane pnlPreviewFields;
  private final FlowPane pnlDataFile, pnlScanArrows, pnlScanNumber;
  private final VBox pnlControls;
  private final ComboBox<RawDataFile> comboDataFileName;
  private final ComboBox<Integer> comboScanNumber;
  private final CheckBox previewCheckBox;

  // XYPlot
  private final SpectraPlot spectrumPlot;

  private ParameterSet parameters;

//  JFreeChart distributionChart;
//  EChartViewer distributionChartViewer;

  private final ErrorDistributionChart errorDistributionChart;

  /**
   * @param parameters
   * @param massDetectorTypeNumber
   */
  public MassCalibrationSetupDialog(boolean valueCheckRequired, ParameterSet parameters) {

    super(valueCheckRequired, parameters);
    this.parameters = parameters;

    dataFiles = MZmineCore.getProjectManager().getCurrentProject().getDataFiles();

    // TODO: if (dataFiles.length == 0)
    // return;

    RawDataFile selectedFiles[] = MZmineCore.getDesktop().getSelectedDataFiles();

    if (selectedFiles.length > 0) {
      previewDataFile = selectedFiles[0];
    } else {
      previewDataFile = dataFiles[0];
    }

    previewCheckBox = new CheckBox("Show preview");

    paramsPane.add(new Separator(), 0, getNumberOfParameters() + 1);
    paramsPane.add(previewCheckBox, 0, getNumberOfParameters() + 2);

    // Elements of pnlLab
    pnlDataFile = new FlowPane();
    pnlDataFile.getChildren().add(new Label("Data file "));

    pnlScanNumber = new FlowPane();
    pnlScanNumber.getChildren().add(new Label("Scan number "));

    int scanNumbers[] = previewDataFile.getScanNumbers();
    ObservableList<Integer> scanNums =
        FXCollections.observableArrayList(CollectionUtils.toIntegerArray(scanNumbers));
    comboScanNumber = new ComboBox<Integer>(scanNums);
    comboScanNumber.getSelectionModel().select(0);
    comboScanNumber.getSelectionModel().selectedItemProperty().addListener((obs, old, newIndex) -> {
      parametersChanged();
    });

    comboDataFileName = new ComboBox<RawDataFile>(
        MZmineCore.getProjectManager().getCurrentProject().getRawDataFiles());
    comboDataFileName.getSelectionModel().select(previewDataFile);
    comboDataFileName.setOnAction(e -> {
      var previewDataFile = comboDataFileName.getSelectionModel().getSelectedItem();
      if (previewDataFile == null) {
        return;
      }
      int scanNumbers2[] = previewDataFile.getScanNumbers();
      ObservableList<Integer> scanNums2 =
          FXCollections.observableArrayList(CollectionUtils.toIntegerArray(scanNumbers2));

      comboScanNumber.setItems(scanNums2);
      comboScanNumber.getSelectionModel().select(0);
      parametersChanged();
    });

    pnlDataFile.getChildren().add(comboDataFileName);

    pnlScanArrows = new FlowPane();
    final String leftArrow = new String(new char[]{'\u2190'});
    Button leftArrowButton = new Button(leftArrow);
    leftArrowButton.setOnAction(e -> {
      int ind = comboScanNumber.getSelectionModel().getSelectedIndex() - 1;
      if (ind >= 0) {
        comboScanNumber.getSelectionModel().select(ind);
      }
    });

    final String rightArrow = new String(new char[]{'\u2192'});
    Button rightArrowButton = new Button(rightArrow);
    rightArrowButton.setOnAction(e -> {
      int ind = comboScanNumber.getSelectionModel().getSelectedIndex() + 1;
      if (ind < (comboScanNumber.getItems().size() - 1)) {
        comboScanNumber.getSelectionModel().select(ind);
      }
    });

    pnlScanArrows.getChildren().addAll(leftArrowButton, comboScanNumber, rightArrowButton);
    pnlScanNumber.getChildren().add(pnlScanArrows);

    spectrumPlot = new SpectraPlot();
    spectrumPlot.setMinSize(400, 300);

    /*distributionChart = createEmptyDistributionChart();
    distributionChartViewer = new EChartViewer(distributionChart);
//    distributionChartViewer = new EChartViewer(distributionChart, true, true, true, true, false);
    distributionChartViewer.setMinSize(400, 300);*/

    errorDistributionChart = new ErrorDistributionChart();
    errorDistributionChart.setMinSize(400, 300);

    pnlControls = new VBox();
    pnlControls.setSpacing(5);
    BorderPane.setAlignment(pnlControls, Pos.CENTER);

    // Put all together
    pnlControls.getChildren().add(pnlDataFile);
    pnlControls.getChildren().add(pnlScanNumber);
    pnlPreviewFields = new BorderPane();
    pnlPreviewFields.setCenter(pnlControls);
    pnlPreviewFields.visibleProperty().bind(previewCheckBox.selectedProperty());
    /*spectrumPlot.visibleProperty().bind(previewCheckBox.selectedProperty());
    spectrumPlot.visibleProperty().addListener((c, o, n) -> {
      if (n == true) {
        mainPane.setCenter(spectrumPlot);
        mainPane.setLeft(mainScrollPane);
        mainPane.autosize();
        mainPane.getScene().getWindow().sizeToScene();
        parametersChanged();
      } else {
        mainPane.setLeft(null);
        mainPane.setCenter(mainScrollPane);
        mainPane.autosize();
        mainPane.getScene().getWindow().sizeToScene();
      }
    });*/

    errorDistributionChart.visibleProperty().bind(previewCheckBox.selectedProperty());
    errorDistributionChart.visibleProperty().addListener((c, o, n) -> {
      if (n == true) {
        mainPane.setCenter(errorDistributionChart);
        mainPane.setLeft(mainScrollPane);
        mainPane.autosize();
        mainPane.getScene().getWindow().sizeToScene();
        parametersChanged();
      } else {
        mainPane.setLeft(null);
        mainPane.setCenter(mainScrollPane);
        mainPane.autosize();
        mainPane.getScene().getWindow().sizeToScene();
      }
    });

    paramsPane.add(pnlPreviewFields, 0, getNumberOfParameters() + 3, 2, 1);
  }

  /**
   * This method must be overloaded by derived class to load all the preview data sets into the
   * spectrumPlot
   */
//  protected abstract void loadPreview(SpectraPlot spectrumPlot, Scan previewScan);


//  protected void loadPreview(SpectraPlot spectrumPlot, Scan previewScan) {
  protected void loadPreview() {

    /*// Remove previous data sets
    spectrumPlot.removeAllDataSets();

    // Add scan data set
    ScanDataSet scanDataSet = new ScanDataSet(previewScan);
    spectrumPlot.addDataSet(scanDataSet, SpectraVisualizerWindow.scanColor, false);

    // If the scan is centroided, switch to centroid mode
    spectrumPlot.setPlotMode(previewScan.getSpectrumType());

    // If the parameters are not complete, exit
    ArrayList<String> errors = new ArrayList<String>();
    boolean paramsOK = parameters.checkParameterValues(errors);
    if (!paramsOK)
      return;

    // Get mass list
    String massListName =
        parameters.getParameter(ShoulderPeaksFilterParameters.massList).getValue();
    MassList massList = previewScan.getMassList(massListName);
    if (massList == null)
      return;

    // Perform filtering
    DataPoint mzValues[] = massList.getDataPoints();
    DataPoint remainingMzValues[] = ShoulderPeaksFilter.filterMassValues(mzValues, parameters);

    Vector<DataPoint> removedPeaks = new Vector<DataPoint>();
    removedPeaks.addAll(Arrays.asList(mzValues));
    removedPeaks.removeAll(Arrays.asList(remainingMzValues));
    DataPoint removedMzValues[] = removedPeaks.toArray(new DataPoint[0]);

    // Add mass list data sets
    DataPointsDataSet removedPeaksDataSet = new DataPointsDataSet("Removed peaks", removedMzValues);
    DataPointsDataSet remainingPeaksDataSet =
        new DataPointsDataSet("Remaining peaks", remainingMzValues);

//    spectrumPlot.addDataSet(removedPeaksDataSet, removedPeaksColor, false);
    spectrumPlot.addDataSet(remainingPeaksDataSet, SpectraVisualizerWindow.peaksColor, false);*/

    ArrayList<String> errors = new ArrayList<String>();
//    boolean paramsOK = parameters.checkParameterValues(errors);
    boolean paramsOK = parameterSet.checkParameterValues(errors);
    if (!paramsOK)
      return;

    RawDataFile previewDataFile =  comboDataFileName.getSelectionModel().getSelectedItem();
    MassCalibrationTask previewTask = new MassCalibrationTask(previewDataFile, parameterSet);
    previewTask.run();

    if (previewTask.getStatus() != TaskStatus.FINISHED) {
      return;
    }

//    distributionChart = createDistributionChart(previewTask.getErrors(), previewTask.getErrorRanges(),
//            previewTask.getBiasEstimate());


    /*XYPlot distributionPlot = distributionChart.getXYPlot();
    cleanDistributionPlot(distributionPlot);
    updateDistributionPlot(distributionPlot, previewTask.getErrors(), previewTask.getErrorRanges(),
            previewTask.getBiasEstimate());*/

    errorDistributionChart.cleanDistributionPlot();
    errorDistributionChart.updateDistributionPlot(previewTask.getErrors(), previewTask.getErrorRanges(),
            previewTask.getBiasEstimate());

  }

  private void updateTitle(Scan currentScan) {

    // Formats
    NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
    NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
    NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();

    // Set window and plot titles
    String title = "[" + previewDataFile.getName() + "] scan #" + currentScan.getScanNumber();

    String subTitle =
        "MS" + currentScan.getMSLevel() + ", RT " + rtFormat.format(currentScan.getRetentionTime());

    DataPoint basePeak = currentScan.getHighestDataPoint();
    if (basePeak != null) {
      subTitle += ", base peak: " + mzFormat.format(basePeak.getMZ()) + " m/z ("
          + intensityFormat.format(basePeak.getIntensity()) + ")";
    }
    spectrumPlot.setTitle(title, subTitle);

  }

  protected void cleanDistributionPlot(XYPlot distributionPlot) {
    for (int i = 0; i < distributionPlot.getDatasetCount(); i++) {
      distributionPlot.setDataset(i, null);
    }
    distributionPlot.clearRangeMarkers();
  }

  protected void updateDistributionPlot(XYPlot distributionPlot, List<Double> errors, Map<String,
          DistributionRange> errorRanges, double biasEstimate) {
    XYDataset dataset = createDistributionDataset(errors);
    distributionPlot.setDataset(dataset);

//    String padding = " ".repeat(50);
    String padding = " ".repeat(0);
    for(String label: errorRanges.keySet()) {
      DistributionRange errorRange = errorRanges.get(label);
      Range<Double> errorValueRange = errorRange.getValueRange();

      ValueMarker valueMarkerLower = createValueMarker(padding + label + " lower", errorValueRange.lowerEndpoint());
      ValueMarker valueMarkerUpper = createValueMarker(padding + label + " upper", errorValueRange.upperEndpoint());
      distributionPlot.addRangeMarker(valueMarkerLower);
      distributionPlot.addRangeMarker(valueMarkerUpper);
    }
		distributionPlot.addRangeMarker(createValueMarker(padding + "Bias estimate", biasEstimate));
  }

  protected JFreeChart createEmptyDistributionChart() {
    String title = "Error distribution";
		XYItemRenderer renderer = new StandardXYItemRenderer(StandardXYItemRenderer.SHAPES);
		NumberAxis xAxis = new NumberAxis("Match number");
		NumberAxis yAxis = new NumberAxis("PPM error");
		XYPlot plot = new XYPlot(null, xAxis, yAxis, renderer);
		plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
		plot.setDomainCrosshairVisible(false);
		plot.setRangeCrosshairVisible(false);

		return new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
  }

  protected JFreeChart createDistributionChart(List<Double> errors, Map<String, DistributionRange> errorRanges,
                                               double biasEstimate) {
    String title = "Error distribution";
    XYDataset dataset = createDistributionDataset(errors);
		XYItemRenderer renderer = new StandardXYItemRenderer(StandardXYItemRenderer.SHAPES);
		NumberAxis xAxis = new NumberAxis("Match number");
		NumberAxis yAxis = new NumberAxis("PPM error");
		XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
		plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);

		for(String label: errorRanges.keySet())
		{
			DistributionRange errorRange = errorRanges.get(label);
      Range<Double> errorValueRange = errorRange.getValueRange();

			ValueMarker valueMarkerLower = createValueMarker(label + " lower", errorValueRange.lowerEndpoint());
			ValueMarker valueMarkerUpper = createValueMarker(label + " upper", errorValueRange.upperEndpoint());
			plot.addRangeMarker(valueMarkerLower);
			plot.addRangeMarker(valueMarkerUpper);
		}
		plot.addRangeMarker(createValueMarker("Bias estimate", biasEstimate));


		return new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
  }

  protected XYDataset createDistributionDataset(List<Double> errors) {
    XYSeries errorsXY = new XYSeries("PPM errors");
		for(int i = 0; i < errors.size(); i++)
		{
			errorsXY.add(i+1, errors.get(i));
		}

		return new XYSeriesCollection(errorsXY);
  }

  protected ValueMarker createValueMarker(String label, double value) {
    ValueMarker valueMarker = new ValueMarker(value);
    valueMarker.setLabel(label);
    valueMarker.setPaint(Color.black);
//    valueMarker.setLabelAnchor(RectangleAnchor.BOTTOM_LEFT);
    valueMarker.setLabelTextAnchor(TextAnchor.BASELINE_LEFT);
    return valueMarker;
  }

  @Override
  protected void parametersChanged() {

    if (true) {
      if (!previewCheckBox.isSelected()) {
        return;
      }

      updateParameterSetFromComponents();
      loadPreview();
      return;
    }

    // Update preview as parameters have changed
    if ((comboScanNumber == null) || (!previewCheckBox.isSelected())) {
      return;
    }

    Integer scanNumber = comboScanNumber.getSelectionModel().getSelectedItem();
    if (scanNumber == null) {
      return;
    }

    Scan currentScan = previewDataFile.getScan(scanNumber);
    updateParameterSetFromComponents();
//    loadPreview(spectrumPlot, currentScan);
    updateTitle(currentScan);
  }
}
