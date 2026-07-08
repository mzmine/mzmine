/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataprocessing.filter_lipidannotationcleanup.IonizationPreference;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidCategories;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidMainClasses;
import java.util.Arrays;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Small dialog for adding a single {@link IonizationPreference} rule. Presents cascading combo
 * boxes for the lipid hierarchy (Category → Main class → Lipid class) and an ionization type combo
 * whose choices are derived from the selected scope's
 * {@link
 * io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRule}s.
 */
final class IonizationPreferenceAddDialog extends Dialog<IonizationPreference> {

  private final @NotNull ComboBox<LipidCategories> categoryCombo;
  private final @NotNull ComboBox<LipidMainClasses> mainClassCombo;
  private final @NotNull ComboBox<LipidClasses> lipidClassCombo;
  private final @NotNull ComboBox<IonizationType> ionizationCombo;
  private final @NotNull ObservableList<LipidMainClasses> mainClassItems = FXCollections.observableArrayList();
  private final @NotNull ObservableList<LipidClasses> lipidClassItems = FXCollections.observableArrayList();
  private final @NotNull ObservableList<IonizationType> ionizationItems = FXCollections.observableArrayList();
  private final @NotNull List<IonizationPreference> existingPreferences;
  private final @NotNull ButtonType addButtonType = new ButtonType("Add", ButtonData.OK_DONE);

  IonizationPreferenceAddDialog(final @NotNull List<IonizationPreference> existingPreferences) {
    this.existingPreferences = existingPreferences;

    setTitle("Add ionization preference");
    setHeaderText("Select a lipid hierarchy scope and preferred ionization type.");

    getDialogPane().sceneProperty().subscribe(scene -> {
      if (scene != null) {
        ConfigService.getConfiguration().getTheme().apply(scene.getStylesheets());
      }
    });

    categoryCombo = new ComboBox<>(FXCollections.observableArrayList(LipidCategories.values()));
    categoryCombo.setPromptText("Select category");
    categoryCombo.setConverter(new StringConverter<>() {
      @Override
      public String toString(final LipidCategories cat) {
        return cat == null ? "" : cat.getAbbreviation() + " – " + cat.getName();
      }

      @Override
      public LipidCategories fromString(final String s) {
        return null;
      }
    });

    mainClassCombo = new ComboBox<>(mainClassItems);
    mainClassCombo.setPromptText("Any (optional)");
    mainClassCombo.setDisable(true);
    mainClassCombo.setConverter(new StringConverter<>() {
      @Override
      public String toString(final LipidMainClasses mc) {
        return mc == null ? "Any (optional)" : mc.getName();
      }

      @Override
      public LipidMainClasses fromString(final String s) {
        return null;
      }
    });

    lipidClassCombo = new ComboBox<>(lipidClassItems);
    lipidClassCombo.setPromptText("Any (optional)");
    lipidClassCombo.setDisable(true);
    lipidClassCombo.setConverter(new StringConverter<>() {
      @Override
      public String toString(final LipidClasses lc) {
        return lc == null ? "Any (optional)" : lc.getAbbr() + " – " + lc.getName();
      }

      @Override
      public LipidClasses fromString(final String s) {
        return null;
      }
    });

    ionizationCombo = new ComboBox<>(ionizationItems);
    ionizationCombo.setPromptText("Select ionization");
    ionizationCombo.setDisable(true);

    final Label noIonsLabel = FxLabels.newLabel(
        "No ionization shared by all classes in this scope. Select a more specific scope.");
    noIonsLabel.setWrapText(true);
    noIonsLabel.setStyle("-fx-text-fill: -fx-accent;");
    noIonsLabel.visibleProperty().bind(Bindings.isEmpty(ionizationCombo.getItems()));
    noIonsLabel.managedProperty().bind(Bindings.isEmpty(ionizationCombo.getItems()));

    categoryCombo.valueProperty().addListener((_, _, newCat) -> onCategoryChanged(newCat));
    mainClassCombo.valueProperty().addListener((_, _, newMc) -> onMainClassChanged(newMc));
    lipidClassCombo.valueProperty().addListener((_, _, newLc) -> onLipidClassChanged(newLc));
    ionizationCombo.valueProperty().addListener((_, _, _) -> updateAddButtonState());

    final GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(8);
    grid.setPadding(new Insets(10));
    grid.add(FxLabels.newLabel("Category:"), 0, 0);
    grid.add(categoryCombo, 1, 0);
    grid.add(FxLabels.newLabel("Main class:"), 0, 1);
    grid.add(mainClassCombo, 1, 1);
    grid.add(FxLabels.newLabel("Lipid class:"), 0, 2);
    grid.add(lipidClassCombo, 1, 2);
    grid.add(FxLabels.newLabel("Ionization:"), 0, 3);
    grid.add(ionizationCombo, 1, 3);
    grid.add(noIonsLabel, 0, 4, 2, 1);
    categoryCombo.setMaxWidth(Double.MAX_VALUE);
    mainClassCombo.setMaxWidth(Double.MAX_VALUE);
    lipidClassCombo.setMaxWidth(Double.MAX_VALUE);
    ionizationCombo.setMaxWidth(Double.MAX_VALUE);

    getDialogPane().setContent(grid);
    getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
    getDialogPane().setPrefWidth(420);

    updateAddButtonState();

    setResultConverter(buttonType -> {
      if (buttonType == null || buttonType.getButtonData() != ButtonData.OK_DONE) {
        return null;
      }
      final LipidCategories cat = categoryCombo.getValue();
      final IonizationType ion = ionizationCombo.getValue();
      if (cat == null || ion == null) {
        return null;
      }
      return new IonizationPreference(cat, mainClassCombo.getValue(), lipidClassCombo.getValue(),
          ion);
    });
  }

