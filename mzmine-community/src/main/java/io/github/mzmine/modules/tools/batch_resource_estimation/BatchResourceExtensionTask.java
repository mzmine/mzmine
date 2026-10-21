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

package io.github.mzmine.modules.tools.batch_resource_estimation;

import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractSimpleToolTask;
import java.io.IOException;
import java.time.Instant;
import java.util.function.BiConsumer;
import org.jetbrains.annotations.NotNull;

/**
 * Adds one larger run to a finished resource estimation, e.g. because a step did not level off up
 * to the largest run, and fits the model again with all runs.
 */
class BatchResourceExtensionTask extends AbstractSimpleToolTask {

  private final @NotNull EstimationSession session;
  private final int files;
  private final @NotNull BiConsumer<EstimationSession, BatchResourceModel> onFinished;

  /**
   * @param parameters the parameters of the estimation
   * @param files      files of the additional run
   * @param onFinished receives the session with the additional run and the new model, called on
   *                   the task thread
   */
  BatchResourceExtensionTask(@NotNull final Instant moduleCallDate,
      @NotNull final ParameterSet parameters, @NotNull final EstimationSession session,
      final int files,
      @NotNull final BiConsumer<EstimationSession, BatchResourceModel> onFinished) {
    super(moduleCallDate, parameters);
    this.session = session;
    this.files = files;
    this.onFinished = onFinished;
    totalItems = 1;
  }

  @Override
  protected void process() {
    final SubsetRun run = session.runner().run(files);
    if (run == null || isCanceled()) {
      return;
    }
    incrementFinishedItems();
    final EstimationSession extended = session.withExtension(run);
    final BatchResourceModel model = extended.fitModel();
    BatchResourceReport.logPrediction(model);
    try {
      BatchResourceReport.exportStatistics(parameters, model);
    } catch (IOException e) {
      error("Cannot export resource estimation statistics: " + e.getMessage(), e);
    }
    onFinished.accept(extended, model);
  }

  @Override
  public void cancel() {
    super.cancel();
    // stops after the current batch step
    session.runner().cancel();
  }

  @Override
  public String getTaskDescription() {
    return "Batch resource estimation: additional run on %d files".formatted(files);
  }
}
