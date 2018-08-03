/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.sirius;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;

public class SingleRowIdentificationParameters extends SiriusParameters {
  public static final IntegerParameter SIRIUS_CANDIDATES = new IntegerParameter(
      "Number of candidates from Sirius method", "Maximum number of results to display", 5);

  public static final IntegerParameter FINGERID_CANDIDATES = new IntegerParameter(
      "Number of candidates from FingerId method", "Pass 0 to get all possible results", 5);

  public static final DoubleParameter NEUTRAL_MASS = new DoubleParameter(
      "Precursor m/z", "Value to use in the search query of precursor ion");

  public static final IntegerParameter SIRIUS_TIMEOUT =
      new IntegerParameter(
          "Timer for Sirius Identification job", "Specify the amount of seconds, during which Sirius Identification job should finish processing.",
          40
      );

  public SingleRowIdentificationParameters() {
    super(new Parameter[] {
        NEUTRAL_MASS,
        ionizationType,
        MZ_TOLERANCE,
        SIRIUS_CANDIDATES,
        FINGERID_CANDIDATES,
        ELEMENTS,
        SIRIUS_TIMEOUT
    });
  }
}
