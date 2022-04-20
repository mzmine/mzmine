/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.visualization.chromatogramandspectra;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.chromatogram.ChromatogramCursorPosition;
import io.github.mzmine.modules.visualization.chromatogram.FeatureDataSet;
import io.github.mzmine.modules.visualization.chromatogram.TICDataSet;
import io.github.mzmine.modules.visualization.chromatogram.TICPlot;
import io.github.mzmine.modules.visualization.chromatogram.TICPlotType;
import io.github.mzmine.modules.visualization.rawdataoverview.RawDataOverviewWindowController;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectrumCursorPosition;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectrumPlotType;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.ScanDataSet;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExitCode;
import java.awt.BasicStroke;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import javafx.animation.PauseTransition;
import javafx.beans.NamedArg;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.plot.ValueMarker;

/**
 * This visualizer can be used to visualize chromatograms and spectra of multiple raw data files at
 * the same time. The selection within the two plots ({@link ChromatogramAndSpectraVisualizer#spectrumPosition}
 * and {@link ChromatogramAndSpectraVisualizer#chromPosition}) are bound to the plots. If the
 * selection changes, the plots will change accordingly. A click in the chromatogram will update the
 * spectra, whereas a click in a spectrum will show the xic of the selected m/z.
 *
 * @author SteffenHeu https://github.com/SteffenHeu / steffen.heuckeroth@uni-muenster.de
 */
public class ChromatogramAndSpectraVisualizer extends SplitPane {

  public static final Logger logger = Logger.getLogger(
      ChromatogramAndSpectraVisualizer.class.getName());
  private static final BasicStroke MARKER_STROKE = new BasicStroke(2.0f);
  protected final TICPlot chromPlot;
  protected final SpectraPlot spectrumPlot;
  protected final ObjectProperty<ScanSelection> scanSelection;
  /**
   * Type of chromatogram to be displayed. This is bound bidirectional to the {@link
   * TICPlot#plotTypeProperty()} and gets updated if {@link ChromatogramAndSpectraVisualizerParameters#plotType}
   * changes.
   */
  protected final ObjectProperty<TICPlotType> plotType;
  /**
   * Current position of the crosshair in the chromatogram plot. Changes to the position should be
   * reflected in the {@link ChromatogramAndSpectraVisualizer#spectrumPlot}.
   */
  protected final ObjectProperty<ChromatogramCursorPosition> chromPosition;
  /**
   * Current position of the crosshair in the spectrum plot. Changes in the position update the
   * {@link ChromatogramAndSpectraVisualizer#chromPlot} via {@link ChromatogramAndSpectraVisualizer#onSpectrumSelectionChanged(ObservableValue,
   * SpectrumCursorPosition, SpectrumCursorPosition)}.
   */
  protected final ObjectProperty<SpectrumCursorPosition> spectrumPosition;
  /**
   * Tolerance range for the feature chromatograms of the base peak in the selected scan. Listener
   * calls {@link ChromatogramAndSpectraVisualizer#updateFeatureDataSets(double)}.
   */
  protected final ObjectProperty<MZTolerance> chromMzTolerance;
  /**
   * Tolerance for the generation of the TICDataset. If set to null, the whole m/z range is
   * displayed.
   */
  protected final ObjectProperty<Range<Double>> mzRange;
  protected final PauseTransition chromDelay = new PauseTransition(Duration.millis(200));
  protected final PauseTransition spectraDelay = new PauseTransition(Duration.millis(200));
  private final NumberFormat mzFormat;
  private final NumberFormat rtFormat;
  protected FlowPane pnSpectrumControls;
  protected ChromatogramPlotControlPane pnChromControls;
  protected BooleanProperty showMassListProperty;
  protected ValueMarker rtMarker;
  protected ValueMarker mzMarker;
  protected ParameterSet parameterSet;
  protected boolean showSpectraOfEveryRawFile;
  /**
   * Stores the raw data files ands tic data sets currently displayed. Could be observed by a
   * listener in the future, if needed.
   */
  protected ObservableMap<RawDataFile, TICDataSet> filesAndDataSets;
  protected SpectraDataSetCalc currentSpectraDataSetCalc;
  protected FeatureDataSetCalc currentFeatureDataSetCalc;

