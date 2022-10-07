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
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineModuleCategory.MainCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.LastFilesButton;
import io.github.mzmine.parameters.parametertypes.filenames.LastFilesComponent;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.javafx.DraggableListCell;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
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

public class BatchComponentController implements LastFilesComponent {

  private final static Logger logger = Logger.getLogger(BatchComponentController.class.getName());

  // by using linked hash map, the items will be added to the tree view as specified in the modules list
  private final Map<MainCategory, TreeItem<Object>> mainCategoryItems = new LinkedHashMap<>();
  private final Map<MZmineModuleCategory, TreeItem<Object>> categoryItems = new LinkedHashMap<>();

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
  public TreeView<Object> tvModules;
  @FXML
  public Button btnLoad;
  @FXML
  public LastFilesButton btnLoadLast;
  @FXML
  public Button btnSave;
  @FXML
  public Button btnClear;
  @FXML
  public TextField searchField;

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

    for (Class<? extends MZmineProcessingModule> moduleClass : BatchModeModulesList.MODULES) {
      final MZmineProcessingModule module = MZmineCore.getModuleInstance(moduleClass);
      final MZmineModuleCategory category = module.getModuleCategory();
      final TreeItem<Object> categoryItem = categoryItems.computeIfAbsent(category, c -> {
        final TreeItem<Object> item = new TreeItem<>(c);
        final TreeItem<Object> mainItem = mainCategoryItems.computeIfAbsent(c.getMainCategory(),
            TreeItem::new);
        mainItem.getChildren().add(item);
        return item;
      });
      categoryItem.getChildren().add(new TreeItem<>(new BatchModuleWrapper(module)));
    }

    final TreeItem<Object> originalRoot = new TreeItem<>("Root");
    originalRoot.getChildren().addAll(mainCategoryItems.values());
    tvModules.setRoot(originalRoot);
    tvModules.setShowRoot(false);

    searchField.textProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue.isEmpty() && !newValue.isBlank()) {
        for (TreeItem<Object> child : originalRoot.getChildren()) {
          child.setExpanded(hasMatchingChild(child, newValue.toLowerCase()));
        }
        selectFirstMatch(originalRoot, newValue.toLowerCase());
      } else {
        for (TreeItem<Object> child : originalRoot.getChildren()) {
          child.setExpanded(false);
        }
      }
    });

    searchField.setOnKeyPressed(event -> {
      if (event.getCode() == KeyCode.ENTER) {
        event.consume();
        onAddModulePressed(null);
      }
    });

    tvModules.setOnMouseClicked(e -> {
      if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
        e.consume();
        onAddModulePressed(null);
      }
    });

    currentStepsList.setOnMouseClicked(e -> {
      if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
        e.consume();
        setParametersPressed(null);
      }
    });
  }

  private boolean hasMatchingChild(TreeItem<Object> item, final String filter) {
    if (!item.isLeaf()) {
      for (var child : item.getChildren()) {
        if (hasMatchingChild(child, filter)) {
          item.setExpanded(true);
          return true;
        }
      }
    } else {
      if (filter.isEmpty()) {
        return false;
      }
      boolean contains = item.getValue().toString().toLowerCase().contains(filter);
//      if (!contains) {
//        item.getParent().getChildren().remove(item);
//      }
      return contains;
    }
    return false;
  }

  private boolean selectFirstMatch(TreeItem<Object> item, final String filter) {
    if (!item.isLeaf()) {
      for (var child : item.getChildren()) {
        if (selectFirstMatch(child, filter)) {
          return true;
        }
      }
    } else {
      if (item.getValue().toString().toLowerCase().contains(filter)) {
        tvModules.getSelectionModel().select(item);
        return true;
      }
    }
    return false;
  }

  public void onAddModulePressed(ActionEvent actionEvent) {
    // Processing module selected?
    final Object selectedItem = tvModules.getSelectionModel().getSelectedItem().getValue();
    if (selectedItem == null) {
      return;
    }
    if (selectedItem instanceof BatchModuleWrapper wrappedModule) {
      // Show method's set-up dialog.
      final MZmineProcessingModule selectedMethod = (MZmineProcessingModule) wrappedModule.getModule();
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
  }

  public void onRemoveModulePressed(ActionEvent actionEvent) {
    var selected = currentStepsList.getSelectionModel().getSelectedItem();
    if (selected != null) {
      batchQueue.remove(selected);
    }
  }

  public void setParametersPressed(ActionEvent actionEvent) {
    // Configure the selected item.
    var selected = currentStepsList.getSelectionModel().getSelectedItem();
    final ParameterSet parameters = selected == null ? null : selected.getParameterSet();
    if (parameters != null) {
      parameters.showSetupDialog(false);
    }
  }

  public void onLoadPressed(ActionEvent actionEvent) {
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

  public void onSavePressed(ActionEvent actionEvent) {
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

  public void clearPressed(ActionEvent actionEvent) {
    batchQueue.clear();
  }

  /**
   * Add a file to the last files button if not already added
   *
   * @param f
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
      throws ParserConfigurationException, TransformerException, FileNotFoundException {

    // Create the document.
    final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .newDocument();
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
   * @throws SAXException                 if there is a SAX problem.
   * @throws IOException                  if there is an i/o problem.
   */
  public void loadBatchSteps(final File file)
      throws ParserConfigurationException, IOException, SAXException {

    final BatchQueue queue = BatchQueue.loadFromXml(
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getDocumentElement());

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

  private TreeItem<Object> cloneTreeItem(TreeItem<Object> item) {
    // not a deep clone
    final TreeItem<Object> clone = new TreeItem<>(item.getValue());
    item.getChildren().forEach(child -> clone.getChildren().add(cloneTreeItem(child)));
    return item;
  }


  // Queue operations.
  private enum QueueOperations {
    Replace, Prepend, Insert, Append
  }
}
