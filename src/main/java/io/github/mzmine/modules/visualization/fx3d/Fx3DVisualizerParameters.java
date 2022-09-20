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

package io.github.mzmine.modules.visualization.fx3d;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeaturesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;

/**
 * 3D visualizer parameter set
 */
public class Fx3DVisualizerParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

  public static final ScanSelectionParameter scanSelection =
      new ScanSelectionParameter(new ScanSelection(1));

  public static final MZRangeParameter mzRange = new MZRangeParameter();

  public static final FeaturesParameter features = new FeaturesParameter();

  public static final IntegerParameter rtResolution = new IntegerParameter(
      "Retention time resolution", "Number of data points on retention time axis", 500);

  public static final IntegerParameter mzResolution =
      new IntegerParameter("m/z resolution", "Number of data points on m/z axis", 500);

  /**
   * Windows size and position
   */
  public static final WindowSettingsParameter windowSettings = new WindowSettingsParameter();

  public Fx3DVisualizerParameters() {
    super(new Parameter[] {dataFiles, scanSelection, mzRange, features, rtResolution, mzResolution,
        windowSettings},
        "https://mzmine.github.io/mzmine_documentation/visualization_modules/ms_raw_data_overview/raw_data_additional.md");
  }

}
