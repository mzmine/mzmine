/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */
package io.github.mzmine.modules.dataprocessing.id_formula_sort;

import java.text.DecimalFormat;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.selectors.PeakListsParameter;

public class FormulaSortParameters extends SimpleParameterSet {

  public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

  public static final DoubleParameter MAX_PPM_WEIGHT =
      new DoubleParameter("Max ppm distance (weight)",
          "Score is calculated as (ppm distance-ppmMax)/ppmMax", new DecimalFormat("0.0"), 10d);

  public static final DoubleParameter ISOTOPE_SCORE_WEIGHT =
      new DoubleParameter("Weight isotope pattern score", "Weight for isotope pattern score",
          new DecimalFormat("0.0"), 1d);

  public static final DoubleParameter MSMS_SCORE_WEIGHT = new DoubleParameter("Weight MS/MS score",
      "Weight for MS/MS score", new DecimalFormat("0.0"), 1d);


  public FormulaSortParameters() {
    this(false);
  }

  public FormulaSortParameters(boolean isSub) {
    super(isSub ? new Parameter[] {MAX_PPM_WEIGHT, ISOTOPE_SCORE_WEIGHT, MSMS_SCORE_WEIGHT}
        : new Parameter[] {PEAK_LISTS, MAX_PPM_WEIGHT, ISOTOPE_SCORE_WEIGHT, MSMS_SCORE_WEIGHT});
  }
}
