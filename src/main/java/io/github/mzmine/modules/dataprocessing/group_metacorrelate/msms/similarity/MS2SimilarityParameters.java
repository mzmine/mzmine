/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
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
