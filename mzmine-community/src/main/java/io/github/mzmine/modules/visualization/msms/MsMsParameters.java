/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboFieldParameter;
import io.github.mzmine.parameters.parametertypes.ComboFieldValue;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.ListDoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter.Options;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilterParameter;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class MsMsParameters extends SimpleParameterSet {

  // Basic parameters

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter(1);

  public static final OptionalParameter<RTRangeParameter> rtRange = new OptionalParameter<>(
      new RTRangeParameter(), false);

  public static final OptionalParameter<MZRangeParameter> mzRange = new OptionalParameter<>(
      new MZRangeParameter("Product m/z", "Range of product m/z values"), false);

  public static final ComboParameter<MsMsXYAxisType> xAxisType = new ComboParameter<>("X axis",
      "X axis type", MsMsXYAxisType.values(), MsMsXYAxisType.RETENTION_TIME);

  public static final ComboParameter<MsMsXYAxisType> yAxisType = new ComboParameter<>("Y axis",
      "Y axis type", MsMsXYAxisType.values(), MsMsXYAxisType.PRECURSOR_MZ);

  public static final ComboParameter<MsMsZAxisType> zAxisType = new ComboParameter<>("Z axis",
      "Z axis type", MsMsZAxisType.values(), MsMsZAxisType.PRECURSOR_INTENSITY);


  public static final MsLevelFilterParameter msLevel = new MsLevelFilterParameter(
      Options.EXCEPT_MS1, new MsLevelFilter(Options.MSn));

  // Most intense fragments filtering

  public static final OptionalParameter<ComboFieldParameter<IntensityFilteringType>> intensityFiltering = new OptionalParameter<>(
      new ComboFieldParameter<>("Intensities filtering",
          "Plot only ions with highest intensity values, see \"Help\" for detailed description of the options",
          IntensityFilteringType.class, false,
          new ComboFieldValue<>("95", IntensityFilteringType.BASE_PEAK_PERCENT)));

  // Diagnostic fragmentation filtering

  public static final ListDoubleParameter targetedMZ_List = new ListDoubleParameter(
      "Diagnostic product ions (m/z)",
      "Scans not containing any ion with all input m/z values will not be plotted", false, null);

  public static final ListDoubleParameter targetedNF_List = new ListDoubleParameter(
      "Diagnostic neutral loss values (Da)",
      "Scans not containing any ion with all input neutral loss values will not be plotted", false,
      null);

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

  public static final OptionalModuleParameter dffParameters = new OptionalModuleParameter(
      "Diagnostic fragmentation filtering", "See \"Help\" for detailed information",
      new SimpleParameterSet(new Parameter[]{targetedMZ_List, targetedNF_List}));

  public static final WindowSettingsParameter windowSettings = new WindowSettingsParameter();

  public MsMsParameters() {
    super(new Parameter[]{dataFiles, xAxisType, yAxisType, zAxisType, msLevel, rtRange, mzRange,
            mzTolerance, intensityFiltering, dffParameters, windowSettings},
        "https://mzmine.github.io/mzmine_documentation/visualization_modules/msmsplot/msms-plot.html");
  }

  /**
   * build default parameters for MS/MS scatter plot
   */
  public static ParameterSet getDefaultMsMsPrecursorParameters(RawDataFile[] raws) {
    ParameterSet params = new MsMsParameters().cloneParameterSet();
    params.getParameter(dataFiles).setValue(new RawDataFilesSelection(raws));
    params.setParameter(mzRange, false);
    params.setParameter(rtRange, false);
    params.setParameter(intensityFiltering, false);
    params.setParameter(msLevel, new MsLevelFilter(Options.MSn));
    params.setParameter(xAxisType, MsMsXYAxisType.RETENTION_TIME);
    params.setParameter(yAxisType, MsMsXYAxisType.PRECURSOR_MZ);
    params.setParameter(zAxisType, MsMsZAxisType.PRECURSOR_INTENSITY);
    params.setParameter(mzTolerance, new MZTolerance(0.005, 5));
    return params;
  }

}
