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

import static io.github.mzmine.modules.tools.batchwizard.WizardPart.DATA_IMPORT;
import static io.github.mzmine.modules.tools.batchwizard.WizardPart.WORKFLOW;

import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.javafx.components.factories.FxTexts;
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
import io.github.mzmine.modules.tools.tools_autoparam.DataFileStatisticsDashboardPane;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.PresetChange;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.PresetSelection;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.WizardParameterEstimationResult;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.WizardParameterEstimationTask;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.BatchOptimizationMainTask;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.OptimizerModule;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.OptimizerParameters;
import io.github.mzmine.modules.tools.tools_autoparam.preclassification.Preclassification;
import io.github.mzmine.modules.tools.tools_autoparam.preclassification.PreclassificationParameters;
import io.github.mzmine.modules.tools.tools_autoparam.preclassification.PreclassificationResult;
import io.github.mzmine.modules.tools.tools_autoparam.preclassification.RawDataPreclassificationTask;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.RawDataPreparation;
import io.github.mzmine.modules.visualization.projectmetadata.extract.SampleMetadataExtractionParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.dialogs.ParameterSetupPane;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesComponent;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskService;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.javafx.FxMenuUtil;
import io.github.mzmine.util.javafx.MZmineIconUtils;
import io.mzio.links.MzioMZmineLinks;
import java.io.File;
import java.text.MessageFormat;
import java.time.Instant;
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
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
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
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TabPane.TabDragPolicy;
import javafx.scene.control.TabPane;
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
   * Number of running pre-classification and estimation tasks, see
   * {@link #startAutoParamTask(AbstractTask, String)}.
   */
  private final SimpleIntegerProperty runningAutoParamTasks = new SimpleIntegerProperty(0);
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

  private void createContentPane() {
    // top menu with selections
    var topPane = createTopMenu();
    // center parameter panes
    tabPane = new TabPane();
    tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
    tabPane.setTabDragPolicy(TabDragPolicy.FIXED);
    BorderPane centerPane = new BorderPane(new StackPane(tabPane, createTabHeaderActions()));
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
      combo.getSelectionModel().selectedItemProperty()
          .addListener((_, _, newValue) -> {
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
        () -> estimateParametersFromFiles(false));
    estimate.setContextMenu(new ContextMenu(
        FxMenuUtil.newMenuItem("Estimate parameters and show statistics",
            () -> estimateParametersFromFiles(true))));
    final Button optimize = FxButtons.createButton("Optimize", Source.OPTIMIZATION.icon(),
        "Optimize the wizard parameters on representative files", this::runOptimizer);

    //disable estimate and optimize on invalid presets
    final BooleanBinding autoParamDisabled = createDisableEstimateAndOptimizeBinding();
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
    tabPane.skinProperty().subscribe(skin -> {
      if (skin == null || !(tabPane.lookup(".tab-header-area") instanceof Region header)) {
        return;
      }
      actions.minHeightProperty().bind(header.heightProperty());
      // assumption: the themes set the header area padding to 0 (jabref_light.css,
      // style_modern.css), the inline style only adds the right padding
      actions.widthProperty().subscribe(
          width -> header.setStyle("-fx-padding: 0 %.1fpx 0 0;".formatted(width.doubleValue())));
    });
  }

  /**
   * the parameter estimation needs chromatographic traces, so spatial imaging (MALDI, LDI, DESI,
   * SIMS) is not supported
   */
  private BooleanBinding createDisableEstimateAndOptimizeBinding() {
    final BooleanBinding autoParamTaskRunning = runningAutoParamTasks.greaterThan(0);
    final ReadOnlyObjectProperty<WizardStepParameters> selectedIonInterface = combos.get(
        WizardPart.ION_INTERFACE).getSelectionModel().selectedItemProperty();
    final BooleanBinding imagingSelected = Bindings.createBooleanBinding(
        () -> selectedIonInterface.get() != null && selectedIonInterface.get()
            .getFactory() instanceof IonInterfaceWizardParameterFactory ionInterface
            && ionInterface.isImaging(), selectedIonInterface);
    final BooleanBinding autoParamDisabled = autoParamTaskRunning.or(imagingSelected);
    return autoParamDisabled;
  }

  private void runOptimizer() {
    if (runningAutoParamTasks.get() > 0) {
      return;
    }
    updateAllParametersFromUi();
    final WizardStepParameters importParam = sequenceSteps.get(DATA_IMPORT).get();
    final @NotNull File[] allFiles = importParam.getParameter(DataImportWizardParameters.fileNames)
        .getValue();
    // selected once, the random selection must not change between pre-classification and run
    final File[] optimizerFiles = RawDataPreparation.selectOptimizerInputFiles(allFiles);

    if (optimizerFiles.length < 3) {
      DialogLoggerUtil.showErrorDialog("Not enough files",
          "The automated parameter optimisation requires ≥3 data files. Those data files"
              + " need to be comparable (Same instrument, same method, similar or same sample)");
      return;
    }

    final var metadataFile = importParam.getOptionalValue(DataImportWizardParameters.metadataFile)
        .orElse(null);

    preclassify(optimizerFiles, metadataFile, "Cannot optimize parameters",
        preclassification -> startOptimizer(optimizerFiles, metadataFile, preclassification));
  }

  /**
   * Shows the optimizer setup dialog after the pre-classification and starts the optimization.
   *
   * @param preclassification the settings fixed by the pre-classification, see
   *                          {@link PreclassificationParameters}
   */
  private void startOptimizer(final File @NotNull [] optimizerFiles,
      @Nullable final File metadataFile, @NotNull final ParameterSet preclassification) {
    // keep edits made while the pre-classification was running
    updateAllParametersFromUi();
    final OptimizerParameters optimizerParam = (OptimizerParameters) ConfigService.getConfiguration()
        .getModuleParameters(OptimizerModule.class);
    // inject wizard sequence so the parameter checklist shows only relevant solutions
    final ExitCode exitCode = optimizerParam.showSetupDialog(true, sequenceSteps);
    if (exitCode != ExitCode.OK) {
      return;
    }

    // a clone, so later edits of the module parameters do not change the running optimization
    final BatchOptimizationMainTask optimizer = new BatchOptimizationMainTask(
        MemoryMapStorage.forRawDataFile(), Instant.now(), optimizerFiles, metadataFile, this,
        (OptimizerParameters) optimizerParam.cloneParameterSet(), preclassification);
    // tracked like estimation, so no second run writes into the wizard and errors are shown
    startAutoParamTask(optimizer, "Cannot optimize parameters");
  }

  /**
   * Imports and pre-classifies the files in a background task, see {@link Preclassification}.
   * Conflicts are shown as an error and stop the run, choices are asked in a dialog. Then continues
   * on the JavaFX thread with the decided settings.
   *
   * @param errorTitle   title of error dialogs
   * @param continuation receives the settings fixed by the pre-classification, see
   *                     {@link PreclassificationParameters}
   */
  private void preclassify(final File @NotNull [] files, @Nullable final File metadataFile,
      @NotNull final String errorTitle,
      @NotNull final Consumer<@NotNull ParameterSet> continuation) {
    // a copy, so the task never reads the live wizard off the JavaFX thread
    final RawDataPreclassificationTask task = new RawDataPreclassificationTask(
        MemoryMapStorage.forRawDataFile(), Instant.now(), files, metadataFile, sequenceSteps.copy(),
        resolution -> {
          final ParameterSet preclassification = resolvePreclassification(resolution, errorTitle);
          if (preclassification != null) {
            continuation.accept(preclassification);
          }
        });
    startAutoParamTask(task, errorTitle);
  }

  /**
   * @return the decided settings, or null if there was a conflict or the user cancelled
   */
  private @Nullable ParameterSet resolvePreclassification(
      @NotNull final PreclassificationResult resolved, @NotNull final String errorTitle) {
    if (resolved.hasConflicts()) {
      DialogLoggerUtil.showErrorDialog(errorTitle, resolved.describeConflicts());
      return null;
    }
    if (!resolved.needsUserChoice()) {
      return resolved.parameters();
    }
    // decision: the dialog only shows the parameters that need a choice. They are the same
    // instances as in the decided settings, so the dialog changes those
    final ParameterSet choices = new SimpleParameterSet(
        resolved.choiceParameters().toArray(new Parameter<?>[0]));
    final ParameterSetupDialog dialog = new ParameterSetupDialog(true, choices,
        FxTextFlows.newTextFlow(FxTexts.text(String.join("\n\n", resolved.choiceMessages()))));
    dialog.showAndWait();
    return dialog.getExitCode() == ExitCode.OK ? resolved.parameters() : null;
  }

  /**
   * Starts a pre-classification, estimation, or optimization task. The estimate and optimize
   * buttons are disabled while any of them runs, errors are shown in a dialog.
   */
  private void startAutoParamTask(@NotNull final AbstractTask task,
      @NotNull final String errorTitle) {
    // decision: a counter instead of a flag. A task hands its result to the JavaFX thread before
    // it finishes, so the next task may already run when the previous one reports its end
    runningAutoParamTasks.set(runningAutoParamTasks.get() + 1);
    task.addTaskStatusListener((_, newStatus, _) -> {
      if (!newStatus.isUnmodifiable()) {
        return;
      }
      Platform.runLater(() -> {
        runningAutoParamTasks.set(runningAutoParamTasks.get() - 1);
        if (newStatus == TaskStatus.ERROR) {
          DialogLoggerUtil.showErrorDialog(errorTitle, task.getErrorMessage());
        }
      });
    });
    TaskService.getController().addTask(task);
  }

  /**
   * @param showStatistics opens the data file statistics dashboard after applying the estimates
   */
  private void estimateParametersFromFiles(final boolean showStatistics) {
    if (runningAutoParamTasks.get() > 0) {
      return;
    }
    updateAllParametersFromUi();
    final WizardStepParameters importParameters = sequenceSteps.get(DATA_IMPORT).orElse(null);
    if (importParameters == null) {
      DialogLoggerUtil.showErrorDialog("Cannot estimate parameters",
          "The wizard has no data import step.");
      return;
    }

    final File[] allFiles = importParameters.getValue(DataImportWizardParameters.fileNames);
    if (allFiles == null || allFiles.length == 0) {
      DialogLoggerUtil.showErrorDialog("Cannot estimate parameters",
          "Select at least one raw data file in the Data Import step first.");
      return;
    }
    // selected once, the random selection must not change between pre-classification and run
    final File[] estimateFiles = RawDataPreparation.selectOptimizerInputFiles(allFiles);

    final File metadataFile = importParameters.getOptionalValue(
        DataImportWizardParameters.metadataFile).orElse(null);
    preclassify(estimateFiles, metadataFile, "Cannot estimate parameters",
        preclassification -> startParameterEstimation(estimateFiles, metadataFile,
            preclassification, showStatistics));
  }

  /**
   * @param preclassification the settings fixed by the pre-classification, see
   *                          {@link PreclassificationParameters}
   */
  private void startParameterEstimation(final File @NotNull [] estimateFiles,
      @Nullable final File metadataFile, @NotNull final ParameterSet preclassification,
      final boolean showStatistics) {
    // keep edits made while the pre-classification was running
    updateAllParametersFromUi();
    final WizardSequence sequenceSnapshot = sequenceSteps.copy();
    final WizardParameterEstimationTask task = new WizardParameterEstimationTask(
        MemoryMapStorage.forRawDataFile(), Instant.now(), estimateFiles, metadataFile,
        sequenceSnapshot, preclassification, this::confirmAndSwitchPresets,
        result -> applyParameterEstimationResult(result, showStatistics));
    startAutoParamTask(task, "Cannot estimate parameters");
  }

  private void applyParameterEstimationResult(@NotNull WizardParameterEstimationResult result,
      final boolean showStatistics) {
    applyParameterValues(sequence -> result.estimates().applyEstimates(sequence),
        Source.ESTIMATION);
    if (!showStatistics) {
      return;
    }
    MZmineCore.getDesktop().addTab(new SimpleTab("Data File Statistics",
        new DataFileStatisticsDashboardPane(result.statistics(), result.interSampleRtStatistics(),
            result.context().massDetectorType())));
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
    logger.info("Switched wizard presets to fit the raw data:\n" + presets.describeSwitches());
  }

  /**
   * Find local preset files and add to the drop-down
   */
  private void findAllLocalPresetFiles() {
    var newLocalPresets = WizardSequenceIOUtils.findAllLocalPresetFiles();

    final List<MenuItem> items = new ArrayList<>();
    items.add(FxMenuUtil.newMenuItem("Load presets...", this::chooseAndLoadLocalSequence));
    items.add(FxMenuUtil.newMenuItem("Save presets...", this::saveLocalWizardSequence));
    items.add(new SeparatorMenuItem());
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
  private void updateAllParametersFromUi() {
    paramPaneMap.values().forEach(ParameterSetupPane::updateParameterSetFromComponents);
  }

  public void setListenersActive(final boolean listenersActive) {
    this.listenersActive = listenersActive;
  }

  public WizardSequence getSequence() {
    return sequenceSteps;
  }
}
