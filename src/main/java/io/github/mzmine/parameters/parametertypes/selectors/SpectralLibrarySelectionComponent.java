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
