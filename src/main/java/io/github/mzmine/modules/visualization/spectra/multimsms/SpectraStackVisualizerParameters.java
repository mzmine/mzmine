/*
 * Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.spectra.multimsms;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;

/**
 * Holds all parameters for the multi msms visualizer
 *
 * @author Robin Schmid <a href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
public class SpectraStackVisualizerParameters extends SimpleParameterSet {

  public static final OptionalParameter<IntegerParameter> columns = new OptionalParameter<>(
      new IntegerParameter("Specify columns", "Number of columns, otherwise auto detect", 2),
      false);

  public static final BooleanParameter useBestForEach = new BooleanParameter("Best MS/MS",
      "Use best MS/MS spectrum for each feature, might come from different raw files", true);
  public static final BooleanParameter useBestMissingRaw = new BooleanParameter(
      "Replace missing MS/MS",
      "Replace missing MS/MS (in selected file) with the best available in all raw data files",
      true);


  public static final BooleanParameter showCrosshair = new BooleanParameter("Crosshair", "", true);
  public static final BooleanParameter showAllAxes = new BooleanParameter("All axes",
      "Show all or only bottom axis", false);
  public static final BooleanParameter showTitle = new BooleanParameter("Titles", "", false);
  public static final BooleanParameter showLegend = new BooleanParameter("Legends", "", false);


  public SpectraStackVisualizerParameters() {
    super(new Parameter[]{columns, useBestForEach, useBestMissingRaw, showCrosshair, showAllAxes,
        showTitle, showLegend});
  }

}
