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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.mzmine.modules.presets.ModulePreset;
import io.github.mzmine.parameters.parametertypes.row_type_filter.RowTypeFilterPreset;
import io.github.mzmine.util.files.FileAndPathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Preset defines presets in mzmine. They may be save in json format with the
 * {@link AbstractJsonPresetStore} or to other formats with other {@link PresetStore}.
 * <p>
 * For jackson useage, add new preset classes to the JsonSubTypes annotation below. This adds a
 * field into
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,   // Use a logical name for the type
    include = JsonTypeInfo.As.PROPERTY,     // Include it as a property in the JSON
    property = "preset_type_class"          // The property name will be "preset_type_class"
)
@JsonSubTypes({
    // map names to classes
    @JsonSubTypes.Type(value = RowTypeFilterPreset.class, name = "row_type_filter"), //
    @JsonSubTypes.Type(value = ModulePreset.class, name = "module_preset") //
})
public interface Preset extends Comparable<Preset> {

  @NotNull String name();

  /**
   * Path safe name
   *
   * @return name but path save encoding
   */
  @JsonIgnore
  default @NotNull String getFileName() {
    return FileAndPathUtil.safePathEncode(name());
  }

  @Override
  default int compareTo(@Nullable Preset o) {
    if (o == null) {
      return 1;
    } else if (this == o) {
      return 0;
    }
    return toString().compareTo(o.toString());
  }

  /**
   * a category (category>group) that acts as the first folder in presets directory
   */
  @NotNull PresetCategory presetCategory();


  /**
   * The group name (category>group). For modules this is the Module class name.
   */
  @JsonInclude
  @Nullable String presetGroup();

  /**
   * @return true if filename or name equal case insensitive
   */
  default boolean equalsIgnoreCaseName(Preset preset) {
    return getFileName().equalsIgnoreCase(preset.getFileName()) || name().equalsIgnoreCase(
        preset.name());
  }

  @NotNull <T extends Preset> T withName(String name);

  /**
   * Exclude name during check to only focus on preset content
   *
   * @return true if content equals (exculding name)
   */
  default boolean equalsByContent(@Nullable Preset other) {
    if (other == null) {
      return false;
    }
    if (other == this) {
      return true;
    }
    return this.withName(other.name()).equals(other);
  }
}
