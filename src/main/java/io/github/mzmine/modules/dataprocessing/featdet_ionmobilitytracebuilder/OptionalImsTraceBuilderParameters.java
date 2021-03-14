package io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;

public class OptionalImsTraceBuilderParameters extends SimpleParameterSet {

  public static OptionalParameter<IntegerParameter> allowedMissingMobilityScans = new OptionalParameter<>(
      new IntegerParameter("Allowed missing mobility scans",
          "Maximum number of consecutively empty mobility scans to be interpolated between.\n"
              + "If this gap is exceeded, a 0 intensity will be added to the start and the end of that gap.\n"
              + "The default value is " + IonMobilityTraceBuilderTask.DEFAULT_ALLOWED_MISSING_MOBILITY_SCANS
              + ".", IonMobilityTraceBuilderTask.DEFAULT_ALLOWED_MISSING_MOBILITY_SCANS));

  public static OptionalParameter<IntegerParameter> allowedMissingFrames = new OptionalParameter<>(
      new IntegerParameter("Allowed missing frames",
          "Maximum number of consecutively empty frames scans to be interpolated between.\n"
              + "If this gap is exceeded, a 0 intensity will be added to the start and the end of that gap.\n"
              + "Interpolation does not count towards the number of consecutive frames. However, added "
              + "trailing and leading 0s do.\nThe default value is "
              + IonMobilityTraceBuilderTask.DEFAULT_ALLOWED_MISSING_FRAMES + ".",
          IonMobilityTraceBuilderTask.DEFAULT_ALLOWED_MISSING_FRAMES));

  public OptionalImsTraceBuilderParameters() {
    super(new Parameter[]{allowedMissingMobilityScans, allowedMissingFrames});
  }
}
