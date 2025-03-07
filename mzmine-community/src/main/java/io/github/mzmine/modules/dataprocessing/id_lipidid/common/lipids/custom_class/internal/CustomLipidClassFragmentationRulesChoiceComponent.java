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

package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.internal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.ExitCode;
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

class CustomLipidClassFragmentationRulesChoiceComponent extends BorderPane {

  // Logger.
  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private final ListView<LipidFragmentationRule> listView = new ListView<>();
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

    listView.setItems(choicesList);
    setCenter(listView);
    setPrefSize(300, 200);
    setMinWidth(300);
    setMaxHeight(200);
    listView.setMinWidth(300);
    addButton.setOnAction(e -> {
      final ParameterSet parameters = new AddLipidFragmentationRuleParameters().cloneParameterSet();
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
      if (!listView.getItems().contains(lipidFragmentationRule)) {
        listView.getItems().add(lipidFragmentationRule);
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
            listView.getItems().add(rule);
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
        gson.toJson(listView.getItems(), fileWriter);
        fileWriter.close();
      } catch (IOException ex) {
        final String msg = "There was a problem writing the Lipid Fragmentation Rule file.";
        MZmineCore.getDesktop().displayErrorMessage(msg + "\n(" + ex.getMessage() + ')');
        logger.log(Level.SEVERE, msg, ex);
      }

    });

    removeButton.setTooltip(new Tooltip("Remove selected Custom Lipid Classes"));
    removeButton.setOnAction(e -> {
      ObservableList<LipidFragmentationRule> selectedItems = listView.getSelectionModel()
          .getSelectedItems();
      listView.getItems().removeAll(selectedItems);
    });

    clearButton.setTooltip(new Tooltip("Remove all Lipid Fragmentation Rules"));
    clearButton.setOnAction(e -> {
      listView.getItems().clear();
    });

    buttonsPane.getChildren()
        .addAll(addButton, importButton, exportButton, removeButton, clearButton);
    setTop(buttonsPane);

    // Select rule with double-click
    listView.setOnMouseClicked(event -> {
      if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
        LipidFragmentationRule selectedFragmentationRule = listView.getSelectionModel()
            .getSelectedItem();

        final ParameterSet parameters = new AddLipidFragmentationRuleParameters().cloneParameterSet();
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
        listView.getItems().remove(selectedFragmentationRule);

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

        int selectedIndex = listView.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
          listView.getItems()
              .add(listView.getSelectionModel().getSelectedIndex(), lipidFragmentationRule);
        } else {
          listView.getItems().add(lipidFragmentationRule);
        }

        // Add to list of choices (if not already present).
        if (!listView.getItems().contains(lipidFragmentationRule)) {
          listView.getItems().add(lipidFragmentationRule);
        }
      }
    });
  }

  void setValue(List<LipidFragmentationRule> checkedItems) {
    listView.getItems().clear();
    for (LipidFragmentationRule lipidFragmentationRule : checkedItems) {
      listView.getItems().add(lipidFragmentationRule);
    }
  }

  public List<LipidFragmentationRule> getValue() {
    return listView.getItems();
  }
}
