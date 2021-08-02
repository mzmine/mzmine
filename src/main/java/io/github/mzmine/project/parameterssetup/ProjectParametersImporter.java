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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.Desktop;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * This class imports project parameters and their values from a CSV file to the main project
 * parameter setup dialog.
 *
 *
 * Description of input file format:
 *
 * First column of the comma-separated file must contain file names matching to names of raw data
 * files opened in MZmine. Each other column corresponds to one project parameter.
 *
 * First row in the file must contain column headers. Header for the first column (filename) is
 * ignored but must exists. Rest of the column headers are used as names for project parameters. All
 * column names in the file must be be unique. If main dialog already contains a parameter with the
 * same name, a warning is shown to the user before overwriting previous parameter.
 *
 * All parameters are set as of String type.
 *
 */
public class ProjectParametersImporter {

  private Logger logger = Logger.getLogger(this.getClass().getName());
  private final MZmineProject currentProject= MZmineCore.getProjectManager().getCurrentProject();
  private final Desktop desktop = MZmineCore.getDesktop();
  private Stage currentStage;

  public ProjectParametersImporter(Stage stage) {
    this.currentStage = stage;
  }


  public boolean importParameters() {

    // Let user choose a CSV file for importing
    File parameterFile = chooseFile();
    if (parameterFile == null) {
      logger.info("Parameter importing cancelled.");
      return false;
    }

    // Read and interpret parameters
    UserParameter<?, ?>[] parameters = processParameters(parameterFile);

    if (parameters == null)
      return false;


    // Read values of parameters and store them in the project
    return processParameterValues(parameterFile, parameters);

  }

  private Hashtable<UserParameter<?,?>, Boolean> showParameterChooseOption(UserParameter<?,?>[] parameters) {
    Hashtable<UserParameter<?,?>,Boolean> ischeckedparameter = new Hashtable<>();
    for(UserParameter<?,?> parameter:parameters){
      ischeckedparameter.put(parameter,false);
    }
    Stage chooseParaStage = new Stage();
    chooseParaStage.initModality(Modality.APPLICATION_MODAL);
    chooseParaStage.setTitle("Choose Parameters to include in project");
    chooseParaStage.setMinHeight(200);
    chooseParaStage.setMinWidth(200);
    VBox vbox = new VBox();
    Label label = new Label("Parameters");
    label.setStyle("-fx-font-weight: bold");
    vbox.setStyle("-fx-font-size:16px;");
    vbox.getChildren().add(label);
    for(UserParameter<?,?> parameter:parameters){
      CheckBox checkBox = new CheckBox(parameter.getName());
      vbox.getChildren().add(checkBox);
      checkBox.setOnAction(e->{
        if(checkBox.isSelected())
          ischeckedparameter.put(parameter,true);
        else
          ischeckedparameter.put(parameter,false);
      });
    }
    Button button = new Button("OK");
    button.setOnAction(e->chooseParaStage.close());
    vbox.getChildren().add(button);
    vbox.setPadding(new Insets(5, 5, 5, 5));
    Scene scene = new Scene(vbox);
    chooseParaStage.setScene(scene);
    chooseParaStage.showAndWait();
    return ischeckedparameter;
  }

