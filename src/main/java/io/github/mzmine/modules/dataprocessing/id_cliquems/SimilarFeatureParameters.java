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

package io.github.mzmine.modules.dataprocessing.id_cliquems;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

public class SimilarFeatureParameters extends SimpleParameterSet {

  public static final MZToleranceParameter MZ_DIFF = new MZToleranceParameter("MZ tolerance",
      "If two features' relative difference of m/z values is less than MZ tolerance, they are candidate for similar features. So, if MZ tolerance is set a (relative) value of 'x' ppm (or absolute value of 'y'), then a feature with mz value of 'm' will have all peaks with the mz in the closed range [m - m*x/10e6 , m + m*x/10e6] (or [m - y, m + y] , whichever range is larger) similar to it (if rt and intensity tolerance values are passed too).",
      0, 5);

  public static final RTToleranceParameter RT_DIFF = new RTToleranceParameter("RT tolerance",
      "If RT tolerance is set a relative value of 'x' (or absolute value 'y'), then a feature with rt value of 't' will have all peaks with rt in the closed range [t - t*x, t + t*x ]  ( or [t-y, t+y]) similar to it (if m/z and intensity tolerance values are passed too).");

  public static final DoubleParameter IN_DIFF = new DoubleParameter("Intensity tolerance (relative)",
      "If Intensity tolerance is set a value of x, then a feature with intensity value 'i' will have all peaks with the intensity range [ i - i*x , i + i*x] similar to it (Note - Tolerance is unitless)(if m/z and rt tolerance values are passed too).",
      MZmineCore.getConfiguration().getIntensityFormat(), 0.0004);

  public SimilarFeatureParameters() {
    super(new Parameter[]{MZ_DIFF, RT_DIFF, IN_DIFF});
    RT_DIFF.setValue(new RTTolerance(false, 0.0004f));

  }

}
