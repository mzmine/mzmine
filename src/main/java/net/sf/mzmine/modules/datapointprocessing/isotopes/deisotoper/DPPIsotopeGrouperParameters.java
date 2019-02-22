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

package net.sf.mzmine.modules.datapointprocessing.isotopes.deisotoper;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class DPPIsotopeGrouperParameters extends SimpleParameterSet {
  
  public static final String ChooseTopIntensity = "Most intense";
  public static final String ChooseLowestMZ = "Lowest m/z";

  public static final String[] representativeIsotopeValues = {ChooseTopIntensity, ChooseLowestMZ};
  
//  public static final StringParameter element = new StringParameter("Element", "Element symbol of the element to deisotope for.");
  
  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

  public static final BooleanParameter monotonicShape = new BooleanParameter("Monotonic shape",
      "If true, then monotonically decreasing height of isotope pattern is required");

  public static final IntegerParameter maximumCharge = new IntegerParameter("Maximum charge",
      "Maximum charge to consider for detecting the isotope patterns");

  public static final ComboParameter<String> representativeIsotope = new ComboParameter<String>(
      "Representative isotope",
      "Which peak should represent the whole isotope pattern. For small molecular weight\n"
          + "compounds with monotonically decreasing isotope pattern, the most intense isotope\n"
          + "should be representative. For high molecular weight peptides, the lowest m/z\n"
          + "peptides, the lowest m/z isotope may be the representative.",
      representativeIsotopeValues);

  public static final BooleanParameter autoRemove = new BooleanParameter("Remove non-isotopes",
      "If checked, all peaks without an isotope pattern will not be displayed and not passed to the next module.");
  
  public DPPIsotopeGrouperParameters() {
    super(new Parameter[] {/*element,*/ mzTolerance, monotonicShape,
        maximumCharge, representativeIsotope, autoRemove});
  }
  
}
