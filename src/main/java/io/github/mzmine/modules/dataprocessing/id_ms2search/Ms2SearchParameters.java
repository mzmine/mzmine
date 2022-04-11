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

package io.github.mzmine.modules.dataprocessing.id_ms2search;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import org.jetbrains.annotations.NotNull;

public class Ms2SearchParameters extends SimpleParameterSet {

  public static final FeatureListsParameter peakList1 = new FeatureListsParameter("Feature List 1",
      1, 1);

  public static final FeatureListsParameter peakList2 = new FeatureListsParameter("Feature List 2",
      1, 1);


  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

  public static final DoubleParameter intensityThreshold = new DoubleParameter(
      "Minimum MS2 ion intensity", "Minimum ion intensity to consider in MS2 comparison");

  public static final IntegerParameter minimumIonsMatched =
      new IntegerParameter("Minimum ion(s) matched per MS2 comparison",
          "Minimum number of peaks between two MS2s that must match");

  public static final DoubleParameter scoreThreshold = new DoubleParameter(
      "Minimum spectral match score to report", "Minimum MS2 comparison score to report");

  public Ms2SearchParameters() {
    super(new Parameter[]{peakList1, peakList2, mzTolerance, intensityThreshold,
        minimumIonsMatched, scoreThreshold});
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
