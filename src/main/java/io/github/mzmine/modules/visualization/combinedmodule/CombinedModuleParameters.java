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

package io.github.mzmine.modules.visualization.combinedmodule;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.MassListParameter;
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;

public class CombinedModuleParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

  public static final RTRangeParameter retentionTimeRange = new RTRangeParameter();

  public static final MassListParameter massList = new MassListParameter();

  public static final MZRangeParameter mzRange =
      new MZRangeParameter("Precursor m/z", "Range of precursor m/z values");

  public static final ComboParameter<AxisType> xAxisType =
      new ComboParameter<AxisType>("X axis", "X axis type", AxisType.values());

  public static final ComboParameter<AxisType> yAxisType =
      new ComboParameter<AxisType>("Y axis", "Y axis type", AxisType.values());

  public static final DoubleParameter noiseLevel = new DoubleParameter("Noise level",
      "Intensities less than this value are interpreted as noise.",
      MZmineCore.getConfiguration().getIntensityFormat());

  public static final ComboParameter<ColorScale> colorScale =
      new ComboParameter<ColorScale>("Color Scale", "Color Scale", ColorScale.values());

  public static final WindowSettingsParameter windowSettings = new WindowSettingsParameter();

  public CombinedModuleParameters() {
    super(new Parameter[]{dataFiles, xAxisType, yAxisType, massList, retentionTimeRange, mzRange,
        colorScale, noiseLevel, windowSettings});
  }
}
