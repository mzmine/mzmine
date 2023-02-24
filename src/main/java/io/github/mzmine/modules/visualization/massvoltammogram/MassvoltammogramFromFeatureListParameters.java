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


import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import java.text.DecimalFormat;

public class MassvoltammogramFromFeatureListParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureList = new FeatureListsParameter(1, 1);

  public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter(
      "Scan Selection", "Filter to choose the scans to be used.", new ScanSelection());

  public static final DoubleParameter delayTime = new DoubleParameter("Delay Time / s",
      "Delay time before analytes from the EC cell reach the mass spectrometer",
      new DecimalFormat("0.0"), 30d);

  public static final DoubleParameter potentialRampSpeed = new DoubleParameter(
      "Potential Ramp / mV/s", "Potential ramp speed in mV/s.", new DecimalFormat("0.0"), 10d);

  public static final DoubleParameter stepSize = new DoubleParameter("Potential Steps / mV",
      "Potential step between drawn Spectra.", new DecimalFormat("0.0"), 100d);

  public static final DoubleRangeParameter potentialRange = new DoubleRangeParameter(
      "Potential Range / mV", "Minimal and maximal potential of ramp.", new DecimalFormat("0.0"));

  public static final MZRangeParameter mzRange = new MZRangeParameter("m/z Range",
      "Minimal and maximal m/z.");

  public static final ComboParameter<ReactionMode> reactionMode = new ComboParameter<>(
      "Reaction Mode", "Reaction mode of the experiment.", ReactionMode.values(),
      ReactionMode.OXIDATIVE);

  public MassvoltammogramFromFeatureListParameters() {
    super(new Parameter[]{featureList, scanSelection, reactionMode, delayTime, potentialRampSpeed,
        potentialRange, stepSize, mzRange});
  }
}

