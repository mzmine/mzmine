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

package io.github.mzmine.datamodel.features;

import io.github.mzmine.datamodel.features.compoundlist.CompoundFeature;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import org.jetbrains.annotations.NotNull;


/**
 * Used to define if we are using Compound or Feature. like in {@link CompoundRow}
 * {@link FeatureListRow} or {@link Feature} {@link CompoundFeature}
 */
public enum FeatureListMode implements UniqueIdSupplier {
  /**
   * classical mzmine feature list rows and features for each detected ion
   */
  FEATURE_ROW,
  /**
   * Compound rows and features
   */
  COMPOUND_ROW;

  public static @NotNull FeatureListMode of(@NotNull FeatureListRow row) {
    return row instanceof CompoundRow ? COMPOUND_ROW : FEATURE_ROW;
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case FEATURE_ROW -> "feature_row";
      case COMPOUND_ROW -> "compound_row";
    };
  }
}
