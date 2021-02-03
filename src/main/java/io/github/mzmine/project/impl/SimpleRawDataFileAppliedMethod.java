/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.project.impl;

import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import javax.annotation.Nonnull;

public class SimpleRawDataFileAppliedMethod implements FeatureListAppliedMethod {

  private String description;
  private ParameterSet parameters;
  private MZmineModule module;

  /**
   * @param parameters  The parameter set used to create this feature list. A clone of the parameter
   *                    set is created in the constructor and saved in this class.
   */
  public SimpleRawDataFileAppliedMethod(MZmineModule module, ParameterSet parameters) {
    this.parameters = parameters.cloneParameterSet();
    this.module = module;
    this.description = module.getName();
  }

  public SimpleRawDataFileAppliedMethod(Class<? extends MZmineModule> moduleClass, ParameterSet parameters) {
    this.parameters = parameters.cloneParameterSet();
    this.module = MZmineCore.getModuleInstance(moduleClass);
    this.description = this.module.getName();
  }

  public SimpleRawDataFileAppliedMethod(String description, MZmineModule module, ParameterSet parameters) {
    this.description = description;
    this.parameters = parameters.cloneParameterSet();
    this.module = module;
  }

  public SimpleRawDataFileAppliedMethod(String description, Class<? extends MZmineModule> moduleClass, ParameterSet parameters) {
    this.description = description;
    this.parameters = parameters.cloneParameterSet();
    this.module = MZmineCore.getModuleInstance(moduleClass);
  }

  public @Nonnull
  String getDescription() {
    return description;
  }

  public String toString() {
    return description;
  }

  public @Nonnull
  ParameterSet getParameters() {
    // don't return the saved parameters, return a clone so parameters cannot be altered by accident.
    return parameters.cloneParameterSet();
  }

  @Override
  public MZmineModule getModule() {
    return module;
  }
}
