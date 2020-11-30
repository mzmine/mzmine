package io.github.mzmine.modules.visualization.mobilogram;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;

public class MobilogramVisualizerParameters extends SimpleParameterSet {
  public static final RawDataFilesParameter rawFiles = new RawDataFilesParameter();

  public MobilogramVisualizerParameters() {
    super(new Parameter[] {rawFiles});
  }

}
