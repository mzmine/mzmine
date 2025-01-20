/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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
import static io.github.mzmine.modules.tools.batchwizard.WizardPart.FILTER;
import static io.github.mzmine.modules.tools.batchwizard.WizardPart.WORKFLOW;
import static io.github.mzmine.modules.tools.batchwizard.builders.WizardBatchBuilder.getOrElse;
import static io.github.mzmine.util.StringUtils.inQuotes;

import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.batchmode.BatchModeModule;
import io.github.mzmine.modules.batchmode.BatchModeParameters;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.tools.batchwizard.io.LocalWizardSequenceFile;
import io.github.mzmine.modules.tools.batchwizard.io.WizardSequenceIOUtils;
import io.github.mzmine.modules.tools.batchwizard.io.WizardSequenceSaveModule;
import io.github.mzmine.modules.tools.batchwizard.subparameters.DataImportWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.FilterWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassSpectrometerWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonMobilityWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.MassSpectrometerWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WorkflowWizardParameterFactory;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.dialogs.ParameterSetupPane;
import io.github.mzmine.parameters.parametertypes.absoluterelative.AbsoluteAndRelativeInt;
import io.github.mzmine.parameters.parametertypes.filenames.LastFilesButton;
import io.github.mzmine.util.ExitCode;
import java.io.File;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
  private final Map<WizardPart, List<WizardStepParameters>> ALL_PRESETS;
  /**
   * The selected sequence. first - last step. Changes in the combobox selection are reflected here
   */
  private final WizardSequence sequenceSteps = new WizardSequence();
  /**
   * Parameter panes of the selected presets
   */
  private final Map<File, LocalWizardSequenceFile> localPresets = new HashMap<>();
  private final Map<WizardStepParameters, @NotNull ParameterSetupPane> paramPaneMap = new HashMap<>();
  private final Map<WizardPart, ComboBox<WizardStepParameters>> combos = new HashMap<>();
  private final LastFilesButton localPresetsButton;
  private boolean listenersActive = true;
  private TabPane tabPane;
  private HBox schemaPane;

  public BatchWizardTab() {
    super("mzwizard");
//    setGraphic(LightAndDarkModeIcon.mzwizardImageTab(200, 18));
    ALL_PRESETS = WizardStepParameters.createAllPresets();
    localPresetsButton = new LastFilesButton("Local presets", true,
        file -> applyLocalPartialSequence(localPresets.get(file)));
    localPresetsButton.setGraphic(
        FxIconUtil.getFontIcon("bi-folder-symlink", FxIconUtil.DEFAULT_ICON_SIZE));
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
    schemaPane.getChildren().clear();
    paramPaneMap.clear();
    int selectedIndex = tabPane.getSelectionModel().getSelectedIndex();
    // evaluate workflow and limit choices
    evaluateWizardSequenceLimitChoices();

    // create parameters for all parts
    // LC/GC - IMS? - MS instrument, Apply defaults
    Tab[] panes = sequenceSteps.stream().map(this::createParameterTab).filter(Objects::nonNull)
        .toArray(Tab[]::new);

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
    // add to schema
    addToSchema(step);
    // NOT add tabs without user parameters (components to set)
    if (step.hasUserParameters() && step.getFactory() != IonMobilityWizardParameterFactory.NO_IMS) {
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

    sequenceSteps.clear();
    combos.clear();
    // create combo boxes for each part of the wizard that has multiple options
    // LC/GC - IMS? - MS instrument, Apply defaults
    for (final WizardPart part : WizardPart.values()) {
      var presets = FXCollections.observableArrayList(ALL_PRESETS.get(part));
      sequenceSteps.add(presets.get(0));
      if (presets.size() == 1) {
        continue;
      }

      // set the number of visible items to the max
      ComboBox<WizardStepParameters> combo = new ComboBox<>(presets);
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
              sequenceSteps.set(part, newValue);
              // keep old parameters before changing pane
              updateAllParametersFromUi();
              createParameterPanes();
            }
          });
    }

    Button createBatch = FxButtons.createButton("Create batch", FxIcons.START, null,
        this::createBatch);
    Button save = FxButtons.createSaveButton("Save presets", this::saveLocalWizardSequence);
    Button load = FxButtons.createLoadButton("Load presets", this::chooseAndLoadLocalSequence);

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
    var newLocalPresets = WizardSequenceIOUtils.findAllLocalPresetFiles();

    localPresets.clear();
    for (final LocalWizardSequenceFile preset : newLocalPresets) {
      localPresets.put(preset.file(), preset);
    }
    localPresetsButton.setLastFiles(
        newLocalPresets.stream().map(LocalWizardSequenceFile::file).toList());
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
   * @param partialSequence might contain some or all steps of the workflow
   */
  private void applyPartialSequence(final WizardSequence partialSequence) {
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

    // check if samples > min samples filter
    if (!checkSampleFilterValid()) {
      return null;
    }
    return sequenceSteps;
  }

  /**
   * @return true if imported samples > min num samples
   */
  private boolean checkSampleFilterValid() {
    int numFiles = getOrElse(sequenceSteps.get(DATA_IMPORT), DataImportWizardParameters.fileNames,
        new File[0]).length;

    var minSamples = getOrElse(sequenceSteps.get(FILTER), FilterWizardParameters.minNumberOfSamples,
        new AbsoluteAndRelativeInt(0, 0));
    if (minSamples.getMaximumValue(numFiles) > numFiles) {
      // continue? y/n
      return DialogLoggerUtil.showDialogYesNo("Warning", """
          The number of %s (Filters tab) does not match the number of imported data files. This will avoid correlation grouping.
          Continue anyway?""".formatted(
          inQuotes(FilterWizardParameters.minNumberOfSamples.getName())));
    }
    return true;
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
