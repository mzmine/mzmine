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

package io.github.mzmine.modules.visualization.projectmetadata.table;

import io.github.mzmine.datamodel.RawDataFile;
import org.jetbrains.annotations.NotNull;

/**
 * Interpolation weights for one sample, 2, or multiple. The weights reflect the date distance to a
 * target sample.
 */
public sealed interface InterpolationWeights {

  public static InterpolationWeights createBinary(@NotNull RawDataFile pre,
      @NotNull RawDataFile next, double weightPre, double weightNext) {
    return new BinaryInterpolationWeights(pre, next, weightPre, weightNext);
  }

  public static InterpolationWeights createSingle(@NotNull RawDataFile file) {
    return new SingleInterpolationWeight(file);
  }

  public static InterpolationWeights create(@NotNull InterpolationWeight @NotNull ... weights) {
    if (weights.length == 1) {
      return new SingleInterpolationWeight(weights[0].file);
    } else if (weights.length == 2) {
      return new BinaryInterpolationWeights(weights[0], weights[1]);
    }
    return new MultiInterpolationWeights(weights);
  }

  record InterpolationWeight(@NotNull RawDataFile file, double weight) {

  }

  @NotNull
  default InterpolationWeight @NotNull [] weights() {
    return switch (this) {
      case SingleInterpolationWeight w ->
          new InterpolationWeight[]{new InterpolationWeight(w.closestRun, 1)};
      case BinaryInterpolationWeights w -> new InterpolationWeight[]{w.previousRun, w.nextRun};
      case MultiInterpolationWeights w -> w.weights;
    };
  }

  record SingleInterpolationWeight(@NotNull RawDataFile closestRun) implements
      InterpolationWeights {

  }

  /**
   * Weights to interpolate normalization between two different two related data files.
   *
   * @param nextRun     sample and weight for the previous run (how close it is time)
   * @param previousRun sample and weight for the next run (how close it is in time)
   */
  record BinaryInterpolationWeights(@NotNull InterpolationWeight previousRun,
                                    @NotNull InterpolationWeight nextRun) implements
      InterpolationWeights {

    public BinaryInterpolationWeights(@NotNull RawDataFile a, @NotNull RawDataFile b,
        double weightA, double weightB) {
      this(new InterpolationWeight(a, weightA), new InterpolationWeight(b, weightB));
    }

  }

  /**
   * For interpolations across multiple QCs
   */
  record MultiInterpolationWeights(@NotNull InterpolationWeight @NotNull [] weights) implements
      InterpolationWeights {

  }
}
