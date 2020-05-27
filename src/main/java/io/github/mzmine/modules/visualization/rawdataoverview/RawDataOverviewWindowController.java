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
import io.github.mzmine.util.color.SimpleColorPalette;
import java.awt.BasicStroke;
import java.awt.Color;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.modules.visualization.chromatogram.TICDataSet;
import io.github.mzmine.modules.visualization.chromatogram.TICPlot;
import io.github.mzmine.modules.visualization.chromatogram.TICPlotType;
import io.github.mzmine.modules.visualization.chromatogram.TICVisualizerWindow;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerWindow;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.MassListDataSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.util.color.Colors;
import io.github.mzmine.util.color.Vision;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

/*
 * Raw data overview window controller class
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster)
 */
public class RawDataOverviewWindowController {

  private RawDataFile rawDataFile;
  private TICPlot ticPlot;
  private TICVisualizerWindow ticWindow;
  private Color posColor;
  private Color negColor;
  private Color neuColor;
  private Vision vision;

  @FXML
  private Label rawDataLabel;

  @FXML
  private TableView<ScanDescription> rawDataTableView;

  @FXML
  private TableColumn<ScanDescription, String> scanColumn;

  @FXML
  private TableColumn<ScanDescription, String> rtColumn;

  @FXML
  private TableColumn<ScanDescription, String> msLevelColumn;

  @FXML
  private TableColumn<ScanDescription, String> precursorMzColumn;

  @FXML
  private TableColumn<ScanDescription, String> mzRangeColumn;

  @FXML
  private TableColumn<ScanDescription, String> scanTypeColumn;

  @FXML
  private TableColumn<ScanDescription, String> polarityColumn;

  @FXML
  private TableColumn<ScanDescription, String> definitionColumn;

  @FXML
  private GridPane metaDataGridPane;

  @FXML
  private BorderPane chromatogramPane;

  @FXML
  private BorderPane spectraPane;

  ObservableList<ScanDescription> tableData = FXCollections.observableArrayList();

  public void initialize(RawDataFile rawDataFile) {

    this.rawDataFile = rawDataFile;
    int numberOfScans = rawDataFile.getNumOfScans();

    // set colors depending on vision
    SimpleColorPalette palette = MZmineCore.getConfiguration().getDefaultColorPalette();
    posColor = palette.getPositiveColorAWT();
    negColor = palette.getNegativeColorAWT();
    neuColor = palette.getNeutralColorAWT();

    // add meta data
    rawDataLabel.setText(rawDataLabel.getText() + " " + rawDataFile.getName());

    String scansMSLevel = "Total scans (" + rawDataFile.getNumOfScans() + ") ";
    Label labelScansMSLevel = new Label();
    metaDataGridPane.add(labelScansMSLevel, 1, 0);
    for (int i = 0; i < rawDataFile.getMSLevels().length; i++) {
      scansMSLevel = scansMSLevel + "MS" + rawDataFile.getMSLevels()[i] + " level ("
          + rawDataFile.getScanNumbers(i + 1).length + ") ";
      labelScansMSLevel.setText(scansMSLevel);
    }

    String rtRangeMSLevel = "";
    Label labelRtRangeLevel = new Label();
    metaDataGridPane.add(labelRtRangeLevel, 1, 1);
    for (int i = 0; i < rawDataFile.getMSLevels().length; i++) {
      rtRangeMSLevel = rtRangeMSLevel + "MS" + rawDataFile.getMSLevels()[i] + " level "
          + MZminePreferences.rtFormat.getValue()
              .format(rawDataFile.getDataRTRange(i + 1).lowerEndpoint())
          + "-" + MZminePreferences.rtFormat.getValue()
              .format(rawDataFile.getDataRTRange(i + 1).upperEndpoint())
          + " [min] ";
      labelRtRangeLevel.setText(rtRangeMSLevel);
    }

    String mzRangeMSLevel = "";
    Label labelMzRangeMSLevel = new Label();
    metaDataGridPane.add(labelMzRangeMSLevel, 1, 2);
    for (int i = 0; i < rawDataFile.getMSLevels().length; i++) {
      mzRangeMSLevel = mzRangeMSLevel + "MS" + rawDataFile.getMSLevels()[i] + " level "
          + MZminePreferences.mzFormat.getValue()
              .format(rawDataFile.getDataMZRange(i + 1).lowerEndpoint())
          + "-" + MZminePreferences.mzFormat.getValue()
              .format(rawDataFile.getDataMZRange(i + 1).upperEndpoint())
          + " ";
      labelMzRangeMSLevel.setText(mzRangeMSLevel);
    }

    metaDataGridPane.add(new Label(MZminePreferences.intensityFormat.getValue()
        .format(rawDataFile.getDataMaxTotalIonCurrent(1))), 1, 3);

    // add raw data to table
    for (int i = 1; i < numberOfScans + 1; i++) {
      Scan scan = rawDataFile.getScan(i);

      // check for precursor
      String precursor = "";
      if (scan.getPrecursorMZ() == 0.000 || scan.getPrecursorMZ() == -1.000) {
        precursor = "";
      } else {
        precursor = MZminePreferences.mzFormat.getValue().format(scan.getPrecursorMZ());
      }

      // format mzRange
      String mzRange =
          MZminePreferences.mzFormat.getValue().format(scan.getDataPointMZRange().lowerEndpoint())
              + "-" + MZminePreferences.mzFormat.getValue()
                  .format(scan.getDataPointMZRange().upperEndpoint());

      tableData.add(new ScanDescription(Integer.toString(i), // scan number
          MZminePreferences.rtFormat.getValue().format(scan.getRetentionTime()), // rt
          Integer.toString(scan.getMSLevel()), // MS level
          precursor, // precursor mz
          mzRange, // mz range
          scan.getSpectrumType().toString(), // profile/centroid
          scan.getPolarity().toString(), // polarity
          scan.getScanDefinition()) // definition
      );
    }

    scanColumn.setCellValueFactory(new PropertyValueFactory<>("scanNumber"));
    rtColumn.setCellValueFactory(new PropertyValueFactory<>("retentionTime"));
    msLevelColumn.setCellValueFactory(new PropertyValueFactory<>("msLevel"));
    precursorMzColumn.setCellValueFactory(new PropertyValueFactory<>("precursorMz"));
    mzRangeColumn.setCellValueFactory(new PropertyValueFactory<>("mzRange"));
    scanTypeColumn.setCellValueFactory(new PropertyValueFactory<>("scanType"));
    polarityColumn.setCellValueFactory(new PropertyValueFactory<>("polarity"));
    definitionColumn.setCellValueFactory(new PropertyValueFactory<>("definition"));

    rawDataTableView.setItems(tableData);

    // add action listener to row selection
    rawDataTableView.getSelectionModel().selectedItemProperty().addListener((tableData) -> {
      updatePlots();
    });

    // select first row
    rawDataTableView.getSelectionModel().select(0);

    // get MS1 scan selection to draw base peak plot
    ScanSelection scanSelection = new ScanSelection(rawDataFile.getDataRTRange(1), 1);

    // get TIC window
    ticWindow = new TICVisualizerWindow(new RawDataFile[] {rawDataFile}, // raw
        TICPlotType.BASEPEAK, // plot type
        scanSelection, // scan selection
        rawDataFile.getDataMZRange(), // mz range
        null, // selected features
        null); // labels

    // get TIC Plot
    this.ticPlot = ticWindow.getTICPlot();
    ticPlot.getChart().getLegend().setVisible(false);
    chromatogramPane.setCenter(ticPlot.getParent());

    // get plot
    XYPlot plot = (XYPlot) ticPlot.getChart().getPlot();
    plot.setDomainCrosshairVisible(false);
    plot.setRangeCrosshairVisible(false);

    // Add mouse listener to chromatogram
    // mouse listener
    ticPlot.addChartMouseListener(new ChartMouseListenerFX() {

      @Override
      public void chartMouseMoved(ChartMouseEventFX event) {}

      @Override
      public void chartMouseClicked(ChartMouseEventFX event) {

        if (rawDataTableView.getItems() != null) {
          if (rawDataTableView.getSelectionModel().getSelectedItem() != null) {
            try {

              // get plot
              XYPlot plot = (XYPlot) ticPlot.getChart().getPlot();

              double xValue = plot.getDomainCrosshairValue();
              if (plot.getDataset() instanceof TICDataSet) {

                // get MS1 scan based on retention time
                double d = Math.pow(10, 2);
                Range<Double> rtRange = Range.open(Math.round(xValue * d) / d - 0.01,
                    Math.round(xValue * d) / d + 0.01);
                ScanSelection selection = new ScanSelection(rtRange, 1);
                String scanNumberString =
                    Integer.toString(selection.getMatchingScanNumbers(rawDataFile)[0]);
                rawDataTableView.getItems().stream()
                    .filter(item -> item.getScanNumber().equals(scanNumberString)).findFirst()
                    .ifPresent(item -> {
                      rawDataTableView.getSelectionModel().select(item);
                      rawDataTableView.scrollTo(item);
                    });

              }
              chromatogramPane.setCenter(ticPlot.getParent());
            } catch (Exception e) {
              e.getStackTrace();
            }
          }
        }
      }
    });

    updatePlots();
  }

