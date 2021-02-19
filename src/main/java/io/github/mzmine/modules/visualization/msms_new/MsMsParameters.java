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

package io.github.mzmine.modules.visualization.msms_new;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboFieldValue;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.ListDoubleParameter;
import io.github.mzmine.parameters.parametertypes.ComboFieldParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class MsMsParameters extends SimpleParameterSet {

  // Basic parameters

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

  public static final RTRangeParameter rtRange = new RTRangeParameter();

  public static final MZRangeParameter mzRange =
      new MZRangeParameter("Product m/z", "Range of product m/z values");

  public static final ComboParameter<MsMsXYAxisType> xAxisType =
      new ComboParameter<>("X axis", "X axis type", MsMsXYAxisType
          .values());

  public static final ComboParameter<MsMsXYAxisType> yAxisType =
      new ComboParameter<>("Y axis", "Y axis type", MsMsXYAxisType
          .values());

  public static final ComboParameter<MsMsZAxisType> zAxisType =
      new ComboParameter<>("Z axis", "Z axis type", MsMsZAxisType
          .values());

  public static final IntegerParameter msLevel = new IntegerParameter("MS level",
      "MS level for plotting, must be greater than 1", 2, 2, 1000);

  // Most intense fragments filtering

  public static final OptionalParameter<ComboFieldParameter<IntensityFilteringType>> intensityFiltering
      = new OptionalParameter<>(new ComboFieldParameter<>("Intensities filtering",
      "Most intense fragments filtering to be plotted", IntensityFilteringType.class,
      false, new ComboFieldValue<>("95", IntensityFilteringType.BASE_PEAK_PERCENT)));

  // Diagnostic fragmentation filtering

  public static final ListDoubleParameter targetedMZ_List =
      new ListDoubleParameter("Diagnostic product ions (m/z)",
          "Product m/z-values that must be included in MS/MS", false, null);

  public static final ListDoubleParameter targetedNF_List =
      new ListDoubleParameter("Diagnostic neutral loss values (Da)",
          "Neutral loss m/z-values that must be included in MS/MS", false, null);

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

  public static final OptionalModuleParameter dffParameters = new OptionalModuleParameter(
      "Diagnostic fragmentation filtering", "",
      new SimpleParameterSet(new Parameter[]{targetedMZ_List, targetedNF_List}));

  public static final WindowSettingsParameter windowSettings = new WindowSettingsParameter();

  public MsMsParameters() {
    super(new Parameter[]{dataFiles, xAxisType, yAxisType, zAxisType, msLevel, rtRange,
        mzRange, mzTolerance, intensityFiltering, dffParameters, windowSettings});
  }

}
