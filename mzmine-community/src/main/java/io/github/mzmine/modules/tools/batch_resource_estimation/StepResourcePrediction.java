package io.github.mzmine.modules.tools.batch_resource_estimation;

import org.jetbrains.annotations.NotNull;

/**
 * Predicted resources of one batch step for a given number of files. Flat so that it can be
 * written as CSV.
 *
 * @param step            1-based step number
 * @param name            module name of the step
 * @param scaling         how the step grows, see {@link StepScalingMode}
 * @param heapGB          predicted peak live heap during the step, including the application
 *                        baseline
 * @param retainedHeapGB  part of {@link #heapGB} that is retained data, including the baseline
 * @param liveTempGB      predicted temp file data alive at the end of the step
 * @param seconds         predicted wall clock time
 */
public record StepResourcePrediction(int step, @NotNull String name, @NotNull String scaling,
                                     double heapGB, double retainedHeapGB, double liveTempGB,
                                     double seconds) {

}
