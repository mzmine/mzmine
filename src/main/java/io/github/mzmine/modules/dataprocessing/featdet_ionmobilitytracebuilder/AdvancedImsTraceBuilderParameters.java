package io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public class AdvancedImsTraceBuilderParameters extends SimpleParameterSet {

  public static final double DEFAULT_TIMS_BIN_WIDTH = 0.0008;
  public static final double DEFAULT_DTIMS_BIN_WIDTH = 0.005;
  public static final double DEFAULT_TWIMS_BIN_WIDTH = 0.005;
  private static final NumberFormat binFormat = new DecimalFormat("0.00000");

  public static final OptionalParameter<DoubleParameter> timsBinningWidth = new OptionalParameter<>(
      new DoubleParameter("Override default TIMS binning width (Vs/cm²)",
          "The binning width in mobility units of the selected raw data file.\n"
              + " The default binning width is " + binFormat.format(DEFAULT_TIMS_BIN_WIDTH) + ".",
          binFormat, DEFAULT_TIMS_BIN_WIDTH, 0.00001, 1E6));

  public static final OptionalParameter<DoubleParameter> twimsBinningWidth = new OptionalParameter(
      new DoubleParameter(
          "Travelling wave binning width (ms)",
          "The binning width in mobility units of the selected raw data file."
              + "The default binning width is " + binFormat.format(DEFAULT_TWIMS_BIN_WIDTH) + ".",
          binFormat, DEFAULT_TWIMS_BIN_WIDTH, 0.00001, 1E6));

  public static final OptionalParameter<DoubleParameter> dtimsBinningWidth = new OptionalParameter<>(
      new DoubleParameter(
          "Drift tube binning width (ms)",
          "The binning width in mobility units of the selected raw data file.\n"
              + "The default binning width is " + binFormat.format(DEFAULT_TIMS_BIN_WIDTH) + ".",
          binFormat, DEFAULT_DTIMS_BIN_WIDTH, 0.00001, 1E6));

  public AdvancedImsTraceBuilderParameters() {
    super(new Parameter[]{timsBinningWidth, dtimsBinningWidth, twimsBinningWidth});
  }
}
