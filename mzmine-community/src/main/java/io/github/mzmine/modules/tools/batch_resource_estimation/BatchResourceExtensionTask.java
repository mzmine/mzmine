package io.github.mzmine.modules.tools.batch_resource_estimation;

import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractSimpleToolTask;
import java.time.Instant;
import java.util.function.BiConsumer;
import org.jetbrains.annotations.NotNull;

/**
 * Adds one larger run to a finished resource estimation, e.g. because a step did not level off up
 * to the largest run, and fits the model again with all runs.
 */
public class BatchResourceExtensionTask extends AbstractSimpleToolTask {

  private final @NotNull EstimationSession session;
  private final int files;
  private final @NotNull BiConsumer<EstimationSession, BatchResourceModel> onFinished;

  /**
   * @param parameters the parameters of the estimation
   * @param files      files of the additional run
   * @param onFinished receives the session with the additional run and the new model, called on
   *                   the task thread
   */
  public BatchResourceExtensionTask(@NotNull final Instant moduleCallDate,
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
