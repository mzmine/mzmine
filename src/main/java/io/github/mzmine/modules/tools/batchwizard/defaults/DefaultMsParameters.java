package io.github.mzmine.modules.tools.batchwizard.defaults;

import io.github.mzmine.modules.tools.batchwizard.BatchWizardMassSpectrometerParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;

public class DefaultMsParameters {

  public static final DefaultMsParameters defaultTofParameters = new DefaultMsParameters(5E2, 1E2,
      1E3, new MZTolerance(0.005, 10), new MZTolerance(0.005, 3), new MZTolerance(0.005, 8));

  public static final DefaultMsParameters defaultImsTofParameters = new DefaultMsParameters(150d,
      1E2, 1E3, new MZTolerance(0.005, 10), new MZTolerance(0.005, 3), new MZTolerance(0.005, 8));

  public static final DefaultMsParameters defaultOrbitrapPositiveParameters = new DefaultMsParameters(
      1E4, 5E3, 5E4, new MZTolerance(0.005, 5), new MZTolerance(0.005, 3),
      new MZTolerance(0.005, 5));

  public static final DefaultMsParameters defaultOrbitrapNegativeParameters = new DefaultMsParameters(
      1E4, 5E3, 5E4, new MZTolerance(0.005, 5), new MZTolerance(0.005, 3),
      new MZTolerance(0.005, 5));

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
    params
        .setParameter(BatchWizardMassSpectrometerParameters.minimumFeatureHeight, minFeatureHeight);
    params.setParameter(BatchWizardMassSpectrometerParameters.scanToScanMzTolerance,
        scanToScanMzTolerance);
    params.setParameter(BatchWizardMassSpectrometerParameters.featureToFeatureMzTolerance,
        featureToFeatureMzTolerance);
    params.setParameter(BatchWizardMassSpectrometerParameters.sampleToSampleMzTolerance,
        sampleToSampleMzTolerance);
  }
}