  private void updatePlots() {
    if (rawDataTableView.getItems() != null) {
      if (rawDataTableView.getSelectionModel().getSelectedItem() != null) {
        try {

          // draw spectra plot
          String scanNumberString =
              rawDataTableView.getSelectionModel().getSelectedItem().getScanNumber();
          int scanNumber = Integer.parseInt(scanNumberString);
          Scan scan = rawDataFile.getScan(scanNumber);
          SpectraVisualizerWindow spectraWindow = new SpectraVisualizerWindow(rawDataFile);
          spectraWindow.loadRawData(scan);

          // set color
          XYPlot plotSpectra = (XYPlot) spectraWindow.getSpectrumPlot().getChart().getPlot();

          // set color
          plotSpectra.getRenderer().setSeriesPaint(0, posColor);

          // add mass list
          MassList[] massLists = rawDataFile.getScan(scanNumber).getMassLists();
          for (MassList massList : massLists) {
            MassListDataSet dataset = new MassListDataSet(massList);
            spectraWindow.getSpectrumPlot().addDataSet(dataset, negColor, true);
          }

          spectraPane.setCenter(spectraWindow.getScene().getRoot());

          // add a retention time Marker to the TIC
          ValueMarker marker = new ValueMarker(rawDataFile.getScan(scanNumber).getRetentionTime());
          marker.setPaint(negColor);
          marker.setStroke(new BasicStroke(3.0f));
          XYPlot plotTic = (XYPlot) ticPlot.getChart().getPlot();

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
            ScanSelection scanSelection = new ScanSelection(rawDataFile.getDataRTRange(1), 1);

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

            TICDataSet dataset = new TICDataSet(rawDataFile,
                scanSelection.getMatchingScans(rawDataFile), mzRange, ticWindow);

            XYAreaRenderer renderer = new XYAreaRenderer();
            renderer.setSeriesPaint(0, neuColor);

            plotTic.setRenderer(1, renderer);
            plotTic.setDataset(1, dataset);

            chromatogramPane.setCenter(ticPlot.getParent());
          } else {
            plotTic.setDataset(1, null);
          }
        } catch (Exception e) {
          e.getStackTrace();
        }

      }
    }
  }

}
