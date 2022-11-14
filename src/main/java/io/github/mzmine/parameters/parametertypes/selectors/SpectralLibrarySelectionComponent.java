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

package io.github.mzmine.parameters.parametertypes.selectors;

import io.github.mzmine.modules.io.import_spectral_library.SpectralLibraryImportParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesParameter;
import io.github.mzmine.util.ExitCode;
import java.io.File;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;

public class SpectralLibrarySelectionComponent extends GridPane {

  private final ComboBox<SpectralLibrarySelectionType> typeCombo;
  private final Button detailsButton;
  private final Label numFilesLabel;
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

    typeCombo.getSelectionModel().selectedItemProperty()
        .addListener((options, oldValue, newValue) -> {
          detailsButton.setDisable(newValue == SpectralLibrarySelectionType.ALL_IMPORTED);
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
  }

  public SpectralLibrarySelection getValue() {
    return new SpectralLibrarySelection(typeCombo.getValue(),
        specificFiles == null ? List.of() : List.of(specificFiles));
  }

  void setValue(SpectralLibrarySelection newValue) {
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
    numFilesLabel.setText("" + getValue().getMatchingLibraries().size());
  }
}
