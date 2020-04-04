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

package io.github.mzmine.modules.visualization.spectra.simplespectra;

import java.awt.Color;
import java.io.File;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Logger;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.data.xy.XYDataset;
import com.google.common.base.Strings;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.exportscans.ExportScansModule;
import io.github.mzmine.modules.io.spectraldbsubmit.view.MSMSLibrarySubmissionWindow;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingManager;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.IsotopesDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.PeakListDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.ScanDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.SinglePeakDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.customdatabase.CustomDBSpectraSearchModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.lipidsearch.LipidSpectraSearchModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.onlinedatabase.OnlineDBSpectraSearchModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.spectraldatabase.SpectraIdentificationSpectralDatabaseModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.sumformula.SumFormulaSpectraSearchModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.dialogs.AxesSetupDialog;
import io.github.mzmine.util.javafx.FxColorUtil;
import io.github.mzmine.util.javafx.FxIconUtil;
import io.github.mzmine.util.javafx.WindowsMenu;
import io.github.mzmine.util.scans.ScanUtils;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Spectrum visualizer using JFreeChart library
 */
public class SpectraVisualizerWindow extends Stage {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private static final Image centroidIcon =
      FxIconUtil.loadImageFromResources("icons/centroidicon.png");
  private static final Image continuousIcon =
      FxIconUtil.loadImageFromResources("icons/continuousicon.png");
  private static final Image dataPointsIcon =
      FxIconUtil.loadImageFromResources("icons/datapointsicon.png");
  private static final Image annotationsIcon =
      FxIconUtil.loadImageFromResources("icons/annotationsicon.png");
  private static final Image pickedPeakIcon =
      FxIconUtil.loadImageFromResources("icons/pickedpeakicon.png");
  private static final Image isotopePeakIcon =
      FxIconUtil.loadImageFromResources("icons/isotopepeakicon.png");
  private static final Image axesIcon = FxIconUtil.loadImageFromResources("icons/axesicon.png");
  private static final Image exportIcon = FxIconUtil.loadImageFromResources("icons/exporticon.png");
  private static final Image dbOnlineIcon =
      FxIconUtil.loadImageFromResources("icons/DBOnlineIcon.png");
  private static final Image dbCustomIcon =
      FxIconUtil.loadImageFromResources("icons/DBCustomIcon.png");
  private static final Image dbLipidsIcon =
      FxIconUtil.loadImageFromResources("icons/DBLipidsIcon.png");
  private static final Image dbSpectraIcon =
      FxIconUtil.loadImageFromResources("icons/DBSpectraIcon.png");
  private static final Image sumFormulaIcon = FxIconUtil.loadImageFromResources("icons/search.png");

  // initialize colors to some default before the color palette is loaded
  public static Color scanColor = new Color(0, 0, 192);
  public static Color peaksColor = Color.red;
  public static Color singlePeakColor = Color.magenta;
  public static Color detectedIsotopesColor = Color.magenta;
  public static Color predictedIsotopesColor = Color.green;

  private final Scene mainScene;
  private final BorderPane mainPane;
  private final ToolBar toolBar;
  private final Button centroidContinuousButton, dataPointsButton, annotationsButton,
      pickedPeakButton, isotopePeakButton, axesButton, exportButton, createLibraryEntryButton,
      dbOnlineButton, dbCustomButton, dbLipidsButton, dbSpectraButton, sumFormulaButton;
  private final SpectraPlot spectrumPlot;
  private final SpectraBottomPanel bottomPanel;

  private RawDataFile dataFile;

  // Currently loaded scan
  private Scan currentScan;

  private File lastExportDirectory;

  // Current scan data set
  private ScanDataSet spectrumDataSet;

  private ParameterSet paramSet;

  private boolean dppmWindowOpen;

  private static final double zoomCoefficient = 1.2f;

