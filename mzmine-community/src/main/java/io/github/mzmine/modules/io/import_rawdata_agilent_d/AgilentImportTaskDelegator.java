/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_agilent_d;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.RawDataImportTask;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.preferences.AgilentImportOptions;
import io.github.mzmine.gui.preferences.MZminePreferences;
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
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Decides whether an Agilent {@code .d} file is imported via the native AgilentBridge or MSConvert,
 * based on {@link MZminePreferences#agilentImportChoice}, and launches the matching task. Modeled
 * on {@code MassLynxImportTaskDelegator}.
 */
public class AgilentImportTaskDelegator extends AbstractSimpleTask implements RawDataImportTask {

  private static final Logger logger = Logger.getLogger(AgilentImportTaskDelegator.class.getName());

  private final RawDataImportTask actualTask;

  public AgilentImportTaskDelegator(@Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, File file,
      @NotNull ScanImportProcessorConfig processorConfig, MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Class<? extends MZmineModule> moduleClass) {

    super(storage, moduleCallDate, parameters, moduleClass);

    final AgilentImportOptions importChoice = ConfigService.getPreference(
        MZminePreferences.agilentImportChoice).getSelectedOption();
    actualTask = switch (importChoice) {
      case MSCONVERT ->
          new MSConvertImportTask(storage, moduleCallDate, file, processorConfig, project,
              moduleClass, parameters);
      case CUSTOM -> {
        final AgilentReaderTaskFactory factory = AgilentReaderTaskFactory.registered();
        if (factory != null) {
          yield factory.create(storage, moduleCallDate, file, moduleClass, parameters, project,
              processorConfig);
        }
        logger.warning(
            "The mzio Agilent reader is not available in this mzmine edition, using the native AgilentReader for "
                + file.getName());
        yield new AgilentImportTask(storage, moduleCallDate, file, moduleClass, parameters, project,
            processorConfig);
      }
      case AGILENT_READER, AGILENT_READER_AUTO_CENTROID ->
          new AgilentImportTask(storage, moduleCallDate, file, moduleClass, parameters, project,
              processorConfig);
    };

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
    return List.of();
  }

  @Override
  protected @NotNull List<RawDataFile> getProcessedDataFiles() {
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
  public @NotNull List<RawDataFile> getImportedRawDataFiles() {
    return actualTask.getImportedRawDataFiles();
  }
}
