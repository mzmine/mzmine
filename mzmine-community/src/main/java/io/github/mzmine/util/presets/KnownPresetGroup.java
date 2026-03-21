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

package io.github.mzmine.util.presets;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.parameters.parametertypes.row_type_filter.RowTypeFilterPreset;
import org.jetbrains.annotations.NotNull;

public enum KnownPresetGroup implements PresetGroup {

  /**
   * {@link RowTypeFilterPreset}
   */
  ROW_TYPE_FILTER_PRESET;

  public KnownPresetGroup parse(String name) {
    return UniqueIdSupplier.parseOrElse(name, values(), null);
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case ROW_TYPE_FILTER_PRESET -> "feature_table_filters";
    };
  }

  @Override
  public String toString() {
    return switch (this) {
      case ROW_TYPE_FILTER_PRESET -> "Feature table filters";
    };
  }
}