  public ChromatogramAndSpectraVisualizer() {
    this(Orientation.HORIZONTAL);
  }

  public ChromatogramAndSpectraVisualizer(@NamedArg("orientation") Orientation orientation) {
    super();

    getStyleClass().add("region-match-chart-bg");

    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    rtFormat = MZmineCore.getConfiguration().getRTFormat();

    filesAndDataSets = FXCollections.synchronizedObservableMap(
        FXCollections.observableMap(new Hashtable<RawDataFile, TICDataSet>()));

    setOrientation(orientation);
    showSpectraOfEveryRawFile = true;

    // initialise properties
    plotType = new SimpleObjectProperty<>();
    chromMzTolerance = new SimpleObjectProperty<>(new MZTolerance(0, 10));
    chromPosition = new SimpleObjectProperty<>();
    spectrumPosition = new SimpleObjectProperty<>();
    scanSelection = new SimpleObjectProperty<>(
        new ScanSelection(null, null, null, null, null, null, 1, null));
    mzRange = new SimpleObjectProperty<>();

    // initialise controls
    pnChromControls = new ChromatogramPlotControlPane();
    pnSpectrumControls = new FlowPane();
    chromPlot = new TICPlot();
    spectrumPlot = new SpectraPlot();
    BorderPane pnWrapSpectrum = new BorderPane();
    BorderPane pnWrapChrom = new BorderPane();
    pnWrapChrom.setCenter(chromPlot);
    pnWrapChrom.setBottom(pnChromControls);

    pnWrapSpectrum.setCenter(spectrumPlot);
    pnWrapSpectrum.setBottom(pnSpectrumControls);
    getItems().addAll(pnWrapChrom, pnWrapSpectrum);

    // chrom plot top bar
    Button btnChromSetup = new Button("...");
    btnChromSetup.setOnAction(e -> {
      if (parameterSet == null) {
        parameterSet = MZmineCore.getConfiguration()
            .getModuleParameters(ChromatogramAndSpectraVisualizerModule.class);
      }
      ExitCode code = parameterSet.showSetupDialog(true);
      if (code == ExitCode.OK) {
        MZTolerance tol = parameterSet.getParameter(
            ChromatogramAndSpectraVisualizerParameters.chromMzTolerance).getValue();
        ScanSelection sel = parameterSet.getParameter(
            ChromatogramAndSpectraVisualizerParameters.scanSelection).getValue();
        if (sel != null) {
          scanSelection.set(sel);
        }
        if (tol != null) {
          chromMzTolerance.set(tol);
        }
        TICPlotType pt = parameterSet.getParameter(
            ChromatogramAndSpectraVisualizerParameters.plotType).getValue();
        if (pt != null) {
          plotType.set(pt);
        }
      }
    });
    Label labelChromatogram = new Label("Chromatrogram view");
    labelChromatogram.setStyle("-fx-font-weight: bold");
    FlowPane pnChromHeader = new FlowPane(labelChromatogram);
    pnChromHeader.setAlignment(Pos.CENTER_LEFT);
    StackPane pnChromStack = new StackPane();
    pnChromStack.setAlignment(Pos.TOP_RIGHT);
    pnChromStack.setPadding(new Insets(5));
    pnChromStack.getChildren().addAll(pnChromHeader, btnChromSetup);
    pnWrapChrom.setTop(pnChromStack);

    // spectrum plot top bar
    ChoiceBox<SpectrumPlotType> cbSpectrumType = new ChoiceBox<>(
        FXCollections.observableArrayList(SpectrumPlotType.values()));
    cbSpectrumType.valueProperty().bindBidirectional(spectrumPlot.plotModeProperty());
    cbSpectrumType.setMinSize(ChoiceBox.USE_PREF_SIZE, ChoiceBox.USE_PREF_SIZE);
    HBox hBoxSpectrum = new HBox();
    hBoxSpectrum.setAlignment(Pos.TOP_RIGHT);
    hBoxSpectrum.setPadding(new Insets(5));
    Label labelSpectrum = new Label("Spectrum view");
    labelSpectrum.setStyle("-fx-font-weight: bold");
    FlowPane pnSpectrumHeader = new FlowPane(labelSpectrum);
    pnSpectrumHeader.setPadding(new Insets(5));
    CheckBox checkBoxShowMassList = new CheckBox("Show mass list");
    checkBoxShowMassList.setMinSize(ChoiceBox.USE_PREF_SIZE, CheckBox.USE_PREF_SIZE);
    HBox hBoxSpectrumSetup = new HBox();
    hBoxSpectrumSetup.setSpacing(10);
    hBoxSpectrumSetup.setAlignment(Pos.BASELINE_RIGHT);
    hBoxSpectrumSetup.getChildren().addAll(checkBoxShowMassList, cbSpectrumType);
    hBoxSpectrum.getChildren().addAll(pnSpectrumHeader, hBoxSpectrumSetup);
    pnWrapSpectrum.setTop(hBoxSpectrum);

    showMassListProperty = checkBoxShowMassList.selectedProperty();
    showMassListProperty.addListener((observable, oldValue, newValue) -> {
      if (filesAndDataSets != null && chromPosition.getValue() != null) {
        updateSpectraPlot(filesAndDataSets.keySet(), chromPosition.getValue());
      }
    });

    chromPlot.setLabelColorMatch(true);
    spectrumPlot.setLabelColorMatch(true);

    // property bindings
    plotType.bindBidirectional(chromPlot.plotTypeProperty());
    chromPosition.bindBidirectional(chromPlot.cursorPositionProperty());
    spectrumPosition.bindBidirectional(spectrumPlot.cursorPositionProperty());
    mzRangeProperty().addListener((obs, old, val) -> pnChromControls.setMzRange(val));

    // property listeners
    plotType.addListener(((observable, oldValue, newValue) -> {
      chromPlot.removeAllDataSets(false);
      updateAllChromatogramDataSets();
    }));

    // update feature data sets if the tolerance for the extraction changes
    chromMzToleranceProperty().addListener((obs, old, val) -> {
      if (getChromPosition() != null) {
        updateFeatureDataSets(getChromPosition().getScan().getBasePeakMz());
      }
    });

    // update spectrum plot if the user clicks in chromatogram plot
    chromPositionProperty().addListener(
        (obs, old, pos) -> onChromatogramSelectionChanged(obs, old, pos));

    spectrumPositionProperty().addListener(
        ((obs, old, pos) -> onSpectrumSelectionChanged(obs, old, pos)));

    // update chromatogram plot if the ScanSelection changes
    scanSelectionProperty().addListener((obs, old, val) -> updateAllChromatogramDataSets());

    pnChromControls.getBtnUpdateXIC().setOnAction(event -> {
      mzRange.set(pnChromControls.getMzRange());
      updateAllChromatogramDataSets();
    });

    chromPlot.getXYPlot().setDomainCrosshairVisible(false);
    chromPlot.getXYPlot().setRangeCrosshairVisible(false);

    setOnMouseClicked(e -> requestFocus());
    setOnKeyPressed(e -> {
      if (e.getCode() == KeyCode.LEFT && e.isControlDown() && getChromPosition() != null) {
        logger.finest("Loading previous scan");
        Scan scan = getScan(getChromPosition().getDataFile(), getChromPosition().getScan(), -1);
        setFocusedScan(getChromPosition().getDataFile(), scan);
        requestFocus();
        e.consume();
      } else if (e.getCode() == KeyCode.RIGHT && e.isControlDown() && getChromPosition() != null) {
        logger.finest("Loading next scan");
        Scan scan = getScan(getChromPosition().getDataFile(), getChromPosition().getScan(), +1);
        setFocusedScan(getChromPosition().getDataFile(), scan);
        requestFocus();
        e.consume();
      }
    });
  }

