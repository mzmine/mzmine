/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilePlaceholder;
import java.time.LocalDateTime;
import org.jetbrains.annotations.NotNull;

/**
 * Interpolates two normalization functions by weighting their returned feature factors.
 */
public class InterpolatedNormalizationFunction implements NormalizationFunction {

  private final RawDataFilePlaceholder referenceFilePlaceholder;
  private final LocalDateTime acquisitionTimestamp;
  private final NormalizationFunction previousFunction;
  private final double previousWeight;
  private final NormalizationFunction nextFunction;
  private final double nextWeight;

  public InterpolatedNormalizationFunction(@NotNull final RawDataFile targetFile,
      @NotNull final LocalDateTime acquisitionTimestamp,
      @NotNull final NormalizationFunction previousFunction, final double previousWeight,
      @NotNull final NormalizationFunction nextFunction, final double nextWeight) {
    this.referenceFilePlaceholder = new RawDataFilePlaceholder(targetFile);
    this.acquisitionTimestamp = acquisitionTimestamp;
    this.previousFunction = previousFunction;
    this.previousWeight = previousWeight;
    this.nextFunction = nextFunction;
    this.nextWeight = nextWeight;
  }

  @Override
  public @NotNull RawDataFilePlaceholder getRawDataFilePlaceholder() {
    return referenceFilePlaceholder;
  }

  @Override
  public @NotNull LocalDateTime getAcquisitionTimestamp() {
    return acquisitionTimestamp;
  }

  @Override
  public double getFactor(@NotNull final Double mz, @NotNull final Float rt) {
    return previousFunction.getFactor(mz, rt) * previousWeight
        + nextFunction.getFactor(mz, rt) * nextWeight;
  }
}
