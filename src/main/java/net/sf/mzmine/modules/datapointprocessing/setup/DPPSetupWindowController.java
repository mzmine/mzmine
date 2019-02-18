package net.sf.mzmine.modules.datapointprocessing.setup;

import java.awt.Window;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.modules.MZmineProcessingStep;
import net.sf.mzmine.modules.datapointprocessing.DataPointProcessingManager;
import net.sf.mzmine.modules.datapointprocessing.DataPointProcessingModule;
import net.sf.mzmine.modules.datapointprocessing.ModuleSubCategory;
import net.sf.mzmine.modules.impl.MZmineProcessingStepImpl;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.project.impl.MZmineProjectImpl;
import net.sf.mzmine.util.ExitCode;

public class DPPSetupWindowController {

  private static Logger logger = Logger.getLogger(DPPSetupWindowController.class.getName());

  @FXML
  private ResourceBundle resources;

  @FXML
  private URL location;

  @FXML
  private TreeView<String> tvProcessing;

  @FXML
  private Button btnApply;

  @FXML
  private Button btnSave;

  @FXML
  private Button btnAdd;

  @FXML
  private Button btnLoad;

  @FXML
  private TreeView<String> tvAllModules;

  @FXML
  private Button btnRemove;

  @FXML
  private Button btnSetParameters;

  @FXML
  void btnApplyClicked(ActionEvent event) {
    logger.finest(event.getSource().toString() + " clicked.");
    
    sendList();
    DPPSetupWindow.getInstance().hide();
  }

  @FXML
  void btnAddClicked(ActionEvent event) {
    logger.finest(event.getSource().toString() + " clicked.");
    
    TreeItem<String> selected = tvAllModules.getSelectionModel().getSelectedItem();

    if (selected instanceof DPPModuleTreeItem) {

      // a module cannot be added twice
      if (tvProcessing.getRoot().getChildren().contains(selected)) {
        logger.finest("Cannot add module " + ((DPPModuleTreeItem) selected).getModule().getName()
            + " to processing list twice.");
        return;
      }

      tvProcessing.getRoot().getChildren().add(selected);
      logger.finest("Added module " + ((DPPModuleTreeItem) selected).getModule().getName()
          + " to processing list.");
    } else {
      logger.finest("Cannot add item " + selected.getValue() + " to processing list.");
    }
  }

  @FXML
  void btnRemoveClicked(ActionEvent event) {
    logger.finest(event.getSource().toString() + " clicked.");
    
    // check if processing list is focused
    if (!tvProcessing.isFocused())
      return;

    TreeItem<String> selected = tvProcessing.getSelectionModel().getSelectedItem();
    if (selected instanceof DPPModuleTreeItem) {
      tvProcessing.getRoot().getChildren().remove(selected);
      logger.finest("Removed module " + ((DPPModuleTreeItem) selected).getModule().getName()
          + " from processing list.");
    } else {
      logger.finest("Cannot remove item " + selected.getValue() + " from processing list.");
    }
  }

  @FXML
  void btnSetParamClicked(ActionEvent event) {
    logger.finest(event.getSource().toString() + " clicked.");
    
    TreeItem<String> _selected = tvProcessing.getSelectionModel().getSelectedItem();
    if (!(_selected instanceof DPPModuleTreeItem))
      return;
    DPPModuleTreeItem selected = (DPPModuleTreeItem) _selected;

    MZmineModule module = selected.getModule();
    ParameterSet stepParameters =
        MZmineCore.getConfiguration().getModuleParameters(module.getClass());

    // do i even have to clone here? since, unlike batch mode, this is the only place we use this
    // parameter set.
//    ParameterSet stepParameters = methodParameters.cloneParameterSet();

    if (stepParameters.getParameters().length > 0) {
      // TODO ugh, is this ok since this is a javafx window?
      Window parent = (Window) SwingUtilities.getAncestorOfClass(Window.class,
          MZmineCore.getDesktop().getMainWindow());
      ExitCode exitCode = stepParameters.showSetupDialog(parent, false);
      if (exitCode != ExitCode.OK)
        return;
    }

    // store the parameters in the tree item
    selected.setParameters(stepParameters);
  }

