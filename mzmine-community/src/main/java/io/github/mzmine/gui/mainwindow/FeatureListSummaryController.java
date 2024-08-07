/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.gui.mainwindow;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
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
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.jetbrains.annotations.Nullable;

public class FeatureListSummaryController {

  private static final Logger logger = Logger.getLogger(
      FeatureListSummaryController.class.getName());

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
  public Button exportfeature;

  public static String parameterToString(Parameter<?> parameter) {
    String name = parameter.getName();
    Object value = parameter.getValue();
    StringBuilder sb = new StringBuilder(name);
    sb.append(":\t");
    if (value == null) {
      sb.append("<not set>");
    } else if (value.getClass().isArray()) {
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
    if (parameter instanceof OptionalParameter) {
      sb.append("\t(");
      sb.append(((OptionalParameter<?>) parameter).getEmbeddedParameter().getValue());
      sb.append(")");
    }
    return sb.toString();
  }

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

  @FXML
  void setAsBatchQueue() {

    boolean result = DialogLoggerUtil.showDialogYesNo("Overwriting batch?",
        "Warning: This will overwrite the current batch queue.\nDo you wish to continue?");

    if (!result) {
      return;
    }

    BatchQueue queue = new BatchQueue();

    for (FeatureListAppliedMethod item : lvAppliedMethods.getItems()) {
      if (item == null) {
        logger.info("Skipping module ???, cannot find module class. Was it renamed?");
        continue;
      }
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

    if (batchModeParameters.showSetupDialog(true) == ExitCode.OK) {
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

  @FXML
    //Export Record
  void exportRecord() throws IOException {
    boolean result = DialogLoggerUtil.showDialogYesNo("Export Feature Summary?",
        "Export Feature Summary List\nDo you wish to continue?");
    if (!result) {
      return;
    }
    FileChooser fc = new FileChooser();
    fc.getExtensionFilters()
        .addAll(new FileChooser.ExtensionFilter("comma-separated values", "*.csv"),
            new FileChooser.ExtensionFilter("All File", "*.*"));
    fc.setTitle("Save Feature List Summary");
    File file = fc.showSaveDialog(new Stage());
    try {
      BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8);
      PrintWriter pw = new PrintWriter(writer);
      for (FeatureListAppliedMethod item : lvAppliedMethods.getItems()) {
        String sb = item.getDescription();
        //StringBuilder sb = new StringBuilder(item.getDescription());
        pw.println(sb);
        ParameterSet parameterSet = item.getParameters();
        for (Parameter<?> parameter : parameterSet.getParameters()) {
          pw.println(parameterToString(parameter));
        }
        pw.println();
      }
      pw.flush();
      pw.close();
    } catch (Exception e) {
      logger.info(e.getMessage());
    }
  }
}
