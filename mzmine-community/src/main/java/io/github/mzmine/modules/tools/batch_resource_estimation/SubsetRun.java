package io.github.mzmine.modules.tools.batch_resource_estimation;

import io.github.mzmine.modules.batchmode.timing.StepMeasurement;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Measurements of one batch run on a subset of the files.
 *
 * @param files              number of raw data files in the subset
 * @param fileEquivalents    raw data bytes of the subset divided by the mean file size of the whole
 *                           dataset, the x of the scaling fits
 * @param baselineHeapGB     used heap after GC right before the run, the application baseline
 * @param baselineLiveTempGB live temp file data right before the run
 * @param maxRows            rows of the largest feature list after the run, usually the aligned
 *                           list
 * @param steps              per step measurements without the whole batch row
 */
public record SubsetRun(int files, double fileEquivalents, double baselineHeapGB,
                        double baselineLiveTempGB, int maxRows,
                        @NotNull List<StepMeasurement> steps) {

  /**
   * @return this run with other baselines, e.g. the common baselines of all runs
   */
  public @NotNull SubsetRun withBaselines(final double heapGB, final double liveTempGB) {
    return new SubsetRun(files, fileEquivalents, heapGB, liveTempGB, maxRows, steps);
  }
}
