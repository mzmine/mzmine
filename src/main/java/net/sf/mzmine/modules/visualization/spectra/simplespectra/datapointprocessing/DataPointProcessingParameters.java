package net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing;

import java.awt.Window;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingManager.MSLevel;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.HiddenParameter;
import net.sf.mzmine.parameters.parametertypes.ProcessingParameter;
import net.sf.mzmine.util.ExitCode;

public class DataPointProcessingParameters extends SimpleParameterSet {
  /**
   * Processing
   */
  public static final HiddenParameter<BooleanParameter, Boolean> spectraProcessing =
      new HiddenParameter<>(new BooleanParameter("Enable Processing", "", false));
  
  public static final HiddenParameter<ProcessingParameter, DataPointProcessingQueue> processingMSx = new HiddenParameter<>(new ProcessingParameter("Processing queue",
      "Set the modules to be executed in the processing queue.", MSLevel.MSANY));
  
  public static final HiddenParameter<ProcessingParameter, DataPointProcessingQueue> processingMS1 = new HiddenParameter<>(new ProcessingParameter("MS1 processing queue",
      "Set the modules to be executed in the processing queue.", MSLevel.MSONE));
  
  public static final HiddenParameter<ProcessingParameter, DataPointProcessingQueue> processingMSn = new HiddenParameter<>(new ProcessingParameter("MS^n processing queue",
      "Set the modules to be executed in the processing queue.", MSLevel.MSMS));
  
  public static final BooleanParameter differentiateMSn = new BooleanParameter("Different settings for MS^n", "If ticked, different processing queues will be used for MS1 and MSn scans.", false);
  
  @Override
  public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
    if ((getParameters() == null) || (getParameters().length == 0))
      return ExitCode.OK;

    ParameterSetupDialog dialog =
        new DataPointProcessingSetupDialog(parent, valueCheckRequired, this);
    dialog.setVisible(true);
    return dialog.getExitCode();
  }

  public DataPointProcessingParameters() {
    super(new Parameter[] {differentiateMSn, spectraProcessing, processingMSx, processingMS1, processingMSn});
  }
}