  private File chooseFile() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Please select a file containing project parameter values for files.");
    fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("All Files","*.*"),
            new FileChooser.ExtensionFilter("text","*.txt"),
            new FileChooser.ExtensionFilter("csv","*.csv")
            );

    File currentFile = currentProject.getProjectFile();
    if (currentFile != null) {
      File currentDir = currentFile.getParentFile();
      if ((currentDir != null) && (currentDir.exists()))
        fileChooser.setInitialDirectory(currentDir);
    }
    return fileChooser.showOpenDialog(currentStage.getScene().getWindow());
  }

  private UserParameter<?, ?>[] processParameters(File parameterFile) {
    ArrayList<UserParameter<?, ?>> parameters = new ArrayList<UserParameter<?, ?>>();

    // Open reader
    BufferedReader parameterFileReader;
    try {
      parameterFileReader = new BufferedReader(new FileReader(parameterFile));

      // Read column headers which are used as parameter names
      String firstRow = parameterFileReader.readLine();
      StringTokenizer st = new StringTokenizer(firstRow, ",");
      st.nextToken(); // Assume first column contains file names
      ArrayList<String> parameterNames = new ArrayList<String>();
      Hashtable<String, ArrayList<String>> parameterValues =
              new Hashtable<String, ArrayList<String>>();
      while (st.hasMoreTokens()) {
        String paramName = st.nextToken();
        if (parameterValues.containsKey(paramName)) {
          logger.severe(
                  "Did not import parameters because of a non-unique parameter name: " + paramName);
          assert desktop != null;
          desktop.displayErrorMessage("Could not open file " + parameterFile);
          parameterFileReader.close();
          return null;
        }
        parameterNames.add(paramName);
        parameterValues.put(paramName, new ArrayList<String>());
      }

      // Read rest of the rows which contain file name in the first column
      // and parameter values in the rest of the columns
      String nextRow = parameterFileReader.readLine();
      int rowNumber = 2;
      while (nextRow != null) {
        st = new StringTokenizer(nextRow, ",");

        // Skip first column (File name)
        if (st.hasMoreTokens())
          st.nextToken();

        Iterator<String> parameterNameIterator = parameterNames.iterator();
        while (st.hasMoreTokens()) {

          if (st.hasMoreTokens() ^ parameterNameIterator.hasNext()) {
            logger.severe("Incorrect number of parameter values on row " + rowNumber);
            assert desktop != null;
            desktop.displayErrorMessage("Incorrect number of parameter values on row " + rowNumber);
            parameterFileReader.close();
            return null;
          }
          parameterValues.get(parameterNameIterator.next()).add(st.nextToken());
        }
        nextRow = parameterFileReader.readLine();
        rowNumber++;
      }

//      Add String parameters
      Iterator<String> parameterNameIterator = parameterNames.iterator();
      while (parameterNameIterator.hasNext()) {
        String name = parameterNameIterator.next();
        parameters.add(new StringParameter(name, null));
      }

      // Close reader
      parameterFileReader.close();

    } catch (IOException ex) {
      logger.severe("Could not read file " + parameterFile);
      desktop.displayErrorMessage("Could not open file " + parameterFile);
      return null;
    }

    return parameters.toArray(new UserParameter[0]);

  }

  private boolean processParameterValues(File parameterFile, UserParameter<?, ?>[] parameters) {

    // Warn user if main dialog already contains a parameter with same name
    for (UserParameter<?,?> parameter :parameters){
      if(currentProject.hasParameter(parameter)){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Overwrite previous parameter(s) with same name?");
        Optional<ButtonType> result = alert.showAndWait();
        if(result.isPresent() && result.get() == ButtonType.OK){
          break;
        }
        else{
          return false;
        }
      }
    }

    // Remove parameters with same name
    for (UserParameter<?, ?> parameter:parameters){
      if(currentProject.hasParameter(parameter)){
        currentProject.removeParameter(currentProject.getParameterByName(parameter.getName()));
      }
    }

    // Add new parameters to the main dialog
    for (UserParameter<?, ?> parameter : parameters) {
      currentProject.addParameter(parameter);
    }


    Hashtable<UserParameter<?,?>,Boolean> chosenParameter = showParameterChooseOption(parameters);

    // Open reader
    BufferedReader parameterFileReader;
    try {
      parameterFileReader = new BufferedReader(new FileReader(parameterFile));
    } catch (FileNotFoundException ex) {
      logger.severe("Could not open file " + parameterFile);
      assert desktop != null;
      desktop.displayErrorMessage("Could not open file " + parameterFile);
      return false;
    }

    try {

      // Skip first row
      parameterFileReader.readLine();

      // Read rest of the rows which contain file name in the first column
      // and parameter values in the rest of the columns
      String nextRow = parameterFileReader.readLine();
      while (nextRow != null) {
        StringTokenizer st = new StringTokenizer(nextRow, ",");

        nextRow = parameterFileReader.readLine();

        if (!st.hasMoreTokens())
          continue;

        // Get raw data file for this row
        String fileName = st.nextToken();

        //Check whether fileName named rawData file is present
        RawDataFile[] dataFiles = currentProject.getDataFiles();
        // Find index for data file
        int dataFileIndex = 0;
        while (dataFileIndex < dataFiles.length) {
          if (dataFiles[dataFileIndex].getName().equals(fileName))
            break;
          dataFileIndex++;
        }

        // File not found?
        if (dataFileIndex == dataFiles.length)
          return false;

        RawDataFile dataFile = dataFiles[dataFileIndex];

        // Set parameter values to project
        int parameterIndex = 0;
        while (st.hasMoreTokens()) {
          String parameterValue = st.nextToken();
          UserParameter<?, ?> parameter = parameters[parameterIndex];
          currentProject.setParameterValue(parameter, dataFile, parameterValue);
          parameterIndex++;
        }
        //Removing unselected paramaters
        for(UserParameter<?,?> parameter:parameters){
            if(!chosenParameter.get(parameter)){
              currentProject.removeParameter(parameter);
            }
        }
      }
    } catch (IOException ex) {
      logger.severe("Could not read file " + parameterFile);
      assert desktop != null;
      desktop.displayErrorMessage("Could not open file " + parameterFile);
      return false;
    }

    // Close reader
    try {
      parameterFileReader.close();
    } catch (IOException ex) {
      logger.severe("Could not close file " + parameterFile);
      assert desktop != null;
      desktop.displayErrorMessage("Could not close file " + parameterFile);
      return false;
    }

    return true;
  }
}
