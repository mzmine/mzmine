package io.github.mzmine.modules.dataprocessing.featdet_mobilogramsmoothing;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
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
      new Integer[]{5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25}, 15);

  public static final PercentParameter CHROMATOGRAPHIC_THRESHOLD_LEVEL = new PercentParameter(
      "Chromatographic threshold",
      "Threshold for removing noise. The algorithm finds such intensity that given percentage of the"
          + "\nchromatogram data points is below that intensity, and removes all data points below that level.",
      0.25);

  public static final DoubleParameter SEARCH_MOBILITY_RANGE = new DoubleParameter(
      "Search minimum in RT range (min)",
      "If a local minimum is minimal in this range of retention time, it will be considered a "
          + "border between two peaks", new DecimalFormat("0.0000"), 0.001);

  public static final PercentParameter MIN_RELATIVE_HEIGHT =
      new PercentParameter("Minimum relative height",
          "Minimum height of a peak relative to the chromatogram top data point", 0.2);

  public static final DoubleParameter MIN_ABSOLUTE_HEIGHT = new DoubleParameter(
      "Minimum absolute height", "Minimum absolute height of a peak to be recognized",
      MZmineCore.getConfiguration().getIntensityFormat(), 400.d);

  public static final DoubleParameter MIN_RATIO = new DoubleParameter("Min ratio of peak top/edge",
      "Minimum ratio between peak's top intensity and side (lowest) data points."
          + "\nThis parameter helps to reduce detection of false peaks in case the chromatogram is not smooth.",
      new DecimalFormat("0.00"), 2.0);

  public static final DoubleRangeParameter PEAK_DURATION =
      new DoubleRangeParameter("Peak duration range (mobility)", "Range of acceptable peak lengths",
          new DecimalFormat("0.00000000"));

  public MobilogramSmootherParameters() {
    super(new Parameter[]{rawDataFiles, filterWidth, CHROMATOGRAPHIC_THRESHOLD_LEVEL,
        SEARCH_MOBILITY_RANGE, MIN_RELATIVE_HEIGHT, MIN_ABSOLUTE_HEIGHT, MIN_RATIO, PEAK_DURATION});

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
