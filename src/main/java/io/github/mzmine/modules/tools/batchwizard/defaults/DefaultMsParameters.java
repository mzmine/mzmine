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

package io.github.mzmine.modules.tools.batchwizard.defaults;

import io.github.mzmine.modules.tools.batchwizard.BatchWizardMassSpectrometerParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;

public class DefaultMsParameters {

  public static final DefaultMsParameters defaultTofParameters = new DefaultMsParameters(5E2, 1E2,
      1E3, new MZTolerance(0.005, 20), new MZTolerance(0.0015, 3), new MZTolerance(0.004, 8));

  public static final DefaultMsParameters defaultImsTofParameters = new DefaultMsParameters(150d,
      1E2, 1E3, new MZTolerance(0.003, 15), new MZTolerance(0.0015, 3), new MZTolerance(0.004, 8));

  public static final DefaultMsParameters defaultOrbitrapPositiveParameters = new DefaultMsParameters(
      1E4, 3E3, 5E4, new MZTolerance(0.002, 10), new MZTolerance(0.0015, 3),
      new MZTolerance(0.0015, 5));

  public static final DefaultMsParameters defaultOrbitrapNegativeParameters = new DefaultMsParameters(
      1E4, 3E3, 5E4, new MZTolerance(0.002, 10), new MZTolerance(0.0015, 3),
      new MZTolerance(0.0015, 5));

  private final double ms1NoiseLevel;
  private final double ms2NoiseLevel;
  private final double minFeatureHeight;
  private final MZTolerance scanToScanMzTolerance;
  private final MZTolerance featureToFeatureMzTolerance;
  private final MZTolerance sampleToSampleMzTolerance;

  public DefaultMsParameters(double ms1NoiseLevel, double ms2NoiseLevel, double minFeatureHeight,
      MZTolerance scanToScanTolerance, MZTolerance featureToFeatureMzTolerance,
      MZTolerance sampleToSampleMzTolerance) {
    this.ms1NoiseLevel = ms1NoiseLevel;
    this.ms2NoiseLevel = ms2NoiseLevel;
    this.minFeatureHeight = minFeatureHeight;
    this.scanToScanMzTolerance = scanToScanTolerance;
    this.featureToFeatureMzTolerance = featureToFeatureMzTolerance;
    this.sampleToSampleMzTolerance = sampleToSampleMzTolerance;
  }

  public void setToParameterSet(ParameterSet params) {
    params.setParameter(BatchWizardMassSpectrometerParameters.ms1NoiseLevel, ms1NoiseLevel);
    params.setParameter(BatchWizardMassSpectrometerParameters.ms2NoiseLevel, ms2NoiseLevel);
    params.setParameter(BatchWizardMassSpectrometerParameters.minimumFeatureHeight,
        minFeatureHeight);
    params.setParameter(BatchWizardMassSpectrometerParameters.scanToScanMzTolerance,
        scanToScanMzTolerance);
    params.setParameter(BatchWizardMassSpectrometerParameters.featureToFeatureMzTolerance,
        featureToFeatureMzTolerance);
    params.setParameter(BatchWizardMassSpectrometerParameters.sampleToSampleMzTolerance,
        sampleToSampleMzTolerance);
  }
}
