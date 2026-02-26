/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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
import io.github.mzmine.javafx.components.factories.TableColumns;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.batchmode.BatchModuleTreePane;
import io.github.mzmine.modules.tools.batchwizard.subparameters.ApplicationScope;
import io.github.mzmine.modules.tools.batchwizard.subparameters.ParameterOverride;
import io.github.mzmine.parameters.UserParameter;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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

/**
 * MVCI view builder for the parameter customization pane. Creates all UI components, binds them to
 * the {@link ParameterCustomizationModel}, and wires user-action callbacks to the
 * {@link ParameterCustomizationController}.
 */
public class ParameterCustomizationViewBuilder extends FxViewBuilder<ParameterCustomizationModel> {

  private static final double MIN_OVERRIDES_TABLE_HEIGHT = 60d;
  private static final double PREF_OVERRIDES_TABLE_HEIGHT = 140d;

  // Callbacks to the controller
  private final Consumer<MZmineRunnableModule> onModuleSelected;
  private final Consumer<UserParameter<?, ?>> onParameterSelected;
  private final Runnable onAdd;
  private final Runnable onUpdate;
  private final Runnable onRemove;
  private final Runnable onClear;
  private final BiConsumer<ParameterOverride, ApplicationScope> onChangeScope;
  private final Consumer<ParameterOverride> onOverrideSelected;
  private final Consumer<KeyEvent> onParameterListKeyPressed;

  // View-local state for the one-time parent-size layout pass
  private boolean initialSelectorLayoutApplied;
  private int initialSelectorLayoutAttempts;

  // Flag to prevent feedback loops when syncing model → table selection
  private boolean updatingTableSelectionFromModel;

  ParameterCustomizationViewBuilder(ParameterCustomizationModel model,
      Consumer<MZmineRunnableModule> onModuleSelected,
      Consumer<UserParameter<?, ?>> onParameterSelected, Runnable onAdd, Runnable onUpdate,
      Runnable onRemove, Runnable onClear,
      BiConsumer<ParameterOverride, ApplicationScope> onChangeScope,
      Consumer<ParameterOverride> onOverrideSelected,
      Consumer<KeyEvent> onParameterListKeyPressed) {
    super(model);
    this.onModuleSelected = onModuleSelected;
    this.onParameterSelected = onParameterSelected;
    this.onAdd = onAdd;
    this.onUpdate = onUpdate;
    this.onRemove = onRemove;
    this.onClear = onClear;
    this.onChangeScope = onChangeScope;
    this.onOverrideSelected = onOverrideSelected;
    this.onParameterListKeyPressed = onParameterListKeyPressed;
  }

