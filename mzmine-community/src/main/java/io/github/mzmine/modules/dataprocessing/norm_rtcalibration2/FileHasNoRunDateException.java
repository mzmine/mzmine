package io.github.mzmine.modules.dataprocessing.norm_rtcalibration2;

import io.github.mzmine.datamodel.RawDataFile;

public class FileHasNoRunDateException extends RuntimeException {

  public FileHasNoRunDateException(RawDataFile file) {
    super(
        "Run date is not available for sample %s. Cannot run RT calibration on a subset and extrapolate. Consider running RT calibration on all files.".formatted(
            file.getName()));
  }
}
