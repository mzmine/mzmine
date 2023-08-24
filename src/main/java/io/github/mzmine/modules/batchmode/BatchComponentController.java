/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.batchmode;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.filenames.LastFilesButton;
import io.github.mzmine.parameters.parametertypes.filenames.LastFilesComponent;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.util.DialogLoggerUtil;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.XMLUtils;
import io.github.mzmine.util.javafx.DraggableListCell;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class BatchComponentController implements LastFilesComponent {

  private final static Logger logger = Logger.getLogger(BatchComponentController.class.getName());


  @FXML
  public AnchorPane root;
  @FXML
  public Button btnSetParameters;
  @FXML
  public Button btnRemoveModule;
  @FXML
  public Button btnAddModule;
  @FXML
  public ListView<MZmineProcessingStep<MZmineProcessingModule>> currentStepsList;
  @FXML
  public BatchModuleTreePane tvModules;
  @FXML
  public Button btnLoad;
  @FXML
  public LastFilesButton btnLoadLast;
  @FXML
  public Button btnSave;
  @FXML
  public Button btnClear;

  @FXML
  public ComboBox<OriginalFeatureListOption> cmbHandleFlists;

  private BatchQueue batchQueue;

  public void initialize() {

    batchQueue = new BatchQueue();
    setValue(batchQueue);

    btnLoadLast.setChangeListener(file -> {
      try {
        loadBatchSteps(file);
      } catch (ParserConfigurationException | IOException | SAXException e) {
        logger.log(Level.WARNING, "Could not load last file " + file.getAbsolutePath(), e);
      }
    });

    currentStepsList.setCellFactory(param -> new DraggableListCell<>() {
      @Override
      protected void updateItem(MZmineProcessingStep<MZmineProcessingModule> item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setText(null);
          setGraphic(null);
        }
        if (item != null && !empty) {
          setText(item.getModule().getName());
          setGraphic(null);
        }
      }
    });
    currentStepsList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

    currentStepsList.setOnMouseClicked(e -> {
      if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
        e.consume();
        setParametersPressed();
      }
    });

    cmbHandleFlists.setItems(FXCollections.observableArrayList(OriginalFeatureListOption.values()));
    cmbHandleFlists.setValue(OriginalFeatureListOption.REMOVE);

    tvModules.setOnAddModuleEventHandler(this::addModule);

    // add key support
    currentStepsList.setOnKeyPressed(event -> {
      if (event.getCode() == KeyCode.ENTER) {
        setParametersPressed();
      }
      if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
        onRemoveModulePressed();
      }
      boolean modifier = event.isAltDown() || event.isShortcutDown();
      if (event.getCode() == KeyCode.PAGE_DOWN && modifier) {
        shiftSelectedStep(1);
      }
      if (event.getCode() == KeyCode.PAGE_UP && modifier) {
        shiftSelectedStep(-1);
      }
      if (event.getCode() == KeyCode.DOWN && modifier) {
        shiftSelectedStep(1);
      }
      if (event.getCode() == KeyCode.UP && modifier) {
        shiftSelectedStep(-1);
      }
      event.consume();
    });
  }

  private void shiftSelectedStep(final int stepShift) {
    int selected = currentStepsList.getSelectionModel().getSelectedIndex();
    if (selected == -1 || selected + stepShift >= currentStepsList.getItems().size()
        || selected + stepShift < 0) {
      return;
    }

    MZmineProcessingStep<MZmineProcessingModule> step = batchQueue.remove(selected);
    batchQueue.add(selected + stepShift, step);
    currentStepsList.setItems(batchQueue);
    currentStepsList.getSelectionModel().select(selected + stepShift);
  }


  public void onAddModulePressed() {
    tvModules.addSelectedModule();
  }

  public void addModule(MZmineRunnableModule module) {
    // only works for processing modules
    if (!(module instanceof MZmineProcessingModule selectedMethod)) {
      return;
    }

    // Show method's set-up dialog.
    final ParameterSet methodParams = MZmineCore.getConfiguration()
        .getModuleParameters(selectedMethod.getClass());

    // Clone the parameter set
    final ParameterSet stepParams =
        methodParams == null ? new SimpleParameterSet() : methodParams.cloneParameterSet();

    // If this is not the first batch step, set the default for raw
    // data file and feature list selection
    if (!batchQueue.isEmpty()) {
      for (Parameter<?> param : stepParams.getParameters()) {
        if (param instanceof final RawDataFilesParameter rdfp) {
          final RawDataFilesSelection selection = new RawDataFilesSelection();
          selection.setSelectionType(RawDataFilesSelectionType.BATCH_LAST_FILES);
          rdfp.setValue(selection);
        }
        if (param instanceof final FeatureListsParameter plp) {
          final FeatureListsSelection selection = new FeatureListsSelection();
          selection.setSelectionType(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS);
          plp.setValue(selection);
        }
      }
    }

    // Configure parameters
    if (stepParams.getParameters().length > 0) {
      ExitCode exitCode = stepParams.showSetupDialog(false);
      if (exitCode != ExitCode.OK) {
        return;
      }
    }

    // Make a new batch step
    final MZmineProcessingStep<MZmineProcessingModule> step = new MZmineProcessingStepImpl<>(
        selectedMethod, stepParams);

    // Add step to queue.
    batchQueue.add(step);
    currentStepsList.setItems(batchQueue);
    currentStepsList.getSelectionModel().select(batchQueue.size() - 1);
  }

  public void onRemoveModulePressed() {
    var selected = currentStepsList.getSelectionModel().getSelectedIndex();
    if (selected != -1) {
      batchQueue.remove(selected);
      currentStepsList.getSelectionModel()
          .select(Math.min(selected, currentStepsList.getItems().size() - 1));
      currentStepsList.requestFocus();
    }
  }

  public void setParametersPressed() {
    // Configure the selected item.
    var selected = currentStepsList.getSelectionModel().getSelectedItem();
    final ParameterSet parameters = selected == null ? null : selected.getParameterSet();
    if (parameters != null) {
      parameters.showSetupDialog(false);
    }
  }

  public void onLoadPressed() {
    try {
      final FileChooser chooser = new FileChooser();

      final ExtensionFilter extension = new ExtensionFilter("MZmine batch files", "*.xml");
      chooser.getExtensionFilters().add(extension);
      chooser.getExtensionFilters().add(new ExtensionFilter("All files", "*.*"));
      chooser.setSelectedExtensionFilter(extension);

      final File lastFile = btnLoadLast.getLastFile();
      if (lastFile != null) {
        chooser.setInitialDirectory(lastFile.getParentFile());
      }

      final File file = chooser.showOpenDialog(root.getScene().getWindow());
      if (file != null) {
        loadBatchSteps(file);
      }
    } catch (Exception ex) {
      MZmineCore.getDesktop()
          .displayErrorMessage("A problem occurred loading the file.\n" + ex.getMessage());
    }
  }

  public void onSavePressed() {
    try {
      final FileChooser chooser = new FileChooser();
      final ExtensionFilter extension = new ExtensionFilter("MZmine batch files", "*.xml");
      chooser.getExtensionFilters().add(extension);
      chooser.getExtensionFilters().add(new ExtensionFilter("All files", "*.*"));
      chooser.setSelectedExtensionFilter(extension);

      final File lastFile = btnLoadLast.getLastFile();
      if (lastFile != null) {
        chooser.setInitialDirectory(lastFile.getParentFile());
      }
      final File file = chooser.showSaveDialog(root.getScene().getWindow());
      if (file != null) {
        saveBatchSteps(file);
      }
    } catch (Exception ex) {
      MZmineCore.getDesktop()
          .displayErrorMessage("A problem occurred saving the file.\n" + ex.getMessage());
    }
  }

  public void clearPressed() {
    batchQueue.clear();
  }

  /**
   * Add a file to the last files button if not already added
   *
   * @param f last file
   */
  public void addLastUsedFile(File f) {
    btnLoadLast.addFile(f);
  }

  /**
   * Get the queue.
   *
   * @return the queue.
   */
  public BatchQueue getValue() {
    return batchQueue;
  }

  /**
   * Sets the queue.
   *
   * @param newValue the new queue.
   */
  public void setValue(final BatchQueue newValue) {

    batchQueue = newValue;
    currentStepsList.setItems(batchQueue);
    selectStep(0);
  }

  @Override
  public void setLastFiles(List<File> lastFiles) {
    btnLoadLast.setLastFiles(lastFiles);
  }

  /**
   * Select a step of the batch queue.
   *
   * @param step the step's index in the queue.
   */
  private void selectStep(final int step) {
    final int size = currentStepsList.getItems().size();
    if (size > 0 && step >= 0) {
      final int index = Math.min(step, size - 1);
      currentStepsList.getSelectionModel().select(index);
      // currentStepsList.ensureIndexIsVisible(index);
    }
  }

  /**
   * Save the batch queue to a file.
   *
   * @param file the file to save in.
   * @throws ParserConfigurationException if there is a parser problem.
   * @throws TransformerException         if there is a transformation problem.
   * @throws FileNotFoundException        if the file can't be found.
   */
  private void saveBatchSteps(final File file)
      throws ParserConfigurationException, TransformerException, IOException {

    // Create the document.
    final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .newDocument();
    final Element element = document.createElement("batch");
    document.appendChild(element);

    // Serialize batch queue.
    batchQueue.saveToXml(element);

    XMLUtils.saveToFile(file, document);

    logger.info("Saved " + batchQueue.size() + " batch step(s) to " + file.getName());
    // add to last used files
    addLastUsedFile(file);
  }

  /**
   * Load a batch queue from a file.
   *
   * @param file the file to read.
   * @throws ParserConfigurationException if there is a parser problem.
   * @throws SAXException                 if there is a SAX problem.
   * @throws IOException                  if there is an i/o problem.
   */
  public void loadBatchSteps(final File file)
      throws ParserConfigurationException, IOException, SAXException {
    List<String> errorMessages = new ArrayList<>();
    final BatchQueue queue = BatchQueue.loadFromXml(XMLUtils.load(file).getDocumentElement(),
        errorMessages);
    // check error messages and show dialog
    if (!errorMessages.isEmpty()) {
      DialogLoggerUtil.showMessageDialog("Check batch parameters carefully.",
          String.join("\n", errorMessages));
    }

    logger.info("Loaded " + queue.size() + " batch step(s) from " + file.getName());

    // Append, prepend, insert or replace.
    List<QueueOperations> operations = List.of(QueueOperations.values());
    ChoiceDialog<QueueOperations> choiceDialog = new ChoiceDialog<>(QueueOperations.Replace,
        operations);
    choiceDialog.setTitle("Add Batch Steps");
    choiceDialog.setContentText("How should the loaded batch steps be added to the queue?");
    choiceDialog.showAndWait();
    QueueOperations option = choiceDialog.getResult();
    if (option == null) {
      return;
    }

    int index = currentStepsList.getSelectionModel().getSelectedIndex();
    switch (option) {
      case Replace -> {
        index = 0;
        batchQueue.clear();
        batchQueue.addAll(queue);
      }
      case Prepend -> {
        index = 0;
        batchQueue.addAll(0, queue);
      }
      case Insert -> {
        index = Math.max(index, 0);
        batchQueue.addAll(index, queue);
      }
      case Append -> {
        index = batchQueue.size();
        batchQueue.addAll(queue);
      }
    }

    selectStep(index);

    // add to last used files
    addLastUsedFile(file);
  }

  // Queue operations.
  private enum QueueOperations {
    Replace, Prepend, Insert, Append
  }

  @FXML
  void onHandleIntermediateFlistsPressed() {
    final OriginalFeatureListOption option = cmbHandleFlists.getValue();
    setHandleOriginalFeatureLists(option);
    var str = switch (option) {
      case KEEP -> "keep intermediate feature lists.";
      case REMOVE -> "remove intermediate feature lists.";
      case PROCESS_IN_PLACE -> "process on feature lists if possible and remove otherwise.";
    };
    MZmineCore.getDesktop()
        .displayMessage("Batch mode updated", "Updated all batch steps to " + str);
  }

  public void setHandleOriginalFeatureLists(OriginalFeatureListOption option) {
    final BatchQueue value = getValue();
    for (MZmineProcessingStep<MZmineProcessingModule> step : value) {
      final ParameterSet parameters = step.getParameterSet();
      for (Parameter<?> parameter : parameters.getParameters()) {
        if (parameter instanceof OriginalFeatureListHandlingParameter handleParameter) {
          handleParameter.setValue(option != OriginalFeatureListOption.PROCESS_IN_PLACE ? option
              : (handleParameter.isIncludeProcessInPlace()
                  ? OriginalFeatureListOption.PROCESS_IN_PLACE : OriginalFeatureListOption.REMOVE));
        }
      }
    }
  }
}
