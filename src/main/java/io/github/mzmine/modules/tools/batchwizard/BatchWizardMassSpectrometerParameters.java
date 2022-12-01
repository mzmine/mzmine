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

package io.github.mzmine.modules.tools.batchwizard;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class BatchWizardMassSpectrometerParameters extends SimpleParameterSet {

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

  public BatchWizardMassSpectrometerParameters() {
    super(new Parameter[]{ms1NoiseLevel, ms2NoiseLevel, minimumFeatureHeight, scanToScanMzTolerance,
        featureToFeatureMzTolerance, sampleToSampleMzTolerance});
  }
}
