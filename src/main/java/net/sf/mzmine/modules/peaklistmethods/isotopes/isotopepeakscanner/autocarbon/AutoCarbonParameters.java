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
package net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepeakscanner.autocarbon;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;

/**
 * 
 * @author Steffen Heuckeroth s_heuc03@uni-muenster.de
 *
 */
public class AutoCarbonParameters extends SimpleParameterSet {
  
  public static final IntegerParameter minCarbon =
      new IntegerParameter("Min. carbon", "Minumum amount of carbon to search for.", 15, true);
  public static final IntegerParameter maxCarbon =
      new IntegerParameter("Max. carbon", "Maximum amount of carbon to search for.", 25, true);
  public static final IntegerParameter minPatternSize =
      new IntegerParameter("Min. pattern size", "Set the minimum amount of data points in an isotope pattern\n"
          + "Make sure this does not exclude every pattern.", 2, true);
  
  public AutoCarbonParameters() {
    super(new Parameter[] { minCarbon, maxCarbon, minPatternSize});
  }
}