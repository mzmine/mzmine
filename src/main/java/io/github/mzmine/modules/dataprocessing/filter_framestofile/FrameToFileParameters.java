package io.github.mzmine.modules.dataprocessing.filter_framestofile;

import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;

public class FrameToFileParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter raw = new RawDataFilesParameter(1);

  public FrameToFileParameters() {
    super(new UserParameter[]{raw});
  }
}