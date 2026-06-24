package io.github.mzmine.modules.dataprocessing.norm_rtcalibration2;

import io.github.mzmine.datamodel.RawDataFile;
import org.jetbrains.annotations.NotNull;

public class InvalidRtCalibrationParametersException extends RuntimeException {

  public InvalidRtCalibrationParametersException(double bandwidth, @NotNull RawDataFile file,
      int size) {
    super(
        "Cannot create a valid RT calibration for file %s with bandwidth %.3f. Found %d standards. Try reducing the bandwidth and/or adding or removing files to calibrate on.".formatted(
            file.getName(), bandwidth, size));
  }
}
