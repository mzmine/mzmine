package io.github.mzmine.modules.tools.batch_resource_estimation;

import org.jetbrains.annotations.NotNull;

/**
 * Overall result of a resource estimation for a number of files. The estimate is reliable if the
 * largest test run was predicted well from the smaller runs.
 */
public enum EstimationVerdict {
  /**
   * The estimate is reliable and the heap and temp space are enough.
   */
  ENOUGH_RESOURCES,
  /**
   * The estimate is less reliable, but even this estimate fits the heap and temp space.
   */
  ENOUGH_RESOURCES_UNCERTAIN,
  /**
   * The estimate exceeds the heap or temp space but is not reliable. A larger test run improves
   * it.
   */
  LARGER_RUN_NEEDED,
  /**
   * The heap or temp space is not enough, the estimate is reliable or all files were used.
   */
  NOT_ENOUGH_RESOURCES;

  public @NotNull String title() {
    return switch (this) {
      case ENOUGH_RESOURCES -> "The batch fits into the available heap and temp space";
      case ENOUGH_RESOURCES_UNCERTAIN ->
          "The batch fits into the available heap and temp space, with some uncertainty";
      case LARGER_RUN_NEEDED -> "A larger test run is needed for a reliable estimate";
      case NOT_ENOUGH_RESOURCES -> "The batch needs more heap or temp space";
    };
  }

  /**
   * @return true if the batch can run with the current settings
   */
  public boolean isSufficient() {
    return switch (this) {
      case ENOUGH_RESOURCES, ENOUGH_RESOURCES_UNCERTAIN -> true;
      case LARGER_RUN_NEEDED, NOT_ENOUGH_RESOURCES -> false;
    };
  }
}
