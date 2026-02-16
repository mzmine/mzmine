/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.tools.batchwizard;

import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.batchmode.BatchModuleTreePane;
import io.github.mzmine.modules.tools.batchwizard.subparameters.ApplicationScope;
import io.github.mzmine.modules.tools.batchwizard.subparameters.ParameterOverride;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.HiddenParameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.Nullable;

/**
 * UI pane for customizing batch module parameters. This is the editor component for
 * ParameterOverridesParameter.
 */
public class ParameterCustomizationPane extends BorderPane {

  private final BatchModuleTreePane moduleTreePane;
  private final ListView<ParameterWrapper> parameterListView;
  private final VBox parameterEditorPane;
  private final Label instructionsLabel;
  private final TableView<ParameterOverride> overridesTable;
  private final Button addButton;
  private final Button overrideButton;
  private final Button removeButton;
  private final Button clearAllButton;
  private final ComboBox<ApplicationScope> scopeComboBox;

  // Temporary storage while editing
  private final Map<String, ParameterOverride> tempOverrides;

  private MZmineRunnableModule selectedModule;
  private ParameterWrapper selectedParameter;
  private Node currentEditorComponent;
  private ParameterOverride selectedOverride;

  public ParameterCustomizationPane() {
    this.moduleTreePane = new BatchModuleTreePane(true);
    this.parameterListView = new ListView<>();
    this.parameterEditorPane = FxLayout.newVBox();
    this.instructionsLabel = FxLabels.newLabel("Select a module to view its parameters");
    this.overridesTable = new TableView<>();
    this.tempOverrides = new HashMap<>();

    this.addButton = FxButtons.createButton("Add", FxIcons.ADD, "Add new parameter override",
        this::addParameterOverride);
    this.overrideButton = FxButtons.createButton("Override", FxIcons.EDIT,
        "Update existing parameter override", this::updateParameterOverride);
    this.removeButton = FxButtons.createButton("Remove", FxIcons.CANCEL, "Remove selected override",
        this::removeSelectedOverride);
    this.clearAllButton = FxButtons.createButton("Clear All", FxIcons.CLEAR,
        "Clear all parameter overrides", this::clearAllOverrides);

    this.scopeComboBox = new ComboBox<>();
    this.scopeComboBox.getItems().addAll(ApplicationScope.values());
    this.scopeComboBox.setValue(ApplicationScope.ALL);
    this.scopeComboBox.setMaxWidth(Double.MAX_VALUE);

    setupUI();
    setupEventHandlers();
  }

  private void setupUI() {
    SplitPane mainSplitPane = new SplitPane();
    mainSplitPane.setOrientation(Orientation.HORIZONTAL);

    VBox leftPane = createModuleTreePane();
    VBox middlePane = createParameterEditorPane();
    VBox rightPane = createOverridesPane();

    mainSplitPane.getItems().addAll(leftPane, middlePane, rightPane);
    mainSplitPane.setDividerPositions(0.25, 0.5);

    setCenter(mainSplitPane);
  }

  private VBox createModuleTreePane() {
    Label moduleLabel = FxLabels.newBoldLabel("Select Module:");
    VBox leftPane = FxLayout.newVBox(null, FxLayout.DEFAULT_PADDING_INSETS, true, moduleLabel,
        moduleTreePane);
    VBox.setVgrow(moduleTreePane, Priority.ALWAYS);
    return leftPane;
  }

  private VBox createParameterEditorPane() {
    Label paramLabel = FxLabels.newBoldLabel("Parameters:");
    Label editorLabel = FxLabels.newBoldLabel("Parameter Editor:");

    parameterEditorPane.getChildren().add(instructionsLabel);

    ScrollPane editorScroll = new ScrollPane(parameterEditorPane);
    editorScroll.setFitToWidth(true);
    editorScroll.setFitToHeight(true);

    addButton.setDisable(true);
    addButton.setMaxWidth(Double.MAX_VALUE);
    overrideButton.setDisable(true);
    overrideButton.setMaxWidth(Double.MAX_VALUE);

    Label scopeLabel = FxLabels.newLabel("Apply to:");
    HBox scopeBox = FxLayout.newHBox(Pos.CENTER_LEFT, FxLayout.DEFAULT_PADDING_INSETS, scopeLabel,
        scopeComboBox);
    HBox.setHgrow(scopeComboBox, Priority.ALWAYS);

    HBox buttonRow = FxLayout.newHBox(Pos.CENTER, addButton, overrideButton);

    VBox buttonBox = FxLayout.newVBox(null, FxLayout.DEFAULT_PADDING_INSETS, true, scopeBox,
        buttonRow);

    VBox editorSection = FxLayout.newVBox(null, FxLayout.DEFAULT_PADDING_INSETS, true, editorLabel,
        editorScroll, buttonBox);
    VBox.setVgrow(editorScroll, Priority.ALWAYS);

    VBox middlePane = FxLayout.newVBox(null, FxLayout.DEFAULT_PADDING_INSETS, true, paramLabel,
        parameterListView, editorSection);
    VBox.setVgrow(parameterListView, Priority.SOMETIMES);
    VBox.setVgrow(editorSection, Priority.ALWAYS);

    return middlePane;
  }

