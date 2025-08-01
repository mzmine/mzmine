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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.MultiChoiceParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.ExitCode;
import java.util.List;
import java.util.Objects;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RawDataFilesComponent extends GridPane {

  private final ComboBox<RawDataFilesSelectionType> typeCombo;
  private final Button detailsButton;
  private final Label numFilesLabel;
  private final ReadOnlyObjectWrapper<List<RawDataFile>> currentlySelected = new ReadOnlyObjectWrapper<>();
  private @NotNull RawDataFilesSelection currentValue = new RawDataFilesSelection();

  public RawDataFilesComponent() {

    setHgap(8.0);

    numFilesLabel = new Label();
    add(numFilesLabel, 0, 0);

    detailsButton = new Button("Select");
    add(detailsButton, 2, 0);

    typeCombo = new ComboBox<>(
        FXCollections.observableArrayList(RawDataFilesSelectionType.values()));
    typeCombo.getSelectionModel().selectFirst();

    typeCombo.getSelectionModel().selectedItemProperty()
        .addListener((options, oldValue, newValue) -> {
          currentValue.setSelectionType(newValue);
          detailsButton.setDisable(newValue != RawDataFilesSelectionType.NAME_PATTERN
              && newValue != RawDataFilesSelectionType.SPECIFIC_FILES);
          updateNumFiles();
        });

    add(typeCombo, 1, 0);

    detailsButton.setOnAction(e -> {
      RawDataFilesSelectionType type = typeCombo.getSelectionModel().getSelectedItem();

      if (type == RawDataFilesSelectionType.SPECIFIC_FILES) {
        final MultiChoiceParameter<RawDataFile> filesParameter = new MultiChoiceParameter<RawDataFile>(
            "Select files", "Select files",
            ProjectService.getProjectManager().getCurrentProject().getDataFiles(),
            currentValue.getSpecificFiles());
        final SimpleParameterSet paramSet = new SimpleParameterSet(new Parameter[]{filesParameter});
        final ExitCode exitCode = paramSet.showSetupDialog(true);
        if (exitCode == ExitCode.OK) {
          RawDataFile files[] = paramSet.getParameter(filesParameter).getValue();
          currentValue.setSpecificFiles(files);
        }
      }
      if (type == RawDataFilesSelectionType.NAME_PATTERN) {
        final StringParameter nameParameter = new StringParameter("Name pattern",
            "Set name pattern that may include wildcards (*), e.g. *mouse* matches any name that contains mouse",
            Objects.requireNonNullElse(currentValue.getNamePattern(), ""));
        final SimpleParameterSet paramSet = new SimpleParameterSet(new Parameter[]{nameParameter});
        final ExitCode exitCode = paramSet.showSetupDialog(true);
        if (exitCode == ExitCode.OK) {
          String namePattern = paramSet.getParameter(nameParameter).getValue();
          currentValue.setNamePattern(namePattern);
        }
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

  void setValue(@Nullable RawDataFilesSelection newValue) {
    currentValue = newValue == null ? new RawDataFilesSelection() : newValue.clone();
    RawDataFilesSelectionType type = currentValue.getSelectionType();
    if (type != null) {
      typeCombo.getSelectionModel().select(type);
    }
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
      currentlySelected.set(null);
    } else {
      List<RawDataFile> files = List.of(currentValue.getMatchingRawDataFiles());

      if (currentlySelected.get() == null || !currentlySelected.get().equals(files)) {
        currentlySelected.set(files);
      }

      if (files.size() == 1) {
        String fileName = files.getFirst().getName();
        if (fileName.length() > 22) {
          fileName = fileName.substring(0, 20) + "...";
        }
        numFilesLabel.setText(fileName);
      } else {
        numFilesLabel.setText(files.size() + " selected");
      }
      numFilesLabel.setTooltip(new Tooltip(currentValue.toString()));
    }
  }

  /**
   * The currently selected property is auto updated every second
   *
   * @return a property that holds the currently selected elements
   */
  public ReadOnlyObjectProperty<List<RawDataFile>> currentlySelectedProperty() {
    return currentlySelected.getReadOnlyProperty();
  }

  /**
   * calls an update of the selection first
   */
  public List<RawDataFile> getCurrentlySelected() {
    updateNumFiles();
    return currentlySelected.get();
  }

}
