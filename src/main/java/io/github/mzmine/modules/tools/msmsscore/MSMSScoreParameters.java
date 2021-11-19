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

package io.github.mzmine.modules.tools.msmsscore;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class MSMSScoreParameters extends SimpleParameterSet {

  public static final MZToleranceParameter msmsTolerance = new MZToleranceParameter(
      "MS/MS m/z tolerance", "Tolerance of the mass value to search (+/- range)");

  public static final PercentParameter msmsMinScore = new PercentParameter("MS/MS score threshold",
      "If the score for MS/MS is lower, discard this match");

  public static final OptionalParameter<IntegerParameter> useTopNSignals = new OptionalParameter<>( new IntegerParameter("Use only top N signals",
      "Use only the most abundant N signals for scoring (speeds up the process)", 20), true);

  public MSMSScoreParameters() {
    super(new Parameter[] {msmsTolerance, msmsMinScore, useTopNSignals});
  }

}
