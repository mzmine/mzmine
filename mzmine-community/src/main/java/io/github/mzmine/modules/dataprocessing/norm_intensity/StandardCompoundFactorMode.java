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

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import org.jetbrains.annotations.NotNull;

/**
 * Defines how a {@link StandardCompoundNormalizationFunction} turns the abundance of an internal
 * standard into a normalization factor. This is not a user parameter: new runs always use
 * {@link #MEDIAN_SCALED}, {@link #ABSOLUTE_LEGACY} only exists to reapply functions that were saved
 * before the median scaling was introduced.
 */
public enum StandardCompoundFactorMode implements UniqueIdSupplier {

  /**
   * factor = referenceAbundance / abundance, where the reference abundance is the median of this
   * standard over all reference samples. The factor is therefore around 1, the intensity scale is
   * preserved, and different standards are interchangeable because each of them only describes the
   * deviation of one sample from the reference level. This is required for the modes that allow
   * different standards in different samples, see {@link StandardCompoundNormalizationMode}.
   */
  MEDIAN_SCALED,
  /**
   * factor = 1 / abundance. Only reached when loading a normalization function saved by an older
   * mzmine version, so that gap filled features are normalized exactly like the features that were
   * normalized when the project was created.
   */
  ABSOLUTE_LEGACY;

  public static @NotNull StandardCompoundFactorMode getDefault() {
    return MEDIAN_SCALED;
  }

  @Override
  public String toString() {
    return switch (this) {
      case MEDIAN_SCALED -> "Median scaled";
      case ABSOLUTE_LEGACY -> "Absolute (legacy)";
    };
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case MEDIAN_SCALED -> "median_scaled";
      case ABSOLUTE_LEGACY -> "absolute_legacy";
    };
  }
}
