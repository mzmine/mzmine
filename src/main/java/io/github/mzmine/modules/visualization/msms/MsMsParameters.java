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

package io.github.mzmine.modules.visualization.msms;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboFieldParameter;
import io.github.mzmine.parameters.parametertypes.ComboFieldValue;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.ListDoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class MsMsParameters extends SimpleParameterSet {

  // Basic parameters

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter(1, 1);

  public static final RTRangeParameter rtRange = new RTRangeParameter();

  public static final MZRangeParameter mzRange =
      new MZRangeParameter("Product m/z", "Range of product m/z values");

  public static final ComboParameter<MsMsXYAxisType> xAxisType =
      new ComboParameter<>("X axis", "X axis type", MsMsXYAxisType
          .values(), MsMsXYAxisType.RETENTION_TIME);

  public static final ComboParameter<MsMsXYAxisType> yAxisType =
      new ComboParameter<>("Y axis", "Y axis type", MsMsXYAxisType
          .values(), MsMsXYAxisType.NEUTRAL_LOSS);

  public static final ComboParameter<MsMsZAxisType> zAxisType =
      new ComboParameter<>("Z axis", "Z axis type", MsMsZAxisType
          .values(), MsMsZAxisType.PRECURSOR_INTENSITY);

  public static final IntegerParameter msLevel = new IntegerParameter("MS level",
      "Scans MS level, must be greater than 1", 2, 2, 1000);

  // Most intense fragments filtering

  public static final OptionalParameter<ComboFieldParameter<IntensityFilteringType>> intensityFiltering
      = new OptionalParameter<>(new ComboFieldParameter<>("Intensities filtering",
      "Plot only ions with highest intensity values, see \"Help\" for detailed description of the options",
      IntensityFilteringType.class, false,
      new ComboFieldValue<>("95", IntensityFilteringType.BASE_PEAK_PERCENT)));

  // Diagnostic fragmentation filtering

  public static final ListDoubleParameter targetedMZ_List =
      new ListDoubleParameter("Diagnostic product ions (m/z)",
          "Scans not containing any ion with all input m/z values will not be plotted",
          false, null);

  public static final ListDoubleParameter targetedNF_List =
      new ListDoubleParameter("Diagnostic neutral loss values (Da)",
          "Scans not containing any ion with all input neutral loss values will not be plotted",
          false, null);

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

  public static final OptionalModuleParameter dffParameters = new OptionalModuleParameter(
      "Diagnostic fragmentation filtering", "See \"Help\" for detailed information",
      new SimpleParameterSet(new Parameter[]{targetedMZ_List, targetedNF_List}));

  public static final WindowSettingsParameter windowSettings = new WindowSettingsParameter();

  public MsMsParameters() {
    super(new Parameter[]{dataFiles, xAxisType, yAxisType, zAxisType, msLevel, rtRange,
        mzRange, mzTolerance, intensityFiltering, dffParameters, windowSettings},
        "https://mzmine.github.io/mzmine_documentation/visualization_modules/msmsplot/msms-plot.html");
  }

}
