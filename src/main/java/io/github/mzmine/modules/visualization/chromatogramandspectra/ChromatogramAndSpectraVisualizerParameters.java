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

package io.github.mzmine.modules.visualization.chromatogramandspectra;

import io.github.mzmine.modules.visualization.chromatogram.TICPlotType;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class ChromatogramAndSpectraVisualizerParameters extends SimpleParameterSet {

  public static final MZToleranceParameter chromMzTolerance =
      new MZToleranceParameter("XIC tolerance",
          "m/z tolerance of the chromatogram builder for extracted ion chromatograms (XICs)", 0.001,
          10);

  public static final ScanSelectionParameter scanSelection =
      new ScanSelectionParameter("Chromatogram scan selection",
          "Parameters for scan selection the chromatogram will be build on.",
          new ScanSelection(null, null, null, null, null, null, 1, null));

  public static final ComboParameter<TICPlotType> plotType = new ComboParameter<>("Plot type",
      "Type of the chromatogram plot.", TICPlotType.values(), TICPlotType.BASEPEAK);

  public ChromatogramAndSpectraVisualizerParameters() {
    super(new Parameter[] {chromMzTolerance, scanSelection, plotType});
  }
}
