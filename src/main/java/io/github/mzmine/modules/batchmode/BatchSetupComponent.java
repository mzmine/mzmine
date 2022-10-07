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

package io.github.mzmine.modules.batchmode;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.LastFilesButton;
import io.github.mzmine.parameters.parametertypes.filenames.LastFilesComponent;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.javafx.DraggableListCell;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class BatchSetupComponent extends BorderPane implements LastFilesComponent {


  // Logger.
  private static final Logger logger = Logger.getLogger(BatchSetupComponent.class.getName());

  // XML extension.
  private static final ExtensionFilter xmlExtensionFilter =
      new ExtensionFilter("XML files", "*.xml");

  // Queue operations.
  private enum QueueOperations {
    Replace, Prepend, Insert, Append
  }

  // The batch queue.
  private BatchQueue batchQueue;

  // Widgets.
  private final ComboBox<Object> methodsCombo;
  private final ListView<MZmineProcessingStep<MZmineProcessingModule>> currentStepsList;
  private final Button btnAdd, btnConfig, btnRemove, btnClear, btnLoad, btnSave;

  Object[] queueListModel;

  // File chooser.
  private FileChooser chooser;

  private LastFilesButton btnLoadLastFiles;

  /**
   * Create the component.
   */
  public BatchSetupComponent() {

    batchQueue = new BatchQueue();

    // Create file chooser.
    chooser = new FileChooser();
    chooser.setTitle("Select Batch Queue File");
    chooser.getExtensionFilters().add(xmlExtensionFilter);


    // The steps list.
    currentStepsList = new ListView<>();
    currentStepsList.setCellFactory(
        param -> new DraggableListCell<MZmineProcessingStep<MZmineProcessingModule>>() {
          @Override
          protected void updateItem(MZmineProcessingStep<MZmineProcessingModule> item,
              boolean empty) {
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

    // Methods combo box.
    methodsCombo = new ComboBox<Object>();
    // methodsCombo.setMaximumRowCount(14);

    // Add processing modules to combo box by category.
    final Collection<MZmineModule> allModules = MZmineCore.getAllModules();

    for (final MZmineModuleCategory category : MZmineModuleCategory.values()) {

      boolean categoryItemAdded = false;
      for (final MZmineModule module : allModules) {

        // Processing module? Exclude the batch mode module.
        if (!module.getClass().equals(BatchModeModule.class)
            && module instanceof MZmineProcessingModule) {

          final MZmineProcessingModule step = (MZmineProcessingModule) module;

          // Correct category?
          if (step.getModuleCategory() == category) {

            // Add category item?
            if (!categoryItemAdded) {
              methodsCombo.getItems().add(category);
              categoryItemAdded = true;
            }

            // Add method item.
            BatchModuleWrapper wrappedModule = new BatchModuleWrapper(step);
            methodsCombo.getItems().add(wrappedModule);
          }
        }
      }
    }

    // Create Load/Save buttons.
    // button to load one of the last used files
    btnLoadLastFiles = new LastFilesButton("Load last...", file -> {
      try {
        loadBatchSteps(file);
      } catch (ParserConfigurationException | IOException | SAXException e) {
        logger.log(Level.WARNING, "Could not load last file " + file.getAbsolutePath(), e);
      }
    });

    btnLoad = new Button("Load...");
    btnLoad.setTooltip(new Tooltip("Loads a batch queue from a file"));
    btnSave = new Button("Save...");
    btnSave.setTooltip(new Tooltip("Saves a batch queue to a file"));
    final HBox panelTop = new HBox(10.0, btnLoadLastFiles, btnLoad, btnSave);

    btnConfig = new Button("Configure");
    btnConfig.setTooltip(new Tooltip("Configure the selected batch step"));
    btnRemove = new Button("Remove");
    btnRemove.setTooltip(new Tooltip("Remove the selected batch step"));
    btnClear = new Button("Clear");
    btnClear.setTooltip(new Tooltip("Removes all batch steps"));
    final VBox pnlRight = new VBox(10.0, btnConfig, btnRemove, btnClear);

    final BorderPane pnlBottom = new BorderPane();
    btnAdd = new Button("Add");
    btnAdd.setTooltip(new Tooltip("Adds the selected method to the batch queue"));
    pnlBottom.setRight(btnAdd);
    pnlBottom.setCenter(methodsCombo);


    btnAdd.setOnAction(e -> {
      // Processing module selected?
      final Object selectedItem = methodsCombo.getSelectionModel().getSelectedItem();
      if (selectedItem instanceof BatchModuleWrapper) {
        // Show method's set-up dialog.
        final BatchModuleWrapper wrappedModule = (BatchModuleWrapper) selectedItem;
        final MZmineProcessingModule selectedMethod =
            (MZmineProcessingModule) wrappedModule.getModule();
        final ParameterSet methodParams =
            MZmineCore.getConfiguration().getModuleParameters(selectedMethod.getClass());

        // Clone the parameter set
        final ParameterSet stepParams = methodParams.cloneParameterSet();

        // If this is not the first batch step, set the default for raw
        // data file and feature list selection
        if (!batchQueue.isEmpty()) {
          for (Parameter<?> param : stepParams.getParameters()) {
            if (param instanceof RawDataFilesParameter) {
              final RawDataFilesParameter rdfp = (RawDataFilesParameter) param;
              final RawDataFilesSelection selection = new RawDataFilesSelection();
              selection.setSelectionType(RawDataFilesSelectionType.BATCH_LAST_FILES);
              rdfp.setValue(selection);
            }
            if (param instanceof FeatureListsParameter) {
              final FeatureListsParameter plp = (FeatureListsParameter) param;
              final FeatureListsSelection selection = new FeatureListsSelection();
              selection.setSelectionType(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS);
              plp.setValue(selection);
            }
          }
        }

        // Configure parameters
        if (stepParams.getParameters().length > 0) {
          ExitCode exitCode = stepParams.showSetupDialog(false);
          if (exitCode != ExitCode.OK)
            return;
        }

        // Make a new batch step
        final MZmineProcessingStep<MZmineProcessingModule> step =
            new MZmineProcessingStepImpl<MZmineProcessingModule>(selectedMethod, stepParams);

        // Add step to queue.
        batchQueue.add(step);
        currentStepsList.setItems(batchQueue);
        currentStepsList.getSelectionModel().select(batchQueue.size() - 1);

      }
    });

    btnRemove.setOnAction(e -> {
      // Remove selected step.
      var selected = currentStepsList.getSelectionModel().getSelectedItem();
      if (selected != null) {
        batchQueue.remove(selected);
      }
    });

    btnClear.setOnAction(e -> {
      // Clear the queue.
      batchQueue.clear();
    });

    btnConfig.setOnAction(e -> {
      // Configure the selected item.
      var selected = currentStepsList.getSelectionModel().getSelectedItem();
      final ParameterSet parameters = selected == null ? null : selected.getParameterSet();
      if (parameters != null) {
        parameters.showSetupDialog(false);
      }
    });

    btnSave.setOnAction(e -> {
      try {
        final File file = chooser.showSaveDialog(this.getScene().getWindow());
        if (file != null) {
          saveBatchSteps(file);
        }
      } catch (Exception ex) {

        MZmineCore.getDesktop()
            .displayErrorMessage("A problem occurred saving the file.\n" + ex.getMessage());
      }
    });

    btnLoad.setOnAction(e -> {
      try {
        // Load the steps.
        final File file = chooser.showOpenDialog(this.getScene().getWindow());
        if (file != null) {

          // Load the batch steps.
          loadBatchSteps(file);
        }
      } catch (Exception ex) {

        MZmineCore.getDesktop()
            .displayErrorMessage("A problem occurred loading the file.\n" + ex.getMessage());
      }
    });
    // Layout sub-panels.
    setTop(panelTop);
    setCenter(currentStepsList);
    setBottom(pnlBottom);
    setRight(pnlRight);

  }


  @Override
  public void setLastFiles(List<File> lastFiles) {
    btnLoadLastFiles.setLastFiles(lastFiles);
  }

  /**
   * Add a file to the last files button if not already added
   *
   * @param f
   */
  public void addLastUsedFile(File f) {
    btnLoadLastFiles.addFile(f);
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
   * @throws TransformerException if there is a transformation problem.
   * @throws FileNotFoundException if the file can't be found.
   */
  private void saveBatchSteps(File file)
      throws ParserConfigurationException, TransformerException, FileNotFoundException {
    // ensure xml format
    file = FileAndPathUtil.getRealFilePath(file, "xml");

    // Create the document.
    final Document document =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    final Element element = document.createElement("batch");
    document.appendChild(element);

    // Serialize batch queue.
    batchQueue.saveToXml(element);

    // Create transformer.
    final Transformer transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

    // Write to file and transform.
    transformer.transform(new DOMSource(document), new StreamResult(new FileOutputStream(file)));

    logger.info("Saved " + batchQueue.size() + " batch step(s) to " + file.getName());
    // add to last used files
    addLastUsedFile(file);
  }

  /**
   * Load a batch queue from a file.
   *
   * @param file the file to read.
   * @throws ParserConfigurationException if there is a parser problem.
   * @throws SAXException if there is a SAX problem.
   * @throws IOException if there is an i/o problem.
   */
  public void loadBatchSteps(final File file)
      throws ParserConfigurationException, IOException, SAXException {

    final BatchQueue queue = BatchQueue.loadFromXml(
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getDocumentElement());

    logger.info("Loaded " + queue.size() + " batch step(s) from " + file.getName());

    // Append, prepend, insert or replace.
    List<QueueOperations> operations = List.of(QueueOperations.values());
    ChoiceDialog<QueueOperations> choiceDialog =
        new ChoiceDialog<>(QueueOperations.Replace, operations);
    choiceDialog.setTitle("Add Batch Steps");
    choiceDialog.setContentText("How should the loaded batch steps be added to the queue?");
    choiceDialog.showAndWait();
    QueueOperations option = choiceDialog.getResult();
    if (option == null)
      return;


    int index = currentStepsList.getSelectionModel().getSelectedIndex();
    switch (option) {
      case Replace:
        index = 0;
        batchQueue.clear();
        batchQueue.addAll(queue);
        break;
      case Prepend:
        index = 0;
        batchQueue.addAll(0, queue);
        break;
      case Insert:
        index = index < 0 ? 0 : index;
        batchQueue.addAll(index, queue);
        break;
      case Append:
        index = batchQueue.size();
        batchQueue.addAll(queue);
        break;
    }

    selectStep(index);

    // add to last used files
    addLastUsedFile(file);
  }



}
