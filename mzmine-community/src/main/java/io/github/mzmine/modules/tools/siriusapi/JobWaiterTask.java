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

import de.unijena.bioinf.ms.nightsky.sdk.model.Job;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.taskcontrol.AbstractSimpleTask;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

public class JobWaiterTask extends AbstractSimpleTask {

  private final Job job;
  private final Runnable onFinished;

  public JobWaiterTask(@NotNull Class<? extends MZmineModule> callingModule,
      @NotNull Instant moduleCallDate, @NotNull ParameterSet parameters, Job job,
      Runnable onFinished) {
    super(null, moduleCallDate, parameters, callingModule);
    this.job = job;
    this.onFinished = onFinished;
  }

  @Override
  protected void process() {
    try {
      while (!hasFinished(job)) {
        TimeUnit.MILLISECONDS.sleep(100);
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    if (!wasSuccessful(job)) {
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
    return List.of(ParameterUtils.getMatchingRawDataFilesFromParameter(getParameters()));
  }

  @Override
  public String getTaskDescription() {
    return "";
  }

  boolean hasFinished(Job job) {
    return switch (job.getProgress().getState()) {
      case WAITING, READY, QUEUED, SUBMITTED, RUNNING -> false;
      case CANCELED, FAILED, DONE -> true;
    };
  }

  boolean wasSuccessful(Job job) {
    return switch (job.getProgress().getState()) {
      case WAITING, READY, QUEUED, SUBMITTED, RUNNING, CANCELED, FAILED -> false;
      case DONE -> true;
    };
  }
}
