package io.github.mzmine.modules.visualization.rawdataoverview;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

public class RawDataFileInfoPaneController {

  private RawDataFile rawDataFile;
  private boolean populated = false;

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
    precursorMzColumn.setCellValueFactory(new PropertyValueFactory<>("precursorMz"));
    mzRangeColumn.setCellValueFactory(new PropertyValueFactory<>("mzRange"));
    scanTypeColumn.setCellValueFactory(new PropertyValueFactory<>("scanType"));
    polarityColumn.setCellValueFactory(new PropertyValueFactory<>("polarity"));
    definitionColumn.setCellValueFactory(new PropertyValueFactory<>("definition"));
    mobilityColumn.setCellValueFactory(new PropertyValueFactory<>("mobility"));

    MZmineCore.getTaskController().addTask(new PopulateTask(rawDataFile));
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

      tableData.clear();

      int numberOfScans = rawDataFile.getNumOfScans();

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
        String mobility = "";
        mobility = MZminePreferences.mzFormat.getValue().format(scan.getMobility());

        // format mzRange
        String mzRange =
            MZminePreferences.mzFormat.getValue()
                .format(scan.getDataPointMZRange().lowerEndpoint())
                + "-" + MZminePreferences.mzFormat.getValue()
                .format(scan.getDataPointMZRange().upperEndpoint());

        tableData.add(new ScanDescription(Integer.toString(i), // scan number
            MZminePreferences.rtFormat.getValue().format(scan.getRetentionTime()), // rt
            Integer.toString(scan.getMSLevel()), // MS level
            precursor, // precursor mz
            mzRange, // mz range
            scan.getSpectrumType().toString(), // profile/centroid
            scan.getPolarity().toString(), // polarity
            scan.getScanDefinition(),      // definition
            mobility) // mobility
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
