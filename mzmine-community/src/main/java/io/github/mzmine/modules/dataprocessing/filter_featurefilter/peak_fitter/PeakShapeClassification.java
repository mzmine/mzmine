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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.filter_featurefilter.peak_fitter;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import org.jetbrains.annotations.NotNull;

public enum PeakShapeClassification implements UniqueIdSupplier {
  GAUSSIAN, FRONTING_GAUSSIAN, TAILING_GAUSSIAN, DOUBLE_GAUSSIAN;

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case GAUSSIAN -> "gaussian";
      case FRONTING_GAUSSIAN -> "fronting_gaussian";
      case TAILING_GAUSSIAN -> "tailing_gaussian";
      case DOUBLE_GAUSSIAN -> "double_gaussian";
    };
  }

  /**
   * penalize asymmetric/double gaussian fits to not make the model overfit the peaks
   */
  public double getPenaltyFactor() {
    return switch (this) {
      case GAUSSIAN -> 1.0;
      case FRONTING_GAUSSIAN, TAILING_GAUSSIAN -> 0.98d;
      case DOUBLE_GAUSSIAN -> 0.965d;
    };
  }


  @Override
  public String toString() {
    return switch (this) {
      case GAUSSIAN -> "Gaussian";
      case DOUBLE_GAUSSIAN -> "Double peak";
      case TAILING_GAUSSIAN -> "Tailing";
      case FRONTING_GAUSSIAN -> "Fronting";
    };
  }
}
