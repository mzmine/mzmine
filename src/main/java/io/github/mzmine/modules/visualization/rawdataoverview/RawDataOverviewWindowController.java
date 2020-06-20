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

package io.github.mzmine.modules.visualization.rawdataoverview;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.chromatogram.CursorPosition;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.awt.BasicStroke;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.ObservableMap;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.visualization.chromatogram.TICDataSet;
import io.github.mzmine.modules.visualization.chromatogram.TICPlot;
import io.github.mzmine.modules.visualization.chromatogram.TICPlotType;
import io.github.mzmine.modules.visualization.chromatogram.TICVisualizerWindow;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerWindow;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.MassListDataSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.util.color.Vision;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

/*
 * Raw data overview window controller class
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster)
 */
public class RawDataOverviewWindowController {

  public static final Logger logger = Logger
      .getLogger(RawDataOverviewWindowController.class.getName());

  private TICPlot bpcPlot;
  private TICVisualizerWindow ticWindow;
  private Color posColor;
  private Color negColor;
  private Color neuColor;
  private Vision vision;
  private boolean initialized = false;

  private RawDataFile selectedRawDataFile;

  private ObservableMap<RawDataFile, RawDataFileInfoPaneController> rawDataFilesAndControllers = FXCollections
      .observableMap(new HashMap<>());
  private ObservableMap<RawDataFile, Tab> rawDataFilesAndTabs = FXCollections
      .observableMap(new HashMap<>());

  @FXML
  private Label rawDataLabel;

  @FXML
  private BorderPane chromatogramPane;

  @FXML
  private BorderPane spectraPane;

  @FXML
  private TabPane tpRawDataInfo;

  public void initialize(RawDataFile rawDataFile) {

//    this.rawDataFile = rawDataFile;
    // add meta data
    rawDataLabel.setText("Overview of raw data file(s): " + rawDataFile.getName());

    // set colors depending on vision
    SimpleColorPalette palette = MZmineCore.getConfiguration().getDefaultColorPalette();
    posColor = palette.getPositiveColorAWT();
    negColor = palette.getNegativeColorAWT();
    neuColor = palette.getNeutralColorAWT();

    initialized = true;

    addRawDataFile(rawDataFile);
    addTICMouseListener();

    updatePlots();

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
    for (RawDataFile rawDataFile : rawDataFilesAndControllers.keySet()) {
      if (!rawDataFiles.contains(rawDataFile)) {
        filesToProcess.add(rawDataFile);
      }
    }
    filesToProcess.forEach(r -> removeRawDataFile(r));

    // presence of file is checked in the add method
    rawDataFiles.forEach(r -> addRawDataFile(r));
  }

  /**
   * Adds a raw data file to the overview.
   * <p>
   * Will overlay  plots if multiple raw data files are selected.
   *
   * @param raw
   */
  public void addRawDataFile(RawDataFile raw) {

    if (!initialized) {
      initialize(raw);
    }
    if (rawDataFilesAndControllers.containsKey(raw)) {
      return;
    }

    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("RawDataFileInfoPane.fxml"));
      BorderPane pane = loader.load();
      rawDataFilesAndControllers.put(raw, loader.getController());
      RawDataFileInfoPaneController con = rawDataFilesAndControllers.get(raw);
      con.getRawDataTableView().getSelectionModel().selectedItemProperty().addListener(c -> {
        setSelectedRawDataFile(raw);
        updatePlots();
      });

      Tab rawDataFileTab = new Tab(raw.getName());
      rawDataFileTab.setContent(pane);
      tpRawDataInfo.getTabs().add(rawDataFileTab);

      rawDataFileTab.selectedProperty().addListener((obs, o, n) -> {
        if (n == true) {
          con.populate(raw);
        }
      });

      rawDataFileTab.setOnClosed((e) -> {
        logger.fine("Removing raw data file " + raw.getName());
        removeRawDataFile(raw);
      });

      rawDataFilesAndTabs.put(raw, rawDataFileTab);

