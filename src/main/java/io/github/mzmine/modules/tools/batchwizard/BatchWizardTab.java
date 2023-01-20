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
import io.github.mzmine.modules.tools.batchwizard.subparameters.IonMobilityWizardParameters;
import io.github.mzmine.parameters.dialogs.ParameterSetupPane;
import io.github.mzmine.parameters.parametertypes.filenames.LastFilesButton;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.javafx.FxIconUtil;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BatchWizardTab extends SimpleTab {

  public static final ExtensionFilter FILE_FILTER = new ExtensionFilter("MZmine wizard preset",
      "*.mzmwizard");

  /**
   * needs to use the same preset object, as its also used in the combo boxes and in other places
   */
  private final Map<WizardPart, List<WizardPreset>> ALL_PRESETS;
  /**
   * The selected workflow. first - last step. Changes in the combobox selection are reflected here
   */
  private final WizardWorkflow workflowSteps = new WizardWorkflow();
  /**
   * Parameter panes of the selected presets
   */
  private final Map<File, LocalWizardWorkflowFile> localPresets = new HashMap<>();
  private final Map<WizardPreset, @NotNull ParameterSetupPane> paramPaneMap = new HashMap<>();
  private final Map<WizardPart, ComboBox<WizardPreset>> combos = new HashMap<>();
  private final LastFilesButton localPresetsButton;
  private boolean listenersActive = true;
  private TabPane tabPane;
  private HBox schemaPane;

  public BatchWizardTab() {
    super("Processing Wizard");
    ALL_PRESETS = WizardPresetDefaults.createPresets();
    localPresetsButton = new LastFilesButton("Local presets", true,
        file -> applyPreset(localPresets.get(file)));
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

  private void createParameterPanes() {
    updateAllParametersFromUi();
    schemaPane.getChildren().clear();
    paramPaneMap.clear();
    int selectedIndex = tabPane.getSelectionModel().getSelectedIndex();
    // create parameters for all parts
    // LC/GC - IMS? - MS instrument, Apply defaults
    Tab[] panes = workflowSteps.stream().map(this::createParameterTab).filter(Objects::nonNull)
        .toArray(Tab[]::new);

    // add to center pane
    tabPane.getTabs().clear();
    tabPane.getTabs().addAll(panes);
    tabPane.getSelectionModel().select(selectedIndex);
  }

  @Nullable
  private Tab createParameterTab(final WizardPreset preset) {
    ParameterSetupPane paramPane = new ParameterSetupPane(true, false, preset.parameters());
    paramPaneMap.put(preset, paramPane);
    // add to schema
    addToSchema(preset);
    // do not add tabs for in active tabs
    if (!preset.name().equals(IonMobilityWizardParameters.ImsDefaults.NO_IMS.toString())) {
      return new Tab(preset.name(), paramPane);
    } else {
      return null;
    }
  }

  private void addToSchema(final WizardPreset preset) {
    String parent = preset.name().toLowerCase();
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

  /**
   * User/.mzmine/wizard/
   */
  @Nullable
  public static File getWizardSettingsPath() {
    File prefPath = FileAndPathUtil.getUserSettingsDir();
    if (prefPath == null) {
      logger.warning("Cannot find parameters default location in user folder");
    } else {
      prefPath = new File(prefPath, "wizard");
      FileAndPathUtil.createDirectory(prefPath);
    }
    return prefPath;
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

      ComboBox<WizardPreset> combo = new ComboBox<>(presets);
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
    save.setOnAction(event -> savePresets());

    Button load = new Button("Load presets");
    load.setOnAction(event -> loadPresets());

    topPane.getChildren()
        .addAll(createSpacer(), new Label("="), createSpacer(), createBatch, save, load,
            localPresetsButton);

    schemaPane = new HBox(0);
    schemaPane.setAlignment(Pos.CENTER);
    vbox.getChildren().addAll(topPane, schemaPane);
    return vbox;
  }

  private void findAllLocalPresetFiles() {
    File path = getWizardSettingsPath();
    if (path == null) {
      return;
    }

    var newLocalPresets = FileAndPathUtil.findFilesInDir(path, FILE_FILTER, false).stream()
        .filter(Objects::nonNull).flatMap(Arrays::stream).filter(Objects::nonNull).map(file -> {
          try {
            WizardWorkflow presets = WizardWorkflowIOUtils.loadFromFile(file, ALL_PRESETS);
            return new LocalWizardWorkflowFile(file, presets);
          } catch (IOException e) {
            logger.warning("Could not import wizard preset file " + file.getAbsolutePath());
            return null;
          }
        }).filter(Objects::nonNull).sorted(Comparator.comparing(LocalWizardWorkflowFile::getName))
        .toList();
    localPresets.clear();
    for (final LocalWizardWorkflowFile preset : newLocalPresets) {
      localPresets.put(preset.file(), preset);
    }
    localPresetsButton.setLastFiles(
        newLocalPresets.stream().map(LocalWizardWorkflowFile::file).toList());
  }

  private void applyPreset(LocalWizardWorkflowFile preset) {
    if (preset == null) {
      return;
    }
    appendPresetsToUi(preset.parts());
  }

  /**
   * @param partialWorkflow might contain some or all steps of the workflow
   */
  private void appendPresetsToUi(final WizardWorkflow partialWorkflow) {
    listenersActive = false;

    // keep current as default parameters
    workflowSteps.apply(partialWorkflow);

    for (var preset : workflowSteps) {
      ComboBox<WizardPreset> combo = combos.get(preset.part());
      if (combo != null) {
        combo.getSelectionModel().select(preset);
      }
    }
    listenersActive = true;
    createParameterPanes();
  }

  private void loadPresets() {
    // update all parameters to use them as a default for each step
    updateAllParametersFromUi();
    // only load those steps that were defined in the local preset file
    File prefPath = getWizardSettingsPath();
    FileChooser chooser = new FileChooser();
    chooser.setInitialDirectory(prefPath);
    chooser.getExtensionFilters().add(FILE_FILTER);
    chooser.setSelectedExtensionFilter(FILE_FILTER);
    File file = chooser.showOpenDialog(null);
    if (file == null) {
      return;
    }

    // use initial parameters to
    try {
      WizardWorkflow wizardPresets = WizardWorkflowIOUtils.loadFromFile(file, ALL_PRESETS);
      if (!wizardPresets.isEmpty()) {
        appendPresetsToUi(wizardPresets);
      }

    } catch (IOException e) {
      logger.log(Level.WARNING, "Cannot read batch wizard presets from " + file.getAbsolutePath(),
          e);
    }
  }

  private void savePresets() {
    // update the preset parameters
    updateAllParametersFromUi();
    WizardWorkflowSaveModule.setupAndSave(workflowSteps);
  }

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
  private @Nullable WizardWorkflow updateAllParametersFromUiAndCheckErrors() {
    List<String> errorMessages = new ArrayList<>();

    // Update parameters from pane and check
    updateAllParametersFromUi();
    workflowSteps.forEach(step -> step.parameters().checkParameterValues(errorMessages));

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
}
