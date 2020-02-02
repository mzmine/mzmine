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

package io.github.mzmine.project.parameterssetup;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.ExitCode;
import javafx.collections.transformation.SortedList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import javafx.util.Callback;

public class ProjectParametersSetupDialog extends Stage {

  private final Scene mainScene;
  private final BorderPane mainPanel;
  private final BorderPane panelParameterValues;
  private final TableView<RawDataFile> tableParameterValues;
  private final FlowPane panelRemoveParameterButton;
  private final Button buttonAddParameter;
  private final Button buttonRemoveParameter;
  private final Button buttonImportParameters;
  private final FlowPane panelOKCancelButtons;
  private final Button buttonOK;
  private final Button buttonCancel;

  private ExitCode exitCode = ExitCode.UNKNOWN;

  public ProjectParametersSetupDialog() {

    setTitle("Setup project parameters and values");

    panelParameterValues = new BorderPane();

    final MZmineProject project = MZmineCore.getProjectManager().getCurrentProject();

    // The SortedList is necessary to detach the table sorting from the underlying observablelist
    SortedList<RawDataFile> sortedFiles = new SortedList<>(project.getRawDataFiles());
    tableParameterValues = new TableView<RawDataFile>(sortedFiles);
    // bind the sortedList comparator to the TableView comparator
    sortedFiles.comparatorProperty().bind(tableParameterValues.comparatorProperty());
    tableParameterValues.setEditable(true);
    tableParameterValues.getSelectionModel().setCellSelectionEnabled(true);

    TableColumn<RawDataFile, String> sampleCol = new TableColumn<>("Raw data file");
    sampleCol.setCellValueFactory(new PropertyValueFactory<RawDataFile, String>("name"));
    tableParameterValues.getColumns().add(sampleCol);

    // tableParameterValues.setRowSelectionAllowed(false);
    panelRemoveParameterButton = new FlowPane();
    buttonAddParameter = new Button("Add new parameter");
    buttonAddParameter.setOnAction(e -> {
      TableColumn<RawDataFile, String> paramCol = new TableColumn<>("New parameter");
      paramCol.setCellFactory(col -> {
        Callback<TableColumn<RawDataFile, String>, TableCell<RawDataFile, String>> defCell =
            TextFieldTableCell.<RawDataFile>forTableColumn();
        TableCell<RawDataFile, String> cell = defCell.call(col);
        cell.setEditable(true);
        return cell;
      });

      tableParameterValues.getColumns().add(paramCol);
    });
    buttonImportParameters = new Button("Import parameters and values...");
    buttonImportParameters.setOnAction(e -> {
      // Import parameter values from a file
      ProjectParametersImporter importer = new ProjectParametersImporter(this);
      importer.importParameters();
    });
    buttonRemoveParameter = new Button("Remove selected parameter");
    buttonRemoveParameter.setOnAction(e -> {
      for (TablePosition<?, ?> p : tableParameterValues.getSelectionModel().getSelectedCells()) {
        if (p.getColumn() < 1)
          continue;
        TableColumn<?, ?> col = p.getTableColumn();
        tableParameterValues.getColumns().remove(col);
      }
      // int selectedColumn = tableParameterValues.getSelectedColumn();
      // UserParameter<?, ?> parameter = tablemodelParameterValues.getParameter(selectedColumn);
      // if (parameter == null) {
      // desktop.displayErrorMessage("Select a parameter column from the table first.");
      // return;
      // }
      // removeParameter(parameter);
    });

    panelRemoveParameterButton.getChildren().add(buttonAddParameter);
    panelRemoveParameterButton.getChildren().add(buttonImportParameters);
    panelRemoveParameterButton.getChildren().add(buttonRemoveParameter);

    panelParameterValues.setCenter(tableParameterValues);
    panelParameterValues.setBottom(panelRemoveParameterButton);
    // panelParameterValues.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    panelOKCancelButtons = new FlowPane();

    buttonOK = new Button("OK");
    buttonOK.setOnAction(e -> {
      // Validate parameter values
      // if (!validateParameterValues())
      // return;

      // Copy parameter values to raw data files
      // copyParameterValuesToRawDataFiles();

      exitCode = ExitCode.OK;
      hide();
    });
    buttonCancel = new Button("Cancel");
    buttonCancel.setOnAction(e -> {
      exitCode = ExitCode.CANCEL;
      hide();
    });
    panelOKCancelButtons.getChildren().addAll(buttonOK, buttonCancel);

    mainPanel = new BorderPane();
    mainPanel.setCenter(panelParameterValues);
    mainPanel.setBottom(panelOKCancelButtons);

    mainScene = new Scene(mainPanel);
    setScene(mainScene);

    // copyParameterValuesFromRawDataFiles();

    // setLocationRelativeTo(null);

  }