  private Scan getScan(RawDataFile dataFile, Scan scan, int shift) {
    if (!Objects.equals(scan.getDataFile(), dataFile)) {
      throw new IllegalArgumentException("data file and the scan data file need to be the same");
    }
    ObservableList<Scan> scans = dataFile.getScans();
    int index = scans.indexOf(scan);
    if (index == -1) {
      return null;
    } else if (shift > 0) {
      return scans.get(Math.min(index + shift, scans.size() - 1));
    } else {
      return scans.get(Math.max(index + shift, 0));
    }
  }

  private void updateAllChromatogramDataSets() {
    List<RawDataFile> rawDataFiles = new ArrayList<>(filesAndDataSets.keySet());
    // update all datasets and force update at the end by setting the state to true
    chromPlot.applyWithNotifyChanges(false, true, () -> {
      chromPlot.getXYPlot().clearDomainMarkers();
      for (RawDataFile raw : rawDataFiles) {
        removeRawDataFile(raw);
        addRawDataFile(raw);
      }
    });
  }

  /**
   * @return The raw data files currently visualised.
   */
  @NotNull
  public Collection<RawDataFile> getRawDataFiles() {
    return filesAndDataSets.keySet();
  }

  /**
   * Sets the raw data files to be displayed. Already present files are not removed to optimise
   * performance. This should be called over {@link RawDataOverviewWindowController#addRawDataFileTab}
   * if possible.
   *
   * @param rawDataFiles
   */
  public void setRawDataFiles(@NotNull Collection<RawDataFile> rawDataFiles) {
    // disable update until all changes are applied, then set true and force update
    spectrumPlot.applyWithNotifyChanges(false, true, () -> {
      chromPlot.applyWithNotifyChanges(false, true, () -> {

        // remove files first
        for (RawDataFile rawDataFile : filesAndDataSets.keySet()) {
          if (!rawDataFiles.contains(rawDataFile)) {
            removeRawDataFile(rawDataFile);
          }
        }

        // presence of file is checked in the add method
        for (RawDataFile r : rawDataFiles) {
          if (!(r instanceof ImagingRawDataFile)) {
            addRawDataFile(r);
          }
        }
      });
    });
  }

