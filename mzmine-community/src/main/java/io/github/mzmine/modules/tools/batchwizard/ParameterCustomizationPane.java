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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * UI pane for customizing batch module parameters. This is the editor component for
 * ParameterOverridesParameter.
 */
public class ParameterCustomizationPane extends BorderPane {

  private static final double MIN_OVERRIDES_TABLE_HEIGHT = 60d;
  private static final double PREF_OVERRIDES_TABLE_HEIGHT = 140d;

  // Type-safe composite key for tempOverrides, replacing fragile string concatenation.
  private record OverrideKey(String moduleClassName, String paramName, ApplicationScope scope) {

    OverrideKey(@NotNull MZmineRunnableModule module, @NotNull String paramName,
        @NotNull ApplicationScope scope) {
      this(module.getClass().getName(), paramName, scope);
    }
  }

  // --- UI fields ---
  private final BatchModuleTreePane moduleTreePane = new BatchModuleTreePane(false);
  private final ListView<UserParameter<?, ?>> parameterListView = new ListView<>();
  private final VBox parameterEditorColumn = FxLayout.newVBox();
  private final Label instructionsLabel = FxLabels.newLabel(
      "Select a module to view its parameters");
  private final TableView<ParameterOverride> overridesTable = new TableView<>();
  private final Button addButton;
  private final Button overrideButton;
  private final Button removeButton;
  private final Button clearAllButton;
  private final ComboBox<ApplicationScope> scopeComboBox = new ComboBox<>();

  // --- State fields ---
  private final Map<OverrideKey, ParameterOverride> tempOverrides = new LinkedHashMap<>();
  private MZmineRunnableModule selectedModule;
  private UserParameter<?, ?> selectedParameter;
  private Node currentEditorComponent;
  private boolean initialSelectorLayoutApplied;
  private int initialSelectorLayoutAttempts;

  public ParameterCustomizationPane() {

    this.addButton = FxButtons.createButton("Add", FxIcons.ADD, "Add new parameter override",
        this::addParameterOverride);
    this.overrideButton = FxButtons.createButton("Override", FxIcons.EDIT,
        "Update existing parameter override", this::updateParameterOverride);
    this.removeButton = FxButtons.createButton("Remove", FxIcons.CANCEL, "Remove selected override",
        this::removeSelectedOverride);
    this.clearAllButton = FxButtons.createButton("Clear All", FxIcons.CLEAR,
        "Clear all parameter overrides", this::clearAllOverrides);

    this.scopeComboBox.getItems().addAll(ApplicationScope.values());
    this.scopeComboBox.setValue(ApplicationScope.ALL);
    this.scopeComboBox.setMaxWidth(Double.MAX_VALUE);

    setupUI();
    setupEventHandlers();
  }

  private void setupUI() {
    SplitPane mainSplitPane = new SplitPane();
    mainSplitPane.setOrientation(Orientation.HORIZONTAL);

    VBox modulePane = createModuleTreePane();
    VBox parameterPane = createParameterListPane();
    VBox editorSection = createParameterEditorSection();
    editorSection.setMaxHeight(Region.USE_PREF_SIZE);
    VBox overridesPane = createOverridesPane();
    GridPane selectorGrid = createSelectorGrid(modulePane, parameterPane, overridesPane);
    VBox editorColumn = FxLayout.newVBox(null, FxLayout.DEFAULT_PADDING_INSETS, true,
        editorSection);
    editorColumn.setAlignment(Pos.TOP_LEFT);
    VBox.setVgrow(editorSection, Priority.NEVER);

    mainSplitPane.getItems().addAll(selectorGrid, editorColumn);
    mainSplitPane.setDividerPositions(0.5);

    setCenter(mainSplitPane);
    applyInitialSelectorLayoutOnce(selectorGrid, modulePane, parameterPane);
  }