  public SpectraVisualizerWindow(RawDataFile dataFile, boolean enableProcessing) {

    setTitle("Spectrum loading...");
    this.dataFile = dataFile;

    loadColorSettings();

    mainPane = new BorderPane();
    mainScene = new Scene(mainPane);
    setScene(mainScene);

    // setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    // setBackground(Color.white);

    spectrumPlot = new SpectraPlot(enableProcessing);
    mainPane.setCenter(spectrumPlot);

    toolBar = new ToolBar();
    toolBar.setOrientation(Orientation.VERTICAL);

    centroidContinuousButton = new Button(null, new ImageView(centroidIcon));
    centroidContinuousButton.setTooltip(new Tooltip("Toggle centroid/continuous mode"));

    dataPointsButton = new Button(null, new ImageView(dataPointsIcon));
    dataPointsButton.setTooltip(new Tooltip("Toggle displaying of data points in continuous mode"));
    dataPointsButton.setOnAction(e -> spectrumPlot.switchDataPointsVisible());

    centroidContinuousButton.setOnAction(e -> {
      if (spectrumPlot.getPlotMode() == MassSpectrumType.CENTROIDED) {
        spectrumPlot.setPlotMode(MassSpectrumType.PROFILE);
        centroidContinuousButton.setGraphic(new ImageView(centroidIcon));
        dataPointsButton.setDisable(false);
      } else {
        spectrumPlot.setPlotMode(MassSpectrumType.CENTROIDED);
        centroidContinuousButton.setGraphic(new ImageView(continuousIcon));
        dataPointsButton.setDisable(true);
      }
    });

    annotationsButton = new Button(null, new ImageView(annotationsIcon));
    annotationsButton.setTooltip(new Tooltip("Toggle displaying of peak values"));
    annotationsButton.setOnAction(e -> spectrumPlot.switchItemLabelsVisible());

    pickedPeakButton = new Button(null, new ImageView(pickedPeakIcon));
    pickedPeakButton.setTooltip(new Tooltip("Toggle displaying of picked peaks"));
    pickedPeakButton.setOnAction(e -> spectrumPlot.switchPickedPeaksVisible());

    isotopePeakButton = new Button(null, new ImageView(isotopePeakIcon));
    isotopePeakButton.setTooltip(new Tooltip("Toggle displaying of predicted isotope peaks"));
    isotopePeakButton.setOnAction(e -> spectrumPlot.switchIsotopePeaksVisible());

    axesButton = new Button(null, new ImageView(axesIcon));
    axesButton.setTooltip(new Tooltip("Setup ranges for axes"));
    axesButton.setOnAction(e -> {
      AxesSetupDialog dialog = new AxesSetupDialog(this, spectrumPlot.getXYPlot());
      dialog.show();
    });

    exportButton = new Button(null, new ImageView(exportIcon));
    exportButton.setTooltip(new Tooltip("Export spectra to spectra file"));
    exportButton.setOnAction(e -> ExportScansModule.showSetupDialog(currentScan));

    createLibraryEntryButton = new Button(null, new ImageView(exportIcon));
    createLibraryEntryButton.setTooltip(new Tooltip("Create spectral library entry"));
    createLibraryEntryButton.setOnAction(e -> {
      // open window with all selected rows
      MSMSLibrarySubmissionWindow libraryWindow = new MSMSLibrarySubmissionWindow();
      libraryWindow.setData(currentScan);
      libraryWindow.setVisible(true);
    });

    dbOnlineButton = new Button(null, new ImageView(dbOnlineIcon));
    dbOnlineButton.setTooltip(new Tooltip("Select online database for annotation"));
    dbOnlineButton.setOnAction(e -> {
      OnlineDBSpectraSearchModule.showSpectraIdentificationDialog(currentScan, spectrumPlot);
    });

    dbCustomButton = new Button(null, new ImageView(dbCustomIcon));
    dbCustomButton.setTooltip(new Tooltip("Select custom database for annotation"));
    dbCustomButton.setOnAction(e -> {
      CustomDBSpectraSearchModule.showSpectraIdentificationDialog(currentScan, spectrumPlot);
    });

    dbLipidsButton = new Button(null, new ImageView(dbLipidsIcon));
    dbLipidsButton.setTooltip(new Tooltip("Select target lipid classes for annotation"));
    dbLipidsButton.setOnAction(e -> {
      LipidSpectraSearchModule.showSpectraIdentificationDialog(currentScan, spectrumPlot);
    });

    dbSpectraButton = new Button(null, new ImageView(dbSpectraIcon));
    dbSpectraButton.setTooltip(new Tooltip("Compare spectrum with spectral database"));
    dbSpectraButton.setOnAction(e -> {
      SpectraIdentificationSpectralDatabaseModule.showSpectraIdentificationDialog(currentScan,
          spectrumPlot);
    });

    sumFormulaButton = new Button(null, new ImageView(sumFormulaIcon));
    sumFormulaButton.setTooltip(new Tooltip("Predict sum formulas for annotation"));
    sumFormulaButton.setOnAction(e -> {
      SumFormulaSpectraSearchModule.showSpectraIdentificationDialog(currentScan, spectrumPlot);
    });

    toolBar.getItems().addAll(centroidContinuousButton, dataPointsButton, annotationsButton,
        pickedPeakButton, isotopePeakButton, axesButton, exportButton, createLibraryEntryButton,
        dbOnlineButton, dbCustomButton, dbLipidsButton, dbSpectraButton, sumFormulaButton);
    mainPane.setRight(toolBar);

    // Create relationship between the current Plot and Tool bar
    // spectrumPlot.setRelatedToolBar(toolBar);

    bottomPanel = new SpectraBottomPanel(this, dataFile);
    mainPane.setBottom(bottomPanel);

    // MZmineCore.getDesktop().addPeakListTreeListener(bottomPanel);

    // Add the Windows menu
    // JMenuBar menuBar = new JMenuBar();
    // menuBar.add(new WindowsMenu());
    // setJMenuBar(menuBar);

    // pack();

    // get the window settings parameter
    paramSet = MZmineCore.getConfiguration().getModuleParameters(SpectraVisualizerModule.class);
    WindowSettingsParameter settings =
        paramSet.getParameter(SpectraVisualizerParameters.windowSettings);

    // update the window and listen for changes
    settings.applySettingsToWindow(this);

    dppmWindowOpen = false;

    // Add the Windows menu
    WindowsMenu.addWindowsMenu(mainScene);
  }

