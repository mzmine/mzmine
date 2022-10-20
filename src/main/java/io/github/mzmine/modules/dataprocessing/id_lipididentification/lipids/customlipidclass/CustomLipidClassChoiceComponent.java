/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.customlipidclass;

import com.google.gson.Gson;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidChainType;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.util.ExitCode;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import org.json.JSONArray;
import org.json.JSONObject;

public class CustomLipidClassChoiceComponent extends BorderPane {

  // Logger.
  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private final ListView<CustomLipidClass> checkList = new ListView<>();
  private final FlowPane buttonsPane = new FlowPane(Orientation.VERTICAL);
  private final Button addButton = new Button("Add...");
  private final Button importButton = new Button("Import...");
  private final Button exportButton = new Button("Export...");
  private final Button removeButton = new Button("Remove");

  // Filename extension.
  private static final String FILENAME_EXTENSION = "*.json";

  public CustomLipidClassChoiceComponent(CustomLipidClass[] choices) {

    ObservableList<CustomLipidClass> choicesList = FXCollections.observableArrayList(
        Arrays.asList(choices));

    checkList.setItems(choicesList);
    setCenter(checkList);
    setMaxHeight(100);
    addButton.setOnAction(e -> {
      final ParameterSet parameters = new AddCustomLipidClassParameters();
      if (parameters.showSetupDialog(true) != ExitCode.OK) {
        return;
      }

      // Create new custom lipid class
      CustomLipidClass customLipidClass = new CustomLipidClass(
          parameters.getParameter(AddCustomLipidClassParameters.name).getValue(),
          parameters.getParameter(AddCustomLipidClassParameters.abbr).getValue(),
          parameters.getParameter(AddCustomLipidClassParameters.backBoneFormula).getValue(),
          parameters.getParameter(AddCustomLipidClassParameters.lipidChainTypes).getChoices(),
          parameters.getParameter(AddCustomLipidClassParameters.customLipidClassFragmentationRules)
              .getEmbeddedParameter().getChoices());

      // Add to list of choices (if not already present).
      if (!checkList.getItems().contains(customLipidClass)) {
        checkList.getItems().add(customLipidClass);
      }
    });

    importButton.setTooltip(new Tooltip("Import custom lipid class from a JSON file"));
    importButton.setOnAction(e -> {

      // Create the chooser if necessary.
      FileChooser chooser = new FileChooser();
      chooser.setTitle("Select custom lipid class file");
      chooser.getExtensionFilters().add(new ExtensionFilter("JSON", FILENAME_EXTENSION));

      // Select a file.
      final File file = chooser.showOpenDialog(this.getScene().getWindow());
      if (file == null) {
        return;
      }

      try {
        FileInputStream fileInputStream = new FileInputStream(file);
        JsonReader reader = Json.createReader(fileInputStream);
        JsonArray jsonArray = reader.readArray();
        reader.close();
        Gson gson = new Gson();
        for (int i = 0; i < jsonArray.size(); i++) {
          CustomLipidClass customLipidClass = new CustomLipidClass(//
              jsonArray.get(i).asJsonObject().get("Custom lipid class").asJsonObject()
                  .getString("Name"), //
              jsonArray.get(i).asJsonObject().get("Custom lipid class").asJsonObject()
                  .getString("Abbr"), //
              jsonArray.get(i).asJsonObject().get("Custom lipid class").asJsonObject()
                  .getString("Backbone"), //
              gson.fromJson(jsonArray.get(i).asJsonObject().get("Custom lipid class").asJsonObject()
                  .getJsonArray("Chain types").toString(), LipidChainType[].class), //
              gson.fromJson(jsonArray.get(i).asJsonObject().get("Custom lipid class").asJsonObject()
                  .getJsonArray("Fragmentation rules").toString(), LipidFragmentationRule[].class)
              //
          );
          checkList.getItems().add(customLipidClass);
        }
      } catch (FileNotFoundException ex) {
        logger.log(Level.WARNING, "Could not open Custom Lipid Class .json file");
        ex.printStackTrace();
      }
    });

    exportButton.setTooltip(new Tooltip("Export custom lipid class as JSON file"));
    exportButton.setOnAction(e -> {
      FileChooser chooser = new FileChooser();
      chooser.setTitle("Select lipid modification file");
      chooser.getExtensionFilters().add(new ExtensionFilter("JSON", FILENAME_EXTENSION));

      final File file = chooser.showSaveDialog(this.getScene().getWindow());
      if (file == null) {
        return;
      }

      try {
        FileWriter fileWriter = new FileWriter(file);
        JSONArray customLipidClassesList = new JSONArray();
        for (final CustomLipidClass lipidClass : checkList.getItems()) {
          JSONObject customLipidClassDetails = new JSONObject();
          customLipidClassDetails.put("Name", lipidClass.getName());
          customLipidClassDetails.put("Abbr", lipidClass.getAbbr());
          customLipidClassDetails.put("Backbone", lipidClass.getBackBoneFormula());
          customLipidClassDetails.put("Chain types", lipidClass.getChainTypes());
          customLipidClassDetails.put("Fragmentation rules", lipidClass.getFragmentationRules());
          JSONObject customLipidClass = new JSONObject();
          customLipidClass.put("Custom lipid class", customLipidClassDetails);
          customLipidClassesList.put(customLipidClass);
        }
        fileWriter.write(customLipidClassesList.toString());
        fileWriter.close();
      } catch (IOException ex) {
        final String msg = "There was a problem writing the Custom Lipid Class file.";
        MZmineCore.getDesktop().displayErrorMessage(msg + "\n(" + ex.getMessage() + ')');
        logger.log(Level.SEVERE, msg, ex);
      }

    });

    removeButton.setTooltip(new Tooltip("Remove all Custom Lipid Classes"));
    removeButton.setOnAction(e -> {
      checkList.getItems().clear();
    });

    buttonsPane.getChildren().addAll(addButton, importButton, exportButton, removeButton);
    setRight(buttonsPane);

  }