  private GridPane createSelectorGrid(VBox modulePane, VBox parameterPane, VBox overridesPane) {
    GridPane grid = new GridPane();
    grid.setPadding(FxLayout.DEFAULT_PADDING_INSETS);
    grid.setHgap(FxLayout.DEFAULT_SPACE);
    grid.setVgap(FxLayout.DEFAULT_SPACE);

    ColumnConstraints col1 = new ColumnConstraints();
    col1.setPercentWidth(50);
    col1.setHgrow(Priority.ALWAYS);
    ColumnConstraints col2 = new ColumnConstraints();
    col2.setPercentWidth(50);
    col2.setHgrow(Priority.ALWAYS);
    grid.getColumnConstraints().addAll(col1, col2);

    grid.add(modulePane, 0, 0);
    grid.add(parameterPane, 1, 0);
    // Override controls span both selector columns.
    grid.add(overridesPane, 0, 1, 2, 1);

    GridPane.setHgrow(modulePane, Priority.ALWAYS);
    GridPane.setHgrow(parameterPane, Priority.ALWAYS);
    GridPane.setHgrow(overridesPane, Priority.ALWAYS);
    // Keep top selector panes at their computed preferred heights.
    GridPane.setVgrow(modulePane, Priority.NEVER);
    GridPane.setVgrow(parameterPane, Priority.NEVER);
    // Let overrides absorb any extra vertical space in the selector column.
    GridPane.setVgrow(overridesPane, Priority.ALWAYS);

    return grid;
  }

  private void applyInitialSelectorLayoutOnce(GridPane selectorGrid, VBox modulePane,
      VBox parameterPane) {
    if (initialSelectorLayoutApplied) {
      return;
    }

    if (getParent() != null) {
      Platform.runLater(
          () -> applySelectorLayoutFromParent(selectorGrid, modulePane, parameterPane));
      return;
    }

    ChangeListener<Parent> parentReadyListener = new ChangeListener<>() {
      @Override
      public void changed(javafx.beans.value.ObservableValue<? extends Parent> observable,
          Parent oldValue, Parent newValue) {
        if (newValue == null) {
          return;
        }
        parentProperty().removeListener(this);
        Platform.runLater(
            () -> applySelectorLayoutFromParent(selectorGrid, modulePane, parameterPane));
      }
    };
    parentProperty().addListener(parentReadyListener);
  }

  private void applySelectorLayoutFromParent(GridPane selectorGrid, VBox modulePane,
      VBox parameterPane) {
    if (initialSelectorLayoutApplied) {
      return;
    }

    final Parent parent = getParent();
    if (parent == null) {
      return;
    }

    final double parentWidth = parent.getLayoutBounds().getWidth();
    final double parentHeight = parent.getLayoutBounds().getHeight();
    if (parentWidth <= 0 || parentHeight <= 0) {
      if (initialSelectorLayoutAttempts++ < 10) {
        Platform.runLater(
            () -> applySelectorLayoutFromParent(selectorGrid, modulePane, parameterPane));
      }
      return;
    }

    initialSelectorLayoutApplied = true;
    computeAndApplySelectorLayout(selectorGrid, modulePane, parameterPane, parentWidth,
        parentHeight);
  }

  private void computeAndApplySelectorLayout(GridPane selectorGrid, VBox modulePane,
      VBox parameterPane, double parentWidth, double parentHeight) {
    // Outer split starts at 50/50. Size selector columns from the left half.
    double leftWidth = parentWidth * 0.5;
    double columnWidth = Math.max(140d, (leftWidth - selectorGrid.getHgap()) / 2d);
    modulePane.setPrefWidth(columnWidth);
    parameterPane.setPrefWidth(columnWidth);

    double paddingHeight =
        selectorGrid.getPadding().getTop() + selectorGrid.getPadding().getBottom();
    double topToBottomGap = selectorGrid.getVgap();
    double availableHeight = Math.max(0d, parentHeight - paddingHeight - topToBottomGap);
    double topPaneHeight = Math.max(120d, Math.min(240d, availableHeight * 0.45d));
    double tableHeight = Math.max(MIN_OVERRIDES_TABLE_HEIGHT,
        Math.min(PREF_OVERRIDES_TABLE_HEIGHT, availableHeight - topPaneHeight));
    if (topPaneHeight + tableHeight > availableHeight) {
      topPaneHeight = Math.max(120d, availableHeight - tableHeight);
    }

    overridesTable.setPrefHeight(tableHeight);
    modulePane.setPrefHeight(topPaneHeight);
    modulePane.setMaxHeight(topPaneHeight);
    parameterPane.setPrefHeight(topPaneHeight);
    parameterPane.setMaxHeight(topPaneHeight);
  }

  private VBox createModuleTreePane() {
    Label moduleLabel = FxLabels.newBoldLabel("Select Module:");
    VBox leftPane = FxLayout.newVBox(null, FxLayout.NO_PADDING_INSETS, true, moduleLabel,
        moduleTreePane);
    leftPane.setMinWidth(0);
    VBox.setVgrow(moduleTreePane, Priority.ALWAYS);
    return leftPane;
  }

