/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.util.ExitCode;
import io.mzio.mzmine.datamodel.parameters.ParameterSet;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class CustomLipidClassFragmentationRulesChoiceComponent extends BorderPane {

  // Logger.
  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private final ListView<LipidFragmentationRule> checkList = new ListView<>();
  private final FlowPane buttonsPane = new FlowPane(Orientation.HORIZONTAL);
  private final Button addButton = new Button("Add...");
  private final Button importButton = new Button("Import...");
  private final Button exportButton = new Button("Export...");
  private final Button removeButton = new Button("Remove selected");
  private final Button clearButton = new Button("Clear");

  // Filename extension.
  private static final String FILENAME_EXTENSION = "*.json";

  public CustomLipidClassFragmentationRulesChoiceComponent(LipidFragmentationRule[] choices) {

    ObservableList<LipidFragmentationRule> choicesList = FXCollections.observableArrayList(
        Arrays.asList(choices));

    checkList.setItems(choicesList);
    setCenter(checkList);
    setPrefSize(300, 200);
    setMinWidth(300);
    setMaxHeight(200);
    checkList.setMinWidth(300);
    addButton.setOnAction(e -> {
      final ParameterSet parameters = new AddLipidFragmentationRuleParameters();
      if (parameters.showSetupDialog(true) != ExitCode.OK) {
        return;
      }

      // Create new custom fragmentation rule
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

    importButton.setTooltip(new Tooltip("Import custom lipid fragmentation rule from json file"));
    importButton.setOnAction(e -> {

      // Create the chooser if necessary.
      FileChooser chooser = new FileChooser();
      chooser.setTitle("Select lipid fragmentation rule JSON File");
      chooser.getExtensionFilters().add(new ExtensionFilter("JSON", FILENAME_EXTENSION));

      // Select a file.
      final File file = chooser.showOpenDialog(this.getScene().getWindow());
      if (file == null) {
        return;
      }
      try {
        Gson gson = new Gson();
        FileReader fileReader = new FileReader(file);
        List<LipidFragmentationRule> lipidFragmentationRules = gson.fromJson(fileReader,
            new TypeToken<List<LipidFragmentationRule>>() {
            }.getType());
        for (LipidFragmentationRule rule : lipidFragmentationRules) {
          if (rule != null) {
            checkList.getItems().add(rule);
          }
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
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        gson.toJson(checkList.getItems(), fileWriter);
        fileWriter.close();
      } catch (IOException ex) {
        final String msg = "There was a problem writing the Lipid Fragmentation Rule file.";
        MZmineCore.getDesktop().displayErrorMessage(msg + "\n(" + ex.getMessage() + ')');
        logger.log(Level.SEVERE, msg, ex);
      }

    });

    removeButton.setTooltip(new Tooltip("Remove selected Custom Lipid Classes"));
    removeButton.setOnAction(e -> {
      ObservableList<LipidFragmentationRule> selectedItems = checkList.getSelectionModel()
          .getSelectedItems();
      checkList.getItems().removeAll(selectedItems);
    });

    clearButton.setTooltip(new Tooltip("Remove all Lipid Fragmentation Rules"));
    clearButton.setOnAction(e -> {
      checkList.getItems().clear();
    });

    buttonsPane.getChildren()
        .addAll(addButton, importButton, exportButton, removeButton, clearButton);
    setTop(buttonsPane);

    // Select rule with double-click
    checkList.setOnMouseClicked(event -> {
      if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
        LipidFragmentationRule selectedFragmentationRule = checkList.getSelectionModel()
            .getSelectedItem();

        final ParameterSet parameters = new AddLipidFragmentationRuleParameters();
        parameters.setParameter(AddLipidFragmentationRuleParameters.polarity,
            selectedFragmentationRule.getPolarityType());
        parameters.setParameter(AddLipidFragmentationRuleParameters.ionizationMethod,
            selectedFragmentationRule.getIonizationType());
        parameters.setParameter(AddLipidFragmentationRuleParameters.lipidFragmentationRuleType,
            selectedFragmentationRule.getLipidFragmentationRuleType());
        parameters.setParameter(
            AddLipidFragmentationRuleParameters.lipidFragmentationRuleInformationLevel,
            selectedFragmentationRule.getLipidFragmentInformationLevelType());
        parameters.setParameter(AddLipidFragmentationRuleParameters.formula,
            selectedFragmentationRule.getMolecularFormula());
        if (parameters.showSetupDialog(true) != ExitCode.OK) {
          return;
        }
        //remove old rule
        checkList.getItems().remove(selectedFragmentationRule);

        // Create new custom fragmentation rule
        LipidFragmentationRule lipidFragmentationRule = new LipidFragmentationRule(//
            parameters.getParameter(AddLipidFragmentationRuleParameters.polarity).getValue(),
            // polarity
            parameters.getParameter(AddLipidFragmentationRuleParameters.ionizationMethod)
                .getValue(),
            // ionization
            parameters.getParameter(AddLipidFragmentationRuleParameters.lipidFragmentationRuleType)
                .getValue(), // rule type
            parameters.getParameter(
                    AddLipidFragmentationRuleParameters.lipidFragmentationRuleInformationLevel) // information
                // level
                .getValue(),
            parameters.getParameter(AddLipidFragmentationRuleParameters.formula).getValue()
            // formula
        );

        int selectedIndex = checkList.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
          checkList.getItems()
              .add(checkList.getSelectionModel().getSelectedIndex(), lipidFragmentationRule);
        } else {
          checkList.getItems().add(lipidFragmentationRule);
        }

        // Add to list of choices (if not already present).
        if (!checkList.getItems().contains(lipidFragmentationRule)) {
          checkList.getItems().add(lipidFragmentationRule);
        }
      }
    });


  }

  void setValue(List<LipidFragmentationRule> checkedItems) {
    checkList.getItems().clear();
    for (LipidFragmentationRule lipidFragmentationRule : checkedItems) {
      checkList.getItems().add(lipidFragmentationRule);
    }
  }

  public List<LipidFragmentationRule> getChoices() {
    return checkList.getItems();
  }

  public List<LipidFragmentationRule> getValue() {
    return checkList.getItems();
  }

  /**
   * Represents a fragmentation rule of a custom lipid class.
   */
  public static class AddLipidFragmentationRuleParameters extends SimpleParameterSet {

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
    public static final ComboParameter<PolarityType> polarity = new ComboParameter<>("Polarity",
        "Select polarity type", new PolarityType[]{PolarityType.POSITIVE, PolarityType.NEGATIVE});

    public static final StringParameter formula = new StringParameter("Molecular formula",
        "Enter a molecular formula, if it is involved in the fragmentation rule. E.g. a head group fragment needs to be specified by its molecular formula.",
        "", false, false);

    public AddLipidFragmentationRuleParameters() {
      super(polarity, ionizationMethod, lipidFragmentationRuleType,
          lipidFragmentationRuleInformationLevel, formula);
    }

    @Override
    public ExitCode showSetupDialog(boolean valueCheckRequired) {
      AddLipidFragmentationRuleSetupDialog dialog = new AddLipidFragmentationRuleSetupDialog(
          valueCheckRequired, this);
      dialog.showAndWait();
      return dialog.getExitCode();
    }

  }
}
