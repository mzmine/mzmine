package io.github.mzmine.modules.dataprocessing.filter_framestofile;

import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;

public class FrameToFileParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter raw = new RawDataFilesParameter(1);
  public static final IntegerParameter minFrame = new IntegerParameter("Inclusive Minimum frame", "");
  public static final IntegerParameter maxFrame = new IntegerParameter("Inclusive Maximum frame",
      "<= 0 if all frames from minFrame to the end shall be exported");

  public FrameToFileParameters() {
    super(new UserParameter[]{raw, minFrame, maxFrame});
  }
}