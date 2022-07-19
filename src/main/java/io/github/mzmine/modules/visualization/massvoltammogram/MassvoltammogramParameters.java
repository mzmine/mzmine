/*
 * Copyright 2006-2022 The MZmine Development Team
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
 */

package io.github.mzmine.modules.visualization.massvoltammogram;

import static io.github.mzmine.modules.dataprocessing.filter_baselinecorrection.correctors.LocMinLoessCorrectorParameters.choices;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.MultiChoiceParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import java.awt.Button;
import java.text.DecimalFormat;
import javolution.lang.ValueType;

public class MassvoltammogramParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter files = new RawDataFilesParameter(1, 1);

  public static final ComboParameter<String> polarity = new ComboParameter<>("Polarity",
      "Polarity of the MS data to be used.", new String[]{"+", "-"});

  public static final DoubleParameter delayTime = new DoubleParameter("Delay Time / s",
      "Delay time before analytes from the EC cell reach the mass spectrometer",
      new DecimalFormat("0.0"), 30d);

  public static final DoubleParameter potentialRampSpeed = new DoubleParameter(
      "Potential ramp / mV/s", "Potential ramp speed in mV/s.", new DecimalFormat("0.0"), 10d);

  public static final DoubleParameter stepSize = new DoubleParameter("Potential steps / mV",
      "Potential step between drawn Spectra.", new DecimalFormat("0.0"), 100d);

  public static final DoubleRangeParameter potentialRange = new DoubleRangeParameter(
      "Potential range / mV", "Minimal and maximal potential of ramp.", new DecimalFormat("0.0"));

  public static final MZRangeParameter mzRange = new MZRangeParameter("m/z Range",
      "Minimal and maximal m/z.");

  public static final ComboParameter<ReactionMode> reactionMode = new ComboParameter<>(
      "Reaction mode", "Reaction mode of the experiment.", ReactionMode.values(),
      ReactionMode.OXIDATIVE);

  public MassvoltammogramParameters() {
    super(new Parameter[]{files, polarity, reactionMode, delayTime, potentialRampSpeed,
        potentialRange, stepSize, mzRange});
  }
}

