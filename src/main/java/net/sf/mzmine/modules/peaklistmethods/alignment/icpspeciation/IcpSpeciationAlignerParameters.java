/*
 * Copyright 2006-2019 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.alignment.icpspeciation;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

public class IcpSpeciationAlignerParameters extends SimpleParameterSet {
  public static final PeakListsParameter icpPeakList =
      new PeakListsParameter("ICP-peak list", 1, 1);

  public static final PeakListsParameter moleculePeakList =
      new PeakListsParameter("Molecule-MS peak list", 1, 1);

  public static final RTToleranceParameter rtTolerance = new RTToleranceParameter("Rt tolerance",
      "Defines the retention time tolerance between ICP and molecule MS scans.");

  public static final DoubleParameter peakShapeCorrelationScore =
      new DoubleParameter("Peak shape score", "");

  public IcpSpeciationAlignerParameters() {
    super(new Parameter[] {icpPeakList, moleculePeakList, rtTolerance, peakShapeCorrelationScore});
  }
}
