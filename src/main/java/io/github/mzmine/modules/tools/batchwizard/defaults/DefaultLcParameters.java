package io.github.mzmine.modules.tools.batchwizard.defaults;

import io.github.mzmine.modules.tools.batchwizard.BatchWizardHPLCParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;

public class DefaultLcParameters {

  public static final DefaultLcParameters uhplc = new DefaultLcParameters(
      new RTTolerance(0.05f, Unit.MINUTES), new RTTolerance(0.05f, Unit.MINUTES),
      new RTTolerance(0.1f, Unit.MINUTES));

  public static final DefaultLcParameters hplc = new DefaultLcParameters(
      new RTTolerance(0.1f, Unit.MINUTES), new RTTolerance(0.05f, Unit.MINUTES),
      new RTTolerance(0.1f, Unit.MINUTES));

  private final RTTolerance fwhm;
  private final RTTolerance intraSampleTolerance;
  private final RTTolerance interSampleTolerance;

  public DefaultLcParameters(RTTolerance fwhm, RTTolerance intraSampleTolerance,
      RTTolerance interSampleTolerance) {
    this.fwhm = fwhm;
    this.intraSampleTolerance = intraSampleTolerance;
    this.interSampleTolerance = interSampleTolerance;
  }

  public void setToParameterSet(ParameterSet parameterSet) {
    parameterSet.getParameter(BatchWizardHPLCParameters.approximateChromatographicFWHM)
        .setValue(fwhm);
    parameterSet.getParameter(BatchWizardHPLCParameters.intraSampleRTTolerance)
        .setValue(intraSampleTolerance);
    parameterSet.getParameter(BatchWizardHPLCParameters.interSampleRTTolerance)
        .setValue(interSampleTolerance);
  }
}
