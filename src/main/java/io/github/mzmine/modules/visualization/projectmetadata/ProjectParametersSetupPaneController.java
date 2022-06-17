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


package io.github.mzmine.modules.visualization.projectmetadata;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.helpwindow.HelpWindow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.TextParameter;
import io.github.mzmine.modules.visualization.projectmetadata.ProjectMetadataParameters.AvailableTypes;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.DoubleMetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.DateMetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.StringMetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.util.ExitCode;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Optional;
import java.util.logging.Logger;

public class ProjectParametersSetupPaneController {

  private Logger logger = Logger.getLogger(this.getClass().getName());
  private final MZmineProject currentProject = MZmineCore.getProjectManager().getCurrentProject();
  private final MetadataTable metadataTable = currentProject.getProjectMetadata();
  private Stage currentStage;
  private RawDataFile[] fileList;

  @FXML
  private TableView<ObservableList<StringProperty>> parameterTable;

  @FXML
  private void initialize() {
    parameterTable.setEditable(true);
    parameterTable.getSelectionModel().setCellSelectionEnabled(true);
    fileList = currentProject.getDataFiles();
    updateParametersToTable();
  }

  public void setStage(Stage stage) {
    currentStage = stage;
    stage.setOnCloseRequest(we -> {
      logger.info("Parameters are not updated");
    });
  }

  /**
   * Render the table using the data from the project parameters structure.
   */
  private void updateParametersToTable() {
    parameterTable.getItems().clear();
    parameterTable.getColumns().clear();

    int columnsNumber = metadataTable.getColumns().size();
    if (columnsNumber == 0) {
      return;
    }

    // display the columns
    TableColumn[] tableColumns = new TableColumn[columnsNumber + 1];
    tableColumns[0] = createColumn(0, "Data File", "These are the names of the RawDataFiles");
    var columns = metadataTable.getColumns();
    int columnId = 1;
    for (var col : columns) {
      tableColumns[columnId] = createColumn(columnId, col.getTitle(), col.getDescription());
      columnId++;
    }
    parameterTable.getColumns().addAll(tableColumns);

    // display each row of the table
    ObservableList<ObservableList<StringProperty>> tableRows = FXCollections.observableArrayList();
    for (RawDataFile rawFile : fileList) {
      ObservableList<StringProperty> fileParametersValue = FXCollections.observableArrayList();
      fileParametersValue.add(new SimpleStringProperty(rawFile.getName()));
      for (MetadataColumn<?> column : columns) {
        // either convert parameter value to string or display an empty string in case if it's unset
        Object value = metadataTable.getValue(column, rawFile);
        fileParametersValue.add(new SimpleStringProperty(value == null ? "" : value.toString()));
      }
      tableRows.add(fileParametersValue);
    }
    parameterTable.getItems().addAll(tableRows);
  }

  private TableColumn<ObservableList<StringProperty>, String> createColumn(final int columnIndex,
      String columnTitle, String columnDescription) {
    // validate the column title (assign the default value in case if it's empty)
    TableColumn<ObservableList<StringProperty>, String> column = new TableColumn<>();
    String title;
    if (columnTitle == null || columnTitle.trim().length() == 0) {
      title = "Column " + (columnIndex + 1);
    } else {
      title = columnTitle;
    }

    // add the tooltips
    Label descriptionLabel = new Label(title);
    descriptionLabel.setTooltip(new Tooltip(columnDescription));
    descriptionLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    column.setGraphic(descriptionLabel);

    // define what the cell value would be
    column.setCellValueFactory(cellDataFeatures -> {
      ObservableList<StringProperty> values = cellDataFeatures.getValue();
      if (columnIndex >= values.size()) {
        return new SimpleStringProperty("");
      } else {
        return cellDataFeatures.getValue().get(columnIndex);
      }
    });

    // won't be applied for the first column, because it contains the file name
    if (columnIndex != 0) {
      column.setCellFactory(TextFieldTableCell.forTableColumn());
      column.setOnEditCommit(event -> {
        String parameterValueNew = event.getNewValue();
        // this complication in extracting the value is caused by using labels as the cells values
        String parameterName = ((Label) event.getTableColumn().getGraphic()).getText();
        MetadataColumn parameter = metadataTable.getColumnByName(parameterName);

        // define RawDataFile name
        int rowNumber = parameterTable.getSelectionModel().selectedIndexProperty().get();
        String fileName = parameterTable.getItems().get(rowNumber).get(0).getValue();
        RawDataFile rawDataFile = null;
        for (RawDataFile file : fileList) {
          if (file.getName().equals(fileName)) {
            rawDataFile = file;
            break;
          }
        }

        // if the parameter value is in the right format then save it to the metadata table,
        // otherwise show alert dialog
        Object convertedParameterInput = parameter.convert(parameterValueNew,
            parameter.defaultValue());
        // the first check allows us to unset an already set parameter's value
        if ((convertedParameterInput == null && parameterValueNew.isBlank())
            || parameter.checkInput(convertedParameterInput)) {
          metadataTable.setValue(parameter, rawDataFile, convertedParameterInput);
        } else {
          Alert alert = new Alert(Alert.AlertType.INFORMATION);
          alert.setTitle("Wrong parameter value format");
          alert.setHeaderText(null);
          alert.setContentText(
              "Please respect the " + parameter.getType() + " parameter value format, e.g. "
                  + parameter.exampleValue());
          alert.showAndWait();
        }
        // need to render
        updateParametersToTable();
      });
    }

    column.setMinWidth(175.0);

    return column;
  }

