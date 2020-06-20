package io.github.mzmine.modules.visualization.chromatogramandspectra;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.chromatogram.CursorPosition;
import io.github.mzmine.modules.visualization.chromatogram.TICDataSet;
import io.github.mzmine.modules.visualization.chromatogram.TICPlot;
import io.github.mzmine.modules.visualization.chromatogram.TICPlotType;
import io.github.mzmine.modules.visualization.rawdataoverview.RawDataOverviewWindowController;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.ScanDataSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.awt.BasicStroke;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.plot.ValueMarker;

public class ChromatogramAndSpectraVisualizer extends SplitPane {

  private static final BasicStroke MARKER_STROKE = new BasicStroke(1.0f);

  protected TICPlot chromPlot;
  protected SpectraPlot spectrumPlot;
  protected ScanSelection scanSelection;
  protected Range<Double> mzRange;
  //  protected ObjectProperty<RawDataFile> selectedRawDataFile;
  protected ObjectProperty<CursorPosition> currentPosition;
  protected boolean showSpectraOfEveryRawFile;

  protected ValueMarker rtMarker;

  // tolerance for the eics of the base peak in the selected scans.
  protected MZTolerance bpcChromTolerance;

  protected Hashtable<RawDataFile, TICDataSet> chromDataSets;

  public ChromatogramAndSpectraVisualizer(Orientation orientation) {
    super();

    setOrientation(orientation);
    showSpectraOfEveryRawFile = true;
    bpcChromTolerance = new MZTolerance(0.001, 10);

//    selectedRawDataFile = new SimpleObjectProperty<>();
    currentPosition = new SimpleObjectProperty<>();

    chromPlot = new TICPlot();
    spectrumPlot = new SpectraPlot();
    scanSelection = new ScanSelection(1);
    getItems().addAll(chromPlot, spectrumPlot);

    initializeChromatogramMouseListener();
    initializeCursorPositionListener();
  }

  /**
   * Changes the plot type. Also recalculates the all data sets if changed from BPC to TIC.
   *
   * @param plotType The new plot type.
   */
  public void setPlotType(TICPlotType plotType) {
    chromPlot.setPlotType(plotType);

    List<RawDataFile> rawDataFiles = new ArrayList<>();
    chromDataSets.keySet().forEach(raw -> rawDataFiles.add(raw));
    rawDataFiles.forEach(raw -> removeRawDataFile(raw));
    rawDataFiles.forEach(raw -> addRawDataFile(raw));
  }

  /**
   * Sets the scan selection. Also updates all data sets in the chromatogram plot accordingly.
   *
   * @param selection The new scan selection.
   */
  public void setScanSelection(Scan selection) {
    this.scanSelection = scanSelection;

    List<RawDataFile> rawDataFiles = new ArrayList<>();
    chromDataSets.keySet().forEach(raw -> rawDataFiles.add(raw));
    rawDataFiles.forEach(raw -> removeRawDataFile(raw));
    rawDataFiles.forEach(raw -> addRawDataFile(raw));
  }

  public TICPlotType getPlotType() {
    return chromPlot.getPlotType();
  }

  public RawDataFile[] getRawDataFiles() {
    return chromDataSets.keySet().toArray(new RawDataFile[0]);
  }

  /**
   * Sets the raw data files to be displayed. Already present files are not removed to optimise
   * performance. This should be called over {@link RawDataOverviewWindowController#addRawDataFile}
   * if possible.
   *
   * @param rawDataFiles
   */
  public void setRawDataFiles(List<RawDataFile> rawDataFiles) {
    // remove files first
    List<RawDataFile> filesToProcess = new ArrayList<>();
    for (RawDataFile rawDataFile : chromDataSets.keySet()) {
      if (!rawDataFiles.contains(rawDataFile)) {
        filesToProcess.add(rawDataFile);
      }
    }
    filesToProcess.forEach(r -> removeRawDataFile(r));

    // presence of file is checked in the add method
    rawDataFiles.forEach(r -> addRawDataFile(r));
  }

  public void addRawDataFile(RawDataFile rawDataFile) {
    final Scan[] scans = scanSelection.getMatchingScans(rawDataFile);
    if (scans.length == 0) {
      MZmineCore.getDesktop().displayErrorMessage("No scans found.");
      return;
    }

    TICDataSet ticDataset = new TICDataSet(rawDataFile, scans, mzRange, null, getPlotType());
    chromDataSets.put(rawDataFile, ticDataset);
    chromPlot.addTICDataset(ticDataset);

//    if (chromDataSets.size() == 1) {
//      setSelectedRawDataFile(rawDataFile);
//    }
  }

