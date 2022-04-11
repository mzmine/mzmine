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

package io.github.mzmine.modules.dataprocessing.id_lipididentification;

import java.text.DecimalFormat;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class LipidSearchMSMSParameters extends SimpleParameterSet {

  public static final MZToleranceParameter mzToleranceMS2 =
      new MZToleranceParameter("m/z tolerance MS2 level:",
          "Enter m/z tolerance for exact mass database matching on MS2 level");

  public static final DoubleParameter minimumMsMsScore = new DoubleParameter("Minimum MS/MS score:",
      "Explained intensity [%] of all signals in MS/MS spectrum", new DecimalFormat("#.0"), 60.0,
      0.0, 100.0);

  public static final BooleanParameter keepUnconfirmedAnnotations = new BooleanParameter(
      "Keep unconfirmed annotations",
      "WARNING!: If checked, annotations based on accurate mass without headgroup fragment annotations are kept.");

  public LipidSearchMSMSParameters() {
    super(new Parameter[] {mzToleranceMS2, minimumMsMsScore, keepUnconfirmedAnnotations});
  }

}
