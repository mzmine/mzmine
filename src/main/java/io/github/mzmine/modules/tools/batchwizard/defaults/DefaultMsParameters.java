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

package io.github.mzmine.modules.tools.batchwizard.defaults;

import io.github.mzmine.modules.tools.batchwizard.BatchWizardMassSpectrometerParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;

public class DefaultMsParameters {

  public static final DefaultMsParameters defaultTofParameters = new DefaultMsParameters(5E2, 1E2,
      1E3, new MZTolerance(0.005, 20), new MZTolerance(0.0015, 3), new MZTolerance(0.004, 8));

  public static final DefaultMsParameters defaultImsTofParameters = new DefaultMsParameters(150d,
      1E2, 1E3, new MZTolerance(0.005, 15), new MZTolerance(0.0015, 3), new MZTolerance(0.004, 8));

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