  public void removeRawDataFile(RawDataFile file) {
    TICDataSet dataset = chromDataSets.get(file);
    chromPlot.getXYPlot().setDataset(chromPlot.getXYPlot().indexOf(dataset), null);
    chromDataSets.remove(file);
  }

  /**
   * Adds a listener to the currentPostion to update the spectraPlot accordingly.
   */
  private void initializeCursorPositionListener() {
    currentPositionProperty().addListener((observableValue, oldValue, pos) -> {
      RawDataFile file = pos.getDataFile();

      updateDomainMarker(pos);

      spectrumPlot.removeAllDataSets();

      if (showSpectraOfEveryRawFile) {
        double rt = pos.getRetentionTime();
        chromDataSets.keySet().forEach(rawDataFile -> {
          int num = rawDataFile.getScanNumberAtRT(rt, scanSelection.getMsLevel());
          if (num != -1) {
            Scan scan = rawDataFile.getScan(num);
            ScanDataSet dataSet = new ScanDataSet(scan);
            spectrumPlot.addDataSet(dataSet, rawDataFile.getColorAWT(), false);
          }
        });
      } else {
        ScanDataSet dataSet = new ScanDataSet(file.getScan(pos.getScanNumber()));
        spectrumPlot.addDataSet(dataSet, pos.getDataFile().getColorAWT(), false);
      }
    });
  }

  /**
   * Listens to clicks in the chromatogram plot and updates the selected raw data file accordingly.
   */
  private void initializeChromatogramMouseListener() {
    assert chromPlot != null;

    chromPlot.getCanvas().addChartMouseListener(new ChartMouseListenerFX() {
      @Override
      public void chartMouseClicked(ChartMouseEventFX event) {
        CursorPosition pos = getCursorPosition();
        if (pos != null) {
//          setSelectedRawDataFile(pos.getDataFile());
          setCurrentPosition(pos);
        }
      }

      @Override
      public void chartMouseMoved(ChartMouseEventFX event) {
        // not in use
      }
    });
  }

  /**
   * Changes the position of the domain marker. Is called by the mouse listener initialized in
   * {@link ChromatogramAndSpectraVisualizer#initializeCursorPositionListener()}
   *
   * @param pos
   */
  private void updateDomainMarker(@Nonnull CursorPosition pos) {
    chromPlot.getXYPlot().clearDomainMarkers();

    if (rtMarker == null) {
      ValueMarker rtMarker = new ValueMarker(
          pos.getDataFile().getScan(pos.getScanNumber()).getRetentionTime());
      rtMarker.setStroke(MARKER_STROKE);
    } else {
      rtMarker.setValue(pos.getDataFile().getScan(pos.getScanNumber()).getRetentionTime());
    }
    rtMarker.setPaint(MZmineCore.getConfiguration().getDefaultColorPalette().getNeutralColorAWT());

    chromPlot.getXYPlot().addDomainMarker(rtMarker);
  }


  public ObjectProperty<CursorPosition> currentPositionProperty() {
    return currentPosition;
  }

  public void setCurrentPosition(CursorPosition currentPosition) {
    this.currentPosition.set(currentPosition);
  }

  public CursorPosition getCurrentPosition() {
    return currentPosition.get();
  }

  /**
   * To listen to changes in the selected raw data file, use {@link ChromatogramAndSpectraVisualizer#currentPositionProperty#addListener}.
   *
   * @return Returns the currently selected raw data file. Could be null.
   */
  @Nullable
  public RawDataFile getSelectedRawDataFile() {
    CursorPosition pos = getCursorPosition();
    return (pos == null) ? null : pos.getDataFile();
  }

  /**
   * @return current cursor position
   */
  public CursorPosition getCursorPosition() {
    double selectedRT = chromPlot.getXYPlot().getDomainCrosshairValue();
    double selectedIT = chromPlot.getXYPlot().getRangeCrosshairValue();
    Enumeration<TICDataSet> e = chromDataSets.elements();
    while (e.hasMoreElements()) {
      TICDataSet dataSet = e.nextElement();
      int index = dataSet.getIndex(selectedRT, selectedIT);
      if (index >= 0) {
        double mz = 0;
        if (getPlotType() == TICPlotType.BASEPEAK) {
          mz = dataSet.getZValue(0, index);
        }
        return new CursorPosition(selectedRT, mz, selectedIT, dataSet.getDataFile(),
            dataSet.getScanNumber(index));
      }
    }
    return null;
  }

  /*public RawDataFile getSelectedRawDataFile() {
    return selectedRawDataFile.get();
  }

  public ObjectProperty<RawDataFile> selectedRawDataFileProperty() {
    return selectedRawDataFile;
  }

  public void setSelectedRawDataFile(
      RawDataFile selectedRawDataFile) {
    this.selectedRawDataFile.set(selectedRawDataFile);
  }*/
}