  public SpectraVisualizerWindow(RawDataFile dataFile) {
    this(dataFile, false);
  }

  private void loadColorSettings() {
    SimpleColorPalette p = MZmineCore.getConfiguration().getDefaultColorPalette();
    scanColor = FxColorUtil.fxColorToAWT(p.get(0));
    peaksColor = FxColorUtil.fxColorToAWT(p.getNextColor());
    singlePeakColor = FxColorUtil.fxColorToAWT(p.getNextColor());
    detectedIsotopesColor = FxColorUtil.fxColorToAWT(p.getNextColor());
    predictedIsotopesColor = FxColorUtil.fxColorToAWT(p.getNextColor());
  }

  public void loadRawData(Scan scan) {

    logger.finest(
        "Loading scan #" + scan.getScanNumber() + " from " + dataFile + " for spectra visualizer");

    spectrumDataSet = new ScanDataSet(scan);

    this.currentScan = scan;

    // If the plot mode has not been set yet, set it accordingly
    if (spectrumPlot.getPlotMode() == null) {
      spectrumPlot.setPlotMode(currentScan.getSpectrumType());
      if (currentScan.getSpectrumType() == MassSpectrumType.CENTROIDED) {
        spectrumPlot.setPlotMode(MassSpectrumType.CENTROIDED);
        centroidContinuousButton.setGraphic(new ImageView(continuousIcon));
      } else {
        spectrumPlot.setPlotMode(MassSpectrumType.PROFILE);
        centroidContinuousButton.setGraphic(new ImageView(centroidIcon));
      }
    }

    // Clean up the MS/MS selector combo

    final ComboBox<String> msmsSelector = bottomPanel.getMSMSSelector();

    // We disable the MSMS selector first and then enable it again later
    // after updating the items. If we skip this, the size of the
    // selector may not be adjusted properly (timing issues?)
    msmsSelector.setDisable(true);

    msmsSelector.getItems().clear();
    boolean msmsVisible = false;

    // Add parent scan to MS/MS selector combo

    NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
    NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
    NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();

    // Add all fragment scans to MS/MS selector combo
    int fragmentScans[] = currentScan.getFragmentScanNumbers();
    if (fragmentScans != null) {
      for (int fragment : fragmentScans) {
        Scan fragmentScan = dataFile.getScan(fragment);
        if (fragmentScan == null)
          continue;
        final String itemText = "Fragment scan #" + fragment + ", RT: "
            + rtFormat.format(fragmentScan.getRetentionTime()) + ", precursor m/z: "
            + mzFormat.format(fragmentScan.getPrecursorMZ());
        // Updating the combo in other than Swing thread may cause
        // exception
        msmsSelector.getItems().add(itemText);
        msmsVisible = true;
      }
    }

    msmsSelector.setDisable(false);

    // Update the visibility of MS/MS selection combo
    bottomPanel.setMSMSSelectorVisible(msmsVisible);

    // Set window and plot titles
    String windowTitle =
        "Spectrum: [" + dataFile.getName() + "; scan #" + currentScan.getScanNumber() + "]";

    String spectrumTitle = ScanUtils.scanToString(currentScan, true);

    DataPoint basePeak = currentScan.getHighestDataPoint();
    if (basePeak != null) {
      spectrumTitle += ", base peak: " + mzFormat.format(basePeak.getMZ()) + " m/z ("
          + intensityFormat.format(basePeak.getIntensity()) + ")";
    }
    String spectrumSubtitle = null;
    if (!Strings.isNullOrEmpty(currentScan.getScanDefinition())) {
      spectrumSubtitle = "Scan definition: " + currentScan.getScanDefinition();
    }

    final String finalSpectrumTitle = spectrumTitle;
    final String finalSpectrumSubtitle = spectrumSubtitle;

    Platform.runLater(() -> {
      setTitle(windowTitle);
      spectrumPlot.setTitle(finalSpectrumTitle, finalSpectrumSubtitle);

      // Set plot data set
      spectrumPlot.removeAllDataSets();
      spectrumPlot.addDataSet(spectrumDataSet, scanColor, false);
    });

  }

