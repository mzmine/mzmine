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
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

public final class IonInterfaceHplcWizardParameters extends IonInterfaceWizardParameters {

  public static final RTToleranceParameter approximateChromatographicFWHM = new RTToleranceParameter(
      "Approximate feature FWHM",
      "The approximate feature width (chromatograpic peak width) in retention time (full-width-at-half-maximum, FWHM). ");

  public static final RTToleranceParameter intraSampleRTTolerance = new RTToleranceParameter(
      "RT tolerance (intra-sample)",
      "Retention time tolerance for multiple signals of the same compound in the same "
          + "sample.\nUsed to detect isotopes or multimers/adducts of the same compound.");

  public static final RTToleranceParameter interSampleRTTolerance = new RTToleranceParameter(
      "RT tolerance (sample-to-sample)",
      "Retention time tolerance for the same compound in different samples.\n"
          + "Used to align multiple measurements of the same sample or a batch run.");

  public static final IntegerParameter minNumberOfDataPoints = new IntegerParameter(
      "Minimum consecutive scans",
      "Minimum number of consecutive scans with detected data points as used in chromatogram building and feature resolving.", 4, 1,
      Integer.MAX_VALUE);

  public static final RTRangeParameter cropRtRange = new RTRangeParameter("Crop retention time",
      "Crops the RT range of chromatograms. Used to exclude time before the flow time\n"
          + "and after the separation, where in many runs cleaning and re-equilibration starts.",
      true, Range.closed(0.5, 30d));

  public static final IntegerParameter maximumIsomersInChromatogram = new IntegerParameter(
      "Max peaks in chromatogram",
      "An estimate maximum number of peaks in a chromatogram (number of same m/z features).\n"
          + "Used to estimate the chromatographic threshold to filter noisy chromatograms.", 10, 1,
      Integer.MAX_VALUE);

  public static final BooleanParameter stableIonizationAcrossSamples = new BooleanParameter(
      "Stable ionization across samples", """
      Only check if the ionization conditions are stable across all samples.
      Uncheck for varying salt content or variations in ionization conditions.
      Used in feature grouping.""", true);

  public static final BooleanParameter smoothing = new BooleanParameter("Smoothing",
      "Apply smoothing in the retention time dimension, usually only needed if the peak shapes are spiky.",
      true);


  public IonInterfaceHplcWizardParameters(final IonInterfaceWizardParameterFactory preset) {
    super(WizardPart.ION_INTERFACE, preset,
        // actual parameters
        smoothing, stableIonizationAcrossSamples, cropRtRange, maximumIsomersInChromatogram,
        minNumberOfDataPoints, approximateChromatographicFWHM, intraSampleRTTolerance,
        interSampleRTTolerance);
  }

  public IonInterfaceHplcWizardParameters(final IonInterfaceWizardParameterFactory preset,
      final boolean stableIonization, final int maxIsomersInSample, final int minDataPoints,
      final Range<Double> cropRt, final RTTolerance fwhm, final RTTolerance intraSampleTolerance,
      final RTTolerance interSampleTolerance) {
    this(preset);
    // defaults
    setParameter(stableIonizationAcrossSamples, stableIonization);
    setParameter(maximumIsomersInChromatogram, maxIsomersInSample);
    setParameter(minNumberOfDataPoints, minDataPoints);
    setParameter(cropRtRange, cropRt);
    setParameter(approximateChromatographicFWHM, fwhm);
    setParameter(intraSampleRTTolerance, intraSampleTolerance);
    setParameter(interSampleRTTolerance, interSampleTolerance);
  }

}
