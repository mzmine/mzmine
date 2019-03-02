package net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;

public class DataPointProcessingParameters extends SimpleParameterSet {
  /**
   * Processing
   */
  public static final BooleanParameter spectraProcessing = new BooleanParameter(
      "Enable spectra processing", "Enable spectra processing for Spectra plots.", false);
  public static final FileNameParameter defaultDPPQueue = new FileNameParameter(
      "Default spectra processing queue", "File that saves the default spectra processing queue");
  
  public DataPointProcessingParameters() {
    super(new Parameter[] {spectraProcessing, defaultDPPQueue});
  }
}
