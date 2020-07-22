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
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
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
      "If two features' relative difference of m/z values is less than MZ tolerance, they are candidate for similar features. So, if MZ tolerance is set a (relative) value of 'x' ppm (or absolute value of 'y'), then a feature with mz value of 'm' will have all peaks with the mz in the closed range [m - m*x/10e6 , m + m*x/10e6] (or [m - y, m + y] , whichever range is larger) similar to it (if rt and intensity tolerance values are passed too).");


  private  static final NumberFormat LLformatter = new DecimalFormat("#0.000000");

  public static final RTToleranceParameter RT_DIFF = new RTToleranceParameter("RT tolerance",
      "If two features' relative difference of rt values is less than RT tolerance, they are candidate for similar features. So, if RT tolerance is set a (relative) value of 'x', then a feature with rt value of 't' will have all peaks with rt in the closed range [t - t*x, t + t*x ] similar to it (if m/z and intensity tolerance values are passed too).");

  public static final DoubleParameter IN_DIFF = new DoubleParameter("Intensity tolerance",
      "If two features' relative difference of intensity values is less than Intensity tolerance, they are candidate for similar features.So, if Intensity tolerance is set a (relative) value of x, then a feature with intensity (absolute or relative) value 'i' will have all peaks with the intensity range [ i - i*x , i + i*x] similar to it (if m/z and rt tolerance values are passed too).",MZmineCore.getConfiguration().getIntensityFormat(),0.0004);

  public static final DoubleParameter TOL = new DoubleParameter("Log-likelihood tolerance",
      "Log likelihood function is maximised for clique formation. The iterations are stopped when the relative absolute change in current log likelihood with respect to the initial log likelihood is less than the log-likelihood tolerance value.",
      LLformatter,0.000001);

  public static final BooleanParameter FILTER = new BooleanParameter("Filter similar features",
      "Two features are similar if their relative change in m/z, rt and intensity are less than the respective tolerances.",true);

  // Max charge.
  public static final IntegerParameter ISOTOPES_MAX_CHARGE =
      new IntegerParameter("Isotopes max. charge",
          "The maximum charge considered when identifying isotopes", 3, 1, null);

  // Max isotopes.
  public static final IntegerParameter ISOTOPES_MAXIMUM_GRADE = new IntegerParameter(
      "Isotopes max. per cluster", "The maximum number of isotopes per cluster", 2, 0, null);

  // Isotope m/z tolerance
  public static final MZToleranceParameter ISOTOPES_MZ_TOLERANCE =
      new MZToleranceParameter("Isotopes mass tolerance",
          "Mass tolerance used when identifying isotopes, relative error in ppm to consider that two features have the mass difference of an isotope");

  //Isotope mass difference
  public static final DoubleParameter ISOTOPE_MASS_DIFF= new DoubleParameter("Isotope mass difference","The mass difference of the isotope",
      LLformatter,1.003355);

  public CliqueMSParameters(){
    super(new Parameter[]{PEAK_LISTS,FILTER,MZ_DIFF,RT_DIFF,IN_DIFF,TOL,ISOTOPES_MAX_CHARGE,ISOTOPES_MAXIMUM_GRADE,ISOTOPES_MZ_TOLERANCE,ISOTOPE_MASS_DIFF});
    MZ_DIFF.setValue(new MZTolerance(0, 5));
    RT_DIFF.setValue(new RTTolerance(false,0.0004));
    ISOTOPES_MZ_TOLERANCE.setValue(new MZTolerance(0,10));
  }

}
