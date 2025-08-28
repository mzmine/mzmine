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

package io.github.mzmine.parameters.parametertypes.row_type_filter;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.github.mzmine.parameters.parametertypes.row_type_filter.filters.RowTypeFilter;
import io.github.mzmine.util.presets.KnownPresetGroup;
import io.github.mzmine.util.presets.Preset;
import io.github.mzmine.util.presets.PresetCategory;
import org.jetbrains.annotations.NotNull;


@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
public record RowTypeFilterPreset(String name, RowTypeFilter filter) implements Preset {

  public static RowTypeFilterPreset createPreset(String name, RowTypeFilterOption option,
      MatchingMode mode, String query) {
    final RowTypeFilter filter = RowTypeFilter.create(option, mode, query);
    return createPreset(name, filter);
  }

  public static @NotNull RowTypeFilterPreset createPreset(String name, RowTypeFilter filter) {
    return new RowTypeFilterPreset(name, filter);
  }


  @Override
  public @NotNull String toString() {
    return "%s: %s %s %s".formatted(filter.selectedType(), name, filter.matchingMode(),
        filter.query());
  }

  @Override
  public @NotNull RowTypeFilterPreset withName(String name) {
    return createPreset(name, filter);
  }

  @Override
  public @NotNull PresetCategory presetCategory() {
    return PresetCategory.FILTERS;
  }

  @Override
  public @NotNull String presetGroup() {
    return KnownPresetGroup.ROW_TYPE_FILTER_PRESET.getUniqueID();
  }
}
