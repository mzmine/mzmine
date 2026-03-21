/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.parameters.parametertypes.selectors;

import io.github.mzmine.modules.io.import_spectral_library.SpectralLibraryImportParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import java.io.File;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import org.jetbrains.annotations.Nullable;

public class SpectralLibrarySelectionComponent extends GridPane {

  private final ComboBox<SpectralLibrarySelectionType> typeCombo;
  private final Button detailsButton;
  private final Label numFilesLabel;
  private final ReadOnlyObjectWrapper<List<SpectralLibrary>> currentlySelected = new ReadOnlyObjectWrapper<>();
  // the latest directly specified files
  private File[] specificFiles;

  public SpectralLibrarySelectionComponent() {

    setHgap(8.0);

    numFilesLabel = new Label();
    add(numFilesLabel, 0, 0);

    detailsButton = new Button("Select");
    add(detailsButton, 2, 0);

    typeCombo = new ComboBox<>(
        FXCollections.observableArrayList(SpectralLibrarySelectionType.values()));
    typeCombo.getSelectionModel().selectFirst();

    detailsButton.disableProperty().bind(typeCombo.getSelectionModel().selectedItemProperty()
        .isEqualTo(SpectralLibrarySelectionType.ALL_IMPORTED));

    typeCombo.getSelectionModel().selectedItemProperty()
        .addListener((options, oldValue, newValue) -> {
          updateNumFiles();
        });

    add(typeCombo, 1, 0);

    detailsButton.setOnAction(e -> {
      final FileNamesParameter fileNamesParameter = SpectralLibraryImportParameters.dataBaseFiles.cloneParameter();
      fileNamesParameter.setValue(specificFiles);
      final SimpleParameterSet paramSet = new SimpleParameterSet(
          new Parameter[]{fileNamesParameter});
      final ExitCode exitCode = paramSet.showSetupDialog(true);
      if (exitCode == ExitCode.OK) {
        specificFiles = paramSet.getValue(fileNamesParameter);
      }
      updateNumFiles();
    });

    setMinWidth(getPrefWidth());

    PauseTransition autoUpdate = new PauseTransition(Duration.seconds(1));
    autoUpdate.setOnFinished(_ -> {
      // only if actually shown on screen
      if (!isVisible() || getScene() == null || getScene().getWindow() == null
          || !getScene().getWindow().isShowing()) {
        return;
      }

      // auto update the number of files in the component to react to changes from As selected in GUI
      // this only changes the component
      updateNumFiles();
      autoUpdate.playFromStart();
    });
    autoUpdate.playFromStart();
  }

  public SpectralLibrarySelection getValue() {
    return new SpectralLibrarySelection(typeCombo.getValue(),
        specificFiles == null ? List.of() : List.of(specificFiles));
  }

  void setValue(@Nullable SpectralLibrarySelection newValue) {
    if (newValue == null) {
      newValue = new SpectralLibrarySelection();
    }
    typeCombo.getSelectionModel().select(newValue.getSelectionType());
    specificFiles = newValue.getSpecificLibraryNames().toArray(File[]::new);
    updateNumFiles();
  }

  public void setToolTipText(String toolTip) {
    typeCombo.setTooltip(new Tooltip(toolTip));
  }

  private void updateNumFiles() {
    final List<SpectralLibrary> matching = getValue().getMatchingLibraries();

    if (currentlySelected.get() == null || !currentlySelected.get().equals(matching)) {
      currentlySelected.set(matching);
    }
    numFilesLabel.setText("" + matching.size());
  }

  /**
   * The currently selected property is auto updated every second
   *
   * @return a property that holds the currently selected elements
   */
  public ReadOnlyObjectProperty<List<SpectralLibrary>> currentlySelectedProperty() {
    return currentlySelected.getReadOnlyProperty();
  }

  /**
   * calls an update of the selection first
   */
  public List<SpectralLibrary> getCurrentlySelected() {
    updateNumFiles();
    return currentlySelected.get();
  }
}
