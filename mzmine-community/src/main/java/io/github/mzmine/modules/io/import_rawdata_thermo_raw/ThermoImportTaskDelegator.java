package io.github.mzmine.modules.io.import_rawdata_thermo_raw;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.gui.preferences.ThermoImportOptions;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_msconvert.MSConvertImportTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractSimpleTask;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.TaskStatusListener;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.time.Instant;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handles if thermo files are imported via msconvert or the raw file parser. launches the correct task.
 */
public class ThermoImportTaskDelegator extends AbstractSimpleTask {

  private AbstractTask actualTask;

  public ThermoImportTaskDelegator(@Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, File file,
      ScanImportProcessorConfig processorConfig, MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Class<? extends MZmineModule> moduleClass) {

    super(storage, moduleCallDate, parameters, moduleClass);

    final ThermoImportOptions importChoice = ConfigService.getPreference(
        MZminePreferences.thermoImportChoice);
    actualTask =
        importChoice == ThermoImportOptions.MSCONVERT ? new MSConvertImportTask(moduleCallDate,
            file, processorConfig, project, moduleClass, parameters)
            : new ThermoRawImportTask(project, file, moduleClass, parameters,
                moduleCallDate, processorConfig);

    // cancel the underlying task if this task is canceled
    this.addTaskStatusListener((_, newStatus, _) -> actualTask.setStatus(newStatus));
  }

  @Override
  protected void process() {
    actualTask.run();
    if(actualTask.getStatus() != TaskStatus.FINISHED) {
      if(actualTask.getErrorMessage() != null) {
        setErrorMessage(actualTask.getErrorMessage());
      }
      setStatus(actualTask.getStatus());
    }
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    // applied method added by actual task
    return List.of();
  }

  @Override
  protected @NotNull List<RawDataFile> getProcessedDataFiles() {
    // applied method added by actual task
    return List.of();
  }

  @Override
  public String getTaskDescription() {
    return actualTask.getTaskDescription();
  }

  @Override
  public double getFinishedPercentage() {
    return actualTask.getFinishedPercentage();
  }
}