  @FXML
  public void addParameter(ActionEvent actionEvent) {
    ProjectMetadataParameters projectMetadataParameters = new ProjectMetadataParameters();
    ExitCode exitCode = projectMetadataParameters.showSetupDialog(true);

    StringParameter parameterTitle = projectMetadataParameters.getParameter(
        ProjectMetadataParameters.title);
    TextParameter parameterDescription = projectMetadataParameters.getParameter(
        ProjectMetadataParameters.description);
    ComboParameter<AvailableTypes> parameterType = projectMetadataParameters.getParameter(
        ProjectMetadataParameters.valueType);

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
      switch (parameterType.getValue()) {
        case TEXT -> {
          metadataTable.addColumn(
              new StringMetadataColumn(parameterTitle.getValue(), parameterDescriptionVal));
        }
        case DOUBLE -> {
          metadataTable.addColumn(
              new DoubleMetadataColumn(parameterTitle.getValue(), parameterDescriptionVal));
        }
        case DATETIME -> {
          metadataTable.addColumn(
              new DateMetadataColumn(parameterTitle.getValue(), parameterDescriptionVal));
        }
      }
      // need to render
      updateParametersToTable();
    }
  }

  @FXML
  public void importParameters(ActionEvent actionEvent) {
    ProjectParametersImporter importer = new ProjectParametersImporter(currentStage);
    if (importer.importParameters()) {
      logger.info("Successfully imported parameters from file");
      updateParametersToTable();
    } else {
      logger.info("Importing parameters from file failed");
    }
  }

  @FXML
  public void exportParameters(ActionEvent actionEvent) {
    ProjectParametersExporter exporter = new ProjectParametersExporter(currentStage);
    if (exporter.exportParameters()) {
      logger.info("Successfully exported parameters");
      updateParametersToTable();
    } else {
      logger.info("Exporting parameters to file failed");
    }
  }

  @FXML
  public void removeParameters(ActionEvent actionEvent) {
    TableColumn column = parameterTable.getFocusModel().getFocusedCell().getTableColumn();
    if (column == null) {
      Alert alert = new Alert(Alert.AlertType.INFORMATION);
      alert.setTitle("No cell selected");
      alert.setHeaderText(null);
      alert.setContentText("Please select at least one cell.");
      alert.showAndWait();
      return;
    }
    String parameterName = ((Label) column.getGraphic()).getText();
    if (parameterName.equals("Data File")) {
      Alert alert = new Alert(Alert.AlertType.INFORMATION);
      alert.setTitle("Cannot remove Raw Data File Column");
      alert.setHeaderText(null);
      alert.setContentText("Please select cell from another column.");
      alert.showAndWait();
      return;
    }
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Remove parameter " + parameterName);
    alert.setTitle("Remove Parameter?");
    alert.setHeaderText(null);
    Optional<ButtonType> result = alert.showAndWait();
    if (result.isPresent() && result.get() == ButtonType.OK) {
      MetadataColumn<?> tbdParameter = metadataTable.getColumnByName(
          parameterName);//ToBeDeletedParameter
      if (tbdParameter != null) {
        metadataTable.removeColumn(tbdParameter);
      }
      updateParametersToTable();
    }
  }

  @FXML
  public void onClickHelp(ActionEvent actionEvent) {
    final URL helpPage = this.getClass().getResource("ParametersSetupHelp.html");
    HelpWindow helpWindow = new HelpWindow(helpPage.toString());
    helpWindow.show();
  }
}
