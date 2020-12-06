package io.github.mzmine.modules.dataprocessing.featdet_mobilogramsmoothing;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.util.ExitCode;
import java.text.DecimalFormat;
import javafx.application.Platform;

public class MobilogramSmootherParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter rawDataFiles = new RawDataFilesParameter();

//  public static final DoubleParameter bandwidth = new DoubleParameter("Bandwidth", "",
//      new DecimalFormat("0.###"), 0.1, 1E-5, 1.0);

  public static final ComboParameter<Integer> filterWidth = new ComboParameter<>(
      "Filter width", "Number of data point covered by the smoothing filter",
      new Integer[] {5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25}, 5);

  public MobilogramSmootherParameters() {
    super(new Parameter[]{rawDataFiles, filterWidth});
  }


  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    assert Platform.isFxApplicationThread();

    if ((parameters == null) || (parameters.length == 0)) {
      return ExitCode.OK;
    }
    ParameterSetupDialog dialog = new MobilogramSmootherSetupDialog(valueCheckRequired, this);
    dialog.showAndWait();
    return dialog.getExitCode();
  }
}
