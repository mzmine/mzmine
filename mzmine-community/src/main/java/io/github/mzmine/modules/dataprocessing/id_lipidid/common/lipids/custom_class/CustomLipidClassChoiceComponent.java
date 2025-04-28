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
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.internal.AddCustomLipidClassParameters;
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

public class CustomLipidClassChoiceComponent extends BorderPane {

  // Logger.
  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private final ListView<CustomLipidClass> listView = new ListView<>();
  private final FlowPane buttonsPane = new FlowPane(Orientation.HORIZONTAL);
  private final Button addButton = new Button("Add...");
  private final Button importButton = new Button("Import...");
  private final Button exportButton = new Button("Export...");
  private final Button removeButton = new Button("Remove selected");
  private final Button clearButton = new Button("Clear");

  // Filename extension.
  private static final String FILENAME_EXTENSION = "*.json";

  public CustomLipidClassChoiceComponent(CustomLipidClass[] choices) {

    ObservableList<CustomLipidClass> choicesList = FXCollections.observableArrayList(
        Arrays.asList(choices));

    listView.setItems(choicesList);
    setCenter(listView);
    setPrefSize(300, 200);
    setMinWidth(200);
    setMaxHeight(200);
    addButton.setOnAction(e -> {
      final ParameterSet parameters = new AddCustomLipidClassParameters().cloneParameterSet();
      if (parameters.showSetupDialog(true) != ExitCode.OK) {
        return;
      }

      // Create new custom lipid class
      CustomLipidClass customLipidClass = new CustomLipidClass(
          parameters.getValue(AddCustomLipidClassParameters.name),
          parameters.getValue(AddCustomLipidClassParameters.abbr),
          parameters.getValue(AddCustomLipidClassParameters.lipidCategory),
          parameters.getValue(AddCustomLipidClassParameters.lipidMainClass),
          parameters.getValue(AddCustomLipidClassParameters.backBoneFormula),
          parameters.getValue(AddCustomLipidClassParameters.lipidChainTypes),
          parameters.getValue(AddCustomLipidClassParameters.customLipidClassFragmentationRules)
      );

      // Add to list of choices (if not already present).
      if (!listView.getItems().contains(customLipidClass)) {
        listView.getItems().add(customLipidClass);
      }
    });

    importButton.setTooltip(new Tooltip("Import custom lipid class from JSON files"));
    importButton.setOnAction(e -> {

      // Create the chooser if necessary.
      FileChooser chooser = new FileChooser();
      chooser.setTitle("Select custom lipid class files");
      chooser.getExtensionFilters().add(new ExtensionFilter("JSON", FILENAME_EXTENSION));

      // Select multiple files.
      List<File> files = chooser.showOpenMultipleDialog(this.getScene().getWindow());
      if (files == null || files.isEmpty()) {
        return;
      }
      try {
        Gson gson = new Gson();
        for (File file : files) {
          FileReader fileReader = new FileReader(file);
          List<CustomLipidClass> customLipidClasses = gson.fromJson(fileReader,
              new TypeToken<List<CustomLipidClass>>() {
              }.getType());
          for (CustomLipidClass customLipidClass : customLipidClasses) {
            if (customLipidClass != null) {
              listView.getItems().add(customLipidClass);
            }
          }
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
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        gson.toJson(listView.getItems(), fileWriter);
        fileWriter.close();
      } catch (IOException ex) {
        final String msg = "There was a problem writing the Custom Lipid Class file.";
        MZmineCore.getDesktop().displayErrorMessage(msg + "\n(" + ex.getMessage() + ')');
        logger.log(Level.SEVERE, msg, ex);
      }

    });

    removeButton.setTooltip(new Tooltip("Remove selected custom Lipid Classes"));
    removeButton.setOnAction(e -> {
      ObservableList<CustomLipidClass> selectedItems = listView.getSelectionModel()
          .getSelectedItems();
      listView.getItems().removeAll(selectedItems);
    });

    clearButton.setTooltip(new Tooltip("Remove all custom Lipid Classes"));
    clearButton.setOnAction(e -> {
      listView.getItems().clear();
    });

    buttonsPane.getChildren()
        .addAll(addButton, importButton, exportButton, removeButton, clearButton);
    setTop(buttonsPane);

    listView.setOnMouseClicked(event -> {
      if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
        CustomLipidClass selectedCustomLipidClass = listView.getSelectionModel().getSelectedItem();

        final ParameterSet parameters = new AddCustomLipidClassParameters().cloneParameterSet();
        parameters.setParameter(AddCustomLipidClassParameters.name,
            selectedCustomLipidClass.getName());
        parameters.setParameter(AddCustomLipidClassParameters.abbr,
            selectedCustomLipidClass.getAbbr());
        parameters.setParameter(AddCustomLipidClassParameters.lipidCategory,
            selectedCustomLipidClass.getCoreClass());
        parameters.setParameter(AddCustomLipidClassParameters.lipidMainClass,
            selectedCustomLipidClass.getMainClass());
        parameters.setParameter(AddCustomLipidClassParameters.backBoneFormula,
            selectedCustomLipidClass.getBackBoneFormula());
        parameters.setParameter(AddCustomLipidClassParameters.lipidChainTypes,
            selectedCustomLipidClass.getChainTypes());
        parameters.setParameter(AddCustomLipidClassParameters.customLipidClassFragmentationRules,
            selectedCustomLipidClass.getFragmentationRules());
        if (parameters.showSetupDialog(true) != ExitCode.OK) {
          return;
        }

        //remove old custom lipid class
        listView.getItems().remove(selectedCustomLipidClass);

        // Create new custom fragmentation rule
        CustomLipidClass customLipidClass = new CustomLipidClass(
            parameters.getValue(AddCustomLipidClassParameters.name),
            parameters.getValue(AddCustomLipidClassParameters.abbr),
            parameters.getValue(AddCustomLipidClassParameters.lipidCategory),
            parameters.getValue(AddCustomLipidClassParameters.lipidMainClass),
            parameters.getValue(AddCustomLipidClassParameters.backBoneFormula),
            parameters.getValue(AddCustomLipidClassParameters.lipidChainTypes),
            parameters.getValue(
                AddCustomLipidClassParameters.customLipidClassFragmentationRules));
        int selectedIndex = listView.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
          listView.getItems()
              .add(listView.getSelectionModel().getSelectedIndex(), customLipidClass);
        } else {
          listView.getItems().add(customLipidClass);
        }

        // Add to list of choices (if not already present).
        if (!listView.getItems().contains(customLipidClass)) {
          listView.getItems().add(customLipidClass);
        }
      }
    });
  }

  void setValue(List<CustomLipidClass> checkedItems) {
    listView.getSelectionModel().clearSelection();
    for (CustomLipidClass mod : checkedItems) {
      listView.getSelectionModel().select(mod);
    }
  }

  public List<CustomLipidClass> getValue() {
    return listView.getItems();
  }

}