  @Override
  public Region build() {
    initialSelectorLayoutApplied = false;
    initialSelectorLayoutAttempts = 0;
    updatingTableSelectionFromModel = false;

    // --- Build sub-sections ---
    BatchModuleTreePane moduleTreePane = new BatchModuleTreePane(false);
    ListView<UserParameter<?, ?>> parameterListView = buildParameterListView();
    VBox parameterEditorColumn = FxLayout.newVBox();
    Label instructionsLabel = FxLabels.newLabel(model.getInstructionsText());

    Button addButton = FxButtons.createButton("Add", FxIcons.ADD, "Add new parameter override",
        onAdd::run);
    Button overrideButton = FxButtons.createButton("Override", FxIcons.EDIT,
        "Update existing parameter override", onUpdate::run);
    Button removeButton = FxButtons.createButton("Remove", FxIcons.CANCEL,
        "Remove selected override", onRemove::run);
    Button clearAllButton = FxButtons.createButton("Clear All", FxIcons.CLEAR,
        "Clear all parameter overrides", onClear::run);
    ComboBox<ApplicationScope> scopeComboBox = new ComboBox<>();
    scopeComboBox.getItems().addAll(ApplicationScope.values());
    scopeComboBox.setMaxWidth(Double.MAX_VALUE);

    TableView<ParameterOverride> overridesTable = new TableView<>();

    // --- Wire tree → controller ---
    moduleTreePane.addModuleFocusedListener(onModuleSelected::accept);
    moduleTreePane.setOnAddModuleEventHandler(_ -> {
      parameterListView.requestFocus();
      parameterListView.getSelectionModel().selectFirst();
    });

    // --- Model → view: scroll module tree ---
    model.scrollToModuleRequestProperty().subscribe(moduleClass -> {
      if (moduleClass != null) {
        if (!moduleTreePane.selectModuleAndScroll(moduleClass)) {
          // Fallback: module not in tree, still notify controller with the last known module.
          MZmineRunnableModule instance = MZmineCore.getModuleInstance(moduleClass);
          if (instance != null) {
            onModuleSelected.accept(instance);
          }
        }
      }
    });

    // --- Model → view: select parameter by name ---
    model.parameterNameToSelectProperty().subscribe(name -> {
      if (name != null) {
        for (UserParameter<?, ?> param : parameterListView.getItems()) {
          if (param.getName().equals(name)) {
            parameterListView.getSelectionModel().select(param);
            break;
          }
        }
      }
    });

    // --- Model → parameterListView items ---
    model.getAvailableParameters().addListener(
        (ListChangeListener<UserParameter<?, ?>>) _ -> parameterListView.getItems()
            .setAll(model.getAvailableParameters()));

    // --- Wire parameter list selection → controller ---
    parameterListView.getSelectionModel().selectedItemProperty()
        .addListener((obs, oldVal, newVal) -> onParameterSelected.accept(newVal));
    parameterListView.addEventHandler(KeyEvent.KEY_PRESSED, onParameterListKeyPressed::accept);

    // --- Scope ComboBox ↔ model (bidirectional) ---
    Bindings.bindBidirectional(scopeComboBox.valueProperty(), model.selectedScopeProperty());

    // --- Button disabled states bound to model ---
    addButton.disableProperty().bind(model.addButtonDisabledProperty());
    overrideButton.disableProperty().bind(model.overrideButtonDisabledProperty());
    removeButton.disableProperty()
        .bind(overridesTable.getSelectionModel().selectedItemProperty().isNull());

    // --- Build overrides table ---
    buildOverridesTable(overridesTable);

    // --- Wire overrides table selection → controller ---
    overridesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
      if (!updatingTableSelectionFromModel) {
        onOverrideSelected.accept(newVal);
      }
    });

    // --- Model → overrides table items ---
    model.getOverrides().addListener(
        (MapChangeListener<ParameterCustomizationModel.OverrideKey, ParameterOverride>) _ -> overridesTable.getItems()
            .setAll(model.getOverrides().values()));

    // --- Model → overrides table selection ---
    model.selectedOverrideProperty().subscribe(override -> {
      updatingTableSelectionFromModel = true;
      if (override != null) {
        overridesTable.getSelectionModel().select(override);
      } else {
        overridesTable.getSelectionModel().clearSelection();
      }
      updatingTableSelectionFromModel = false;
    });

    // --- Editor column: react to model changes ---
    model.instructionsTextProperty().subscribe(instructionsLabel::setText);
    model.showInstructionsProperty()
        .subscribe(_ -> updateEditorColumn(parameterEditorColumn, instructionsLabel));
    model.currentEditorComponentProperty()
        .subscribe(_ -> updateEditorColumn(parameterEditorColumn, instructionsLabel));

    // --- Build layout sections ---
    VBox modulePane = buildModuleTreePane(moduleTreePane);
    VBox parameterListWrapper = buildParameterListPane(parameterListView);
    VBox editorSection = buildParameterEditorSection(parameterEditorColumn, addButton,
        overrideButton, scopeComboBox);
    editorSection.setMaxHeight(Region.USE_PREF_SIZE);
    VBox overridesPane = buildOverridesPane(overridesTable, removeButton, clearAllButton);
    GridPane selectorGrid = createSelectorGrid(modulePane, parameterListWrapper, overridesPane);

    VBox editorColumn = FxLayout.newVBox(null, FxLayout.DEFAULT_PADDING_INSETS, true,
        editorSection);
    editorColumn.setAlignment(Pos.TOP_LEFT);
    VBox.setVgrow(editorSection, Priority.NEVER);

    SplitPane mainSplitPane = new SplitPane();
    mainSplitPane.setOrientation(Orientation.HORIZONTAL);
    mainSplitPane.getItems().addAll(selectorGrid, editorColumn);
    mainSplitPane.setDividerPositions(0.5);

    // Deferred one-time layout sizing based on the parent's computed bounds.
    BorderPane root = new BorderPane(mainSplitPane);

    // --- Ctrl+F focuses the module search field ---
    // Registered on root so it covers the full pane, matching original behaviour.
    root.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
      if (event.isControlDown() && event.getCode() == KeyCode.F) {
        moduleTreePane.focusSearchField();
        event.consume();
      }
    });

    root.parentProperty().subscribe(parent -> {
      if (parent != null) {
        applyParentSizeToSelectorLayoutOnce(root, selectorGrid, modulePane, parameterListWrapper,
            overridesTable);
      }
    });

    return root;
  }

  // --- Editor column update ---

  private void updateEditorColumn(VBox parameterEditorColumn, Label instructionsLabel) {
    parameterEditorColumn.getChildren().clear();
    Node editor = model.getCurrentEditorComponent();
    if (model.isShowInstructions() || editor == null) {
      parameterEditorColumn.getChildren().add(instructionsLabel);
    } else {
      parameterEditorColumn.getChildren()
          .add(buildEditorGrid(model.getSelectedParameter(), editor));
    }
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

  // --- Sub-section builders ---

  private ListView<UserParameter<?, ?>> buildParameterListView() {
    ListView<UserParameter<?, ?>> listView = new ListView<>();
    listView.setCellFactory(lv -> new ListCell<>() {
      @Override
      protected void updateItem(UserParameter<?, ?> item, boolean empty) {
        super.updateItem(item, empty);
        setText(empty || item == null ? null : item.getName());
      }
    });
    return listView;
  }

  private VBox buildModuleTreePane(BatchModuleTreePane moduleTreePane) {
    Label moduleLabel = FxLabels.newBoldLabel("Select Module:");
    VBox leftPane = FxLayout.newVBox(null, FxLayout.NO_PADDING_INSETS, true, moduleLabel,
        moduleTreePane);
    VBox.setVgrow(moduleTreePane, Priority.ALWAYS);
    return leftPane;
  }

  private VBox buildParameterListPane(ListView<UserParameter<?, ?>> parameterListView) {
    Label paramLabel = FxLabels.newBoldLabel("Parameters:");
    VBox middlePane = FxLayout.newVBox(null, FxLayout.NO_PADDING_INSETS, true, paramLabel,
        parameterListView);
    VBox.setVgrow(parameterListView, Priority.ALWAYS);
    return middlePane;
  }

  private VBox buildParameterEditorSection(VBox parameterEditorColumn, Button addButton,
      Button overrideButton, ComboBox<ApplicationScope> scopeComboBox) {
    Label editorLabel = FxLabels.newBoldLabel("Parameter Editor:");

    addButton.setMaxWidth(Double.MAX_VALUE);
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

  private void buildOverridesTable(TableView<ParameterOverride> overridesTable) {
    TableColumn<ParameterOverride, String> moduleCol = TableColumns.createColumn("Module",
        Region.USE_COMPUTED_SIZE, param -> new SimpleStringProperty(param.moduleName()));
    moduleCol.setPrefWidth(150);

    TableColumn<ParameterOverride, String> paramCol = TableColumns.createColumn("Parameter",
        Region.USE_COMPUTED_SIZE,
        param -> new SimpleObjectProperty<>(param.parameterWithValue().getName()));
    paramCol.setPrefWidth(150);

    TableColumn<ParameterOverride, String> valueCol = TableColumns.createColumn("Value",
        Region.USE_COMPUTED_SIZE, param -> new SimpleStringProperty(param.getDisplayValue()));
    valueCol.setPrefWidth(150);

    overridesTable.getColumns().addAll(moduleCol, paramCol, valueCol, createScopeColumn());
    overridesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    overridesTable.setPlaceholder(FxLabels.newLabel("No parameter overrides set"));
    overridesTable.setEditable(true);
    overridesTable.setMinWidth(0);
    overridesTable.setMinHeight(MIN_OVERRIDES_TABLE_HEIGHT);
    overridesTable.setPrefHeight(PREF_OVERRIDES_TABLE_HEIGHT);
  }

  private TableColumn<ParameterOverride, ApplicationScope> createScopeColumn() {
    TableColumn<ParameterOverride, ApplicationScope> scopeCol = TableColumns.createColumn(
        "Apply to", Region.USE_COMPUTED_SIZE, param -> new SimpleObjectProperty<>(param.scope()));
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
        onChangeScope.accept(event.getRowValue(), newScope);
      }
    });
    return scopeCol;
  }

  private VBox buildOverridesPane(TableView<ParameterOverride> overridesTable, Button removeButton,
      Button clearAllButton) {
    Label overviewLabel = FxLabels.newBoldLabel("Parameter Overrides:");
    Region headerSpacer = new Region();
    HBox.setHgrow(headerSpacer, Priority.ALWAYS);
    HBox overridesHeader = FxLayout.newHBox(Pos.CENTER_LEFT, FxLayout.NO_PADDING_INSETS,
        overviewLabel, headerSpacer, removeButton, clearAllButton);
    VBox overridesPane = FxLayout.newVBox(null, FxLayout.NO_PADDING_INSETS, true, overridesHeader,
        overridesTable);
    VBox.setVgrow(overridesTable, Priority.ALWAYS);
    return overridesPane;
  }

  private static GridPane createSelectorGrid(VBox modulePane, VBox parameterPane,
      VBox overridesPane) {
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
    grid.add(overridesPane, 0, 1, 2, 1);

    GridPane.setHgrow(modulePane, Priority.ALWAYS);
    GridPane.setHgrow(parameterPane, Priority.ALWAYS);
    GridPane.setHgrow(overridesPane, Priority.ALWAYS);
    GridPane.setVgrow(modulePane, Priority.NEVER);
    GridPane.setVgrow(parameterPane, Priority.NEVER);
    GridPane.setVgrow(overridesPane, Priority.ALWAYS);
    return grid;
  }

  // --- Layout helpers ---

  private void applyParentSizeToSelectorLayoutOnce(Parent root, GridPane selectorGrid,
      VBox modulePane, VBox parameterListWrapper, TableView<ParameterOverride> overridesTable) {
    if (initialSelectorLayoutApplied) {
      return;
    }
    final Parent parent = root.getParent();
    if (parent == null) {
      return;
    }
    final double parentWidth = parent.getLayoutBounds().getWidth();
    final double parentHeight = parent.getLayoutBounds().getHeight();
    if (parentWidth <= 0 || parentHeight <= 0) {
      if (initialSelectorLayoutAttempts++ < 10) {
        Platform.runLater(() -> applyParentSizeToSelectorLayoutOnce(root, selectorGrid, modulePane,
            parameterListWrapper, overridesTable));
      }
      return;
    }
    initialSelectorLayoutApplied = true;
    computeAndApplySelectorLayout(selectorGrid, modulePane, parameterListWrapper, overridesTable,
        parentWidth, parentHeight);
  }

  private void computeAndApplySelectorLayout(GridPane selectorGrid, VBox modulePane,
      VBox parameterListWrapper, TableView<ParameterOverride> overridesTable, double parentWidth,
      double parentHeight) {
    double leftWidth = parentWidth * 0.5;
    double columnWidth = Math.max(140d, (leftWidth - selectorGrid.getHgap()) / 2d);
    modulePane.setPrefWidth(columnWidth);
    parameterListWrapper.setPrefWidth(columnWidth);

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
    parameterListWrapper.setPrefHeight(topPaneHeight);
    parameterListWrapper.setMaxHeight(topPaneHeight);
  }
}
