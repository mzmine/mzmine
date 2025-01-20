/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.tools.siriusapi;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.taskcontrol.AbstractSimpleTask;
import io.sirius.ms.sdk.model.Job;
import io.sirius.ms.sdk.model.JobProgress.StateEnum;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class JobWaiterTask extends AbstractSimpleTask {

  private static final Logger logger = Logger.getLogger(JobWaiterTask.class.getName());

  private final Supplier<Job> jobSupplier;
  private final Runnable onFinished;

  public JobWaiterTask(@NotNull Class<? extends MZmineModule> callingModule,
      @NotNull Instant moduleCallDate, @NotNull ParameterSet parameters, Supplier<Job> jobSupplier,
      Runnable onFinished) {
    super(null, moduleCallDate, parameters, callingModule);
    this.jobSupplier = jobSupplier;
    this.onFinished = onFinished;
  }

  @Override
  protected void process() {
    try {
      while (!hasFinished(jobSupplier.get())) {
        TimeUnit.MILLISECONDS.sleep(100);
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    if (!wasSuccessful(jobSupplier.get())) {
      logger.warning(() -> "Sirius job was not successful.");
      return;
    }

    onFinished.run();
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(ParameterUtils.getMatchingFeatureListsFromParameter(getParameters()));
  }

  @Override
  protected @NotNull List<RawDataFile> getProcessedDataFiles() {
    return List.of();
  }

  @Override
  public String getTaskDescription() {
    return "";
  }

  boolean hasFinished(Job job) {
    final StateEnum state = job.getProgress().getState();
    return switch (state) {
      case WAITING, READY, QUEUED, SUBMITTED, RUNNING -> false;
      case CANCELED, FAILED, DONE -> true;
    };
  }

  boolean wasSuccessful(Job job) {
    final StateEnum state = job.getProgress().getState();
    return switch (state) {
      case WAITING, READY, QUEUED, SUBMITTED, RUNNING, CANCELED, FAILED -> false;
      case DONE -> true;
    };
  }
}