      if (ticWindow == null) {
        initialiseBPCPlot(raw);
      } else {
        ticWindow.addRawDataFile(raw);
      }
      if (rawDataFileTab.selectedProperty().getValue()) {
        con.populate(raw);
      }

    } catch (IOException e) {
      logger.log(Level.SEVERE, "Could not load RawDataFileInfoPane.fxml", e);
    }

    logger.fine("Added raw data file " + raw.getName());
  }

  public void removeRawDataFile(RawDataFile raw) {
    ticWindow.removeRawDataFile(raw);
    rawDataFilesAndControllers.remove(raw);
    Tab tab = rawDataFilesAndTabs.remove(raw);
    tpRawDataInfo.getTabs().remove(tab);
  }

  // plot update methods

  private void initialiseBPCPlot(RawDataFile rawDataFile) {
    // get MS1 scan selection to draw base peak plot
    ScanSelection scanSelection = new ScanSelection(rawDataFile.getDataRTRange(1), 1);

    // get TIC window
    ticWindow = new TICVisualizerWindow(new RawDataFile[]{rawDataFile}, // raw
        TICPlotType.BASEPEAK, // plot type
        scanSelection, // scan selection
        rawDataFile.getDataMZRange(), // mz range
        null, // selected features
        null); // labels+

    // get TIC Plot
    this.bpcPlot = ticWindow.getTICPlot();
    bpcPlot.getChart().getLegend().setVisible(false);
    chromatogramPane.setCenter(bpcPlot.getParent());

    // get plot
    XYPlot plot = (XYPlot) bpcPlot.getChart().getPlot();
    plot.setDomainCrosshairVisible(false);
    plot.setRangeCrosshairVisible(false);
    setSelectedRawDataFile(rawDataFile);
  }

  private void addTICMouseListener() {
    // Add mouse listener to chromatogram
    // mouse listener
    bpcPlot.addChartMouseListener(new ChartMouseListenerFX() {

      @Override
      public void chartMouseMoved(ChartMouseEventFX event) {
      }

      @Override
      public void chartMouseClicked(ChartMouseEventFX event) {

        CursorPosition pos = ticWindow.getCursorPosition();
        RawDataFile selectedRawDataFile = pos.getDataFile();
        setSelectedRawDataFile(selectedRawDataFile);

        RawDataFileInfoPaneController con = rawDataFilesAndControllers.get(selectedRawDataFile);
        if (con == null || selectedRawDataFile == null) {
          logger.info("Cannot find controller for raw data file " + selectedRawDataFile.getName());
          return;
        }

        TableView<ScanDescription> rawDataTableView = con.getRawDataTableView();
        tpRawDataInfo.getSelectionModel().select(rawDataFilesAndTabs.get(selectedRawDataFile));

        if (rawDataTableView.getItems() != null) {
          if (rawDataTableView.getSelectionModel().getSelectedItem() != null) {
            try {
              String scanNumberString = String.valueOf(pos.getScanNumber());
              rawDataTableView.getItems().stream()
                  .filter(item -> item.getScanNumber().equals(scanNumberString)).findFirst()
                  .ifPresent(item -> {
                    rawDataTableView.getSelectionModel().select(item);
                    rawDataTableView.scrollTo(item);
                  });
              chromatogramPane.setCenter(bpcPlot.getParent());
            } catch (Exception e) {
              e.getStackTrace();
            }
          }
        }
      }
    });
  }

  private void initialiseSpectrumPlot() {

  }

  private void updatePlots() {

    if (getSelectedRawDataFile() == null) {
      return;
    }

    RawDataFile selectedRawDataFile = getSelectedRawDataFile();

    TableView<ScanDescription> rawDataTableView = rawDataFilesAndControllers
        .get(getSelectedRawDataFile()).getRawDataTableView();

    if (rawDataTableView.getItems() != null) {
      if (rawDataTableView.getSelectionModel().getSelectedItem() != null) {
        try {

          // draw spectra plot
          String scanNumberString =
              rawDataTableView.getSelectionModel().getSelectedItem().getScanNumber();
          int scanNumber = Integer.parseInt(scanNumberString);
          Scan scan = selectedRawDataFile.getScan(scanNumber);
          SpectraVisualizerWindow spectraWindow = new SpectraVisualizerWindow(selectedRawDataFile);
          spectraWindow.loadRawData(scan);

          // set color
          XYPlot plotSpectra = (XYPlot) spectraWindow.getSpectrumPlot().getChart().getPlot();

          // set color
          plotSpectra.getRenderer().setSeriesPaint(0, posColor);

          // add mass list
          MassList[] massLists = selectedRawDataFile.getScan(scanNumber).getMassLists();
          for (MassList massList : massLists) {
            MassListDataSet dataset = new MassListDataSet(massList);
            spectraWindow.getSpectrumPlot().addDataSet(dataset, negColor, true);
          }

          spectraPane.setCenter(spectraWindow.getScene().getRoot());

          // add a retention time Marker to the TIC
          ValueMarker marker = new ValueMarker(
              selectedRawDataFile.getScan(scanNumber).getRetentionTime());
          marker.setPaint(negColor);
          marker.setStroke(new BasicStroke(1.0f));
          XYPlot plotTic = (XYPlot) bpcPlot.getChart().getPlot();

          // set color
          plotTic.getRenderer().setSeriesPaint(0, posColor);

          // delete old marker
          plotTic.clearDomainMarkers();

          // add new marker
          plotTic.addDomainMarker(marker);

          // add EIC of scan base peak if selected scan is MS1 level, when MS2 show base peak of
          // precursor

          // get MS1 scan selection to draw tic plot
          if (scan.getMSLevel() == 1 || scan.getMSLevel() == 2) {
            ScanSelection scanSelection = new ScanSelection(selectedRawDataFile.getDataRTRange(1), 1);

            // mz range for 10 ppm window
            Range<Double> mzRange = null;
            double mzHighestDataPoint = 0.0;
            if (scan.getMSLevel() == 1) {
              mzHighestDataPoint = scan.getHighestDataPoint().getMZ();
            } else if (scan.getMSLevel() == 2) {
              mzHighestDataPoint = scan.getPrecursorMZ();
            } else {
              plotTic.setDataset(1, null);
            }

            double tenppm = (mzHighestDataPoint * 10E-6);
            double upper = mzHighestDataPoint + tenppm;
            double lower = mzHighestDataPoint - tenppm;
            mzRange = Range.closed(lower - tenppm, upper + tenppm);
            scan.getDataPointsByMass(mzRange);

            TICDataSet dataset = new TICDataSet(selectedRawDataFile,
                scanSelection.getMatchingScans(selectedRawDataFile), mzRange, ticWindow);

            XYAreaRenderer renderer = new XYAreaRenderer();
            renderer.setSeriesPaint(0, neuColor);

            plotTic.setRenderer(1, renderer);
            plotTic.setDataset(1, dataset);

            chromatogramPane.setCenter(bpcPlot.getParent());
          } else {
            plotTic.setDataset(1, null);
          }
        } catch (Exception e) {
          e.getStackTrace();
        }

      }
    }
  }

  public RawDataFile getSelectedRawDataFile() {
    return selectedRawDataFile;
  }

  private void setSelectedRawDataFile(RawDataFile selectedRawDataFile) {
    this.selectedRawDataFile = selectedRawDataFile;
  }
}
