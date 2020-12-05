package io.github.mzmine.modules.dataprocessing.featdet_mobilogramsmoothing;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import java.text.DecimalFormat;

public class MobilogramSmootherParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter rawDataFiles = new RawDataFilesParameter();

  private static final DoubleParameter bandwidth = new DoubleParameter("Bandwidth", "",
      new DecimalFormat("0.###"), 0.1, 1E-5, 1.0);

  public MobilogramSmootherParameters() {
    super(new Parameter[]{rawDataFiles, bandwidth});
  }

}
