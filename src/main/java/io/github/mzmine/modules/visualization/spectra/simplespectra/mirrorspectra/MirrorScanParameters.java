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

package io.github.mzmine.modules.visualization.spectra.simplespectra.mirrorspectra;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.scans.similarity.Weights;

public class MirrorScanParameters extends SimpleParameterSet {

  public static final MZToleranceParameter mzTol = new MZToleranceParameter("m/z tolerance",
      "Tolerance to match signals in both scans", 0.0025, 20);

  public static final ComboParameter<Weights> weight = new ComboParameter<>("Weights",
      "Weights for m/z and intensity", Weights.VALUES, Weights.MASSBANK);

  public static final OptionalParameter<MZToleranceParameter> removePrecursor = new OptionalParameter<>(
      new MZToleranceParameter("Remove m/z around precursor", "Removes residual precursor signals",
          5, 0));

  /**
   * Windows size and position
   */
  public static final WindowSettingsParameter windowSettings = new WindowSettingsParameter();

  public MirrorScanParameters() {
    super(new Parameter[]{mzTol, weight, removePrecursor, windowSettings});
  }

}