  /**
   * Adds a raw data file to the chromatogram plot.
   *
   * @param rawDataFile
   */
  public void addRawDataFile(@NotNull final RawDataFile rawDataFile) {

    if (filesAndDataSets.keySet().contains(rawDataFile)) {
      logger.fine("Raw data file " + rawDataFile.getName() + " already displayed.");
      return;
    }

    final Scan[] scans = getScanSelection().getMatchingScans(rawDataFile);
    if (scans.length == 0) {
      logger.warning(
          "No matching scans found in file \"" + rawDataFile.getName() + "\" for scan selection.");
      return;
    }

    Range<Double> rawMZRange =
        (getMzRange() != null && pnChromControls.cbXIC.isSelected()) ? getMzRange()
            : rawDataFile.getDataMZRange();

    TICDataSet ticDataset = new TICDataSet(rawDataFile, List.of(scans), rawMZRange, null,
        getPlotType());
    filesAndDataSets.put(rawDataFile, ticDataset);
    chromPlot.addTICDataSet(ticDataset, rawDataFile.getColorAWT());

    logger.finest("Added raw data file " + rawDataFile.getName());
  }

  /**
   * Removes a raw data file and it's features from the chromatogram and spectrum plot.
   *
   * @param file The raw data file
   */
  public void removeRawDataFile(@NotNull final RawDataFile file) {
    logger.fine("Removing raw data file " + file.getName());
    TICDataSet dataset = filesAndDataSets.get(file);
    chromPlot.getXYPlot().setDataset(chromPlot.getXYPlot().indexOf(dataset), null);
    chromPlot.removeFeatureDataSetsOfFile(file);
    filesAndDataSets.remove(file);
  }

  /**
   * Called by a listener to the currentPostion to update the spectraPlot accordingly. The listener
   * is triggered by a change to the {@link ChromatogramAndSpectraVisualizer#chromPosition}
   * property.
   */
  private void onChromatogramSelectionChanged(
      ObservableValue<? extends ChromatogramCursorPosition> obs, ChromatogramCursorPosition old,
      ChromatogramCursorPosition pos) {
    updateChromatogramDomainMarker(pos);
    // update feature data sets
    Scan scan = pos.getScan();
    if (scan.getBasePeakMz() != null) {
      updateFeatureDataSets(scan.getBasePeakMz());
    }
    // update spectrum plots
    updateSpectraPlot(filesAndDataSets.keySet(), pos);
  }

