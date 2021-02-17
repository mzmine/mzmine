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

package io.github.mzmine.modules.dataprocessing.id_sirius;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;

public class PeakListIdentificationParameters extends SiriusParameters {
  public static final FeatureListsParameter peakLists = new FeatureListsParameter();

  public static final IntegerParameter CANDIDATES_AMOUNT = new IntegerParameter(
      "Amount of Sirius candidates to return",
      "Specify the amount of candidates to be saved after processing by Sirius Identification job",
      1);

  public static final IntegerParameter CANDIDATES_FINGERID =
      new IntegerParameter("Amount of FingerId results to return",
          "Specify the amount of candidates to be returned from a single Sirius result", 1);

  public static final IntegerParameter THREADS_AMOUNT = new IntegerParameter(
      "Amount of parallel jobs", "Specify the amount of parallel processing jobs", 8);

  public static final IntegerParameter SIRIUS_TIMEOUT = new IntegerParameter(
      "Timer for Sirius Identification job (sec)",
      "Specify the amount of seconds, during which Sirius Identification job should finish processing a row.",
      30);

  public PeakListIdentificationParameters() {
    super(new Parameter[] {peakLists, ionizationType, MZ_TOLERANCE, ELEMENTS, CANDIDATES_AMOUNT,
        CANDIDATES_FINGERID, THREADS_AMOUNT, SIRIUS_TIMEOUT});
  }
}
