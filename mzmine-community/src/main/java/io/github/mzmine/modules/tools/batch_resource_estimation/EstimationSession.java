package io.github.mzmine.modules.tools.batch_resource_estimation;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * All runs of a resource estimation and what is needed to add further runs to it.
 *
 * @param runner        runs further subsets of the same batch and files
 * @param modes         scaling mode of each batch step
 * @param numThreads    number of threads of the runs
 * @param fitRuns       the subsets of the extrapolation, ascending by size
 * @param validationRun the run that validates the extrapolation of the subsets
 * @param extensionRuns larger runs that were added after the validation run, ascending by size
 */
public record EstimationSession(@NotNull SubsetRunner runner,
                                @NotNull List<StepScalingMode> modes, int numThreads,
                                @NotNull List<SubsetRun> fitRuns, @NotNull SubsetRun validationRun,
                                @NotNull List<SubsetRun> extensionRuns) {

  public @NotNull BatchResourceModel fitModel() {
    return BatchResourceModel.fit(fitRuns, validationRun, extensionRuns, modes,
        runner.totalFiles(), numThreads);
  }

  /**
   * @return this session with a further larger run
   */
  public @NotNull EstimationSession withExtension(@NotNull final SubsetRun run) {
    final List<SubsetRun> extensions = new ArrayList<>(extensionRuns);
    extensions.add(run);
    return new EstimationSession(runner, modes, numThreads, fitRuns, validationRun,
        List.copyOf(extensions));
  }
}
