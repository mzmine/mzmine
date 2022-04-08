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


package io.github.mzmine.project.parameterssetup;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.helpwindow.HelpWindow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Hashtable;
import java.util.Optional;
import java.util.logging.Logger;

public class ProjectParametersSetupDialogController {

  private Logger logger = Logger.getLogger(this.getClass().getName());
  private final MZmineProject currentProject = MZmineCore.getProjectManager().getCurrentProject();
//  private final Hashtable<UserParameter<?, ?>, Hashtable<RawDataFile, Object>> initialParameters = //
//      (Hashtable<UserParameter<?, ?>, Hashtable<RawDataFile, Object>>)//
//          currentProject.getProjectParametersAndValues().clone();
  private final MetadataTable initialMetadata = currentProject.getProjectMetadata();
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
//      currentProject.setProjectParametersAndValues(initialParameters);
      logger.info("Parameters are not updated");
    });
  }

  private void updateParametersToTable() {
    parameterTable.getItems().clear();
    parameterTable.getColumns().clear();
    int columnsize = currentProject.getParameters().length;
    if (columnsize == 0) {
      return;
    }
    TableColumn[] tableColumns = new TableColumn[columnsize + 1];
    tableColumns[0] = createColumn(0, "Raw Data File");
    UserParameter<?, ?>[] parameterList = currentProject.getParameters();
    for (int i = 0; i < columnsize; ++i) {
      tableColumns[i + 1] = createColumn(i + 1, parameterList[i].getName());
    }
    parameterTable.getColumns().addAll(tableColumns);

    ObservableList<ObservableList<StringProperty>> paramValue = FXCollections.observableArrayList();
    for (RawDataFile rawFile : fileList) {
      ObservableList<StringProperty> fileParametersValue = FXCollections.observableArrayList();
      fileParametersValue.add(new SimpleStringProperty(rawFile.getName()));
      for (UserParameter<?, ?> parameter : parameterList) {
        fileParametersValue.add(new SimpleStringProperty(
            (String) currentProject.getParameterValue(parameter, rawFile)));
      }
      paramValue.add(fileParametersValue);
    }

    parameterTable.getItems().addAll(paramValue);
  }

  private TableColumn<ObservableList<StringProperty>, String> createColumn(final int columnIndex,
      String columnTitle) {
    TableColumn<ObservableList<StringProperty>, String> column = new TableColumn<>();
    String title;
    if (columnTitle == null || columnTitle.trim().length() == 0) {
      title = "Column " + (columnIndex + 1);
    } else {
      title = columnTitle;
    }
    column.setText(title);
    column.setCellValueFactory(cellDataFeatures -> {
      ObservableList<StringProperty> values = cellDataFeatures.getValue();
      if (columnIndex >= values.size()) {
        return new SimpleStringProperty("");
      } else {
        return cellDataFeatures.getValue().get(columnIndex);
      }
    });
    if (columnIndex != 0) {
      column.setCellFactory(TextFieldTableCell.forTableColumn());
      column.setOnEditCommit(event -> {
//                        String oldParaVal = event.getOldValue();
        String newParaVal = event.getNewValue();
        String parameterName = event.getTableColumn().getText();
        UserParameter<?, ?> parameter = currentProject.getParameterByName(parameterName);
        int rowNo = parameterTable.getSelectionModel().selectedIndexProperty().get();
        String fileName = parameterTable.getItems().get(rowNo).get(0).getValue();
        RawDataFile rawDataFile = null;
        for (RawDataFile file : fileList) {
          if (file.getName().equals(fileName)) {
            rawDataFile = file;
            break;
          }
        }
        currentProject.setParameterValue(parameter, rawDataFile, newParaVal);
        updateParametersToTable();
      });

    }
    column.setMinWidth(175.0);
    return column;
  }

  @FXML
  public void addPara(ActionEvent actionEvent) {
    Stage addParaStage = new Stage();
    addParaStage.initModality(Modality.APPLICATION_MODAL);
    addParaStage.setTitle("Add New Parameter");
    addParaStage.setMinHeight(100);
    addParaStage.setMinWidth(80);
    VBox vBox = new VBox();
    HBox hBox1 = new HBox();
    Label label1 = new Label("Parameter Name");
    label1.setPrefWidth(150);
    TextField paraField = new TextField();
    paraField.setPromptText("Enter Parameter Name");
    HBox hBox2 = new HBox();
    Label label2 = new Label("Description");
    label2.setPrefWidth(150);
    TextField descriptionField = new TextField();
    descriptionField.setPromptText("Enter Description");
    hBox1.getChildren().addAll(label1, paraField);
    hBox2.getChildren().addAll(label2, descriptionField);
    Button okButton = new Button("OK");
    Button cancelButton = new Button("Cancel");
    ButtonBar buttonBar = new ButtonBar();
    buttonBar.getButtons().addAll(okButton, cancelButton);
    okButton.setOnAction(e -> {
      String parameterName = paraField.getText();
      String description = paraField.getText();
      if (parameterName.equals("")) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Parameter cannot be left blank");
        alert.setHeaderText(null);
        alert.setContentText("Please enter some parameter name.");
        alert.showAndWait();
        return;
      }
      if (currentProject.getParameterByName(parameterName) != null) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Parameter already present");
        alert.setHeaderText(null);
        alert.setContentText("Please enter unique parameter name.");
        alert.showAndWait();
        addParaStage.close();
        return;
      }

      // todo here you will define the column type

      UserParameter<?, ?> newParameter = new StringParameter(parameterName, description);
      currentProject.addParameter(newParameter);
      for (RawDataFile file : fileList) {
        currentProject.setParameterValue(newParameter, file, "");
      }
      updateParametersToTable();
      addParaStage.close();
    });
    cancelButton.setOnAction(e -> {
      addParaStage.close();
    });
    addParaStage.setOnCloseRequest(e -> {
      String parameterName = paraField.getText();
      String description = paraField.getText();
      if (parameterName.equals("")) {
        return;
      }
      if (currentProject.getParameterByName(parameterName) != null) {
        return;
      }
      UserParameter<?, ?> newParameter = new StringParameter(parameterName, description);
      currentProject.addParameter(newParameter);
      for (RawDataFile file : fileList) {
        currentProject.setParameterValue(newParameter, file, "");
      }
      updateParametersToTable();
    });
    vBox.getChildren().addAll(hBox1, hBox2, buttonBar);
    vBox.setPadding(new Insets(5, 5, 5, 5));
    Scene scene = new Scene(vBox);
    addParaStage.setScene(scene);
    addParaStage.showAndWait();
  }

  @FXML
  public void importPara(ActionEvent actionEvent) {
    ProjectParametersImporter importer = new ProjectParametersImporter(currentStage);
    if (importer.importParameters()) {
      logger.info("Successfully imported parameters from file");
      updateParametersToTable();
    } else {
      logger.info("Importing parameters from file failed");
    }
  }

  @FXML
  public void removePara(ActionEvent actionEvent) {
    TableColumn column = parameterTable.getFocusModel().getFocusedCell().getTableColumn();
    if (column == null) {
      Alert alert = new Alert(Alert.AlertType.INFORMATION);
      alert.setTitle("No cell selected");
      alert.setHeaderText(null);
      alert.setContentText("Please select atleast one cell.");
      alert.showAndWait();
      return;
    }
    String parameterName = column.getText();
    if (parameterName.equals("Raw Data File")) {
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
      UserParameter<?, ?> tbdParameter = currentProject.getParameterByName(
          parameterName);//ToBeDeletedParameter
      if (tbdParameter != null) {
        currentProject.removeParameter(tbdParameter);
      }
      updateParametersToTable();
    }
  }

  @FXML
  public void onClickOK(ActionEvent actionEvent) {
    currentStage.close();
  }

  @FXML
  public void onClickHelp(ActionEvent actionEvent) {
    final URL helpPage = this.getClass().getResource("ParametersSetupHelp.html");
    HelpWindow helpWindow = new HelpWindow(helpPage.toString());
    helpWindow.show();
  }

}