  private void onCategoryChanged(final @Nullable LipidCategories newCat) {
    mainClassCombo.getSelectionModel().clearSelection();
    lipidClassCombo.getSelectionModel().clearSelection();
    ionizationCombo.getSelectionModel().clearSelection();

    if (newCat == null) {
      mainClassItems.clear();
      mainClassCombo.setDisable(true);
      lipidClassItems.clear();
      lipidClassCombo.setDisable(true);
      ionizationItems.clear();
      ionizationCombo.setDisable(true);
    } else {
      mainClassItems.setAll(
          Arrays.stream(LipidMainClasses.values()).filter(mc -> mc.getLipidCategory() == newCat)
              .toList());
      mainClassCombo.setDisable(false);
      lipidClassItems.clear();
      lipidClassCombo.setDisable(true);
      refreshIonizations(newCat, null, null);
      ionizationCombo.setDisable(false);
    }
    updateAddButtonState();
  }

  private void onMainClassChanged(final @Nullable LipidMainClasses newMc) {
    lipidClassCombo.getSelectionModel().clearSelection();
    ionizationCombo.getSelectionModel().clearSelection();

    final LipidCategories cat = categoryCombo.getValue();
    if (cat == null) {
      return;
    }
    if (newMc == null) {
      lipidClassItems.clear();
      lipidClassCombo.setDisable(true);
    } else {
      lipidClassItems.setAll(
          Arrays.stream(LipidClasses.values()).filter(lc -> lc.getMainClass() == newMc).toList());
      lipidClassCombo.setDisable(false);
    }
    refreshIonizations(cat, newMc, null);
    updateAddButtonState();
  }

  private void onLipidClassChanged(final @Nullable LipidClasses newLc) {
    ionizationCombo.getSelectionModel().clearSelection();
    final LipidCategories cat = categoryCombo.getValue();
    if (cat == null) {
      return;
    }
    refreshIonizations(cat, mainClassCombo.getValue(), newLc);
    updateAddButtonState();
  }

  private void refreshIonizations(final @NotNull LipidCategories cat,
      final @Nullable LipidMainClasses mc, final @Nullable LipidClasses lc) {
    final List<IonizationType> ions = IonizationPreference.availableIonizations(cat, mc, lc);
    ionizationItems.setAll(ions);
    if (!ions.isEmpty()) {
      ionizationCombo.getSelectionModel().selectFirst();
    }
  }

  private void updateAddButtonState() {
    final Button addButton = (Button) getDialogPane().lookupButton(addButtonType);
    if (addButton == null) {
      return;
    }
    final LipidCategories cat = categoryCombo.getValue();
    final IonizationType ion = ionizationCombo.getValue();
    if (cat == null || ion == null) {
      addButton.setDisable(true);
      return;
    }
    // prevent duplicate scopes
    final IonizationPreference candidate = new IonizationPreference(cat, mainClassCombo.getValue(),
        lipidClassCombo.getValue(), ion);
    final boolean duplicate = existingPreferences.stream().anyMatch(p -> p.sameScope(candidate));
    addButton.setDisable(duplicate);
  }
}
