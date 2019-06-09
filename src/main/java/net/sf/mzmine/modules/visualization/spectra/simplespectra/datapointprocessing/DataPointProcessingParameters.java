package net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.HiddenParameter;
import net.sf.mzmine.parameters.parametertypes.ProcessingParameter;

public class DataPointProcessingParameters extends SimpleParameterSet {
  /**
   * Processing
   */
  public static final HiddenParameter<BooleanParameter, Boolean> spectraProcessing =
      new HiddenParameter<>(new BooleanParameter("Enable Processing", "", false));
  
  public static final ProcessingParameter processing = new ProcessingParameter("Processing queue",
      "Set the modules to be executed in the processing queue.");

  public DataPointProcessingParameters() {
    super(new Parameter[] {processing, spectraProcessing});
  }
}