  private VBox createParameterListPane() {
    Label paramLabel = FxLabels.newBoldLabel("Parameters:");
    parameterListView.setCellFactory(lv -> new ListCell<>() {
      @Override
      protected void updateItem(UserParameter<?, ?> item, boolean empty) {
        super.updateItem(item, empty);
        setText(empty || item == null ? null : item.getName());
      }
    });
    VBox middlePane = FxLayout.newVBox(null, FxLayout.NO_PADDING_INSETS, true, paramLabel,
        parameterListView);
    middlePane.setMinWidth(0);
    VBox.setVgrow(parameterListView, Priority.ALWAYS);
    return middlePane;
  }

  private VBox createParameterEditorSection() {
    Label editorLabel = FxLabels.newBoldLabel("Parameter Editor:");

    parameterEditorColumn.getChildren().add(instructionsLabel);

    addButton.setDisable(true);
    addButton.setMaxWidth(Double.MAX_VALUE);
    overrideButton.setDisable(true);
    overrideButton.setMaxWidth(Double.MAX_VALUE);

    Label scopeLabel = FxLabels.newLabel("Apply to module occurrence:");
    scopeLabel.setTooltip(new Tooltip(
        "Select if this parameter setting should be applied to the first, last, or all batch steps that use this module."));
    HBox scopeBox = FxLayout.newHBox(Pos.CENTER_LEFT, FxLayout.NO_PADDING_INSETS, scopeLabel,
        scopeComboBox);
    HBox.setHgrow(scopeComboBox, Priority.ALWAYS);

    HBox buttonRow = FxLayout.newHBox(Pos.CENTER, FxLayout.NO_PADDING_INSETS, addButton,
        overrideButton);

    VBox buttonBox = FxLayout.newVBox(null, FxLayout.NO_PADDING_INSETS, true, scopeBox, buttonRow);

    VBox editorSection = FxLayout.newVBox(null, FxLayout.NO_PADDING_INSETS, true, editorLabel,
        buttonBox, new Separator(Orientation.HORIZONTAL), parameterEditorColumn);
    VBox.setVgrow(parameterEditorColumn, Priority.NEVER);

    return editorSection;
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

    overridesTable.getColumns().addAll(moduleCol, paramCol, valueCol, createScopeColumn());
    overridesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    overridesTable.setPlaceholder(FxLabels.newLabel("No parameter overrides set"));
    overridesTable.setEditable(true);
    // Allow columns to compress when horizontal space is limited.
    overridesTable.setMinWidth(0);
    // Keep the table usable while still allowing vertical compression.
    overridesTable.setMinHeight(MIN_OVERRIDES_TABLE_HEIGHT);
    overridesTable.setPrefHeight(PREF_OVERRIDES_TABLE_HEIGHT);

    Region headerSpacer = new Region();
    HBox.setHgrow(headerSpacer, Priority.ALWAYS);
    HBox overridesHeader = FxLayout.newHBox(Pos.CENTER_LEFT, FxLayout.NO_PADDING_INSETS,
        overviewLabel, headerSpacer, removeButton, clearAllButton);

    VBox overridesPane = FxLayout.newVBox(null, FxLayout.NO_PADDING_INSETS, true, overridesHeader,
        overridesTable);
    VBox.setVgrow(overridesTable, Priority.ALWAYS);

    return overridesPane;
  }

