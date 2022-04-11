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
import io.github.mzmine.parameters.parametertypes.*;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import io.github.mzmine.util.maths.similarity.SimilarityMeasure;

public class FeatureShapeCorrelationParameters extends SimpleParameterSet {

  // AS MAIN SETTINGS
  // General parameters
  public static final FeatureListsParameter PEAK_LISTS = new FeatureListsParameter(0);
  // RT-tolerance: Grouping
  public static final RTToleranceParameter RT_TOLERANCE = new RTToleranceParameter("RT tolerance",
      "Maximum allowed difference of retention time to set a relationship between peaks");

  // min intensity of data points to be peak shape correlated
  public static final DoubleParameter NOISE_LEVEL_PEAK_SHAPE = new DoubleParameter(
      "Noise level (peak shape correlation)", "Only correlate data points >= noiseLevel.",
      MZmineCore.getConfiguration().getIntensityFormat(), 1E4);

  // use mass lists
  public static final BooleanParameter USE_MASS_LIST_DATA = new BooleanParameter(
      "Use mass list data", "Uses the raw data stored in the given mass list", true);


  // ############################################################
  // SUB
  // min data points to be used for correlation
  public static final IntegerParameter MIN_DP_CORR_PEAK_SHAPE =
      new IntegerParameter("Min data points",
          "Minimum of data points to be used for correlation of peak shapes.", 5, 5, 100000);

  // min data points to be used for correlation
  public static final IntegerParameter MIN_DP_FEATURE_EDGE = new IntegerParameter(
      "Min data points on edge", "Minimum of data points on both sides of apex.", 2, 2, 100000);

  public static final ComboParameter<SimilarityMeasure> MEASURE = new ComboParameter<>("Measure",
      "Similarity measure", SimilarityMeasure.values(), SimilarityMeasure.PEARSON);

  // minimum Pearson correlation (r) for feature grouping in the same scan event of one raw file
  public static final PercentParameter MIN_R_SHAPE_INTRA = new PercentParameter(
      "Min feature shape correlation",
      "Minimum percentage for feature shape correlation for feature grouping in the same scan event of one raw file.",
      0.85, 0d, 1d);

  // minimum Pearson correlation (r) for feature grouping in the same scan event of one raw file
  public static final OptionalParameter<PercentParameter> MIN_TOTAL_CORR =
      new OptionalParameter<>(new PercentParameter("Min total correlation",
          "Minimum total correlation (all correlated data points of all features across samples)",
          0.5, 0d, 1d));

  // Constructor
  public FeatureShapeCorrelationParameters(RTTolerance rtTol, double noiseLevel, int minDP,
                                           int minDPEdge, double minR, boolean useTotalCorr, double minTotalR,
                                           SimilarityMeasure measure) {
    super(new Parameter[] {RT_TOLERANCE, NOISE_LEVEL_PEAK_SHAPE, MIN_DP_CORR_PEAK_SHAPE,
        MIN_DP_FEATURE_EDGE, MEASURE, MIN_R_SHAPE_INTRA, MIN_TOTAL_CORR});
    this.getParameter(RT_TOLERANCE).setValue(rtTol);
    this.getParameter(NOISE_LEVEL_PEAK_SHAPE).setValue(noiseLevel);
    this.getParameter(MIN_DP_CORR_PEAK_SHAPE).setValue(minDP);
    this.getParameter(MIN_DP_FEATURE_EDGE).setValue(minDPEdge);
    this.getParameter(MIN_R_SHAPE_INTRA).setValue(minR);
    this.getParameter(MIN_TOTAL_CORR).setValue(useTotalCorr);
    this.getParameter(MIN_TOTAL_CORR).getEmbeddedParameter().setValue(minTotalR);
    this.getParameter(MEASURE).setValue(measure);
  }

  public FeatureShapeCorrelationParameters() {
    this(false);
  }

  /**
   * As sub settings: No retention time tolerance and peak lists
   * 
   * @param isSub sub parameters
   */
  public FeatureShapeCorrelationParameters(boolean isSub) {
    super(isSub ? // no peak list and rt tolerance
        new Parameter[] {MIN_DP_CORR_PEAK_SHAPE, MIN_DP_FEATURE_EDGE, MEASURE, MIN_R_SHAPE_INTRA,
            MIN_TOTAL_CORR}
        : new Parameter[] {PEAK_LISTS, RT_TOLERANCE,
            // USE_MASS_LIST_DATA, MAIN_PEAK_HEIGHT,
            NOISE_LEVEL_PEAK_SHAPE, MIN_DP_CORR_PEAK_SHAPE, MIN_DP_FEATURE_EDGE, MEASURE,
            MIN_R_SHAPE_INTRA, MIN_TOTAL_CORR});
  }

}
