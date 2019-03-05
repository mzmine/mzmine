/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.setup;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import javax.swing.filechooser.FileNameExtensionFilter;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.modules.MZmineProcessingStep;
import net.sf.mzmine.modules.impl.MZmineProcessingStepImpl;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingManager;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingModule;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingParameters;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingQueue;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.DPPModuleCategoryTreeItem;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.DPPModuleTreeItem;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ModuleSubCategory;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.dialogs.LoadSaveFileChooser;

/**
 * Classic JavaFX controller for the SetupWindow.
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class DPPSetupWindowController {

  private static Logger logger = Logger.getLogger(DPPSetupWindowController.class.getName());

  // File chooser
  private LoadSaveFileChooser chooser;
  private static final String XML_EXTENSION = "xml";

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
  private Button btnSetDefault;


  @FXML
  void btnApplyClicked(ActionEvent event) {
    logger.finest(event.getSource().toString() + " clicked.");

    sendQueue();
  }

  @FXML
  void btnAddClicked(ActionEvent event) {
    logger.finest(event.getSource().toString() + " clicked.");

    addModule();
  }

  @FXML
  void btnRemoveClicked(ActionEvent event) {
    logger.finest(event.getSource().toString() + " clicked.");

    removeModule();
  }

  @FXML
  void btnSetParamClicked(ActionEvent event) {
    logger.finest(event.getSource().toString() + " clicked.");

    setParameters(tvProcessing.getSelectionModel().getSelectedItem());
  }

  @FXML
  void btnLoadClicked(ActionEvent event) {
    logger.finest(event.getSource().toString() + " clicked.");

    final File file = chooser.getLoadFile(DPPSetupWindow.getInstance().getFrame());

    DataPointProcessingQueue queue = DataPointProcessingQueue.loadFromFile(file);
    setTreeViewProcessingItemsFromQueue(queue);
    sendQueue();
  }

  @FXML
  void btnSaveClicked(ActionEvent event) {
    logger.finest(event.getSource().toString() + " clicked.");

    final File file = chooser.getSaveFile(DPPSetupWindow.getInstance().getFrame(), XML_EXTENSION);
    DataPointProcessingQueue queue = getProcessingQueueFromTreeView();
    queue.saveToFile(file);
  }

  @FXML
  void btnSetDefaultClicked(ActionEvent event) {
    final File file = chooser.getLoadFile(DPPSetupWindow.getInstance().getFrame());
    if (file != null) {
      DataPointProcessingManager.getInst().getParameters()
          .getParameter(DataPointProcessingParameters.defaultDPPQueue).setValue(file);
      logger.finest("Set default processing queue to: " + file.getAbsolutePath());
    }
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
    assert btnSetDefault != null : "fx:id=\"btnSetDefault\" was not injected: check your FXML file 'DPPSetupWindow.fxml'.";

    setupTreeViews();
    initTreeviewMouseEvents();
    initTreeViewMenus();

    // if set, load default processing steps already
    // DataPointProcessingManager manager = DataPointProcessingManager.getInst();
    // if (!manager.getProcessingQueue().isEmpty())
    // setTreeViewProcessingItemsFromQueue(manager.getProcessingQueue());

    chooser = new LoadSaveFileChooser("Select Processing Queue File");
    chooser.addChoosableFileFilter(new FileNameExtensionFilter("XML files", XML_EXTENSION));
  }

  /**
   * This method initializes the TreeViews. It adds all DataPointProcessingModules in
   * MZmineCore.getAllModules() to the module list of the tvAllModules. Categories are processed
   * automatically.
   */
  private void setupTreeViews() {
    TreeItem<String> tiAllModulesRoot = new TreeItem<>("Modules");
    TreeItem<String> tiProcessingRoot = new TreeItem<>("Processing steps");

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

    // tvAllModules.getRoot().setExpanded(true);
    // tvProcessing.getRoot().setExpanded(true);

  }

  private void initTreeviewMouseEvents() {

    // Parameter setting
    tvProcessing.setOnMouseClicked(e -> {
      if (e.getClickCount() < 2)
        return;
      logger.finest("Double clicked item in processing tree view.");
      setParameters(tvProcessing.getSelectionModel().getSelectedItem());
    });

    // Module addition
    tvAllModules.setOnMouseClicked(e -> {
      if (e.getClickCount() < 2)
        return;
      logger.finest("Double clicked item in all module tree view.");
      addModule();
    });
  }

  private void initTreeViewMenus() {
    ContextMenu menutvProcessing = new ContextMenu();

    MenuItem miProcessingRemove = new MenuItem("Remove selected");
    miProcessingRemove.setOnAction(e -> {
      removeModule();
    });

    MenuItem miProcessingSetParameters = new MenuItem("Set parameters");
    miProcessingSetParameters.setOnAction(e -> {
      setParameters(tvProcessing.getSelectionModel().getSelectedItem());
    });
    menutvProcessing.getItems().addAll(miProcessingRemove, miProcessingSetParameters);

    ContextMenu menutvAllModules = new ContextMenu();

    MenuItem menuItemAdd = new MenuItem("Add selected module");
    menuItemAdd.setOnAction(e -> {
      addModule();
    });
    MenuItem miAllModulesSetParameters = new MenuItem("Set parameters");
    miAllModulesSetParameters.setOnAction(e -> {
      setParameters(tvAllModules.getSelectionModel().getSelectedItem());
    });

    menutvAllModules.getItems().addAll(menuItemAdd, miAllModulesSetParameters);

    tvProcessing.setContextMenu(menutvProcessing);
    tvAllModules.setContextMenu(menutvAllModules);
  }

  /**
   * Opens the parameter setup dialog of the selected module.
   */
  private void setParameters(TreeItem<?> _selected) {
    if (_selected == null || !(_selected instanceof DPPModuleTreeItem))
      return;

    DPPModuleTreeItem selected = (DPPModuleTreeItem) _selected;

    MZmineModule module = selected.getModule();
    ParameterSet stepParameters =
        MZmineCore.getConfiguration().getModuleParameters(module.getClass());

    // do i even have to clone here? since, unlike batch mode, this is the only place we use this
    // parameter set.
    // ParameterSet stepParameters = methodParameters.cloneParameterSet();

    if (stepParameters.getParameters().length > 0) {
      ExitCode exitCode = stepParameters.showSetupDialog(null, false);
      if (exitCode != ExitCode.OK)
        return;
    }

    // store the parameters in the tree item
    selected.setParameters(stepParameters);
  }

  /**
   * Adds the selected module in the tvAllModules to the processing list
   */
  private void addModule() {
    TreeItem<String> selected = tvAllModules.getSelectionModel().getSelectedItem();
    if (selected == null)
      return;

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

  /**
   * Removes the selected module in the tvProcessingList from the list
   */
  private void removeModule() {
    TreeItem<String> selected = tvProcessing.getSelectionModel().getSelectedItem();
    if (selected == null)
      return;

    if (selected instanceof DPPModuleTreeItem) {
      tvProcessing.getRoot().getChildren().remove(selected);
      logger.finest("Removed module " + ((DPPModuleTreeItem) selected).getModule().getName()
          + " from processing list.");
    } else {
      logger.finest("Cannot remove item " + selected.getValue() + " from processing list.");
    }
  }

  /**
   * Creates a DataPointProcessingQueue from the items currently in the tree view.
   * 
   * @return Instance of DataPointProcessingQueue.
   */
  private DataPointProcessingQueue getProcessingQueueFromTreeView() {
    DataPointProcessingQueue list = new DataPointProcessingQueue();

    if (tvProcessing.getRoot().getChildren().size() < 1)
      return list;

    for (TreeItem<String> item : tvProcessing.getRoot().getChildren()) {
      if (!(item instanceof DPPModuleTreeItem))
        continue;
      DPPModuleTreeItem moduleitem = (DPPModuleTreeItem) item;
      list.add(createProcessingStep(moduleitem));
    }

    return list;
  }

  /**
   * Creates a MZmineProcessingStep<DataPointProcessingModule> from an DPPModuleTreeItem.
   * 
   * @param item Tree item.
   * @return Instance of MZmineProcessingStep<DataPointProcessingModule>.
   */
  private MZmineProcessingStep<DataPointProcessingModule> createProcessingStep(
      DPPModuleTreeItem item) {
    return new MZmineProcessingStepImpl<>(item.getModule(), item.getParameters());
  }

  /**
   * Sends the queue to the DataPointProcessingManager.
   */
  private void sendQueue() {
    if (tvProcessing.getRoot().getChildren().size() < 1)
      return;

    DataPointProcessingQueue queue = getProcessingQueueFromTreeView();
    if (queue.isEmpty())
      logger.info("Processing queue is empty. Sending empty list.");

    DataPointProcessingManager manager = DataPointProcessingManager.getInst();
    manager.clearProcessingSteps();
    manager.setProcessingQueue(queue);
  }

  /**
   * Creates a collection of DPPModuleTreeItem from a queue. Can be used after loading a queue from
   * a file.
   * 
   * @param queue The queue.
   * @return Collection<DPPModuleTreeItem>.
   */
  private Collection<DPPModuleTreeItem> createTreeItemsFromQueue(DataPointProcessingQueue queue) {
    Collection<DPPModuleTreeItem> items = new ArrayList<DPPModuleTreeItem>();

    for (MZmineProcessingStep<DataPointProcessingModule> step : queue) {
      logger.info("adding module " + step.getModule().getName() + " to the list.");
      for (TreeItem<?> categoryItem : tvAllModules.getRoot().getChildren()) {
        if (categoryItem instanceof DPPModuleCategoryTreeItem) {
          for (TreeItem<?> moduleItem : categoryItem.getChildren()) {
            if (moduleItem instanceof DPPModuleTreeItem
                && step.getModule().equals(((DPPModuleTreeItem) moduleItem).getModule())) {
              items.add((DPPModuleTreeItem) moduleItem);
            }
          }
        }
      }
    }

    return items;
  }

  /**
   * Convenience method to publicly set the items of the processing list from the tree view. Used to
   * set the default queue, if set, loaded by the manager's constructor.
   * 
   * @param queue
   */
  public void setTreeViewProcessingItemsFromQueue(DataPointProcessingQueue queue) {
    tvProcessing.getRoot().getChildren().addAll(createTreeItemsFromQueue(queue));
  }
}

