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

package io.github.mzmine.modules.dataprocessing.group_metacorrelate.correlation;


import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.util.maths.similarity.SimilarityMeasure;

public class InterSampleHeightCorrParameters extends SimpleParameterSet {

  // ###############################################
  // MAIN
  /**
   * Filter by minimum height
   */
  public static final DoubleParameter MIN_HEIGHT = new DoubleParameter("Min height",
      "Used by min samples filter and MS annotations. Minimum height to recognize a feature (important to distinguish between real peaks and minor gap-filled).",
      MZmineCore.getConfiguration().getIntensityFormat(), 1E5);

  /**
   * Filter by minimum height
   */
  public static final DoubleParameter NOISE_LEVEL =
      new DoubleParameter("Noise level", "Noise level of MS1, used by feature shape correlation",
          MZmineCore.getConfiguration().getIntensityFormat(), 1E4);

  // #######################################################
  // SUB
  // intensity correlation across sample max intensities
  public static final PercentParameter MIN_CORRELATION = new PercentParameter("Min correlation",
      "Minimum percentage for Pearson intensity profile correlation in the same scan event across raw files.",
      0.70, 0d, 1d);


  // min data points to be used for correlation
  public static final IntegerParameter MIN_DP = new IntegerParameter("Min data points",
      "Minimum of data points to be used for correlation", 2, 2, 100000);

  public static final ComboParameter<SimilarityMeasure> MEASURE = new ComboParameter<>("Measure",
      "Similarity measure", SimilarityMeasure.values(), SimilarityMeasure.PEARSON);


  // Constructor
  public InterSampleHeightCorrParameters(double minR, int minDP, double minHeight,
      double noiseLevel, SimilarityMeasure measure) {
    this();
    this.getParameter(MIN_CORRELATION).setValue(minR);
    this.getParameter(MIN_DP).setValue(minDP);
    this.getParameter(MIN_HEIGHT).setValue(minHeight);
    this.getParameter(NOISE_LEVEL).setValue(noiseLevel);
    this.getParameter(MEASURE).setValue(measure);
  }


  public InterSampleHeightCorrParameters() {
    this(false);
  }

  /**
   * 
   */
  public InterSampleHeightCorrParameters(boolean isSub) {
    super(isSub ? new Parameter[] {MIN_DP, MEASURE, MIN_CORRELATION}
        : new Parameter[] {MIN_HEIGHT, NOISE_LEVEL, MIN_DP, MEASURE, MIN_CORRELATION});
  }

}
