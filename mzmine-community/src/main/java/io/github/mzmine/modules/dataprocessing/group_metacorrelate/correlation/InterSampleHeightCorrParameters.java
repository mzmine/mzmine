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

package io.github.mzmine.modules.dataprocessing.group_metacorrelate.correlation;


import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.util.maths.similarity.SimilarityMeasure;
import java.util.Map;

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
  public static final IntegerParameter MIN_DP = new IntegerParameter("Minimum samples",
      "Minimum number of samples (data points) to be used for correlation", 2, 2, 100000);

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
    super(isSub ? new Parameter[]{MIN_DP, MEASURE, MIN_CORRELATION}
        : new Parameter[]{MIN_HEIGHT, NOISE_LEVEL, MIN_DP, MEASURE, MIN_CORRELATION});
  }


  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    // parameters were renamed but stayed the same type
    var nameParameterMap = super.getNameParameterMap();
    // we use the same parameters here so no need to increment the version. Loading will work fine
    nameParameterMap.put("Min data points", MIN_DP);
    return nameParameterMap;
  }
}
