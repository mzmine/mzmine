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

package net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel;

import javafx.scene.control.TreeItem;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingModule;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.setup.DPPSetupWindowController;
import net.sf.mzmine.parameters.ParameterSet;

/**
 * Stores {@link DataPointProcessingModule}s and their parameters in a tree item. All MZmineModules implementing
 * DataPointProcessingModule are automatically added in {@link DPPSetupWindowController}.
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class DPPModuleTreeItem extends TreeItem<String> {

  private String name;
  private DataPointProcessingModule module;
  private ModuleSubCategory subCat;
  private ParameterSet parameters;

  public DPPModuleTreeItem(DataPointProcessingModule module) {
    super(module.getName());
    setName(module.getName());
    setModule(module);
    setSubCat(module.getModuleSubCategory());
    setParameters(MZmineCore.getConfiguration().getModuleParameters(module.getClass()));
  }

  public String getName() {
    return name;
  }

  public DataPointProcessingModule getModule() {
    return module;
  }

  private void setName(String name) {
    this.name = name;
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
}
