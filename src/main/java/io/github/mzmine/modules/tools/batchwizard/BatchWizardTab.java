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
import io.github.mzmine.modules.tools.batchwizard.io.BatchWizardPresetIOUtils;
import io.github.mzmine.modules.tools.batchwizard.io.BatchWizardPresetSaveModule;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardIonMobilityParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterUtils;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;
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
   * The selected workflow. first - last step. Changes in the combobox selection are reflected here
   */
  private final List<WizardPreset> presetParts = new ArrayList<>();
  /**
   * Parameter panes of the selected presets
   */
  private final Map<File, LocalWizardPresetFile> localPresets = new HashMap<>();
  private final Map<WizardPreset, @NotNull ParameterSetupPane> paramPaneMap = new HashMap<>();
  private final Map<WizardPart, ComboBox<WizardPreset>> combos = new HashMap<>();
  private final LastFilesButton localPresetsButton;
  private boolean listenersActive = true;
  private TabPane tabPane;
  private HBox schemaPane;

  public BatchWizardTab() {
    super("Processing Wizard");
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
    Tab[] panes = presetParts.stream()
        // if IMS is deactivated, remove from list
        .map(preset -> {
          ParameterSetupPane paramPane = new ParameterSetupPane(true, false, preset.parameters());
          paramPaneMap.put(preset, paramPane);
          // add to schema
          addToSchema(preset);
          // do not add tabs for in active tabs
          if (!preset.name().equals(WizardIonMobilityParameters.ImsDefaults.NO_IMS.toString())) {
            return new Tab(preset.name(), paramPane);
          } else {
            return null;
          }
        }).filter(Objects::nonNull).toArray(Tab[]::new);

    // add to center pane
    tabPane.getTabs().clear();
    tabPane.getTabs().addAll(panes);
    tabPane.getSelectionModel().select(selectedIndex);
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

    presetParts.clear();
    combos.clear();
    // create combo boxes for each part of the wizard that has multiple options
    // LC/GC - IMS? - MS instrument, Apply defaults
    var presetMap = WizardDefaultPresets.createPresets();
    int partIndex = -1;
    for (final WizardPart part : WizardPart.values()) {
      partIndex++;
      var presets = FXCollections.observableArrayList(presetMap.get(part));
      presetParts.add(presets.get(0));
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
      final int finalPartIndex = partIndex;
      combo.getSelectionModel().selectedItemProperty()
          .addListener((observable, oldValue, newValue) -> {
            if (listenersActive) {
              presetParts.remove(finalPartIndex);
              presetParts.add(finalPartIndex, newValue);
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
            List<WizardPreset> presets = BatchWizardPresetIOUtils.loadFromFile(file);
            return new LocalWizardPresetFile(file, presets);
          } catch (IOException e) {
            logger.warning("Could not import wizard preset file " + file.getAbsolutePath());
            return null;
          }
        }).filter(Objects::nonNull).sorted(Comparator.comparing(LocalWizardPresetFile::getName))
        .toList();
    localPresets.clear();
    for (final LocalWizardPresetFile preset : newLocalPresets) {
      localPresets.put(preset.file(), preset);
    }
    localPresetsButton.setLastFiles(
        newLocalPresets.stream().map(LocalWizardPresetFile::file).toList());
  }

  private void applyPreset(LocalWizardPresetFile preset) {
    if (preset == null) {
      return;
    }
    appendPresetsToUi(preset.parts());
  }

  private void appendPresetsToUi(final List<WizardPreset> targetPresets) {
    listenersActive = false;
    // keep current as default parameters
    Map<WizardPart, WizardPreset> combined = presetParts.stream()
        .collect(Collectors.toMap(WizardPreset::part, p -> p));
    // change the target presets for the defined parts - might be all or only a few
    for (final WizardPreset preset : targetPresets) {
      combined.put(preset.part(), preset);
    }

    presetParts.clear();
    presetParts.addAll(combined.values());
    Collections.sort(presetParts);

    for (var preset : presetParts) {
      ComboBox<WizardPreset> combo = combos.get(preset.part());
      if (combo != null) {
        for (final WizardPreset item : combo.getItems()) {
          if (item.name().equals(preset.name())) {
            ParameterUtils.copyParameters(preset.parameters(), item.parameters());
            combo.getSelectionModel().select(item);
            break;
          }
        }
      }
    }
    createParameterPanes();
    listenersActive = true;
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
      List<WizardPreset> wizardPresets = BatchWizardPresetIOUtils.loadFromFile(file);
      if (wizardPresets != null && !wizardPresets.isEmpty()) {
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
    BatchWizardPresetSaveModule.setupAndSave(presetParts);
  }

  public void createBatch() {
    ParameterSet wizardParam = getWizardParametersFromPanes();
    if (wizardParam == null) {
      return;
    }

    BatchModeParameters batchModeParameters = (BatchModeParameters) MZmineCore.getConfiguration()
        .getModuleParameters(BatchModeModule.class);
    final BatchQueue q = new WizardBatchBuilder(wizardParam).createQueue();
    batchModeParameters.getParameter(BatchModeParameters.batchQueue).setValue(q);

    if (batchModeParameters.showSetupDialog(false) == ExitCode.OK) {
      MZmineCore.runMZmineModule(BatchModeModule.class, batchModeParameters.cloneParameterSet());
    }

    // keep old settings
    MZmineCore.getConfiguration().setModuleParameters(BatchWizardModule.class, wizardParam);
  }

  @Nullable
  private ParameterSet getWizardParametersFromPanes() {
    List<String> errorMessages = new ArrayList<>();

    // Update parameters from pane and check
    updateAllParametersFromUi();
    paramPaneMap.forEach((key, value) -> key.parameters().checkParameterValues(errorMessages));

    if (!errorMessages.isEmpty()) {
      MZmineCore.getDesktop().displayErrorMessage("Please check the parameters.\n" + errorMessages);
      return null;
    }
    ParameterSet wizardParam = MZmineCore.getConfiguration()
        .getModuleParameters(BatchWizardModule.class).cloneParameterSet();
    paramPaneMap.keySet().forEach(preset -> preset.setParametersToWizardParameters(wizardParam));
    return wizardParam;
  }

  private void updateAllParametersFromUi() {
    paramPaneMap.values().forEach(ParameterSetupPane::updateParameterSetFromComponents);
  }
}
