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

import static io.github.mzmine.modules.tools.batchwizard.WizardPart.WORKFLOW;

import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.javafx.validation.FxValidation;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.batchmode.BatchModeModule;
import io.github.mzmine.modules.batchmode.BatchModeParameters;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.tools.batchwizard.WizardParameterChanges.Source;
import io.github.mzmine.modules.tools.batchwizard.io.LocalWizardSequenceFile;
import io.github.mzmine.modules.tools.batchwizard.io.WizardSequenceIOUtils;
import io.github.mzmine.modules.tools.batchwizard.io.WizardSequenceSaveModule;
import io.github.mzmine.modules.tools.batchwizard.subparameters.CustomizationWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.DataImportWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassSpectrometerWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonMobilityWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.MassSpectrometerWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WorkflowWizardParameterFactory;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.PresetChange;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.PresetSelection;
import io.github.mzmine.modules.visualization.projectmetadata.extract.SampleMetadataExtractionParameters;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.dialogs.ParameterSetupPane;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesComponent;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.javafx.FxMenuUtil;
import io.github.mzmine.util.javafx.MZmineIconUtils;
import io.mzio.links.MzioMZmineLinks;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TabPane.TabDragPolicy;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.util.Subscription;
import org.controlsfx.control.ToggleSwitch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class BatchWizardTab extends SimpleTab {

  /**
   * needs to use the same preset object, as its also used in the combo boxes and in other places
   */
  private final Map<WizardPart, List<WizardStepParameters>> ALL_PRESETS;
  /**
   * The selected sequence. first - last step. Changes in the combobox selection are reflected here
   */
  private final WizardSequence sequenceSteps = new WizardSequence();
  /**
   * Parameter panes of the selected presets
   */
  private final Map<WizardStepParameters, @NotNull ParameterSetupPane> paramPaneMap = new HashMap<>();
  private final List<Subscription> paramPaneSubscriptions = new ArrayList<>();
  /**
   * Remove the highlights of {@link #parameterChanges} from the current panes
   */
  private final List<Subscription> changeHighlightSubscriptions = new ArrayList<>();
  private final Map<WizardPart, ComboBox<WizardStepParameters>> combos = new HashMap<>();
  /**
   * Load and save presets and apply local presets
   */
  private final MenuButton presetsMenu = new MenuButton("Presets");
  private final SimpleBooleanProperty advancedMode = new SimpleBooleanProperty(false);
  /**
   * Parameter estimation and optimization from the representative files
   */
  private final WizardAutoParamActions autoParamActions = new WizardAutoParamActions(this);
  /**
   * Parameters changed by estimation or optimization. Highlighted until overridden again or a batch
   * is created.
   */
  private @NotNull WizardParameterChanges parameterChanges = WizardParameterChanges.empty();
  private boolean listenersActive = true;
  private TabPane tabPane;
  private HBox schemaPane;

  public BatchWizardTab() {
    super("mzwizard");
//    setGraphic(LightAndDarkModeIcon.mzwizardImageTab(200, 18));
    ALL_PRESETS = WizardStepParameters.createAllPresets();
    createContentPane();
    findAllLocalPresetFiles();
    // reset to mzmine default presets (loading the local presets have changed the parameters already once)
    ALL_PRESETS.values().stream().flatMap(Collection::stream)
        .forEach(WizardStepParameters::resetToDefaults);
  }

  private static void updateMetadataSelectedFiles(
      @NotNull final SampleMetadataExtractionParameters metadataParameters,
      @NotNull final FileNamesComponent fileNamesComponent) {
    metadataParameters.setSelectedFiles(fileNamesComponent.getValue());
  }

  private static void addCheckboxToCustomizationTabHeader(
      CustomizationWizardParameters customizationParams, ParameterSetupPane paramPane, Tab tab) {
    final CheckBox enableCheckBox = new CheckBox();
    final boolean customEnabled = customizationParams.getValue(
        CustomizationWizardParameters.enabled);
    enableCheckBox.setSelected(customEnabled);

    // Bind checkbox to parameter and pane disabled state
    enableCheckBox.selectedProperty().addListener((_, _, newVal) -> {
      customizationParams.setParameter(CustomizationWizardParameters.enabled, newVal);
      paramPane.setDisable(!newVal);
    });
    // Set initial disabled state after the pane is added to scene graph
    Platform.runLater(() -> paramPane.setDisable(!customEnabled));
    tab.setGraphic(enableCheckBox);
  }

  /// caption above the combo box. min and pref width 0 so the column width is defined by the combo
  /// box and long captions wrap instead of widening the column
  private static @NonNull Label generateCaptionLabel(WizardPart part) {
    final Label caption = FxLabels.wrap(FxLabels.newBoldLabel(part.caption()));
    caption.setTooltip(new Tooltip(part.tooltip()));
    caption.setMinWidth(0);
    caption.setPrefWidth(0);
    caption.setMaxWidth(Double.MAX_VALUE);
    caption.setAlignment(Pos.CENTER);
    caption.setTextAlignment(TextAlignment.CENTER);
    GridPane.setValignment(caption, VPos.BOTTOM);
    return caption;
  }

  private void createContentPane() {
    // top menu with selections
    var topPane = createTopMenu();
    // center parameter panes
    tabPane = new TabPane();
    tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
    tabPane.setTabDragPolicy(TabDragPolicy.FIXED);
    final BorderPane centerPane = new BorderPane(new StackPane(tabPane, createTabHeaderActions()));
    var centerScroll = new ScrollPane(centerPane);
    centerScroll.setFitToWidth(true);
    centerScroll.setFitToHeight(true);
    createParameterPanes();
    var mainPane = new BorderPane(centerScroll);
    mainPane.setTop(topPane);
    setContent(mainPane);
  }

  /**
   * Called once any part in the workflow changes the preset, e.g., HPLC - GC-EI
   */
  private synchronized void createParameterPanes() {
    schemaPane.getChildren().clear();
    paramPaneSubscriptions.forEach(Subscription::unsubscribe);
    paramPaneSubscriptions.clear();
    // old panes are discarded, highlights are recreated from parameterChanges
    changeHighlightSubscriptions.clear();
    paramPaneMap.clear();
    int selectedIndex = tabPane.getSelectionModel().getSelectedIndex();
    // evaluate workflow and limit choices
    evaluateWizardSequenceLimitChoices();

    // create parameters for all parts
    // LC/GC - IMS? - MS instrument, Apply defaults
    // hide customization tab unless advanced mode is enabled
    Tab[] panes = sequenceSteps.stream()
        .filter(step -> !(step instanceof CustomizationWizardParameters) || advancedMode.get())
        .map(this::createParameterTab).filter(Objects::nonNull).toArray(Tab[]::new);

    // add to center pane
    tabPane.getTabs().clear();
    tabPane.getTabs().addAll(panes);
    tabPane.getSelectionModel().select(selectedIndex);
  }

  /**
   * {@link WizardPart#ION_INTERFACE} limits {@link WizardPart#IMS}
   * <p>
   * {@link WizardPart#ION_INTERFACE} limits {@link WizardPart#WORKFLOW}
   * <p>
   * {@link WizardPart#IMS} limits {@link WizardPart#MS}
   */
  private void evaluateWizardSequenceLimitChoices() {
    var ionization = sequenceSteps.get(WizardPart.ION_INTERFACE)
        .map(step -> (IonInterfaceWizardParameterFactory) step.getFactory())
        .orElse(IonInterfaceWizardParameterFactory.HPLC);

    // apply filters
    filterComboBox(WizardPart.IMS, WizardPartFilter.allow(ionization.getMatchingImsPresets()));
    filterWorkflows(sequenceSteps);

    // check timsTOF and TWIMS TOF only
    IonMobilityWizardParameterFactory ims = sequenceSteps.get(WizardPart.IMS)
        .map(step -> (IonMobilityWizardParameterFactory) step.getFactory())
        .orElse(IonMobilityWizardParameterFactory.NO_IMS);

    filterComboBox(WizardPart.MS, WizardPartFilter.allow(ims.getMatchingMassSpectrometerPresets()));

    // after import or applying a partial sequence (changing multiple steps at once) it is important to select the correct item
    ensureComboBoxSelection();
  }

  private void ensureComboBoxSelection() {
    // select the correct options
    for (var preset : sequenceSteps) {
      ComboBox<WizardStepParameters> combo = combos.get(preset.getPart());
      if (combo != null) {
        combo.getSelectionModel().select(preset);
      }
    }
  }

  private void filterWorkflows(WizardSequence sequenceSteps) {
    final List<WizardStepParameters> availableWorkflows = ALL_PRESETS.get(WizardPart.WORKFLOW)
        .stream().filter(workflow -> {
          if (workflow instanceof WorkflowWizardParameters workflowParams) {
            return workflowParams.isApplicableToSteps(sequenceSteps);
          }
          return false;
        }).toList();
    var selected = setItemsToCombo(WORKFLOW, availableWorkflows, false);
    sequenceSteps.set(WORKFLOW, selected); // set the updated sequence
  }

  /**
   * Filter combobox and set new selection
   *
   * @param part the part
   */
  private void filterComboBox(final WizardPart part, WizardPartFilter filter) {

    List<WizardStepParameters> filteredPresets = ALL_PRESETS.get(part).stream()
        .filter(workflow -> filter.accept(workflow.getFactory())).toList();

    ComboBox<WizardStepParameters> combo = combos.get(part);
    ObservableList<WizardStepParameters> currentPresets = combo.getItems();
    if (!currentPresets.equals(filteredPresets)) {
      // need to set new selection to workflow
      var selected = setItemsToCombo(part, filteredPresets, false);
      sequenceSteps.set(part, selected);

      if (part == WizardPart.MS) {
        var ims = sequenceSteps.get(WizardPart.IMS)
            .map(step -> (IonMobilityWizardParameterFactory) step.getFactory())
            .orElse(IonMobilityWizardParameterFactory.NO_IMS);
        // reduce the parameters for timsTOF to something meaningful
        // only if the MS parameter for tof are unchanged (if user already selected other inputs, keep
        MassSpectrometerWizardParameters msParamsForIms = MassSpectrometerWizardParameterFactory.createForIms(
            ims);
        if (msParamsForIms != null && selected.hasDefaultParameters()) {
          ParameterUtils.copyParameters(msParamsForIms, selected);
        }
      }
    }
  }

  private WizardStepParameters setItemsToCombo(final WizardPart part,
      final List<WizardStepParameters> newItems, boolean notifyListeners) {
    final ComboBox<WizardStepParameters> combo = combos.get(part);

    boolean oldNotify = listenersActive;
    setListenersActive(notifyListeners);
    // keep selection or select first element if not available
    SingleSelectionModel<WizardStepParameters> selection = combo.getSelectionModel();
    // set new items
    combo.setItems(FXCollections.observableList(newItems));
    sequenceSteps.get(part).ifPresentOrElse(selection::select, selection::clearSelection);
    if (selection.getSelectedIndex() < 0) {
      selection.selectFirst();
    }
    setListenersActive(oldNotify);
    return selection.getSelectedItem();
  }

  @Nullable
  private Tab createParameterTab(final WizardStepParameters step) {
    ParameterSetupPane paramPane = new ParameterSetupPane(true, false, step);
    paramPaneMap.put(step, paramPane);
    if (step instanceof DataImportWizardParameters dataImportParameters) {
      subscribeMetadataExtractionToImportFiles(dataImportParameters, paramPane);
    }
    // add to schema
    addToSchema(step);
    // NOT add tabs without user parameters (components to set)
    if (step.hasUserParameters() && step.getFactory() != IonMobilityWizardParameterFactory.NO_IMS) {
      Tab tab = new Tab(step.getPresetName(), paramPane);

      // Special handling for customization tab - add checkbox to header
      if (step instanceof CustomizationWizardParameters customizationParams) {
        addCheckboxToCustomizationTabHeader(customizationParams, paramPane, tab);
      }
      decorateChangedParameters(step, paramPane, tab);

      return tab;
    } else {
      return null;
    }
  }

  /**
   * Marks the components of parameters that were changed by estimation or optimization with a
   * checkmark and the tab header with an icon.
   */
  private void decorateChangedParameters(@NotNull final WizardStepParameters step,
      @NotNull final ParameterSetupPane paramPane, @NotNull final Tab tab) {
    final List<WizardParameterChange> changes = parameterChanges.forPart(step.getPart());
    if (changes.isEmpty()) {
      return;
    }
    for (final WizardParameterChange change : changes) {
      final Node component = paramPane.getDecorationTarget(change.parameter());
      if (component != null) {
        final Subscription removeSubscription = FxValidation.markChanged(component,
            change.formatTooltip(parameterChanges.source().toString()),
            parameterChanges.source().icon(),
            ConfigService.getDefaultColorPalette().getPositiveColor());
        changeHighlightSubscriptions.add(removeSubscription);
      }
    }
    // customization tab header already holds the enable checkbox as graphic
    if (tab.getGraphic() == null) {
      tab.setGraphic(MZmineIconUtils.getCheckedIcon());
      tab.setTooltip(new Tooltip("Parameters changed by " + parameterChanges.source()));
      changeHighlightSubscriptions.add(() -> {
        tab.setGraphic(null);
        tab.setTooltip(null);
      });
    }
  }

  /**
   * Removes all highlights of automatically changed parameters without rebuilding the panes.
   */
  private void clearParameterChanges() {
    parameterChanges = WizardParameterChanges.empty();
    changeHighlightSubscriptions.forEach(Subscription::unsubscribe);
    changeHighlightSubscriptions.clear();
  }

  private void subscribeMetadataExtractionToImportFiles(
      @NotNull final DataImportWizardParameters dataImportParameters,
      @NotNull final ParameterSetupPane paramPane) {
    final FileNamesComponent fileNamesComponent = paramPane.getComponentForParameter(
        DataImportWizardParameters.fileNames);
    if (fileNamesComponent == null) {
      return;
    }

    final SampleMetadataExtractionParameters metadataParameters = dataImportParameters.getParameter(
        DataImportWizardParameters.extractMetadata).getEmbeddedParameters();
    paramPaneSubscriptions.add(fileNamesComponent.textProperty()
        .subscribe(_ -> updateMetadataSelectedFiles(metadataParameters, fileNamesComponent)));
  }

  /**
   * Schema for workflow in the resources directory src/main/resources/icons/wizard/
   *
   * @param preset one preset per part
   */
  private void addToSchema(final WizardStepParameters preset) {
    String parent = preset.getUniquePresetId().toLowerCase();
    try {
      LocalDate now = LocalDate.now();
      String formatPath = "icons/wizard/{0}wizard_icons_{1}.png";
      // load aprils fools day resources
      String specialSet = (now.getMonthValue() == 4 && now.getDayOfMonth() == 1) ? "april/" : "";
      final Image icon = FxIconUtil.loadImageFromResources(
          MessageFormat.format(formatPath, specialSet, parent));
      ImageView view = new ImageView(icon);
      view.setPreserveRatio(true);
      view.setFitHeight(150);

      if (MZmineCore.getConfiguration().isDarkMode()) {
        ColorAdjust whiteEffect = new ColorAdjust();
        whiteEffect.setBrightness(1.0);
        view.setEffect(whiteEffect);
        view.setCache(true);
        view.setCacheHint(CacheHint.SPEED);
      }

      schemaPane.getChildren().add(view);
    } catch (Exception ex) {
      logger.log(Level.WARNING, ex.getMessage());
    }
  }

  private Region createTopMenu() {
    VBox controlSchemaPane = new VBox(4);
    controlSchemaPane.setAlignment(Pos.CENTER);
    VBox.setMargin(controlSchemaPane, new Insets(5));

    // row 0: captions, row 1: combo boxes, separators and create batch button
    final GridPane comboBoxGrid = new GridPane(FxLayout.DEFAULT_SPACE, FxLayout.DEFAULT_SPACE);
    int column = 0;

    sequenceSteps.clear();
    combos.clear();
    // create combo boxes for each part of the wizard that has multiple options
    // LC/GC - IMS? - MS instrument, Apply defaults
    for (final WizardPart part : WizardPart.values()) {
      var presets = FXCollections.observableArrayList(ALL_PRESETS.get(part));
      if (presets.isEmpty()) {
        continue;
      }
      sequenceSteps.add(presets.getFirst());
      if (presets.size() == 1) {
        continue;
      }

      // set the number of visible items to the max
      ComboBox<WizardStepParameters> combo = new ComboBox<>(presets);
      combo.setVisibleRowCount(IonInterfaceWizardParameterFactory.values().length);
      combos.put(part, combo);
      // add a separator if not the first
      if (column > 0) {
        comboBoxGrid.add(new Label("-"), column++, 1);
      }
      combo.getSelectionModel().select(0);
      final Label caption = generateCaptionLabel(part);
      caption.widthProperty().addListener(
          (_, _, width) -> caption.setMinHeight(caption.prefHeight(width.doubleValue())));
      comboBoxGrid.add(caption, column, 0);
      comboBoxGrid.add(combo, column++, 1);

      // add listener
      combo.getSelectionModel().selectedItemProperty().addListener((_, _, newValue) -> {
        if (listenersActive) {
          sequenceSteps.set(part, newValue);
          // selecting another preset overrides the automatically changed values of this part
          // assumption: presets that change as a consequence (e.g., MS after IMS) are rare
          // and ignored
          parameterChanges = parameterChanges.withoutPart(part);
          // keep old parameters before changing pane
          updateAllParametersFromUi();
          createParameterPanes();
        }
      });
    }

    // decision: workflow = create batch. Presets are the only other action in the header, the
    // parameter actions sit next to the parameter tabs they fill in
    final Button createBatch = FxButtons.createButton("Create batch", FxIcons.START,
        "Create the batch from the selected workflow and parameters", this::createBatch);
    createBatch.getStyleClass().add("accent-button");
    presetsMenu.setGraphic(
        FxIconUtil.getFontIcon("bi-folder-symlink", FxIconUtil.DEFAULT_ICON_SIZE));
    presetsMenu.setTooltip(new Tooltip("Load, save, or apply local presets"));
    comboBoxGrid.add(FxLabels.newLabel("="), column++, 1);
    comboBoxGrid.add(createBatch, column++, 1);
    comboBoxGrid.add(presetsMenu, column, 1);
    comboBoxGrid.disableProperty().bind(autoParamActions.runningTasksProperty().greaterThan(0));

    final FlowPane instrumentComboBoxPane = FxLayout.newFlowPane(comboBoxGrid);
    instrumentComboBoxPane.setAlignment(Pos.CENTER);

    schemaPane = new HBox(0);
    schemaPane.setAlignment(Pos.CENTER);

    controlSchemaPane.getChildren().addAll(instrumentComboBoxPane, schemaPane);
    return controlSchemaPane;
  }

  /**
   * Parameter estimation and optimization, advanced mode, and help. Overlays the right end of the
   * tab header. Requires the {@link #combos} and {@link #tabPane} to be initialized.
   */
  private @NotNull HBox createTabHeaderActions() {
    final Button estimate = FxButtons.createButton("Estimate", Source.ESTIMATION.icon(),
        "Derive wizard parameters from the same representative files used for optimization.\n"
            + "Right click to also show the data file statistics.",
        () -> autoParamActions.estimate(false));
    estimate.setContextMenu(new ContextMenu(
        FxMenuUtil.newMenuItem("Estimate parameters and show statistics",
            () -> autoParamActions.estimate(true))));
    final Button optimize = FxButtons.createButton("Optimize", Source.OPTIMIZATION.icon(),
        "Optimize the wizard parameters on representative files.\n"
            + "Right click for advanced optimizer settings.",
        () -> autoParamActions.optimize(false));
    optimize.setContextMenu(new ContextMenu(
        FxMenuUtil.newMenuItem("Optimize with advanced settings",
            () -> autoParamActions.optimize(true))));

    //disable estimate and optimize on invalid presets
    final BooleanBinding autoParamDisabled = autoParamActions.createDisabledBinding(
        combos.get(WizardPart.ION_INTERFACE).getSelectionModel().selectedItemProperty());
    estimate.disableProperty().bind(autoParamDisabled);
    optimize.disableProperty().bind(autoParamDisabled);

    // advanced mode toggle switch
    final ToggleSwitch advancedToggle = new ToggleSwitch("Advanced mode");
    advancedToggle.setTooltip(new Tooltip("Show or hide the advanced parameter customization tab"));
    advancedToggle.selectedProperty().bindBidirectional(advancedMode);
    advancedMode.addListener((_, _, _) -> {
      if (listenersActive) {
        // When a preset is loaded the listeners are not enabled
        createParameterPanes();
      }
    });

    final Button help = FxButtons.createHelpButton(MzioMZmineLinks.WIZARD_DOCUMENTATION.getUrl());

    final HBox actions = FxLayout.newHBox(Pos.CENTER_RIGHT,
        new Insets(0, FxLayout.DEFAULT_SPACE, 0, 0), FxLabels.newLabel("Fill from data:"), estimate,
        optimize, new Separator(Orientation.VERTICAL), advancedToggle, help);
    actions.getStyleClass().add("tab-header-actions");
    actions.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    actions.setPickOnBounds(false);
    StackPane.setAlignment(actions, Pos.TOP_RIGHT);
    reserveTabHeaderSpace(actions);
    return actions;
  }

  /// The actions overlay the right end of the tab header. Pads the header area so that the tabs and
  /// the tab overflow button never run below the actions and centers the actions vertically in the
  /// header.
  private void reserveTabHeaderSpace(@NotNull final Region actions) {
    final Subscription[] headerSub = {Subscription.EMPTY};
    tabPane.skinProperty().subscribe(skin -> {
      headerSub[0].unsubscribe();
      headerSub[0] = Subscription.EMPTY;
      actions.minHeightProperty().unbind();
      if (skin == null || !(tabPane.lookup(".tab-header-area") instanceof Region header)) {
        return;
      }
      actions.minHeightProperty().bind(header.heightProperty());
      // assumption: the themes set the header area padding to 0 (jabref_light.css,
      // style_modern.css), the inline style only adds the right padding
      headerSub[0] = actions.widthProperty().subscribe(
          width -> header.setStyle("-fx-padding: 0 %.1fpx 0 0;".formatted(width.doubleValue())));
    });
  }

  /**
   * Applies estimated or optimized values to the current wizard sequence. Only the parameters set
   * by the applier change, all other current wizard values are kept. Changed parameters are
   * highlighted with the given source.
   *
   * @param applier sets the new values on the current wizard sequence
   * @param source  the source to highlight the changed parameters with
   */
  public void applyParameterValues(@NotNull Consumer<WizardSequence> applier,
      @NotNull Source source) {
    // Preserve unrelated edits made while a background task was running.
    updateAllParametersFromUi();
    final WizardSequence before = sequenceSteps.copy();
    final boolean previousListenersActive = listenersActive;
    setListenersActive(false);
    try {
      // decision: estimation and optimization replace previous customization with their own
      // overrides, so overrides of a previous run do not linger. This intentionally also discards
      // overrides the user added manually, without confirmation.
      sequenceSteps.get(WizardPart.CUSTOMIZATION).ifPresent(WizardStepParameters::resetToDefaults);
      applier.accept(sequenceSteps);
      parameterChanges = WizardParameterChanges.diff(before, sequenceSteps, source);
      advancedMode.set(sequenceSteps.get(WizardPart.CUSTOMIZATION)
          .map(step -> step.getValue(CustomizationWizardParameters.overrides))
          .map(overrides -> !overrides.isEmpty()).orElse(false));
      createParameterPanes();
    } finally {
      setListenersActive(previousListenersActive);
    }
  }

  /**
   * Asks the user whether to switch to the presets that fit the raw data and switches the wizard
   * directly if confirmed. The switch is not highlighted as a parameter change. Must be called on
   * the JavaFX thread.
   * <p>
   * Presets that are kept with a warning are only shown, without a question if there is nothing to
   * switch.
   *
   * @param presets the presets that fit the raw data
   * @return true if the wizard was switched to the presets
   */
  public boolean confirmAndSwitchPresets(@NotNull PresetSelection presets) {
    if (presets.isEmpty()) {
      return false;
    }
    final String warnings = presets.hasWarnings() ? """
        Please check the raw data, the selected presets are kept:
        
        %s""".formatted(presets.describeWarnings()) : "";
    if (!presets.hasSwitches()) {
      DialogLoggerUtil.showWarningDialog("Check wizard presets", warnings);
      return false;
    }
    final boolean confirmed = DialogLoggerUtil.showDialogYesNo(
        presets.hasWarnings() ? AlertType.WARNING : AlertType.CONFIRMATION, "Switch wizard presets",
        """
            Other presets fit the raw data better than the ones selected in the wizard:
            
            %s
            
            Switch the wizard to these presets? The new presets start from their default \
            parameters, the estimated values are applied on top.%s""".formatted(
            presets.describeSwitches(), warnings.isEmpty() ? "" : "\n\n" + warnings));
    if (confirmed) {
      switchPresets(presets);
    }
    return confirmed;
  }

  /**
   * Asks the user to switch the wizard back to the presets a result was computed with, if other
   * presets were selected in the meantime. Switched presets start from their default parameters,
   * like the estimation and the optimization do. Must be called on the JavaFX thread.
   *
   * @param resultSequence the sequence the result was computed with
   * @param resultName     names the result in the message, e.g., "optimization"
   * @return true if the presets already match or the wizard was switched, false if the user
   * declined, so the result must not be applied
   */
  public boolean confirmAndRestorePresets(@NotNull WizardSequence resultSequence,
      @NotNull String resultName) {
    updateAllParametersFromUi();
    final PresetSelection presets = PresetSelection.differences(sequenceSteps, resultSequence,
        "used by the %s".formatted(resultName));
    if (!presets.hasSwitches()) {
      return true;
    }
    final boolean confirmed = DialogLoggerUtil.showDialogYesNo(AlertType.WARNING,
        "Switch wizard presets", """
            The %s was computed with other presets than the ones selected in the wizard:
            
            %s
            
            Switch the wizard back to these presets? The switched presets start from their \
            default parameters. The %s results are only applied to the presets they were \
            computed with.""".formatted(resultName, presets.describeSwitches(), resultName));
    if (confirmed) {
      switchPresets(presets);
    }
    return confirmed;
  }

  /**
   * Selects the presets in the wizard. The combo boxes follow when the parameter panes are
   * recreated.
   */
  private void switchPresets(@NotNull PresetSelection presets) {
    // Preserve unrelated edits before the panes are recreated.
    updateAllParametersFromUi();
    final boolean previousListenersActive = listenersActive;
    setListenersActive(false);
    try {
      // kept presets are only warnings and keep their parameters
      for (final PresetChange change : presets.switches()) {
        ALL_PRESETS.get(change.part()).stream()
            .filter(preset -> preset.getFactory().equals(change.to())).findFirst()
            .ifPresent(preset -> {
              // decision: the estimates and the optimization are based on the defaults of the
              // new preset, so previous edits of that preset are discarded
              preset.resetToDefaults();
              sequenceSteps.set(change.part(), preset);
            });
      }
      createParameterPanes();
    } finally {
      setListenersActive(previousListenersActive);
    }
    logger.info("Switched wizard presets:\n" + presets.describeSwitches());
  }

  /**
   * Find local preset files and add to the drop-down
   */
  private void findAllLocalPresetFiles() {

    final List<MenuItem> items = new ArrayList<>();
    items.add(FxMenuUtil.newMenuItem("Load presets...", this::chooseAndLoadLocalSequence));
    items.add(FxMenuUtil.newMenuItem("Save presets...", this::saveLocalWizardSequence));
    items.add(new SeparatorMenuItem());

    final var newLocalPresets = WizardSequenceIOUtils.findAllLocalPresetFiles();
    final MenuItem localHeader = new MenuItem(
        newLocalPresets.isEmpty() ? "No local presets" : "Local presets");
    localHeader.setDisable(true);
    items.add(localHeader);
    for (final LocalWizardSequenceFile preset : newLocalPresets) {
      items.add(FxMenuUtil.newMenuItem(FileAndPathUtil.eraseFormat(preset.file().getName()),
          () -> applyLocalPartialSequence(preset)));
    }
    presetsMenu.getItems().setAll(items);
  }

  /**
   * Apply preloaded workflow
   *
   * @param partialSequence partial workflow or whole
   */
  private void applyLocalPartialSequence(LocalWizardSequenceFile partialSequence) {
    if (partialSequence == null) {
      return;
    }
    applyPartialSequence(partialSequence.parts());
  }

  /**
   * Applies the sequence without highlighting changes. Clears previous highlights.
   *
   * @param partialSequence might contain some or all steps of the workflow
   */
  public void applyPartialSequence(@NotNull final WizardSequence partialSequence) {
    setListenersActive(false);

    // keep old parameters before applying sequence
    updateAllParametersFromUi();

    // partialSequence might contain other instances of the presets (after loading)
    // need to apply all parameter changes to ALL_PRESETS
    WizardSequence correctPartialSequence = new WizardSequence();
    for (final WizardStepParameters otherPreset : partialSequence) {
      ALL_PRESETS.get(otherPreset.getPart()).stream()
          .filter(allPreset -> allPreset.getFactory().equals(otherPreset.getFactory()))
          .forEach(allPreset -> {
            ParameterUtils.copyParameters(otherPreset, allPreset);
            correctPartialSequence.add(allPreset);
          });
    }

    // keep current as default parameters
    sequenceSteps.apply(correctPartialSequence);
    // decision: loading presets overrides values, so previous highlights are cleared
    parameterChanges = WizardParameterChanges.empty();

    // auto-enable/disable advanced mode based on loaded customization state
    // listenersActive is false here, so the advancedMode listener does not trigger createParameterPanes again
    boolean customizationEnabled = partialSequence.get(WizardPart.CUSTOMIZATION)
        .filter(p -> p instanceof CustomizationWizardParameters)
        .map(p -> p.getValue(CustomizationWizardParameters.enabled)).orElse(false);
    advancedMode.set(customizationEnabled);

    // apply preset filters so that combos show the correct options
    createParameterPanes();

    // now activate listeners again. Should be after changing the sequence and createParameterPanes
    setListenersActive(true);
  }

  /**
   * Open a file chooser and load a local workflow file
   */
  private void chooseAndLoadLocalSequence() {
    // update all parameters to use them as a default for each step
    updateAllParametersFromUi();
    // only load those steps that were defined in the local preset file
    WizardSequence wizardPresets = WizardSequenceIOUtils.chooseAndLoadFile();
    if (!wizardPresets.isEmpty()) {
      applyPartialSequence(wizardPresets);
    }
  }

  /**
   * Open save dialog and save to file
   */
  private void saveLocalWizardSequence() {
    // update the preset parameters
    updateAllParametersFromUi();
    WizardSequenceSaveModule.setupAndSave(sequenceSteps);
  }

  /**
   * The final product of the wizard is the batch
   */
  public void createBatch() {
    var sequenceSteps = updateAllParametersFromUiAndCheckErrors();
    if (sequenceSteps == null) {
      return;
    }
    final Optional<WizardStepParameters> workflow = sequenceSteps.get(WORKFLOW);
    if (workflow.isEmpty()) {
      DialogLoggerUtil.showErrorDialog("Cannot create batch",
          "A workflow must be selected to create a batch.");
      return;
    }

    BatchModeParameters batchModeParameters = (BatchModeParameters) MZmineCore.getConfiguration()
        .getModuleParameters(BatchModeModule.class);
    try {
      final BatchQueue q = ((WorkflowWizardParameterFactory) workflow.get()
          .getFactory()).getBatchBuilder(sequenceSteps).createQueue();
      batchModeParameters.getParameter(BatchModeParameters.batchQueue).setValue(q);

      // highlights are only relevant until the batch is built
      clearParameterChanges();
      if (batchModeParameters.showSetupDialog(false) == ExitCode.OK) {
        MZmineCore.runMZmineModule(BatchModeModule.class, batchModeParameters.cloneParameterSet());
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, "Cannot create batch" + e.getMessage(), e);
      DialogLoggerUtil.showErrorDialog("Cannot create batch", e.getMessage());
    }
  }

  /**
   * @return the sequenceSteps variable on success or null on error (misconfiguration)
   */
  private @Nullable WizardSequence updateAllParametersFromUiAndCheckErrors() {
    List<String> errorMessages = new ArrayList<>();

    // Update parameters from pane and check
    updateAllParametersFromUi();
    sequenceSteps.forEach(step -> step.checkParameterValues(errorMessages));

    if (!errorMessages.isEmpty()) {
      MZmineCore.getDesktop().displayErrorMessage("Please check the parameters.\n" + errorMessages);
      return null;
    }

    // checks like min sample number, qcs, metadata file
    if (!new BatchWizardCreateBatchChecker(sequenceSteps).checks()) {
      return null;
    }

    return sequenceSteps;
  }


  /**
   * Updates the parameters in all steps from the UI components. Does not check for completeness.
   */
  void updateAllParametersFromUi() {
    paramPaneMap.values().forEach(ParameterSetupPane::updateParameterSetFromComponents);
  }

  public void setListenersActive(final boolean listenersActive) {
    this.listenersActive = listenersActive;
  }

  public WizardSequence getSequence() {
    return sequenceSteps;
  }
}
