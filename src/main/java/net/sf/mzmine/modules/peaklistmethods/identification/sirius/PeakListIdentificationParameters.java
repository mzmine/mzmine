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
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;

public class PeakListIdentificationParameters extends SiriusParameters {
  public static final PeakListsParameter peakLists = new PeakListsParameter();

  public static final IntegerParameter CANDIDATES_AMOUNT = new IntegerParameter("Amount of Sirius candidates to save",
      "Specify the amount of candidates to be saved after processing by Sirius module", 1);

  public static final IntegerParameter CANDIDATES_FINGERID = new IntegerParameter(
      "Amount of FingerId results to save",
      "Specify the amount of candidates to be saved after processing by Sirius & FingerId module",
      1, 1, 5);

  public static final IntegerParameter THREADS_AMOUNT = new IntegerParameter("Amount of parallel jobs",
          "Specify the amount of parallel processing jobs",
          4, 1, 20);


  public PeakListIdentificationParameters() {
    super(new Parameter[] {
        peakLists,
        ionizationType,
        MZ_TOLERANCE,
        ELEMENTS,
        CANDIDATES_AMOUNT,
        CANDIDATES_FINGERID,
        THREADS_AMOUNT,
        SIRIUS_TIMEOUT
    });
  }
}
