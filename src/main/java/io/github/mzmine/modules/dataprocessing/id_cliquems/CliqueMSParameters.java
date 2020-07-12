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

package io.github.mzmine.modules.dataprocessing.id_cliquems;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import java.text.NumberFormat;

public class CliqueMSParameters extends SimpleParameterSet {

  public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

  public static final DoubleParameter MZ_DIFF = new DoubleParameter("MZ tolerance",
      "mz tolerance for finding similar features.", MZmineCore.getConfiguration().getMZFormat(),0.000005);

  public static final DoubleParameter RT_DIFF = new DoubleParameter("RT tolerance",
      "rt tolerance for finding similar features.",MZmineCore.getConfiguration().getRTFormat(),0.0004);

  public static final DoubleParameter IN_DIFF = new DoubleParameter("Intensity tolerance",
      "intensity tolerance for finding similar features.",MZmineCore.getConfiguration().getIntensityFormat(),0.0004);

  public static final DoubleParameter TOL = new DoubleParameter("Log-likelihood tolerance",
      "This parameter sets the minimum relative increase in log-likelihood.",
      NumberFormat.getInstance(),0.000001);

  public static final BooleanParameter FILTER = new BooleanParameter("Filter features",
      "filter similar features before network formation",true);

  public CliqueMSParameters(){
    super(new Parameter[]{PEAK_LISTS,FILTER,MZ_DIFF,RT_DIFF,IN_DIFF,TOL});
  }

}
