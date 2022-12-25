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
import io.github.mzmine.modules.tools.batchwizard.WizardPreset.WizardPart;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupPane;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.javafx.FxIconUtil;
import java.util.ArrayList;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

public class BatchWizardTab extends SimpleTab {

  private final List<WizardPreset> presetParts = new ArrayList<>();
  private final Map<WizardPreset, @NotNull ParameterSetupPane> paramPaneMap = new HashMap<>();
  private TabPane tabPane;
  private HBox schemaPane;

  public BatchWizardTab() {
    super("Processing Wizard");
    createContentPane();
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
      final Image icon = FxIconUtil.loadImageFromResources(
          "icons/wizard/wizard_icons_" + parent + ".png");
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

  private VBox createTopMenu() {
    VBox vbox = new VBox(4);
    vbox.setAlignment(Pos.CENTER);
    VBox.setMargin(vbox, new Insets(5));

    var topPane = new HBox(4);
    topPane.setAlignment(Pos.CENTER);
    HBox.setMargin(topPane, new Insets(5));

    presetParts.clear();
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
            presetParts.remove(finalPartIndex);
            presetParts.add(finalPartIndex, newValue);
            createParameterPanes();
          });
    }

    Button createBatch = new Button("Create batch");
    createBatch.setOnAction(event -> createBatch());
    topPane.getChildren().addAll(createSpacer(), new Label("="), createSpacer(), createBatch);

    schemaPane = new HBox(0);
    schemaPane.setAlignment(Pos.CENTER);
    vbox.getChildren().addAll(topPane, schemaPane);
    return vbox;
  }

  public Region createSpacer() {
    var spacer = new Region();
    spacer.setPrefWidth(10);
    return spacer;
  }

  public void createBatch() {
    List<String> errorMessages = new ArrayList<>();

    // Update parameters from pane and check
    paramPaneMap.values().forEach(ParameterSetupPane::updateParameterSetFromComponents);
    paramPaneMap.forEach((key, value) -> key.parameters().checkParameterValues(errorMessages));

    if (!errorMessages.isEmpty()) {
      MZmineCore.getDesktop().displayErrorMessage("Please check the parameters.\n" + errorMessages);
      return;
    }
    ParameterSet wizardParam = MZmineCore.getConfiguration()
        .getModuleParameters(BatchWizardModule.class).cloneParameterSet();
    paramPaneMap.keySet().forEach(preset -> preset.setParametersToWizardParameters(wizardParam));

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
}