  private TableColumn<ParameterOverride, ApplicationScope> createScopeColumn() {
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
      ApplicationScope newScope = event.getNewValue();
      if (newScope != null && newScope != event.getRowValue().scope()) {
        changeOverrideScope(event.getRowValue(), newScope);
      }
    });
    return scopeCol;
  }

  private void changeOverrideScope(@NotNull ParameterOverride override,
      @NotNull ApplicationScope newScope) {
    final String moduleClass = override.moduleClassName();
    final String paramName = override.parameterWithValue().getName();
    tempOverrides.remove(new OverrideKey(moduleClass, paramName, override.scope()));
    tempOverrides.put(new OverrideKey(moduleClass, paramName, newScope),
        new ParameterOverride(moduleClass, override.moduleName(), override.parameterWithValue(),
            newScope));
    refreshOverridesTable();
  }

  private void setupEventHandlers() {
    moduleTreePane.addModuleFocusedListener(this::onModuleSelected);
    moduleTreePane.setOnAddModuleEventHandler(_ -> {
      // select and focus on enter pressed
      parameterListView.requestFocus();
      parameterListView.getSelectionModel().selectFirst();
    });

    parameterListView.getSelectionModel().selectedItemProperty()
        .addListener((obs, oldVal, newVal) -> onParameterSelected(newVal));
    parameterListView.addEventHandler(KeyEvent.KEY_PRESSED, this::parameterListViewOnKeyPressed);

    removeButton.disableProperty().bind(
        overridesTable.getSelectionModel().selectedItemProperty().isNull());

    overridesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
      if (newVal != null) {
        selectOverrideInEditor(newVal);
      }
      updateButtonStates();
    });

    // When scope changes, reload the parameter value for that scope
    scopeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
      if (selectedParameter != null && newVal != null) {
        loadExistingOverrideValue(selectedParameter);
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
  }

  private void parameterListViewOnKeyPressed(KeyEvent e) {
    switch (e.getCode()) {
      case ENTER -> {
        Control c = switch (currentEditorComponent) {
          case Control control -> control;
          case Parent p -> findFirstControlNode(p);
          case null, default -> null;
        };
        if (c != null) {
          e.consume();
          c.requestFocus();
          // add override on enter pressed
          c.addEventHandler(KeyEvent.KEY_PRESSED, ev -> {
            if(ev.getCode() == KeyCode.ENTER) {
              addParameterOverride();
            }
          });
        }
      }
      case null, default -> {
      }
    }

  }

  /**
   * Traverses the parent and it's childs to find the first intance of a {@link Control}.
   */
  private @Nullable Control findFirstControlNode(@NotNull Parent p) {
    ObservableList<Node> children = p.getChildrenUnmodifiable();
    // check main layer first
    for (Node child : children) {
      if (child instanceof Control c) {
        return c;
      }
    }

    for (Node child : children) {
      if (child instanceof Parent cp) {
        Control c = findFirstControlNode(cp);
        if (c != null) {
          return c;
        }
      }
    }
    return null;
  }

  private void onModuleSelected(@Nullable MZmineRunnableModule module) {
    this.selectedModule = module;
    this.selectedParameter = null;
    this.currentEditorComponent = null;
    parameterListView.getItems().clear();
    updateButtonStates();

    if (module == null) {
      onParameterSelected(null);
      return;
    }

    // important: clone the parameter set before loading it in here
    ParameterSet parameters = ConfigService.getConfiguration()
        .getModuleParameters(module.getClass()).cloneParameterSet();
    if (parameters == null) {
      showInstructions("No parameters available for this module");
      return;
    }

    Arrays.stream(parameters.getParameters())
        .filter(p -> p instanceof UserParameter<?, ?> && !(p instanceof HiddenParameter))
        .map(p -> (UserParameter<?, ?>) p).forEach(parameterListView.getItems()::add);

    if (parameterListView.getItems().isEmpty()) {
      showInstructions("No user-configurable parameters for this module");
    } else {
      showInstructions("Select a parameter to edit its value");
    }
  }

  private void onParameterSelected(@Nullable UserParameter<?, ?> parameter) {
    parameterEditorColumn.getChildren().clear();
    if (parameter != null && selectedModule != null) {
      this.selectedParameter = parameter;
      currentEditorComponent = createEditorForParameter(parameter);
      if (currentEditorComponent != null) {
        parameterEditorColumn.getChildren().add(buildEditorGrid(parameter, currentEditorComponent));
        loadExistingOverrideValue(parameter);
      } else {
        showInstructions("Cannot create editor for this parameter type");
      }
    }
    updateButtonStates();
  }

  private GridPane buildEditorGrid(UserParameter<?, ?> parameter, Node editorComponent) {
    GridPane grid = new GridPane();
    grid.setHgap(FxLayout.DEFAULT_SPACE);
    grid.setVgap(FxLayout.DEFAULT_SPACE);
    grid.setPadding(FxLayout.DEFAULT_PADDING_INSETS);

    Label nameLabel = FxLabels.newBoldLabel(parameter.getName() + ":");
    String description = parameter.getDescription();
    if (description != null && !description.isBlank()) {
      nameLabel.setTooltip(new Tooltip(description));
    }

    grid.add(nameLabel, 0, 0);
    grid.add(editorComponent, 0, 1);
    GridPane.setHgrow(editorComponent, Priority.ALWAYS);
    return grid;
  }

  @SuppressWarnings("unchecked")
  private <T, C extends Node> @Nullable Node createEditorForParameter(
      UserParameter<?, ?> parameter) {
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
    ParameterOverride temp = tempOverrides.get(
        new OverrideKey(selectedModule, parameter.getName(), currentScope));
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

  private void addParameterOverride() {
    putParameterOverride("Parameter override added");
  }

  private void updateParameterOverride() {
    putParameterOverride("Parameter override updated");
  }

  @SuppressWarnings("unchecked")
  private void putParameterOverride(String actionLabel) {
    if (selectedParameter == null || selectedModule == null || currentEditorComponent == null) {
      return;
    }
    try {
      UserParameter<Object, Node> param = (UserParameter<Object, Node>) selectedParameter;
      param.setValueFromComponent(currentEditorComponent);

      String moduleClassName = selectedModule.getClass().getName();
      String moduleName = getModuleName(selectedModule.getClass());
      ApplicationScope scope = scopeComboBox.getValue();
      final ParameterOverride added = new ParameterOverride(moduleClassName, moduleName,
          param.cloneParameter(), scope);
      tempOverrides.put(new OverrideKey(selectedModule, param.getName(), scope), added);

      refreshOverridesTable();
      overridesTable.getSelectionModel().select(added);
      showInstructions(
          actionLabel + ": " + param.getName() + " (apply to: " + scope.getLabel() + ")");
    } catch (Exception e) {
      showInstructions("Error: " + e.getMessage());
    }
  }

  private void removeSelectedOverride() {
    ParameterOverride selected = overridesTable.getSelectionModel().getSelectedItem();
    if (selected == null) {
      return;
    }
    tempOverrides.remove(
        new OverrideKey(selected.moduleClassName(), selected.parameterWithValue().getName(),
            selected.scope()));
    refreshOverridesTable();
  }

  private void clearAllOverrides() {
    tempOverrides.clear();
    refreshOverridesTable();
    showInstructions("All parameter overrides cleared");
  }

  private void refreshOverridesTable() {
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
    parameterEditorColumn.getChildren().clear();
    instructionsLabel.setText(message);
    parameterEditorColumn.getChildren().add(instructionsLabel);
    updateButtonStates();
  }

  /**
   * Updates the enabled state of Add and Override buttons based on current selection.
   */
  private void updateButtonStates() {
    final boolean hasParameterSelected =
        selectedParameter != null && selectedModule != null && currentEditorComponent != null;

    // Add button is enabled when a parameter is selected
    addButton.setDisable(!hasParameterSelected);

    // Override button is enabled when a parameter is selected and an override already exists
    // for the current module+parameter+scope combination
    final boolean overrideExists = hasParameterSelected && tempOverrides.containsKey(
        new OverrideKey(selectedModule, selectedParameter.getName(), scopeComboBox.getValue()));
    overrideButton.setDisable(!overrideExists);
  }

  /**
   * Selects the module and parameter in the editor when an override is clicked in the table.
   */
  @SuppressWarnings("unchecked")
  private void selectOverrideInEditor(ParameterOverride override) {
    MZmineRunnableModule targetModule = null;
    Class<? extends MZmineRunnableModule> targetModuleType = null;

    try {
      Class<?> moduleClass = Class.forName(override.moduleClassName());
      if (MZmineRunnableModule.class.isAssignableFrom(moduleClass)) {
        targetModuleType = (Class<? extends MZmineRunnableModule>) moduleClass;
        targetModule = MZmineCore.getModuleInstance(targetModuleType);
      }
    } catch (Exception e) {
      // Module not found
      return;
    }

    if (targetModule != null) {
      // Sync module tree selection and scroll so the current module context is visible.
      if (!moduleTreePane.selectModuleAndScroll(targetModuleType)) {
        // Fallback if tree lookup failed.
        onModuleSelected(targetModule);
      }

      // Select the parameter in the list
      String targetParamName = override.parameterWithValue().getName();
      for (UserParameter<?, ?> param : parameterListView.getItems()) {
        if (param.getName().equals(targetParamName)) {
          parameterListView.getSelectionModel().select(param);
          break;
        }
      }

      // Set the scope
      scopeComboBox.setValue(override.scope());
    }
  }

  /**
   * Get all parameter overrides as a list.
   */
  public List<ParameterOverride> getParameterOverrides() {
    return new ArrayList<>(tempOverrides.values());
  }

  /**
   * Set parameter overrides from a list.
   */
  public void setParameterOverrides(List<ParameterOverride> overrides) {
    tempOverrides.clear();
    for (ParameterOverride override : overrides) {
      tempOverrides.put(
          new OverrideKey(override.moduleClassName(), override.parameterWithValue().getName(),
              override.scope()), override);
    }
    refreshOverridesTable();
  }
}