  void setValue(List<CustomLipidClass> checkedItems) {
    checkList.getSelectionModel().clearSelection();
    for (CustomLipidClass mod : checkedItems) {
      checkList.getSelectionModel().select(mod);
    }
  }

  public List<CustomLipidClass> getChoices() {
    return checkList.getItems();
  }

  public List<CustomLipidClass> getValue() {
    return checkList.getSelectionModel().getSelectedItems();
  }

  /**
   * Represents a custom lipid class.
   */
  public static class AddCustomLipidClassParameters extends SimpleParameterSet {

    public static final OptionalParameter<CustomLipidClassFragmentationRulesChoiceParameters> customLipidClassFragmentationRules = new OptionalParameter<>(
        new CustomLipidClassFragmentationRulesChoiceParameters("Add fragmentation rules",
            "Add custom lipid class fragmentation rules", new LipidFragmentationRule[0]));

    private static final StringParameter abbr = new StringParameter(
        "Custom lipid class abbreviation", "Enter a abbreviation for the custom lipid class");

    private static final StringParameter backBoneFormula = new StringParameter(
        "Lipid Backbone Molecular Formula",
        "Enter the backbone molecular formula of the custom lipid class. Include all elements of the original molecular, e.g. in case of glycerol based  lipid classes add C3H8O3");
    // lipid modification
    private static final StringParameter name = new StringParameter("Custom lipid class name",
        "Enter the name of the custom lipid class");
    private static final CustomLipidChainChoiceParameter lipidChainTypes = new CustomLipidChainChoiceParameter(
        "Add Lipid Chains", "Add Lipid Chains", new LipidChainType[0]);

    public AddCustomLipidClassParameters() {
      super(new Parameter[]{name, abbr, backBoneFormula, lipidChainTypes,
          customLipidClassFragmentationRules});
    }
  }

}
