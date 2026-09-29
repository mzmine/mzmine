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

package io.github.mzmine.modules.tools.tools_autoparam;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.FeatureRecord;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.ParameterEstimators;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.RawDataAnalysis;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Runs {@link AutoParamTask} on all selected files in parallel and opens a
 * {@link DataFileStatisticsDashboardPane} with the collected results.
 */
public class AutoParamDashboardTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(AutoParamDashboardTask.class.getName());

  private final ParameterSet parameters;
  private final List<RawDataFile> files;

  public AutoParamDashboardTask(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      @NotNull ParameterSet parameters, @NotNull List<RawDataFile> files) {
    super(storage, moduleCallDate);
    this.parameters = parameters;
    this.files = files;
  }

  @Override
  public String getTaskDescription() {
    return "Computing data file statistics for %d file(s)".formatted(files.size());
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // decision: showTab=false suppresses individual per-file AutoParametersPane tabs
    final List<DataFileStatistics> stats = files.stream().map(
        file -> new AutoParamTask(getMemoryMapStorage(), Instant.now(),
            AutoParamParameters.of(files), AutoParamModule.class, file, (List<FeatureRecord>) null,
            false)).parallel().map(AutoParamTask::runAndGet).toList();

    logger.info("Computed statistics for %d files".formatted(stats.size()));

    if (DesktopService.isGUI()) {
      final InterSampleRtStatistics rtStatistics = ParameterEstimators.interSampleRtStatistics(
          RawDataAnalysis.analyze(stats));
      FxThread.runLater(() -> MZmineCore.getDesktop().addTab(
          new DataFileStatisticsDashboardTab(stats, rtStatistics)));
    }

    setStatus(TaskStatus.FINISHED);
  }
}
