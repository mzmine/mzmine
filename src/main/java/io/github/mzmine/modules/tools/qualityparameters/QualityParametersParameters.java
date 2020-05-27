package io.github.mzmine.modules.tools.qualityparameters;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class QualityParametersParameters extends SimpleParameterSet {

  public static final PeakListsParameter peakLists = new PeakListsParameter(1);

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter("S/N tolerance",
      "Sets the tolerance range for S/N calculations. For high resolving instruments 0 "
          + "is recommended. However for instruments with less resolution a higher tolerance can be"
          + " useful. Due to peaks overlapping sometimes because of lower resolution and accuracy "
          + "the S/N could be accidentally lowered.", 0, 0);

  public QualityParametersParameters() {
    super(new Parameter[]{peakLists, mzTolerance});
  }
}
