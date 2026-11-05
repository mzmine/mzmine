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

package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.internal;

import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidCategories;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidMainClasses;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.CustomLipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.CustomLipidClassPreviewPane;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;

class AddCustomLipidClassSetupDialog extends ParameterSetupDialog {

  private final ReentrantLock lock = new ReentrantLock();
  private boolean isUpdateFromPreset = false;

  private final SplitPane paramPreviewSplit;
  private final CustomLipidClassPreviewPane previewPane = new CustomLipidClassPreviewPane();
  private String name;
  private String abbr;
  private LipidCategories lipidCategory;
  private LipidMainClasses lipidMainClass;
  private String backboneFormula;
  private LipidChainType[] lipidChainTypes;
  private LipidFragmentationRule[] customLipidClassFragmentationRules;

  public AddCustomLipidClassSetupDialog(boolean valueCheckRequired, ParameterSet parameters) {
    super(valueCheckRequired, parameters);
    this.name = parameters.getParameter(AddCustomLipidClassParameters.name).getValue();
    this.abbr = parameters.getParameter(AddCustomLipidClassParameters.abbr).getValue();
    this.lipidCategory = parameters.getParameter(AddCustomLipidClassParameters.lipidCategory)
        .getValue();
    this.lipidMainClass = parameters.getParameter(AddCustomLipidClassParameters.lipidMainClass)
        .getValue();
    this.backboneFormula = parameters.getParameter(AddCustomLipidClassParameters.backBoneFormula)
        .getValue();
    this.lipidChainTypes = parameters.getParameter(AddCustomLipidClassParameters.lipidChainTypes)
        .getValue();
    this.customLipidClassFragmentationRules = parameters.getParameter(
        AddCustomLipidClassParameters.customLipidClassFragmentationRules).getValue();
    paramPreviewSplit = new SplitPane();
    paramPreviewSplit.getItems().add(getParamPane());
    paramPreviewSplit.setOrientation(Orientation.HORIZONTAL);
    mainPane.setCenter(paramPreviewSplit);
    paramPreviewSplit.getItems().add(previewPane);
    paramPreviewSplit.setDividerPosition(0, 0.5);
    paramsPane.setHgap(7d);
    paramsPane.setVgap(1d);
    updateExampleLipid();

    ObservableList<LipidClasses> observableList = FXCollections.observableArrayList();
    observableList.addAll(Arrays.asList(LipidClasses.values()));
    ComboBox<LipidClasses> loadPresetLipid = new ComboBox<>(observableList);
    loadPresetLipid.setPromptText("Preset lipid class");
    Tooltip loadPresetLipidTooltip = new Tooltip("Load a preset lipid class as a starting point");
    loadPresetLipid.setTooltip(loadPresetLipidTooltip);
    loadPresetLipid.setOnAction(event -> {
      LipidClasses selectedLipidClass = loadPresetLipid.getSelectionModel().getSelectedItem();
      updateWithPresetLipidClass(selectedLipidClass);
    });
    getButtonBar().getButtons().add(loadPresetLipid);
  }


  private void updateWithPresetLipidClass(LipidClasses selectedLipidClass) {
    try {
      lock.lock();
      isUpdateFromPreset = true;
      updateParametersFromPreset(selectedLipidClass);
    } finally {
      lock.unlock();
    }
  }

  private void updateParametersFromPreset(LipidClasses selectedLipidClass) {
    this.parameterSet.setParameter(AddCustomLipidClassParameters.name,
        selectedLipidClass.getName());
    this.parameterSet.setParameter(AddCustomLipidClassParameters.abbr,
        selectedLipidClass.getAbbr());
    this.parameterSet.setParameter(AddCustomLipidClassParameters.lipidCategory,
        selectedLipidClass.getCoreClass());
    this.parameterSet.setParameter(AddCustomLipidClassParameters.lipidMainClass,
        selectedLipidClass.getMainClass());
    this.parameterSet.setParameter(AddCustomLipidClassParameters.lipidChainTypes,
        selectedLipidClass.getChainTypes());
    this.parameterSet.setParameter(AddCustomLipidClassParameters.backBoneFormula,
        selectedLipidClass.getBackBoneFormula());
    this.parameterSet.setParameter(AddCustomLipidClassParameters.customLipidClassFragmentationRules,
        selectedLipidClass.getFragmentationRules());
    setParameterValuesToComponents();
    updateExampleLipid();

    isUpdateFromPreset = true;
    parametersChanged();
    isUpdateFromPreset = false;
  }

  @Override
  protected void parametersChanged() {
    try {
      lock.lock();
      if (!isUpdateFromPreset) {
        super.updateParameterSetFromComponents();
      }
      name = parameterSet.getParameter(AddCustomLipidClassParameters.name).getValue();
      abbr = parameterSet.getParameter(AddCustomLipidClassParameters.abbr).getValue();
      lipidCategory = parameterSet.getParameter(AddCustomLipidClassParameters.lipidCategory)
          .getValue();
      lipidMainClass = parameterSet.getParameter(AddCustomLipidClassParameters.lipidMainClass)
          .getValue();
      backboneFormula = parameterSet.getParameter(AddCustomLipidClassParameters.backBoneFormula)
          .getValue();
      lipidChainTypes = parameterSet.getParameter(AddCustomLipidClassParameters.lipidChainTypes)
          .getValue();
      customLipidClassFragmentationRules = parameterSet.getParameter(
          AddCustomLipidClassParameters.customLipidClassFragmentationRules).getValue();
      if (parameterSet.checkParameterValues(new ArrayList<>(), true)) {
        updateExampleLipid();
      }
    } finally {
      lock.unlock();
    }
  }


  private void updateExampleLipid() {
    if (name != null && abbr != null && lipidMainClass != null && lipidCategory != null
        && lipidChainTypes != null && lipidChainTypes.length > 0) {
      previewPane.setLipidClass(
          new CustomLipidClass(name, abbr, lipidCategory, lipidMainClass, backboneFormula,
              lipidChainTypes, customLipidClassFragmentationRules));
    } else {
      previewPane.setLipidClass(null);
    }
  }
}