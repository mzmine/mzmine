/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.io.spectraldbsubmit.row;

import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import java.io.File;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Dialog for selecting a target spectral library. Allows the user to either add entries to an
 * existing project library or create a new one.
 */
public class SpectralLibrarySelectionDialog extends Dialog<SpectralLibrary> {

  private static final String ADD_EXISTING = "Add to existing library";
  private static final String CREATE_NEW = "Create new library";

  private final RadioButton addExistingRadio = new RadioButton(ADD_EXISTING);
  private final RadioButton createNewRadio = new RadioButton(CREATE_NEW);
  private final ComboBox<SpectralLibrary> libraryCombo = new ComboBox<>();
  private final TextField nameField = new TextField();

  public SpectralLibrarySelectionDialog(int nRows, List<SpectralLibrary> lastLibraries,
      @NotNull final String suggestedName) {
    setTitle("Sending %d rows to Spectral Library".formatted(nRows));
    setHeaderText("Select a target spectral library for the generated entries.");

    final ToggleGroup toggleGroup = new ToggleGroup();
    addExistingRadio.setToggleGroup(toggleGroup);
    createNewRadio.setToggleGroup(toggleGroup);

    nameField.setPromptText("Library name");
    nameField.setText(suggestedName);

    // populate combo with current project libraries
    final List<SpectralLibrary> currentLibraries = ProjectService.getProjectManager()
        .getCurrentProject().getCurrentSpectralLibraries();
    libraryCombo.getItems().setAll(currentLibraries);
    libraryCombo.setConverter(new StringConverter<>() {
      @Override
      public String toString(@Nullable SpectralLibrary lib) {
        // null if no library loaded
        return lib == null ? null : lib.getNameWithSize();
      }

      @Override
      public SpectralLibrary fromString(final String string) {
        return null;
      }
    });

    // decision: disable "add to existing" if no libraries are imported
    final boolean hasExistingLibraries = !currentLibraries.isEmpty();
    addExistingRadio.setDisable(!hasExistingLibraries);
    if (hasExistingLibraries) {
      addExistingRadio.setSelected(true);

      // try select last selected library
      if (!lastLibraries.isEmpty()) {
        try {
          libraryCombo.getSelectionModel().select(lastLibraries.getFirst());
        } catch (Exception e) {
          libraryCombo.getSelectionModel().selectFirst();
        }
      } else {
        libraryCombo.getSelectionModel().selectFirst();
      }
    } else {
      createNewRadio.setSelected(true);
    }

    // update control visibility based on selection
    updateControlState(toggleGroup.getSelectedToggle());
    toggleGroup.selectedToggleProperty()
        .addListener((obs, oldToggle, newToggle) -> updateControlState(newToggle));

    final GridPane content = new GridPane();
    content.setHgap(10);
    content.setVgap(8);
    content.setPadding(new Insets(10));

    content.add(addExistingRadio, 0, 0);
    content.add(libraryCombo, 1, 0);
    content.add(createNewRadio, 0, 1);
    content.add(FxLabels.newLabel("Name:"), 0, 2);
    content.add(nameField, 1, 2);

    getDialogPane().setContent(content);
    getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

    // disable OK when state is invalid
    final var okButton = getDialogPane().lookupButton(ButtonType.OK);
    okButton.disableProperty().bind(
        addExistingRadio.selectedProperty().and(libraryCombo.valueProperty().isNull())
            .or(createNewRadio.selectedProperty().and(nameField.textProperty().isEmpty())));

    setResultConverter(buttonType -> {
      if (buttonType != ButtonType.OK) {
        return null;
      }
      if (addExistingRadio.isSelected()) {
        return libraryCombo.getValue();
      }

      final String name = nameField.getText().strip();
      final List<SpectralLibrary> libraries = ProjectService.getProject()
          .getCurrentSpectralLibraries();
      if (libraries.stream().anyMatch(lib -> lib.getName().equals(name))) {
        final boolean overwrite = DialogLoggerUtil.showDialogYesNo(AlertType.WARNING,
            "Overwriting existing library?",
            "Do you really want to overwrite the existing library, replacing all its content with the new library entries?");
        if (!overwrite) {
          return null;
        }
      }
      // create a new library with a placeholder file path
      return new SpectralLibrary(MemoryMapStorage.forMassList(), name, new File(name + ".json"));
    });
  }

  private void updateControlState(@NotNull final Toggle selectedToggle) {
    final boolean addExisting = selectedToggle == addExistingRadio;
    libraryCombo.setDisable(!addExisting);
    nameField.setDisable(addExisting);
  }
}