  /**
   * Called by changes to {@link ChromatogramAndSpectraVisualizer#spectrumPosition}.
   *
   * @param obs
   * @param old
   * @param pos
   */
  private void onSpectrumSelectionChanged(ObservableValue<? extends SpectrumCursorPosition> obs,
      SpectrumCursorPosition old, SpectrumCursorPosition pos) {
    updateSpectrumDomainMarker(pos);
    mzRangeProperty().set(getChromMzTolerance().getToleranceRange(pos.getMz()));
    updateFeatureDataSets(pos.getMz());
  }

  /**
   * Changes the position of the domain marker. Is called by the mouse listener initialized in
   * {@link ChromatogramAndSpectraVisualizer#onChromatogramSelectionChanged(ObservableValue,
   * ChromatogramCursorPosition, ChromatogramCursorPosition)}
   *
   * @param pos
   */
  private void updateChromatogramDomainMarker(@NotNull ChromatogramCursorPosition pos) {
    chromPlot.applyWithNotifyChanges(false, () -> {

      chromPlot.getXYPlot().clearDomainMarkers();

      if (rtMarker == null) {
        rtMarker = new ValueMarker(pos.getScan().getRetentionTime());
        rtMarker.setStroke(MARKER_STROKE);
      } else {
        rtMarker.setValue(pos.getScan().getRetentionTime());
      }
      rtMarker.setPaint(
          MZmineCore.getConfiguration().getDefaultColorPalette().getNeutralColorAWT());

      chromPlot.getXYPlot().addDomainMarker(rtMarker);
    });
  }

  private void updateSpectrumDomainMarker(@NotNull SpectrumCursorPosition pos) {
    spectrumPlot.applyWithNotifyChanges(false, () -> {
      spectrumPlot.getXYPlot().clearDomainMarkers();

      if (mzMarker == null) {
        mzMarker = new ValueMarker(pos.getMz());
        mzMarker.setStroke(MARKER_STROKE);
      } else {
        mzMarker.setValue(pos.getMz());
      }
      mzMarker.setPaint(
          MZmineCore.getConfiguration().getDefaultColorPalette().getNeutralColorAWT());

      spectrumPlot.getXYPlot().addDomainMarker(mzMarker);
    });
  }

  /**
   * Sets a single scan into the spectrum plot. Triggers {@link ChromatogramAndSpectraVisualizer#chromPositionProperty()}'s
   * listeners.
   *
   * @param rawDataFile The rawDataFile to focus.
   * @param scanNum     The scan number.
   */
  public void setFocusedScan(@NotNull RawDataFile rawDataFile, Scan scanNum) {
    if (!filesAndDataSets.keySet().contains(rawDataFile) || scanNum == null) {
      return;
    }
    ChromatogramCursorPosition pos = new ChromatogramCursorPosition(scanNum.getRetentionTime(), 0,
        0, rawDataFile, scanNum);
    setChromPosition(pos);
  }

  /**
   * Forces a single scan in the spectrum plot without notifying the listener of the {@link
   * ChromatogramAndSpectraVisualizer#chromPosition}.
   *
   * @param rawDataFile The raw data file
   * @param scanNum     The number of the scan
   */
  private void forceScanDataSet(@NotNull RawDataFile rawDataFile, Scan scanNum) {
    spectrumPlot.applyWithNotifyChanges(false, () -> {
      spectrumPlot.removeAllDataSets();
      ScanDataSet dataSet = new ScanDataSet(scanNum);
      spectrumPlot.addDataSet(dataSet, rawDataFile.getColorAWT(), false, false);
      spectrumPlot.setTitle(rawDataFile.getName() + "(#" + scanNum + ")", "");
    });
  }

