/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import static io.github.mzmine.javafx.components.factories.FxTooltips.newTooltip;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidCategories;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidMainClasses;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
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
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class CustomLipidClassChoiceComponent extends BorderPane {

  // Logger.
  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private final ListView<CustomLipidClass> checkList = new ListView<>();
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

    checkList.setItems(choicesList);
    setCenter(checkList);
    setPrefSize(300, 200);
    setMinWidth(200);
    setMaxHeight(200);
    addButton.setOnAction(e -> {
      final ParameterSet parameters = new AddCustomLipidClassParameters();
      if (parameters.showSetupDialog(true) != ExitCode.OK) {
        return;
      }

      // Create new custom lipid class
      CustomLipidClass customLipidClass = new CustomLipidClass(
          parameters.getParameter(AddCustomLipidClassParameters.name).getValue(),
          parameters.getParameter(AddCustomLipidClassParameters.abbr).getValue(),
          parameters.getParameter(AddCustomLipidClassParameters.lipidCategory).getValue(),
          parameters.getParameter(AddCustomLipidClassParameters.lipidMainClass).getValue(),
          parameters.getParameter(AddCustomLipidClassParameters.backBoneFormula).getValue(),
          parameters.getParameter(AddCustomLipidClassParameters.lipidChainTypes).getChoices(),
          parameters.getParameter(AddCustomLipidClassParameters.customLipidClassFragmentationRules)
              .getChoices());

      // Add to list of choices (if not already present).
      if (!checkList.getItems().contains(customLipidClass)) {
        checkList.getItems().add(customLipidClass);
      }
    });

    importButton.setTooltip(newTooltip("Import custom lipid class from JSON files"));
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
              checkList.getItems().add(customLipidClass);
            }
          }
        }

      } catch (FileNotFoundException ex) {
        logger.log(Level.WARNING, "Could not open Custom Lipid Class .json file");
        ex.printStackTrace();
      }
    });

    exportButton.setTooltip(newTooltip("Export custom lipid class as JSON file"));
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
        gson.toJson(checkList.getItems(), fileWriter);
        fileWriter.close();
      } catch (IOException ex) {
        final String msg = "There was a problem writing the Custom Lipid Class file.";
        MZmineCore.getDesktop().displayErrorMessage(msg + "\n(" + ex.getMessage() + ')');
        logger.log(Level.SEVERE, msg, ex);
      }

    });

    removeButton.setTooltip(newTooltip("Remove selected custom Lipid Classes"));
    removeButton.setOnAction(e -> {
      ObservableList<CustomLipidClass> selectedItems = checkList.getSelectionModel()
          .getSelectedItems();
      checkList.getItems().removeAll(selectedItems);
    });

    clearButton.setTooltip(newTooltip("Remove all custom Lipid Classes"));
    clearButton.setOnAction(e -> {
      checkList.getItems().clear();
    });

    buttonsPane.getChildren()
        .addAll(addButton, importButton, exportButton, removeButton, clearButton);
    setTop(buttonsPane);

    checkList.setOnMouseClicked(event -> {
      if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
        CustomLipidClass selectedCustomLipidClass = checkList.getSelectionModel().getSelectedItem();

        final ParameterSet parameters = new AddCustomLipidClassParameters();
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
        checkList.getItems().remove(selectedCustomLipidClass);

        // Create new custom fragmentation rule
        CustomLipidClass customLipidClass = new CustomLipidClass(
            parameters.getParameter(AddCustomLipidClassParameters.name).getValue(),
            parameters.getParameter(AddCustomLipidClassParameters.abbr).getValue(),
            parameters.getParameter(AddCustomLipidClassParameters.lipidCategory).getValue(),
            parameters.getParameter(AddCustomLipidClassParameters.lipidMainClass).getValue(),
            parameters.getParameter(AddCustomLipidClassParameters.backBoneFormula).getValue(),
            parameters.getParameter(AddCustomLipidClassParameters.lipidChainTypes).getChoices(),
            parameters.getParameter(
                AddCustomLipidClassParameters.customLipidClassFragmentationRules).getChoices());
        int selectedIndex = checkList.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
          checkList.getItems()
              .add(checkList.getSelectionModel().getSelectedIndex(), customLipidClass);
        } else {
          checkList.getItems().add(customLipidClass);
        }

        // Add to list of choices (if not already present).
        if (!checkList.getItems().contains(customLipidClass)) {
          checkList.getItems().add(customLipidClass);
        }
      }
    });

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
    return checkList.getItems();
  }

  /**
   * Represents a custom lipid class.
   */
  public static class AddCustomLipidClassParameters extends SimpleParameterSet {


    public static final StringParameter name = new StringParameter("Custom lipid class name",
        "Enter the name of the custom lipid class", "My lipid class", true);
    public static final StringParameter abbr = new StringParameter(
        "Custom lipid class abbreviation", "Enter a abbreviation for the custom lipid class",
        "MyClass", true);
    public static final StringParameter backBoneFormula = new StringParameter(
        "Lipid backbone molecular formula",
        "Enter the backbone molecular formula of the custom lipid class. Include all elements of the original molecular, e.g. in case of glycerol based lipid classes add C3H8O3. "
        + "For fatty acids start with H2O, for ceramides start with C3H8", "C3H8O3", true);
    public static final ComboParameter<LipidMainClasses> lipidMainClass = new ComboParameter<>(
        "Lipid main class", "Enter the name of the custom lipid class", LipidMainClasses.values(),
        LipidMainClasses.PHOSPHATIDYLCHOLINE);
    public static final ComboParameter<LipidCategories> lipidCategory = new ComboParameter<>(
        "Lipid category",
        "The selected lipid category influences the calculation of the lipid class and the available fragmentation rules",
        new LipidCategories[]{LipidCategories.FATTYACYLS, LipidCategories.GLYCEROLIPIDS,
            LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidCategories.SPHINGOLIPIDS,
            LipidCategories.STEROLLIPIDS}, LipidCategories.GLYCEROPHOSPHOLIPIDS);
    public static final CustomLipidChainChoiceParameter lipidChainTypes = new CustomLipidChainChoiceParameter(
        "Add lipid chains", "Add Lipid Chains",
        new LipidChainType[]{LipidChainType.ACYL_CHAIN, LipidChainType.ACYL_CHAIN});
    public static final CustomLipidClassFragmentationRulesChoiceParameters customLipidClassFragmentationRules = new CustomLipidClassFragmentationRulesChoiceParameters(
        "Add fragmentation rules", "Add custom lipid class fragmentation rules",
        new LipidFragmentationRule[]{
            new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN)});

    public AddCustomLipidClassParameters() {
      super(lipidCategory, lipidMainClass, name, abbr, backBoneFormula, lipidChainTypes,
          customLipidClassFragmentationRules);
    }

    @Override
    public ExitCode showSetupDialog(boolean valueCheckRequired) {
      CustomLipidClassSetupDialog dialog = new CustomLipidClassSetupDialog(valueCheckRequired,
          this);
      dialog.showAndWait();

      if (!lipidCategory.getValue().equals(LipidCategories.SPHINGOLIPIDS) && Arrays.stream(
          lipidChainTypes.getChoices()).anyMatch(lipidChainType ->
          lipidChainType.equals(LipidChainType.SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN)
          || lipidChainType.equals(LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN)
          || lipidChainType.equals(LipidChainType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN))) {
        MZmineCore.getDesktop().displayConfirmation("Confirmation",
            "You are using a sphingolipid specific chain for a lipid of the category "
            + lipidCategory.getValue()
            + ". This may result in unexpected behaviour and is not recommended. Please select Sphingolipids as lipid category.");
      }
      if (!lipidCategory.getValue().equals(LipidCategories.SPHINGOLIPIDS) && Arrays.stream(
          customLipidClassFragmentationRules.getChoices()).anyMatch(rule ->
          rule.getLipidFragmentationRuleType()
              .equals(LipidFragmentationRuleType.SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN_FRAGMENT)
          || rule.getLipidFragmentationRuleType()
              .equals(LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_FRAGMENT)
          || rule.getLipidFragmentationRuleType()
              .equals(LipidFragmentationRuleType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_FRAGMENT)
          || rule.getLipidFragmentationRuleType().equals(
              LipidFragmentationRuleType.SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT)
          || rule.getLipidFragmentationRuleType().equals(
              LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT)
          || rule.getLipidFragmentationRuleType().equals(
              LipidFragmentationRuleType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT))) {
        MZmineCore.getDesktop().displayMessage(
            "You are using a sphingolipid specific fragmentation rule for a lipid of the category "
            + lipidCategory.getValue()
            + ". This may result in unexpected behaviour and is not recommended. Please select Sphingolipids as lipid category.");
      }
      return dialog.getExitCode();
    }

  }

}
