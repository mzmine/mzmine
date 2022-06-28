/*
 * Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.rawdataoverview;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.javafx.TableViewUitls;
import java.text.NumberFormat;
import java.util.logging.Logger;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.skin.TableViewSkin;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.layout.GridPane;
import org.jetbrains.annotations.NotNull;

public class RawDataFileInfoPaneController {

  private static final Logger logger = Logger.getLogger(
      RawDataFileInfoPaneController.class.getName());

  private boolean populated = false;

  @FXML
  private TableView<Scan> rawDataTableView;

  @FXML
  private TableColumn<Scan, Integer> scanColumn;

  @FXML
  private TableColumn<Scan, Float> rtColumn;

  @FXML
  private TableColumn<Scan, Double> basePeakColumn;

  @FXML
  private TableColumn<Scan, Double> basePeakIntensityColumn;

  @FXML
  private TableColumn<Scan, Integer> msLevelColumn;

  @FXML
  private TableColumn<Scan, Double> precursorMzColumn;

  @FXML
  private TableColumn<Scan, Range<Double>> mzRangeColumn;

  @FXML
  private TableColumn<Scan, String> scanTypeColumn;

  @FXML
  private TableColumn<Scan, String> polarityColumn;
  @FXML
  private TableColumn<Scan, Float> injectTimeColumn;

  @FXML
  private TableColumn<Scan, String> definitionColumn;

  @FXML
  private Label lblNumScans;

  @FXML
  private Label lblRtRange;

  @FXML
  private Label lblMzRange;

  @FXML
  private Label lblMaxTIC;

  @FXML
  private GridPane metaDataGridPane;

  @FXML
  public void initialize() {
    scanColumn.setCellValueFactory(p -> new SimpleObjectProperty<>(p.getValue().getScanNumber()));
    rtColumn.setCellValueFactory(p -> new SimpleObjectProperty<>(p.getValue().getRetentionTime()));
    msLevelColumn.setCellValueFactory(p -> new SimpleObjectProperty<>(p.getValue().getMSLevel()));
    basePeakColumn.setCellValueFactory(
        p -> new SimpleObjectProperty<>(p.getValue().getBasePeakMz()));
    basePeakIntensityColumn.setCellValueFactory(
        p -> new SimpleObjectProperty<>(p.getValue().getBasePeakIntensity()));
    precursorMzColumn.setCellValueFactory(
        p -> new SimpleObjectProperty<>(p.getValue().getPrecursorMz()));
    mzRangeColumn.setCellValueFactory(
        p -> new SimpleObjectProperty<>(p.getValue().getScanningMZRange()));
    scanTypeColumn.setCellValueFactory(
        p -> new SimpleStringProperty(p.getValue().getSpectrumType().toString()));
    polarityColumn.setCellValueFactory(
        p -> new SimpleStringProperty(p.getValue().getPolarity().toString()));
    injectTimeColumn.setCellValueFactory(
        p -> new SimpleObjectProperty<>(p.getValue().getInjectionTime()));
    definitionColumn.setCellValueFactory(
        p -> new SimpleStringProperty(p.getValue().getScanDefinition()));

    NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
    NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
    NumberFormat itFormat = MZmineCore.getConfiguration().getIntensityFormat();

    TableViewUitls.setFormattedCellFactory(precursorMzColumn, mzFormat);
    TableViewUitls.setFormattedCellFactory(basePeakColumn, mzFormat);
    TableViewUitls.setFormattedCellFactory(basePeakIntensityColumn, itFormat);
    TableViewUitls.setFormattedCellFactory(rtColumn, rtFormat);
    TableViewUitls.setFormattedCellFactory(injectTimeColumn, rtFormat);
    TableViewUitls.setFormattedRangeCellFactory(mzRangeColumn, mzFormat);

    TableViewUitls.autoFitLastColumn(rawDataTableView);
  }

  /**
   * Only populate the table if it gets selected. This is called by a listener in
   * {@link RawDataOverviewWindowController}.
   *
   * @param rawDataFile
   */
  protected void populate(RawDataFile rawDataFile) {
    if (populated) {
      return;
    }
    logger.fine("Populating table for raw data file " + rawDataFile.getName());
    populated = true;
    updateRawDataFileInfo(rawDataFile);
    updateScanTable(rawDataFile);
  }

  private void updateRawDataFileInfo(RawDataFile rawDataFile) {
    // clear previous info
    metaDataGridPane.getChildren().removeIf(
        node -> GridPane.getColumnIndex(node) != null && GridPane.getColumnIndex(node) == 1);

    String scansMSLevel = "Total scans (" + rawDataFile.getNumOfScans() + ") ";
    for (int i = 0; i < rawDataFile.getMSLevels().length; i++) {
      int level = rawDataFile.getMSLevels()[i];
      scansMSLevel =
          scansMSLevel + "MS" + level + " level (" + rawDataFile.getScanNumbers(level).size()
              + ") ";
      lblNumScans.setText(scansMSLevel);
    }

    String rtRangeMSLevel = "";
    for (int i = 0; i < rawDataFile.getMSLevels().length; i++) {
      rtRangeMSLevel = rtRangeMSLevel + "MS" + rawDataFile.getMSLevels()[i] + " level "
          + MZminePreferences.rtFormat.getValue()
          .format(rawDataFile.getDataRTRange(i + 1).lowerEndpoint()) + "-"
          + MZminePreferences.rtFormat.getValue()
          .format(rawDataFile.getDataRTRange(i + 1).upperEndpoint()) + " [min] ";
      lblRtRange.setText(rtRangeMSLevel);
    }

    String mzRangeMSLevel = "";
    for (int i = 0; i < rawDataFile.getMSLevels().length; i++) {
      mzRangeMSLevel = mzRangeMSLevel + "MS" + rawDataFile.getMSLevels()[i] + " level "
          + MZminePreferences.mzFormat.getValue()
          .format(rawDataFile.getDataMZRange(i + 1).lowerEndpoint()) + "-"
          + MZminePreferences.mzFormat.getValue()
          .format(rawDataFile.getDataMZRange(i + 1).upperEndpoint()) + " ";
      lblMzRange.setText(mzRangeMSLevel);
    }

    lblMaxTIC.setText(MZminePreferences.intensityFormat.getValue()
        .format(rawDataFile.getDataMaxTotalIonCurrent(1)));
  }

  protected void updateScanTable(RawDataFile rawDataFile) {
    rawDataTableView.getItems().clear();
    ObservableList<Scan> scans = rawDataFile.getScans();

    if (scans.size() > 5E5) {
      // it's not the computation that takes long, it's putting the data into the table.
      // This bricks the MZmine window
      logger.info("Number of entries >500 000 for raw data file " + rawDataFile.getName() + " ("
          + rawDataFile.getNumOfScans() + ")");
      logger.info("Will not compute table data.");
      return;
    }
    rawDataTableView.getItems().addAll(scans);
  }

  @NotNull
  public Range<Integer> getVisibleRange() {
    TableViewSkin<?> skin = (TableViewSkin) rawDataTableView.getSkin();
    if (skin == null) {
      return Range.closed(0, 0);
    }
    VirtualFlow<?> flow = (VirtualFlow) skin.getChildren().get(1);
    int indexFirst;
    int indexLast;
    if (flow != null && flow.getFirstVisibleCell() != null && flow.getLastVisibleCell() != null) {
      indexFirst = flow.getFirstVisibleCell().getIndex();
      if (indexFirst >= rawDataTableView.getItems().size()) {
        indexFirst = rawDataTableView.getItems().size() - 1;
      }
      indexLast = flow.getLastVisibleCell().getIndex();
      if (indexLast >= rawDataTableView.getItems().size()) {
        indexLast = rawDataTableView.getItems().size() - 1;
      }
    } else {
      indexFirst = 0;
      indexLast = 0;
    }
    return Range.closed(indexFirst, indexLast);
  }

  /**
   * Used to add action listener for table selection
   */
  protected TableView<Scan> getRawDataTableView() {
    return rawDataTableView;
  }

}
