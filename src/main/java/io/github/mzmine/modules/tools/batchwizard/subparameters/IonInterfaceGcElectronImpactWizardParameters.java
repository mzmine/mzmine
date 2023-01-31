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

package io.github.mzmine.modules.tools.batchwizard.subparameters;

import com.google.common.collect.Range;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import java.text.NumberFormat;

public final class IonInterfaceGcElectronImpactWizardParameters extends
    IonInterfaceWizardParameters {

  public static final RTToleranceParameter intraSampleRTTolerance = new RTToleranceParameter(
      "Intra-sample RT tolerance",
      "Retention time tolerance for multiple signals of the same compound in the same "
          + "sample.\nUsed to detect isotopes or multimers/adducts of the same compound.");
  public static final RTToleranceParameter interSampleRTTolerance = new RTToleranceParameter(
      "Inter-sample RT tolerance",
      "Retention time tolerance for the same compound in different samples.\n"
          + "Used to align multiple measurements of the same sample or a batch run.");
  public static final IntegerParameter minNumberOfDataPoints = new IntegerParameter(
      "Min # of data points",
      "Minimum number of data points as used in chromatogram building and feature resolving.", 4, 1,
      Integer.MAX_VALUE);
  public static final DoubleParameter SN_THRESHOLD = new DoubleParameter("S/N threshold",
      "Signal to noise ratio threshold", NumberFormat.getNumberInstance(), 10.0, 0.0, null);
  public static final DoubleRangeParameter RT_FOR_CWT_SCALES_DURATION = new DoubleRangeParameter(
      "RT wavelet range",
      "Upper and lower bounds of retention times to be used for setting the wavelet scales. Choose a range that that simmilar to the range of peak widths expected to be found from the data.",
      MZmineCore.getConfiguration().getRTFormat(), true, true, Range.closed(0.001, 0.1));
  public static final DoubleParameter PREF_WINDOW_WIDTH = new DoubleParameter(
      "Deconvolution window width (min)", "Preferred width of deconvolution windows (in minutes).",
      NumberFormat.getNumberInstance(), 0.2);
  public static final DoubleParameter SAMPLE_COUNT_RATIO = new DoubleParameter(
      "Min confidence (between 0 and 1)",
      "A fraction of the total number of samples. An aligned feature must be detected at "
          + "least in several samples.\nThis parameter determines the minimum number of samples where a "
          + "feature must be detected.",
      NumberFormat.getInstance(), 0.7, 0.0, 1.0);
  public static final DoubleParameter SCORE_TOLERANCE = new DoubleParameter(
      "Alignment Score threshold ",
      "The minimum value of the similarity function required for features to be aligned together.",
      NumberFormat.getInstance(), 0.75, 0.0, 1.0);

  public IonInterfaceGcElectronImpactWizardParameters(
      final IonInterfaceWizardParameterFactory preset) {
    super(WizardPart.ION_INTERFACE, preset,
        // actual parameters
        minNumberOfDataPoints, intraSampleRTTolerance, interSampleRTTolerance,
        SN_THRESHOLD, RT_FOR_CWT_SCALES_DURATION, PREF_WINDOW_WIDTH, SAMPLE_COUNT_RATIO,  SCORE_TOLERANCE
        );
  }

  public IonInterfaceGcElectronImpactWizardParameters(
      final IonInterfaceWizardParameterFactory preset, final int minDataPoints,
      final RTTolerance intraSampleTolerance, final RTTolerance interSampleTolerance,
      final Double minFeatureHeight, final Double snThreshold,
      final Range<Double> rtforCWT, final Double windowWidth,
      final Double sampleCountRatio, final Double scoreTolerance) {

    this(preset);

    // defaults - others override those values
    setParameter(intraSampleRTTolerance, intraSampleTolerance);
    setParameter(interSampleRTTolerance, interSampleTolerance);
    setParameter(minNumberOfDataPoints, minDataPoints);
    setParameter(SN_THRESHOLD, snThreshold);
    setParameter(RT_FOR_CWT_SCALES_DURATION, rtforCWT);
    setParameter(PREF_WINDOW_WIDTH, windowWidth);
    setParameter(SAMPLE_COUNT_RATIO, sampleCountRatio);
    setParameter(SCORE_TOLERANCE, scoreTolerance);
  }

}
