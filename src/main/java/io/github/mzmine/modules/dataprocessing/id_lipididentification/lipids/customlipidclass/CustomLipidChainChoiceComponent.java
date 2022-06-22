/*
 * Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.customlipidclass;

import com.google.gson.Gson;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidChainType;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
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

public class CustomLipidChainChoiceComponent extends BorderPane {

  // Logger.
  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private final ListView<LipidChainType> checkList = new ListView<>();
  private final FlowPane buttonsPane = new FlowPane(Orientation.VERTICAL);
  private final Button addButton = new Button("Add...");
  private final Button importButton = new Button("Import...");
  private final Button exportButton = new Button("Export...");
  private final Button removeButton = new Button("Remove");

  // Filename extension.
  private static final String FILENAME_EXTENSION = "*.json";

  public CustomLipidChainChoiceComponent(LipidChainType[] choices) {

    ObservableList<LipidChainType> choicesList = FXCollections.observableArrayList(
        Arrays.asList(choices));
    checkList.setItems(choicesList);
    setCenter(checkList);
    setMaxHeight(100);
    checkList.setMinWidth(300);
    addButton.setOnAction(e -> {
      final ParameterSet parameters = new AddLipidChainTypeParameters();
      if (parameters.showSetupDialog(true) != ExitCode.OK) {
        return;
      }

      // Create new custom lipid class
      LipidChainType lipidChainType = parameters.getParameter(
          AddLipidChainTypeParameters.lipidChainType).getValue();

      // Add to list of choices
      checkList.getItems().add(lipidChainType);
    });

    importButton.setTooltip(new Tooltip("Import Chains for Custom Lipid Class from a CSV file"));
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
          LipidChainType lipidChainType = gson.fromJson(
              jsonArray.get(i).asJsonObject().getString("Lipid Chain Type"), LipidChainType.class);
          checkList.getItems().add(lipidChainType);
        }
      } catch (FileNotFoundException ex) {
        logger.log(Level.WARNING, "Could not open Lipid Chain .json file");
        ex.printStackTrace();
      }

    });

    exportButton.setTooltip(new Tooltip("Export Lipid Chains to a JSON file"));
    exportButton.setOnAction(e -> {
      FileChooser chooser = new FileChooser();
      chooser.setTitle("Select Lipid Chain file");
      chooser.getExtensionFilters().add(new ExtensionFilter("JSON", FILENAME_EXTENSION));

      final File file = chooser.showSaveDialog(this.getScene().getWindow());
      if (file == null) {
        return;
      }

      try {
        FileWriter fileWriter = new FileWriter(file);
        JSONArray chainTypeList = new JSONArray();
        for (final LipidChainType chainType : checkList.getItems()) {
          JSONObject chainTypeJson = new JSONObject();
          chainTypeJson.put("Lipid Chain Type", chainType);
          chainTypeList.put(chainTypeJson);
        }
        fileWriter.write(chainTypeList.toString());
        fileWriter.close();
      } catch (IOException ex) {
        final String msg = "There was a problem writing the Custom Lipid Class file.";
        MZmineCore.getDesktop().displayErrorMessage(msg + "\n(" + ex.getMessage() + ')');
        logger.log(Level.SEVERE, msg, ex);
      }
    });

    removeButton.setTooltip(new Tooltip("Remove all Lipid Chains"));
    removeButton.setOnAction(e -> {
      checkList.getItems().clear();
    });

    buttonsPane.getChildren().addAll(addButton, importButton, exportButton, removeButton);
    setRight(buttonsPane);

  }

  void setValue(List<LipidChainType> checkedItems) {
    checkList.getSelectionModel().clearSelection();
    for (LipidChainType chain : checkedItems) {
      checkList.getSelectionModel().select(chain);
    }
  }

  public List<LipidChainType> getChoices() {
    return checkList.getItems();
  }

  public List<LipidChainType> getValue() {
    return checkList.getSelectionModel().getSelectedItems();
  }

  /**
   * Represents a fragmentation rule of a custom lipid class.
   */
  private static class AddLipidChainTypeParameters extends SimpleParameterSet {

    private static final ComboParameter<LipidChainType> lipidChainType = new ComboParameter<>(
        "Select lipid chain type", "Select lipid chain type",
        new LipidChainType[]{LipidChainType.ACYL_CHAIN, LipidChainType.ALKYL_CHAIN});

    private AddLipidChainTypeParameters() {
      super(new Parameter[]{lipidChainType});
    }
  }
}
