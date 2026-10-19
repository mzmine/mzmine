package io.github.mzmine.modules.tools.batch_resource_estimation;

import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Predicted resources of the whole batch for a given number of files.
 *
 * @param files                  number of files the prediction is for
 * @param steps                  per step predictions
 * @param heapGB                 highest predicted live heap of all steps
 * @param retainedHeapGB         highest predicted retained heap of all steps, the data that stays
 *                               in memory independent of the threads
 * @param tempGB                 highest predicted live temp file data of all steps
 * @param seconds                predicted total time of all steps
 * @param rows                   predicted rows of the largest (aligned) feature list
 * @param recommendedXmxGB       heap to configure (-Xmx) including GC headroom, whole GB
 * @param heapBottleneckStep     name of the step with the highest heap
 * @param retainedBottleneckStep name of the step with the highest retained heap
 */
public record BatchResourcePrediction(int files, @NotNull List<StepResourcePrediction> steps,
                                      double heapGB, double retainedHeapGB, double tempGB,
                                      double seconds, int rows, int recommendedXmxGB,
                                      @NotNull String heapBottleneckStep,
                                      @NotNull String retainedBottleneckStep) {

}
