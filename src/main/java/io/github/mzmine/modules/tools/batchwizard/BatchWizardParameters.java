package io.github.mzmine.modules.tools.batchwizard;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ParameterSetParameter;

public class BatchWizardParameters extends SimpleParameterSet {

  public static final ParameterSetParameter msParams = new ParameterSetParameter("MS parameters", "",
      new BatchWizardMassSpectrometerParameters());

  public static final ParameterSetParameter hplcParams = new ParameterSetParameter("(U)HPLC parameters", "",
      new BatchWizardMassSpectrometerParameters());

  public BatchWizardParameters() {
    super(new Parameter[] {msParams, hplcParams});
  }
}
