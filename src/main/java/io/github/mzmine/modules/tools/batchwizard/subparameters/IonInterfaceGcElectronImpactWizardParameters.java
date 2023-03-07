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
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import java.text.NumberFormat;

public final class IonInterfaceGcElectronImpactWizardParameters extends
    IonInterfaceWizardParameters {

  public static final RTRangeParameter cropRtRange = new RTRangeParameter("Crop retention time",
      "Crops the RT range of chromatograms. Used to exclude time before the flow time\n"
      + "and after the separation, where in many runs cleaning and re-equilibration starts.", true,
      Range.closed(0.5, 30d));
  public static final RTToleranceParameter approximateChromatographicFWHM = new RTToleranceParameter(
      "Approximate feature FWHM",
      "The approximate feature width (chromatograpic peak width) in retention time (full-width-at-half-maximum, FWHM). ",
      new RTTolerance(0.05f, Unit.MINUTES));
  public static final RTToleranceParameter intraSampleRTTolerance = new RTToleranceParameter(
      "RT tolerance (intra-sample)",
      "Retention time tolerance for multiple signals of the same compound in the same "
      + "sample.\nUsed to detect isotopes or multimers/adducts of the same compound.",
      new RTTolerance(0.04f, Unit.MINUTES));
  public static final RTToleranceParameter interSampleRTTolerance = new RTToleranceParameter(
      "RT tolerance (sample-to-sample)",
      "Retention time tolerance for the same compound in different samples.\n"
      + "Used to align multiple measurements of the same sample or a batch run.",
      new RTTolerance(0.1f, Unit.MINUTES));
  public static final IntegerParameter minNumberOfDataPoints = new IntegerParameter(
      "Minimum consecutive scans",
      "Minimum number of consecutive scans with detected data points as used in chromatogram building and feature resolving.",
      4, 1, Integer.MAX_VALUE);
  public static final DoubleParameter SN_THRESHOLD = new DoubleParameter("Signal/noise threshold",
      "Signal to noise ratio threshold", NumberFormat.getNumberInstance(), 5d, 0.0, null);
  public static final DoubleRangeParameter RT_FOR_CWT_SCALES_DURATION = new DoubleRangeParameter(
      "Min/max feature width",
      "Upper and lower bounds of retention times to be used for setting the wavelet scales. Choose a range that that simmilar to the range of peak widths expected to be found from the data.",
      MZmineCore.getConfiguration().getRTFormat(), true, true, Range.closed(0.001, 0.06));

  public static final BooleanParameter smoothing = new BooleanParameter("Smoothing",
      "Apply smoothing in the retention time dimension, usually only needed if the peak shapes are spiky.",
      false);

  public IonInterfaceGcElectronImpactWizardParameters(
      final IonInterfaceWizardParameterFactory preset) {
    super(WizardPart.ION_INTERFACE, preset,
        // actual parameters
        smoothing, cropRtRange, minNumberOfDataPoints, SN_THRESHOLD, RT_FOR_CWT_SCALES_DURATION,
        // tolerances
        approximateChromatographicFWHM, intraSampleRTTolerance, interSampleRTTolerance);
  }

  public IonInterfaceGcElectronImpactWizardParameters(
      final IonInterfaceWizardParameterFactory preset, final boolean smoothingActive,
      final Range<Double> cropRt, final RTTolerance fwhm, final RTTolerance intraSampleTolerance,
      final RTTolerance interSampleTolerance, final int minDataPoints, final Double snThreshold,
      final Range<Double> rtforCWT) {

    this(preset);

    // defaults - others override those values
    setParameter(smoothing, smoothingActive);
    setParameter(cropRtRange, cropRt);
    setParameter(approximateChromatographicFWHM, fwhm);
    setParameter(intraSampleRTTolerance, intraSampleTolerance);
    setParameter(interSampleRTTolerance, interSampleTolerance);
    setParameter(minNumberOfDataPoints, minDataPoints);
    setParameter(SN_THRESHOLD, snThreshold);
    setParameter(RT_FOR_CWT_SCALES_DURATION, rtforCWT);
  }

}
