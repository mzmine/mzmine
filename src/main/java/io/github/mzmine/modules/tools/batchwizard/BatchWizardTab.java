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
import io.github.mzmine.modules.tools.batchwizard.WizardPreset.ImsDefaults;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupPane;
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

  /**
   * The selected workflow. first - last step. Changes in the combobox selection are reflected here
   */
  private final List<WizardPreset> presetParts = new ArrayList<>();
  /**
   * Parameter panes of the selected presets
   */
  private final Map<WizardPreset, @NotNull ParameterSetupPane> paramPaneMap = new HashMap<>();
  private final Map<WizardPart, ComboBox<WizardPreset>> combos = new HashMap<>();
  private final ComboBox<LocalWizardFile> localPresets = new ComboBox<>();
  private final ExtensionFilter FILE_FILTER;
  private boolean listenersActive = true;
  private TabPane tabPane;
  private HBox schemaPane;

  public BatchWizardTab() {
    super("Processing Wizard");
    createContentPane();
    FILE_FILTER = new ExtensionFilter("MZmine wizard preset", "*.mzmwizard");
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
          if (!preset.name().equals(ImsDefaults.NO_IMS.toString())) {
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
    String parent = preset.parentPreset().toLowerCase();
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
  private static File getWizardSettingsPath() {
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
    Map<WizardPart, List<WizardPreset>> map = WizardDefaultPresets.createPresets();
    int partIndex = -1;
    for (final WizardPart part : WizardPart.values()) {
      partIndex++;
      var presets = FXCollections.observableArrayList(map.get(part));
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

    Button apply = new Button("Apply");
    apply.setOnAction(event -> applyPreset());

    topPane.getChildren()
        .addAll(createSpacer(), new Label("="), createSpacer(), createBatch, save, load,
            localPresets, apply);

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
            List<WizardPreset> presets = BatchWizardParametersUtils.loadFromFile(file);
            return new LocalWizardFile(file, presets);
          } catch (IOException e) {
            logger.warning("Could not import wizard preset file " + file.getAbsolutePath());
            return null;
          }
        }).filter(Objects::nonNull).sorted(Comparator.comparing(LocalWizardFile::getName)).toList();
    localPresets.getItems().clear();
    localPresets.getItems().addAll(newLocalPresets);
  }

  private void applyPreset() {
    LocalWizardFile local = localPresets.getSelectionModel().getSelectedItem();
    if (local != null) {
      setPresetsToUi(local.parts());
    }
  }

  private void setPresetsToUi(final List<WizardPreset> targetPresets) {
    listenersActive = false;
    presetParts.clear();
    presetParts.addAll(targetPresets);

    for (var preset : targetPresets) {
      ComboBox<WizardPreset> combo = combos.get(preset.part());
      if (combo != null) {
        combo.getSelectionModel().select(preset);
      }
    }
    createParameterPanes();
    listenersActive = true;
  }

  private void loadPresets() {
    ParameterSet parameters = getWizardParametersFromPanes();
    if (parameters == null) {
      return;
    }

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
      List<WizardPreset> wizardPresets = BatchWizardParametersUtils.loadFromFile(file);
      if (wizardPresets != null && !wizardPresets.isEmpty()) {
        setPresetsToUi(wizardPresets);
      }

    } catch (IOException e) {
      logger.log(Level.WARNING, "Cannot read batch wizard presets from " + file.getAbsolutePath(),
          e);
    }
  }

  private void savePresets() {
    ParameterSet parameters = getWizardParametersFromPanes();
    if (parameters == null) {
      return;
    }

    File prefPath = getWizardSettingsPath();

    FileChooser chooser = new FileChooser();
    chooser.setInitialDirectory(prefPath);
    chooser.getExtensionFilters().add(FILE_FILTER);
    chooser.setSelectedExtensionFilter(FILE_FILTER);
    File file = chooser.showSaveDialog(null);
    if (file == null) {
      return;
    }
    try {
      BatchWizardParametersUtils.saveToFile(presetParts, file, true);
    } catch (IOException e) {
      logger.log(Level.WARNING, "Cannot write batch wizard presets to " + file.getAbsolutePath(),
          e);
    }
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