  private VBox createOverridesPane() {
    Label overviewLabel = FxLabels.newBoldLabel("Parameter Overrides:");

    TableColumn<ParameterOverride, String> moduleCol = new TableColumn<>("Module");
    moduleCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().moduleName()));
    moduleCol.setPrefWidth(150);

    TableColumn<ParameterOverride, String> paramCol = new TableColumn<>("Parameter");
    paramCol.setCellValueFactory(
        param -> new SimpleStringProperty(param.getValue().parameterWithValue().getName()));
    paramCol.setPrefWidth(150);

    TableColumn<ParameterOverride, String> valueCol = new TableColumn<>("Value");
    valueCol.setCellValueFactory(
        param -> new SimpleStringProperty(param.getValue().getDisplayValue()));
    valueCol.setPrefWidth(150);

    TableColumn<ParameterOverride, ApplicationScope> scopeCol = new TableColumn<>("Apply to");
    scopeCol.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().scope()));
    scopeCol.setPrefWidth(100);
    scopeCol.setCellFactory(col -> new ComboBoxTableCell<>(ApplicationScope.values()) {
      @Override
      public void updateItem(ApplicationScope item, boolean empty) {
        super.updateItem(item, empty);
        if (!empty && item != null) {
          setText(item.getLabel());
        }
      }
    });
    scopeCol.setOnEditCommit(event -> {
      ParameterOverride override = event.getRowValue();
      ApplicationScope newScope = event.getNewValue();
      if (newScope != null && newScope != override.scope()) {
        // Remove the old entry with the old scope
        String oldKey = makeKey(override.moduleClassName(), override.parameterWithValue().getName(),
            override.scope());
        tempOverrides.remove(oldKey);

        // Add the new entry with the new scope
        String newKey = makeKey(override.moduleClassName(), override.parameterWithValue().getName(),
            newScope);
        tempOverrides.put(newKey,
            new ParameterOverride(override.moduleClassName(), override.moduleName(),
                override.parameterWithValue(), newScope));
        refreshOverridesTable();
      }
    });

    overridesTable.getColumns().addAll(moduleCol, paramCol, valueCol, scopeCol);
    overridesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    overridesTable.setPlaceholder(FxLabels.newLabel("No parameter overrides set"));
    overridesTable.setEditable(true);

    removeButton.setMaxWidth(Double.MAX_VALUE);
    clearAllButton.setMaxWidth(Double.MAX_VALUE);

    HBox buttonBox = FxLayout.newHBox(Pos.CENTER, FxLayout.DEFAULT_PADDING_INSETS, removeButton,
        clearAllButton);

    VBox rightPane = FxLayout.newVBox(null, FxLayout.DEFAULT_PADDING_INSETS, true, overviewLabel,
        overridesTable, buttonBox);
    VBox.setVgrow(overridesTable, Priority.ALWAYS);

    return rightPane;
  }

  private void setupEventHandlers() {
    moduleTreePane.setOnAddModuleEventHandler(this::onModuleSelected);
    parameterListView.getSelectionModel().selectedItemProperty()
        .addListener((obs, oldVal, newVal) -> onParameterSelected(newVal));

    overridesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
      removeButton.setDisable(newVal == null);
      selectedOverride = newVal;
      if (newVal != null) {
        selectOverrideInEditor(newVal);
      }
      updateButtonStates();
    });

    removeButton.setDisable(true);

    // When scope changes, reload the parameter value for that scope
    scopeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
      if (selectedParameter != null && newVal != null) {
        loadExistingOverrideValue(selectedParameter.parameter);
      }
      updateButtonStates();
    });

    // Ctrl+F focuses the module search box - handle both KEY_PRESSED and KEY_RELEASED
    // to prevent global quick search dialog from opening
    addEventFilter(KeyEvent.KEY_PRESSED, event -> {
      if (event.isControlDown() && event.getCode() == KeyCode.F) {
        moduleTreePane.focusSearchField();
        event.consume();
      }
    });
    addEventFilter(KeyEvent.KEY_RELEASED, event -> {
      if (event.isControlDown() && event.getCode() == KeyCode.F) {
        event.consume();
      }
    });
  }

  private void onModuleSelected(MZmineRunnableModule module) {
    if (module == null) {
      return;
    }

    this.selectedModule = module;
    this.selectedParameter = null;
    this.currentEditorComponent = null;
    updateButtonStates();

    // important: clone the parameter set before loading it in here
    ParameterSet parameters = ConfigService.getConfiguration()
        .getModuleParameters(module.getClass()).cloneParameterSet();
    if (parameters == null) {
      parameterListView.getItems().clear();
      showInstructions("No parameters available for this module");
      return;
    }

    parameterListView.getItems().clear();
    Arrays.stream(parameters.getParameters()).filter(p -> p instanceof UserParameter<?, ?>)
        .filter(p -> !(p instanceof HiddenParameter))
        .map(p -> new ParameterWrapper((UserParameter<?, ?>) p))
        .forEach(parameterListView.getItems()::add);

    if (parameterListView.getItems().isEmpty()) {
      showInstructions("No user-configurable parameters for this module");
    } else {
      showInstructions("Select a parameter to edit its value");
    }
  }

  private void onParameterSelected(@Nullable ParameterWrapper paramWrapper) {
    if (paramWrapper == null || selectedModule == null) {
      updateButtonStates();
      return;
    }

    this.selectedParameter = paramWrapper;
    parameterEditorPane.getChildren().clear();

    UserParameter<?, ?> parameter = paramWrapper.parameter;
    currentEditorComponent = createEditorForParameter(parameter);

    if (currentEditorComponent != null) {
      GridPane grid = new GridPane();
      grid.setHgap(10);
      grid.setVgap(10);
      grid.setPadding(new Insets(10));

      Label nameLabel = FxLabels.newBoldLabel(parameter.getName() + ":");

      Label descLabel = FxLabels.newLabel(parameter.getDescription());
      descLabel.setWrapText(true);
      descLabel.setMaxWidth(400);

      grid.add(nameLabel, 0, 0);
      grid.add(descLabel, 0, 1);
      grid.add(currentEditorComponent, 0, 2);

      GridPane.setHgrow(currentEditorComponent, Priority.ALWAYS);

      parameterEditorPane.getChildren().add(grid);

      loadExistingOverrideValue(parameter);
      updateButtonStates();
    } else {
      showInstructions("Cannot create editor for this parameter type");
      updateButtonStates();
    }
  }

  @SuppressWarnings("unchecked")
  private <T, C extends Node> Node createEditorForParameter(UserParameter<?, ?> parameter) {
    try {
      UserParameter<T, C> typedParam = (UserParameter<T, C>) parameter;
      C component = typedParam.createEditingComponent();
      T currentValue = typedParam.getValue();
      typedParam.setValueToComponent(component, currentValue);
      return component;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Checks if the parameter already has an override for the currently selected scope.
   */
  @SuppressWarnings("unchecked")
  private void loadExistingOverrideValue(UserParameter<?, ?> parameter) {
    if (selectedModule == null) {
      return;
    }

    ApplicationScope currentScope = scopeComboBox.getValue();
    String key = makeKey(selectedModule.getClass().getName(), parameter.getName(), currentScope);
    ParameterOverride temp = tempOverrides.get(key);
    if (temp != null) {
      try {
        ((UserParameter<Object, Node>) parameter).setValueToComponent(currentEditorComponent,
            temp.parameterWithValue().getValue());
        scopeComboBox.setValue(temp.scope());
      } catch (Exception e) {
        // Ignore
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void addParameterOverride() {
    if (selectedParameter == null || selectedModule == null || currentEditorComponent == null) {
      return;
    }

    try {
      UserParameter<Object, Node> param = (UserParameter<Object, Node>) selectedParameter.parameter;
      param.setValueFromComponent(currentEditorComponent);

      String moduleClassName = selectedModule.getClass().getName();
      String moduleName = getModuleName(selectedModule.getClass());
      ApplicationScope scope = scopeComboBox.getValue();
      String key = makeKey(moduleClassName, param.getName(), scope);

      tempOverrides.put(key,
          new ParameterOverride(moduleClassName, moduleName, param.cloneParameter(), scope));

      refreshOverridesTable();
      showInstructions(
          "Parameter override added: " + param.getName() + " (apply to: " + scope.getLabel() + ")");
    } catch (Exception e) {
      showInstructions("Error adding parameter: " + e.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  private void updateParameterOverride() {
    if (selectedParameter == null || selectedModule == null || currentEditorComponent == null) {
      return;
    }

    try {
      UserParameter<Object, Node> param = (UserParameter<Object, Node>) selectedParameter.parameter;
      param.setValueFromComponent(currentEditorComponent);

      String moduleClassName = selectedModule.getClass().getName();
      String moduleName = getModuleName(selectedModule.getClass());
      ApplicationScope scope = scopeComboBox.getValue();
      String key = makeKey(moduleClassName, param.getName(), scope);

      tempOverrides.put(key,
          new ParameterOverride(moduleClassName, moduleName, param.cloneParameter(), scope));

      refreshOverridesTable();
      showInstructions(
          "Parameter override updated: " + param.getName() + " (apply to: " + scope.getLabel()
              + ")");
    } catch (Exception e) {
      showInstructions("Error updating parameter: " + e.getMessage());
    }
  }

  private void removeSelectedOverride() {
    ParameterOverride selected = overridesTable.getSelectionModel().getSelectedItem();
    if (selected == null) {
      return;
    }

    final String key = makeKey(selected.moduleClassName(), selected.parameterWithValue().getName(),
        selected.scope());
    tempOverrides.remove(key);
    refreshOverridesTable();
  }

  private void clearAllOverrides() {
    tempOverrides.clear();
    refreshOverridesTable();
    showInstructions("All parameter overrides cleared");
  }

  private void refreshOverridesTable() {
    overridesTable.getItems().clear();
    overridesTable.getItems().setAll(tempOverrides.values());
  }

  private String getModuleName(Class<? extends MZmineRunnableModule> moduleClass) {
    try {
      MZmineModule module = MZmineCore.getModuleInstance(moduleClass);
      return module != null ? module.getName() : moduleClass.getSimpleName();
    } catch (Exception e) {
      return moduleClass.getSimpleName();
    }
  }

  private void showInstructions(String message) {
    parameterEditorPane.getChildren().clear();
    instructionsLabel.setText(message);
    parameterEditorPane.getChildren().add(instructionsLabel);
    updateButtonStates();
  }

  private String makeKey(String moduleClassName, String paramName, ApplicationScope scope) {
    return moduleClassName + "::" + paramName + "::" + scope;
  }

  /**
   * Updates the enabled state of Add and Override buttons based on current selection
   */
  private void updateButtonStates() {
    boolean hasParameterSelected =
        selectedParameter != null && selectedModule != null && currentEditorComponent != null;

    // Add button is enabled when a parameter is selected
    addButton.setDisable(!hasParameterSelected);

    // Override button is enabled when:
    // 1. A parameter is selected
    // 2. An override exists for the current module+parameter+scope combination
    if (hasParameterSelected) {
      String moduleClassName = selectedModule.getClass().getName();
      String paramName = selectedParameter.parameter.getName();
      ApplicationScope scope = scopeComboBox.getValue();
      String key = makeKey(moduleClassName, paramName, scope);
      boolean overrideExists = tempOverrides.containsKey(key);
      overrideButton.setDisable(!overrideExists);
    } else {
      overrideButton.setDisable(true);
    }
  }

  /**
   * Selects the module and parameter in the editor when an override is clicked in the table
   */
  private void selectOverrideInEditor(ParameterOverride override) {
    // Find the module
    String targetModuleClass = override.moduleClassName();
    MZmineRunnableModule targetModule = null;

    try {
      Class<?> moduleClass = Class.forName(targetModuleClass);
      if (MZmineRunnableModule.class.isAssignableFrom(moduleClass)) {
        targetModule = (MZmineRunnableModule) MZmineCore.getModuleInstance(
            (Class<? extends MZmineRunnableModule>) moduleClass);
      }
    } catch (Exception e) {
      // Module not found
      return;
    }

    if (targetModule != null) {
      // Load the module's parameters
      onModuleSelected(targetModule);

      // Select the parameter in the list
      String targetParamName = override.parameterWithValue().getName();
      for (ParameterWrapper wrapper : parameterListView.getItems()) {
        if (wrapper.parameter.getName().equals(targetParamName)) {
          parameterListView.getSelectionModel().select(wrapper);
          break;
        }
      }

      // Set the scope
      scopeComboBox.setValue(override.scope());
    }
  }

  /**
   * Get all parameter overrides as a list
   */
  public List<ParameterOverride> getParameterOverrides() {
    return new ArrayList<>(tempOverrides.values());
  }

  /**
   * Set parameter overrides from a list
   */
  public void setParameterOverrides(List<ParameterOverride> overrides) {
    tempOverrides.clear();
    for (ParameterOverride override : overrides) {
      String key = makeKey(override.moduleClassName(), override.parameterWithValue().getName(),
          override.scope());
      // We need to reconstruct the actual value object from the saved string
      // For now, we'll store it as a temp with the string value
      // In a real implementation, you'd need to deserialize based on valueType
      tempOverrides.put(key, override);
    }
    refreshOverridesTable();
  }

  private static class ParameterWrapper {

    private final UserParameter<?, ?> parameter;

    public ParameterWrapper(UserParameter<?, ?> parameter) {
      this.parameter = parameter;
    }

    @Override
    public String toString() {
      return parameter.getName();
    }
  }
}