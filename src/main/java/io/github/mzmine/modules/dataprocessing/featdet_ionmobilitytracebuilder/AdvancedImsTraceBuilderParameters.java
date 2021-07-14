package io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder;

import static io.github.mzmine.modules.dataprocessing.featdet_mobilogram_summing.MobilogramBinningParameters.DEFAULT_DTIMS_BIN_WIDTH;
import static io.github.mzmine.modules.dataprocessing.featdet_mobilogram_summing.MobilogramBinningParameters.DEFAULT_TIMS_BIN_WIDTH;
import static io.github.mzmine.modules.dataprocessing.featdet_mobilogram_summing.MobilogramBinningParameters.DEFAULT_TWIMS_BIN_WIDTH;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;

public class AdvancedImsTraceBuilderParameters extends SimpleParameterSet {

  public static final OptionalParameter<IntegerParameter> timsBinningWidth = new OptionalParameter<>(
      new IntegerParameter("Override default TIMS binning width",
          "The binning width in scans of the selected raw data file.\n"
              + " The default binning width is " + DEFAULT_TIMS_BIN_WIDTH + ".",
          DEFAULT_TIMS_BIN_WIDTH, 1, 1000));

  public static final OptionalParameter<IntegerParameter> twimsBinningWidth = new OptionalParameter(
      new IntegerParameter("Travelling wave binning width",
          "The binning width in scans of the selected raw data file."
              + "The default binning width is " + DEFAULT_TWIMS_BIN_WIDTH + ".",
          DEFAULT_TWIMS_BIN_WIDTH, 1, 1000));

  public static final OptionalParameter<IntegerParameter> dtimsBinningWidth = new OptionalParameter<>(
      new IntegerParameter("Drift tube binning width",
          "The binning width in scans of the selected raw data file.\n"
              + "The default binning width is " + DEFAULT_TIMS_BIN_WIDTH + ".",
          DEFAULT_DTIMS_BIN_WIDTH, 1, 1000));

  public AdvancedImsTraceBuilderParameters() {
    super(new Parameter[]{timsBinningWidth, dtimsBinningWidth, twimsBinningWidth});
  }
}
