package io.github.mzmine.modules.dataprocessing.id_ccscalibration.external;

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.CCSCalculator;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.reference.ReferenceCCSCalibrationTask;
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
      ReferenceCCSCalibrationTask.class.getName());

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
