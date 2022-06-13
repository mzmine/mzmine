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
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.javafx.StringToDoubleComparator;
import io.github.mzmine.util.javafx.TableViewUitls;
import java.text.NumberFormat;
import java.util.Optional;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
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
  private TableView<ScanDescription> rawDataTableView;

  @FXML
  private TableColumn<ScanDescription, String> scanColumn;

  @FXML
  private TableColumn<ScanDescription, String> rtColumn;

  @FXML
  private TableColumn<ScanDescription, String> basePeakColumn;

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
  private TableColumn<ScanDescription, String> injectTimeColumn;

  @FXML
  private TableColumn<ScanDescription, String> definitionColumn;

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
    scanColumn.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().scanNumber()));
    rtColumn.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().retentionTime()));
    msLevelColumn.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().msLevel()));
    basePeakColumn.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().basePeak()));
    basePeakIntensityColumn.setCellValueFactory(
        p -> new SimpleStringProperty(p.getValue().basePeakIntensity()));
    precursorMzColumn.setCellValueFactory(
        p -> new SimpleStringProperty(p.getValue().precursorMz()));
    mzRangeColumn.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().mzRange()));
    scanTypeColumn.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().scanType()));
    polarityColumn.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().polarity()));
    injectTimeColumn.setCellValueFactory(
        p -> new SimpleStringProperty(p.getValue().injectionTime()));
    definitionColumn.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().definition()));
    scanColumn.setComparator(new StringToDoubleComparator());
    rtColumn.setComparator(new StringToDoubleComparator());
    msLevelColumn.setComparator(new StringToDoubleComparator());
    precursorMzColumn.setComparator(new StringToDoubleComparator());
    injectTimeColumn.setComparator(new StringToDoubleComparator());
    basePeakColumn.setComparator(new StringToDoubleComparator());
    basePeakIntensityColumn.setComparator(new StringToDoubleComparator());

    MZmineCore.getTaskController().addTask(new PopulateTask(rawDataFile));
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
  protected TableView<ScanDescription> getRawDataTableView() {
    return rawDataTableView;
  }

  private class PopulateTask implements Task {

    private final ObservableList<ScanDescription> tableData = FXCollections.observableArrayList();
    private final RawDataFile rawDataFile;
    private double perc = 0;
    private TaskStatus status;
    private boolean isCanceled;

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

      final ObservableList<Scan> scanNumbers = rawDataFile.getScans();
      if (scanNumbers.size() > 5E5) {
        status = TaskStatus.FINISHED;
        // it's not the computation that takes long, it's putting the data into the table.
        // This bricks the MZmine window
        logger.info("Number of entries >500 000 for raw data file " + rawDataFile.getName() + " ("
            + rawDataFile.getNumOfScans() + ")");
        logger.info("Will not compute table data.");
        return;
      }

      // add raw data to table
      for (int i = 0; i < scanNumbers.size(); i++) {
        Scan scan = scanNumbers.get(i);
        if (scan == null) {
          continue;
        }

        // check for precursor
        String precursor = "";
        if (scan.getMsMsInfo() != null && scan.getMsMsInfo() instanceof DDAMsMsInfo dda) {
          precursor = mzFormat.format(dda.getIsolationMz());
        } else {
          precursor = "";
        }

        // format mzRange
        Range<Double> mzRange = scan.getDataPointMZRange();

        String mzRangeStr = "";
        if (mzRange != null) {
          mzRangeStr = mzFormat.format(mzRange.lowerEndpoint()) + "-" + mzFormat.format(
              mzRange.upperEndpoint());
        }

        String basePeakMZ = "-";
        String basePeakIntensity = "-";

        if (scan.getBasePeakMz() != null) {
          basePeakMZ = mzFormat.format(scan.getBasePeakMz());
          basePeakIntensity = itFormat.format(scan.getBasePeakIntensity());
        }

        String injectTime = Optional.ofNullable(scan.getInjectionTime()).map(rtFormat::format)
            .orElse("");

        tableData.add(new ScanDescription(scan, Integer.toString(scan.getScanNumber()), // scan
            // number
            rtFormat.format(scan.getRetentionTime()), // rt
            Integer.toString(scan.getMSLevel()), // MS level
            precursor, // precursor mz
            mzRangeStr, // mz range
            scan.getSpectrumType().toString(), // profile/centroid
            scan.getPolarity().toString(), // polarity
            scan.getScanDefinition(), // definition
            basePeakMZ, // base peak mz
            basePeakIntensity, injectTime) // base peak intensity and inject time
        );

        perc = i / (double) (scanNumbers.size() + 1);
        if (isCanceled) {
          status = TaskStatus.CANCELED;
          return;
        }
      }

      Platform.runLater(() -> {
        rawDataTableView.setItems(tableData);

        // rawDataTableView.getSelectionModel().select(0);
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
