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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.javafx.StringToDoubleComparator;
import java.text.NumberFormat;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.skin.TableViewSkin;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.layout.GridPane;
import javax.annotation.Nonnull;

public class RawDataFileInfoPaneController {

  private RawDataFile rawDataFile;
  private boolean populated = false;
  private List<TableRow> rawDataTableViewRows;

  @FXML
  private TableView<ScanDescription> rawDataTableView;

  @FXML
  private TableColumn<ScanDescription, String> scanColumn;

  @FXML
  private TableColumn<ScanDescription, String> rtColumn;

  @FXML
  private TableColumn<ScanDescription, Double> basePeakColumn;

  @FXML
  private TableColumn<ScanDescription, String> basePeakIntensityColumn;

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
  private TableColumn<ScanDescription, String> mobilityColumn;

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

  /**
   * Only populate the table if it gets selected. This is called by a listener in {@link
   * RawDataOverviewWindowController}.
   *
   * @param rawDataFile
   */
  protected void populate(RawDataFile rawDataFile) {
    if (populated == true) {
      return;
    }
    populated = true;
    this.rawDataFile = rawDataFile;
    updateRawDataFileInfo(rawDataFile);
    updateScanTable(rawDataFile);
  }

  private void updateRawDataFileInfo(RawDataFile rawDataFile) {
    // clear previous info
    metaDataGridPane.getChildren().removeIf(
        node -> GridPane.getColumnIndex(node) != null && GridPane.getColumnIndex(node) == 1);

    String scansMSLevel = "Total scans (" + rawDataFile.getNumOfScans() + ") ";
    for (int i = 0; i < rawDataFile.getMSLevels().length; i++) {
      scansMSLevel = scansMSLevel + "MS" + rawDataFile.getMSLevels()[i] + " level ("
          + rawDataFile.getScanNumbers(i + 1).length + ") ";
      lblNumScans.setText(scansMSLevel);
    }

    String rtRangeMSLevel = "";
    for (int i = 0; i < rawDataFile.getMSLevels().length; i++) {
      rtRangeMSLevel = rtRangeMSLevel + "MS" + rawDataFile.getMSLevels()[i] + " level "
          + MZminePreferences.rtFormat.getValue()
          .format(rawDataFile.getDataRTRange(i + 1).lowerEndpoint())
          + "-" + MZminePreferences.rtFormat.getValue()
          .format(rawDataFile.getDataRTRange(i + 1).upperEndpoint())
          + " [min] ";
      lblRtRange.setText(rtRangeMSLevel);
    }

    String mzRangeMSLevel = "";
    for (int i = 0; i < rawDataFile.getMSLevels().length; i++) {
      mzRangeMSLevel = mzRangeMSLevel + "MS" + rawDataFile.getMSLevels()[i] + " level "
          + MZminePreferences.mzFormat.getValue()
          .format(rawDataFile.getDataMZRange(i + 1).lowerEndpoint())
          + "-" + MZminePreferences.mzFormat.getValue()
          .format(rawDataFile.getDataMZRange(i + 1).upperEndpoint())
          + " ";
      lblMzRange.setText(mzRangeMSLevel);
    }

    lblMaxTIC.setText(MZminePreferences.intensityFormat.getValue()
        .format(rawDataFile.getDataMaxTotalIonCurrent(1)));
  }

  protected void updateScanTable(RawDataFile rawDataFile) {

    scanColumn.setCellValueFactory(new PropertyValueFactory<>("scanNumber"));
    rtColumn.setCellValueFactory(new PropertyValueFactory<>("retentionTime"));
    msLevelColumn.setCellValueFactory(new PropertyValueFactory<>("msLevel"));
    basePeakColumn.setCellValueFactory(new PropertyValueFactory<>("basePeak"));
    basePeakIntensityColumn.setCellValueFactory(new PropertyValueFactory<>("basePeakIntensity"));
    precursorMzColumn.setCellValueFactory(new PropertyValueFactory<>("precursorMz"));
    mzRangeColumn.setCellValueFactory(new PropertyValueFactory<>("mzRange"));
    scanTypeColumn.setCellValueFactory(new PropertyValueFactory<>("scanType"));
    polarityColumn.setCellValueFactory(new PropertyValueFactory<>("polarity"));
    definitionColumn.setCellValueFactory(new PropertyValueFactory<>("definition"));
    mobilityColumn.setCellValueFactory(new PropertyValueFactory<>("mobility"));

    scanColumn.setComparator(new StringToDoubleComparator());
    rtColumn.setComparator(new StringToDoubleComparator());
    msLevelColumn.setComparator(new StringToDoubleComparator());
//    basePeakColumn.setComparator(new StringToDoubleComparator());
    basePeakIntensityColumn.setComparator(new StringToDoubleComparator());
    mobilityColumn.setComparator(new StringToDoubleComparator());

    MZmineCore.getTaskController().addTask(new PopulateTask(rawDataFile));
  }

