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
