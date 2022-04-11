/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
