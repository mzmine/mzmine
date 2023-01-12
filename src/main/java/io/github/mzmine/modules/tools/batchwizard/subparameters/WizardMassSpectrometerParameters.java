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
import io.github.mzmine.modules.tools.batchwizard.WizardPreset.MsInstrumentDefaults;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.HiddenParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class WizardMassSpectrometerParameters extends SimpleParameterSet {

  public static final ComboParameter<Polarity> polarity = new ComboParameter<>("Ion mode",
      "Polarity of the ion mode", Polarity.values(), Polarity.Positive);

  public static final DoubleParameter ms1NoiseLevel = new DoubleParameter("MS1 noise level",
      "Noise level for peaks in MS1 spectra. Should remove noise but keep analyte signals.",
      MZmineCore.getConfiguration().getIntensityFormat());

  public static final DoubleParameter ms2NoiseLevel = new DoubleParameter("MS2 noise level",
      "Noise level for peaks in MS2 spectra. Should remove noise but keep analyte signals.",
      MZmineCore.getConfiguration().getIntensityFormat());

  public static final DoubleParameter minimumFeatureHeight = new DoubleParameter(
      "Minimum feature height",
      "Intensity threshold at one retention time to be recognised as a feature.",
      MZmineCore.getConfiguration().getIntensityFormat());

  public static final MZToleranceParameter scanToScanMzTolerance = new MZToleranceParameter(
      "Scan to scan m/z tolerance",
      "Describes the m/z fluctuations of peaks from one scan to another within the same"
          + " sample.\nUsed for chromatogram building.");

  public static final MZToleranceParameter featureToFeatureMzTolerance = new MZToleranceParameter(
      "Feature to feature m/z tolerance",
      "Describes the m/z variations of features that belong together, such as isotopic"
          + " signals. The values are averaged along the whole feature.\nUsed for recognition of"
          + "isotopic signals and ion identity networks.");

  public static final MZToleranceParameter sampleToSampleMzTolerance = new MZToleranceParameter(
      "Sample to sample m/z tolerace",
      "Describes the m/z fluctuations between different samples. Used for alignment.");
  /**
   * the UI element shown on top to signal the workflow used. Presets May be changed and then saved
   * to user presets as parameter files.
   */
  public static final HiddenParameter<MsInstrumentDefaults> wizardPart = new HiddenParameter<>(
      new ComboParameter<>("Wizard part", "Defines the wizard part used",
          MsInstrumentDefaults.values(), MsInstrumentDefaults.Orbitrap));

  /**
   * the part category of presets - is used in all parameter classes
   */
  public static final WizardPartParameter wizardPartCategory = new WizardPartParameter(
      WizardPart.MS);

  public WizardMassSpectrometerParameters() {
    super(new Parameter[]{
        // hidden
        wizardPart, wizardPartCategory,
        // shown
        polarity, ms1NoiseLevel, ms2NoiseLevel, minimumFeatureHeight, scanToScanMzTolerance,
        featureToFeatureMzTolerance, sampleToSampleMzTolerance});
  }

  public WizardMassSpectrometerParameters(final double ms1noise, final double ms2noise,
      final double minHeight, final MZTolerance scan2scanMzTolerance,
      final MZTolerance f2fMzTolerance, final MZTolerance sample2sampleMzTolerance) {
    this();
    setParameter(ms1NoiseLevel, ms1noise);
    setParameter(ms2NoiseLevel, ms2noise);
    setParameter(minimumFeatureHeight, minHeight);
    setParameter(scanToScanMzTolerance, scan2scanMzTolerance);
    setParameter(featureToFeatureMzTolerance, f2fMzTolerance);
    setParameter(sampleToSampleMzTolerance, sample2sampleMzTolerance);
  }

  public static WizardMassSpectrometerParameters create(final MsInstrumentDefaults defaults) {
    WizardMassSpectrometerParameters params = switch (defaults) {
      case Orbitrap ->
          new WizardMassSpectrometerParameters(1E4, 3E3, 5E4, new MZTolerance(0.002, 10),
              new MZTolerance(0.0015, 3), new MZTolerance(0.0015, 5));
      case qTOF -> new WizardMassSpectrometerParameters(5E2, 1E2, 1E3, new MZTolerance(0.005, 20),
          new MZTolerance(0.0015, 3), new MZTolerance(0.004, 8));
      // TODO optimize some defaults
      case FTICR -> new WizardMassSpectrometerParameters(5E2, 1E2, 1E3, new MZTolerance(0.0005, 5),
          new MZTolerance(0.0005, 2), new MZTolerance(0.0005, 3.5));
    };
    params.setParameter(wizardPart, defaults);
    params.setParameter(wizardPartCategory, WizardPart.MS);
    return params;
  }
}
