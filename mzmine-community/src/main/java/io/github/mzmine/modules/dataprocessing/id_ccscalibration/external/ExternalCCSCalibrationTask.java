/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.id_ccscalibration.external;

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.CCSCalculator;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.setcalibration.SetCCSCalibrationModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.Arrays;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExternalCCSCalibrationTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(
      ExternalCCSCalibrationTask.class.getName());

  private final CCSCalculator ccsCalculator;
  private final ParameterSet ccsCalculatorParameters;
  private final RawDataFile[] files;
  private double progress = 0;
  private int processed = 0;

  public ExternalCCSCalibrationTask(@Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, ParameterSet ccsCalculatorParameters) {
    super(storage, moduleCallDate);
    this.ccsCalculator = MZmineCore.getModuleInstance(ExternalCCSCalibrationModule.class);
    this.ccsCalculatorParameters = ccsCalculatorParameters;
    files = ccsCalculatorParameters.getParameter(ExternalCCSCalibrationParameters.files).getValue()
        .getMatchingRawDataFiles();
  }

  @Override
  public String getTaskDescription() {
    return "External CCS calibration for raw data files" + Arrays.toString(files);
  }

  @Override
  public double getFinishedPercentage() {
    return progress;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // set external calibration
    var calibration = ccsCalculator.getCalibration(null, ccsCalculatorParameters);
    if (calibration == null) {
      logger.info(
          "No calibration found using " + ccsCalculator + " " + ccsCalculatorParameters.toString());
      setErrorMessage("No calibration found.");
      setStatus(TaskStatus.CANCELED);
      return;
    }

    for (RawDataFile file : files) {
      if (file instanceof IMSRawDataFile imsFile) {
        imsFile.setCCSCalibration(calibration);
        processed++;
        progress = processed / (double) files.length;
      }
    }
    // for reproducibility, this might take a bit longer.
    SetCCSCalibrationModule.setCalibrationToFiles(files, calibration);

    setStatus(TaskStatus.FINISHED);
  }
}
