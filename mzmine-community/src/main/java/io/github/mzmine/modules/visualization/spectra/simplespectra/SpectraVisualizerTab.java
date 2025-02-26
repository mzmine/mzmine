/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.spectra.simplespectra;

import com.google.common.base.Strings;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.impl.MultiChargeStateIsotopePattern;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.export_scans.ExportScansModule;
import io.github.mzmine.modules.io.spectraldbsubmit.view.MSMSLibrarySubmissionWindow;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.IsotopesDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.MassListDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.PeakListDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.ScanDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.SinglePeakDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.customdatabase.CustomDBSpectraSearchModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.spectraldatabase.SingleSpectrumLibrarySearchModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.sumformula.SumFormulaSpectraSearchModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.color.ColorUtils;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.dialogs.AxesSetupDialog;
import io.github.mzmine.util.scans.ScanUtils;
import java.awt.Color;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Orientation;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.data.xy.XYDataset;

/**
 * Spectrum visualizer using JFreeChart library
 */
public class SpectraVisualizerTab extends MZmineTab {

  private static final Logger logger = Logger.getLogger(SpectraVisualizerTab.class.getName());

  private static final Image centroidIcon = FxIconUtil.loadImageFromResources(
      "icons/centroidicon.png");
  private static final Image continuousIcon = FxIconUtil.loadImageFromResources(
      "icons/continuousicon.png");
  private static final Image dataPointsIcon = FxIconUtil.loadImageFromResources(
      "icons/datapointsicon.png");
  private static final Image annotationsIcon = FxIconUtil.loadImageFromResources(
      "icons/annotationsicon.png");
  private static final Image pickedPeakIcon = FxIconUtil.loadImageFromResources(
      "icons/pickedpeakicon.png");
  private static final Image isotopePeakIcon = FxIconUtil.loadImageFromResources(
      "icons/isotopepeakicon.png");
  private static final Image axesIcon = FxIconUtil.loadImageFromResources("icons/axesicon.png");
  private static final Image exportIcon = FxIconUtil.loadImageFromResources("icons/exporticon.png");
  private static final Image dbCustomIcon = FxIconUtil.loadImageFromResources(
      "icons/DBCustomIcon.png");
  private static final Image dbLipidsIcon = FxIconUtil.loadImageFromResources(
      "icons/DBLipidsIcon.png");
  private static final Image dbSpectraIcon = FxIconUtil.loadImageFromResources(
      "icons/DBSpectraIcon.png");
  private static final Image sumFormulaIcon = FxIconUtil.loadImageFromResources("icons/search.png");
  // initialize colors to some default before the color palette is loaded
  public static Color scanColor = new Color(0, 0, 192);
  public static Color massListColor = Color.orange;
  public static Color peaksColor = Color.red;
  public static Color singlePeakColor = Color.magenta;
  public static Color detectedIsotopesColor = Color.magenta;
  public static Color predictedIsotopesColor = Color.green;
  // private final Scene mainScene;
  private final BorderPane mainPane;
  private final ToolBar toolBar;
  private final Button centroidContinuousButton;
  private final Button dataPointsButton;
  private final SpectraPlot spectrumPlot;
  private final SpectraBottomPanel bottomPanel;
  private final ObjectProperty<MZTolerance> mzToleranceProperty = new SimpleObjectProperty<>();
  private RawDataFile dataFile;
  // Currently loaded scan
  private Scan currentScan;
  // Current scan data set
  private ScanDataSet spectrumDataSet;
  private MassListDataSet massListDataSet;
  private Color dataFileColor;

  private SimpleColorPalette palette = MZmineCore.getConfiguration().getDefaultColorPalette();


