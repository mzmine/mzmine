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

package io.github.mzmine.gui.mainwindow;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.batchmode.BatchModeModule;
import io.github.mzmine.modules.batchmode.BatchModeParameters;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.EmbeddedParameterSet;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.util.ExitCode;
import java.util.Arrays;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.jetbrains.annotations.Nullable;

public class FeatureListSummaryController {

  private static final Logger logger = Logger
      .getLogger(FeatureListSummaryController.class.getName());

  @FXML
  public TextField tfNumRows;
  @FXML
  public TextField tfCreated;
  @FXML
  public ListView<FeatureListAppliedMethod> lvAppliedMethods;
  @FXML
  public TextArea tvParameterValues;
  @FXML
  public Label lbFeatureListName;
  @FXML
  public Button btnOpenInBatchQueue;

  @FXML
  public void initialize() {

    lvAppliedMethods.getSelectionModel().selectedItemProperty()
        .addListener((observable, oldValue, newValue) -> {
          tvParameterValues.clear();

          if (newValue == null) {
            return;
          }

          tvParameterValues.appendText(newValue.getDescription());
          tvParameterValues.appendText("\n");
          for (Parameter<?> parameter : newValue.getParameters().getParameters()) {
            tvParameterValues.appendText(parameterToString(parameter));
            tvParameterValues.appendText("\n");
          }
        });
  }

  public void setFeatureList(@Nullable ModularFeatureList featureList) {
    clear();

    if (featureList == null) {
      return;
    }

    lbFeatureListName.setText(featureList.getName());
    tfNumRows.setText(String.valueOf(featureList.getNumberOfRows()));
    tfCreated.setText(featureList.getDateCreated());
    lvAppliedMethods.setItems(featureList.getAppliedMethods());
  }

  public void setRawDataFile(@Nullable RawDataFile file) {
    clear();
    if (file == null) {
      return;
    }

    lbFeatureListName.setText(file.getName());
    tfNumRows.setText(String.valueOf(file.getNumOfScans()));
    tfCreated.setText(file.getAbsolutePath());
    lvAppliedMethods.setItems(file.getAppliedMethods());
  }

  public void clear() {
    lbFeatureListName.setText("None selected");
    tfNumRows.setText("");
    tfCreated.setText("");
    lvAppliedMethods.getItems().clear();
    tvParameterValues.setText("");
  }

  private String parameterToString(Parameter<?> parameter) {
    String name = parameter.getName();
    Object value = parameter.getValue();
    StringBuilder sb = new StringBuilder(name);
    sb.append(":\t");
    if(value.getClass().isArray()) {
      sb.append(Arrays.toString((Object[]) value));
    } else {
      sb.append(value.toString());
    }
    if (parameter instanceof EmbeddedParameterSet embedded) {
      ParameterSet parameterSet = embedded.getEmbeddedParameters();
      for (Parameter<?> parameter1 : parameterSet.getParameters()) {
        sb.append("\n\t");
        sb.append(parameterToString(parameter1));
      }
    }
    if(parameter instanceof OptionalParameter) {
      sb.append("\t(");
      sb.append(((OptionalParameter<?>) parameter).getEmbeddedParameter().getValue());
      sb.append(")");
    }
    return sb.toString();
  }

  @FXML
  void setAsBatchQueue() {

    ButtonType btn = MZmineCore.getDesktop().displayConfirmation(
        "Warning: This will overwrite the current batch queue.\nDo you wish to continue?",
        ButtonType.YES, ButtonType.NO);

    if (btn != ButtonType.YES) {
      return;
    }

    BatchQueue queue = new BatchQueue();

    for (FeatureListAppliedMethod item : lvAppliedMethods.getItems()) {
      if (item.getModule() instanceof MZmineProcessingModule) {
        ParameterSet parameterSet = item.getParameters().cloneParameterSet();
        setSelectionToLastBatchStep(parameterSet);
        MZmineProcessingStep<MZmineProcessingModule> step = new MZmineProcessingStepImpl(
            item.getModule(), parameterSet);
        queue.add(step);
      } else {
        logger.warning(() -> "Cannot add module " + item.getModule() + " as a batch step because "
            + "it is not an instance of MZmineProcessingModule.");
      }
    }

    BatchModeParameters batchModeParameters = (BatchModeParameters) MZmineCore.getConfiguration()
        .getModuleParameters(BatchModeModule.class);
    batchModeParameters.getParameter(BatchModeParameters.batchQueue).setValue(queue);

    if(batchModeParameters.showSetupDialog(true) == ExitCode.OK) {
      MZmineCore.runMZmineModule(BatchModeModule.class, batchModeParameters.cloneParameterSet());
    }
  }

  private void setSelectionToLastBatchStep(ParameterSet parameters) {
    for (Parameter<?> parameter : parameters.getParameters()) {
      if (parameter instanceof FeatureListsParameter) {
        final FeatureListsSelection featureListsSelection = new FeatureListsSelection();
        featureListsSelection.setSelectionType(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS);
        ((FeatureListsParameter) parameter).setValue(featureListsSelection);
      } else if (parameter instanceof RawDataFilesParameter) {
        final RawDataFilesSelection rawDataFilesSelection = new RawDataFilesSelection(
            RawDataFilesSelectionType.BATCH_LAST_FILES);
        ((RawDataFilesParameter) parameter).setValue(rawDataFilesSelection);
      }
    }
  }
}
