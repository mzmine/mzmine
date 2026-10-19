package io.github.mzmine.modules.tools.batch_resource_estimation;

import org.jetbrains.annotations.NotNull;

/**
 * The per step resources that are plotted in the result of the resource estimation.
 */
public enum ResourceMetric {
  HEAP, RETAINED_HEAP, TEMP_FILES, TIME;

  public double value(@NotNull final StepResourcePrediction step) {
    return switch (this) {
      case HEAP -> step.heapGB();
      case RETAINED_HEAP -> step.retainedHeapGB();
      case TEMP_FILES -> step.liveTempGB();
      case TIME -> step.seconds();
    };
  }

  public @NotNull String title() {
    return switch (this) {
      case HEAP -> "Peak heap";
      case RETAINED_HEAP -> "Retained heap";
      case TEMP_FILES -> "Temp files";
      case TIME -> "Processing time";
    };
  }

  public @NotNull String axisLabel() {
    return switch (this) {
      case HEAP, RETAINED_HEAP, TEMP_FILES -> "GB";
      case TIME -> "seconds";
    };
  }
}