  public void loadPeaks(PeakList selectedPeakList) {

    spectrumPlot.removePeakListDataSets();

    if (selectedPeakList == null) {
      return;
    }

    logger.finest(
        "Loading a feature list " + selectedPeakList + " to a spectrum window " + getTitle());

    PeakListDataSet peaksDataSet =
        new PeakListDataSet(dataFile, currentScan.getScanNumber(), selectedPeakList);

    // Set plot data sets
    spectrumPlot.addDataSet(peaksDataSet, peaksColor, true);

  }

  public void loadSinglePeak(Feature peak) {

    SinglePeakDataSet peakDataSet = new SinglePeakDataSet(currentScan.getScanNumber(), peak);

    // Set plot data sets
    spectrumPlot.addDataSet(peakDataSet, singlePeakColor, true);

  }

  public void loadIsotopes(IsotopePattern newPattern) {
    // We need to find a normalization factor for the new isotope
    // pattern, to show meaningful intensity range
    double mz = newPattern.getHighestDataPoint().getMZ();
    Range<Double> searchMZRange = Range.closed(mz - 0.5, mz + 0.5);
    ScanDataSet scanDataSet = spectrumPlot.getMainScanDataSet();
    double normalizationFactor = scanDataSet.getHighestIntensity(searchMZRange);

    // If normalization factor is 0, it means there were no data points
    // in given m/z range. In such case we use the max intensity of
    // whole scan as normalization factor.
    if (normalizationFactor == 0) {
      searchMZRange = Range.atLeast(0.0);
      normalizationFactor = scanDataSet.getHighestIntensity(searchMZRange);
    }

    IsotopePattern normalizedPattern =
        IsotopePatternCalculator.normalizeIsotopePattern(newPattern, normalizationFactor);
    Color newColor;
    if (newPattern.getStatus() == IsotopePatternStatus.DETECTED)
      newColor = detectedIsotopesColor;
    else
      newColor = predictedIsotopesColor;
    IsotopesDataSet newDataSet = new IsotopesDataSet(normalizedPattern);
    spectrumPlot.addDataSet(newDataSet, newColor, true);

  }

  public void loadSpectrum(IsotopePattern newPattern) {
    Color newColor = newPattern.getStatus() == IsotopePatternStatus.DETECTED ? detectedIsotopesColor
        : predictedIsotopesColor;
    IsotopesDataSet newDataSet = new IsotopesDataSet(newPattern);
    spectrumPlot.addDataSet(newDataSet, newColor, true);
  }

  public void setAxesRange(double xMin, double xMax, double xTickSize, double yMin, double yMax,
      double yTickSize) {
    NumberAxis xAxis = (NumberAxis) spectrumPlot.getXYPlot().getDomainAxis();
    NumberAxis yAxis = (NumberAxis) spectrumPlot.getXYPlot().getRangeAxis();
    xAxis.setRange(xMin, xMax);
    xAxis.setTickUnit(new NumberTickUnit(xTickSize));
    yAxis.setRange(yMin, yMax);
    yAxis.setTickUnit(new NumberTickUnit(yTickSize));
  }

