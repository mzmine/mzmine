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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.modules.dataprocessing.group_metacorrelate.msms.similarity;


import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;

/**
 * MS/MS similarity check based on difference and signal comparison
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class NeutralLossSimilarityParameters extends SimpleParameterSet {

  public static final IntegerParameter MAX_DP_FOR_DIFF = new IntegerParameter(
      "Maximum DP for differences matching",
      "Difference (neutral loss) matching is done on a maximum of n MS2 signals per scan. All differences between these signals are calculated and matched between spectra.",
      25);

  public NeutralLossSimilarityParameters() {
    super(
        new Parameter[]{MAX_DP_FOR_DIFF});
  }

}