  public ExitCode getExitCode() {
    return exitCode;
  }



  /*
   * private boolean validateParameterValues() { // Create new parameters and set values for (int
   * columnIndex = 0; columnIndex < parameterValues.keySet().size(); columnIndex++) {
   * UserParameter<?, ?> parameter = tablemodelParameterValues.getParameter(columnIndex + 1);
   *
   * if (parameter instanceof DoubleParameter) {
   *
   * for (int dataFileIndex = 0; dataFileIndex < dataFiles.length; dataFileIndex++) { Object
   * objValue = tablemodelParameterValues.getValueAt(dataFileIndex, columnIndex + 1); if (objValue
   * instanceof String) { try { Double.parseDouble((String) objValue); } catch
   * (NumberFormatException ex) { MZmineCore.getDesktop().displayErrorMessage( "Incorrect value (" +
   * objValue + ") for parameter " + parameter.getName() + " in data file " +
   * dataFiles[dataFileIndex].getName() + "."); return false; } } } }
   *
   * }
   *
   * return true;
   *
   * }
   *
   * private void copyParameterValuesToRawDataFiles() {
   *
   * MZmineProject currentProject = MZmineCore.getProjectManager().getCurrentProject();
   *
   * // Remove all previous parameters from project UserParameter<?, ?>[] parameters =
   * currentProject.getParameters(); for (UserParameter<?, ?> parameter : parameters) {
   * currentProject.removeParameter(parameter); }
   *
   * // Add new parameters parameters = parameterValues.keySet().toArray(new UserParameter[0]); for
   * (UserParameter<?, ?> parameter : parameters) { currentProject.addParameter(parameter); }
   *
   * // Set values for new parameters for (int columnIndex = 0; columnIndex <
   * parameterValues.keySet().size(); columnIndex++) { UserParameter<?, ?> parameter =
   * tablemodelParameterValues.getParameter(columnIndex + 1);
   *
   * for (int dataFileIndex = 0; dataFileIndex < dataFiles.length; dataFileIndex++) { RawDataFile
   * file = dataFiles[dataFileIndex];
   *
   * Object value = tablemodelParameterValues.getValueAt(dataFileIndex, columnIndex + 1); if
   * (parameter instanceof DoubleParameter) { Double doubleValue = null; if (value instanceof
   * Double) doubleValue = (Double) value; if (value instanceof String) doubleValue =
   * Double.parseDouble((String) value); currentProject.setParameterValue(parameter, file,
   * doubleValue); } if (parameter instanceof StringParameter) { if (value == null) value = "";
   * currentProject.setParameterValue(parameter, file, value); } if (parameter instanceof
   * ComboParameter) { if (value == null) value = ""; currentProject.setParameterValue(parameter,
   * file, value); }
   *
   * }
   *
   * }
   *
   * }
   *
   * private void copyParameterValuesFromRawDataFiles() {
   *
   * MZmineProject currentProject = MZmineCore.getProjectManager().getCurrentProject();
   *
   * for (int dataFileIndex = 0; dataFileIndex < dataFiles.length; dataFileIndex++) {
   *
   * RawDataFile file = dataFiles[dataFileIndex]; UserParameter<?, ?>[] parameters =
   * currentProject.getParameters();
   *
   * // Loop through all parameters defined for this file for (UserParameter<?, ?> p : parameters) {
   *
   * // Check if this parameter has been seen before? Object[] values; if
   * (!(parameterValues.containsKey(p))) { // No, initialize a new array of values for this
   * parameter values = new Object[dataFiles.length]; for (int i = 0; i < values.length; i++)
   * values[i] = p.getValue(); parameterValues.put(p, values); } else { values =
   * parameterValues.get(p); }
   *
   * // Set value of this parameter for the current raw data file values[dataFileIndex] =
   * currentProject.getParameterValue(p, file);
   *
   * }
   *
   * }
   *
   * }
   */

}
