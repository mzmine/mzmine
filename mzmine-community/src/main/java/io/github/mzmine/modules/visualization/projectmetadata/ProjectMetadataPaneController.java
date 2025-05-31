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


package io.github.mzmine.modules.visualization.projectmetadata;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.gui.helpwindow.HelpWindow;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.projectmetadata.ProjectMetadataColumnParameters.AvailableTypes;
import io.github.mzmine.modules.visualization.projectmetadata.io.ProjectMetadataExportModule;
import io.github.mzmine.modules.visualization.projectmetadata.io.ProjectMetadataImportModule;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.DateMetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.DoubleMetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.StringMetadataColumn;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.TextParameter;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.ExitCode;
import java.net.URL;
import java.util.Optional;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

public class ProjectMetadataPaneController {

  private static final Logger logger = Logger.getLogger(
      ProjectMetadataPaneController.class.getName());

  private final MZmineProject currentProject = ProjectService.getProjectManager()
      .getCurrentProject();
  private final MetadataTable metadataTable = currentProject.getProjectMetadata();
  private Stage currentStage;

  @FXML
  private TableView<MetadataRow> tableView;
  private MetadataTableModel tableModel;

  @FXML
  private void initialize() {
    tableView.setEditable(true);
    tableView.getSelectionModel().setCellSelectionEnabled(true);

    tableModel = new MetadataTableModel(metadataTable, tableView);
  }

  public void setStage(Stage stage) {
    currentStage = stage;
    stage.setOnCloseRequest(we -> logger.info("Parameters are not updated"));
  }

  @FXML
  public void addParameter(ActionEvent actionEvent) {
    ProjectMetadataColumnParameters projectMetadataColumnParameters = (ProjectMetadataColumnParameters) new ProjectMetadataColumnParameters().cloneParameterSet();
    ExitCode exitCode = projectMetadataColumnParameters.showSetupDialog(true);

    StringParameter parameterTitle = projectMetadataColumnParameters.getParameter(
        ProjectMetadataColumnParameters.title);
    TextParameter parameterDescription = projectMetadataColumnParameters.getParameter(
        ProjectMetadataColumnParameters.description);
    ComboParameter<AvailableTypes> parameterType = projectMetadataColumnParameters.getParameter(
        ProjectMetadataColumnParameters.valueType);

    if (exitCode == ExitCode.OK) {
      // in case if the new parameter is not unique
      if (metadataTable.getColumnByName(parameterTitle.getValue()) != null) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Parameter already present");
        alert.setHeaderText(null);
        alert.setContentText("Please enter unique parameter name.");
        alert.showAndWait();
        return;
      }

      // it's important to replace tabs due to the tsv file format
      String parameterDescriptionVal = parameterDescription.getValue().replace("\t", " ");

      // add the new column to the parameters table
      MetadataColumn<?> col = switch (parameterType.getValue()) {
        case TEXT -> new StringMetadataColumn(parameterTitle.getValue(), parameterDescriptionVal);
        case NUMBER -> new DoubleMetadataColumn(parameterTitle.getValue(), parameterDescriptionVal);
        case DATETIME -> new DateMetadataColumn(parameterTitle.getValue(), parameterDescriptionVal);
      };
      tableModel.createAndAddNewColumn(col);
    }
  }

  @FXML
  public void importParameters(ActionEvent ev) {
    final ExitCode exitCode = MZmineCore.setupAndRunModule(ProjectMetadataImportModule.class,
        () -> {
          logger.info("Successfully imported parameters from file");
          FxThread.runLater(() -> tableModel.createAndSetExistingColumns());
        }, () -> logger.warning("Importing parameters from file failed"));
    if (exitCode == ExitCode.ERROR) {
      logger.warning("Setup of metadata import failed");
    }
  }

  @FXML
  public void exportParameters(ActionEvent ev) {
    MZmineCore.setupAndRunModule(ProjectMetadataExportModule.class);
  }

  @FXML
  public void removeParameters(ActionEvent ev) {
    final var column = tableView.getFocusModel().getFocusedCell().getTableColumn();
    if (column == null) {
      DialogLoggerUtil.showMessageDialog("No cell selected.", "Please select at least one cell");
      return;
    }
    String columnName = column.getText();
    if (columnName.equals(MetadataColumn.FILENAME_HEADER)) {
      DialogLoggerUtil.showErrorDialog("Cannot remove Raw Data File Column",
          "Cannot remove Raw Data File Column");
      return;
    }
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Remove parameter " + columnName);
    alert.setTitle("Remove Parameter?");
    alert.setHeaderText(null);
    Optional<ButtonType> result = alert.showAndWait();
    if (result.isPresent() && result.get() == ButtonType.OK) {
      MetadataColumn<?> columnToDelete = metadataTable.getColumnByName(columnName);
      if (columnToDelete != null) {
        tableModel.removeColumn(columnToDelete);
      }
    }
  }

  @FXML
  public void onClickHelp(ActionEvent ev) {
    final URL helpPage = this.getClass().getResource("ParametersSetupHelp.html");
    if (helpPage != null) {
      HelpWindow helpWindow = new HelpWindow(helpPage.toString());
      helpWindow.show();
    }
  }

  public void reload(final ActionEvent ev) {
    tableModel.createAndSetExistingColumns();
  }
}
