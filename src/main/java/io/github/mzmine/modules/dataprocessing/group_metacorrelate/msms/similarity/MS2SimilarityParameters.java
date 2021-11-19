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


import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

/**
 * MS/MS similarity check based on difference and signal comparison
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class MS2SimilarityParameters extends SimpleParameterSet {

  // NOT INCLUDED in sub
  // General parameters
  public static final FeatureListsParameter FEATURE_LISTS = new FeatureListsParameter();
  // INCLUDED in sub
  // MZ-tolerance: deisotoping, adducts
  public static final MZToleranceParameter MZ_TOLERANCE = new MZToleranceParameter(
      "m/z tolerance (MS2)",
      "Tolerance value of the m/z difference between MS2 signals (add absolute tolerance to cover small neutral losses (5 ppm on m=18 is insufficient))",
      0.003, 10);

  public static final DoubleParameter MIN_HEIGHT = new DoubleParameter("Min height (in MS2)",
      "Minimum height of signal", MZmineCore.getConfiguration().getIntensityFormat(), 0d);

  public static final DoubleParameter MIN_COSINE_SIMILARITY = new DoubleParameter(
      "Min cosine similarity", "Minimum spectral cosine similarity",
      MZmineCore.getConfiguration().getScoreFormat(), 0.7, 0d, 1d);

  public static final BooleanParameter MODIFICATION_AWARE_COSINE = new BooleanParameter(
      "Modification aware similarity",
      "Allows the difference between the precursor m/z of the compared scans during signal matching. "
      + "A signal has to have the same m/z or be shifted by the same difference to be matched.",
      true);

  public static final BooleanParameter ONLY_BEST_MS2_SCAN = new BooleanParameter(
      "Only best MS2 scan", "Compares only the best MS2 scan (or all MS2 scans)", true);

  public static final IntegerParameter MIN_DP = new IntegerParameter("Minimum data points (DP)",
      "Minimum data points in MS2 scan mass list", 3);

  public static final IntegerParameter MIN_MATCH = new IntegerParameter("Minimum matched signals",
      "Minimum matched signals or neutral losses (m/z differences)", 3);

  public static final OptionalModuleParameter<NeutralLossSimilarityParameters> CHECK_NEUTRAL_LOSS_SIMILARITY = new OptionalModuleParameter<>(
      "Check MS2 neutral loss similarity",
      "Generates a list of m/z differences and calculates cosine similarity",
      new NeutralLossSimilarityParameters(), false);

  public MS2SimilarityParameters() {
    super(
        new Parameter[]{FEATURE_LISTS, MZ_TOLERANCE, ONLY_BEST_MS2_SCAN, MIN_HEIGHT, MIN_DP,
            MIN_MATCH, MIN_COSINE_SIMILARITY, MODIFICATION_AWARE_COSINE,
            CHECK_NEUTRAL_LOSS_SIMILARITY});
  }

}
