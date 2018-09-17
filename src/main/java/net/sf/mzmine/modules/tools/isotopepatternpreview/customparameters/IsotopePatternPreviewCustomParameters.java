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


package net.sf.mzmine.modules.tools.isotopepatternpreview.customparameters;

import java.text.DecimalFormat;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.PercentParameter;

/**
 * 
 * @author Steffen Heuckeroth steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class IsotopePatternPreviewCustomParameters extends SimpleParameterSet {

  public static final PercentParameter minAbundance =
      new PercentParameter("Minimum abundance", "The minimum abundance (%) of Isotopes", 0.01);

  public static final DoubleParameter mergeWidth = new DoubleParameter("Merge width(Da)",
      "This will be used to merge isotope compositions in the calculated isotope pattern if they overlap.",
      MZmineCore.getConfiguration().getMZFormat(), 0.0005, 0.0d, 10.0d);

  public static final DoubleParameter minPatternIntensity =
      new DoubleParameter("Min. relative pattern intensity",
          "The minimum normalized intensity of a peak in the final calculated isotope pattern.\n"
              + "Min = 0.0, Max = 0.99...",
          new DecimalFormat("0.####"), 0.01, 0.0d, 0.99999);

  public static final IntegerParameter charge = new IntegerParameter("Charge",
      "Enter a charge to apply to the molecule. (e.g. [M]+ = +1 / [M]- = -1\n"
      + "This can also be set to 0 for the uncharged molecule.", 1, true);

  public IsotopePatternPreviewCustomParameters() {
    super(new Parameter[] {minAbundance, mergeWidth, minPatternIntensity, charge});
  }
}
