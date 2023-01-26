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

package io.github.mzmine.modules.tools.batchwizard;

import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.batchmode.BatchModeModule;
import io.github.mzmine.modules.batchmode.BatchModeParameters;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.tools.batchwizard.builders.WizardBatchBuilder;
import io.github.mzmine.modules.tools.batchwizard.io.LocalWizardWorkflowFile;
import io.github.mzmine.modules.tools.batchwizard.io.WizardWorkflowIOUtils;
import io.github.mzmine.modules.tools.batchwizard.io.WizardWorkflowSaveModule;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassSpectrometerWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepPreset;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonMobilityWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.MassSpectrometerWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WorkflowWizardParameterFactory;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.dialogs.ParameterSetupPane;
import io.github.mzmine.parameters.parametertypes.filenames.LastFilesButton;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.javafx.FxIconUtil;
import java.io.File;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TabPane.TabDragPolicy;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BatchWizardTab extends SimpleTab {

  /**
   * needs to use the same preset object, as its also used in the combo boxes and in other places
   */
  private final Map<WizardPart, List<WizardStepPreset>> ALL_PRESETS;
  /**
   * The selected workflow. first - last step. Changes in the combobox selection are reflected here
   */
  private final WizardSequence workflowSteps = new WizardSequence();
  /**
   * Parameter panes of the selected presets
   */
  private final Map<File, LocalWizardWorkflowFile> localPresets = new HashMap<>();
  private final Map<WizardStepPreset, @NotNull ParameterSetupPane> paramPaneMap = new HashMap<>();
  private final Map<WizardPart, ComboBox<WizardStepPreset>> combos = new HashMap<>();
  private final LastFilesButton localPresetsButton;
  private boolean listenersActive = true;
  private TabPane tabPane;
  private HBox schemaPane;

  public BatchWizardTab() {
    super("Processing Wizard");
    ALL_PRESETS = WizardStepPreset.createAllPresets();
    localPresetsButton = new LastFilesButton("Local presets", true,
        file -> applyLocalPartialWorkflow(localPresets.get(file)));
    createContentPane();
    findAllLocalPresetFiles();
  }

  private void createContentPane() {
    // top menu with selections
    var topPane = createTopMenu();
    // center parameter panes
    tabPane = new TabPane();
    tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
    tabPane.setTabDragPolicy(TabDragPolicy.FIXED);
    BorderPane centerPane = new BorderPane(tabPane);
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
    updateAllParametersFromUi();
    schemaPane.getChildren().clear();
    paramPaneMap.clear();
    int selectedIndex = tabPane.getSelectionModel().getSelectedIndex();
    // evaluate workflow and limit choices
    evaluateWorkflowLimitChoices();

    // create parameters for all parts
    // LC/GC - IMS? - MS instrument, Apply defaults
    Tab[] panes = workflowSteps.stream().map(this::createParameterTab).filter(Objects::nonNull)
        .toArray(Tab[]::new);

    // add to center pane
    tabPane.getTabs().clear();
    tabPane.getTabs().addAll(panes);
    tabPane.getSelectionModel().select(selectedIndex);
  }

  private void evaluateWorkflowLimitChoices() {
    var ionization = workflowSteps.get(WizardPart.ION_INTERFACE)
        .map(step -> (IonInterfaceWizardParameterFactory) step.getPreset())
        .orElse(IonInterfaceWizardParameterFactory.HPLC);

    List<WizardStepPreset> filteredWorkflows = ALL_PRESETS.get(WizardPart.WORKFLOW).stream()
        .filter(workflow -> switch (ionization) {
          case HPLC, UHPLC, HILIC, GC_CI, DIRECT_INFUSION, FLOW_INJECT, MALDI, LDI, DESI, SIMS ->
              !workflow.getPreset().equals(WorkflowWizardParameterFactory.GC_EI_DECONVOLUTION);
          case GC_EI ->
              workflow.getPreset().equals(WorkflowWizardParameterFactory.GC_EI_DECONVOLUTION);
        }).toList();

    ComboBox<WizardStepPreset> workflowCombo = combos.get(WizardPart.WORKFLOW);
    ObservableList<WizardStepPreset> currentWorkflows = workflowCombo.getItems();
    if (!currentWorkflows.equals(filteredWorkflows)) {
      // need to set new selection to workflow
      workflowSteps.set(WizardPart.WORKFLOW,
          setItemsToCombo(workflowCombo, filteredWorkflows, false));
    }

    // check timsTOF and TWIMS TOF only
    var ims = workflowSteps.get(WizardPart.IMS)
        .map(step -> (IonMobilityWizardParameterFactory) step.getPreset())
        .orElse(IonMobilityWizardParameterFactory.NO_IMS);

    ComboBox<WizardStepPreset> msCombo = combos.get(WizardPart.MS);
    ObservableList<WizardStepPreset> currentMs = msCombo.getItems();
    List<WizardStepPreset> filteredMs = ALL_PRESETS.get(WizardPart.MS).stream()
        .filter(ms -> switch (ims) {
          case TIMS, TWIMS -> ms.getPreset().equals(MassSpectrometerWizardParameterFactory.qTOF);
          case NO_IMS, IMS, DTIMS -> true;
        }).toList();

    if (!currentMs.equals(filteredMs)) {
      WizardStepPreset selectedMs = setItemsToCombo(msCombo, filteredMs, false);
      // need to set new selection to workflow
      workflowSteps.set(WizardPart.MS, selectedMs);

      // reduce the parameters for timsTOF to something meaningful
      // only if the MS parameter for tof are unchanged (if user already selected other inputs, keep
      MassSpectrometerWizardParameters msParamsForIms = MassSpectrometerWizardParameterFactory.createForIms(
          ims);
      if (msParamsForIms != null && selectedMs.hasDefaultParameters()) {
        ParameterUtils.copyParameters(msParamsForIms, selectedMs);
      }
    }
  }

  private WizardStepPreset setItemsToCombo(final ComboBox<WizardStepPreset> combo,
      final List<WizardStepPreset> newItems, boolean notifyListeners) {
    boolean oldNotify = listenersActive;
    setListenersActive(notifyListeners);
    // keep selection or select first element if not available
    SingleSelectionModel<WizardStepPreset> selection = combo.getSelectionModel();
    WizardStepPreset oldSelected = selection.getSelectedItem();
    // set new items
    combo.setItems(FXCollections.observableList(newItems));
    selection.select(oldSelected);
    if (selection.getSelectedIndex() < 0) {
      selection.selectFirst();
    }
    setListenersActive(oldNotify);
    return selection.getSelectedItem();
  }

  @Nullable
  private Tab createParameterTab(final WizardStepPreset step) {
    ParameterSetupPane paramPane = new ParameterSetupPane(true, false, step);
    paramPaneMap.put(step, paramPane);
    // add to schema
    addToSchema(step);
    // NOT add tabs without user parameters (components to set)
    if (step.hasUserParameters()) {
      return new Tab(step.getPresetName(), paramPane);
    } else {
      return null;
    }
  }

  /**
   * Schema for workflow in the resources directory src/main/resources/icons/wizard/
   *
   * @param preset one preset per part
   */
  private void addToSchema(final WizardStepPreset preset) {
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
      view.setFitHeight(100);

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

  public Region createSpacer() {
    var spacer = new Region();
    spacer.setPrefWidth(10);
    return spacer;
  }

  private VBox createTopMenu() {
    VBox vbox = new VBox(4);
    vbox.setAlignment(Pos.CENTER);
    VBox.setMargin(vbox, new Insets(5));

    var topPane = new FlowPane(4, 4);
    topPane.setAlignment(Pos.CENTER);
    HBox.setMargin(topPane, new Insets(5));

    workflowSteps.clear();
    combos.clear();
    // create combo boxes for each part of the wizard that has multiple options
    // LC/GC - IMS? - MS instrument, Apply defaults
    for (final WizardPart part : WizardPart.values()) {
      var presets = FXCollections.observableArrayList(ALL_PRESETS.get(part));
      workflowSteps.add(presets.get(0));
      if (presets.size() == 1) {
        continue;
      }

      // set the number of visible items to the max
      ComboBox<WizardStepPreset> combo = new ComboBox<>(presets);
      combo.setVisibleRowCount(IonInterfaceWizardParameterFactory.values().length);
      combos.put(part, combo);
      // add a spacer if not the first
      if (!topPane.getChildren().isEmpty()) {
        topPane.getChildren().add(new Label("-"));
      }
      combo.getSelectionModel().select(0);
      topPane.getChildren().add(combo);

      // add listener
      combo.getSelectionModel().selectedItemProperty()
          .addListener((observable, oldValue, newValue) -> {
            if (listenersActive) {
              workflowSteps.set(part, newValue);
              createParameterPanes();
            }
          });
    }

    Button createBatch = new Button("Create batch");
    createBatch.setOnAction(event -> createBatch());

    Button save = new Button("Save presets");
    save.setOnAction(event -> saveLocalWorkflow());

    Button load = new Button("Load presets");
    load.setOnAction(event -> chooseAndLoadLocalWorkflow());

    topPane.getChildren()
        .addAll(createSpacer(), new Label("="), createSpacer(), createBatch, save, load,
            localPresetsButton);

    schemaPane = new HBox(0);
    schemaPane.setAlignment(Pos.CENTER);
    vbox.getChildren().addAll(topPane, schemaPane);
    return vbox;
  }

  /**
   * Find local preset files and add to the drop-down
   */
  private void findAllLocalPresetFiles() {
    var newLocalPresets = WizardWorkflowIOUtils.findAllLocalPresetFiles(ALL_PRESETS);

    localPresets.clear();
    for (final LocalWizardWorkflowFile preset : newLocalPresets) {
      localPresets.put(preset.file(), preset);
    }
    localPresetsButton.setLastFiles(
        newLocalPresets.stream().map(LocalWizardWorkflowFile::file).toList());
  }

  /**
   * Apply preloaded workflow
   *
   * @param partialWorkflow partial workflow or whole
   */
  private void applyLocalPartialWorkflow(LocalWizardWorkflowFile partialWorkflow) {
    if (partialWorkflow == null) {
      return;
    }
    appendPresetsToUi(partialWorkflow.parts());
  }

  /**
   * @param partialWorkflow might contain some or all steps of the workflow
   */
  private void appendPresetsToUi(final WizardSequence partialWorkflow) {
    setListenersActive(false);

    // keep current as default parameters
    workflowSteps.apply(partialWorkflow);

    for (var preset : workflowSteps) {
      ComboBox<WizardStepPreset> combo = combos.get(preset.getPart());
      if (combo != null) {
        combo.getSelectionModel().select(preset);
      }
    }
    setListenersActive(true);

    createParameterPanes();
  }

  /**
   * Open a file chooser and load a local workflow file
   */
  private void chooseAndLoadLocalWorkflow() {
    // update all parameters to use them as a default for each step
    updateAllParametersFromUi();
    // only load those steps that were defined in the local preset file
    WizardSequence wizardPresets = WizardWorkflowIOUtils.chooseAndLoadFile(ALL_PRESETS);
    if (!wizardPresets.isEmpty()) {
      appendPresetsToUi(wizardPresets);
    }
  }

  /**
   * Open save dialog and save to file
   */
  private void saveLocalWorkflow() {
    // update the preset parameters
    updateAllParametersFromUi();
    WizardWorkflowSaveModule.setupAndSave(workflowSteps);
  }

  /**
   * The final product of the wizard is the batch
   */
  public void createBatch() {
    var workflowSteps = updateAllParametersFromUiAndCheckErrors();
    if (workflowSteps == null) {
      return;
    }

    BatchModeParameters batchModeParameters = (BatchModeParameters) MZmineCore.getConfiguration()
        .getModuleParameters(BatchModeModule.class);
    final BatchQueue q = WizardBatchBuilder.createBatchBuilderForWorkflow(workflowSteps)
        .createQueue();
    batchModeParameters.getParameter(BatchModeParameters.batchQueue).setValue(q);

    if (batchModeParameters.showSetupDialog(false) == ExitCode.OK) {
      MZmineCore.runMZmineModule(BatchModeModule.class, batchModeParameters.cloneParameterSet());
    }
  }

  /**
   * @return the workflowSteps variable on success or null on error (misconfiguration)
   */
  private @Nullable WizardSequence updateAllParametersFromUiAndCheckErrors() {
    List<String> errorMessages = new ArrayList<>();

    // Update parameters from pane and check
    updateAllParametersFromUi();
    workflowSteps.forEach(step -> step.checkParameterValues(errorMessages));

    if (!errorMessages.isEmpty()) {
      MZmineCore.getDesktop().displayErrorMessage("Please check the parameters.\n" + errorMessages);
      return null;
    }
    return workflowSteps;
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
}
