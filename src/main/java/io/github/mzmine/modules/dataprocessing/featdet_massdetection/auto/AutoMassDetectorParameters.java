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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection.auto;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.DetectIsotopesParameter;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;

public class AutoMassDetectorParameters extends SimpleParameterSet {

  public static final DoubleParameter noiseLevel = new DoubleParameter("Noise level",
      "The minimum signal intensity to be considered a peak.",
      MZmineCore.getConfiguration().getIntensityFormat(), 1E3);

  public static final OptionalModuleParameter<DetectIsotopesParameter> detectIsotopes
      = new OptionalModuleParameter<>("Detect isotope signals below noise level",
      "Include peaks corresponding to isotope masses distribution of specified elements.",
      new DetectIsotopesParameter());

  public AutoMassDetectorParameters() {
    super(new Parameter[] {noiseLevel, detectIsotopes},
        "https://mzmine.github.io/mzmine_documentation/module_docs/featdet_mass_detection/mass-detection-algorithms.html#auto");
  }
}
