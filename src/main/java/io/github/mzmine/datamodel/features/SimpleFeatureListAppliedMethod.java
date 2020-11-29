package io.github.mzmine.datamodel.features;

import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.parameters.ParameterSet;
import javax.annotation.Nonnull;

public class SimpleFeatureListAppliedMethod implements FeatureListAppliedMethod {

  private String description;
  private String parameters;

  public SimpleFeatureListAppliedMethod(String description, ParameterSet parameters) {
    this.description = description;
    if (parameters != null) {
      this.parameters = parameters.toString();
    } else {
      this.parameters = "";
    }
  }

  public SimpleFeatureListAppliedMethod(String description, String parameters) {
    this.description = description;
    this.parameters = parameters;
  }

  public SimpleFeatureListAppliedMethod(String description) {
    this.description = description;
  }

  public @Nonnull
  String getDescription() {
    return description;
  }

  public String toString() {
    return description;
  }

  public @Nonnull String getParameters() {
    return parameters;
  }

}