/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.filter_groupms2.GroupMS2SubParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.util.R.REngineType;
import javafx.collections.FXCollections;
import org.jetbrains.annotations.Nullable;

public abstract class GeneralResolverParameters extends SimpleParameterSet {

  public static final FeatureListsParameter PEAK_LISTS = new FeatureListsParameter();

  public static final StringParameter SUFFIX = new StringParameter("Suffix",
      "This string is added to feature list name as suffix", "resolved");

  public static final OriginalFeatureListHandlingParameter handleOriginal = //
      new OriginalFeatureListHandlingParameter(true);

  public static final OptionalModuleParameter<GroupMS2SubParameters> groupMS2Parameters = new OptionalModuleParameter<>(
      "MS/MS scan pairing", "Set MS/MS scan pairing parameters.", new GroupMS2SubParameters());

  public static final ComboParameter<ResolvingDimension> dimension = new ComboParameter<>(
      "Dimension", "Select the dimension to be resolved.",
      FXCollections.observableArrayList(ResolvingDimension.values()),
      ResolvingDimension.RETENTION_TIME);

  /**
   * R engine type. Only added in parameter sets that need R.
   */
  public static final ComboParameter<REngineType> RENGINE_TYPE = new ComboParameter<REngineType>(
      "R engine", "The R engine to be used for communicating with R.", REngineType.values(),
      REngineType.RCALLER);

  public static final IntegerParameter MIN_NUMBER_OF_DATAPOINTS = new IntegerParameter(
      "Min # of data points", "Minimum number of data points on a feature", 3, true);

  public GeneralResolverParameters(Parameter[] parameters) {
    super(parameters);
  }

  @Deprecated
  public abstract FeatureResolver getResolver();

  @Nullable
  public Resolver getResolver(ParameterSet parameterSet, ModularFeatureList flist) {
    return null;
  }
}
