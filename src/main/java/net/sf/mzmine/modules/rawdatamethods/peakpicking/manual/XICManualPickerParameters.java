package net.sf.mzmine.modules.rawdatamethods.peakpicking.manual;

import java.awt.Window;
import com.google.common.collect.Range;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.tic.TICPlot;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialogWithChromatogramPreview;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.HiddenParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import net.sf.mzmine.util.ExitCode;

public class XICManualPickerParameters extends SimpleParameterSet {

  public static final HiddenParameter<RawDataFilesParameter, RawDataFilesSelection> rawDataFiles =
      new HiddenParameter<RawDataFilesParameter, RawDataFilesSelection>(
          new RawDataFilesParameter("Raw data file", 1, 100));

  public static final HiddenParameter<DoubleRangeParameter, Range<Double>> rtRange =
      new HiddenParameter<DoubleRangeParameter, Range<Double>>(new DoubleRangeParameter(
          "Retention time", "Retention time range", MZmineCore.getConfiguration().getRTFormat()));

  public static final HiddenParameter<DoubleRangeParameter, Range<Double>> mzRange =
      new HiddenParameter<DoubleRangeParameter, Range<Double>>(new DoubleRangeParameter("m/z range",
          "m/z range", MZmineCore.getConfiguration().getMZFormat()));

  public XICManualPickerParameters() {
    super(new Parameter[] {rawDataFiles, rtRange, mzRange});
  }

  @Override
  public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {

    ParameterSetupDialog dialog =
        new XICManualPickerDialog(MZmineCore.getDesktop().getMainWindow(), true, this);
    dialog.setVisible(true);

    return dialog.getExitCode();
  }
}