  public void loadPreviousScan() {

    if (dataFile == null)
      return;

    int msLevel = currentScan.getMSLevel();
    int scanNumbers[] = dataFile.getScanNumbers(msLevel);
    int scanIndex = Arrays.binarySearch(scanNumbers, currentScan.getScanNumber());
    if (scanIndex > 0) {
      final int prevScanIndex = scanNumbers[scanIndex - 1];

      Runnable newThreadRunnable = new Runnable() {

        @Override
        public void run() {
          loadRawData(dataFile.getScan(prevScanIndex));
        }

      };

      Thread newThread = new Thread(newThreadRunnable);
      newThread.start();

    }
  }


  public void loadNextScan() {

    if (dataFile == null)
      return;

    int msLevel = currentScan.getMSLevel();
    int scanNumbers[] = dataFile.getScanNumbers(msLevel);
    int scanIndex = Arrays.binarySearch(scanNumbers, currentScan.getScanNumber());

    if (scanIndex < (scanNumbers.length - 1)) {
      final int nextScanIndex = scanNumbers[scanIndex + 1];

      Runnable newThreadRunnable = new Runnable() {

        @Override
        public void run() {
          loadRawData(dataFile.getScan(nextScanIndex));
        }

      };

      Thread newThread = new Thread(newThreadRunnable);
      newThread.start();

    }
  }


  void oldActionCommands(String command) {
    if (command.equals("ADD_ISOTOPE_PATTERN")) {

      IsotopePattern newPattern = IsotopePatternCalculator.showIsotopePredictionDialog(null, true);

      if (newPattern == null)
        return;

      loadIsotopes(newPattern);

    }

    if ((command.equals("ZOOM_IN")) || (command.equals("ZOOM_IN_BOTH_COMMAND"))) {
      spectrumPlot.getXYPlot().getDomainAxis().resizeRange(1 / zoomCoefficient);
    }

    if ((command.equals("ZOOM_OUT")) || (command.equals("ZOOM_OUT_BOTH_COMMAND"))) {
      spectrumPlot.getXYPlot().getDomainAxis().resizeRange(zoomCoefficient);
    }

    if (command.equals("SET_SAME_RANGE")) {

      // Get current axes range
      NumberAxis xAxis = (NumberAxis) spectrumPlot.getXYPlot().getDomainAxis();
      NumberAxis yAxis = (NumberAxis) spectrumPlot.getXYPlot().getRangeAxis();
      double xMin = xAxis.getRange().getLowerBound();
      double xMax = xAxis.getRange().getUpperBound();
      double xTick = xAxis.getTickUnit().getSize();
      double yMin = yAxis.getRange().getLowerBound();
      double yMax = yAxis.getRange().getUpperBound();
      double yTick = yAxis.getTickUnit().getSize();

      for (Window frame : Stage.getWindows()) {
        if (!(frame instanceof SpectraVisualizerWindow))
          continue;
        SpectraVisualizerWindow spectraFrame = (SpectraVisualizerWindow) frame;
        spectraFrame.setAxesRange(xMin, xMax, xTick, yMin, yMax, yTick);
      }

    }


  }

  public void enableProcessing() {
    DataPointProcessingManager inst = DataPointProcessingManager.getInst();
    inst.setEnabled(!inst.isEnabled());
    bottomPanel.updateProcessingButton();
    getSpectrumPlot().checkAndRunController();

    // if the tick is removed, set the data back to default
    if (!inst.isEnabled()) {
      getSpectrumPlot().removeDataPointProcessingResultDataSets();
      // loadRawData(currentScan);
    }
  }

  public void setProcessingParams() {
    if (!dppmWindowOpen) {
      dppmWindowOpen = true;

      ExitCode exitCode =
          DataPointProcessingManager.getInst().getParameters().showSetupDialog(true);

      dppmWindowOpen = false;
      if (exitCode == ExitCode.OK && DataPointProcessingManager.getInst().isEnabled()) {
        // if processing was run before, this removes the
        // previous results.
        getSpectrumPlot().removeDataPointProcessingResultDataSets();
        getSpectrumPlot().checkAndRunController();
      }
    }
  }

  public void addAnnotation(Map<DataPoint, String> annotation) {
    spectrumDataSet.addAnnotation(annotation);
  }

  public SpectraPlot getSpectrumPlot() {
    return spectrumPlot;
  }

  public ToolBar getToolBar() {
    return toolBar;
  }

  public void addDataSet(XYDataset dataset, Color color) {
    spectrumPlot.addDataSet(dataset, color, true);
  }

}
