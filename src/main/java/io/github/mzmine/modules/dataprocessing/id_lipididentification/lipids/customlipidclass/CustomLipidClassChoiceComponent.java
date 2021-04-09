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

package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.customlipidclass;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonReader;

import org.controlsfx.control.CheckListView;
import org.json.JSONArray;
import org.json.JSONObject;

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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class CustomLipidClassChoiceComponent extends BorderPane {

	// Logger.
	private final Logger logger = Logger.getLogger(this.getClass().getName());

	private final CheckListView<CustomLipidClass> checkList = new CheckListView<CustomLipidClass>();
	private final FlowPane buttonsPane = new FlowPane(Orientation.VERTICAL);
	private final Button addButton = new Button("Add...");
	private final Button importButton = new Button("Import...");
	private final Button exportButton = new Button("Export...");
	private final Button removeButton = new Button("Remove");

	// Filename extension.
	private static final String FILENAME_EXTENSION = "*.json";

	public CustomLipidClassChoiceComponent(CustomLipidClass[] choices) {

		ObservableList<CustomLipidClass> choicesList = FXCollections.observableArrayList(Arrays.asList(choices));

		checkList.setItems(choicesList);
		setCenter(checkList);
		setMaxHeight(100);
		addButton.setOnAction(e -> {
			final ParameterSet parameters = new AddCustomLipidClassParameters();
			if (parameters.showSetupDialog(true) != ExitCode.OK)
				return;

			// Create new custom lipid class
			CustomLipidClass customLipidClass = new CustomLipidClass(
					parameters.getParameter(AddCustomLipidClassParameters.name).getValue(),
					parameters.getParameter(AddCustomLipidClassParameters.abbr).getValue(),
					parameters.getParameter(AddCustomLipidClassParameters.backBoneFormula).getValue(),
					parameters.getParameter(AddCustomLipidClassParameters.lipidChainTypes)
							.getChoices(),
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
			if (file == null)
				return;

			try {
			FileInputStream fileInputStream = new FileInputStream(file);
	            JsonReader reader = Json.createReader(fileInputStream);
				JsonArray jsonArray = reader.readArray();
	            reader.close();
	            Gson gson = new Gson();
				for (int i = 0; i < jsonArray.size(); i++) {
	            CustomLipidClass customLipidClass = new CustomLipidClass(//
						jsonArray.get(i).asJsonObject().get("Custom lipid class").asJsonObject().getString(
								"Name"), //
						jsonArray.get(i).asJsonObject().get("Custom lipid class").asJsonObject().getString("Abbr"), //
						jsonArray.get(i).asJsonObject().get("Custom lipid class").asJsonObject().getString(
								"Backbone"), //
						gson.fromJson(jsonArray.get(i).asJsonObject().get("Custom lipid class").asJsonObject()
								.getJsonArray("Chain types").toString(),
								LipidChainType[].class), //
						gson.fromJson(jsonArray.get(i).asJsonObject().get("Custom lipid class").asJsonObject()
								.getJsonArray("Fragmentation rules").toString(),
								LipidFragmentationRule[].class) //
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
			if (file == null)
				return;

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
		for (CustomLipidClass mod : checkedItems)
			checkList.getSelectionModel().select(mod);
	}

	public List<CustomLipidClass> getChoices() {
		return checkList.getCheckModel().getCheckedItems();
	}

	public List<CustomLipidClass> getValue() {
		return checkList.getSelectionModel().getSelectedItems();
	}

	/**
	 * Represents a custom lipid class.
	 */
	public static class AddCustomLipidClassParameters extends SimpleParameterSet {

		// lipid modification
		private static final StringParameter name = new StringParameter("Custom lipid class name",
				"Enter the name of the custom lipid class");

		private static final StringParameter abbr = new StringParameter("Custom lipid class abbreviation",
				"Enter a abbreviation for the custom lipid class");

		private static final StringParameter backBoneFormula = new StringParameter("Lipid Backbone Molecular Formula",
				"Enter the backbone molecular formula of the custom lipid class. Include all elements of the original molecular, e.g. in case of glycerol based  lipid classes add C3H8O3");

		private static final CustomLipidChainChoiceParameter lipidChainTypes = new CustomLipidChainChoiceParameter(
				"Add Lipid Chains", "Add Lipid Chains", new LipidChainType[0]);

		public static final OptionalParameter<CustomLipidClassFragmentationRulesChoiceParameters> customLipidClassFragmentationRules = new OptionalParameter<CustomLipidClassFragmentationRulesChoiceParameters>(
				new CustomLipidClassFragmentationRulesChoiceParameters(
						"Add fragmentation rules",
						"Add custom lipid class fragmentation rules", new LipidFragmentationRule[0]));

		public AddCustomLipidClassParameters() {
			super(new Parameter[] { name, abbr, backBoneFormula, lipidChainTypes, customLipidClassFragmentationRules });
	    }
	}

}