  /**
   * This can be called from the outside to focus a specific scan without triggering {@link
   * ChromatogramAndSpectraVisualizer#chromPositionProperty()}'s listeners. Use with care.
   *
   * @param rawDataFile The rawDataFile to focus.
   * @param scanNum     The scan number.
   */
  public void setFocusedScanSilent(@NotNull RawDataFile rawDataFile, Scan scanNum) {
    if (!filesAndDataSets.keySet().contains(rawDataFile) || scanNum == null) {
      return;
    }
    ChromatogramCursorPosition pos = new ChromatogramCursorPosition(scanNum.getRetentionTime(), 0,
        0, rawDataFile, scanNum);
    updateChromatogramDomainMarker(pos);
    forceScanDataSet(rawDataFile, scanNum);
  }

  public TICPlot getChromPlot() {
    return chromPlot;
  }


  public SpectraPlot getSpectrumPlot() {
    return spectrumPlot;
  }

  // ----- Plot updaters -----

  /**
   * Calculates {@link FeatureDataSet}s for the given m/z range. Called when {@link
   * ChromatogramAndSpectraVisualizer#chromPosition} or {@link ChromatogramAndSpectraVisualizer#spectrumPosition}
   * changes.
   *
   * @param mz
   */
  private void updateFeatureDataSets(final double mz) {
    chromDelay.setOnFinished((event) -> delayedFeatureDataUpdate(mz));
    chromDelay.playFromStart();
  }

  /**
   * Accumulate all updates for x ms
   *
   * @param mz
   */
  private void delayedFeatureDataUpdate(final double mz) {
    // mz of the base peak in the selected scan of the selected raw data file.
    Range<Double> bpcChromToleranceRange = getChromMzTolerance().getToleranceRange(mz);
    FeatureDataSetCalc thread = new FeatureDataSetCalc(filesAndDataSets.keySet(),
        bpcChromToleranceRange, getScanSelection(), getChromPlot());

    // put the current range into the mz range component. This is the reason it does not have a
    // listener that automatically updates the tic plot
    pnChromControls.setMzRange(bpcChromToleranceRange);

    thread.addTaskStatusListener((task, newStatus, oldStatus) -> {
      logger.finest("FeatureUpdate status changed from " + oldStatus.toString() + " to "
                    + newStatus.toString());
      currentFeatureDataSetCalc = null;
    });
    if (currentFeatureDataSetCalc != null) {
      currentFeatureDataSetCalc.setStatus(TaskStatus.CANCELED);
    }
    currentFeatureDataSetCalc = thread;
    MZmineCore.getTaskController().addTask(thread);
  }

  /**
   * Updates the {@link ChromatogramAndSpectraVisualizer#spectrumPlot} with the scan data sets of
   * the currently selected retention time in the {@link ChromatogramAndSpectraVisualizer#chromPlot}.
   *
   * @param rawDataFiles The raw data files in the chromatogram plot.
   * @param pos          the currently selected {@link ChromatogramCursorPosition}.
   */
  private void updateSpectraPlot(@NotNull Collection<RawDataFile> rawDataFiles,
      @NotNull ChromatogramCursorPosition pos) {
    spectraDelay.setOnFinished((event) -> delayedUpdateSpectraPlot(rawDataFiles, pos));
    spectraDelay.playFromStart();
  }

  /**
   * Accumulate update calls
   *
   * @param rawDataFiles
   * @param pos
   */
  private void delayedUpdateSpectraPlot(@NotNull Collection<RawDataFile> rawDataFiles,
      @NotNull ChromatogramCursorPosition pos) {
    SpectraDataSetCalc thread = new SpectraDataSetCalc(rawDataFiles, pos, getScanSelection(),
        showSpectraOfEveryRawFile, getSpectrumPlot(), showMassListProperty);

    thread.addTaskStatusListener((task, newStatus, oldStatus) -> {
      // logger
      // .finest("SpectraUpdate status changed from " + oldStatus.toString() + " to " + newStatus
      // .toString());
      currentSpectraDataSetCalc = null;
    });

    if (currentSpectraDataSetCalc != null) {
      currentSpectraDataSetCalc.setStatus(TaskStatus.CANCELED);
    }
    currentSpectraDataSetCalc = thread;
    getSpectrumPlot().getXYPlot().clearDomainMarkers();
    MZmineCore.getTaskController().addTask(thread);
  }

