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

import org.jetbrains.annotations.NotNull;

/**
 * Overall result of a resource estimation for a number of files. The estimate is reliable if the
 * largest test run was predicted well from the smaller runs and the heap measurements were taken
 * after observed full collections.
 */
enum EstimationVerdict {
  /**
   * The estimate is reliable and the heap and temp space are enough.
   */
  ENOUGH_RESOURCES,
  /**
   * The estimate is less reliable, but even this estimate fits the heap and temp space.
   */
  ENOUGH_RESOURCES_UNCERTAIN,
  /**
   * Only unreliable steps exceed the available RAM, or no step has a reliable measurement.
   * The missing live RAM requirements prevent a conclusion about the whole batch.
   */
  RAM_REQUIREMENT_UNCERTAIN,
  /**
   * The estimate exceeds the heap or temp space but is not reliable. A larger test run may
   * improve it.
   */
  LARGER_RUN_NEEDED,
  /**
   * A reliable RAM or temp space estimate exceeds the available resources, or the uncertain
   * extrapolation cannot be extended further. Missing RAM measurements alone do not imply this.
   */
  NOT_ENOUGH_RESOURCES;

  @NotNull String title() {
    return switch (this) {
      case ENOUGH_RESOURCES -> "The batch fits within the available RAM and temporary storage";
      case ENOUGH_RESOURCES_UNCERTAIN -> "The batch should fit, but the estimate is uncertain";
      case RAM_REQUIREMENT_UNCERTAIN -> "The RAM requirement is uncertain";
      case LARGER_RUN_NEEDED -> "A larger test run is needed for a reliable estimate";
      case NOT_ENOUGH_RESOURCES -> "The batch needs more RAM or temporary storage";
    };
  }

  /**
   * @return true if the batch can run with the current settings
   */
  boolean isSufficient() {
    return switch (this) {
      case ENOUGH_RESOURCES, ENOUGH_RESOURCES_UNCERTAIN -> true;
      case RAM_REQUIREMENT_UNCERTAIN, LARGER_RUN_NEEDED, NOT_ENOUGH_RESOURCES -> false;
    };
  }
}
