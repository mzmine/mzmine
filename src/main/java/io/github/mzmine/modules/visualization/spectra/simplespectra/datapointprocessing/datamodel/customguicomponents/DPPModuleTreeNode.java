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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.customguicomponents;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ModuleSubCategory;
import io.github.mzmine.parameters.ParameterSet;

/**
 * Stores {@link DataPointProcessingModule}s and their parameters in a tree item. All MZmineModules
 * implementing DataPointProcessingModule are automatically added in
 * {@link DPPSetupWindowController}.
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class DPPModuleTreeNode extends DisableableTreeNode {
  private DataPointProcessingModule module;
  private ModuleSubCategory subCat;
  private ParameterSet parameters;
  private boolean dialogShowing;

  /**
   * avoid usage of this constructor, this is only used to set up the all modules tree view. usually
   * it is beneficial to set the parameters at creation.
   * 
   * @param module
   */
  public DPPModuleTreeNode(DataPointProcessingModule module) {
    this(module, MZmineCore.getConfiguration().getModuleParameters(module.getClass()));
  }

  public DPPModuleTreeNode(DataPointProcessingModule module, ParameterSet parameters) {
    super(module.getName());
    setModule(module);
    setSubCat(module.getModuleSubCategory());
    setParameters(parameters);
    setDialogShowing(false);
  }

  public DataPointProcessingModule getModule() {
    return module;
  }

  private void setModule(DataPointProcessingModule module) {
    this.module = module;
  }

  public ModuleSubCategory getSubCat() {
    return subCat;
  }

  private void setSubCat(ModuleSubCategory subCat) {
    this.subCat = subCat;
  }

  public ParameterSet getParameters() {
    return parameters;
  }

  public void setParameters(ParameterSet parameters) {
    this.parameters = parameters;
  }

  public boolean isDialogShowing() {
    return dialogShowing;
  }

  public void setDialogShowing(boolean dialogShowing) {
    this.dialogShowing = dialogShowing;
  }

  @Override
  public DPPModuleTreeNode clone() {
    return new DPPModuleTreeNode(module, getParameters().cloneParameterSet());
  }
}