  // ----- Property getters and setters -----

  @NotNull
  public TICPlotType getPlotType() {
    return plotType.get();
  }

  /**
   * Changes the plot type. Also recalculates the all data sets if changed from BPC to TIC.
   *
   * @param plotType The new plot type.
   */
  public void setPlotType(@NotNull TICPlotType plotType) {
    this.plotType.set(plotType);
  }

  @NotNull
  public ObjectProperty<TICPlotType> plotTypeProperty() {
    return plotType;
  }

  @NotNull
  public ObjectProperty<ChromatogramCursorPosition> chromPositionProperty() {
    return chromPosition;
  }

  @NotNull
  public ChromatogramCursorPosition getChromPosition() {
    return chromPosition.get();
  }

  private void setChromPosition(@NotNull ChromatogramCursorPosition chromPosition) {
    this.chromPosition.set(chromPosition);
  }

  public SpectrumCursorPosition getSpectrumPosition() {
    return spectrumPosition.get();
  }

  public void setSpectrumPosition(SpectrumCursorPosition spectrumPosition) {
    this.spectrumPosition.set(spectrumPosition);
  }

  public ObjectProperty<SpectrumCursorPosition> spectrumPositionProperty() {
    return spectrumPosition;
  }

  /**
   * To listen to changes in the selected raw data file, use {@link ChromatogramAndSpectraVisualizer#chromPositionProperty#addListener}.
   *
   * @return Returns the currently selected raw data file. Could be null.
   */
  @Nullable
  public RawDataFile getSelectedRawDataFile() {
    ChromatogramCursorPosition pos = getChromPosition();
    return (pos == null) ? null : pos.getDataFile();
  }

  @Nullable
  public Range<Double> getMzRange() {
    return mzRange.get();
  }

  public void setMzRange(@Nullable Range<Double> mzRange) {
    this.mzRange.set(mzRange);
  }

  @NotNull
  public ObjectProperty<Range<Double>> mzRangeProperty() {
    return mzRange;
  }

  @NotNull
  public ScanSelection getScanSelection() {
    return scanSelection.get();
  }

  /**
   * Sets the scan selection. Also updates all data sets in the chromatogram plot accordingly.
   *
   * @param selection The new scan selection.
   */
  public void setScanSelection(@NotNull ScanSelection selection) {
    scanSelection.set(selection);
  }

  @NotNull
  public ObjectProperty<ScanSelection> scanSelectionProperty() {
    return scanSelection;
  }

  @NotNull
  public MZTolerance getChromMzTolerance() {
    return chromMzTolerance.get();
  }

  public void setChromMzTolerance(@NotNull MZTolerance chromMzTolerance) {
    this.chromMzTolerance.set(chromMzTolerance);
  }

  @NotNull
  public ObjectProperty<MZTolerance> chromMzToleranceProperty() {
    return chromMzTolerance;
  }

  // ----- Object method overrides -----
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ChromatogramAndSpectraVisualizer)) {
      return false;
    }
    ChromatogramAndSpectraVisualizer that = (ChromatogramAndSpectraVisualizer) o;
    return showSpectraOfEveryRawFile == that.showSpectraOfEveryRawFile && chromPlot.equals(
        that.chromPlot) && spectrumPlot.equals(that.spectrumPlot) && Objects.equals(
        scanSelection.get(), that.scanSelection.get()) && Objects.equals(mzRange.get(),
        that.mzRange.get()) && Objects.equals(chromPosition.get(), that.chromPosition.get())
           && Objects.equals(rtMarker, that.rtMarker) && chromMzTolerance.get()
               .equals(that.chromMzTolerance.get()) && Objects.equals(filesAndDataSets,
        that.filesAndDataSets);
  }

  @Override
  public int hashCode() {
    return Objects.hash(chromPlot, spectrumPlot, scanSelection, mzRange, chromPosition,
        showSpectraOfEveryRawFile, rtMarker, chromMzTolerance, filesAndDataSets);
  }
}
