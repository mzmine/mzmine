package io.github.mzmine.datamodel.features;

import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import javax.annotation.Nonnull;

public class SimpleFeatureListAppliedMethod implements FeatureListAppliedMethod {

  private final String description;
  private final ParameterSet parameters;
  private final MZmineModule module;

  /**
   * @param parameters The parameter set used to create this feature list. A clone of the parameter
   *                   set is created in the constructor and saved in this class.
   */
  public SimpleFeatureListAppliedMethod(MZmineModule module, ParameterSet parameters) {
    this.parameters = parameters.cloneParameterSet();
    this.module = module;
    this.description = module.getName();
  }

  public SimpleFeatureListAppliedMethod(Class<? extends MZmineModule> moduleClass, ParameterSet parameters) {
    this.parameters = parameters.cloneParameterSet();
    this.module = MZmineCore.getModuleInstance(moduleClass);
    this.description = this.module.getName();
  }

  public SimpleFeatureListAppliedMethod(String description, MZmineModule module, ParameterSet parameters) {
    this.description = description;
    this.parameters = parameters.cloneParameterSet();
    this.module = module;
  }

  public SimpleFeatureListAppliedMethod(String description, Class<? extends MZmineModule> moduleClass, ParameterSet parameters) {
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