  @Nonnull
  public Range<Integer> getVisibleRange() {
    TableViewSkin<?> skin = (TableViewSkin) rawDataTableView.getSkin();
    if (skin == null) {
      return Range.closed(0, 0);
    }
    VirtualFlow<?> flow = (VirtualFlow) skin.getChildren().get(1);
    int indexFirst;
    int indexLast;
    if (flow != null && flow.getFirstVisibleCell() != null
        && flow.getLastVisibleCell() != null) {
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
   *
   * @return
   */
  protected TableView<ScanDescription> getRawDataTableView() {
    return rawDataTableView;
  }

  private class PopulateTask implements Task {

    private ObservableList<ScanDescription> tableData = FXCollections.observableArrayList();

    private double perc = 0;
    private TaskStatus status;
    private boolean isCanceled;
    private RawDataFile rawDataFile;

    public PopulateTask(RawDataFile rawDataFile) {
      perc = 0;
      status = TaskStatus.PROCESSING;
      isCanceled = false;
      this.rawDataFile = rawDataFile;
    }

    @Override
    public void run() {

      NumberFormat mzFormat = MZminePreferences.mzFormat.getValue();
      NumberFormat rtFormat = MZminePreferences.rtFormat.getValue();
      NumberFormat itFormat = MZminePreferences.intensityFormat.getValue();

      tableData.clear();

      int numberOfScans = rawDataFile.getNumOfScans();

      // add raw data to table
      for (int i = 1; i < numberOfScans + 1; i++) {
        Scan scan = rawDataFile.getScan(i);

        if (scan == null) {
          continue;
        }

        // check for precursor
        String precursor = "";
        if (scan.getPrecursorMZ() == 0.000 || scan.getPrecursorMZ() == -1.000) {
          precursor = "";
        } else {
          precursor = mzFormat.format(scan.getPrecursorMZ());
        }
        String mobility = "";
        mobility = mzFormat.format(scan.getMobility());

        // format mzRange
        String mzRange =
            mzFormat.format(scan.getDataPointMZRange().lowerEndpoint())
                + "-" + mzFormat.format(scan.getDataPointMZRange().upperEndpoint());

        String basePeakMZ = "-";
        String basePeakIntensity = "-";

        if (scan.getHighestDataPoint() != null) {
          basePeakMZ = mzFormat.format(scan.getHighestDataPoint().getMZ());
          basePeakIntensity = itFormat.format(scan.getHighestDataPoint().getIntensity());
        }

        tableData.add(new ScanDescription(Integer.toString(i), // scan number
            rtFormat.format(scan.getRetentionTime()), // rt
            Integer.toString(scan.getMSLevel()), // MS level
            precursor, // precursor mz
            mzRange, // mz range
            scan.getSpectrumType().toString(), // profile/centroid
            scan.getPolarity().toString(), // polarity
            scan.getScanDefinition(),      // definition
            mobility,  // mobility
            basePeakMZ, // base peak mz
            basePeakIntensity) // base peak intensity
        );

        perc = i / (numberOfScans + 1);
        if (isCanceled == true) {
          status = TaskStatus.CANCELED;
          return;
        }
      }

      Platform.runLater(() -> {
        rawDataTableView.setItems(tableData);
//        rawDataTableView.getSelectionModel().select(0);
      });

      status = TaskStatus.FINISHED;
    }

    @Override
    public String getTaskDescription() {
      return "Loading scan info of " + rawDataFile.getName();
    }

    @Override
    public double getFinishedPercentage() {
      return perc;
    }

    @Override
    public TaskStatus getStatus() {
      return status;
    }

    @Override
    public String getErrorMessage() {
      return null;
    }

    @Override
    public TaskPriority getTaskPriority() {
      return TaskPriority.NORMAL;
    }

    @Override
    public void cancel() {
      this.isCanceled = true;
    }

  }
}
