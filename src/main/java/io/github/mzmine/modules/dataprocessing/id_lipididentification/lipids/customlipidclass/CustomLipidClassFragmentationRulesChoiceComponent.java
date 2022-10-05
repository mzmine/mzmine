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
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidAnnotationLevel;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
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

public class CustomLipidClassFragmentationRulesChoiceComponent extends BorderPane {

  // Logger.
  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private final ListView<LipidFragmentationRule> checkList = new ListView<>();
  private final FlowPane buttonsPane = new FlowPane(Orientation.VERTICAL);
  private final Button addButton = new Button("Add...");
  private final Button importButton = new Button("Import...");
  private final Button exportButton = new Button("Export...");
  private final Button removeButton = new Button("Remove");

  // Filename extension.
  private static final String FILENAME_EXTENSION = "*.csv";

  public CustomLipidClassFragmentationRulesChoiceComponent(LipidFragmentationRule[] choices) {

    ObservableList<LipidFragmentationRule> choicesList = FXCollections.observableArrayList(
        Arrays.asList(choices));

    checkList.setItems(choicesList);
    setCenter(checkList);
    setMaxHeight(100);
    checkList.setMinWidth(300);
    addButton.setOnAction(e -> {
      final ParameterSet parameters = new AddLipidFragmentationRuleParameters();
      if (parameters.showSetupDialog(true) != ExitCode.OK) {
        return;
      }

      // Create new custom lipid class
      LipidFragmentationRule lipidFragmentationRule = new LipidFragmentationRule(//
          parameters.getParameter(AddLipidFragmentationRuleParameters.polarity).getValue(),
          // polarity
          parameters.getParameter(AddLipidFragmentationRuleParameters.ionizationMethod).getValue(),
          // ionization
          parameters.getParameter(AddLipidFragmentationRuleParameters.lipidFragmentationRuleType)
              .getValue(), // rule type
          parameters.getParameter(
                  AddLipidFragmentationRuleParameters.lipidFragmentationRuleInformationLevel) // information
              // level
              .getValue(),
          parameters.getParameter(AddLipidFragmentationRuleParameters.formula).getValue() // formula
      );

      // Add to list of choices (if not already present).
      if (!checkList.getItems().contains(lipidFragmentationRule)) {
        checkList.getItems().add(lipidFragmentationRule);
      }
    });

    importButton.setTooltip(new Tooltip("Import custom lipid class from a CSV file"));
    importButton.setOnAction(e -> {

      // Create the chooser if necessary.
      FileChooser chooser = new FileChooser();
      chooser.setTitle("Select Lipid Chain JSON File");
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
          LipidFragmentationRule rule = gson.fromJson(
              jsonArray.get(i).asJsonObject().getString("Lipid Fragmentation Rule"),
              LipidFragmentationRule.class);
          checkList.getItems().add(rule);
        }
      } catch (FileNotFoundException ex) {
        logger.log(Level.WARNING, "Could not open Custom Lipid Fragmentation Rule .json file");
        ex.printStackTrace();
      }

    });

    exportButton.setTooltip(new Tooltip("Export Lipid Fragmentatio Rules to a JSON file"));
    exportButton.setOnAction(e -> {
      FileChooser chooser = new FileChooser();
      chooser.setTitle("Select Lipid Fragmentation Rules file");
      chooser.getExtensionFilters().add(new ExtensionFilter("JSON", FILENAME_EXTENSION));

      final File file = chooser.showSaveDialog(this.getScene().getWindow());
      if (file == null) {
        return;
      }

      try {
        FileWriter fileWriter = new FileWriter(file);
        JSONArray chainTypeList = new JSONArray();
        for (final LipidFragmentationRule rule : checkList.getItems()) {
          JSONObject ruleJson = new JSONObject();
          ruleJson.put("Lipid Fragmentation Rule", rule);
          chainTypeList.put(ruleJson);
        }
        fileWriter.write(chainTypeList.toString());
        fileWriter.close();
      } catch (IOException ex) {
        final String msg = "There was a problem writing the Lipid Fragmentation Rule file.";
        MZmineCore.getDesktop().displayErrorMessage(msg + "\n(" + ex.getMessage() + ')');
        logger.log(Level.SEVERE, msg, ex);
      }

    });

    removeButton.setTooltip(new Tooltip("Remove all Lipid Fragmentation Rules"));
    removeButton.setOnAction(e -> {
      checkList.getItems().clear();
    });

    buttonsPane.getChildren().addAll(addButton, importButton, exportButton, removeButton);
    setRight(buttonsPane);

  }

  void setValue(List<LipidFragmentationRule> checkedItems) {
    checkList.getSelectionModel().clearSelection();
    for (LipidFragmentationRule mod : checkedItems) {
      checkList.getSelectionModel().select(mod);
    }
  }

  public List<LipidFragmentationRule> getChoices() {
    return checkList.getItems();
  }

  public List<LipidFragmentationRule> getValue() {
    return checkList.getSelectionModel().getSelectedItems();
  }

  /**
   * Represents a fragmentation rule of a custom lipid class.
   */
  private static class AddLipidFragmentationRuleParameters extends SimpleParameterSet {

    public static final ComboParameter<IonizationType> ionizationMethod = new ComboParameter<>(
        "Ionization method", "Type of ion used to calculate the ionized mass",
        IonizationType.values());
    public static final ComboParameter<LipidFragmentationRuleType> lipidFragmentationRuleType = new ComboParameter<>(
        "Lipid fragmentation rule type", "Choose the type of the lipid fragmentation rule",
        LipidFragmentationRuleType.values());
    public static final ComboParameter<LipidAnnotationLevel> lipidFragmentationRuleInformationLevel = new ComboParameter<>(
        "Lipid fragment information level",
        "Choose the information value of the lipid fragment, molecular formula level, or chain composition level",
        LipidAnnotationLevel.values());
    private static final ComboParameter<PolarityType> polarity = new ComboParameter<>("Polarity",
        "Select polarity type", new PolarityType[]{PolarityType.POSITIVE, PolarityType.NEGATIVE});

    private static final StringParameter formula = new StringParameter("Molecular formula",
        "Enter a molecular formula, if it is involved in the fragmentation rule. E.g. a head group fragment needs to be specified by its molecular formula.",
        null, false, false);

    private AddLipidFragmentationRuleParameters() {
      super(new Parameter[]{polarity, ionizationMethod, lipidFragmentationRuleType,
          lipidFragmentationRuleInformationLevel, formula});
    }
  }
}
