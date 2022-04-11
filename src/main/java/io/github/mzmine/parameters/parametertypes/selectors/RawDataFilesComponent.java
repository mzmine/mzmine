/*
 * Copyright 2006-2021 The MZmine Development Team
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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.MultiChoiceParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.util.ExitCode;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;

public class RawDataFilesComponent extends GridPane {

  private final ComboBox<RawDataFilesSelectionType> typeCombo;
  private final Button detailsButton;
  private final Label numFilesLabel;
  private RawDataFilesSelection currentValue = new RawDataFilesSelection();

  public RawDataFilesComponent() {

    setHgap(8.0);

    numFilesLabel = new Label();
    add(numFilesLabel, 0, 0);

    detailsButton = new Button("Select");
    add(detailsButton, 2, 0);

    typeCombo =
        new ComboBox<>(FXCollections.observableArrayList(RawDataFilesSelectionType.values()));
    typeCombo.getSelectionModel().selectFirst();

    typeCombo.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
      currentValue.setSelectionType(newValue);
      detailsButton.setDisable(newValue != RawDataFilesSelectionType.NAME_PATTERN
          && newValue != RawDataFilesSelectionType.SPECIFIC_FILES);
      updateNumFiles();
    });

    add(typeCombo, 1, 0);


    detailsButton.setOnAction(e -> {
      RawDataFilesSelectionType type = typeCombo.getSelectionModel().getSelectedItem();

      if (type == RawDataFilesSelectionType.SPECIFIC_FILES) {
        final MultiChoiceParameter<RawDataFile> filesParameter =
            new MultiChoiceParameter<RawDataFile>("Select files", "Select files",
                MZmineCore.getProjectManager().getCurrentProject().getDataFiles(),
                currentValue.getSpecificFiles());
        final SimpleParameterSet paramSet =
            new SimpleParameterSet(new Parameter[] {filesParameter});
        final ExitCode exitCode = paramSet.showSetupDialog(true);
        if (exitCode == ExitCode.OK) {
          RawDataFile files[] = paramSet.getParameter(filesParameter).getValue();
          currentValue.setSpecificFiles(files);
        }
      }
      if (type == RawDataFilesSelectionType.NAME_PATTERN) {
        final StringParameter nameParameter = new StringParameter("Name pattern",
            "Set name pattern that may include wildcards (*), e.g. *mouse* matches any name that contains mouse",
            currentValue.getNamePattern());
        final SimpleParameterSet paramSet = new SimpleParameterSet(new Parameter[] {nameParameter});
        final ExitCode exitCode = paramSet.showSetupDialog(true);
        if (exitCode == ExitCode.OK) {
          String namePattern = paramSet.getParameter(nameParameter).getValue();
          currentValue.setNamePattern(namePattern);
        }
      }
      updateNumFiles();
    });


    setMinWidth(getPrefWidth());

  }

  void setValue(RawDataFilesSelection newValue) {
    currentValue = newValue.clone();
    RawDataFilesSelectionType type = newValue.getSelectionType();
    if (type != null)
      typeCombo.getSelectionModel().select(type);
    updateNumFiles();
  }

  public RawDataFilesSelection getValue() {
    RawDataFilesSelection clone = currentValue.clone();
    clone.resetSelection();
    return clone;
  }

  public void setToolTipText(String toolTip) {
    typeCombo.setTooltip(new Tooltip(toolTip));
  }

  private void updateNumFiles() {
    currentValue.resetSelection();
    if (currentValue.getSelectionType() == RawDataFilesSelectionType.BATCH_LAST_FILES) {
      numFilesLabel.setText("");
      numFilesLabel.setTooltip(null);
    } else {
      RawDataFile files[] = currentValue.getMatchingRawDataFiles();
      if (files.length == 1) {
        String fileName = files[0].getName();
        if (fileName.length() > 22)
          fileName = fileName.substring(0, 20) + "...";
        numFilesLabel.setText(fileName);
      } else {
        numFilesLabel.setText(files.length + " selected");
      }
      numFilesLabel.setTooltip(new Tooltip(currentValue.toString()));
    }
  }
}
