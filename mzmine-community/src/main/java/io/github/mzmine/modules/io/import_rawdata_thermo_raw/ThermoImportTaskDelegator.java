/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_thermo_raw;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.RawDataImportTask;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.gui.preferences.ThermoImportOptions;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_msconvert.MSConvertImportTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractSimpleTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.time.Instant;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handles if thermo files are imported via msconvert or the raw file parser. launches the correct
 * task.
 */
public class ThermoImportTaskDelegator extends AbstractSimpleTask implements RawDataImportTask {

  private RawDataImportTask actualTask;

  public ThermoImportTaskDelegator(@Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, File file, ScanImportProcessorConfig processorConfig,
      MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Class<? extends MZmineModule> moduleClass) {

    super(storage, moduleCallDate, parameters, moduleClass);

    final ThermoImportOptions importChoice = ConfigService.getPreference(
        MZminePreferences.thermoImportChoice);
    actualTask = importChoice == ThermoImportOptions.MSCONVERT ? new MSConvertImportTask(storage,
        moduleCallDate, file, processorConfig, project, moduleClass, parameters)
        : new ThermoRawImportTask(storage, project, file, moduleClass, parameters, moduleCallDate,
            processorConfig);

    // cancel the underlying task if this task is canceled
    this.addTaskStatusListener((_, newStatus, _) -> actualTask.setStatus(newStatus));
  }

  @Override
  protected void process() {
    actualTask.run();
    if (actualTask.getStatus() != TaskStatus.FINISHED) {
      if (actualTask.getErrorMessage() != null) {
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

  @Override
  public RawDataFile getImportedRawDataFile() {
    return actualTask.getImportedRawDataFile();
  }
}
