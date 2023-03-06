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

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.GnpsValues.Polarity;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.MassSpectrometerWizardParameterFactory;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.ToleranceType;

public final class MassSpectrometerWizardParameters extends WizardStepParameters {

  public static final ComboParameter<Polarity> polarity = new ComboParameter<>("Ion mode",
      "Polarity of the ion mode", Polarity.values(), Polarity.Positive);

  public static final DoubleParameter ms1NoiseLevel = new DoubleParameter("MS1 noise level",
      "Noise level for peaks in MS1 spectra. Should remove noise but keep analyte signals.",
      MZmineCore.getConfiguration().getIntensityFormat());

  public static final DoubleParameter msnNoiseLevel = new DoubleParameter("MS2..MSn noise level",
      "Noise level for signals in MS2 to MSn spectra. Should remove noise but keep analyte signals.",
      MZmineCore.getConfiguration().getIntensityFormat());

  public static final DoubleParameter minimumFeatureHeight = new DoubleParameter(
      "Minimum feature height",
      "Intensity threshold at one retention time to be recognised as a feature.",
      MZmineCore.getConfiguration().getIntensityFormat());

  public static final MZToleranceParameter scanToScanMzTolerance = new MZToleranceParameter(
      ToleranceType.SCAN_TO_SCAN,
      "Describes the m/z fluctuations of peaks from one scan to another within the same"
          + " sample.\nUsed for chromatogram building.");

  public static final MZToleranceParameter featureToFeatureMzTolerance = new MZToleranceParameter(
      ToleranceType.INTRA_SAMPLE,
      "Describes the m/z variations of features that belong together, such as isotopic"
          + " signals. The values are averaged along the whole feature.\nUsed for recognition of"
          + "isotopic signals and ion identity networks.");

  public static final MZToleranceParameter sampleToSampleMzTolerance = new MZToleranceParameter(
      ToleranceType.SAMPLE_TO_SAMPLE,
      "Describes the m/z fluctuations between different samples. Used for alignment.");

  public MassSpectrometerWizardParameters(MassSpectrometerWizardParameterFactory preset) {
    super(WizardPart.MS, preset,
        // parameters
        polarity, ms1NoiseLevel, msnNoiseLevel, minimumFeatureHeight, scanToScanMzTolerance,
        featureToFeatureMzTolerance, sampleToSampleMzTolerance);
  }

  public MassSpectrometerWizardParameters(final MassSpectrometerWizardParameterFactory preset,
      final double ms1noise, final double ms2noise, final double minHeight,
      final MZTolerance scan2scanMzTolerance, final MZTolerance f2fMzTolerance,
      final MZTolerance sample2sampleMzTolerance) {
    this(preset);
    setParameter(ms1NoiseLevel, ms1noise);
    setParameter(msnNoiseLevel, ms2noise);
    setParameter(minimumFeatureHeight, minHeight);
    setParameter(scanToScanMzTolerance, scan2scanMzTolerance);
    setParameter(featureToFeatureMzTolerance, f2fMzTolerance);
    setParameter(sampleToSampleMzTolerance, sample2sampleMzTolerance);
  }

}
