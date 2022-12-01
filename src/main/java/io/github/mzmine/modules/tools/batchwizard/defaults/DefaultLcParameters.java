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

import com.google.common.collect.Range;
import io.github.mzmine.modules.tools.batchwizard.BatchWizardHPLCParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;

public class DefaultLcParameters {

  public static final DefaultLcParameters uhplc = new DefaultLcParameters(Range.closed(0.3, 30d),
      new RTTolerance(0.05f, Unit.MINUTES), new RTTolerance(0.05f, Unit.MINUTES),
      new RTTolerance(0.1f, Unit.MINUTES));

  public static final DefaultLcParameters hplc = new DefaultLcParameters(Range.closed(0.5, 60d),
      new RTTolerance(0.1f, Unit.MINUTES), new RTTolerance(0.08f, Unit.MINUTES),
      new RTTolerance(0.4f, Unit.MINUTES));

  public static final DefaultLcParameters gc = new DefaultLcParameters(true, Range.closed(0.5, 60d),
      3, 1, 50, new RTTolerance(0.03f, Unit.MINUTES), new RTTolerance(0.03f, Unit.MINUTES),
      new RTTolerance(0.08f, Unit.MINUTES));

  public static final DefaultLcParameters hilic = new DefaultLcParameters(true,
      Range.closed(0.5d, 30d), 5, 1, 10, new RTTolerance(0.1f, Unit.MINUTES),
      new RTTolerance(3, Unit.SECONDS), new RTTolerance(3, Unit.SECONDS));

  private final boolean stableIonizationAcrossSamples;
  private final Range<Double> cropRtRange;
  private final int minNumberDataPoints;
  private final int minSamples;
  private final int maxIsomersInChromatogram;
  private final RTTolerance fwhm;
  private final RTTolerance intraSampleTolerance;
  private final RTTolerance interSampleTolerance;

  public DefaultLcParameters(Range<Double> rtRange, RTTolerance fwhm,
      RTTolerance intraSampleTolerance, RTTolerance interSampleTolerance) {
    this(true, rtRange, 4, 1, 15, fwhm, intraSampleTolerance, interSampleTolerance);
  }

  public DefaultLcParameters(boolean stableIonizationAcrossSamples, Range<Double> cropRtRange,
      int minNumberDataPoints, int minSamples, int maxIsomersInChromatogram, RTTolerance fwhm,
      RTTolerance intraSampleTolerance, RTTolerance interSampleTolerance) {
    this.stableIonizationAcrossSamples = stableIonizationAcrossSamples;
    this.cropRtRange = cropRtRange;
    this.minNumberDataPoints = minNumberDataPoints;
    this.minSamples = minSamples;
    this.maxIsomersInChromatogram = maxIsomersInChromatogram;
    this.fwhm = fwhm;
    this.intraSampleTolerance = intraSampleTolerance;
    this.interSampleTolerance = interSampleTolerance;
  }

  public void setToParameterSet(ParameterSet param) {
    param.setParameter(BatchWizardHPLCParameters.stableIonizationAcrossSamples,
        stableIonizationAcrossSamples);
    param.setParameter(BatchWizardHPLCParameters.minNumberOfSamples, minSamples);
    param.setParameter(BatchWizardHPLCParameters.cropRtRange, cropRtRange);
    param.setParameter(BatchWizardHPLCParameters.maximumIsomersInChromatogram,
        maxIsomersInChromatogram);
    param.setParameter(BatchWizardHPLCParameters.minNumberOfDataPoints, minNumberDataPoints);
    param.setParameter(BatchWizardHPLCParameters.approximateChromatographicFWHM, fwhm);
    param.setParameter(BatchWizardHPLCParameters.intraSampleRTTolerance, intraSampleTolerance);
    param.setParameter(BatchWizardHPLCParameters.interSampleRTTolerance, interSampleTolerance);
  }
}
