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

package io.github.mzmine.modules.presets;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.presets.Preset;
import io.github.mzmine.util.presets.PresetCategory;
import org.jetbrains.annotations.NotNull;


/**
 * @param name
 * @param presetGroup the module uniqueID
 * @param parameters
 */
public record ModulePreset(String name, @NotNull String presetGroup,
                           ParameterSet parameters) implements Preset {

  @Override
  public @NotNull String toString() {
    return name;
  }

  @Override
  public @NotNull ModulePreset withName(String name) {
    return new ModulePreset(name, presetGroup, parameters);
  }


  // constant is set to read only - will save but not read it
  @Override
  @JsonProperty(access = Access.READ_ONLY)
  public @NotNull PresetCategory presetCategory() {
    return PresetCategory.MODULES;
  }

}
