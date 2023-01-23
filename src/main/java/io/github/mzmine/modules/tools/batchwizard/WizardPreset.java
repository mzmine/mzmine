/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.tools.batchwizard;

import io.github.mzmine.modules.tools.batchwizard.subparameters.AbstractWizardParameters;
import io.github.mzmine.parameters.ParameterUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @param name           the title of the preset for LC, IMS, MS, ... toString
 * @param uniquePresetId for enums its the .name() for presets with only one options its the only
 *                       string and equal to the name
 * @param parameters     the parameters - will be cloned internally
 */
public record WizardPreset(@NotNull String name, @NotNull String uniquePresetId,
                           @NotNull AbstractWizardParameters<?> parameters) implements
    Comparable<WizardPreset> {

  /**
   * @param name           the title of the preset for LC, IMS, MS, ... toString
   * @param uniquePresetId for enums its the .name() for presets with only one options its the only
   *                       string and equal to the name
   * @param parameters     the parameters
   */
  public WizardPreset(final String name, final String uniquePresetId,
      final AbstractWizardParameters<?> parameters) {
    // needs the clone to separate the parameters from the static ones
    this.parameters = parameters;
    this.name = name;
    this.uniquePresetId = uniquePresetId;
  }


  @Override
  public String toString() {
    return name;
  }

  @Override
  public int compareTo(@NotNull final WizardPreset o) {
    return part().compareTo(o.part());
  }

  public WizardPart part() {
    return parameters.getPart();
  }


  /**
   * @return true if all parameters are set to the default values
   */
  public boolean hasDefaultParameters() {
    var defaultPreset = createDefaultParameterPreset();
    return defaultPreset != null && ParameterUtils.equalValues(defaultPreset.parameters,
        parameters);
  }

  /**
   * @return the default parameters preset
   */
  public WizardPreset createDefaultParameterPreset() {
    return part().createPresetParameters().stream()
        .filter(preset -> preset.uniquePresetId.equals(uniquePresetId)).findFirst().orElse(null);
  }
}
