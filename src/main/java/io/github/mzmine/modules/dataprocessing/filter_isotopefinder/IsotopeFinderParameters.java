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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder;

import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.elements.ElementsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class IsotopeFinderParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final ElementsParameter elements = new ElementsParameter("Chemical elements",
      "Chemical elements which isotopes will be considered");
  public static final MZToleranceParameter isotopeMzTolerance = new MZToleranceParameter(0.0005,
      10);
  public static final IntegerParameter maxCharge = new IntegerParameter(
      "Maximum charge of isotope m/z",
      "Maximum possible charge of isotope distribution m/z's. All present m/z values obtained by dividing "
      + "isotope masses with 1, 2, ..., maxCharge values will be considered. The default value is 1, "
      + "but insert an integer greater than 1 if you want to consider ions of higher charge states.",
      1, true, 1, 1000);

  public static final ComboParameter<ScanRange> scanRange = new ComboParameter<>("Search in scans",
      " Options to search isotopes in the single most intense scan"
      + " or within all scans in full-width at half maximum range.", ScanRange.values(),
      ScanRange.SINGLE_MOST_INTENSE);

  public IsotopeFinderParameters() {
    super(new UserParameter[]{featureLists, elements, isotopeMzTolerance, maxCharge, scanRange});
  }

  public enum ScanRange {
    // IN_FWHM,
    SINGLE_MOST_INTENSE;

    @Override
    public String toString() {
      return super.toString().replaceAll("_", " ");
    }
  }

}
