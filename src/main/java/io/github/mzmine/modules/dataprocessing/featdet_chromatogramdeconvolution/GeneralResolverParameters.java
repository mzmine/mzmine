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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution;

import io.github.mzmine.modules.dataprocessing.filter_groupms2.GroupMS2Parameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.CenterMeasureParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.util.R.REngineType;
import io.github.mzmine.util.maths.CenterMeasure;

public abstract class GeneralResolverParameters extends SimpleParameterSet {

  public static final FeatureListsParameter PEAK_LISTS = new FeatureListsParameter();

  public static final StringParameter SUFFIX = new StringParameter("Suffix",
      "This string is added to feature list name as suffix", "deconvoluted");

  public static final CenterMeasureParameter MZ_CENTER_FUNCTION = new CenterMeasureParameter(
      "m/z center calculation", "Median, average or an automatic log10-weighted approach",
      CenterMeasure.values(), null, CenterMeasure.MEDIAN, null);

  public static final BooleanParameter AUTO_REMOVE = new BooleanParameter(
      "Remove original feature list",
      "If checked, original chromatogram will be removed and only the deconvolved version remains");

  public static final OptionalModuleParameter<GroupMS2Parameters> groupMS2Parameters = new OptionalModuleParameter<>(
      "MS/MS scan pairing", "Set MS/MS scan pairing parameters.", new GroupMS2Parameters());

  /**
   * R engine type. Only added in parameter sets that need R.
   */
  public static final ComboParameter<REngineType> RENGINE_TYPE = new ComboParameter<REngineType>(
      "R engine", "The R engine to be used for communicating with R.", REngineType.values(),
      REngineType.RCALLER);

  public GeneralResolverParameters(Parameter[] parameters) {
    super(parameters);
  }

  public abstract PeakResolver getResolver();
}
