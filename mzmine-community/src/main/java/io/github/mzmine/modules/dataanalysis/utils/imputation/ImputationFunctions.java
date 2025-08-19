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

package io.github.mzmine.modules.dataanalysis.utils.imputation;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import org.jetbrains.annotations.NotNull;

public enum ImputationFunctions implements UniqueIdSupplier {

  GLOBAL_LIMIT_OF_DETECTION, OneFifthOfMinimum, Zero, None;

  public static final ImputationFunctions[] valuesExcludeNone = new ImputationFunctions[]{
      GLOBAL_LIMIT_OF_DETECTION, OneFifthOfMinimum, Zero};

  public ImputationFunction getImputer() {
    return switch (this) {
      case Zero -> new ZeroImputer();
      case GLOBAL_LIMIT_OF_DETECTION -> new GlobalLimitOfDetectionImputer();
      case OneFifthOfMinimum -> new OneFifthOfMinimumImputer();
      case None -> new KeepOriginalImputer();
    };
  }

  @Override
  public String toString() {
    return switch (this) {
      case Zero -> "Zero (0)";
      case OneFifthOfMinimum -> "1/5 of minimum";
      case GLOBAL_LIMIT_OF_DETECTION ->
          "LOD (1/%.0f of global minimum)".formatted(GlobalLimitOfDetectionImputer.DEVISOR);
      case None -> "None";
    };
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case Zero -> "Zero";
      case OneFifthOfMinimum -> "OneFifthOfMinimum";
      case GLOBAL_LIMIT_OF_DETECTION -> "GLOBAL_LIMIT_OF_DETECTION";
      case None -> "none";
    };
  }

  /**
   * @return true if not None
   */
  public boolean isActive() {
    return this != None;
  }
}
