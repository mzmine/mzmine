/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.dataanalysis.utils.scaling;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import org.jetbrains.annotations.NotNull;

public enum ScalingFunctions implements UniqueIdSupplier {
  AutoScaling, ParetoScaling, RangeScaling, MeanCentering, None;

  public final static ScalingFunctions[] valuesExcludeNone = new ScalingFunctions[]{AutoScaling,
      ParetoScaling, RangeScaling, MeanCentering};

  public final static ScalingFunctions[] valuesPCAOptions = new ScalingFunctions[]{AutoScaling,
      ParetoScaling, RangeScaling};

  public ScalingFunction getScalingFunction() {
    return switch (this) {
      case AutoScaling -> new AutoScalingFunction();
      case ParetoScaling -> new ParetoScalingFunction();
      case RangeScaling -> new RangeScalingFunction();
      case MeanCentering -> new MeanCenterScalingFunction();
      case None -> new NoneScalingFunction();
    };
  }

  @Override
  public String toString() {
    return switch (this) {
      case AutoScaling -> "Auto scaling (SD)";
      case ParetoScaling -> "Pareto scaling (√SD)";
      case RangeScaling -> "Range scaling [-1; 1]";
      case MeanCentering -> "Mean centering";
      case None -> "No scaling";
    };
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case AutoScaling -> "AutoScaling";
      case ParetoScaling -> "ParetoScaling";
      case RangeScaling -> "RangeScaling";
      case MeanCentering -> "MeanCentering";
      case None -> "None";
    };
  }

  /**
   * @return true if this is not None
   */
  public boolean isActive() {
    return this != None;
  }
}
