/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.rawdataoverview;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.datamodel.msms.IonMobilityMsMsInfo;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.datamodel.msms.PasefMsMsInfo;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.javafx.components.factories.TableColumns;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.javafx.MZmineIconUtils;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import org.kordamp.ikonli.javafx.FontIcon;

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
  private TableColumn<Scan, String> precursorMzColumn;

  @FXML
  private TableColumn<Scan, String> fragMethodColumn;

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
  private TableColumn<Scan, FontIcon> massDetectionColumn;

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
    NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
    NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
    NumberFormat itFormat = MZmineCore.getConfiguration().getIntensityFormat();

    scanColumn.setCellValueFactory(p -> new SimpleObjectProperty<>(p.getValue().getScanNumber()));
    rtColumn.setCellValueFactory(p -> new SimpleObjectProperty<>(p.getValue().getRetentionTime()));
    msLevelColumn.setCellValueFactory(p -> new SimpleObjectProperty<>(p.getValue().getMSLevel()));
    basePeakColumn.setCellValueFactory(
        p -> new SimpleObjectProperty<>(p.getValue().getBasePeakMz()));
    basePeakIntensityColumn.setCellValueFactory(
        p -> new SimpleObjectProperty<>(p.getValue().getBasePeakIntensity()));
    precursorMzColumn.setCellValueFactory(
        p -> new SimpleStringProperty(getPrecursorString(p.getValue(), mzFormat)));
    fragMethodColumn.setCellValueFactory(
        p -> new SimpleStringProperty(getFragmentationMethod(p.getValue())));
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
    massDetectionColumn.setCellValueFactory(
        p -> p.getValue().getMassList() != null ? new SimpleObjectProperty<>(
            MZmineIconUtils.getCheckedIcon())
            : new SimpleObjectProperty<>(MZmineIconUtils.getUncheckedIcon()));

    TableColumns.setFormattedCellFactory(basePeakColumn, mzFormat);
    TableColumns.setFormattedCellFactory(basePeakIntensityColumn, itFormat);
    TableColumns.setFormattedCellFactory(rtColumn, rtFormat);
    TableColumns.setFormattedCellFactory(injectTimeColumn, rtFormat);
    TableColumns.setFormattedRangeCellFactory(mzRangeColumn, mzFormat);

    TableColumns.autoFitLastColumn(rawDataTableView);
  }

  private String getFragmentationMethod(final Scan scan) {
    final MsMsInfo info = scan.getMsMsInfo();
    if (info == null) {
      return "";
    }

    // If unknown just dont show the activation method. Makes table look cleaner
    return Stream.of(info.getActivationMethod(), info.getActivationEnergy())
        .filter(Objects::nonNull).filter(o -> o != ActivationMethod.UNKNOWN).map(Objects::toString)
        .collect(Collectors.joining(", "));
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

  private String getPrecursorString(Scan scan, NumberFormat mzFormat) {
    if (scan == null) {
      return null;
    }

    if (scan instanceof Frame f && !f.getImsMsMsInfos().isEmpty()) {
      // IMS Frames may have multiple precursor m/zs in acquisition modes such as PASEF.
      return String.join("; ", exractInfosFromFrame(f.getImsMsMsInfos(), mzFormat));
    }
    final Double precursorMz = scan.getPrecursorMz();
    return precursorMz != null ? mzFormat.format(scan.getPrecursorMz()) : null;
  }

  private static List<String> exractInfosFromFrame(Collection<IonMobilityMsMsInfo> infos,
      NumberFormat mzFormat) {
    return infos.stream().map(info -> {
      return switch (info) {
        case PasefMsMsInfo i -> mzFormat.format(i.getIsolationMz());
        case IonMobilityMsMsInfo i ->
            mzFormat.format(Objects.requireNonNullElse(i.getIsolationWindow().lowerEndpoint(), -1))
            + "-" + mzFormat.format(
                Objects.requireNonNullElse(i.getIsolationWindow().upperEndpoint(), -1));
      };
    }).toList();
  }
}
