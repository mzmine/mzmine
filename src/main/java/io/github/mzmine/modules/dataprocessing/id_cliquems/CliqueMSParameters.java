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
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public class CliqueMSParameters extends SimpleParameterSet {

  public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

  public static final MZToleranceParameter MZ_DIFF = new MZToleranceParameter("MZ tolerance",
      "If two features' relative difference of m/z values is less than MZ tolerance, they are candidate for similar features.");


  private  static final NumberFormat LLformatter = new DecimalFormat("#0.000000");

  public static final RTToleranceParameter RT_DIFF = new RTToleranceParameter("RT tolerance",
      "If two features' relative difference of rt values is less than RT tolerance, they are candidate for similar features.");

  public static final DoubleParameter IN_DIFF = new DoubleParameter("Intensity tolerance",
      "If two features' relative difference of intensity values is less than Intensity tolerance, they are candidate for similar features.",MZmineCore.getConfiguration().getIntensityFormat(),0.0004);

  public static final DoubleParameter TOL = new DoubleParameter("Log-likelihood tolerance",
      "Log likelihood function is maximised for clique formation. The iterations are stopped when the relative absolute change in current log likelihood with respect to the initial log likelihood is less than the log-likelihood tolerance value.",
      LLformatter,0.000001);

  public static final BooleanParameter FILTER = new BooleanParameter("Filter similar features",
      "Two features are similar if their relative change in m/z, rt and intensity are less than the respective tolerances.",true);

  public CliqueMSParameters(){
    super(new Parameter[]{PEAK_LISTS,FILTER,MZ_DIFF,RT_DIFF,IN_DIFF,TOL});
    MZ_DIFF.setValue(new MZTolerance(0, 5));
    RT_DIFF.setValue(new RTTolerance(false,0.0004));
  }

}