  @FXML
  void btnLoadClicked(ActionEvent event) {
    logger.finest(event.getSource().toString() + " clicked.");
    // TODO
  }

  @FXML
  void btnSaveClicked(ActionEvent event) {
    logger.finest(event.getSource().toString() + " clicked.");
    // TODO
  }

  @FXML
  void initialize() {
    assert tvProcessing != null : "fx:id=\"tvProcessing\" was not injected: check your FXML file 'DPPSetupWindow.fxml'.";
    assert btnApply != null : "fx:id=\"btnApply\" was not injected: check your FXML file 'DPPSetupWindow.fxml'.";
    assert btnSave != null : "fx:id=\"btnSave\" was not injected: check your FXML file 'DPPSetupWindow.fxml'.";
    assert btnAdd != null : "fx:id=\"btnAdd\" was not injected: check your FXML file 'DPPSetupWindow.fxml'.";
    assert btnLoad != null : "fx:id=\"btnLoad\" was not injected: check your FXML file 'DPPSetupWindow.fxml'.";
    assert tvAllModules != null : "fx:id=\"tvAllModules\" was not injected: check your FXML file 'DPPSetupWindow.fxml'.";
    assert btnRemove != null : "fx:id=\"btnRemove\" was not injected: check your FXML file 'DPPSetupWindow.fxml'.";
    assert btnSetParameters != null : "fx:id=\"btnSetParameters\" was not injected: check your FXML file 'DPPSetupWindow.fxml'.";

    setupTreeViews();
  }

  /**
   * This method initializes the TreeViews. It adds all DataPointProcessingModules in
   * MZmineCore.getAllModules() to the module list of the tvAllModules. Categories are processed
   * automatically.
   */
  private void setupTreeViews() {
    TreeItem<String> tiAllModulesRoot = new TreeItem<String>("Modules");
    TreeItem<String> tiProcessingRoot = new TreeItem<String>("Processing steps");

    // create category items dynamically, if a new category is added later on.
    DPPModuleCategoryTreeItem[] moduleCategories =
        new DPPModuleCategoryTreeItem[ModuleSubCategory.values().length];
    for (int i = 0; i < moduleCategories.length; i++) {
      moduleCategories[i] = new DPPModuleCategoryTreeItem(ModuleSubCategory.values()[i]);
    }

    // add modules to their module category items
    Collection<MZmineModule> moduleList = MZmineCore.getAllModules();
    for (MZmineModule module : moduleList) {
      if (module instanceof DataPointProcessingModule) {
        DataPointProcessingModule dppm = (DataPointProcessingModule) module;

        // add each module as a child of the module category items
        for (DPPModuleCategoryTreeItem catItem : moduleCategories) {
          if (dppm.getModuleSubCategory().equals(catItem.getCategory())) {
            catItem.getChildren().add(new DPPModuleTreeItem(dppm));
          }
        }

      }
    }

    // add the categories to the root item
    tiAllModulesRoot.getChildren().addAll(moduleCategories);

    tvProcessing.setRoot(tiProcessingRoot);
    tvAllModules.setRoot(tiAllModulesRoot);

    tvAllModules.showRootProperty().set(true);
    tvProcessing.showRootProperty().set(true);
  }

  private MZmineProcessingStep<DataPointProcessingModule> createProcessingStep(
      DPPModuleTreeItem item) {
    return new MZmineProcessingStepImpl<>(item.getModule(), item.getParameters());
  }

  private void sendList() {
    if (tvProcessing.getRoot().getChildren().size() < 1)
      return;

    DataPointProcessingManager manager = DataPointProcessingManager.getInst();
    manager.clearProcessingSteps();

    List<MZmineProcessingStep<DataPointProcessingModule>> list =
        new ArrayList<MZmineProcessingStep<DataPointProcessingModule>>();
    
    for (TreeItem<String> item : tvProcessing.getRoot().getChildren()) {
      if (!(item instanceof DPPModuleTreeItem))
        continue;
      DPPModuleTreeItem moduleitem = (DPPModuleTreeItem) item;
      list.add(createProcessingStep(moduleitem));
    }

    manager.setProcessingSteps(list);
  }
}