  public SpectraVisualizerTab(RawDataFile dataFile, Scan scanNumber, boolean enableProcessing,
      boolean hideSettingsPanel) {
    super("Spectra visualizer", true, false);
    // setTitle("Spectrum loading...");
    this.dataFile = dataFile;
    this.currentScan = scanNumber;
    loadColorSettings();

    mainPane = new BorderPane();
    // mainScene = new Scene(mainPane);
    // setScene(mainScene);

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
      if (spectrumPlot.getPlotMode() == SpectrumPlotType.CENTROID) {
        spectrumPlot.setPlotMode(SpectrumPlotType.PROFILE);
        centroidContinuousButton.setGraphic(new ImageView(centroidIcon));
        dataPointsButton.setDisable(false);
      } else {
        spectrumPlot.setPlotMode(SpectrumPlotType.CENTROID);
        centroidContinuousButton.setGraphic(new ImageView(continuousIcon));
        dataPointsButton.setDisable(true);
      }
    });

    Button annotationsButton = new Button(null, new ImageView(annotationsIcon));
    annotationsButton.setTooltip(new Tooltip("Toggle displaying of peak values"));
    annotationsButton.setOnAction(e -> spectrumPlot.switchItemLabelsVisible());

    Button pickedPeakButton = new Button(null, new ImageView(pickedPeakIcon));
    pickedPeakButton.setTooltip(new Tooltip("Toggle displaying of picked peaks"));
    pickedPeakButton.setOnAction(e -> spectrumPlot.switchPickedPeaksVisible());

    Button isotopePeakButton = new Button(null, new ImageView(isotopePeakIcon));
    isotopePeakButton.setTooltip(new Tooltip("Toggle displaying of predicted isotope peaks"));
    isotopePeakButton.setOnAction(e -> spectrumPlot.switchIsotopePeaksVisible());

    Button axesButton = new Button(null, new ImageView(axesIcon));
    axesButton.setTooltip(new Tooltip("Setup ranges for axes"));
    axesButton.setOnAction(e -> {
      AxesSetupDialog dialog = new AxesSetupDialog(MZmineCore.getDesktop().getMainWindow(),
          spectrumPlot.getXYPlot());
      dialog.show();
    });

    Button exportButton = new Button(null, new ImageView(exportIcon));
    exportButton.setTooltip(new Tooltip("Export spectra to spectra file"));
    exportButton.setOnAction(e -> ExportScansModule.showSetupDialog(currentScan));

    Button createLibraryEntryButton = new Button(null, new ImageView(exportIcon));
    createLibraryEntryButton.setTooltip(new Tooltip("Create spectral library entry"));
    createLibraryEntryButton.setOnAction(e -> {
      // open window with all selected rows
      MSMSLibrarySubmissionWindow libraryWindow = new MSMSLibrarySubmissionWindow();
      libraryWindow.setData(currentScan);
      libraryWindow.show();
    });

    Button dbCustomButton = new Button(null, new ImageView(dbCustomIcon));
    dbCustomButton.setTooltip(new Tooltip("Select custom database for annotation"));
    dbCustomButton.setOnAction(
        e -> CustomDBSpectraSearchModule.showSpectraIdentificationDialog(currentScan, spectrumPlot,
            Instant.now()));

    Button dbSpectraButton = new Button(null, new ImageView(dbSpectraIcon));
    dbSpectraButton.setTooltip(new Tooltip("Compare spectrum with spectral libraries"));
    dbSpectraButton.setOnAction(
        e -> SingleSpectrumLibrarySearchModule.showSpectraIdentificationDialog(currentScan,
            spectrumPlot, Instant.now()));

    Button sumFormulaButton = new Button(null, new ImageView(sumFormulaIcon));
    sumFormulaButton.setTooltip(new Tooltip("Predict sum formulas for annotation"));
    sumFormulaButton.setOnAction(
        e -> SumFormulaSpectraSearchModule.showSpectraIdentificationDialog(currentScan,
            spectrumPlot, Instant.now()));

    toolBar.getItems()
        .addAll(centroidContinuousButton, dataPointsButton, annotationsButton, pickedPeakButton,
            isotopePeakButton, axesButton, exportButton, createLibraryEntryButton, dbCustomButton,
            dbSpectraButton, sumFormulaButton);

    mainPane.setRight(toolBar);
    bottomPanel = new SpectraBottomPanel(this, dataFile);
    if (!hideSettingsPanel) {
      Accordion bottomAccordion = new Accordion(new TitledPane("Spectrum options", bottomPanel));
      mainPane.setBottom(bottomAccordion);
    }
    setContent(mainPane);

    // get parameters
    ParameterSet specParams = MZmineCore.getConfiguration()
        .getModuleParameters(SpectraVisualizerModule.class);

    MZTolerance mzTolerance = specParams.getValue(SpectraVisualizerParameters.mzTolerance);
    setMzTolerance(mzTolerance);
    mzToleranceProperty.bindBidirectional(spectrumPlot.mzToleranceProperty());

  }

  public SpectraVisualizerTab(RawDataFile dataFile, Scan scanNumber, boolean enableProcessing) {
    this(dataFile, scanNumber, enableProcessing, false);
  }

  public SpectraVisualizerTab(RawDataFile dataFile) {
    this(dataFile, null, false);
  }

  public MZTolerance getMzTolerance() {
    return mzToleranceProperty.get();
  }

  public void setMzTolerance(MZTolerance mzTolerance) {
    this.mzToleranceProperty.set(mzTolerance);
  }

  public ObjectProperty<MZTolerance> mzToleranceProperty() {
    return mzToleranceProperty;
  }

  public BorderPane getMainPane() {
    return mainPane;
  }

  private void loadColorSettings() {
    SimpleColorPalette palette = MZmineCore.getConfiguration().getDefaultColorPalette();
    dataFileColor = dataFile != null ? dataFile.getColorAWT() : palette.getNextColorAWT();
    scanColor = dataFileColor;
    massListColor = FxColorUtil.fxColorToAWT(
        ColorUtils.getContrastPaletteColor(FxColorUtil.awtColorToFX(dataFileColor), palette));
    peaksColor = getNextFileContrastColor();
    singlePeakColor = getNextFileContrastColor();
    detectedIsotopesColor = getNextFileContrastColor();
    predictedIsotopesColor = getNextFileContrastColor();
  }

  public void loadRawData(Scan scan) {
    if (scan == null) {
      clearPlot();
      logger.finest("Clearing spectra plot as scan was null");
      return;
    }
    logger.finest(
        "Loading scan #" + scan.getScanNumber() + " from " + dataFile + " for spectra visualizer");

    spectrumDataSet = new ScanDataSet(scan);
    MassList massList = scan.getMassList();
    if (massList != null) {
      massListDataSet = new MassListDataSet(massList);
    }

    this.currentScan = scan;

    spectrumPlot.applyWithNotifyChanges(false, true, () -> {

      // If the plot mode has not been set yet, set it accordingly
      if (spectrumPlot.getPlotMode() == null) {
        spectrumPlot.setPlotMode(SpectrumPlotType.fromScan(currentScan));
        if (currentScan.getSpectrumType() == MassSpectrumType.CENTROIDED) {
          spectrumPlot.setPlotMode(SpectrumPlotType.CENTROID);
          centroidContinuousButton.setGraphic(new ImageView(continuousIcon));
        } else {
          spectrumPlot.setPlotMode(SpectrumPlotType.PROFILE);
          centroidContinuousButton.setGraphic(new ImageView(centroidIcon));
        }
      }

      // Clean up the MS/MS selector combo

      final ComboBox<Scan> msmsSelector = bottomPanel.getMSMSSelector();

      // We disable the MSMS selector first and then enable it again later
      // after updating the items. If we skip this, the size of the
      // selector may not be adjusted properly (timing issues?)
      msmsSelector.setDisable(true);

      msmsSelector.getItems().clear();
      boolean msmsVisible = false;

      // Add parent scan to MS/MS selector combo
      NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
      NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();

      // TODO: Search fragment scans
      // Add all fragment scans to MS/MS selector combo
      Scan[] fragmentScans = null; // currentScan.getFragmentScanNumbers();
      if (fragmentScans != null) {
        for (Scan fragmentScan : fragmentScans) {
          if (fragmentScan == null) {
            continue;
          }
          msmsSelector.getItems().add(fragmentScan);
          msmsVisible = true;
        }
      }

      msmsSelector.setDisable(false);

      // Update the visibility of MS/MS selection combo
      bottomPanel.setMSMSSelectorVisible(msmsVisible);

      // Set window and plot titles
      String spectrumTitle = ScanUtils.scanToString(currentScan, true);

      Integer basePeak = scan.getBasePeakIndex();

      if (basePeak != null) {
        spectrumTitle += ", base peak: " + mzFormat.format(scan.getBasePeakMz()) + " m/z ("
            + intensityFormat.format(scan.getBasePeakIntensity()) + ")";
      }
      String spectrumSubtitle = null;
      if (!Strings.isNullOrEmpty(currentScan.getScanDefinition())) {
        spectrumSubtitle = "Scan definition: " + currentScan.getScanDefinition();
      }

      final String finalSpectrumTitle = spectrumTitle;
      final String finalSpectrumSubtitle = spectrumSubtitle;

      // Platform.runLater(() -> { // this should be the fx thread, otherwise loading isotopes will
      // fail
      // setTitle(windowTitle);
      spectrumPlot.setTitle(finalSpectrumTitle, finalSpectrumSubtitle);

      // Set plot data set
      spectrumPlot.removeAllDataSets();
      spectrumPlot.getXYPlot().clearDomainMarkers();
      spectrumPlot.addDataSet(spectrumDataSet, scanColor, false, false);
      spectrumPlot.addDataSet(massListDataSet, massListColor, false, false);
      spectrumPlot.getXYPlot().getRenderer().setDefaultPaint(dataFileColor);
      // });

      if (scan.getMSLevel() > 1 && scan.getPrecursorMz() != null) {
        // add all precursors
        final Double prmz = scan.getPrecursorMz();
        if (prmz != null) {
          spectrumPlot.getXYPlot().addDomainMarker(new ValueMarker(prmz));
        }
      }
    });
  }

  private void clearPlot() {
    spectrumPlot.setTitle("", "");

    // Set plot data set
    spectrumPlot.removeAllDataSets();
    spectrumPlot.getXYPlot().clearDomainMarkers();
  }

  public void loadPeaks(FeatureList selectedPeakList) {
    // avoid multiple chart updates
    spectrumPlot.applyWithNotifyChanges(false, () -> {
      spectrumPlot.removePeakListDataSets();

      if (selectedPeakList == null) {
        return;
      }

      logger.finest(
          "Loading a feature list " + selectedPeakList + " to a spectrum window"/* + getTitle() */);

      PeakListDataSet peaksDataSet = new PeakListDataSet(dataFile, currentScan, selectedPeakList);

      // Set plot data sets
      spectrumPlot.addDataSet(peaksDataSet, peaksColor, true, false);
    });
  }

  public void loadSinglePeak(Feature peak) {

    SinglePeakDataSet peakDataSet = new SinglePeakDataSet(currentScan, peak);

    // Set plot data sets
    spectrumPlot.addDataSet(peakDataSet, singlePeakColor, true, true);

  }

  public void loadIsotopes(IsotopePattern newPattern) {
    spectrumPlot.applyWithNotifyChanges(false, () -> {

      if (newPattern instanceof MultiChargeStateIsotopePattern multi) {

        List<IsotopePattern> patterns = multi.getPatterns();
        for (int i = 0; i < patterns.size(); i++) {
          Color newColor;
          if (newPattern.getStatus() == IsotopePatternStatus.DETECTED) {
            newColor = detectedIsotopesColor;
          } else {
            newColor = predictedIsotopesColor;
          }

          IsotopePattern pattern = patterns.get(i);
          final IsotopePattern normalizedPattern = normalizeIsotopePattern(pattern);
          if (normalizedPattern == null) {
            return;
          }

          final IsotopesDataSet newDataSet = new IsotopesDataSet(normalizedPattern,
              (i == 0 ? "Isotopes (%d, preferred)" : "Isotopes (%d)").formatted(
                  pattern.getNumberOfDataPoints()));
          spectrumPlot.addDataSet(newDataSet, newColor, true, false);
        }
      } else {
        Color newColor;
        if (newPattern.getStatus() == IsotopePatternStatus.DETECTED) {
          newColor = detectedIsotopesColor;
        } else {
          newColor = predictedIsotopesColor;
        }
        final IsotopePattern normalizedPattern = normalizeIsotopePattern(newPattern);
        if (normalizedPattern == null) {
          return;
        }

        IsotopesDataSet newDataSet = new IsotopesDataSet(normalizedPattern);
        spectrumPlot.addDataSet(newDataSet, newColor, true, false);
      }
    });
  }

  private Color getNextFileContrastColor() {
    if (currentScan != null) {
      return palette.getNextColorAWT(
          FxColorUtil.fxColorToAWT(currentScan.getDataFile().getColor()));
    } else if (dataFile != null) {
      return palette.getNextColorAWT(FxColorUtil.fxColorToAWT(dataFile.getColor()));
    } else {
      return palette.getNextColorAWT();
    }
  }

  @Nullable
  private IsotopePattern normalizeIsotopePattern(IsotopePattern newPattern) {
    // We need to find a normalization factor for the new isotope
    // pattern, to show meaningful intensity range
    Integer basePeak = newPattern.getBasePeakIndex();
    if (basePeak == null) {
      return null;
    }
    double mz = newPattern.getBasePeakMz();

    Range<Double> searchMZRange = Range.closed(mz - 0.5, mz + 0.5);
    ScanDataSet scanDataSet = spectrumPlot.getMainScanDataSet();
    double normalizationFactor = scanDataSet.getHighestIntensity(searchMZRange);

    // If normalization factor is 0, it means there were no data points
    // in given m/z range. In such case we use the max intensity of
    // whole scan as normalization factor.
    if (normalizationFactor == 0) {
      normalizationFactor = Objects.requireNonNullElse(scanDataSet.getScan().getBasePeakIntensity(),
          1d);
    }

    IsotopePattern normalizedPattern = IsotopePatternCalculator.normalizeIsotopePattern(newPattern,
        normalizationFactor);
    return normalizedPattern;
  }

  public void loadSpectrum(IsotopePattern newPattern) {
    Color newColor = newPattern.getStatus() == IsotopePatternStatus.DETECTED ? detectedIsotopesColor
        : predictedIsotopesColor;
    IsotopesDataSet newDataSet = new IsotopesDataSet(newPattern);
    spectrumPlot.addDataSet(newDataSet, newColor, true, true);
  }

  public void loadPreviousScan() {

    if (dataFile == null) {
      return;
    }

    int msLevel = currentScan.getMSLevel();
    List<Scan> scans = dataFile.getScanNumbers(msLevel);
    int scanIndex = scans.indexOf(currentScan);
    if (scanIndex > 0) {
      final Scan prevScan = scans.get(scanIndex - 1);
      Thread newThread = new Thread(() -> loadRawData(prevScan));
      newThread.start();
    }
  }


  public void loadNextScan() {

    if (dataFile == null) {
      return;
    }

    int msLevel = currentScan.getMSLevel();
    List<Scan> scanNumbers = dataFile.getScanNumbers(msLevel);
    int scanIndex = scanNumbers.indexOf(currentScan);

    if (scanIndex < (scanNumbers.size() - 1)) {
      final Scan nextScan = scanNumbers.get(scanIndex + 1);

      Thread newThread = new Thread(() -> loadRawData(nextScan));
      newThread.start();
    }
  }


  public void addAnnotation(Map<Integer, String> annotation) {
    spectrumDataSet.addAnnotation(annotation);
  }

  /**
   * Add annotations for m/z values
   *
   * @param annotation m/z value and annotation map
   */
  public void addMzAnnotation(Map<DataPoint, String> annotation) {
    spectrumDataSet.addMzAnnotation(annotation);
  }

  public SpectraPlot getSpectrumPlot() {
    return spectrumPlot;
  }

  public ToolBar getToolBar() {
    return toolBar;
  }

  public void addDataSet(XYDataset dataset, Color color, boolean notifyChange) {
    spectrumPlot.addDataSet(dataset, color, true, notifyChange);
  }

  @NotNull
  @Override
  public Collection<? extends RawDataFile> getRawDataFiles() {
    return new ArrayList<>(Collections.singletonList(dataFile));
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getFeatureLists() {
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getAlignedFeatureLists() {
    return Collections.emptyList();
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {
    if (rawDataFiles == null || rawDataFiles.isEmpty()) {
      return;
    }

    // get first raw data file
    RawDataFile newFile = rawDataFiles.iterator().next();
    if (dataFile.equals(newFile)) {
      return;
    }

    // add new scan
    Scan newScan = newFile.binarySearchClosestScan(currentScan.getRetentionTime());
    if (newScan == null) {
      MZmineCore.getDesktop().displayErrorMessage(
          "Raw data file " + dataFile + " does not contain scan at retention time "
              + currentScan.getRetentionTime());
      return;
    }

    dataFileColor = newFile.getColorAWT();

    loadRawData(newScan);

    dataFile = newFile;
    currentScan = newScan;
  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }

  @Override
  public void onAlignedFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }
}
