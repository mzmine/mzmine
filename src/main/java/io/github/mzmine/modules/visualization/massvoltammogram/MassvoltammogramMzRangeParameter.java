package io.github.mzmine.modules.visualization.massvoltammogram;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeParameter;

public class MassvoltammogramMzRangeParameter extends SimpleParameterSet {

  public static final MZRangeParameter mzRange = new MZRangeParameter("m/z Range", "Minimal and maximal m/z");

  public MassvoltammogramMzRangeParameter(){
    super(new Parameter[]{mzRange});
  }

}
