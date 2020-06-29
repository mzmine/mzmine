package io.github.mzmine.modules.dataprocessing.id_cliquems;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.PeakListsParameter;

public class CliqueMSParameters extends SimpleParameterSet {
  public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

  public CliqueMSParameters(){
    super(new Parameter[]{PEAK_LISTS});
  }

}
