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

package io.github.mzmine.modules.visualization.projectmetadata.extract;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.github.mzmine.util.presets.KnownPresetGroup;
import io.github.mzmine.util.presets.Preset;
import io.github.mzmine.util.presets.PresetCategory;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * A single reusable {@link MetadataRegexMapping} - one row of the
 * {@link MetadataRegexExtractionComponent} grid, including its value mappings and the
 * remaining-values handling. Added to the grid by the presets button next to "Add mapping".
 *
 * @param name    the preset name shown in the presets menu
 * @param mapping the mapping added as a new row when this preset is activated
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
public record MetadataRegexMappingPreset(@NotNull String name,
                                         @NotNull MetadataRegexMapping mapping) implements Preset {

  /**
   * @param name                the preset name
   * @param column              the target metadata column
   * @param type                the target column type
   * @param regex               the regex applied to the file name
   * @param unmappedValue       the value to use when a match is found but no mapping
   * @param generalDefaultValue the value to use when no match is found and no unmapped value is
   *                            specified, even if there is no match
   * @param mapping             value mappings and remaining-value handling applied to the extracted
   *                            value
   * @return a preset extracting from {@link RegexInputSource#FILE_NAME}
   */
  public static @NotNull MetadataRegexMappingPreset createMappingPreset(@NotNull final String name,
      @NotNull final String column, @NotNull final ExtractColumnType type,
      @NotNull final String regex, @NotNull final DropUnmappedMode dropUnmapped,
      @NotNull final String unmappedValue, @NotNull String generalDefaultValue,
      @NotNull final List<MetadataValueMapping> mapping) {
    return new MetadataRegexMappingPreset(name,
        new MetadataRegexMapping(RegexInputSource.FILE_NAME, column, type, regex,
            generalDefaultValue, dropUnmapped,
            unmappedValue, mapping));
  }

  /**
   * @return a preset that stores the extracted value directly, without value mappings
   */
  public static @NotNull MetadataRegexMappingPreset createExactValuePreset(
      @NotNull final String name, @NotNull final String column,
      @NotNull final ExtractColumnType type, @NotNull final String regex) {
    return createMappingPreset(name, column, type, regex, DropUnmappedMode.KEEP_UNMAPPED, "", "",
        List.of());
  }

  /**
   * @return a preset that maps all values to one single value
   */
  public static @NotNull MetadataRegexMappingPreset createMapOneValuePreset(
      @NotNull final String name, @NotNull final String column,
      @NotNull final ExtractColumnType type, @NotNull final String regex,
      @NotNull final String unmappedValue) {
    return createMappingPreset(name, column, type, regex, DropUnmappedMode.KEEP_UNMAPPED,
        unmappedValue, "", List.of());
  }

  @Override
  public @NotNull String toString() {
    return "%s (%s)".formatted(name, mapping.columnName().isBlank() ? "?" : mapping.columnName());
  }

  @Override
  public @NotNull MetadataRegexMappingPreset withName(final String name) {
    return new MetadataRegexMappingPreset(name, mapping);
  }

  @Override
  public @NotNull PresetCategory presetCategory() {
    return PresetCategory.OTHER;
  }

  @Override
  public @NotNull String presetGroup() {
    return KnownPresetGroup.METADATA_REGEX_MAPPING_PRESET.getUniqueID();
  }
}
